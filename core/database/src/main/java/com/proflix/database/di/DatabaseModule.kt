package com.proflix.database.di

import android.content.Context
import androidx.room.Room
import com.proflix.common.utils.Constants
import com.proflix.database.ProFlixDatabase
import com.proflix.database.dao.ContinueWatchingDao
import com.proflix.database.dao.FavoriteDao
import com.proflix.database.dao.HistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ProFlixDatabase {
        return Room.databaseBuilder(
            context,
            ProFlixDatabase::class.java,
            Constants.DATABASE_NAME
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideHistoryDao(database: ProFlixDatabase): HistoryDao = database.historyDao()

    @Provides
    fun provideFavoriteDao(database: ProFlixDatabase): FavoriteDao = database.favoriteDao()

    @Provides
    fun provideContinueWatchingDao(database: ProFlixDatabase): ContinueWatchingDao = database.continueWatchingDao()
}
