package com.berberoglus.sbnetworking.support

import com.berberoglus.sbnetworking.AuthTokenProvider

/** Test double for [AuthTokenProvider] with configurable refresh success/throw. */
class FakeAuthTokenProvider(
    override var accessToken: String?,
    override var refreshToken: String?,
    override var apiKey: String?,
    override val apiKeyHeaderNames: List<String> = listOf("apikey"),
    private val supportsRefresh: Boolean = false,
    private val refreshError: Throwable? = null,
) : AuthTokenProvider {
    var updateTokensCallCount = 0
        private set
    var refreshCallCount = 0
        private set

    override fun updateTokens(accessToken: String, refreshToken: String) {
        this.accessToken = accessToken
        this.refreshToken = refreshToken
        updateTokensCallCount++
    }

    override suspend fun refresh() {
        refreshCallCount++
        refreshError?.let { throw it }
        if (!supportsRefresh) throw com.berberoglus.sbnetworking.HttpClientError.Unauthorized
        updateTokens(accessToken = "new_token", refreshToken = "new_refresh")
    }
}
