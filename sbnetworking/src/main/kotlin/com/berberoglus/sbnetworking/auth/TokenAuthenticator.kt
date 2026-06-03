package com.berberoglus.sbnetworking.auth

import com.berberoglus.sbnetworking.AuthTokenProvider
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * Reproduces the iOS 401 → `refresh()` → retry-once behavior using OkHttp's idiomatic
 * `Authenticator`. On a 401 OkHttp calls [authenticate]; returning a request makes OkHttp retry.
 *
 * - No provider → return `null` (401 propagates → maps to [com.berberoglus.sbnetworking.HttpClientError.Unauthorized]).
 * - With provider → call [AuthTokenProvider.refresh]; on success, retry once with refreshed headers.
 *   A second 401 (responseCount >= 2) returns `null` (retry-once, exactly like iOS).
 * - If `refresh()` throws, the throwable is rethrown so the original caller observes it
 *   (iOS `test401WithRefreshThatThrowsPropagatesError`).
 *
 * The `synchronized(this)` block serializes refresh attempts within this client instance. Full
 * cross-call deduplication and the "refresh must not recurse through this authenticator"
 * requirement (Auth_Session_Security_Rules.md §3.2) are the [AuthTokenProvider]'s responsibility,
 * since refresh is consumer-supplied (faithful to iOS, where `refresh()` is opaque).
 */
class TokenAuthenticator(
    private val tokenProvider: AuthTokenProvider?,
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        val provider = tokenProvider ?: return null
        if (responseCount(response) >= MAX_ATTEMPTS) return null

        synchronized(this) {
            runBlocking { provider.refresh() }
            val builder = response.request.newBuilder()
            provider.apiKey?.let { builder.header("apikey", it) }
            provider.accessToken?.let { builder.header("Authorization", "Bearer $it") }
            return builder.build()
        }
    }

    private fun responseCount(response: Response): Int {
        var prior = response.priorResponse
        var count = 1
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    private companion object {
        const val MAX_ATTEMPTS = 2
    }
}
