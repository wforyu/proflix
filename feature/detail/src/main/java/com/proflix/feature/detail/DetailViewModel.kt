package com.proflix.feature.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proflix.common.utils.Result
import com.proflix.database.dao.FavoriteDao
import com.proflix.database.entity.FavoriteEntity
import com.proflix.provider.domain.ProviderRepository
import com.proflix.provider.domain.model.Content
import com.proflix.provider.domain.model.Episode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val isLoading: Boolean = true,
    val content: Content? = null,
    val episodes: List<Episode> = emptyList(),
    val isFavorite: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val providerRepository: ProviderRepository,
    private val favoriteDao: FavoriteDao
) : ViewModel() {

    private val contentId: String = savedStateHandle["contentId"] ?: ""

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    init {
        loadDetail()
    }

    fun loadDetail() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            when (val result = providerRepository.getDetail(contentId)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        content = result.data,
                        isLoading = false
                    )
                    loadEpisodes()
                    checkFavorite()
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

    private fun loadEpisodes() {
        viewModelScope.launch {
            when (val result = providerRepository.getEpisodes(contentId)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(episodes = result.data.sortedBy { it.number })
                }
                is Result.Error -> {}
                is Result.Loading -> {}
            }
        }
    }

    private fun checkFavorite() {
        viewModelScope.launch {
            val isFav = favoriteDao.isFavorite(contentId)
            _uiState.value = _uiState.value.copy(isFavorite = isFav)
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            val content = _uiState.value.content ?: return@launch

            if (_uiState.value.isFavorite) {
                favoriteDao.deleteFavorite(content.id)
            } else {
                favoriteDao.insertFavorite(
                    FavoriteEntity(
                        animeId = content.id,
                        title = content.title,
                        poster = content.poster,
                        addedDate = System.currentTimeMillis()
                    )
                )
            }

            _uiState.value = _uiState.value.copy(isFavorite = !_uiState.value.isFavorite)
        }
    }
}
