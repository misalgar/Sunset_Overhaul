package com.markooverhaul.sunset.core.common

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Minimal, privacy-first logger.
 *
 * The provider contract is undocumented, and stream URLs may contain credentials, so this logger
 * never receives raw URLs, tokens or usernames. Call sites must pass already-redacted values.
 */
@Singleton
class SunsetLogger @Inject constructor() {

    fun d(tag: String, message: String) {
        if (isDebug()) android.util.Log.d(tag, message)
    }

    fun w(tag: String, message: String) {
        android.util.Log.w(tag, message)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        android.util.Log.e(tag, message, throwable)
    }

    private fun isDebug(): Boolean = try {
        Class.forName("com.markooverhaul.sunset.v2.BuildConfig")
            .getField("DEBUG")
            .getBoolean(null)
    } catch (_: Throwable) {
        false
    }
}
