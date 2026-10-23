package com.markooverhaul.sunset.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class ReduceMotionSetting { SYSTEM, ON, OFF }

data class AppSettings(
    val autoplayNext: Boolean = true,
    val subtitlesEnabled: Boolean = true,
    val audioLanguage: String? = null,
    /** BCP-47 tag, or null to follow the system locale. Only `en` and `es` are supported. */
    val languageTag: String? = null,
    val reduceMotion: ReduceMotionSetting = ReduceMotionSetting.SYSTEM,
)

/**
 * Lightweight preferences. Only values the foundation milestone actually uses are exposed; rows
 * that are not wired yet are clearly labelled as placeholders in Settings.
 */
@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            autoplayNext = prefs[KEY_AUTOPLAY] ?: true,
            subtitlesEnabled = prefs[KEY_SUBTITLES] ?: true,
            audioLanguage = prefs[KEY_AUDIO_LANGUAGE],
            languageTag = prefs[KEY_LANGUAGE],
            reduceMotion = prefs[KEY_REDUCE_MOTION]?.let { stored ->
                ReduceMotionSetting.entries.firstOrNull { it.name == stored }
            } ?: ReduceMotionSetting.SYSTEM,
        )
    }

    suspend fun setAutoplayNext(enabled: Boolean) {
        dataStore.edit { it[KEY_AUTOPLAY] = enabled }
    }

    suspend fun setSubtitlesEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_SUBTITLES] = enabled }
    }

    suspend fun setAudioLanguage(tag: String?) {
        dataStore.edit { prefs ->
            if (tag.isNullOrBlank()) prefs.remove(KEY_AUDIO_LANGUAGE) else prefs[KEY_AUDIO_LANGUAGE] = tag
        }
    }

    suspend fun setLanguageTag(tag: String?) {
        dataStore.edit { prefs ->
            if (tag.isNullOrBlank()) prefs.remove(KEY_LANGUAGE) else prefs[KEY_LANGUAGE] = tag
        }
    }

    suspend fun setReduceMotion(setting: ReduceMotionSetting) {
        dataStore.edit { it[KEY_REDUCE_MOTION] = setting.name }
    }

    private companion object {
        val KEY_AUTOPLAY = booleanPreferencesKey("autoplay_next")
        val KEY_SUBTITLES = booleanPreferencesKey("subtitles_enabled")
        val KEY_AUDIO_LANGUAGE = stringPreferencesKey("audio_language")
        val KEY_LANGUAGE = stringPreferencesKey("app_language")
        val KEY_REDUCE_MOTION = stringPreferencesKey("reduce_motion")
    }
}
