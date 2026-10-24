package com.markooverhaul.sunset.core.data.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.markooverhaul.sunset.core.model.Session
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persistence boundary for the signed-in session.
 *
 * The foundation milestone only ever stores a synthetic demo session. A real provider token must
 * NOT be written here: when the production adapter is implemented, replace this with a
 * Keystore-backed implementation (see docs/PROVIDER_INTEGRATION.md). Storing production tokens in
 * plaintext DataStore would violate spec §15.1.
 */
interface SessionStore {
    val session: Flow<Session?>
    val rememberedUsername: Flow<String?>
    val lastServerUrl: Flow<String?>

    suspend fun read(): Session?
    suspend fun save(session: Session)
    suspend fun clear()
    suspend fun setRememberedUsername(username: String?)
    suspend fun setLastServerUrl(url: String?)
}

@Singleton
class DataStoreSessionStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SessionStore {

    override val session: Flow<Session?> = dataStore.data.map { prefs ->
        val token = prefs[KEY_TOKEN]
        val username = prefs[KEY_USERNAME]
        if (token.isNullOrBlank() || username.isNullOrBlank()) {
            null
        } else {
            Session(
                username = username,
                token = token,
                expiresAtUtcMs = prefs[KEY_EXPIRES_AT]?.toLongOrNull(),
                isDemo = prefs[KEY_DEMO]?.toBooleanStrictOrNull() ?: true,
            )
        }
    }

    override val rememberedUsername: Flow<String?> = dataStore.data.map { it[KEY_REMEMBERED_USERNAME] }

    override val lastServerUrl: Flow<String?> = dataStore.data.map { it[KEY_LAST_SERVER] }

    override suspend fun read(): Session? = session.firstOrNull()

    override suspend fun save(session: Session) {
        dataStore.edit { prefs ->
            prefs[KEY_USERNAME] = session.username
            prefs[KEY_TOKEN] = session.token
            prefs[KEY_DEMO] = session.isDemo.toString()
            session.expiresAtUtcMs?.let { prefs[KEY_EXPIRES_AT] = it.toString() }
                ?: prefs.remove(KEY_EXPIRES_AT)
        }
    }

    override suspend fun clear() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_USERNAME)
            prefs.remove(KEY_TOKEN)
            prefs.remove(KEY_DEMO)
            prefs.remove(KEY_EXPIRES_AT)
        }
    }

    override suspend fun setRememberedUsername(username: String?) {
        dataStore.edit { prefs ->
            if (username.isNullOrBlank()) prefs.remove(KEY_REMEMBERED_USERNAME)
            else prefs[KEY_REMEMBERED_USERNAME] = username.trim()
        }
    }

    override suspend fun setLastServerUrl(url: String?) {
        dataStore.edit { prefs ->
            if (url.isNullOrBlank()) prefs.remove(KEY_LAST_SERVER)
            else prefs[KEY_LAST_SERVER] = url.trim()
        }
    }

    private companion object {
        val KEY_USERNAME = stringPreferencesKey("session_username")
        val KEY_TOKEN = stringPreferencesKey("session_token")
        val KEY_DEMO = stringPreferencesKey("session_is_demo")
        val KEY_EXPIRES_AT = stringPreferencesKey("session_expires_at")
        val KEY_REMEMBERED_USERNAME = stringPreferencesKey("remembered_username")
        val KEY_LAST_SERVER = stringPreferencesKey("last_server_url")
    }
}
