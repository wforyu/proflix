package com.proflix.provider.data.provider

import android.util.Log
import com.proflix.core.network.EmbedVideoExtractor
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLDecoder

class OploverzStreamExtractor(
    private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val TAG = "OploverzStreamExtractor"
    }

    data class VideoResult(
        val url: String,
        val quality: String,
        val format: String
    )

    suspend fun resolve(streamUrl: String, quality: String): VideoResult? {
        val lower = streamUrl.lowercase()

        if (EmbedVideoExtractor.isDirectVideoUrl(streamUrl)) {
            val fmt = EmbedVideoExtractor.detectFormat(streamUrl)
            return VideoResult(streamUrl, quality, fmt.extension)
        }

        if (isUpboltUrl(streamUrl)) {
            return resolveUpbolt(streamUrl, quality)
        }

        if (isBloggerUrl(streamUrl)) {
            return resolveBlogger(streamUrl, quality)
        }

        if (isFiledonUrl(streamUrl)) {
            return resolveFiledon(streamUrl, quality)
        }

        return try {
            val html = fetchHtml(streamUrl, streamUrl)
            resolveFromHtml(html, quality)
        } catch (_: Exception) {
            null
        }
    }

    private fun isUpboltUrl(url: String): Boolean {
        return url.contains("upbolt.to")
    }

    private fun isBloggerUrl(url: String): Boolean {
        return url.contains("blogger.com/video.g") || url.contains("blogger.com/video?")
    }

    private fun isFiledonUrl(url: String): Boolean {
        return url.contains("filedon.co")
    }

    private fun resolveUpbolt(upboltUrl: String, quality: String): VideoResult? {
        return try {
            val code = extractUpboltCode(upboltUrl) ?: return null

            val formBody = FormBody.Builder()
                .add("op", "embed")
                .add("file_code", code)
                .add("auto", "1")
                .add("referer", "")
                .build()

            val request = Request.Builder()
                .url("https://upbolt.to/dl")
                .post(formBody)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .addHeader("Referer", upboltUrl)
                .addHeader("Origin", "https://upbolt.to")
                .build()

            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string() ?: return null
            response.close()

            Log.d(TAG, "upbolt POST response length: ${body.length}")

            val videoUrl = extractVideoUrlFromResponse(body)
            if (videoUrl != null) {
                Log.d(TAG, "upbolt resolved: $videoUrl")
                val fmt = EmbedVideoExtractor.detectFormat(videoUrl)
                return VideoResult(videoUrl, quality, fmt.extension)
            }

            Log.w(TAG, "upbolt: no video URL found in response")
            null
        } catch (e: Exception) {
            Log.w(TAG, "upbolt resolve failed: ${e.message}")
            null
        }
    }

    private fun extractUpboltCode(url: String): String? {
        return try {
            val decoded = URLDecoder.decode(url, "UTF-8")
            val path = decoded.substringAfter("upbolt.to/e/", "")
                .substringBefore("?")
                .substringBefore(".html")
                .trimEnd('/')
            path.ifBlank { null }
        } catch (_: Exception) {
            val match = Regex("/e/([a-zA-Z0-9]+)").find(url)
            match?.groupValues?.get(1)
        }
    }

    private fun extractVideoUrlFromResponse(html: String): String? {
        val patterns = listOf(
            Regex("""(?:file|source|src|videoUrl|url)\s*[:=]\s*['"]?(https?://[^'";\s]+\.(?:mp4|m3u8|mpd)(?:\?[^'"'\s]*)?)['"]?"""),
            Regex("""['"](?:file|source|src|videoUrl|url)['"]\s*:\s*['"]?(https?://[^'";\s]+\.(?:mp4|m3u8|mpd)(?:\?[^'"'\s]*)?)['"]?"""),
            Regex("""<source\s+[^>]*src\s*=\s*['"]?(https?://[^'"\s>]+\.(?:mp4|m3u8|mpd)(?:\?[^'"'\s>])*)['"]?"""),
            Regex("""<video\s+[^>]*src\s*=\s*['"]?(https?://[^'"\s>]+\.(?:mp4|m3u8|mpd)(?:\?[^'"'\s>])*)['"]?"""),
            Regex("""['"]?sources['"]?\s*:\s*\[\s*\{[^}]*['"]?file['"]?\s*:\s*['"]?(https?://[^'";\s]+)['"]?"""),
            Regex("""(?:hlsUrl|dashUrl|mp4Url)\s*[:=]\s*['"]?(https?://[^'";\s]+)['"]?"""),
            Regex("""sources\s*:\s*\[(\{[^}]+\})\]"""),
            Regex("""\{[^}]*file\s*:\s*['"]([^'"]+)['"]"""),
        )

        for (pattern in patterns) {
            val match = pattern.find(html) ?: continue
            val url = match.groupValues.getOrNull(1)?.trim() ?: match.value.trim()
            val cleanUrl = url.removeSurrounding("'").removeSurrounding("\"")
                .replace("\\u003d", "=")
                .replace("\\u0026", "&")
                .replace("\\/", "/")
            if (cleanUrl.startsWith("http") && (cleanUrl.contains(".mp4") || cleanUrl.contains(".m3u8") || cleanUrl.contains(".mpd"))) {
                return cleanUrl
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

    private suspend fun resolveBlogger(bloggerUrl: String, quality: String): VideoResult? {
        return try {
            val extractor = EmbedVideoExtractor(okHttpClient)
            val result = extractor.extract(bloggerUrl)
            if (result != null) {
                VideoResult(result.url, quality, result.format.extension)
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun resolveFiledon(filedonUrl: String, quality: String): VideoResult? {
        return try {
            val html = fetchHtml(filedonUrl, filedonUrl)
            val videoUrl = extractVideoUrlFromResponse(html)
            if (videoUrl != null) {
                val fmt = EmbedVideoExtractor.detectFormat(videoUrl)
                VideoResult(videoUrl, quality, fmt.extension)
            } else {
                val doc = org.jsoup.Jsoup.parse(html, filedonUrl)
                for (iframe in doc.select("iframe[src]")) {
                    val src = iframe.attr("abs:src")
                    if (src.isNotBlank() && !src.contains("about:blank")) {
                        if (EmbedVideoExtractor.isDirectVideoUrl(src)) {
                            val fmt = EmbedVideoExtractor.detectFormat(src)
                            return VideoResult(src, quality, fmt.extension)
                        }
                        val inner = resolve(src, quality)
                        if (inner != null) return inner
                    }
                }
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun resolveFromHtml(html: String, quality: String): VideoResult? {
        val videoUrl = extractVideoUrlFromResponse(html)
        if (videoUrl != null) {
            val fmt = EmbedVideoExtractor.detectFormat(videoUrl)
            return VideoResult(videoUrl, quality, fmt.extension)
        }

        val doc = org.jsoup.Jsoup.parse(html)
        for (iframe in doc.select("iframe[src]")) {
            val src = iframe.attr("abs:src")
            if (src.isNotBlank() && !src.contains("about:blank")) {
                if (EmbedVideoExtractor.isDirectVideoUrl(src)) {
                    val fmt = EmbedVideoExtractor.detectFormat(src)
                    return VideoResult(src, quality, fmt.extension)
                }
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
        val body = response.body?.string() ?: throw Exception("Empty response")
        response.close()
        return body
    }
}
