package com.proflix.provider.data.provider

import android.util.Log
import com.proflix.core.network.EmbedVideoExtractor
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup

class SamehadakuStreamExtractor(
    private val okHttpClient: OkHttpClient,
    private val embedVideoExtractor: EmbedVideoExtractor
) {
    companion object {
        private const val TAG = "SamehadakuStreamExtractor"
    }

    data class VideoResult(
        val url: String,
        val quality: String,
        val format: String
    )

    suspend fun resolveFromAjaxResponse(
        postId: String,
        nume: String,
        episodeUrl: String,
        baseUrl: String
    ): List<VideoResult> {
        val results = mutableListOf<VideoResult>()

        val iframeUrl = fetchPlayerIframe(postId, nume, episodeUrl, baseUrl) ?: return results
        Log.d(TAG, "AJAX iframe URL: $iframeUrl")

        val resolved = resolve(iframeUrl, "Server $nume")
        if (resolved != null) {
            results.add(resolved)
        }

        return results
    }

    suspend fun resolveFromEpisodePage(episodeUrl: String): List<VideoResult> {
        val results = mutableListOf<VideoResult>()

        try {
            val html = fetchHtml(episodeUrl, episodeUrl)
            val doc = Jsoup.parse(html, episodeUrl)

            for (iframe in doc.select("iframe[src]")) {
                val src = iframe.attr("src").trim()
                if (src.isBlank() || src.contains("facebook") || src.contains("about:blank")) continue

                val fullUrl = resolveAbsoluteUrl(src, episodeUrl) ?: continue
                val resolved = resolve(fullUrl, "default")
                if (resolved != null) {
                    results.add(resolved)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "resolveFromEpisodePage failed: ${e.message}")
        }

        return results
    }

    suspend fun resolve(embedUrl: String, quality: String): VideoResult? {
        if (EmbedVideoExtractor.isDirectVideoUrl(embedUrl)) {
            val fmt = EmbedVideoExtractor.detectFormat(embedUrl)
            return VideoResult(embedUrl, quality, fmt.extension)
        }

        if (isBloggerUrl(embedUrl)) {
            return resolveBlogger(embedUrl, quality)
        }

        return resolveGenericEmbed(embedUrl, quality)
    }

    private fun fetchPlayerIframe(
        postId: String,
        nume: String,
        episodeUrl: String,
        baseUrl: String
    ): String? {
        return try {
            val body = FormBody.Builder()
                .add("action", "player_ajax")
                .add("post", postId)
                .add("nume", nume)
                .add("type", "schtml")
                .build()

            val request = Request.Builder()
                .url("$baseUrl/wp-admin/admin-ajax.php")
                .post(body)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .addHeader("Accept", "text/html, */*; q=0.01")
                .addHeader("Accept-Language", "id-ID,id;q=0.9,en-US;q=0.8,en;q=0.7")
                .addHeader("X-Requested-With", "XMLHttpRequest")
                .addHeader("Origin", baseUrl)
                .addHeader("Referer", episodeUrl)
                .build()

            Log.d(TAG, "Fetching player iframe: post=$postId, nume=$nume")
            val response = okHttpClient.newCall(request).execute()
            val html = response.use { it.body?.string() } ?: return null

            Log.d(TAG, "AJAX response length=${html.length}")

            if (html.contains("challenge-platform") || html.contains("Just a moment")) {
                Log.w(TAG, "Cloudflare challenge detected for post=$postId nume=$nume")
                return null
            }

            val doc = Jsoup.parse(html, baseUrl)
            val iframe = doc.selectFirst("iframe[src]")
            val src = iframe?.attr("src")?.trim()

            if (!src.isNullOrBlank()) {
                Log.d(TAG, "Found iframe src: $src")
                resolveAbsoluteUrl(src, baseUrl)
            } else {
                Log.w(TAG, "No iframe found in AJAX response. Response: ${html.take(500)}")
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "fetchPlayerIframe failed: ${e.message}")
            null
        }
    }

    private suspend fun resolveBlogger(bloggerUrl: String, quality: String): VideoResult? {
        return try {
            val result = embedVideoExtractor.extract(bloggerUrl)
            if (result != null) {
                VideoResult(result.url, quality, result.format.extension)
            } else {
                Log.w(TAG, "Blogger extraction failed for: $bloggerUrl")
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "resolveBlogger failed: ${e.message}")
            null
        }
    }

    private suspend fun resolveGenericEmbed(embedUrl: String, quality: String): VideoResult? {
        return try {
            val html = fetchHtml(embedUrl, embedUrl)
            val patterns = listOf(
                Regex("""(?:file|source|src|videoUrl|url)\s*[:=]\s*['"]?(https?://[^'";\s]+\.(?:mp4|m3u8|mpd)(?:\?[^'"'\s]*)?)['"]?"""),
                Regex("""['"](?:file|source|src|videoUrl|url)['"]\s*:\s*['"]?(https?://[^'";\s]+\.(?:mp4|m3u8|mpd)(?:\?[^'"'\s]*)?)['"]?"""),
                Regex("""<source\s+[^>]*src\s*=\s*['"]?(https?://[^'"\s>]+\.(?:mp4|m3u8|mpd)(?:\?[^'"'\s>])*)['"]?"""),
                Regex("""<video\s+[^>]*src\s*=\s*['"]?(https?://[^'"\s>]+\.(?:mp4|m3u8|mpd)(?:\?[^'"'\s>])*)['"]?"""),
                Regex("""['"]?sources['"]?\s*:\s*\[\s*\{[^}]*['"]?file['"]?\s*:\s*['"]?(https?://[^'";\s]+)['"]?"""),
            )

            for (pattern in patterns) {
                val match = pattern.find(html) ?: continue
                val url = match.groupValues.getOrNull(1)?.trim() ?: continue
                if (url.isNotBlank() && url.startsWith("http")) {
                    val fmt = EmbedVideoExtractor.detectFormat(url)
                    return VideoResult(url, quality, fmt.extension)
                }
            }

            val doc = Jsoup.parse(html, embedUrl)
            for (iframe in doc.select("iframe[src]")) {
                val src = iframe.attr("abs:src")
                if (src.isNotBlank() && !src.contains("about:blank")) {
                    if (EmbedVideoExtractor.isDirectVideoUrl(src)) {
                        val fmt = EmbedVideoExtractor.detectFormat(src)
                        return VideoResult(src, quality, fmt.extension)
                    }
                }
            }

            null
        } catch (e: Exception) {
            Log.w(TAG, "resolveGenericEmbed failed: ${e.message}")
            null
        }
    }

    private fun isBloggerUrl(url: String): Boolean {
        return url.contains("blogger.com/video.g") || url.contains("blogger.com/video?")
    }

    private fun resolveAbsoluteUrl(url: String, baseUrl: String): String? {
        if (url.isBlank()) return null
        return when {
            url.startsWith("http") -> url
            url.startsWith("//") -> "https:$url"
            url.startsWith("/") -> {
                val baseRegex = Regex("^(https?://[^/]+)")
                val base = baseRegex.find(baseUrl)?.groupValues?.get(1) ?: "https://"
                "$base$url"
            }
            else -> url
        }
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
        return response.use { it.body?.string() ?: throw Exception("Empty response") }
    }
}
