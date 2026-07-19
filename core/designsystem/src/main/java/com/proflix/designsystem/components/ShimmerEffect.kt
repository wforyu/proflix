package com.proflix.designsystem.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import com.proflix.designsystem.theme.DarkSurfaceVariant

fun Modifier.shimmerEffect(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_translate"
    )

    val shimmerBrush = remember(translateAnim) {
        Brush.linearGradient(
            colors = listOf(
                DarkSurfaceVariant.copy(alpha = 0.6f),
                DarkSurfaceVariant.copy(alpha = 0.2f),
                DarkSurfaceVariant.copy(alpha = 0.6f)
            ),
            start = Offset(translateAnim - 200f, translateAnim - 200f),
            end = Offset(translateAnim, translateAnim)
        )
    }

    background(shimmerBrush)
}

@Composable
fun ShimmerBox(modifier: Modifier = Modifier) {
    Box(modifier = modifier.shimmerEffect())
}
