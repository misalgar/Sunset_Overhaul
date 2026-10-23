package com.markooverhaul.sunset.v2.di

import com.markooverhaul.sunset.core.data.provider.FakeContentProvider
import com.markooverhaul.sunset.core.model.ContentProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Debug-only provider binding. The offline fixture provider makes the whole app demonstrable
 * without credentials, a network connection or any production endpoint.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DebugProviderModule {

    @Binds
    @Singleton
    abstract fun bindContentProvider(impl: FakeContentProvider): ContentProvider
}
