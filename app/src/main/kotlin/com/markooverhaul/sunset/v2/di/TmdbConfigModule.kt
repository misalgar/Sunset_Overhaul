package com.markooverhaul.sunset.v2.di

import com.markooverhaul.sunset.core.network.tmdb.TmdbCredentials
import com.markooverhaul.sunset.v2.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** TMDB credentials come from the machine-local `local.properties`; blank values disable it. */
@Module
@InstallIn(SingletonComponent::class)
object TmdbConfigModule {

    @Provides
    @Singleton
    fun provideTmdbCredentials(): TmdbCredentials = TmdbCredentials(
        readToken = BuildConfig.TMDB_READ_TOKEN.takeIf { it.isNotBlank() },
        apiKey = BuildConfig.TMDB_API_KEY.takeIf { it.isNotBlank() },
    )
}
