package com.proflix.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.proflix.database.dao.ContinueWatchingDao
import com.proflix.database.dao.FavoriteDao
import com.proflix.database.dao.HistoryDao
import com.proflix.database.entity.ContinueWatchingEntity
import com.proflix.database.entity.FavoriteEntity
import com.proflix.database.entity.HistoryEntity

@Database(
    entities = [
        HistoryEntity::class,
        FavoriteEntity::class,
        ContinueWatchingEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ProFlixDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun continueWatchingDao(): ContinueWatchingDao
}
