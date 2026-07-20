package com.proflix.provider.data.provider

import android.util.Base64
import android.util.Log
import com.proflix.core.network.EmbedVideoExtractor
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

class AnoboyStreamExtractor(
    private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val TAG = "AnoboyStreamExtractor"

        private val BLOGGER_RPC_IDS = listOf(
            "jrwoqf", "H1TAV", "UQiIPe", "bcGtuc", "hruxee",
            "UvZcsc", "Augo5c", "vBV9id", "p6vSuf", "Ppwqme"
        )
    }

    data class VideoResult(
        val url: String,
        val quality: String,
        val format: String
    )

    suspend fun resolveFromMirrorOption(encodedHtml: String, quality: String): VideoResult? {
        return try {
            val decoded = String(Base64.decode(encodedHtml, Base64.DEFAULT))
            val srcRegex = Regex("""src\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            val match = srcRegex.find(decoded)
            val iframeSrc = match?.groupValues?.get(1) ?: ""

            if (iframeSrc.isNotBlank()) {
                resolve(iframeSrc, quality)
            } else null
        } catch (_: Exception) {
            null
        }
    }

    suspend fun resolve(embedUrl: String, quality: String): VideoResult? {
        if (EmbedVideoExtractor.isDirectVideoUrl(embedUrl)) {
            val fmt = EmbedVideoExtractor.detectFormat(embedUrl)
            return VideoResult(embedUrl, quality, fmt.extension)
        }

        if (embedUrl.contains("blogger.com/video.g") || embedUrl.contains("blogger.com/video?")) {
            return resolveBloggerToken(embedUrl, quality)
        }

        return try {
            val html = fetchHtml(embedUrl, embedUrl)
            val videoUrl = extractVideoUrlFromHtml(html)
            if (videoUrl != null) {
                val fmt = EmbedVideoExtractor.detectFormat(videoUrl)
                VideoResult(videoUrl, quality, fmt.extension)
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun resolveBloggerToken(bloggerUrl: String, quality: String): VideoResult? {
        val token = extractBloggerToken(bloggerUrl) ?: return null
        Log.d(TAG, "Resolving blogger token: ${token.take(30)}...")

        for (rpcId in BLOGGER_RPC_IDS) {
            for (paramFormat in listOf(
                { t: String -> """["$t"]""" },
                { t: String -> """["$t","",0]""" },
                { t: String -> """["$t",null]""" },
                { t: String -> """["$t",""]""" }
            )) {
                try {
                    val params = paramFormat(token)
                    val innerPayload = params.replace("\"", "\\\"")
                    val rpcPayload = """[["$rpcId","$innerPayload",null,"generic"]]"""

                    val formBody = FormBody.Builder()
                        .add("f.req", rpcPayload)
                        .build()

                    val request = Request.Builder()
                        .url("https://www.blogger.com/_/BloggerVideoPlayerUi/data/batchexecute")
                        .post(formBody)
                        .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
                        .addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                        .addHeader("Referer", "https://www.blogger.com/video.g?token=${java.net.URLEncoder.encode(token, "UTF-8")}")
                        .addHeader("Origin", "https://www.blogger.com")
                        .build()

                    val response = okHttpClient.newCall(request).execute()
                    val body = response.body?.string() ?: continue
                    response.close()

                    if (body.isBlank() || body.startsWith("<!")) continue

                    val videoUrl = parseBloggerResponse(body)
                    if (videoUrl != null) {
                        Log.d(TAG, "batchexecute resolved via rpcId=$rpcId")
                        val fmt = EmbedVideoExtractor.detectFormat(videoUrl)
                        return VideoResult(videoUrl, quality, fmt.extension)
                    }
                } catch (_: Exception) {}
            }
        }

        return try {
            val html = fetchHtml(bloggerUrl, "https://www.blogger.com/")
            val videoUrl = extractVideoUrlFromHtml(html)
            if (videoUrl != null) {
                val fmt = EmbedVideoExtractor.detectFormat(videoUrl)
                VideoResult(videoUrl, quality, fmt.extension)
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private fun extractBloggerToken(url: String): String? {
        val patterns = listOf(
            Regex("""token=([^&"'\\s]+)"""),
            Regex("""token%3D([^&"'\\s]+)""", RegexOption.IGNORE_CASE)
        )
        for (pattern in patterns) {
            pattern.find(url)?.groupValues?.get(1)?.let { token ->
                return token.replace("\\u003d", "=").replace("\\u0026", "&")
            }
        }
        return null
    }

    private fun parseBloggerResponse(body: String): String? {
        val cleanBody = body.trimStart().removePrefix(")]}'").trim()

        try {
            val arr = JSONArray(cleanBody)
            for (i in 0 until arr.length()) {
                val outer = arr.optJSONArray(i) ?: continue

                for (j in 0 until outer.length()) {
                    val candidate = outer.optString(j, "")
                    if (candidate.isBlank() || candidate == "null") continue
                    val videoUrl = extractGoogleVideoUrl(candidate)
                    if (videoUrl != null) return videoUrl
                }

                if (outer.length() >= 3) {
                    val dataStr = outer.optString(2, "")
                    if (dataStr.isNotBlank() && dataStr != "null") {
                        val videoUrl = extractGoogleVideoUrl(dataStr)
                        if (videoUrl != null) return videoUrl
                    }
                }

                if (outer.length() >= 2) {
                    val inner = outer.optJSONArray(1)
                    if (inner != null) {
                        for (k in 0 until inner.length()) {
                            val innerCandidate = inner.optString(k, "")
                            if (innerCandidate.isBlank() || innerCandidate == "null") continue
                            val videoUrl = extractGoogleVideoUrl(innerCandidate)
                            if (videoUrl != null) return videoUrl
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        val directUrl = extractGoogleVideoUrl(cleanBody)
        if (directUrl != null) return directUrl

        return null
    }

    private fun extractGoogleVideoUrl(text: String): String? {
        val unescaped = text
            .replace("\\u003d", "=")
            .replace("\\u0026", "&")
            .replace("\\/", "/")
            .replace("\\\"", "\"")

        val patterns = listOf(
            Regex("""https?://[a-z0-9.-]*googlevideo\.com/videoplayback\?[^'"\s\\]+"""),
            Regex("""https?:\\?/\\?/[a-z0-9.-]*googlevideo\.com\\?/videoplayback[^"'\\]+"""),
            Regex("""https?://[a-z0-9.-]+\.googlevideo\.com/[^'"\s<>]+"""),
            Regex("""https?://[a-z0-9.-]*googlevideo\.com/videoplayback[^'"\\]+"""),
        )

        for (pattern in patterns) {
            val match = pattern.find(unescaped) ?: continue
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

    private fun extractVideoUrlFromHtml(html: String): String? {
        val unescaped = html
            .replace("\\u003d", "=")
            .replace("\\u0026", "&")
            .replace("\\/", "/")

        val googleVideo = extractGoogleVideoUrl(unescaped)
        if (googleVideo != null) return googleVideo

        val patterns = listOf(
            Regex("""['"]https?://[^'"]*googlevideo[^'"]*['"]"""),
            Regex("""url\s*[:=]\s*['"]?(https?://[^'";\s]+)['"]?"""),
            Regex("""src\s*[:=]\s*['"]?(https?://[^'";\s]+)['"]?"""),
            Regex("""\"playback_url\"\s*:\s*\"(https?:\\?/\\?/[^\"\\]+)"""),
            Regex("""\"video_url\"\s*:\s*\"(https?:\\?/\\?/[^\"\\]+)"""),
            Regex("""\"stream_url\"\s*:\s*\"(https?:\\?/\\?/[^\"\\]+)"""),
            Regex("""\"(https?:\\?/\\?/[^\"\\]*googlevideo[^\"\\]*)\""""),
        )
        for (pattern in patterns) {
            pattern.find(html)?.let { match ->
                var url = match.groupValues.getOrNull(1)?.trim() ?: match.value.trim()
                url = url.removeSurrounding("'").removeSurrounding("\"")
                url = url.replace("\\u003d", "=").replace("\\u0026", "&").replace("\\/", "/")
                if (url.contains("googlevideo.com") || url.contains("videoplayback")) {
                    return url
                }
                val nestedUrl = extractGoogleVideoUrl(url)
                if (nestedUrl != null) return nestedUrl
            }
        }

        val videoHosts = listOf(
            "etvp.cc", "turboviplay.com", "sptvp.com",
            "turbovidhls.com", "embedwish.com", "sumpiernos.com",
            "vcdn2.mystream.to", "4meplayer.com", "4meplayer.pro", "abyssplayer.com"
        )
        val hostPattern = Regex("""(https?://[a-z0-9.-]*(?:${videoHosts.joinToString("|") { Regex.escape(it) }})[^\s"'<>]*)""")
        hostPattern.find(html)?.let { match ->
            val url = match.groupValues[1].trim()
            if (url.isNotBlank()) return url
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
        val body = response.body?.string() ?: throw Exception("Empty response")
        response.close()
        return body
    }

}
