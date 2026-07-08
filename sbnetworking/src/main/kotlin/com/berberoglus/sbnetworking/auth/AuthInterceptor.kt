package com.berberoglus.sbnetworking.auth

import com.berberoglus.sbnetworking.AuthTokenProvider
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Attaches the API-key header(s) and `Authorization: Bearer` from an [AuthTokenProvider]. The
 * [AuthTokenProvider.apiKey] value is sent under every name in [AuthTokenProvider.apiKeyHeaderNames]
 * (default `["apikey"]`). Existing request headers (e.g. ones set by the Retrofit
 * `@Headers`/`@Header`) are preserved; auth headers are only added when absent so an explicit
 * per-call header wins.
 */
class AuthInterceptor(
    private val tokenProvider: AuthTokenProvider?,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val provider = tokenProvider ?: return chain.proceed(chain.request())
        val original = chain.request()
        val builder = original.newBuilder()
        provider.apiKey?.let { key ->
            provider.apiKeyHeaderNames.forEach { name ->
                if (original.header(name) == null) builder.header(name, key)
            }
        }
        provider.accessToken
            ?.takeIf { original.header(HEADER_AUTHORIZATION) == null }
            ?.let { builder.header(HEADER_AUTHORIZATION, "Bearer $it") }
        return chain.proceed(builder.build())
    }

    private companion object {
        const val HEADER_AUTHORIZATION = "Authorization"
    }
}
