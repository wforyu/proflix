package com.proflix.provider.domain.model

data class StreamSource(
    val url: String,
    val quality: String,
    val format: String = "mp4"
)
