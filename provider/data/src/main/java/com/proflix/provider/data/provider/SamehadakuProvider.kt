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

@Singleton
class SamehadakuProvider @Inject constructor(
    private val okHttpClient: OkHttpClient
) : Provider {

    private val embedExtractor = EmbedVideoExtractor(okHttpClient)

    companion object {
        var BASE_URL = "https://x6.sokuja.uk"
    }

    override suspend fun getHome(): Result<HomeContent> = withContext(Dispatchers.IO) {
        safeCall {
            val html = fetchHtml(BASE_URL)
            val doc = Jsoup.parse(html, BASE_URL)

            val trending = parseSidebarPopular(doc)
            val latest = parseRscLatest(html, doc)

            HomeContent(
                heroContent = parseHeroFromRsc(html, doc),
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
            val doc = Jsoup.parse(html, BASE_URL)
            parseRscLatest(html, doc)
        }
    }

    override suspend fun getDetail(id: String): Result<Content> = withContext(Dispatchers.IO) {
        safeCall {
            val url = if (id.startsWith("http")) id else "$BASE_URL/$id"
            val html = fetchHtml(url)
            val doc = Jsoup.parse(html, url)

            val ldJson = extractLdJson(doc)
            val tvSeries = ldJson?.optJSONObject("tvSeries") ?: ldJson

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
            if (year.isBlank()) {
                for (el in doc.select("dl div, dt, dd")) {
                    val text = el.text()
                    when {
                        text.contains("Tahun") || text.contains("Year") ->
                            year = text.substringAfter(":").trim().ifBlank { year }
                    }
                }
            }
            val ratingObj = tvSeries?.optJSONObject("aggregateRating")
            rating = ratingObj?.opt("ratingValue")?.toString() ?: rating

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
        return response.body?.string() ?: throw Exception("Empty response")
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
        val patterns = listOf(
            Regex("""\"episodeNumber\"\s*:\s*(\d+)"""),
            Regex("""\"episodeNumber\":\s*(\d+)""")
        )
        for (pattern in patterns) {
            pattern.find(html)?.groupValues?.get(1)?.toIntOrNull()?.let { return it }
        }
        return null
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
            val body = response.body?.string() ?: return sources
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
                val embedUrl = mirror.optString("embedUrl", "")
                val quality = mirror.optString("quality", mirror.optString("serverName", "default"))

                if (embedUrl.isNotBlank()) {
                    val format = EmbedVideoExtractor.detectFormat(embedUrl)
                    sources.add(
                        StreamSource(
                            url = embedUrl,
                            quality = quality,
                            format = format.extension
                        )
                    )
                }
            }
        } catch (_: Exception) {}

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

                if (title.isNotBlank()) {
                    return Content(
                        id = id,
                        title = title,
                        poster = poster,
                        banner = poster,
                        description = desc,
                        genres = emptyList(),
                        year = "",
                        rating = "",
                        type = ContentType.ANIME
                    )
                }
            }
        }

        val titlePattern = Regex("""\"name\"\s*:\s*\"([^\"]+)\"""")
        val descPattern = Regex("""\"description\"\s*:\s*\"([^\"]{10,200})\"""")
        val imgPattern = Regex("""\"image\"\s*:\s*\"([^\"]+)\"""")
        val ratingPattern = Regex("""\"ratingValue\"\s*:\s*\"?(\d+\.?\d*)\"?""")

        val title = titlePattern.find(html)?.groupValues?.get(1) ?: return null
        val desc = descPattern.find(html)?.groupValues?.get(1) ?: ""
        val img = imgPattern.find(html)?.groupValues?.get(1) ?: ""
        val rating = ratingPattern.find(html)?.groupValues?.get(1) ?: ""

        val genres = mutableListOf<String>()
        for (a in doc.select("a[href*=\"/genre/\"]")) {
            val g = a.text().trim()
            if (g.isNotBlank() && g !in genres) genres.add(g)
        }

        return Content(
            id = "",
            title = title,
            poster = img,
            banner = img,
            description = desc,
            genres = genres.take(5),
            year = "",
            rating = rating,
            type = ContentType.ANIME
        )
    }

    private fun parseRscLatest(html: String, doc: Document): List<Content> {
        val contents = mutableListOf<Content>()
        val seen = mutableSetOf<String>()

        for (a in doc.select("a[href*=\"-episode-\"][href*=\"-subtitle-indonesia\"]")) {
            val href = a.attr("href").trim()
            val slug = href.removePrefix(BASE_URL).trimStart('/')
            if (slug.isBlank() || slug in seen) continue

            val animeSlug = Regex("(.+?)-episode-\\d+").find(slug)?.groupValues?.get(1) ?: continue
            if (animeSlug.isBlank() || animeSlug in seen) continue

            seen.add(animeSlug)
            seen.add(slug)

            val epNum = Regex("episode-(\\d+)").find(slug)?.groupValues?.get(1)?.toIntOrNull()

            val epTitle = a.selectFirst(".text-gray-200, .font-medium")?.text()?.trim()
                ?: a.text().trim().ifBlank { null }

            val displayTitle = if (!epTitle.isNullOrBlank() && !epTitle.contains("Episode", ignoreCase = true)) {
                epTitle
            } else {
                animeSlug.replace("-", " ").replaceFirstChar { it.uppercase() }
            }

            val poster = getImageUrl(a.selectFirst("img")) ?: ""

            contents.add(
                Content(
                    id = animeSlug,
                    title = displayTitle,
                    poster = poster,
                    banner = poster,
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
            val epPattern = Regex("\"([^\"]+)-episode-(\\d+)-subtitle-indonesia\"")
            for (match in epPattern.findAll(html)) {
                val animeSlug = match.groupValues[1]
                if (animeSlug !in seen) {
                    seen.add(animeSlug)
                    val title = animeSlug.replace("-", " ").replaceFirstChar { it.uppercase() }
                    contents.add(
                        Content(
                            id = animeSlug,
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
                }
            }
        }

        return contents.take(20)
    }
}
