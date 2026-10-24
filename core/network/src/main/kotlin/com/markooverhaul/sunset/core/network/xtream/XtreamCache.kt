package com.markooverhaul.sunset.core.network.xtream

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Small JSON disk cache so a relaunch does not re-download the channel list. Entries expire after
 * [DEFAULT_TTL_MS]; the catalogue is small for the live slice, VOD/series will need Room instead.
 */
@Singleton
class XtreamCache @Inject constructor(@ApplicationContext context: Context) {

    private val directory = File(context.cacheDir, "xtream")

    fun read(key: String, maxAgeMs: Long = DEFAULT_TTL_MS): String? {
        val file = File(directory, "$key.json")
        if (!file.isFile) return null
        if (System.currentTimeMillis() - file.lastModified() > maxAgeMs) return null
        return runCatching { file.readText() }.getOrNull()
    }

    fun write(key: String, body: String) {
        runCatching {
            directory.mkdirs()
            File(directory, "$key.json").writeText(body)
        }
    }

    fun remove(key: String) {
        runCatching { File(directory, "$key.json").delete() }
    }

    fun clear() {
        runCatching { directory.deleteRecursively() }
    }

    companion object {
        const val DEFAULT_TTL_MS = 6 * 60 * 60 * 1000L
    }
}
