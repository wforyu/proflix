package com.proflix.provider.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class EpisodeDto(
    val id: String,
    val content_id: String,
    val season: Int,
    val number: Int,
    val title: String,
    val description: String,
    val thumbnail: String,
    val duration: Long
) {
    fun toDomainModel(): com.proflix.provider.domain.model.Episode {
        return com.proflix.provider.domain.model.Episode(
            id = id,
            contentId = content_id,
            season = season,
            number = number,
            title = title,
            description = description,
            thumbnail = thumbnail,
            duration = duration
        )
    }
}
