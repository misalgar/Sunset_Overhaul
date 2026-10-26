package com.markooverhaul.sunset.v2.di

import com.markooverhaul.sunset.core.model.ContentProvider
import com.markooverhaul.sunset.core.network.xtream.XtreamContentProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Release binding: the family/friends build talks to the production Xtream panel only. The server
 * address, username and password are entered at login and stored encrypted; no provider host or
 * credential is compiled into the APK.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ReleaseProviderModule {

    @Binds
    @Singleton
    abstract fun bindContentProvider(impl: XtreamContentProvider): ContentProvider
}
