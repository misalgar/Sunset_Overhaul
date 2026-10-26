package com.markooverhaul.sunset.core.database

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.markooverhaul.sunset.core.model.movieTitleKey
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

private val Migration3To4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE catalogue_movies ADD COLUMN titleKey TEXT")
        db.execSQL("ALTER TABLE catalogue_movies ADD COLUMN groupKey TEXT")
        db.execSQL("ALTER TABLE catalogue_movies ADD COLUMN isDefault INTEGER NOT NULL DEFAULT 1")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_catalogue_movies_titleKey` ON `catalogue_movies` (`titleKey`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_catalogue_movies_groupKey` ON `catalogue_movies` (`groupKey`)")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_catalogue_movies_isDefault_addedAtUtcMs` " +
                "ON `catalogue_movies` (`isDefault`, `addedAtUtcMs`)",
        )
        // Existing rows get their title key here; CatalogueVariantIndex fills groups and defaults
        // on the first launch after the upgrade, so no catalogue resync is needed.
        val update = db.compileStatement("UPDATE catalogue_movies SET titleKey = ? WHERE id = ?")
        db.query("SELECT id, title, year FROM catalogue_movies").use { cursor ->
            while (cursor.moveToNext()) {
                val year = if (cursor.isNull(2)) null else cursor.getInt(2)
                update.bindString(1, movieTitleKey(cursor.getString(1), year))
                update.bindString(2, cursor.getString(0))
                update.executeUpdateDelete()
                update.clearBindings()
            }
        }
        update.close()
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SunsetDatabase =
        Room.databaseBuilder(context, SunsetDatabase::class.java, "sunset.db")
            .addMigrations(Migration2To3, Migration3To4)
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
