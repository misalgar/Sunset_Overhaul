package com.markooverhaul.sunset.core.common

import kotlinx.coroutines.CoroutineDispatcher

/**
 * Dispatcher holder created by [CommonModule]. Inject the holder or the qualified dispatchers;
 * do not reach for [kotlinx.coroutines.Dispatchers] directly in feature code.
 */
data class SunsetDispatchers(
    val io: CoroutineDispatcher,
    val default: CoroutineDispatcher,
)
