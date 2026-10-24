package com.markooverhaul.sunset.core.database

import android.content.Context
import androidx.room.Room
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
    fun provideDatabase(@ApplicationContext context: Context): SunsetDatabase =
        Room.databaseBuilder(context, SunsetDatabase::class.java, "sunset.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideFavouriteDao(database: SunsetDatabase): FavouriteDao = database.favouriteDao()

    @Provides
    fun provideWatchProgressDao(database: SunsetDatabase): WatchProgressDao = database.watchProgressDao()

    @Provides
    fun provideSearchHistoryDao(database: SunsetDatabase): SearchHistoryDao = database.searchHistoryDao()

    @Provides
    fun provideCatalogueDao(database: SunsetDatabase): CatalogueDao = database.catalogueDao()
}
