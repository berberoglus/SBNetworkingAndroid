package com.berberoglus.sbnetworking.auth

import com.berberoglus.sbnetworking.AuthTokenProvider
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Attaches the `apikey` and `Authorization: Bearer` headers from an [AuthTokenProvider]. Existing
 * request headers (e.g. ones set by the Retrofit `@Headers`/`@Header`) are preserved; auth headers
 * are only added when absent so an explicit per-call header wins.
 */
class AuthInterceptor(
    private val tokenProvider: AuthTokenProvider?,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val provider = tokenProvider ?: return chain.proceed(chain.request())
        val original = chain.request()
        val builder = original.newBuilder()
        provider.apiKey
            ?.takeIf { original.header(HEADER_API_KEY) == null }
            ?.let { builder.header(HEADER_API_KEY, it) }
        provider.accessToken
            ?.takeIf { original.header(HEADER_AUTHORIZATION) == null }
            ?.let { builder.header(HEADER_AUTHORIZATION, "Bearer $it") }
        return chain.proceed(builder.build())
    }

    private companion object {
        const val HEADER_API_KEY = "apikey"
        const val HEADER_AUTHORIZATION = "Authorization"
    }
}
