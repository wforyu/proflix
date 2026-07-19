package com.proflix.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.proflix.database.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY addedDate DESC")
    fun getAllFavorites(): Flow<List<FavoriteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE animeId = :animeId")
    suspend fun deleteFavorite(animeId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE animeId = :animeId)")
    suspend fun isFavorite(animeId: String): Boolean

    @Query("SELECT * FROM favorites WHERE animeId = :animeId")
    suspend fun getFavoriteById(animeId: String): FavoriteEntity?
}
