package com.proflix.provider.domain.model

data class Content(
    val id: String,
    val title: String,
    val poster: String,
    val banner: String,
    val description: String,
    val genres: List<String>,
    val year: String,
    val rating: String,
    val type: ContentType
)

enum class ContentType {
    ANIME, MOVIE, SERIES
}
