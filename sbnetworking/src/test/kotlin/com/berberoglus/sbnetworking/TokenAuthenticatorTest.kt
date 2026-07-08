package com.berberoglus.sbnetworking

import com.berberoglus.sbnetworking.support.FakeAuthTokenProvider
import com.berberoglus.sbnetworking.support.TestApi
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class TokenAuthenticatorTest {

    private lateinit var server: MockWebServer

    @Before fun setUp() { server = MockWebServer().also { it.start() } }
    @After fun tearDown() { server.shutdown() }

    private fun apiWith(provider: AuthTokenProvider?): TestApi {
        val client = HttpClient.Builder(HttpClientEnvironment(scheme = "http", baseUrl = server.hostName + ":" + server.port))
            .authTokenProvider(provider)
            .build()
        return client.retrofit.create(TestApi::class.java)
    }

    @Test fun `401 then refresh then retry succeeds`() = runTest {
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"error":"unauthorized"}"""))
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"resultCount":1}"""))
        val provider = FakeAuthTokenProvider("old_token", "refresh_xyz", "key", supportsRefresh = true)

        val response = apiWith(provider).dummyRetry()

        assertThat(response.resultCount).isEqualTo(1)
        assertThat(provider.updateTokensCallCount).isEqualTo(1)
    }

    @Test fun `retried request carries every configured api-key header`() = runTest {
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"error":"unauthorized"}"""))
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"resultCount":1}"""))
        val provider = FakeAuthTokenProvider(
            "old_token",
            "refresh_xyz",
            "key",
            apiKeyHeaderNames = listOf("apikey", "s-api-key"),
            supportsRefresh = true,
        )

        apiWith(provider).dummyRetry()

        server.takeRequest() // first request → 401
        val retried = server.takeRequest()
        assertThat(retried.getHeader("apikey")).isEqualTo("key")
        assertThat(retried.getHeader("s-api-key")).isEqualTo("key")
    }

    @Test fun `401 without provider throws Unauthorized`() = runTest {
        server.enqueue(MockResponse().setResponseCode(401))
        assertThrows(HttpClientError.Unauthorized::class.java) {
            kotlinx.coroutines.runBlocking { apiWith(null).dummyRetry() }
        }
    }

    @Test fun `401 with failing refresh propagates the refresh error`() = runTest {
        class RefreshBoom : RuntimeException()
        server.enqueue(MockResponse().setResponseCode(401))
        val provider = FakeAuthTokenProvider("token", "refresh", "key", refreshError = RefreshBoom())

        // OkHttp wraps the authenticator throwable; assert the cause chain contains RefreshBoom.
        val error = assertThrows(Throwable::class.java) {
            kotlinx.coroutines.runBlocking { apiWith(provider).dummyRetry() }
        }
        val chain = generateSequence(error) { it.cause }
        assertThat(chain.any { it is RefreshBoom }).isTrue()
    }
}
