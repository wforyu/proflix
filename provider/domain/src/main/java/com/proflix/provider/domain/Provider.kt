package com.proflix.provider.domain

import com.proflix.common.utils.Result
import com.proflix.provider.domain.model.Content
import com.proflix.provider.domain.model.Episode
import com.proflix.provider.domain.model.HomeContent
import com.proflix.provider.domain.model.StreamSource

interface Provider {
    suspend fun getHome(): Result<HomeContent>
    suspend fun getTrending(): Result<List<Content>>
    suspend fun getLatest(): Result<List<Content>>
    suspend fun getDetail(id: String): Result<Content>
    suspend fun getEpisodes(contentId: String): Result<List<Episode>>
    suspend fun getStream(episodeId: String): Result<List<StreamSource>>
    suspend fun search(query: String): Result<List<Content>>
    suspend fun getProviderName(): String
}
