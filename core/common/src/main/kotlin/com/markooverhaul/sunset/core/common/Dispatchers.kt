package com.markooverhaul.sunset.core.common

import javax.inject.Qualifier

/** Marks the application-wide IO dispatcher; never use [kotlinx.coroutines.Dispatchers.IO] inline. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

/** Marks the application-wide default dispatcher. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher
