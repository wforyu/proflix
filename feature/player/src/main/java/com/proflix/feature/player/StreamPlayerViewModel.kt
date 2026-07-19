package com.proflix.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proflix.common.utils.Result
import com.proflix.provider.domain.ProviderRepository
import com.proflix.provider.domain.model.Episode
import com.proflix.provider.domain.model.StreamSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlayerUiState(
    val isLoading: Boolean = true,
    val streamUrl: String = "",
    val streamSources: List<StreamSource> = emptyList(),
    val selectedSourceIndex: Int = 0,
    val title: String = "",
    val episodes: List<Episode> = emptyList(),
    val currentEpisodeIndex: Int = -1,
    val autoPlayNext: Boolean = true,
    val error: String? = null,
    val playbackEnded: Boolean = false
)

@HiltViewModel
class StreamPlayerViewModel @Inject constructor(
    private val providerRepository: ProviderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    fun resolveStream(episodeId: String, title: String, contentId: String = "") {
        if (episodeId.isBlank()) {
            _uiState.value = PlayerUiState(
                isLoading = false,
                title = title,
                error = "No episode ID provided"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                title = title,
                error = null,
                playbackEnded = false
            )

            when (val result = providerRepository.getStream(episodeId)) {
                is Result.Success -> {
                    val sources = result.data
                    val firstUrl = sources.firstOrNull()?.url ?: ""
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        streamUrl = firstUrl,
                        streamSources = sources,
                        selectedSourceIndex = 0
                    )
                }
                is Result.Error -> {
                    val fallbackUrl = providerRepository.getDefaultDomain(
                        providerRepository.getCurrentProviderType()
                    ) + "/$episodeId"
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        streamUrl = fallbackUrl,
                        error = result.message
                    )
                }
                is Result.Loading -> {}
            }

            if (contentId.isNotBlank()) {
                loadEpisodes(contentId, episodeId)
            }
        }
    }

    private suspend fun loadEpisodes(contentId: String, currentEpisodeId: String) {
        when (val result = providerRepository.getEpisodes(contentId)) {
            is Result.Success -> {
                val episodes = result.data.sortedBy { it.number }
                val currentIndex = episodes.indexOfFirst { it.id == currentEpisodeId }
                _uiState.value = _uiState.value.copy(
                    episodes = episodes,
                    currentEpisodeIndex = currentIndex
                )
            }
            is Result.Error -> {}
            is Result.Loading -> {}
        }
    }

    fun selectSource(index: Int) {
        val source = _uiState.value.streamSources.getOrNull(index) ?: return
        _uiState.value = _uiState.value.copy(
            selectedSourceIndex = index,
            streamUrl = source.url
        )
    }

    fun onPlaybackEnded() {
        _uiState.value = _uiState.value.copy(playbackEnded = true)
        if (_uiState.value.autoPlayNext) {
            playNextEpisode()
        }
    }

    fun playNextEpisode() {
        val state = _uiState.value
        val episodes = state.episodes
        val currentIndex = state.currentEpisodeIndex
        if (currentIndex < 0 || currentIndex >= episodes.size - 1) return

        val nextEp = episodes[currentIndex + 1]
        viewModelScope.launch {
            _uiState.value = state.copy(
                isLoading = true,
                title = nextEp.title,
                streamUrl = "",
                streamSources = emptyList(),
                selectedSourceIndex = 0,
                currentEpisodeIndex = currentIndex + 1,
                playbackEnded = false,
                error = null
            )

            when (val result = providerRepository.getStream(nextEp.id)) {
                is Result.Success -> {
                    val sources = result.data
                    val firstUrl = sources.firstOrNull()?.url ?: ""
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        streamUrl = firstUrl,
                        streamSources = sources,
                        selectedSourceIndex = 0
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

    fun playPreviousEpisode() {
        val state = _uiState.value
        val episodes = state.episodes
        val currentIndex = state.currentEpisodeIndex
        if (currentIndex <= 0) return

        val prevEp = episodes[currentIndex - 1]
        viewModelScope.launch {
            _uiState.value = state.copy(
                isLoading = true,
                title = prevEp.title,
                streamUrl = "",
                streamSources = emptyList(),
                selectedSourceIndex = 0,
                currentEpisodeIndex = currentIndex - 1,
                playbackEnded = false,
                error = null
            )

            when (val result = providerRepository.getStream(prevEp.id)) {
                is Result.Success -> {
                    val sources = result.data
                    val firstUrl = sources.firstOrNull()?.url ?: ""
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        streamUrl = firstUrl,
                        streamSources = sources,
                        selectedSourceIndex = 0
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

    fun toggleAutoPlayNext() {
        _uiState.value = _uiState.value.copy(
            autoPlayNext = !_uiState.value.autoPlayNext
        )
    }

    fun hasNextEpisode(): Boolean {
        val state = _uiState.value
        return state.currentEpisodeIndex >= 0 &&
                state.currentEpisodeIndex < state.episodes.size - 1
    }

    fun hasPreviousEpisode(): Boolean {
        return _uiState.value.currentEpisodeIndex > 0
    }
}
