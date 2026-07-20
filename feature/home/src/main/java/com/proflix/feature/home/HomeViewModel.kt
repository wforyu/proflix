package com.proflix.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proflix.common.utils.Result
import com.proflix.provider.domain.ProviderRepository
import com.proflix.provider.domain.ProviderType
import com.proflix.provider.domain.model.Content
import com.proflix.provider.domain.model.ContinueWatchingItem
import com.proflix.provider.domain.model.HomeContent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProviderContent(
    val provider: ProviderType,
    val trending: List<Content>,
    val latest: List<Content>,
    val categories: Map<String, List<Content>>
)

data class HomeUiState(
    val isLoading: Boolean = true,
    val heroContent: Content? = null,
    val trending: List<Content> = emptyList(),
    val latest: List<Content> = emptyList(),
    val continueWatching: List<ContinueWatchingItem> = emptyList(),
    val categories: Map<String, List<Content>> = emptyMap(),
    val providerContents: List<ProviderContent> = emptyList(),
    val currentProvider: ProviderType = ProviderType.ANOBOY,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val providerRepository: ProviderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHome()
    }

    fun loadHome() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                currentProvider = providerRepository.getCurrentProviderType()
            )

            try {
                val allResults = providerRepository.getHomeFromAllProviders()

                val providerContents = mutableListOf<ProviderContent>()
                var heroContent: Content? = null
                val allTrending = mutableListOf<Content>()
                val allLatest = mutableListOf<Content>()
                val allCategories = mutableMapOf<String, List<Content>>()

                for ((type, result) in allResults) {
                    if (result is Result.Success) {
                        val home = result.data
                        val providerTrending = home.trending
                        val providerLatest = home.latest

                        providerContents.add(
                            ProviderContent(
                                provider = type,
                                trending = providerTrending,
                                latest = providerLatest,
                                categories = home.categories
                            )
                        )

                        if (heroContent == null && home.heroContent != null) {
                            heroContent = home.heroContent
                        }

                        if (providerTrending.isNotEmpty()) {
                            allTrending.addAll(providerTrending)
                        }
                        if (providerLatest.isNotEmpty()) {
                            allLatest.addAll(providerLatest)
                        }

                        for ((key, value) in home.categories) {
                            val prefixedKey = "${type.displayName} - $key"
                            if (value.isNotEmpty()) {
                                allCategories[prefixedKey] = value
                            }
                        }
                    }
                }

                _uiState.value = HomeUiState(
                    isLoading = false,
                    heroContent = heroContent,
                    trending = allTrending,
                    latest = allLatest,
                    continueWatching = emptyList(),
                    categories = allCategories,
                    providerContents = providerContents,
                    currentProvider = providerRepository.getCurrentProviderType(),
                    error = if (providerContents.isEmpty()) "Failed to load from all providers" else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }

    fun switchProvider(type: ProviderType) {
        providerRepository.switchProvider(type)
        loadHome()
    }
}
