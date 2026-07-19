package com.proflix.provider.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class StreamDto(
    val url: String,
    val quality: String,
    val format: String
) {
    fun toDomainModel(): com.proflix.provider.domain.model.StreamSource {
        return com.proflix.provider.domain.model.StreamSource(
            url = url,
            quality = quality,
            format = format
        )
    }
}
