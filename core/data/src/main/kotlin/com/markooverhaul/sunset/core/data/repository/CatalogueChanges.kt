package com.markooverhaul.sunset.core.data.repository

import com.markooverhaul.sunset.core.database.CatalogueVariantIndex
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.StateFlow

@Singleton
class CatalogueChanges @Inject constructor(
    index: CatalogueVariantIndex,
) {
    /** Changes when a visible default version changes, so open lists can reload. */
    val revisions: StateFlow<Int> = index.revision
}
