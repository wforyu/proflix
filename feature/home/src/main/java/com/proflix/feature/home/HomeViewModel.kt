package com.proflix.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proflix.common.utils.Result
import com.proflix.database.datastore.PreferencesManager
import com.proflix.provider.domain.ProviderRepository
import com.proflix.provider.domain.ProviderType
import com.proflix.provider.domain.model.Content
import com.proflix.provider.domain.model.ContinueWatchingItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val heroContent: Content? = null,
    val trending: List<Content> = emptyList(),
    val latest: List<Content> = emptyList(),
    val continueWatching: List<ContinueWatchingItem> = emptyList(),
    val categories: Map<String, List<Content>> = emptyMap(),
    val currentProvider: ProviderType = ProviderType.ANOBOY,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val providerRepository: ProviderRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var loadJob: kotlinx.coroutines.Job? = null

    init {
        viewModelScope.launch {
            restoreProviderSettings()
            loadHome()
        }
    }

    private suspend fun restoreProviderSettings() {
        try {
            val savedProvider = try {
                preferencesManager.selectedProvider.first()
            } catch (_: Exception) { "ANOBOY" }
            val type = try { ProviderType.valueOf(savedProvider) } catch (_: Exception) { ProviderType.ANOBOY }
            providerRepository.switchProvider(type)

            val customAnoboy = try { preferencesManager.customDomainAnoboy.first() } catch (_: Exception) { "" }
            val customSamehadaku = try { preferencesManager.customDomainSamehadaku.first() } catch (_: Exception) { "" }
            val customOploverz = try { preferencesManager.customDomainOploverz.first() } catch (_: Exception) { "" }

            if (customAnoboy.isNotBlank()) providerRepository.setCustomDomain(ProviderType.ANOBOY, customAnoboy)
            if (customSamehadaku.isNotBlank()) providerRepository.setCustomDomain(ProviderType.SAMEHADAKU, customSamehadaku)
            if (customOploverz.isNotBlank()) providerRepository.setCustomDomain(ProviderType.OPLOVERZ, customOploverz)
        } catch (_: Exception) {}
    }

    fun loadHome() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                currentProvider = providerRepository.getCurrentProviderType()
            )

            try {
                val result = providerRepository.getHome()

                when (result) {
                    is Result.Success -> {
                        val home = result.data
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            heroContent = home.heroContent,
                            trending = home.trending,
                            latest = home.latest,
                            continueWatching = home.continueWatching,
                            categories = home.categories,
                            error = null
                        )
                    }
                    is Result.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = result.message ?: "Failed to load content"
                        )
                    }
                    is Result.Loading -> {}
                }
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
