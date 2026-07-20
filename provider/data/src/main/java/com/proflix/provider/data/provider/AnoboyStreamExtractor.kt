package com.proflix.provider.data.provider

import android.util.Base64
import android.util.Log
import com.proflix.core.network.EmbedVideoExtractor
import okhttp3.OkHttpClient

class AnoboyStreamExtractor(
    private val okHttpClient: OkHttpClient,
    private val embedVideoExtractor: EmbedVideoExtractor
) {
    companion object {
        private const val TAG = "AnoboyStreamExtractor"
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
                Log.d(TAG, "Mirror decoded iframe src: ${iframeSrc.take(80)}...")
                resolve(iframeSrc, quality)
            } else null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to decode mirror option: ${e.message}")
            null
        }
    }

    suspend fun resolve(embedUrl: String, quality: String): VideoResult? {
        if (EmbedVideoExtractor.isDirectVideoUrl(embedUrl)) {
            val fmt = EmbedVideoExtractor.detectFormat(embedUrl)
            return VideoResult(embedUrl, quality, fmt.extension)
        }

        try {
            val result = embedVideoExtractor.extract(embedUrl)
            if (result != null) {
                Log.d(TAG, "EmbedVideoExtractor resolved: ${result.url.take(80)}...")
                return VideoResult(result.url, quality, result.format.extension)
            }
        } catch (e: Exception) {
            Log.w(TAG, "EmbedVideoExtractor failed for $embedUrl: ${e.message}")
        }

        Log.d(TAG, "EmbedVideoExtractor returned null, trying iframe fallback...")
        return resolveIframeFallback(embedUrl, quality)
    }

    private suspend fun resolveIframeFallback(embedUrl: String, quality: String): VideoResult? {
        return try {
            val html = fetchHtml(embedUrl, embedUrl)
            val doc = org.jsoup.Jsoup.parse(html, embedUrl)

            for (iframe in doc.select("iframe[src]")) {
                val src = iframe.attr("abs:src").ifBlank { iframe.attr("abs:data-src") }
                if (src.isNotBlank() && !src.contains("about:blank")) {
                    if (EmbedVideoExtractor.isDirectVideoUrl(src)) {
                        val fmt = EmbedVideoExtractor.detectFormat(src)
                        return VideoResult(src, quality, fmt.extension)
                    }
                    val result = embedVideoExtractor.extract(src)
                    if (result != null) {
                        return VideoResult(result.url, quality, result.format.extension)
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.w(TAG, "iframe fallback failed: ${e.message}")
            null
        }
    }

    private fun fetchHtml(url: String, referer: String): String {
        val request = okhttp3.Request.Builder()
            .url(url)
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .addHeader("Accept-Language", "id-ID,id;q=0.9,en-US;q=0.8,en;q=0.7")
            .addHeader("Referer", referer)
            .build()

        val response = okHttpClient.newCall(request).execute()
        return response.use { it.body?.string() ?: throw Exception("Empty response") }
    }
}
