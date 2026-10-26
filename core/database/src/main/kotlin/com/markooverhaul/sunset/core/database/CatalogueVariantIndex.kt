package com.markooverhaul.sunset.core.database

import com.markooverhaul.sunset.core.model.VariantCandidate
import com.markooverhaul.sunset.core.model.VersionPreference
import com.markooverhaul.sunset.core.model.assignVariantGroups
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** The viewer's preferred version. Implemented by the settings layer, which owns the value. */
interface VersionPreferenceSource {
    val preference: Flow<VersionPreference>
}

/**
 * Keeps each movie's group and default version in step with the catalogue and the preference.
 *
 * Syncs call [rebuild] directly. Enrichment and preference changes call [requestRebuild], which is
 * debounced so a burst of TMDB matches becomes one pass over the table. Only rows whose group or
 * default flag actually changed are written.
 */
@Singleton
class CatalogueVariantIndex @Inject constructor(
    private val dao: CatalogueDao,
    private val preferences: VersionPreferenceSource,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val requests = Channel<Unit>(Channel.CONFLATED)
    private val mutex = Mutex()
    private val _revision = MutableStateFlow(0)

    /** Increases only when a visible default flips, so open lists don't reload on every TMDB match. */
    val revision: StateFlow<Int> = _revision.asStateFlow()

    init {
        scope.launch {
            preferences.preference.collect { requestRebuild() }
        }
        scope.launch {
            while (true) {
                requests.receive()
                delay(REBUILD_DEBOUNCE_MS)
                runCatching { rebuild() }
            }
        }
    }

    fun requestRebuild() {
        requests.trySend(Unit)
    }

    suspend fun rebuild() {
        mutex.withLock {
            val preference = preferences.preference.first()
            val rows = dao.movieVariantRows()
            val states = assignVariantGroups(
                candidates = rows.map { row ->
                    VariantCandidate(
                        id = row.id,
                        titleKey = row.titleKey.orEmpty(),
                        tmdbId = row.tmdbId,
                        language = row.language,
                        quality = row.quality,
                        addedAtUtcMs = row.addedAtUtcMs,
                    )
                },
                preference = preference,
            )
            val current = rows.associateBy { it.id }
            val changed = states.mapNotNull { state ->
                val row = current.getValue(state.id)
                if (row.groupKey == state.groupKey && row.isDefault == state.isDefault) {
                    null
                } else {
                    MovieVariantUpdate(state.id, state.groupKey, state.isDefault)
                }
            }
            if (changed.isNotEmpty()) {
                dao.applyMovieVariants(changed)
                val visibleChange = changed.any { update ->
                    current.getValue(update.id).isDefault != update.isDefault
                }
                if (visibleChange) _revision.update { it + 1 }
            }
        }
    }

    private companion object {
        const val REBUILD_DEBOUNCE_MS = 1_500L
    }
}
