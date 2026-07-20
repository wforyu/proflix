package com.proflix.provider.data.provider

import android.util.Log
import com.proflix.common.utils.Result
import com.proflix.common.utils.safeCall
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
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SamehadakuProvider @Inject constructor(
    private val okHttpClient: OkHttpClient
) : Provider {

    private val streamExtractor = SamehadakuStreamExtractor(okHttpClient)

    companion object {
        var BASE_URL = "https://v2.samehadaku.how"
        private const val TAG = "SamehadakuProvider"
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }

    override suspend fun getHome(): Result<HomeContent> = withContext(Dispatchers.IO) {
        safeCall {
            val doc = fetchDocument(BASE_URL)
            val latest = parseLatestEpisodes(doc)
            val hero = latest.firstOrNull()

            HomeContent(
                heroContent = hero,
                trending = emptyList(),
                latest = latest,
                continueWatching = emptyList(),
                categories = buildMap {
                    if (latest.isNotEmpty()) put("Episode Terbaru", latest)
                }
            )
        }
    }

    override suspend fun getTrending(): Result<List<Content>> = withContext(Dispatchers.IO) {
        safeCall { emptyList() }
    }

    override suspend fun getLatest(): Result<List<Content>> = withContext(Dispatchers.IO) {
        safeCall {
            val doc = fetchDocument(BASE_URL)
            parseLatestEpisodes(doc)
        }
    }

    override suspend fun getDetail(id: String): Result<Content> = withContext(Dispatchers.IO) {
        safeCall {
            val url = resolveUrl(id)
            val doc = fetchDocument(url)

            val title = doc.selectFirst("h1.headpost")?.text()?.trim()
                ?: doc.selectFirst("meta[property=\"og:title\"]")?.attr("content")?.trim()
                ?: id.substringAfterLast("/").substringBeforeLast("-")
                    .replace("-", " ").trim().replaceFirstChar { it.uppercase() }

            val poster = doc.selectFirst("img.anmsa")?.attr("src")?.trim()
                ?: doc.selectFirst("meta[property=\"og:image\"]")?.attr("content")
                ?: ""

            val description = doc.selectFirst(".entry-content .ttls")?.text()?.trim()
                ?: doc.selectFirst("meta[property=\"og:description\"]")?.attr("content")
                ?: ""

            val genres = mutableListOf<String>()
            for (a in doc.select(".mta a[href*=\"/genre/\"]")) {
                val g = a.text().trim()
                if (g.isNotBlank() && g !in genres) genres.add(g)
            }

            var year = ""
            var rating = ""
            val scoreEl = doc.selectFirst(".score")
            if (scoreEl != null) {
                val scoreText = scoreEl.text().trim()
                rating = scoreText.replace(Regex("[^0-9.]"), "").trim()
            }

            val typeEl = doc.selectFirst(".animepost .type")
            val typeText = typeEl?.text()?.trim() ?: ""
            val contentType = when {
                typeText.contains("Movie", true) -> ContentType.MOVIE
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
            val url = resolveUrl(contentId)
            val doc = fetchDocument(url)
            val episodes = parseEpisodeList(doc, contentId)
            Log.d(TAG, "Parsed ${episodes.size} episodes for $contentId")
            episodes.sortedBy { it.number }
        }
    }

    override suspend fun getStream(episodeId: String): Result<List<StreamSource>> = withContext(Dispatchers.IO) {
        safeCall {
            val sources = mutableListOf<StreamSource>()
            val url = resolveUrl(episodeId)
            val doc = fetchDocument(url)

            val serverOptions = doc.select("#server .east_player_option")
            Log.d(TAG, "Found ${serverOptions.size} server options")

            for (option in serverOptions) {
                val dataPost = option.attr("data-post").trim()
                val dataNume = option.attr("data-nume").trim()
                val label = option.selectFirst("span")?.text()?.trim() ?: "Server $dataNume"

                if (dataPost.isBlank() || dataNume.isBlank()) continue

                val results = streamExtractor.resolveFromAjaxResponse(dataPost, dataNume, url, BASE_URL)
                for (result in results) {
                    sources.add(StreamSource(url = result.url, quality = label, format = result.format))
                }

                if (results.isEmpty()) {
                    Log.w(TAG, "No stream from server $label")
                }
            }

            if (sources.isEmpty()) {
                Log.w(TAG, "No streams from server tabs, trying fallback iframe parsing")
                val fallbackResults = streamExtractor.resolveFromEpisodePage(url)
                for (result in fallbackResults) {
                    sources.add(StreamSource(url = result.url, quality = result.quality, format = result.format))
                }
            }

            Log.d(TAG, "Resolved ${sources.size} stream sources")
            sources
        }
    }

    override suspend fun search(query: String): Result<List<Content>> = withContext(Dispatchers.IO) {
        safeCall {
            val doc = fetchDocument("$BASE_URL/?s=${java.net.URLEncoder.encode(query, "UTF-8")}")
            val results = mutableListOf<Content>()

            for (article in doc.select("article.animpost")) {
                val link = article.selectFirst("a[href*=\"/anime/\"]") ?: continue
                val href = link.attr("href").trim()
                val id = href.removePrefix(BASE_URL).trimStart('/')
                if (id.isBlank()) continue

                val title = article.selectFirst(".title h2")?.text()?.trim()
                    ?: article.selectFirst("img.anmsa")?.attr("alt")?.trim()
                    ?: ""
                val poster = article.selectFirst("img.anmsa")?.attr("src") ?: ""
                val scoreText = article.selectFirst(".score")?.text()?.trim() ?: ""
                val rating = scoreText.replace(Regex("[^0-9.]"), "").trim()
                val typeText = article.selectFirst(".type")?.text()?.trim() ?: ""

                val genres = mutableListOf<String>()
                for (a in article.select(".mta a[href*=\"/genre/\"]")) {
                    val g = a.text().trim()
                    if (g.isNotBlank() && g !in genres) genres.add(g)
                }

                val contentType = when {
                    typeText.contains("Movie", true) -> ContentType.MOVIE
                    else -> ContentType.ANIME
                }

                if (title.isNotBlank() && id.isNotBlank() && results.none { it.id == id }) {
                    results.add(
                        Content(
                            id = id,
                            title = title,
                            poster = poster,
                            banner = poster,
                            description = "",
                            genres = genres,
                            year = "",
                            rating = rating,
                            type = contentType
                        )
                    )
                }
            }

            results
        }
    }

    override suspend fun getProviderName(): String = "Samehadaku"

    private fun parseLatestEpisodes(doc: Document): List<Content> {
        val contents = mutableListOf<Content>()

        for (li in doc.select("div.post-show ul li")) {
            val titleLink = li.selectFirst("h2.entry-title a") ?: continue
            val animeHref = titleLink.attr("href").trim()
            val id = animeHref.removePrefix(BASE_URL).trimStart('/')
            if (id.isBlank()) continue

            val title = titleLink.text().trim()
            val poster = li.selectFirst("img.npws")?.attr("src") ?: ""
            val epText = li.selectFirst("div.dtla span:first-child author[itemprop=\"name\"]")?.text()?.trim() ?: ""

            if (title.isNotBlank() && contents.none { it.id == id }) {
                contents.add(
                    Content(
                        id = id,
                        title = title,
                        poster = poster,
                        banner = poster,
                        description = if (epText.isNotBlank()) "Episode $epText" else "",
                        genres = emptyList(),
                        year = "",
                        rating = "",
                        type = ContentType.ANIME
                    )
                )
            }
        }

        return contents.take(20)
    }

    private fun parseEpisodeList(doc: Document, contentId: String): List<Episode> {
        val episodes = mutableListOf<Episode>()

        for (li in doc.select("div.lstepsiode.listeps ul li, div.listeps ul li")) {
            val epLink = li.selectFirst("div.epsleft span.lchx a") ?: continue
            val href = epLink.attr("href").trim()
            val epUrl = href.removePrefix(BASE_URL).trimStart('/')
            if (epUrl.isBlank()) continue

            val epNumText = li.selectFirst("div.epsright span.eps a")?.text()?.trim() ?: ""
            val epNum = epNumText.toIntOrNull()
                ?: Regex("episode[- ]?(\\d+)").find(epUrl.lowercase())?.groupValues?.get(1)?.toIntOrNull()
                ?: (episodes.size + 1)

            val displayTitle = epLink.text().trim().ifBlank { "Episode $epNum" }

            if (episodes.none { it.id == epUrl }) {
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

    private fun resolveUrl(id: String): String {
        return when {
            id.startsWith("http") -> id
            id.startsWith("/") -> "$BASE_URL$id"
            else -> "$BASE_URL/$id"
        }
    }

    private fun fetchDocument(url: String): Document {
        val html = fetchHtml(url)
        return Jsoup.parse(html, url)
    }

    private fun fetchHtml(url: String): String {
        val request = Request.Builder()
            .url(url)
            .addHeader("User-Agent", USER_AGENT)
            .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .addHeader("Accept-Language", "id-ID,id;q=0.9,en-US;q=0.8,en;q=0.7")
            .build()

        val response = okHttpClient.newCall(request).execute()
        response.use {
            return it.body?.string() ?: throw Exception("Empty response from $url")
        }
    }

}
