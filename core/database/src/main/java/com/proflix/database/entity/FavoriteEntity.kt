package com.proflix.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey
    val animeId: String,
    val title: String,
    val poster: String,
    val addedDate: Long
)
