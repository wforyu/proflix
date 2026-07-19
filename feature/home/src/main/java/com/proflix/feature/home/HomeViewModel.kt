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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

            when (val result = providerRepository.getHome()) {
                is Result.Success -> {
                    val home = result.data
                    _uiState.value = HomeUiState(
                        isLoading = false,
                        heroContent = home.heroContent,
                        trending = home.trending,
                        latest = home.latest,
                        continueWatching = home.continueWatching,
                        categories = home.categories,
                        currentProvider = providerRepository.getCurrentProviderType()
                    )
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
                is Result.Loading -> {}
            }
        }
    }

    fun switchProvider(type: ProviderType) {
        providerRepository.switchProvider(type)
        loadHome()
    }
}
