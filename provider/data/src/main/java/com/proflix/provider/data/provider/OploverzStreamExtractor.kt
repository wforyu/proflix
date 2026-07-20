package com.proflix.provider.data.provider

import android.util.Log
import com.proflix.core.network.EmbedVideoExtractor
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLDecoder

class OploverzStreamExtractor(
    private val okHttpClient: OkHttpClient,
    private val embedVideoExtractor: EmbedVideoExtractor
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
        if (EmbedVideoExtractor.isDirectVideoUrl(streamUrl)) {
            val fmt = EmbedVideoExtractor.detectFormat(streamUrl)
            return VideoResult(streamUrl, quality, fmt.extension)
        }

        try {
            val result = embedVideoExtractor.extract(streamUrl)
            if (result != null) {
                return VideoResult(result.url, quality, result.format.extension)
            }
        } catch (e: Exception) {
            Log.w(TAG, "embedVideoExtractor failed for $streamUrl: ${e.message}")
        }

        return try {
            val html = fetchHtml(streamUrl, streamUrl)
            resolveFromHtml(html, quality)
        } catch (_: Exception) {
            null
        }
    }

    private fun resolveFromHtml(html: String, quality: String): VideoResult? {
        val patterns = listOf(
            Regex("""(?:file|source|src|videoUrl|url)\s*[:=]\s*['"]?(https?://[^'";\s]+\.(?:mp4|m3u8|mpd)(?:\?[^'"'\s]*)?)['"]?"""),
            Regex("""['"](?:file|source|src|videoUrl|url)['"]\s*:\s*['"]?(https?://[^'";\s]+\.(?:mp4|m3u8|mpd)(?:\?[^'"'\s]*)?)['"]?"""),
            Regex("""['"]?sources['"]?\s*:\s*\[\s*\{[^}]*['"]?file['"]?\s*:\s*['"]?(https?://[^'";\s]+)['"]?"""),
        )

        for (pattern in patterns) {
            val match = pattern.find(html) ?: continue
            val url = match.groupValues.getOrNull(1)?.trim() ?: match.value.trim()
            val cleanUrl = url.removeSurrounding("'").removeSurrounding("\"")
                .replace("\\u003d", "=")
                .replace("\\u0026", "&")
                .replace("\\/", "/")
            if (cleanUrl.startsWith("http") && (cleanUrl.contains(".mp4") || cleanUrl.contains(".m3u8") || cleanUrl.contains(".mpd"))) {
                val fmt = EmbedVideoExtractor.detectFormat(cleanUrl)
                return VideoResult(cleanUrl, quality, fmt.extension)
            }
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
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
            .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .addHeader("Accept-Language", "id-ID,id;q=0.9,en-US;q=0.8,en;q=0.7")
            .addHeader("Referer", referer)
            .build()

        val response = okHttpClient.newCall(request).execute()
        return response.use { it.body?.string() ?: throw Exception("Empty response") }
    }
}
