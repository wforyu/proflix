package com.proflix.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proflix.common.utils.Result
import com.proflix.provider.domain.ProviderRepository
import com.proflix.provider.domain.model.Content
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val results: List<Content> = emptyList(),
    val isLoading: Boolean = false,
    val recentSearches: List<String> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val providerRepository: ProviderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        searchJob?.cancel()
        if (query.isNotBlank()) {
            searchJob = viewModelScope.launch {
                delay(300)
                search(query)
                if (query.isNotBlank() && !_uiState.value.recentSearches.contains(query)) {
                    _uiState.value = _uiState.value.copy(
                        recentSearches = listOf(query) + _uiState.value.recentSearches.take(9)
                    )
                }
            }
        } else {
            _uiState.value = _uiState.value.copy(results = emptyList(), isLoading = false)
        }
    }

    private suspend fun search(query: String) {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        when (val result = providerRepository.search(query)) {
            is Result.Success -> {
                _uiState.value = _uiState.value.copy(
                    results = result.data,
                    isLoading = false
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

    fun onSearch(query: String) {
        if (query.isNotBlank() && !_uiState.value.recentSearches.contains(query)) {
            _uiState.value = _uiState.value.copy(
                recentSearches = listOf(query) + _uiState.value.recentSearches.take(9)
            )
        }
    }

    fun clearSearch() {
        _uiState.value = SearchUiState(
            recentSearches = _uiState.value.recentSearches
        )
    }

    fun removeRecentSearch(search: String) {
        _uiState.value = _uiState.value.copy(
            recentSearches = _uiState.value.recentSearches - search
        )
    }
}
