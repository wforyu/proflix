package com.proflix.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "history",
    primaryKeys = ["contentId", "episodeId"]
)
data class HistoryEntity(
    val contentId: String,
    val title: String,
    val poster: String,
    val episodeId: String,
    val episodeTitle: String,
    val position: Long,
    val duration: Long,
    val lastWatched: Long
)
