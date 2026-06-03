package com.berberoglus.sbnetworking

import com.berberoglus.sbnetworking.auth.AuthInterceptor
import com.berberoglus.sbnetworking.auth.TokenAuthenticator
import com.berberoglus.sbnetworking.internal.HttpClientCallAdapterFactory
import com.berberoglus.sbnetworking.internal.JSON_MEDIA_TYPE
import com.berberoglus.sbnetworking.internal.appJson
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

class HttpClientBuilder internal constructor(
    private val environment: HttpClientEnvironment,
) {
    private var authTokenProvider: AuthTokenProvider? = null
    private var enableLogging: Boolean = false
    private var connectTimeoutSeconds: Long = DEFAULT_TIMEOUT
    private var readTimeoutSeconds: Long = DEFAULT_TIMEOUT
    private var writeTimeoutSeconds: Long = DEFAULT_TIMEOUT
    private var baseOkHttpClient: OkHttpClient? = null

    fun authTokenProvider(provider: AuthTokenProvider?) = apply { this.authTokenProvider = provider }
    fun enableLogging(enabled: Boolean) = apply { this.enableLogging = enabled }
    fun connectTimeoutSeconds(seconds: Long) = apply { this.connectTimeoutSeconds = seconds }
    fun readTimeoutSeconds(seconds: Long) = apply { this.readTimeoutSeconds = seconds }
    fun writeTimeoutSeconds(seconds: Long) = apply { this.writeTimeoutSeconds = seconds }

    /** Inject a base OkHttpClient (the analog of injecting iOS `URLSession` for tests). */
    fun okHttpClient(client: OkHttpClient) = apply { this.baseOkHttpClient = client }

    fun build(): HttpClient {
        val baseUrl = environment.baseHttpUrl()
            ?: throw HttpClientError.InvalidUrl

        // One authenticator instance, shared by OkHttp and the call-adapter factory, so a failed
        // refresh captured during authentication can be surfaced by the call paths.
        val tokenAuthenticator = TokenAuthenticator(authTokenProvider)

        val clientBuilder = (baseOkHttpClient?.newBuilder() ?: OkHttpClient.Builder())
            .connectTimeout(connectTimeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(writeTimeoutSeconds, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor(authTokenProvider))
            .authenticator(tokenAuthenticator)

        if (enableLogging) {
            clientBuilder.addInterceptor(
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY },
            )
        }

        val okHttpClient = clientBuilder.build()
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            // Normalize Retrofit *Api failures into HttpClientError; must precede the default adapter.
            .addCallAdapterFactory(HttpClientCallAdapterFactory(tokenAuthenticator))
            .addConverterFactory(appJson.asConverterFactory(JSON_MEDIA_TYPE))
            .build()

        return HttpClient(environment, authTokenProvider, okHttpClient, retrofit, tokenAuthenticator)
    }

    private companion object {
        const val DEFAULT_TIMEOUT = 30L
    }
}
