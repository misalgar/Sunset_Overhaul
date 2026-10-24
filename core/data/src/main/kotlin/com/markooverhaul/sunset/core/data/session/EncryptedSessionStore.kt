package com.markooverhaul.sunset.core.data.session

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.markooverhaul.sunset.core.model.Session
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

/**
 * Keystore-backed session storage.
 *
 * A real panel session carries the account credentials (Xtream Codes is stateless: every request
 * repeats the username and password), so the token is encrypted at rest with an AES-256-GCM key
 * that never leaves the Android Keystore. Usernames and server addresses stay readable because the
 * login screen needs them as defaults and they are not secrets on their own.
 */
@Singleton
class EncryptedSessionStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SessionStore {

    override val session: Flow<Session?> = dataStore.data.map { prefs ->
        val token = prefs[KEY_TOKEN] ?: return@map null
        val username = prefs[KEY_USERNAME] ?: return@map null
        val plain = decrypt(token) ?: return@map null
        Session(
            username = username,
            token = plain,
            expiresAtUtcMs = prefs[KEY_EXPIRES_AT]?.toLongOrNull(),
            isDemo = prefs[KEY_DEMO]?.toBooleanStrictOrNull() ?: true,
        )
    }

    override val rememberedUsername: Flow<String?> = dataStore.data.map { it[KEY_REMEMBERED_USERNAME] }

    override val lastServerUrl: Flow<String?> = dataStore.data.map { it[KEY_LAST_SERVER] }

    override suspend fun read(): Session? = session.firstOrNull()

    override suspend fun save(session: Session) {
        val encrypted = encrypt(session.token)
        dataStore.edit { prefs ->
            prefs[KEY_USERNAME] = session.username
            if (encrypted.isBlank()) prefs.remove(KEY_TOKEN) else prefs[KEY_TOKEN] = encrypted
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

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        val existing = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        if (existing != null) return existing.secretKey
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private fun encrypt(plain: String): String = runCatching {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val iv = cipher.iv
        val payload = iv + cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        PREFIX + Base64.encodeToString(payload, Base64.NO_WRAP)
    }.getOrElse {
        // Keystore unavailable (should not happen on API 23+): keep the session usable in memory
        // only by storing nothing rather than plaintext credentials.
        ""
    }

    private fun decrypt(stored: String): String? {
        if (!stored.startsWith(PREFIX)) return null
        return runCatching {
            val payload = Base64.decode(stored.removePrefix(PREFIX), Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val ivLength = 12
            val iv = payload.copyOfRange(0, ivLength)
            val body = payload.copyOfRange(ivLength, payload.size)
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(body), Charsets.UTF_8)
        }.getOrNull()
    }

    private companion object {
        const val KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "sunset_session_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val PREFIX = "enc:v1:"

        val KEY_USERNAME = stringPreferencesKey("session_username")
        val KEY_TOKEN = stringPreferencesKey("session_token")
        val KEY_DEMO = stringPreferencesKey("session_is_demo")
        val KEY_EXPIRES_AT = stringPreferencesKey("session_expires_at")
        val KEY_REMEMBERED_USERNAME = stringPreferencesKey("remembered_username")
        val KEY_LAST_SERVER = stringPreferencesKey("last_server_url")
    }
}
