package com.proflix.player

import androidx.media3.common.C

data class PlayerState(
    val isPlaying: Boolean = false,
    val playbackPosition: Long = 0L,
    val duration: Long = 0L,
    val playbackSpeed: Float = 1f,
    val isBuffering: Boolean = false,
    val volume: Float = 1f,
    val isMuted: Boolean = false,
    val isFullscreen: Boolean = false
) {
    val progress: Float
        get() = if (duration > 0) (playbackPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f

    val bufferedProgress: Float
        get() = if (duration > 0) (duration.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f
}

enum class PlaybackSpeed(val value: Float, val label: String) {
    NORMAL(1f, "1x"),
    FAST(1.5f, "1.5x"),
    FASTER(2f, "2x"),
    SLOW(0.5f, "0.5x")
}

object PlayerConstants {
    val SPEEDS = PlaybackSpeed.entries
    val DEFAULT_SPEED = PlaybackSpeed.NORMAL
}
