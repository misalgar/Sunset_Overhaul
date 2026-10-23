package com.markooverhaul.sunset.core.data.repository

import com.markooverhaul.sunset.core.database.FavouriteDao
import com.markooverhaul.sunset.core.database.FavouriteEntity
import com.markooverhaul.sunset.core.model.ContentType
import com.markooverhaul.sunset.core.model.Favourite
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Local favourites (My List). Provider-independent, works offline. */
@Singleton
class FavouritesRepository @Inject constructor(
    private val dao: FavouriteDao,
) {
    val favourites: Flow<List<Favourite>> = dao.observeAll().map { entities ->
        entities.mapNotNull { it.toDomain() }
    }

    val contentIds: Flow<Set<String>> = dao.observeContentIds().map { it.toSet() }

    suspend fun isFavourite(contentId: String, contentType: ContentType): Boolean =
        dao.isFavourite(contentId, contentType.name)

    /** Returns true when the item is now a favourite, false when it was removed. */
    suspend fun toggle(contentId: String, contentType: ContentType): Boolean {
        val existing = dao.find(contentId, contentType.name)
        return if (existing != null) {
            dao.delete(contentId, contentType.name)
            false
        } else {
            dao.upsert(
                FavouriteEntity(
                    contentId = contentId,
                    contentType = contentType.name,
                    addedAtUtcMs = System.currentTimeMillis(),
                ),
            )
            true
        }
    }

    suspend fun remove(contentId: String, contentType: ContentType) {
        dao.delete(contentId, contentType.name)
    }

    suspend fun clear() = dao.clear()

    private fun FavouriteEntity.toDomain(): Favourite? {
        val type = runCatching { ContentType.valueOf(contentType) }.getOrNull() ?: return null
        return Favourite(contentId = contentId, contentType = type, addedAtUtcMs = addedAtUtcMs)
    }
}
