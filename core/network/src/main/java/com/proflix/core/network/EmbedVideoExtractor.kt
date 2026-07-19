package com.proflix.core.network

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLDecoder

class EmbedVideoExtractor(
    private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val TAG = "EmbedVideoExtractor"

        private val BLOGGER_RPC_IDS = listOf(
            "jrwoqf", "H1TAV", "UQiIPe", "bcGtuc", "hruxee",
            "UvZcsc", "Augo5c", "vBV9id", "p6vSuf", "Ppwqme"
        )

        private val PARAM_FORMATS = listOf<(String) -> String>(
            { token -> """["$token"]""" },
            { token -> """["$token","",0]""" },
            { token -> """["$token",null]""" },
            { token -> """["$token",""]""" },
            { token -> """["${token.trim()}"]""" },
        )

        fun isDirectVideoUrl(url: String): Boolean {
            val lower = url.lowercase()
            return lower.endsWith(".mp4") ||
                    lower.endsWith(".m3u8") ||
                    lower.endsWith(".mpd") ||
                    lower.endsWith(".mkv") ||
                    lower.contains(".mp4?") ||
                    lower.contains(".m3u8?") ||
                    lower.contains(".mpd?") ||
                    lower.contains("googlevideo.com/videoplayback") ||
                    lower.contains("storages.sokuja.uk/") ||
                    lower.contains(".mp4#") ||
                    lower.contains("/videoplayback?")
        }

        fun detectFormat(url: String): VideoFormat {
            val lower = url.lowercase()
            return when {
                lower.contains(".m3u8") || lower.contains("hls") -> VideoFormat.HLS
                lower.contains(".mpd") -> VideoFormat.DASH
                lower.contains(".mkv") -> VideoFormat.MKV
                lower.endsWith(".mp4") || lower.contains(".mp4?") || lower.contains(".mp4#") -> VideoFormat.MP4
                lower.contains("googlevideo.com") -> VideoFormat.HLS
                else -> VideoFormat.MP4
            }
        }
    }

    data class VideoResult(
        val url: String,
        val format: VideoFormat
    )

    enum class VideoFormat(val extension: String) {
        MP4("mp4"),
        HLS("m3u8"),
        DASH("mpd"),
        MKV("mkv"),
        UNKNOWN("mp4")
    }

    suspend fun extract(embedUrl: String): VideoResult? {
        if (isDirectVideoUrl(embedUrl)) {
            return VideoResult(url = embedUrl, format = detectFormat(embedUrl))
        }

        if (isBloggerUrl(embedUrl)) {
            return resolveBloggerUrl(embedUrl)
        }

        return try {
            val html = fetchHtml(embedUrl, embedUrl)
            extractFromHtml(html, embedUrl)
        } catch (_: Exception) {
            null
        }
    }

    private fun resolveBloggerUrl(bloggerUrl: String): VideoResult? {
        val token = extractBloggerToken(bloggerUrl) ?: return null

        val batchResult = resolveViaBatchexecute(token)
        if (batchResult != null) return batchResult

        return try {
            val html = fetchHtml(bloggerUrl, "https://www.blogger.com/")
            extractVideoUrlFromBloggerPage(html)
        } catch (_: Exception) {
            null
        }
    }

    private fun extractBloggerToken(url: String): String? {
        val decoded = try {
            URLDecoder.decode(url, "UTF-8")
        } catch (_: Exception) {
            url
        }
        val patterns = listOf(
            Regex("""token=([^&"'\\s]+)"""),
            Regex("""token%3D([^&"'\\s]+)""", RegexOption.IGNORE_CASE),
            Regex("""token\s*=\s*['"]?([^'"&\\s]+)""")
        )
        for (pattern in patterns) {
            pattern.find(decoded)?.groupValues?.get(1)?.let { token ->
                return token.replace("\\u003d", "=").replace("\\u0026", "&")
            }
        }
        return null
    }

    private fun resolveViaBatchexecute(token: String): VideoResult? {
        val url = "https://www.blogger.com/_/BloggerVideoPlayerUi/data/batchexecute"

        for (rpcId in BLOGGER_RPC_IDS) {
            for (paramBuilder in PARAM_FORMATS) {
                try {
                    val params = paramBuilder(token)
                    val innerPayload = """${params.replace("\"", "\\\"")}"""
                    val rpcPayload = """[["$rpcId","${innerPayload}",null,"generic"]]"""
                    val formBody = "f.req=${java.net.URLEncoder.encode(rpcPayload, "UTF-8")}"

                    val request = Request.Builder()
                        .url(url)
                        .post(formBody.toRequestBody("application/x-www-form-urlencoded; charset=UTF-8".toMediaType()))
                        .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
                        .addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                        .addHeader("Referer", "https://www.blogger.com/video.g?token=${java.net.URLEncoder.encode(token, "UTF-8")}")
                        .addHeader("Origin", "https://www.blogger.com")
                        .addHeader("X-Goog-AuthUser", "0")
                        .build()

                    val response = okHttpClient.newCall(request).execute()
                    val body = response.body?.string() ?: continue

                    if (body.isBlank() || body.startsWith("<!")) continue

                    val videoUrl = parseBatchexecuteResponse(body)
                    if (videoUrl != null) {
                        Log.d(TAG, "batchexecute resolved via rpcId=$rpcId params=$params")
                        return VideoResult(url = videoUrl, format = detectFormat(videoUrl))
                    }
                } catch (_: Exception) {}
            }
        }
        return null
    }

    private fun parseBatchexecuteResponse(body: String): String? {
        val cleanBody = body.trimStart().removePrefix(")]}'").trim()

        try {
            val arr = JSONArray(cleanBody)
            for (i in 0 until arr.length()) {
                val outer = arr.optJSONArray(i) ?: continue

                for (j in 0 until outer.length()) {
                    val candidate = outer.optString(j, "")
                    if (candidate.isBlank() || candidate == "null") continue

                    val videoUrl = extractVideoUrlFromJsonString(candidate)
                    if (videoUrl != null) {
                        Log.d(TAG, "parseBatchexecuteResponse found at arr[$i][$j]")
                        return videoUrl
                    }
                }

                if (outer.length() >= 3) {
                    val dataStr = outer.optString(2, "")
                    if (dataStr.isNotBlank() && dataStr != "null") {
                        val videoUrl = extractVideoUrlFromJsonString(dataStr)
                        if (videoUrl != null) {
                            Log.d(TAG, "parseBatchexecuteResponse found at arr[$i][2]")
                            return videoUrl
                        }
                    }
                }

                if (outer.length() >= 2) {
                    val inner = outer.optJSONArray(1)
                    if (inner != null) {
                        for (k in 0 until inner.length()) {
                            val innerCandidate = inner.optString(k, "")
                            if (innerCandidate.isBlank() || innerCandidate == "null") continue
                            val videoUrl = extractVideoUrlFromJsonString(innerCandidate)
                            if (videoUrl != null) {
                                Log.d(TAG, "parseBatchexecuteResponse found at arr[$i][1][$k]")
                                return videoUrl
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "parseBatchexecuteResponse JSON parse error: ${e.message}")
        }

        val directUrl = extractGoogleVideoUrl(cleanBody)
        if (directUrl != null) {
            Log.d(TAG, "parseBatchexecuteResponse found via raw regex")
            return directUrl
        }

        return null
    }

    private fun extractVideoUrlFromJsonString(dataStr: String): String? {
        val unescaped = dataStr
            .replace("\\u003d", "=")
            .replace("\\u0026", "&")
            .replace("\\/", "/")
            .replace("\\\"", "\"")

        val videoUrl = extractGoogleVideoUrl(unescaped)
        if (videoUrl != null) return videoUrl

        try {
            val jsonData = JSONObject(unescaped)
            val fields = listOf(
                "playback_url", "playbackUrl", "video_url", "videoUrl",
                "stream_url", "streamUrl", "url", "src",
                "content_url", "contentUrl", "embedUrl"
            )
            for (field in fields) {
                val url = jsonData.optString(field, "")
                if (url.isNotBlank() && url.startsWith("http")) {
                    if (url.contains("googlevideo.com") || url.contains(".mp4") || url.contains(".m3u8") || url.contains(".mpd")) {
                        return url
                    }
                    val extracted = extractGoogleVideoUrl(url)
                    if (extracted != null) return extracted
                }
            }

            val keys = jsonData.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val value = jsonData.opt(key)
                if (value is String && value.length > 20) {
                    val nestedUrl = extractGoogleVideoUrl(value)
                    if (nestedUrl != null) return nestedUrl
                    if (value.startsWith("{")) {
                        val deepUrl = extractVideoUrlFromJsonString(value)
                        if (deepUrl != null) return deepUrl
                    }
                }
                if (value is JSONObject) {
                    val nestedUrl = extractVideoUrlFromJsonString(value.toString())
                    if (nestedUrl != null) return nestedUrl
                }
                if (value is JSONArray) {
                    for (k in 0 until value.length()) {
                        val item = value.opt(k)
                        if (item is String) {
                            val nestedUrl = extractGoogleVideoUrl(item)
                            if (nestedUrl != null) return nestedUrl
                        }
                        if (item is JSONObject) {
                            val nestedUrl = extractVideoUrlFromJsonString(item.toString())
                            if (nestedUrl != null) return nestedUrl
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        return null
    }

    private fun extractGoogleVideoUrl(text: String): String? {
        val patterns = listOf(
            Regex("""https?://[a-z0-9.-]*googlevideo\.com/videoplayback\?[^'"\s\\]+"""),
            Regex("""https?:\\?/\\?/[a-z0-9.-]*googlevideo\.com\\?/videoplayback[^"'\\]+"""),
            Regex("""https?://[a-z0-9.-]+\.googlevideo\.com/[^'"\s<>]+"""),
            Regex("""https?://[a-z0-9.-]*googlevideo\.com/videoplayback[^'"\\]+"""),
        )

        for (pattern in patterns) {
            val match = pattern.find(text) ?: continue
            var url = match.value
                .replace("\\u003d", "=")
                .replace("\\u0026", "&")
                .replace("\\/", "/")
                .replace("\\\\", "\\")
                .trimEnd('\'', '"', ',', ';', ')')

            if (url.contains("googlevideo.com/videoplayback")) {
                return url
            }
        }

        return null
    }

    private fun extractVideoUrlFromBloggerPage(html: String): VideoResult? {
        val videoUrl = extractGoogleVideoUrl(html)
        if (videoUrl != null) {
            Log.d(TAG, "extractVideoUrlFromBloggerPage found via google video regex")
            return VideoResult(url = videoUrl, format = detectFormat(videoUrl))
        }

        val urlPatterns = listOf(
            Regex("""['"]https?://[^'"]*googlevideo[^'"]*['"]"""),
            Regex("""url\s*[:=]\s*['"]?(https?://[^'";\s]+)['"]?"""),
            Regex("""src\s*[:=]\s*['"]?(https?://[^'";\s]+)['"]?"""),
            Regex("""'url'\s*:\s*'(https?://[^']+)'"""),
            Regex("""window\.__INITIAL_STATE__\s*=\s*(\{.+?\});""", RegexOption.DOT_MATCHES_ALL),
            Regex("""\"playback_url\"\s*:\s*\"(https?:\\?/\\?/[^\"\\]+)"""),
            Regex("""\"video_url\"\s*:\s*\"(https?:\\?/\\?/[^\"\\]+)"""),
            Regex("""\"stream_url\"\s*:\s*\"(https?:\\?/\\?/[^\"\\]+)"""),
            Regex("""\"(https?:\\?/\\?/[^\"\\]*googlevideo[^\"\\]*)\""""),
            Regex("""data\-p\s*=\s*['\"]([^'\"]+)['\"]"""),
            Regex("""TSDtV\s*[:=]\s*'([^']+)'"""),
            Regex("""TSDtV\s*[:=]\s*\"([^\"]+)\""""),
        )
        for (pattern in urlPatterns) {
            pattern.find(html)?.let { match ->
                var url = match.groupValues.getOrNull(1)?.trim() ?: match.value.trim()
                url = url.removeSurrounding("'").removeSurrounding("\"")
                url = url.replace("\\u003d", "=").replace("\\u0026", "&").replace("\\/", "/")
                if (url.contains("googlevideo.com") || url.contains("videoplayback")) {
                    Log.d(TAG, "extractVideoUrlFromBloggerPage found via pattern")
                    return VideoResult(url = url, format = detectFormat(url))
                }
                val nestedUrl = extractGoogleVideoUrl(url)
                if (nestedUrl != null) {
                    Log.d(TAG, "extractVideoUrlFromBloggerPage found via nested regex in matched value")
                    return VideoResult(url = nestedUrl, format = detectFormat(nestedUrl))
                }
            }
        }

        return null
    }

    private fun isBloggerUrl(url: String): Boolean {
        return url.contains("blogger.com/video.g") || url.contains("blogger.com/video?")
    }

    private fun extractFromHtml(html: String, baseUrl: String): VideoResult? {
        val patterns = listOf(
            Regex("""var\s+urlPlay\s*=\s*['"]([^'"]+)['"]"""),
            Regex("""(?:file|source|src|videoUrl)\s*[:=]\s*['"]?(https?://[^'";\s]+\.(?:mp4|m3u8|mpd)(?:\?[^'"'\s]*)?)['"]?"""),
            Regex("""['"](?:file|source|src|videoUrl)['"]\s*:\s*['"]?(https?://[^'";\s]+\.(?:mp4|m3u8|mpd)(?:\?[^'"'\s]*)?)['"]?"""),
            Regex("""<source\s+[^>]*src\s*=\s*['"]?(https?://[^'"\s>]+\.(?:mp4|m3u8|mpd)(?:\?[^'"'\s>])*)['"]?"""),
            Regex("""<video\s+[^>]*src\s*=\s*['"]?(https?://[^'"\s>]+\.(?:mp4|m3u8|mpd)(?:\?[^'"'\s>])*)['"]?"""),
            Regex("""['"]?sources['"]?\s*:\s*\[\s*\{[^}]*['"]?file['"]?\s*:\s*['"]?(https?://[^'";\s]+)['"]?"""),
            Regex("""['"]?url['"]?\s*:\s*['"]?(https?://[^'";\s]+\.(?:mp4|m3u8|mpd)(?:\?[^'"'\s]*)?)['"]?"""),
        )

        for (pattern in patterns) {
            val match = pattern.find(html) ?: continue
            val url = match.groupValues.getOrNull(1)?.trim() ?: continue
            if (url.isNotBlank() && url.startsWith("http")) {
                return VideoResult(url = url, format = detectFormat(url))
            }
        }

        val videoHosts = listOf(
            "etvp.cc", "turboviplay.com", "sptvp.com",
            "turbovidhls.com", "embedwish.com", "sumpiernos.com",
            "vcdn2.mystream.to", "filedon.co", "upbolt.to",
            "4meplayer.com", "4meplayer.pro", "abyssplayer.com"
        )
        val hostPattern = Regex("""(https?://[a-z0-9.-]*(?:${videoHosts.joinToString("|") { Regex.escape(it) }})[^\s"'<>]*)""")
        hostPattern.find(html)?.let { match ->
            val url = match.groupValues[1].trim()
            if (url.isNotBlank()) {
                return VideoResult(url = url, format = detectFormat(url))
            }
        }

        return null
    }



    private fun fetchHtml(url: String, referer: String): String {
        val request = Request.Builder()
            .url(url)
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .addHeader("Accept-Language", "id-ID,id;q=0.9,en-US;q=0.8,en;q=0.7")
            .addHeader("Referer", referer)
            .build()

        val response = okHttpClient.newCall(request).execute()
        return response.body?.string() ?: throw Exception("Empty response")
    }
}
