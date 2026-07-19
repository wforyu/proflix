package com.proflix.feature.player

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private val NetflixRed = Color(0xFFE50914)
private val SurfaceDark = Color(0xFF141414)
private val SurfaceCard = Color(0xFF1A1A1A)
private val TextGray = Color(0xFF999999)

@SuppressLint("SourceLockedOrientationActivity")
@Composable
fun StreamPlayerScreen(
    episodeId: String,
    title: String,
    contentId: String,
    onBack: () -> Unit,
    onNavigateToEpisode: (String, String, String) -> Unit,
    viewModel: StreamPlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? Activity
    var isFullscreen by remember { mutableStateOf(false) }

    fun toggleFullscreen() {
        isFullscreen = !isFullscreen
        if (isFullscreen) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            @Suppress("DEPRECATION")
            activity?.window?.decorView?.systemUiVisibility = (
                android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                    or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            @Suppress("DEPRECATION")
            activity?.window?.decorView?.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_VISIBLE
        }
    }

    LaunchedEffect(episodeId) {
        viewModel.resolveStream(episodeId, title, contentId)
    }

    DisposableEffect(Unit) {
        onDispose {
            if (isFullscreen) {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                @Suppress("DEPRECATION")
                activity?.window?.decorView?.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_VISIBLE
            }
        }
    }

    if (uiState.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(
                    color = NetflixRed,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Loading...",
                    color = TextGray,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (!isFullscreen) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDark)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    if (isFullscreen) toggleFullscreen() else onBack()
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Text(
                    text = uiState.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                IconButton(onClick = { toggleFullscreen() }) {
                    Icon(
                        imageVector = Icons.Default.Fullscreen,
                        contentDescription = "Fullscreen",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        if (!isFullscreen && uiState.streamSources.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDark)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                uiState.streamSources.forEachIndexed { index, source ->
                    val isSelected = index == uiState.selectedSourceIndex
                    Text(
                        text = source.quality,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) Color.Black else TextGray,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (isSelected) NetflixRed
                                else Color.Transparent
                            )
                            .clickable { viewModel.selectSource(index) }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        if (uiState.streamUrl.isNotBlank()) {
            ExoPlayerContent(
                url = uiState.streamUrl,
                isFullscreen = isFullscreen,
                onToggleFullscreen = { toggleFullscreen() },
                onPlaybackEnded = { viewModel.onPlaybackEnded() }
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FullscreenExit,
                        contentDescription = null,
                        tint = TextGray.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No stream available",
                        color = TextGray,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (uiState.error != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = uiState.error ?: "",
                            color = TextGray.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        if (!isFullscreen) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDark)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.playPreviousEpisode() },
                        enabled = viewModel.hasPreviousEpisode()
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = if (viewModel.hasPreviousEpisode()) Color.White else TextGray.copy(alpha = 0.3f),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.playNextEpisode() },
                        enabled = viewModel.hasNextEpisode()
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = if (viewModel.hasNextEpisode()) Color.White else TextGray.copy(alpha = 0.3f),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Auto-play",
                        color = TextGray,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Switch(
                        checked = uiState.autoPlayNext,
                        onCheckedChange = { viewModel.toggleAutoPlayNext() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = NetflixRed,
                            uncheckedThumbColor = TextGray,
                            uncheckedTrackColor = SurfaceCard
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ExoPlayerContent(
    url: String,
    isFullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    onPlaybackEnded: () -> Unit
) {
    val context = LocalContext.current
    var playerError by remember { mutableStateOf<String?>(null) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        onPlaybackEnded()
                    }
                    if (playbackState == Player.STATE_READY) {
                        duration = this@apply.duration
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    playerError = error.message ?: "Playback error"
                }
            })
        }
    }

    LaunchedEffect(url) {
        playerError = null
        val mediaItem = MediaItem.fromUri(Uri.parse(url))
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    LaunchedEffect(exoPlayer) {
        while (isActive) {
            if (exoPlayer.isPlaying) {
                currentPosition = exoPlayer.currentPosition
                duration = exoPlayer.duration
            }
            delay(500)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    val showSkipOpening = duration > 0 && duration > 90_000 && currentPosition in 60_000..90_000
    val showSkipEnding = duration > 0 && duration > 180_000 && duration - currentPosition in 1_000..90_000 && currentPosition > duration / 2

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (!isFullscreen) Modifier.height(220.dp) else Modifier.fillMaxSize())
            .background(Color.Black)
    ) {
        AndroidView(
            factory = {
                PlayerView(it).apply {
                    player = exoPlayer
                    useController = true
                    setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                    controllerShowTimeoutMs = if (isFullscreen) 3000 else 4000
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                }
            },
            update = { playerView ->
                playerView.player = exoPlayer
                playerView.useController = true
            },
            modifier = Modifier.fillMaxSize()
        )

        if (playerError != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Playback failed",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = playerError ?: "",
                        color = TextGray,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = showSkipOpening,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 56.dp)
        ) {
            TextButton(
                onClick = {
                    exoPlayer.seekTo(90_000)
                },
                modifier = Modifier
                    .background(NetflixRed.copy(alpha = 0.9f), RoundedCornerShape(4.dp))
            ) {
                Text(
                    text = "Skip Opening",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        AnimatedVisibility(
            visible = showSkipEnding,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 56.dp)
        ) {
            TextButton(
                onClick = {
                    exoPlayer.seekTo(duration)
                },
                modifier = Modifier
                    .background(NetflixRed.copy(alpha = 0.9f), RoundedCornerShape(4.dp))
            ) {
                Text(
                    text = "Skip Ending",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}
