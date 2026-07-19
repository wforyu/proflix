package com.proflix.provider.data.provider

import android.util.Base64
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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnoboyProvider @Inject constructor(
    private val okHttpClient: OkHttpClient
) : Provider {

    private val embedExtractor = EmbedVideoExtractor(okHttpClient)

    companion object {
        var BASE_URL = "https://anoboy.pk"
    }

    override suspend fun getHome(): Result<HomeContent> = withContext(Dispatchers.IO) {
        safeCall {
            val doc = fetchDocument(BASE_URL)

            val trending = parseTrending(doc)
            val latest = parseLatest(doc)

            HomeContent(
                heroContent = trending.firstOrNull() ?: latest.firstOrNull(),
                trending = trending,
                latest = latest,
                continueWatching = emptyList(),
                categories = mapOf(
                    "Trending" to trending,
                    "Latest Release" to latest
                )
            )
        }
    }

    override suspend fun getTrending(): Result<List<Content>> = withContext(Dispatchers.IO) {
        safeCall {
            val doc = fetchDocument(BASE_URL)
            parseTrending(doc)
        }
    }

    override suspend fun getLatest(): Result<List<Content>> = withContext(Dispatchers.IO) {
        safeCall {
            val doc = fetchDocument(BASE_URL)
            parseLatest(doc)
        }
    }

    override suspend fun getDetail(id: String): Result<Content> = withContext(Dispatchers.IO) {
        safeCall {
            val animeId = toAnimeId(id)
            val doc = fetchDocument("$BASE_URL/$animeId")

            val title = doc.selectFirst("h1.titless, .entry-title, h2.entry-title")?.text()
                ?: doc.selectFirst(".episodelist + h2, #singlepisode .headlist .det h2 a")?.text()
                ?: doc.title()?.substringBeforeLast("-")?.trim()
                ?: id.substringAfterLast("/").substringBeforeLast("-").replace("-", " ").trim()
                    .replaceFirstChar { it.uppercase() }

            val poster = doc.selectFirst(".bigcontent .thumb img, .thumb img, .epthumb img")
                ?.let { it.attr("abs:data-src").ifBlank { it.attr("abs:src") } } ?: ""

            val banner = doc.selectFirst(".bigcontent .thumb img, .anmsrc img")
                ?.let { it.attr("abs:data-src").ifBlank { it.attr("abs:src") } } ?: poster

            val description = doc.selectFirst(".mindesc, .mindes, .sinopc, .entry-content")?.text() ?: ""

            val genres = doc.select(".infox .genxed a, .mgen a, .bigcontent .infox .genxed a")
                .map { it.text().trim() }

            var year = ""
            var rating = ""
            doc.select(".infox .spe span, .bigcontent .infox .spe span").forEach { span ->
                val text = span.text()
                when {
                    text.contains("Year") || text.contains("Tahun") -> {
                        year = text.substringAfter(":").trim()
                    }
                    text.contains("Score") || text.contains("Rating") -> {
                        rating = text.substringAfter(":").trim()
                    }
                }
            }

            Content(
                id = animeId,
                title = title,
                poster = poster,
                banner = banner,
                description = description,
                genres = genres,
                year = year,
                rating = rating,
                type = ContentType.ANIME
            )
        }
    }

    override suspend fun getEpisodes(contentId: String): Result<List<Episode>> = withContext(Dispatchers.IO) {
        safeCall {
            val animeId = toAnimeId(contentId)
            val doc = fetchDocument("$BASE_URL/$animeId")
            val episodes = mutableListOf<Episode>()

            doc.select(".eplister ul li, .episodelist ul li, .listeps li").forEach { ep ->
                val link = ep.selectFirst("a")
                val epUrl = link?.attr("abs:href") ?: ""
                val eplNum = ep.selectFirst(".epl-num")?.text()?.trim()?.toIntOrNull()
                val eplTitle = ep.selectFirst(".epl-title")?.text()?.trim() ?: ""
                val epTitle = eplTitle.ifBlank {
                    ep.selectFirst(".playinfo h3, h3")?.text()?.trim() ?: ""
                }.ifBlank {
                    link?.attr("title")?.trim() ?: ""
                }
                val epInfo = ep.selectFirst(".epl-date, .playinfo span, span")?.text() ?: ""
                val epNum = eplNum
                    ?: Regex("Episode\\s*(\\d+)").find(epTitle)?.groupValues?.get(1)?.toIntOrNull()
                    ?: Regex("Ep\\.?\\s*(\\d+)").find(epTitle)?.groupValues?.get(1)?.toIntOrNull()
                    ?: (episodes.size + 1)

                val epId = epUrl.removePrefix(BASE_URL).trimStart('/')

                if (epId.isNotBlank()) {
                    episodes.add(
                        Episode(
                            id = epId,
                            contentId = animeId,
                            season = 1,
                            number = epNum,
                            title = epTitle.ifBlank { "Episode $epNum" },
                            description = epInfo,
                            thumbnail = ep.selectFirst("img")
                                ?.let { it.attr("abs:data-src").ifBlank { it.attr("abs:src") } } ?: "",
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
            val doc = fetchDocument("$BASE_URL/$episodeId")
            val sources = mutableListOf<StreamSource>()

            doc.select("select.mirror option").forEach { option ->
                val value = option.attr("value").trim()
                val label = option.text().trim()
                if (value.isNotBlank() && label.isNotBlank() && label != "Select Video Server") {
                    try {
                        val decoded = String(Base64.decode(value, Base64.DEFAULT))
                        val srcRegex = Regex("""src\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
                        val match = srcRegex.find(decoded)
                        val iframeSrc = match?.groupValues?.get(1) ?: ""
                        if (iframeSrc.isNotBlank()) {
                            if (EmbedVideoExtractor.isDirectVideoUrl(iframeSrc)) {
                                val format = EmbedVideoExtractor.detectFormat(iframeSrc)
                                sources.add(StreamSource(url = iframeSrc, quality = label, format = format.extension))
                            } else {
                                val result = embedExtractor.extract(iframeSrc)
                                if (result != null) {
                                    sources.add(StreamSource(url = result.url, quality = label, format = result.format.extension))
                                }
                            }
                        }
                    } catch (_: Exception) {
                    }
                }
            }

            if (sources.isEmpty()) {
                doc.select("#pembed iframe, .player-embed iframe, #embed_holder iframe").forEach { iframe ->
                    val src = iframe.attr("abs:src").ifBlank { iframe.attr("abs:data-src") }
                    if (src.isNotBlank() && !src.contains("about:blank")) {
                        if (EmbedVideoExtractor.isDirectVideoUrl(src)) {
                            val format = EmbedVideoExtractor.detectFormat(src)
                            sources.add(StreamSource(url = src, quality = "default", format = format.extension))
                        } else {
                            val result = embedExtractor.extract(src)
                            if (result != null) {
                                sources.add(StreamSource(url = result.url, quality = "default", format = result.format.extension))
                            }
                        }
                    }
                }
            }

            if (sources.isEmpty()) {
                doc.select(".soraddlx .soraurlx").forEach { dl ->
                    val quality = dl.selectFirst("strong")?.text() ?: "default"
                    val url = dl.selectFirst("a")?.attr("abs:href") ?: ""
                    if (url.isNotBlank()) {
                        if (EmbedVideoExtractor.isDirectVideoUrl(url)) {
                            val format = EmbedVideoExtractor.detectFormat(url)
                            sources.add(StreamSource(url = url, quality = quality, format = format.extension))
                        } else {
                            val result = embedExtractor.extract(url)
                            if (result != null) {
                                sources.add(StreamSource(url = result.url, quality = quality, format = result.format.extension))
                            }
                        }
                    }
                }
            }

            sources
        }
    }

    override suspend fun search(query: String): Result<List<Content>> = withContext(Dispatchers.IO) {
        safeCall {
            val doc = fetchDocument("$BASE_URL/?s=$query")
            val results = mutableListOf<Content>()

            doc.select("article.bs").forEach { element ->
                val link = element.selectFirst("a")
                val title = element.selectFirst(".tt h2[itemprop=\"headline\"]")?.text()
                    ?: element.selectFirst(".tt")?.text() ?: ""
                val poster = element.selectFirst("img")
                    ?.let { it.attr("abs:data-src").ifBlank { it.attr("abs:src") } } ?: ""
                val href = link?.attr("href") ?: ""
                val id = href.removePrefix(BASE_URL).trimStart('/')

                if (title.isNotBlank() && id.isNotBlank()) {
                    results.add(
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

            results
        }
    }

    override suspend fun getProviderName(): String = "Anoboy"

    private fun fetchDocument(url: String): org.jsoup.nodes.Document {
        val request = Request.Builder()
            .url(url)
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
            .addHeader("Accept-Language", "id-ID,id;q=0.9,en-US;q=0.8,en;q=0.7")
            .build()

        val response = okHttpClient.newCall(request).execute()
        val body = response.body?.string() ?: throw Exception("Empty response")
        return org.jsoup.Jsoup.parse(body, url)
    }

    private fun toAnimeId(id: String): String {
        if (id.startsWith("anime/")) return id
        val match = Regex("^(.+)-episode-\\d+").find(id)
        if (match != null) {
            return "anime/${match.groupValues[1]}"
        }
        return "anime/$id"
    }

    private fun parseTrending(doc: org.jsoup.nodes.Document): List<Content> {
        val contents = mutableListOf<Content>()

        doc.select("aside#sidebar li a[href*=\"/anime/\"]").forEach { a ->
            val title = a.text().trim()
            val href = a.attr("href") ?: ""
            val id = href.removePrefix(BASE_URL).trimStart('/')
            val poster = a.selectFirst("img")
                ?.let { it.attr("abs:data-src").ifBlank { it.attr("abs:src") } } ?: ""

            if (title.isNotBlank() && id.isNotBlank() && id.startsWith("anime/")) {
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

        return contents.distinctBy { it.id }
    }

    private fun parseLatest(doc: org.jsoup.nodes.Document): List<Content> {
        val contents = mutableListOf<Content>()

        doc.select("article.bs").forEach { element ->
            val link = element.selectFirst(".bsx a") ?: element.selectFirst("a")
            val title = element.selectFirst(".tt h2[itemprop=\"headline\"]")?.text()
                ?: element.selectFirst(".tt")?.text() ?: ""
            val poster = element.selectFirst(".limit img")
                ?.let { it.attr("abs:data-src").ifBlank { it.attr("abs:src") } } ?: ""
            val href = link?.attr("href") ?: ""
            val rawId = href.removePrefix(BASE_URL).trimStart('/')
            val id = toAnimeId(rawId)
            val epx = element.selectFirst(".epx")?.text()?.trim() ?: ""

            if (title.isNotBlank() && id.isNotBlank()) {
                contents.add(
                    Content(
                        id = id,
                        title = title,
                        poster = poster,
                        banner = poster,
                        description = epx,
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
}
