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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OploverzProvider @Inject constructor(
    private val okHttpClient: OkHttpClient
) : Provider {

    private val embedExtractor = EmbedVideoExtractor(okHttpClient)

    companion object {
        var BASE_URL = "https://oploverz.site"
    }

    override suspend fun getHome(): Result<HomeContent> = withContext(Dispatchers.IO) {
        safeCall {
            val html = fetchHtml(BASE_URL)
            val pageData = extractPageData(html)

            val trending = parseTrending(pageData)
            val latest = parseLatestEpisodes(pageData)
            val recentlyAdded = parseRecentlyAdded(pageData)
            val heroContent = parseFeatured(pageData)

            HomeContent(
                heroContent = heroContent ?: trending.firstOrNull() ?: latest.firstOrNull(),
                trending = trending,
                latest = latest,
                continueWatching = emptyList(),
                categories = mapOf(
                    "Trending" to trending,
                    "Latest Episodes" to latest,
                    "Recently Added" to recentlyAdded
                )
            )
        }
    }

    override suspend fun getTrending(): Result<List<Content>> = withContext(Dispatchers.IO) {
        safeCall {
            val html = fetchHtml(BASE_URL)
            val pageData = extractPageData(html)
            parseTrending(pageData)
        }
    }

    override suspend fun getLatest(): Result<List<Content>> = withContext(Dispatchers.IO) {
        safeCall {
            val html = fetchHtml(BASE_URL)
            val pageData = extractPageData(html)
            parseLatestEpisodes(pageData)
        }
    }

    override suspend fun getDetail(id: String): Result<Content> = withContext(Dispatchers.IO) {
        safeCall {
            val html = fetchHtml("$BASE_URL/series/$id")
            val pageData = extractPageData(html)

            val series = pageData?.optJSONObject("series")
                ?: pageData?.optJSONObject("anime")

            val title = series?.optString("title", "")?.ifBlank { id } ?: id
            val poster = series?.optString("poster", "") ?: ""
            val description = series?.optString("description", "") ?: ""

            val genres = mutableListOf<String>()
            series?.optJSONArray("genres")?.let { arr ->
                for (i in 0 until arr.length()) {
                    arr.optJSONObject(i)?.optString("name")?.let { genres.add(it) }
                }
            }

            val score = series?.opt("score")?.toString() ?: ""
            val seasonObj = series?.optJSONObject("season")
            val season = seasonObj?.optString("name", "") ?: ""

            Content(
                id = id,
                title = title,
                poster = poster,
                banner = poster,
                description = description,
                genres = genres,
                year = season,
                rating = score,
                type = ContentType.ANIME
            )
        }
    }

    override suspend fun getEpisodes(contentId: String): Result<List<Episode>> = withContext(Dispatchers.IO) {
        safeCall {
            val html = fetchHtml("$BASE_URL/series/$contentId")
            val episodes = mutableListOf<Episode>()

            val htmlEpisodes = parseEpisodesFromHtml(html, contentId)
            if (htmlEpisodes.isNotEmpty()) {
                return@safeCall htmlEpisodes
            }

            val pageData = extractPageData(html)
            val episodesArr = pageData?.optJSONObject("episodes")?.optJSONArray("data")
                ?: pageData?.optJSONArray("episodes")

            episodesArr?.let { arr ->
                for (i in 0 until arr.length()) {
                    val ep = arr.optJSONObject(i) ?: continue
                    val epNum = ep.optString("episodeNumber", "${i + 1}").toIntOrNull() ?: (i + 1)
                    val rawTitle = ep.optString("title", "")
                    val epTitle = if (!rawTitle.isNullOrBlank() && rawTitle != "null") {
                        rawTitle
                    } else {
                        "Episode $epNum"
                    }
                    val series = ep.optJSONObject("series")
                    val seriesSlug = series?.optString("slug", "") ?: contentId
                    val epId = "$seriesSlug/episode/$epNum"

                    episodes.add(
                        Episode(
                            id = epId,
                            contentId = contentId,
                            season = 1,
                            number = epNum,
                            title = epTitle,
                            description = "",
                            thumbnail = "",
                            duration = 0L
                        )
                    )
                }
            }

            episodes
        }
    }

    override suspend fun getStream(episodeId: String): Result<List<StreamSource>> = withContext(Dispatchers.IO) {
        safeCall {
            val sources = mutableListOf<StreamSource>()

            val episodeUrl = if (episodeId.startsWith("http")) episodeId else "$BASE_URL/series/$episodeId"
            val html = fetchHtml(episodeUrl)
            val pageData = extractPageData(html)

            val rawUrls = mutableListOf<Pair<String, String>>()

            val epNum = Regex("episode/(\\d+)").find(episodeId)?.groupValues?.get(1)

            val episodesArr = pageData?.optJSONObject("episodes")?.optJSONArray("data")
                ?: pageData?.optJSONArray("episodes")

            if (episodesArr != null) {
                for (i in 0 until episodesArr.length()) {
                    val ep = episodesArr.optJSONObject(i) ?: continue
                    if (epNum != null && ep.optString("episodeNumber") != epNum) continue
                    ep.optJSONArray("streamUrl")?.let { arr ->
                        for (j in 0 until arr.length()) {
                            val stream = arr.optJSONObject(j) ?: continue
                            val url = stream.optString("url", "")
                            val quality = stream.optString("source", "default")
                            if (url.isNotBlank()) {
                                rawUrls.add(url to quality)
                            }
                        }
                    }
                    if (rawUrls.isNotEmpty()) break
                }
            }

            if (rawUrls.isEmpty()) {
                val doc = org.jsoup.Jsoup.parse(html, episodeUrl)
                for (iframe in doc.select("iframe[src]")) {
                    val url = iframe.attr("abs:src")
                    if (url.isNotBlank() && !url.contains("about:blank")) {
                        rawUrls.add(url to "default")
                    }
                }
            }

            for ((rawUrl, quality) in rawUrls) {
                if (EmbedVideoExtractor.isDirectVideoUrl(rawUrl)) {
                    val format = EmbedVideoExtractor.detectFormat(rawUrl)
                    sources.add(StreamSource(url = rawUrl, quality = quality, format = format.extension))
                } else {
                    val result = embedExtractor.extract(rawUrl)
                    if (result != null) {
                        sources.add(StreamSource(url = result.url, quality = quality, format = result.format.extension))
                    } else {
                        try {
                            val innerHtml = fetchHtml(rawUrl)
                            val doc = org.jsoup.Jsoup.parse(innerHtml, rawUrl)
                            for (iframe in doc.select("iframe[src]")) {
                                val innerUrl = iframe.attr("abs:src")
                                if (innerUrl.isNotBlank() && !innerUrl.contains("about:blank")) {
                                    if (EmbedVideoExtractor.isDirectVideoUrl(innerUrl)) {
                                        val format = EmbedVideoExtractor.detectFormat(innerUrl)
                                        sources.add(StreamSource(url = innerUrl, quality = quality, format = format.extension))
                                    } else {
                                        val innerResult = embedExtractor.extract(innerUrl)
                                        if (innerResult != null) {
                                            sources.add(StreamSource(url = innerResult.url, quality = quality, format = innerResult.format.extension))
                                        }
                                    }
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }
            }

            sources
        }
    }

    override suspend fun search(query: String): Result<List<Content>> = withContext(Dispatchers.IO) {
        safeCall {
            val results = mutableListOf<Content>()
            val seen = mutableSetOf<String>()
            val html = fetchHtml("$BASE_URL/series")
            val doc = org.jsoup.Jsoup.parse(html, "$BASE_URL/series")

            for (a in doc.select("a[href^=\"/series/\"]")) {
                val href = a.attr("href").trim()
                val slug = href.removePrefix("/series/").trim()
                if (slug.isBlank() || slug.contains("/") || slug in seen) continue
                val title = a.text().trim()
                if (title.isNotBlank() && title.contains(query, ignoreCase = true)) {
                    seen.add(slug)
                    results.add(
                        Content(
                            id = slug,
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

            if (results.isEmpty()) {
                val pageData = extractPageData(html)
                val seriesArr = pageData?.optJSONObject("series")?.optJSONArray("data")
                    ?: pageData?.optJSONArray("series")
                    ?: pageData?.optJSONObject("search")?.optJSONArray("data")

                seriesArr?.let { arr ->
                    for (i in 0 until arr.length()) {
                        val item = arr.optJSONObject(i) ?: continue
                        val slug = item.optString("slug", "")
                        val title = item.optString("title", "")
                        val poster = item.optString("poster", "")

                        if (title.isNotBlank() && slug.isNotBlank() && slug !in seen && title.contains(query, ignoreCase = true)) {
                            seen.add(slug)
                            results.add(
                                Content(
                                    id = slug,
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
                }
            }

            results
        }
    }

    override suspend fun getProviderName(): String = "Oploverz"

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

    private fun extractPageData(html: String): JSONObject? {
        return try {
            val mergedData = JSONObject()

            val kitData = extractSvelteKitData(html)
            if (kitData != null) {
                mergeJsonObjects(mergedData, kitData)
            }

            val constPattern = Regex(
                """<script[^>]*>\s*(?:const|let|var)\s+data\s*=\s*(\{[\s\S]*?\})\s*;?\s*</script>""",
                RegexOption.DOT_MATCHES_ALL
            )
            constPattern.find(html)?.let { match ->
                try {
                    mergeJsonObjects(mergedData, JSONObject(match.groupValues[1]))
                } catch (_: Exception) {}
            }

            if (mergedData.length() == 0) null else mergedData
        } catch (_: Exception) {
            null
        }
    }

    private fun extractSvelteKitData(html: String): JSONObject? {
        try {
            val nodeIdsIdx = html.indexOf("node_ids:")
            if (nodeIdsIdx == -1) return null

            val dataIdx = html.indexOf("data:", nodeIdsIdx)
            if (dataIdx == -1) return null

            val bracketStart = html.indexOf("[", dataIdx)
            if (bracketStart == -1 || bracketStart - dataIdx > 20) return null

            val dataStr = extractBalancedBrackets(html, bracketStart, '[', ']') ?: return null

            try {
                val arr = JSONArray(dataStr)
                return mergeKitArray(arr)
            } catch (_: Exception) {}

            try {
                val jsonStr = convertJsKeysToJson(dataStr)
                val arr = JSONArray(jsonStr)
                return mergeKitArray(arr)
            } catch (_: Exception) {}

            return null
        } catch (_: Exception) {
            return null
        }
    }

    private fun mergeKitArray(arr: JSONArray): JSONObject {
        val result = JSONObject()
        for (i in 0 until arr.length()) {
            val item = arr.optJSONObject(i) ?: continue
            if (item.optString("type") == "data") {
                val data = item.optJSONObject("data") ?: continue
                mergeJsonObjects(result, data)
            }
        }
        return result
    }

    private fun extractBalancedBrackets(html: String, start: Int, open: Char, close: Char): String? {
        if (start >= html.length || html[start] != open) return null
        var depth = 0
        var inStr = false
        var esc = false
        for (i in start until html.length) {
            val c = html[i]
            when {
                esc -> esc = false
                c == '\\' && inStr -> esc = true
                c == '"' -> inStr = !inStr
                !inStr -> {
                    if (c == open) depth++
                    else if (c == close) {
                        depth--
                        if (depth == 0) return html.substring(start, i + 1)
                    }
                }
            }
        }
        return null
    }

    private fun convertJsKeysToJson(js: String): String {
        return js.replace(Regex("""(?<=[\{,]\s*)(\w+)(\s*:)""")) {
            "\"${it.groupValues[1]}\"${it.groupValues[2]}"
        }
    }

    private fun mergeJsonObjects(target: JSONObject, source: JSONObject) {
        val keys = source.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            try {
                target.put(key, source.get(key))
            } catch (_: Exception) {}
        }
    }

    private fun parseTrending(pageData: JSONObject?): List<Content> {
        val contents = mutableListOf<Content>()
        val trendingData = pageData?.optJSONObject("trending")?.optJSONArray("data") ?: return contents

        for (i in 0 until trendingData.length()) {
            val item = trendingData.optJSONObject(i) ?: continue
            val slug = item.optString("slug", "")
            val title = item.optString("title", "")
            val poster = item.optString("poster", "")
            val description = item.optString("description", "")
            val score = item.opt("score")?.toString() ?: ""

            val genres = mutableListOf<String>()
            item.optJSONArray("genres")?.let { arr ->
                for (j in 0 until arr.length()) {
                    arr.optJSONObject(j)?.optString("name")?.let { genres.add(it) }
                }
            }

            if (title.isNotBlank() && slug.isNotBlank()) {
                contents.add(
                    Content(
                        id = slug,
                        title = title,
                        poster = poster,
                        banner = poster,
                        description = description,
                        genres = genres,
                        year = "",
                        rating = score,
                        type = ContentType.ANIME
                    )
                )
            }
        }

        return contents
    }

    private fun parseLatestEpisodes(pageData: JSONObject?): List<Content> {
        val contents = mutableListOf<Content>()
        val episodesData = pageData?.optJSONObject("latestEpisodes")?.optJSONArray("data") ?: return contents

        for (i in 0 until episodesData.length()) {
            val ep = episodesData.optJSONObject(i) ?: continue
            val epTitle = ep.optString("title", "")?.ifBlank { null }
            val epNum = ep.optString("episodeNumber", "0").toIntOrNull() ?: 0
            val series = ep.optJSONObject("series")
            val slug = series?.optString("slug", "") ?: ""
            val seriesTitle = series?.optString("title", "") ?: ""
            val poster = series?.optString("poster", "") ?: ""
            val id = if (slug.isNotBlank()) slug else seriesTitle.lowercase().replace(" ", "-")
            val displayTitle = epTitle ?: seriesTitle

            if (displayTitle.isNotBlank()) {
                contents.add(
                    Content(
                        id = id,
                        title = "$displayTitle - Episode $epNum",
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

    private fun parseRecentlyAdded(pageData: JSONObject?): List<Content> {
        val contents = mutableListOf<Content>()
        val recentlyData = pageData?.optJSONObject("recently")?.optJSONArray("data") ?: return contents

        for (i in 0 until recentlyData.length()) {
            val item = recentlyData.optJSONObject(i) ?: continue
            val slug = item.optString("slug", "")
            val title = item.optString("title", "")
            val poster = item.optString("poster", "")

            if (title.isNotBlank() && slug.isNotBlank()) {
                contents.add(
                    Content(
                        id = slug,
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

    private fun parseEpisodesFromHtml(html: String, contentId: String): List<Episode> {
        val episodes = mutableListOf<Episode>()
        val doc = org.jsoup.Jsoup.parse(html, "$BASE_URL/series/$contentId")
        val seen = mutableSetOf<Int>()

        doc.select("a[href*=\"/episode/\"]").forEach { a ->
            val href = a.attr("href").trim()
            val epNum = Regex("/episode/(\\d+)").find(href)?.groupValues?.get(1)?.toIntOrNull() ?: return@forEach
            if (epNum in seen) return@forEach
            seen.add(epNum)

            val title = a.selectFirst("p")?.text()?.trim()?.ifBlank { null }
                ?: "Episode $epNum"
            val displayTitle = if (title.startsWith("Episode $epNum")) title else "Episode $epNum"

            episodes.add(
                Episode(
                    id = "$contentId/episode/$epNum",
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

        return episodes.sortedBy { it.number }
    }

    private fun parseFeatured(pageData: JSONObject?): Content? {
        val featured = pageData?.optJSONArray("featuredSeries") ?: return null
        if (featured.length() == 0) return null

        val item = featured.optJSONObject(0) ?: return null
        val banner = item.optString("banner", "")
        val series = item.optJSONObject("series") ?: return null

        val slug = series.optString("slug", "")
        val title = series.optString("title", "")
        val poster = series.optString("poster", "")
        val description = series.optString("description", "")

        val genres = mutableListOf<String>()
        series.optJSONArray("genres")?.let { arr ->
            for (j in 0 until arr.length()) {
                arr.optJSONObject(j)?.optString("name")?.let { genres.add(it) }
            }
        }

        return if (title.isNotBlank() && slug.isNotBlank()) {
            Content(
                id = slug,
                title = title,
                poster = poster,
                banner = banner.ifBlank { poster },
                description = description,
                genres = genres,
                year = "",
                rating = series.opt("score")?.toString() ?: "",
                type = ContentType.ANIME
            )
        } else null
    }
}
