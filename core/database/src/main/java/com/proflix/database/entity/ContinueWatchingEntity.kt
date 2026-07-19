package com.proflix.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "continue_watching")
data class ContinueWatchingEntity(
    @PrimaryKey
    val contentId: String,
    val episodeId: String,
    val title: String,
    val poster: String,
    val episodeTitle: String,
    val watchPosition: Long,
    val duration: Long,
    val progress: Float,
    val lastUpdated: Long
)
