package com.proflix.provider.data.provider

import android.util.Log
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
    private val okHttpClient: OkHttpClient,
    private val embedVideoExtractor: EmbedVideoExtractor
) : Provider {

    companion object {
        @Volatile var BASE_URL = "https://oploverz.site"
        private const val API_BASE = "https://backapi.oploverz.ac/api"
        private const val TAG = "OploverzProvider"
        private val EPISODE_NUM_REGEX = Regex("/episode/(\\d+)")
        private val BLOGGER_TOKEN_REGEX = Regex("""token=([^&"'\\s]+)""")
        private val STREAM_URL_PATTERN = Regex("""(?:streamUrl|stream_url|videoUrl|video_url)\s*[:=]\s*['"]?(https?://[^'";\s]+)['"]?""")
        private val EMBED_URL_PATTERN = Regex("""['"]?(https?://(?:blogger\.com/video|filedon\.co|upbolt\.to|4meplayer|abyssplayer|turboviplay|etvp\.cc|sptvp\.com|embedwish|sumpiernos|vcdn2\.mystream)[^'"\s<>]*)['"]?""")
    }

    override suspend fun getHome(): Result<HomeContent> = withContext(Dispatchers.IO) {
        safeCall {
            val trending = fetchTrending()
            val latest = fetchLatestEpisodes()

            HomeContent(
                heroContent = trending.firstOrNull() ?: latest.firstOrNull(),
                trending = trending,
                latest = latest,
                continueWatching = emptyList(),
                categories = buildMap {
                    if (trending.isNotEmpty()) put("Trending", trending)
                    if (latest.isNotEmpty()) put("Latest Episodes", latest)
                }
            )
        }
    }

    override suspend fun getTrending(): Result<List<Content>> = withContext(Dispatchers.IO) {
        safeCall { fetchTrending() }
    }

    override suspend fun getLatest(): Result<List<Content>> = withContext(Dispatchers.IO) {
        safeCall { fetchLatestEpisodes() }
    }

    override suspend fun getDetail(id: String): Result<Content> = withContext(Dispatchers.IO) {
        safeCall {
            val json = fetchApiJson("$API_BASE/series/$id")
                ?: throw Exception("Failed to fetch series detail")

            val data = json.optJSONObject("data") ?: json

            val title = data.optString("title", "").ifBlank { id }
            val poster = data.optString("poster", "")
            val description = data.optString("description", "")
            val score = data.opt("score")?.toString() ?: ""

            val genres = mutableListOf<String>()
            data.optJSONArray("genres")?.let { arr ->
                for (i in 0 until arr.length()) {
                    arr.optJSONObject(i)?.optString("name")?.let { genres.add(it) }
                }
            }

            val seasonObj = data.optJSONObject("season")
            val year = seasonObj?.optString("name", "") ?: ""

            Content(
                id = id,
                title = title,
                poster = poster,
                banner = poster,
                description = description,
                genres = genres,
                year = year,
                rating = score,
                type = ContentType.ANIME
            )
        }
    }

    override suspend fun getEpisodes(contentId: String): Result<List<Episode>> = withContext(Dispatchers.IO) {
        safeCall {
            val episodes = mutableListOf<Episode>()
            val html = fetchHtml("$BASE_URL/series/$contentId")
            val doc = org.jsoup.Jsoup.parse(html, "$BASE_URL/series/$contentId")

            doc.select("a[href*=\"/episode/\"]").forEach { a ->
                val href = a.attr("href").trim()
                if (!href.contains("/series/$contentId/episode/")) return@forEach
                val epNum = EPISODE_NUM_REGEX.find(href)?.groupValues?.get(1)?.toIntOrNull()
                    ?: return@forEach

                if (episodes.any { it.number == epNum }) return@forEach

                val epTitle = "Episode $epNum"
                val epId = "$contentId/episode/$epNum"

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

            episodes.sortedBy { it.number }
        }
    }

    override suspend fun getStream(episodeId: String): Result<List<StreamSource>> = withContext(Dispatchers.IO) {
        safeCall {
            val sources = mutableListOf<StreamSource>()
            val episodeUrl = if (episodeId.startsWith("http")) episodeId else "$BASE_URL/series/$episodeId"

            Log.d(TAG, "Loading streams from: $episodeUrl")
            val html = fetchHtml(episodeUrl)
            val doc = org.jsoup.Jsoup.parse(html, episodeUrl)

            val embedUrls = extractEmbedUrlsFromPage(html, doc)
            Log.d(TAG, "Found ${embedUrls.size} embed URLs")

            for ((rawUrl, quality) in embedUrls) {
                try {
                    val result = resolveUrl(rawUrl, quality)
                    if (result != null) {
                        sources.add(
                            StreamSource(
                                url = result.url,
                                quality = result.quality,
                                format = result.format
                            )
                        )
                        Log.d(TAG, "Resolved stream: ${result.quality} -> ${result.url.take(80)}...")
                    } else {
                        Log.w(TAG, "Failed to resolve: $rawUrl")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error resolving stream $rawUrl: ${e.message}")
                }
            }

            if (sources.isEmpty()) {
                Log.w(TAG, "No streams resolved from page data, trying iframe fallback")
                for (iframe in doc.select("iframe[src]")) {
                    val src = iframe.attr("abs:src")
                    if (src.isNotBlank() && !src.contains("about:blank")) {
                        try {
                            val result = resolveUrl(src, "default")
                            if (result != null) {
                                sources.add(
                                    StreamSource(
                                        url = result.url,
                                        quality = result.quality,
                                        format = result.format
                                    )
                                )
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

            try {
                val json = fetchApiJson("$API_BASE/series?page=1&limit=100&search=${java.net.URLEncoder.encode(query, "UTF-8")}")
                val dataArr = json?.optJSONArray("data") ?: json?.optJSONObject("data")?.optJSONArray("data")

                dataArr?.let { arr ->
                    for (i in 0 until arr.length()) {
                        val item = arr.optJSONObject(i) ?: continue
                        val slug = item.optString("slug", "")
                        val title = item.optString("title", "")
                        val poster = item.optString("poster", "")

                        if (title.isNotBlank() && slug.isNotBlank() && slug !in seen) {
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
            } catch (e: Exception) {
                Log.w(TAG, "API search failed: ${e.message}")
            }

            if (results.isEmpty()) {
                try {
                    val html = fetchHtml("$BASE_URL/series?q=${java.net.URLEncoder.encode(query, "UTF-8")}")
                    val doc = org.jsoup.Jsoup.parse(html, "$BASE_URL/series")

                    for (a in doc.select("a[href^=\"/series/\"]")) {
                        val href = a.attr("href").trim()
                        val slug = href.removePrefix("/series/").trim()
                        if (slug.isBlank() || slug.contains("/") || slug in seen) continue
                        val title = a.text().trim()
                        if (title.isNotBlank() && title.contains(query, ignoreCase = true) || slug.contains(query, ignoreCase = true)) {
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
                } catch (_: Exception) {}
            }

            results
        }
    }

    override suspend fun getProviderName(): String = "Oploverz"

    private suspend fun resolveUrl(rawUrl: String, quality: String): StreamSource? {
        if (EmbedVideoExtractor.isDirectVideoUrl(rawUrl)) {
            val fmt = EmbedVideoExtractor.detectFormat(rawUrl)
            return StreamSource(url = rawUrl, quality = quality, format = fmt.extension)
        }

        try {
            val result = embedVideoExtractor.extract(rawUrl)
            if (result != null) {
                return StreamSource(url = result.url, quality = quality, format = result.format.extension)
            }
        } catch (e: Exception) {
            Log.w(TAG, "embedVideoExtractor failed for $rawUrl: ${e.message}")
        }

        try {
            val html = fetchHtml(rawUrl)
            val doc = org.jsoup.Jsoup.parse(html, rawUrl)
            for (iframe in doc.select("iframe[src]")) {
                val src = iframe.attr("abs:src")
                if (src.isNotBlank() && !src.contains("about:blank")) {
                    if (EmbedVideoExtractor.isDirectVideoUrl(src)) {
                        val fmt = EmbedVideoExtractor.detectFormat(src)
                        return StreamSource(url = src, quality = quality, format = fmt.extension)
                    }
                    val inner = embedVideoExtractor.extract(src)
                    if (inner != null) {
                        return StreamSource(url = inner.url, quality = quality, format = inner.format.extension)
                    }
                }
            }

            val directUrl = extractDirectVideoFromHtml(html)
            if (directUrl != null) {
                val fmt = EmbedVideoExtractor.detectFormat(directUrl)
                return StreamSource(url = directUrl, quality = quality, format = fmt.extension)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Fallback resolution failed for $rawUrl: ${e.message}")
        }

        return null
    }

    private fun extractDirectVideoFromHtml(html: String): String? {
        val patterns = listOf(
            Regex("""(?:file|source|src|videoUrl|url)\s*[:=]\s*['"]?(https?://[^'";\s]+\.(?:mp4|m3u8|mpd)(?:\?[^'"'\s]*)?)['"]?"""),
            Regex("""['"](?:file|source|src|videoUrl|url)['"]\s*:\s*['"]?(https?://[^'";\s]+\.(?:mp4|m3u8|mpd)(?:\?[^'"'\s]*)?)['"]?"""),
            Regex("""['"]?sources['"]?\s*:\s*\[\s*\{[^}]*['"]?file['"]?\s*:\s*['"]?(https?://[^'";\s]+)['"]?"""),
        )
        for (pattern in patterns) {
            val match = pattern.find(html) ?: continue
            val url = match.groupValues.getOrNull(1)?.trim() ?: continue
            if (url.isNotBlank() && url.startsWith("http")) return url
        }
        return null
    }

    private fun extractEmbedUrlsFromPage(html: String, doc: org.jsoup.nodes.Document): List<Pair<String, String>> {
        val results = mutableListOf<Pair<String, String>>()
        val seen = mutableSetOf<String>()

        for (match in EMBED_URL_PATTERN.findAll(html)) {
            var url = match.groupValues[1].trim()
            url = url.replace("\\u003d", "=").replace("\\u0026", "&").replace("\\/", "/")
            url = url.trimEnd(',', ';', ')', ']', '"', '\'')
            if (url.isNotBlank() && url !in seen && url.startsWith("http")) {
                seen.add(url)
                val quality = if (url.contains("blogger.com")) "Blogger" else "embed"
                results.add(url to quality)
            }
        }

        for (match in STREAM_URL_PATTERN.findAll(html)) {
            var url = match.groupValues[1].trim()
            url = url.replace("\\u003d", "=").replace("\\u0026", "&").replace("\\/", "/")
            url = url.trimEnd(',', ';', ')', ']', '"', '\'')
            if (url.isNotBlank() && url !in seen && url.startsWith("http")) {
                seen.add(url)
                results.add(url to "default")
            }
        }

        for (iframe in doc.select("iframe[src]")) {
            val src = iframe.attr("abs:src")
            if (src.isNotBlank() && !src.contains("about:blank") && src !in seen) {
                seen.add(src)
                results.add(src to "iframe")
            }
        }

        return results
    }

    private fun fetchTrending(): List<Content> {
        val contents = mutableListOf<Content>()
        try {
            val json = fetchApiJson("$API_BASE/series?page=1&limit=20")
            val data = json?.optJSONArray("data") ?: return contents

            for (i in 0 until data.length()) {
                val item = data.optJSONObject(i) ?: continue
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
        } catch (e: Exception) {
            Log.w(TAG, "fetchTrending failed: ${e.message}")
        }
        return contents
    }

    private fun fetchLatestEpisodes(): List<Content> {
        val contents = mutableListOf<Content>()
        try {
            val json = fetchApiJson("$API_BASE/episodes?page=1&limit=20")
            val data = json?.optJSONArray("data") ?: return contents

            for (i in 0 until data.length()) {
                val ep = data.optJSONObject(i) ?: continue
                val epNum = ep.optString("episodeNumber", "0").toIntOrNull() ?: 0
                val series = ep.optJSONObject("series") ?: continue
                val slug = series.optString("slug", "")
                val seriesTitle = series.optString("title", "")
                val poster = series.optString("poster", "")

                if (seriesTitle.isNotBlank() && slug.isNotBlank()) {
                    contents.add(
                        Content(
                            id = slug,
                            title = "$seriesTitle - Episode $epNum",
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
        } catch (e: Exception) {
            Log.w(TAG, "fetchLatestEpisodes failed: ${e.message}")
        }
        return contents
    }

    private fun fetchApiJson(endpoint: String): JSONObject? {
        return try {
            val request = Request.Builder()
                .url(endpoint)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
                .addHeader("Accept", "application/json")
                .addHeader("Referer", "$BASE_URL/")
                .build()

        val response = okHttpClient.newCall(request).execute()
        val body = response.use {
            if (!it.isSuccessful) {
                Log.w(TAG, "fetchApiJson HTTP ${it.code} from $endpoint")
                return null
            }
            it.body?.string()
        } ?: return null

        if (body.isBlank()) return null
            JSONObject(body)
        } catch (e: Exception) {
            Log.w(TAG, "fetchApiJson failed for $endpoint: ${e.message}")
            null
        }
    }

    private fun fetchHtml(url: String): String {
        val request = Request.Builder()
            .url(url)
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
            .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
            .addHeader("Accept-Language", "id-ID,id;q=0.9,en-US;q=0.8,en;q=0.7")
            .addHeader("Sec-Fetch-Dest", "document")
            .addHeader("Sec-Fetch-Mode", "navigate")
            .addHeader("Sec-Fetch-Site", "none")
            .addHeader("Upgrade-Insecure-Requests", "1")
            .build()

        val response = okHttpClient.newCall(request).execute()
        response.use {
            if (!it.isSuccessful) {
                throw Exception("HTTP ${it.code} from $url")
            }
            return it.body?.string() ?: throw Exception("Empty response from $url")
        }
    }
}
