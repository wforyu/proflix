package com.proflix.provider.domain.model

data class Episode(
    val id: String,
    val contentId: String,
    val season: Int,
    val number: Int,
    val title: String,
    val description: String,
    val thumbnail: String,
    val duration: Long
)
