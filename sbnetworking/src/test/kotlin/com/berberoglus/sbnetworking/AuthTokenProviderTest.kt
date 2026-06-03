package com.berberoglus.sbnetworking

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertThrows
import org.junit.Test

class AuthTokenProviderTest {

    @Test fun `default refresh throws Unauthorized`() {
        val provider = object : AuthTokenProvider {
            override val accessToken: String? = null
            override val refreshToken: String? = null
            override val apiKey: String? = null
            override fun updateTokens(accessToken: String, refreshToken: String) {}
        }
        assertThrows(HttpClientError.Unauthorized::class.java) {
            runBlocking { provider.refresh() }
        }
    }
}
