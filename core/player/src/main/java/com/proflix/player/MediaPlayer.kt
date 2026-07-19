package com.proflix.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaPlayer @Inject constructor() {

    private var exoPlayer: ExoPlayer? = null
    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    @OptIn(UnstableApi::class)
    fun initialize(context: Context) {
        if (exoPlayer == null) {
            val trackSelector = DefaultTrackSelector(context)
            exoPlayer = ExoPlayer.Builder(context)
                .setTrackSelector(trackSelector)
                .build()
                .apply {
                    addListener(playerListener)
                }
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playerState.value = _playerState.value.copy(isPlaying = isPlaying)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            _playerState.value = _playerState.value.copy(
                isBuffering = playbackState == Player.STATE_BUFFERING
            )
            if (playbackState == Player.STATE_READY) {
                _playerState.value = _playerState.value.copy(
                    duration = exoPlayer?.duration ?: 0L
                )
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            _playerState.value = _playerState.value.copy(isBuffering = false)
        }
    }

    fun setMediaUrl(url: String, title: String = "") {
        exoPlayer?.let { player ->
            val mediaItem = MediaItem.Builder()
                .setUri(url)
                .build()
            player.setMediaItem(mediaItem)
            player.prepare()
            player.playWhenReady = true
        }
    }

    fun play() {
        exoPlayer?.play()
    }

    fun pause() {
        exoPlayer?.pause()
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
    }

    fun setPlaybackSpeed(speed: Float) {
        exoPlayer?.setPlaybackSpeed(speed)
        _playerState.value = _playerState.value.copy(playbackSpeed = speed)
    }

    fun getCurrentPosition(): Long {
        return exoPlayer?.currentPosition ?: 0L
    }

    fun getDuration(): Long {
        return exoPlayer?.duration ?: 0L
    }

    fun release() {
        exoPlayer?.removeListener(playerListener)
        exoPlayer?.release()
        exoPlayer = null
        _playerState.value = PlayerState()
    }

    fun updateProgress() {
        exoPlayer?.let { player ->
            _playerState.value = _playerState.value.copy(
                playbackPosition = player.currentPosition,
                duration = player.duration
            )
        }
    }
}
