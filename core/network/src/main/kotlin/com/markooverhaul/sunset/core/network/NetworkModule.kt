package com.markooverhaul.sunset.core.network

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import okhttp3.MediaType.Companion.toMediaType

/**
 * Networking scaffolding. Nothing here is wired to a provider and nothing runs in the demo build.
 *
 * Timeouts and redaction follow spec §12.8. A base URL is deliberately absent; [createRetrofit] is
 * called only after an approved integration supplies one.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideEzServerConfig(): EzServerConfig = EzServerConfig()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val redacting = HttpLoggingInterceptor { message ->
            // Never log query strings or auth headers; URLs can contain tokens.
            if (!message.contains("?") && !message.contains("token", ignoreCase = true)) {
                android.util.Log.d("SunsetHttp", message.take(200))
            }
        }.apply {
            level = HttpLoggingInterceptor.Level.BASIC
            redactHeader("Authorization")
            redactHeader("token")
        }
        return OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(redacting)
            .build()
    }

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    /** Only call with an approved base URL. Not used by the blocked adapter. */
    fun createRetrofit(baseUrl: String, client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
}
