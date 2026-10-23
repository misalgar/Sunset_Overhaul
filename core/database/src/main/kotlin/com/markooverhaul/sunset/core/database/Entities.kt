package com.markooverhaul.sunset.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favourites")
data class FavouriteEntity(
    @PrimaryKey val contentId: String,
    val contentType: String,
    val addedAtUtcMs: Long,
)

@Entity(tableName = "watch_progress")
data class WatchProgressEntity(
    @PrimaryKey val contentId: String,
    val contentType: String,
    val episodeId: String? = null,
    val title: String,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAtUtcMs: Long,
    val completed: Boolean = false,
)

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey val query: String,
    val lastUsedAtUtcMs: Long,
)
