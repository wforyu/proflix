package com.proflix.provider.domain.model

data class HomeContent(
    val heroContent: Content?,
    val trending: List<Content>,
    val latest: List<Content>,
    val continueWatching: List<ContinueWatchingItem>,
    val categories: Map<String, List<Content>>
)

data class ContinueWatchingItem(
    val content: Content,
    val episode: Episode,
    val progress: Float,
    val lastWatched: Long
)
