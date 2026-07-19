package com.proflix.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.proflix.database.entity.ContinueWatchingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContinueWatchingDao {
    @Query("SELECT * FROM continue_watching ORDER BY lastUpdated DESC")
    fun getAllContinueWatching(): Flow<List<ContinueWatchingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContinueWatching(continueWatching: ContinueWatchingEntity)

    @Query("DELETE FROM continue_watching WHERE contentId = :contentId")
    suspend fun deleteContinueWatching(contentId: String)

    @Query("SELECT * FROM continue_watching WHERE contentId = :contentId")
    suspend fun getByContentId(contentId: String): ContinueWatchingEntity?
}
