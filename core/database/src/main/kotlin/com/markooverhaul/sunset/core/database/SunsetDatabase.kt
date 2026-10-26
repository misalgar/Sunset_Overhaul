package com.markooverhaul.sunset.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        FavouriteEntity::class,
        WatchProgressEntity::class,
        SearchHistoryEntity::class,
        CatalogueMetaEntity::class,
        CatalogueMovieEntity::class,
        CatalogueSeriesEntity::class,
        CatalogueEpisodeEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
abstract class SunsetDatabase : RoomDatabase() {
    abstract fun favouriteDao(): FavouriteDao
    abstract fun watchProgressDao(): WatchProgressDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun catalogueDao(): CatalogueDao
}
