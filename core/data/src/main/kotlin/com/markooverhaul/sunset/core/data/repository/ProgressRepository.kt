package com.markooverhaul.sunset.core.data.repository

import com.markooverhaul.sunset.core.database.WatchProgressDao
import com.markooverhaul.sunset.core.database.WatchProgressEntity
import com.markooverhaul.sunset.core.model.ContentType
import com.markooverhaul.sunset.core.model.WatchProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Continue Watching and resume positions. Backed by Room; no network involvement. */
@Singleton
class ProgressRepository @Inject constructor(
    private val dao: WatchProgressDao,
) {
    val continueWatching: Flow<List<WatchProgress>> = dao.observeContinueWatching()
        .map { entities -> entities.mapNotNull { it.toDomain() } }

    fun observe(contentId: String): Flow<WatchProgress?> =
        dao.observe(contentId).map { it?.toDomain() }

    suspend fun get(contentId: String): WatchProgress? = dao.find(contentId)?.toDomain()

    suspend fun save(
        contentId: String,
        contentType: ContentType,
        title: String,
        positionMs: Long,
        durationMs: Long,
        episodeId: String? = null,
    ) {
        if (durationMs <= 0L) return
        val clamped = positionMs.coerceIn(0L, durationMs)
        val completed = clamped.toFloat() / durationMs >= WatchProgress.COMPLETE_FRACTION
        dao.upsert(
            WatchProgressEntity(
                contentId = contentId,
                contentType = contentType.name,
                episodeId = episodeId,
                title = title,
                positionMs = if (completed) durationMs else clamped,
                durationMs = durationMs,
                updatedAtUtcMs = System.currentTimeMillis(),
                completed = completed,
            ),
        )
    }

    suspend fun clear(contentId: String) = dao.delete(contentId)

    suspend fun clearAll() = dao.clear()

    private fun WatchProgressEntity.toDomain(): WatchProgress {
        val type = runCatching { ContentType.valueOf(contentType) }.getOrDefault(ContentType.MOVIE)
        return WatchProgress(
            contentId = contentId,
            contentType = type,
            episodeId = episodeId,
            title = title,
            positionMs = positionMs,
            durationMs = durationMs,
            updatedAtUtcMs = updatedAtUtcMs,
            completed = completed,
        )
    }
}
