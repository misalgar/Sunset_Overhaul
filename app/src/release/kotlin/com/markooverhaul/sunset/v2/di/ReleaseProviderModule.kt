package com.markooverhaul.sunset.v2.di

import com.markooverhaul.sunset.core.model.ContentProvider
import com.markooverhaul.sunset.core.network.EzServerContentProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Release binding. The adapter fails closed with "Service not configured"; no fake provider is
 * bound and no synthetic session can be started in a release build.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ReleaseProviderModule {

    @Binds
    @Singleton
    abstract fun bindContentProvider(impl: EzServerContentProvider): ContentProvider
}
