package com.markooverhaul.sunset.core.database

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Migration2To3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE catalogue_movies ADD COLUMN language TEXT")
        db.execSQL("ALTER TABLE catalogue_movies ADD COLUMN quality TEXT")
        db.execSQL("ALTER TABLE catalogue_series ADD COLUMN language TEXT")
        db.execSQL("ALTER TABLE catalogue_series ADD COLUMN quality TEXT")
        // Force the catalogue to be rebuilt so existing rows gain the new provider variant fields.
        db.execSQL("DELETE FROM catalogue_meta")
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SunsetDatabase =
        Room.databaseBuilder(context, SunsetDatabase::class.java, "sunset.db")
            .addMigrations(Migration2To3)
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
