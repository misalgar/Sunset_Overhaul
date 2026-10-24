package com.markooverhaul.sunset.v2.di

import com.markooverhaul.sunset.core.data.provider.RoutingContentProvider
import com.markooverhaul.sunset.core.model.ContentProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Debug-only provider binding.
 *
 * The router keeps the offline demo catalogue available (empty server address on the login form)
 * while allowing a real Xtream panel to be used for on-device integration testing. Release builds
 * still bind the fail-closed adapter.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DebugProviderModule {

    @Binds
    @Singleton
    abstract fun bindContentProvider(impl: RoutingContentProvider): ContentProvider
}
