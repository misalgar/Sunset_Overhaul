package com.markooverhaul.sunset.core.player.di

import com.markooverhaul.sunset.core.player.Media3PlaybackEngine
import com.markooverhaul.sunset.core.player.PlaybackEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class PlayerModule {

    /** Unscoped on purpose: each player screen owns (and releases) its engine. */
    @Binds
    abstract fun bindPlaybackEngine(impl: Media3PlaybackEngine): PlaybackEngine
}
