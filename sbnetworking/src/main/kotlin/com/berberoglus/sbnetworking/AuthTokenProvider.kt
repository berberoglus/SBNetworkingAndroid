package com.berberoglus.sbnetworking

/**
 * Provides auth tokens and optional refresh logic.
 *
 * When provided to an [HttpClient], the client:
 * - adds the `apikey` header from [apiKey] (if non-null),
 * - adds `Authorization: Bearer <accessToken>` (if non-null),
 * - on 401: calls [refresh] then retries the request once.
 *
 * Override [refresh] to enable 401 retry; the default throws [HttpClientError.Unauthorized].
 * Use the `apikey` header name (not `S-Api-Key`) for Supabase compatibility.
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

    fun updateTokens(accessToken: String, refreshToken: String)

    /** Default: no refresh support. Override to enable 401 retry. */
    suspend fun refresh() {
        throw HttpClientError.Unauthorized
    }
}
