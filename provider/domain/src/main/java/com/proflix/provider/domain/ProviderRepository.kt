package com.proflix.provider.domain

import com.proflix.provider.domain.model.Content
import com.proflix.provider.domain.model.Episode
import com.proflix.provider.domain.model.HomeContent
import com.proflix.provider.domain.model.StreamSource
import com.proflix.common.utils.Result

enum class ProviderType(val displayName: String) {
    ANOBOY("Anoboy"),
    SAMEHADAKU("Samehadaku"),
    OPLOVERZ("Oploverz")
}

interface ProviderRepository {
    suspend fun getHome(): Result<HomeContent>
    suspend fun getTrending(): Result<List<Content>>
    suspend fun getLatest(): Result<List<Content>>
    suspend fun getDetail(id: String): Result<Content>
    suspend fun getEpisodes(contentId: String): Result<List<Episode>>
    suspend fun getStream(episodeId: String): Result<List<StreamSource>>
    suspend fun search(query: String): Result<List<Content>>
    fun switchProvider(type: ProviderType)
    fun getCurrentProviderType(): ProviderType
    fun getAvailableProviders(): List<ProviderType>
    fun getDefaultDomain(type: ProviderType): String
    fun setCustomDomain(type: ProviderType, domain: String)
    suspend fun getProviderName(): String
}
