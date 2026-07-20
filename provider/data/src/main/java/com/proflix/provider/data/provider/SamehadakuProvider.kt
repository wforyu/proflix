package com.proflix.provider.data.provider

import com.proflix.common.utils.Result
import com.proflix.common.utils.safeCall
import com.proflix.core.network.EmbedVideoExtractor
import com.proflix.provider.domain.Provider
import com.proflix.provider.domain.model.Content
import com.proflix.provider.domain.model.ContentType
import com.proflix.provider.domain.model.Episode
import com.proflix.provider.domain.model.HomeContent
import com.proflix.provider.domain.model.StreamSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log

@Singleton
class SamehadakuProvider @Inject constructor(
    private val okHttpClient: OkHttpClient
) : Provider {

    private val embedExtractor = EmbedVideoExtractor(okHttpClient)

    companion object {
        var BASE_URL = "https://x6.sokuja.uk"
        private const val TAG = "SamehadakuProvider"
    }

    override suspend fun getHome(): Result<HomeContent> = withContext(Dispatchers.IO) {
        safeCall {
            val html = fetchHtml(BASE_URL)
            val doc = Jsoup.parse(html, BASE_URL)

            val hero = parseHeroFromRsc(html, doc)
            val trending = parseSidebarPopular(doc)
            val latest = parseLatestFromRsc(html)

            HomeContent(
                heroContent = hero,
                trending = trending,
                latest = latest,
                continueWatching = emptyList(),
                categories = buildMap {
                    if (trending.isNotEmpty()) put("Anime Populer", trending)
                    if (latest.isNotEmpty()) put("Update Terbaru", latest)
                }
            )
        }
    }

    override suspend fun getTrending(): Result<List<Content>> = withContext(Dispatchers.IO) {
        safeCall {
            val html = fetchHtml(BASE_URL)
            val doc = Jsoup.parse(html, BASE_URL)
            parseSidebarPopular(doc)
        }
    }

    override suspend fun getLatest(): Result<List<Content>> = withContext(Dispatchers.IO) {
        safeCall {
            val html = fetchHtml(BASE_URL)
            parseLatestFromRsc(html)
        }
    }

    override suspend fun getDetail(id: String): Result<Content> = withContext(Dispatchers.IO) {
        safeCall {
            val url = if (id.startsWith("http")) id else "$BASE_URL/$id"
            val html = fetchHtml(url)
            val doc = Jsoup.parse(html, url)

            val tvSeries = extractLdJson(doc)

            val title = tvSeries?.optString("name", "")?.ifBlank { null }
                ?: doc.selectFirst("h1")?.text()
                ?: id.substringAfterLast("/").substringBeforeLast("-")
                    .replace("-", " ").trim().replaceFirstChar { it.uppercase() }

            val poster = tvSeries?.optString("image", "")?.ifBlank { null }
                ?: doc.selectFirst("meta[property=\"og:image\"]")?.attr("content")
                ?: ""

            val description = tvSeries?.optString("description", "")?.ifBlank { null }
                ?: doc.selectFirst("meta[property=\"og:description\"]")?.attr("content")
                ?: ""

            val genres = mutableListOf<String>()
            tvSeries?.optJSONArray("genre")?.let { arr ->
                for (i in 0 until arr.length()) {
                    arr.optString(i)?.let { if (it.isNotBlank()) genres.add(it) }
                }
            }
            if (genres.isEmpty()) {
                for (a in doc.select("a[href*=\"/genre/\"]")) {
                    val g = a.text().trim()
                    if (g.isNotBlank() && g !in genres) genres.add(g)
                }
            }

            var year = ""
            var rating = ""
            for (dt in doc.select("dt")) {
                val dtText = dt.text().trim()
                if (dtText.contains("Tahun", true) || dtText.contains("Year", true)) {
                    val dd = dt.nextElementSibling()
                    if (dd != null && dd.tagName() == "dd") {
                        year = dd.text().trim()
                    }
                }
            }
            val ratingObj = tvSeries?.optJSONObject("aggregateRating")
            rating = ratingObj?.optString("ratingValue", "") ?: rating

            val animeType = tvSeries?.optString("@type", "") ?: ""
            val contentType = when {
                animeType.contains("Movie", true) -> ContentType.MOVIE
                else -> ContentType.ANIME
            }

            Content(
                id = id,
                title = title,
                poster = poster,
                banner = poster,
                description = description,
                genres = genres,
                year = year,
                rating = rating,
                type = contentType
            )
        }
    }

    override suspend fun getEpisodes(contentId: String): Result<List<Episode>> = withContext(Dispatchers.IO) {
        safeCall {
            val url = if (contentId.startsWith("http")) contentId else "$BASE_URL/$contentId"
            val html = fetchHtml(url)

            val episodesFromRsc = parseEpisodesFromRsc(html, contentId)
            if (episodesFromRsc.isNotEmpty()) {
                Log.d(TAG, "Parsed ${episodesFromRsc.size} episodes from RSC data")
                return@safeCall episodesFromRsc.sortedBy { it.number }
            }

            Log.d(TAG, "RSC episodes empty, falling back to HTML parsing")
            val doc = Jsoup.parse(html, url)
            val episodes = mutableListOf<Episode>()

            val animeSlug = contentId.substringAfterLast("/").substringBeforeLast("-subtitle-indonesia")
                .substringBeforeLast("-episode-")

            for (a in doc.select("a[href*=\"-episode-\"][href*=\"-subtitle-indonesia\"]")) {
                val href = a.attr("href").trim()
                val epUrl = href.removePrefix(BASE_URL).trimStart('/')
                if (epUrl.isBlank()) continue

                if (animeSlug.isNotBlank() && !epUrl.startsWith(animeSlug)) continue

                val epTitle = a.selectFirst(".text-gray-200, .font-medium")?.text()
                    ?: a.text().trim().ifBlank { null }
                    ?: ""

                val epNum = Regex("episode[- ]?(\\d+)").find(epUrl.lowercase())?.groupValues?.get(1)?.toIntOrNull()
                    ?: Regex("Ep\\s*(\\d+)").find(epTitle)?.groupValues?.get(1)?.toIntOrNull()
                    ?: (episodes.size + 1)

                val thumbnail = getImageUrl(a.selectFirst("img"))

                if (episodes.none { it.id == epUrl }) {
                    episodes.add(
                        Episode(
                            id = epUrl,
                            contentId = contentId,
                            season = 1,
                            number = epNum,
                            title = epTitle.ifBlank { "Episode $epNum" },
                            description = "",
                            thumbnail = thumbnail ?: "",
                            duration = 0L
                        )
                    )
                }
            }

            episodes.sortedBy { it.number }
        }
    }

    override suspend fun getStream(episodeId: String): Result<List<StreamSource>> = withContext(Dispatchers.IO) {
        safeCall {
            val sources = mutableListOf<StreamSource>()
            val url = if (episodeId.startsWith("http")) episodeId else "$BASE_URL/$episodeId"
            val html = fetchHtml(url)

            val episodeNumber = extractEpisodeNumberFromLdJson(html)
            if (episodeNumber != null) {
                val mirrors = fetchVideoMirrors(episodeNumber)
                sources.addAll(mirrors)
            }

            if (sources.isEmpty()) {
                val doc = Jsoup.parse(html, url)
                for (iframe in doc.select("iframe[src]")) {
                    val fullUrl = getImageUrl(iframe) ?: continue
                    if (fullUrl.contains("about:blank") || fullUrl.contains("storages.sokuja.uk/sda/")) continue

                    if (EmbedVideoExtractor.isDirectVideoUrl(fullUrl)) {
                        val format = EmbedVideoExtractor.detectFormat(fullUrl)
                        sources.add(StreamSource(url = fullUrl, quality = "default", format = format.extension))
                    } else {
                        val result = embedExtractor.extract(fullUrl)
                        if (result != null) {
                            sources.add(StreamSource(url = result.url, quality = "default", format = result.format.extension))
                        }
                    }
                }
            }

            sources
        }
    }

    override suspend fun search(query: String): Result<List<Content>> = withContext(Dispatchers.IO) {
        safeCall {
            val doc = fetchDocument("$BASE_URL/?s=${java.net.URLEncoder.encode(query, "UTF-8")}")
            val results = mutableListOf<Content>()

            for (a in doc.select("a[href*=\"/anime/\"][href*=\"-subtitle-indonesia\"]")) {
                val href = a.attr("href").trim()
                val id = href.removePrefix(BASE_URL).trimStart('/')
                if (id.isBlank()) continue

                val title = a.selectFirst("h3, .font-medium")?.text()
                    ?: a.selectFirst("img")?.attr("alt")
                    ?: ""
                val poster = getImageUrl(a.selectFirst("img")) ?: ""
                val year = a.selectFirst(".text-gray-400")?.text()?.trim() ?: ""

                if (title.isNotBlank() && id.isNotBlank() && results.none { it.id == id }) {
                    results.add(
                        Content(
                            id = id,
                            title = title,
                            poster = poster,
                            banner = poster,
                            description = "",
                            genres = emptyList(),
                            year = year,
                            rating = "",
                            type = ContentType.ANIME
                        )
                    )
                }
            }

            results
        }
    }

    override suspend fun getProviderName(): String = "Samehadaku"

    private fun fetchDocument(url: String): Document {
        val html = fetchHtml(url)
        return Jsoup.parse(html, url)
    }

    private fun fetchHtml(url: String): String {
        val request = Request.Builder()
            .url(url)
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .addHeader("Accept-Language", "id-ID,id;q=0.9,en-US;q=0.8,en;q=0.7")
            .build()

        val response = okHttpClient.newCall(request).execute()
        response.use {
            return it.body?.string() ?: throw Exception("Empty response")
        }
    }

    private fun getImageUrl(element: Element?): String? {
        val src = element?.attr("src")?.ifBlank { null }
            ?: element?.attr("data-src")?.ifBlank { null }
            ?: return null
        return when {
            src.startsWith("http") -> src
            src.startsWith("//") -> "https:$src"
            src.startsWith("/") -> "$BASE_URL$src"
            else -> src
        }
    }

    private fun extractLdJson(doc: Document): JSONObject? {
        for (script in doc.select("script[type=\"application/ld+json\"]")) {
            try {
                val json = JSONObject(script.html())
                val type = json.optString("@type", "")
                if (type == "TVSeries") return json
            } catch (_: Exception) {}
        }
        return null
    }

    private fun extractEpisodeNumberFromLdJson(html: String): Int? {
        val pattern = Regex("""\"episodeNumber\"\s*:\s*(\d+)""")
        return pattern.find(html)?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun fetchVideoMirrors(episodeId: Int): List<StreamSource> {
        val sources = mutableListOf<StreamSource>()
        try {
            val request = Request.Builder()
                .url("$BASE_URL/api/video-mirrors?e=$episodeId")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .addHeader("Accept", "application/json")
                .addHeader("Referer", "$BASE_URL/")
                .build()

            val response = okHttpClient.newCall(request).execute()
            val body = response.use { it.body?.string() } ?: return sources
            val json = JSONObject(body)
            val mirrors = json.optJSONArray("mirrors")
                ?: json.optJSONArray("data")
                ?: json.optJSONObject("mirrors")?.let { obj ->
                    val arr = JSONArray()
                    arr.put(obj)
                    arr
                }
                ?: return sources

            for (i in 0 until mirrors.length()) {
                val mirror = mirrors.optJSONObject(i) ?: continue
                val videoUrl = mirror.optString("url", "")
                    .ifBlank { mirror.optString("src", "") }
                    .ifBlank { mirror.optString("embedUrl", "") }
                val quality = mirror.optString("quality", mirror.optString("serverName", "default"))

                if (videoUrl.isNotBlank()) {
                    val format = EmbedVideoExtractor.detectFormat(videoUrl)
                    sources.add(
                        StreamSource(
                            url = videoUrl,
                            quality = quality,
                            format = format.extension
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "fetchVideoMirrors failed: ${e.message}")
        }

        return sources
    }

    private fun parseSidebarPopular(doc: Document): List<Content> {
        val contents = mutableListOf<Content>()

        for (a in doc.select("aside ul li a[href*=\"/anime/\"]")) {
            val href = a.attr("href").trim()
            val id = href.removePrefix(BASE_URL).trimStart('/')
            val title = a.selectFirst("p.font-medium, p.text-sm")?.text()?.trim() ?: ""
            val poster = getImageUrl(a.selectFirst("img")) ?: ""

            if (title.isNotBlank() && id.isNotBlank() && contents.none { it.id == id }) {
                contents.add(
                    Content(
                        id = id,
                        title = title,
                        poster = poster,
                        banner = poster,
                        description = "",
                        genres = emptyList(),
                        year = "",
                        rating = "",
                        type = ContentType.ANIME
                    )
                )
            }
        }

        return contents
    }

    private fun parseHeroFromRsc(html: String, doc: Document): Content? {
        val s0Div = doc.selectFirst("div[id=\"S:0\"]")
        if (s0Div != null) {
            val heroLink = s0Div.selectFirst("a[href*=\"/anime/\"][href*=\"-subtitle-indonesia\"]")
            if (heroLink != null) {
                val href = heroLink.attr("href").trim()
                val id = href.removePrefix(BASE_URL).trimStart('/')
                val title = heroLink.selectFirst("img")?.attr("alt")
                    ?: heroLink.selectFirst("h2, h3, p.font-medium")?.text()
                    ?: ""
                val poster = getImageUrl(heroLink.selectFirst("img")) ?: ""
                val desc = heroLink.selectFirst("p.text-sm, .text-gray-300")?.text() ?: ""

                val genres = mutableListOf<String>()
                for (a in s0Div.select("a[href*=\"/genre/\"]")) {
                    val g = a.text().trim()
                    if (g.isNotBlank() && g !in genres) genres.add(g)
                }

                val rating = s0Div.select("span").mapNotNull { it.text().trim() }
                    .firstOrNull { it.startsWith("★") }
                    ?.replace("★", "")?.trim() ?: ""

                if (title.isNotBlank()) {
                    return Content(
                        id = id,
                        title = title,
                        poster = poster,
                        banner = poster,
                        description = desc,
                        genres = genres,
                        year = "",
                        rating = rating,
                        type = ContentType.ANIME
                    )
                }
            }
        }

        val heroPattern = Regex("\"slug\":\"([^\"]+)-subtitle-indonesia\"")
        val titlePattern = Regex("""\"name"\s*:\s*"([^"]+)"""")
        val imgPattern = Regex("""\"bannerUrl"\s*:\s*"([^"]+)"""")
        val scorePattern = Regex("""\"score"\s*:\s*(\d+\.?\d*)""")
        val tagsPattern = Regex("""\{"tag":\{"name":"([^"]+)","slug":"([^"]+)","type":"genre"\}""")

        val slug = heroPattern.find(html)?.groupValues?.get(1) ?: return null
        val title = titlePattern.find(html)?.groupValues?.get(1) ?: slug
            .replace("-", " ").replaceFirstChar { it.uppercase() }
        val img = imgPattern.find(html)?.groupValues?.get(1) ?: ""
        val score = scorePattern.find(html)?.groupValues?.get(1) ?: ""

        val genres = tagsPattern.findAll(html).map { it.groupValues[1] }.distinct().take(5).toList()

        val poster = when {
            img.startsWith("http") -> img
            img.startsWith("/") -> "$BASE_URL$img"
            else -> img
        }

        return Content(
            id = "$slug-subtitle-indonesia",
            title = title,
            poster = poster,
            banner = poster,
            description = "",
            genres = genres,
            year = "",
            rating = score,
            type = ContentType.ANIME
        )
    }

    private fun parseLatestFromRsc(html: String): List<Content> {
        val contents = mutableListOf<Content>()
        val seen = mutableSetOf<String>()

        val episodePattern = Regex(""""href"\s*:\s*"/([^"]+?)-episode-(\d+)-subtitle-indonesia/"""")
        for (match in episodePattern.findAll(html)) {
            val slug = match.groupValues[1]
            if (slug.isBlank() || slug in seen) continue

            seen.add(slug)

            val epNum = match.groupValues[2].toIntOrNull()
            val title = slug.replace("-", " ").replaceFirstChar { it.uppercase() }

            contents.add(
                Content(
                    id = "$slug-subtitle-indonesia",
                    title = title,
                    poster = "",
                    banner = "",
                    description = if (epNum != null) "Episode $epNum" else "",
                    genres = emptyList(),
                    year = "",
                    rating = "",
                    type = ContentType.ANIME
                )
            )

            if (contents.size >= 20) break
        }

        if (contents.isEmpty()) {
            val altPattern = Regex("\"([^\"]+)-episode-(\\d+)-subtitle-indonesia\"")
            for (match in altPattern.findAll(html)) {
                val slug = match.groupValues[1]
                if (slug.isNotBlank() && slug !in seen) {
                    seen.add(slug)
                    val title = slug.replace("-", " ").replaceFirstChar { it.uppercase() }
                    contents.add(
                        Content(
                            id = "$slug-subtitle-indonesia",
                            title = title,
                            poster = "",
                            banner = "",
                            description = "",
                            genres = emptyList(),
                            year = "",
                            rating = "",
                            type = ContentType.ANIME
                        )
                    )
                    if (contents.size >= 20) break
                }
            }
        }

        return contents.take(20)
    }

    private fun parseEpisodesFromRsc(html: String, contentId: String): List<Episode> {
        val episodes = mutableListOf<Episode>()

        val episodesArrayPattern = Regex("\"episodes\":\\[(.+?)\\](?=,\\s*\"episodes(?:Total|Count)\")")
        val match = episodesArrayPattern.find(html)

        if (match == null) {
            val altPattern = Regex("\"episodes\":\\[(\\{.*?\\}(?:,\\{.*?\\})*)\\]")
            val altMatch = altPattern.find(html) ?: return episodes
            return parseEpisodesArray(altMatch.groupValues[1], contentId)
        }

        return parseEpisodesArray(match.groupValues[1], contentId)
    }

    private fun parseEpisodesArray(arrayContent: String, contentId: String): List<Episode> {
        val episodes = mutableListOf<Episode>()

        if (arrayContent.isBlank()) return episodes

        try {
            val jsonArray = JSONArray("[$arrayContent]")
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.optJSONObject(i) ?: continue
                val slug = obj.optString("slug", "")
                val title = obj.optString("title", "")
                val epNum = obj.optInt("episodeNumber", 0)
                val epId = obj.optInt("id", 0)

                if (slug.isBlank()) continue

                val epUrl = if (slug.endsWith("-subtitle-indonesia")) slug else "$slug-subtitle-indonesia"
                val displayTitle = title.ifBlank { "Episode $epNum" }

                episodes.add(
                    Episode(
                        id = epUrl,
                        contentId = contentId,
                        season = 1,
                        number = epNum,
                        title = displayTitle,
                        description = "",
                        thumbnail = "",
                        duration = 0L
                    )
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse episodes JSON: ${e.message}")

            val entryPattern = Regex("\\{\"id\":(\\d+),\"slug\":\"([^\"]+)\",\"title\":\"([^\"]*)\",\"episodeNumber\":(\\d+)")
            for (entry in entryPattern.findAll(arrayContent)) {
                val slug = entry.groupValues[2]
                val title = entry.groupValues[3]
                val epNum = entry.groupValues[4].toIntOrNull() ?: 0

                if (slug.isBlank()) continue

                val epUrl = if (slug.endsWith("-subtitle-indonesia")) slug else "$slug-subtitle-indonesia"
                val displayTitle = title.ifBlank { "Episode $epNum" }

                episodes.add(
                    Episode(
                        id = epUrl,
                        contentId = contentId,
                        season = 1,
                        number = epNum,
                        title = displayTitle,
                        description = "",
                        thumbnail = "",
                        duration = 0L
                    )
                )
            }
        }

        return episodes
    }
}
