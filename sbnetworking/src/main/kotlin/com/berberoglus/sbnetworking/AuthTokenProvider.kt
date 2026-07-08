package com.berberoglus.sbnetworking

/**
 * Provides auth tokens and optional refresh logic.
 *
 * When provided to an [HttpClient], the client:
 * - adds the [apiKey] value under every header name in [apiKeyHeaderNames] (if the key is non-null),
 * - adds `Authorization: Bearer <accessToken>` (if non-null),
 * - on 401: calls [refresh] then retries the request once.
 *
 * Override [refresh] to enable 401 retry; the default throws [HttpClientError.Unauthorized].
 *
 * Implementations must be safe for concurrent reads from OkHttp's background threads.
 *
 * **Non-recursion contract (Auth_Session_Security_Rules.md §3.2):** [refresh] MUST perform the
 * token-refresh network call through a **separate** Retrofit/OkHttp instance that does NOT install
 * this library's [com.berberoglus.sbnetworking.auth.TokenAuthenticator]. Refreshing through the same
 * authenticated [HttpClient] would let a 401 from the refresh endpoint recurse into the authenticator.
 * The `responseCount` guard in `TokenAuthenticator` bounds retries, but the separate-client rule is
 * the consumer's responsibility, since `refresh()` is consumer-supplied.
 */
interface AuthTokenProvider {
    val accessToken: String?
    val refreshToken: String?
    val apiKey: String?

    /**
     * Header name(s) the [apiKey] value is sent under. Every listed name receives the same value.
     * Gateways differ (`apikey`, `s-api-key`, `x-api-key`, ...) and some drop specific names when an
     * `Authorization` header is present, so a consuming app may list several to satisfy all its
     * environments. Which name(s) to use is a backend-specific decision that belongs to the app.
     * Default is the single `apikey` header (pre-configurable behavior).
     */
    val apiKeyHeaderNames: List<String> get() = listOf("apikey")

    fun updateTokens(accessToken: String, refreshToken: String)

    /** Default: no refresh support. Override to enable 401 retry. */
    suspend fun refresh() {
        throw HttpClientError.Unauthorized
    }
}
