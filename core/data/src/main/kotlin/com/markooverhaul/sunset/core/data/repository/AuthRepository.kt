package com.markooverhaul.sunset.core.data.repository

import com.markooverhaul.sunset.core.data.session.SessionStore
import com.markooverhaul.sunset.core.model.ContentProvider
import com.markooverhaul.sunset.core.model.ProviderError
import com.markooverhaul.sunset.core.model.Session
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns authentication and the persisted session.
 *
 * The provider is injected, so debug runs against [com.markooverhaul.sunset.core.data.provider.FakeContentProvider]
 * and release runs against the fail-closed EZServer adapter. No credentials are logged.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val provider: ContentProvider,
    private val sessionStore: SessionStore,
) {
    val session: Flow<Session?> = sessionStore.session
    val rememberedUsername: Flow<String?> = sessionStore.rememberedUsername

    /** Restores a stored token. Returns null when there is no stored session. */
    suspend fun restore(): Session? {
        val stored = sessionStore.read() ?: return null
        return try {
            provider.restoreSession(stored.token).also { sessionStore.save(it) }
        } catch (error: ProviderError) {
            if (error is ProviderError.SessionExpired || error is ProviderError.AccountExpired) {
                sessionStore.clear()
            }
            throw error
        }
    }

    suspend fun login(
        username: String,
        password: String,
        rememberUsername: Boolean,
    ): Session {
        val session = provider.login(username.trim(), password)
        sessionStore.save(session)
        sessionStore.setRememberedUsername(if (rememberUsername) username.trim() else null)
        return session
    }

    /**
     * Debug-only convenience used by the bootstrap flow so the offline demo opens straight into
     * Home. Release builds never call this because the demo provider is not bound.
     */
    suspend fun startDemoSession(): Session =
        provider.login(DEMO_USERNAME, DEMO_PASSWORD).also { sessionStore.save(it) }

    suspend fun signOut() {
        sessionStore.clear()
    }

    suspend fun rememberUsername(username: String?) {
        sessionStore.setRememberedUsername(username)
    }

    private companion object {
        const val DEMO_USERNAME = "demo"
        const val DEMO_PASSWORD = "demo"
    }
}
