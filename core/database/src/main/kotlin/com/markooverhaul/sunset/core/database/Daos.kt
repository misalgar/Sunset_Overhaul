package com.markooverhaul.sunset.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavouriteDao {
    @Query("SELECT * FROM favourites ORDER BY addedAtUtcMs DESC")
    fun observeAll(): Flow<List<FavouriteEntity>>

    @Query("SELECT contentId FROM favourites")
    fun observeContentIds(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM favourites WHERE contentId = :contentId AND contentType = :contentType)")
    suspend fun isFavourite(contentId: String, contentType: String): Boolean

    @Query("SELECT * FROM favourites WHERE contentId = :contentId AND contentType = :contentType LIMIT 1")
    suspend fun find(contentId: String, contentType: String): FavouriteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: FavouriteEntity)

    @Query("DELETE FROM favourites WHERE contentId = :contentId AND contentType = :contentType")
    suspend fun delete(contentId: String, contentType: String)

    @Query("DELETE FROM favourites")
    suspend fun clear()
}

@Dao
interface WatchProgressDao {
    @Query(
        "SELECT * FROM watch_progress WHERE completed = 0 AND positionMs > 0 " +
            "ORDER BY updatedAtUtcMs DESC LIMIT :limit",
    )
    fun observeContinueWatching(limit: Int = 20): Flow<List<WatchProgressEntity>>

    @Query("SELECT * FROM watch_progress WHERE contentId = :contentId LIMIT 1")
    suspend fun find(contentId: String): WatchProgressEntity?

    @Query("SELECT * FROM watch_progress WHERE contentId = :contentId LIMIT 1")
    fun observe(contentId: String): Flow<WatchProgressEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: WatchProgressEntity)

    @Query("DELETE FROM watch_progress WHERE contentId = :contentId")
    suspend fun delete(contentId: String)

    @Query("DELETE FROM watch_progress")
    suspend fun clear()
}

@Dao
interface SearchHistoryDao {
    @Query("SELECT * FROM search_history ORDER BY lastUsedAtUtcMs DESC LIMIT :limit")
    fun observeRecent(limit: Int = 10): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE query = :query")
    suspend fun delete(query: String)

    @Query("DELETE FROM search_history")
    suspend fun clear()
}
