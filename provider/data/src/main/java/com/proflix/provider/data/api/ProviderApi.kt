package com.proflix.provider.data.api

import com.proflix.provider.data.dto.ContentDto
import com.proflix.provider.data.dto.EpisodeDto
import com.proflix.provider.data.dto.HomeDto
import com.proflix.provider.data.dto.StreamDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ProviderApi {

    @GET("home")
    suspend fun getHome(): HomeDto

    @GET("trending")
    suspend fun getTrending(): List<ContentDto>

    @GET("latest")
    suspend fun getLatest(): List<ContentDto>

    @GET("detail/{id}")
    suspend fun getDetail(@Path("id") id: String): ContentDto

    @GET("episodes/{contentId}")
    suspend fun getEpisodes(@Path("contentId") contentId: String): List<EpisodeDto>

    @GET("stream/{episodeId}")
    suspend fun getStream(@Path("episodeId") episodeId: String): List<StreamDto>

    @GET("search")
    suspend fun search(@Query("q") query: String): List<ContentDto>
}
