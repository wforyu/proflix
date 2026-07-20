package com.proflix.provider.data.provider

import com.proflix.provider.domain.Provider
import com.proflix.provider.domain.ProviderType
import com.proflix.provider.domain.model.HomeContent
import com.proflix.common.utils.Result
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProviderManager @Inject constructor(
    private val anoboyProvider: AnoboyProvider,
    private val samehadakuProvider: SamehadakuProvider,
    private val oploverzProvider: OploverzProvider
) {
    private var currentType: ProviderType = ProviderType.ANOBOY

    fun getCurrentProvider(): Provider {
        return when (currentType) {
            ProviderType.ANOBOY -> anoboyProvider
            ProviderType.SAMEHADAKU -> samehadakuProvider
            ProviderType.OPLOVERZ -> oploverzProvider
        }
    }

    fun getProvider(type: ProviderType): Provider {
        return when (type) {
            ProviderType.ANOBOY -> anoboyProvider
            ProviderType.SAMEHADAKU -> samehadakuProvider
            ProviderType.OPLOVERZ -> oploverzProvider
        }
    }

    fun setProvider(type: ProviderType) {
        currentType = type
    }

    fun getCurrentType(): ProviderType = currentType

    fun getAvailableProviders(): List<ProviderType> = ProviderType.entries

    suspend fun getHomeFromAllProviders(): Map<ProviderType, Result<HomeContent>> {
        val results = mutableMapOf<ProviderType, Result<HomeContent>>()
        for (type in ProviderType.entries) {
            results[type] = try {
                getProvider(type).getHome()
            } catch (e: Exception) {
                Result.Error(e)
            }
        }
        return results
    }

    fun setCustomDomain(type: ProviderType, domain: String) {
        val url = domain.trimEnd('/')
        when (type) {
            ProviderType.ANOBOY -> AnoboyProvider.BASE_URL = url.ifBlank { "https://anoboy.pk" }
            ProviderType.SAMEHADAKU -> SamehadakuProvider.BASE_URL = url.ifBlank { "https://v2.samehadaku.how" }
            ProviderType.OPLOVERZ -> OploverzProvider.BASE_URL = url.ifBlank { "https://oploverz.site" }
        }
    }

    fun getDefaultDomain(type: ProviderType): String = when (type) {
        ProviderType.ANOBOY -> "https://anoboy.pk"
        ProviderType.SAMEHADAKU -> "https://v2.samehadaku.how"
        ProviderType.OPLOVERZ -> "https://oploverz.site"
    }
}
