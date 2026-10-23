package com.markooverhaul.sunset.core.data.repository

import com.markooverhaul.sunset.core.database.SearchHistoryDao
import com.markooverhaul.sunset.core.database.SearchHistoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class RecentSearch(val query: String, val lastUsedAtUtcMs: Long)

@Singleton
class SearchHistoryRepository @Inject constructor(
    private val dao: SearchHistoryDao,
) {
    val recent: Flow<List<RecentSearch>> = dao.observeRecent().map { entities ->
        entities.map { RecentSearch(it.query, it.lastUsedAtUtcMs) }
    }

    suspend fun record(query: String) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return
        dao.upsert(SearchHistoryEntity(query = trimmed, lastUsedAtUtcMs = System.currentTimeMillis()))
    }

    suspend fun remove(query: String) = dao.delete(query)

    suspend fun clear() = dao.clear()
}
