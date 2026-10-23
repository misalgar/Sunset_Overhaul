package com.markooverhaul.sunset.core.common

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object CommonModule {

    @Provides
    @Singleton
    fun provideDispatchers(): SunsetDispatchers = SunsetDispatchers(
        io = Dispatchers.IO,
        default = Dispatchers.Default,
    )

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(dispatchers: SunsetDispatchers): CoroutineDispatcher = dispatchers.io

    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(dispatchers: SunsetDispatchers): CoroutineDispatcher = dispatchers.default
}
