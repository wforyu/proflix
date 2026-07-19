package com.proflix.provider.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class HomeDto(
    val hero: ContentDto? = null,
    val trending: List<ContentDto> = emptyList(),
    val latest: List<ContentDto> = emptyList(),
    val categories: Map<String, List<ContentDto>> = emptyMap()
)
