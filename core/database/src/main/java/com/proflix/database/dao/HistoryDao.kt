package com.proflix.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.proflix.database.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY lastWatched DESC")
    fun getAllHistory(): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: HistoryEntity)

    @Query("DELETE FROM history WHERE contentId = :contentId")
    suspend fun deleteHistory(contentId: String)

    @Query("DELETE FROM history")
    suspend fun clearHistory()

    @Query("SELECT * FROM history WHERE contentId = :contentId")
    suspend fun getHistoryById(contentId: String): HistoryEntity?
}
