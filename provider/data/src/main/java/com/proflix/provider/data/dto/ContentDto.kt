package com.proflix.provider.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class ContentDto(
    val id: String,
    val title: String,
    val poster: String,
    val banner: String,
    val description: String,
    val genres: List<String>,
    val year: String,
    val rating: String,
    val type: String
) {
    fun toDomainModel(): com.proflix.provider.domain.model.Content {
        return com.proflix.provider.domain.model.Content(
            id = id,
            title = title,
            poster = poster,
            banner = banner,
            description = description,
            genres = genres,
            year = year,
            rating = rating,
            type = com.proflix.provider.domain.model.ContentType.valueOf(type.uppercase())
        )
    }
}
