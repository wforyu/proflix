package com.proflix.provider.data.repository

import com.proflix.common.utils.Result
import com.proflix.provider.data.provider.ProviderManager
import com.proflix.provider.domain.ProviderRepository
import com.proflix.provider.domain.ProviderType
import com.proflix.provider.domain.model.Content
import com.proflix.provider.domain.model.Episode
import com.proflix.provider.domain.model.HomeContent
import com.proflix.provider.domain.model.StreamSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProviderRepositoryImpl @Inject constructor(
    private val providerManager: ProviderManager
) : ProviderRepository {

    override suspend fun getHome(): Result<HomeContent> {
        return providerManager.getCurrentProvider().getHome()
    }

    override suspend fun getTrending(): Result<List<Content>> {
        return providerManager.getCurrentProvider().getTrending()
    }

    override suspend fun getLatest(): Result<List<Content>> {
        return providerManager.getCurrentProvider().getLatest()
    }

    override suspend fun getDetail(id: String): Result<Content> {
        return providerManager.getCurrentProvider().getDetail(id)
    }

    override suspend fun getEpisodes(contentId: String): Result<List<Episode>> {
        return providerManager.getCurrentProvider().getEpisodes(contentId)
    }

    override suspend fun getStream(episodeId: String): Result<List<StreamSource>> {
        return providerManager.getCurrentProvider().getStream(episodeId)
    }

    override suspend fun search(query: String): Result<List<Content>> {
        return providerManager.getCurrentProvider().search(query)
    }

    override fun switchProvider(type: ProviderType) {
        providerManager.setProvider(type)
    }

    override fun getCurrentProviderType(): ProviderType {
        return providerManager.getCurrentType()
    }

    override fun getAvailableProviders(): List<ProviderType> {
        return providerManager.getAvailableProviders()
    }

    override fun getDefaultDomain(type: ProviderType): String {
        return providerManager.getDefaultDomain(type)
    }

    override fun setCustomDomain(type: ProviderType, domain: String) {
        providerManager.setCustomDomain(type, domain)
    }

    override suspend fun getProviderName(): String {
        return providerManager.getCurrentProvider().getProviderName()
    }

    override suspend fun getHomeFromAllProviders(): Map<ProviderType, Result<HomeContent>> {
        return providerManager.getHomeFromAllProviders()
    }
}
