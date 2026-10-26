package com.markooverhaul.sunset.core.data.repository

import com.markooverhaul.sunset.core.database.VersionPreferenceSource
import com.markooverhaul.sunset.core.model.VersionPreference
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Singleton
class SettingsVersionPreferenceSource @Inject constructor(
    private val settings: SettingsRepository,
) : VersionPreferenceSource {
    override val preference: Flow<VersionPreference> =
        settings.settings.map { it.versionPreference }.distinctUntilChanged()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class VersionPreferenceModule {
    @Binds
    abstract fun bindVersionPreferenceSource(
        impl: SettingsVersionPreferenceSource,
    ): VersionPreferenceSource
}
