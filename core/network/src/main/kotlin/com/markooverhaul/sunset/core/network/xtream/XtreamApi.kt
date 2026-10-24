package com.markooverhaul.sunset.core.network.xtream

import com.markooverhaul.sunset.core.model.ProviderError
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Minimal Xtream Codes client.
 *
 * The panel rejects the default `okhttp` user agent with a 403 (WAF rule), so every request sends
 * the app agent. Only `player_api.php` is used for the live slice; XMLTV and stream URLs come later.
 */
@Singleton
class XtreamApi @Inject constructor() {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(45, TimeUnit.SECONDS)
        .build()

    suspend fun auth(baseUrl: String, username: String, password: String): XtreamAuthResponse =
        decodeOrThrow(call(baseUrl, username, password, action = null))

    suspend fun call(
        baseUrl: String,
        username: String,
        password: String,
        action: String?,
    ): String = withContext(Dispatchers.IO) {
        val url = baseUrl.toHttpUrlOrNull()
            ?.newBuilder()
            ?.addPathSegment("player_api.php")
            ?.addQueryParameter("username", username)
            ?.addQueryParameter("password", password)
            ?.apply { if (action != null) addQueryParameter("action", action) }
            ?.build()
            ?: throw ProviderError.Network

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw statusError(response.code)
                response.body?.string()?.takeIf { it.isNotBlank() } ?: throw ProviderError.Server
            }
        } catch (error: ProviderError) {
            throw error
        } catch (_: IOException) {
            throw ProviderError.Network
        } catch (_: Throwable) {
            throw ProviderError.Server
        }
    }

    fun decodeOrThrow(body: String): XtreamAuthResponse = try {
        json.decodeFromString(XtreamAuthResponse.serializer(), body)
    } catch (_: Throwable) {
        throw ProviderError.Server
    }

    private fun statusError(code: Int): ProviderError = when {
        code == 401 -> ProviderError.InvalidCredentials
        code == 404 -> ProviderError.NotFound
        code == 429 -> ProviderError.RateLimited
        code >= 500 -> ProviderError.Server
        // 403 is the WAF, not a credential failure: surfacing "invalid credentials" would be a lie.
        else -> ProviderError.Server
    }

    companion object {
        const val USER_AGENT = "SunsetTV/2.0 (Android)"

        val json: Json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
            explicitNulls = false
        }

        /** Accepts `host:port`, `http://host:port` and trailing slashes. */
        fun normalizeBaseUrl(raw: String?): String? {
            val trimmed = raw?.trim()?.trimEnd('/') ?: return null
            if (trimmed.isEmpty()) return null
            val withScheme = when {
                trimmed.startsWith("http://", ignoreCase = true) -> trimmed
                trimmed.startsWith("https://", ignoreCase = true) -> trimmed
                else -> "http://$trimmed"
            }
            return withScheme.toHttpUrlOrNull()?.toString()?.trimEnd('/')
        }
    }
}
