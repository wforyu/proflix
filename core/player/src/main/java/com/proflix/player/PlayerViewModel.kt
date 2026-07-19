package com.proflix.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val mediaPlayer: MediaPlayer
) : ViewModel() {

    val playerState: StateFlow<PlayerState> = mediaPlayer.playerState

    private var progressJob: Job? = null

    fun setMediaUrl(url: String, title: String = "") {
        mediaPlayer.setMediaUrl(url, title)
        startProgressTracking()
    }

    fun play() = mediaPlayer.play()
    fun pause() = mediaPlayer.pause()
    fun seekTo(positionMs: Long) = mediaPlayer.seekTo(positionMs)
    fun setPlaybackSpeed(speed: Float) = mediaPlayer.setPlaybackSpeed(speed)

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (true) {
                mediaPlayer.updateProgress()
                delay(500)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        progressJob?.cancel()
        mediaPlayer.release()
    }
}
