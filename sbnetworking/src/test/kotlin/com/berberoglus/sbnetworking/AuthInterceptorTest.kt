package com.berberoglus.sbnetworking

import com.berberoglus.sbnetworking.auth.AuthInterceptor
import com.berberoglus.sbnetworking.support.FakeAuthTokenProvider
import com.google.common.truth.Truth.assertThat
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

class AuthInterceptorTest {

    private lateinit var server: MockWebServer

    @Before fun setUp() { server = MockWebServer().also { it.start() } }
    @After fun tearDown() { server.shutdown() }

    private fun clientWith(provider: AuthTokenProvider?): OkHttpClient =
        OkHttpClient.Builder().addInterceptor(AuthInterceptor(provider)).build()

    @Test fun `provider adds apikey and authorization headers`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        val provider = FakeAuthTokenProvider("access_123", "refresh_456", "anon_key_xyz")
        clientWith(provider).newCall(Request.Builder().url(server.url("/")).build()).execute().close()

        val recorded = server.takeRequest()
        assertThat(recorded.getHeader("apikey")).isEqualTo("anon_key_xyz")
        assertThat(recorded.getHeader("Authorization")).isEqualTo("Bearer access_123")
    }

    @Test fun `provider sends every configured api-key header name`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        val provider = FakeAuthTokenProvider(
            accessToken = "access_123",
            refreshToken = null,
            apiKey = "key_xyz",
            apiKeyHeaderNames = listOf("apikey", "s-api-key"),
        )
        clientWith(provider).newCall(Request.Builder().url(server.url("/")).build()).execute().close()

        val recorded = server.takeRequest()
        assertThat(recorded.getHeader("apikey")).isEqualTo("key_xyz")
        assertThat(recorded.getHeader("s-api-key")).isEqualTo("key_xyz")
        assertThat(recorded.getHeader("Authorization")).isEqualTo("Bearer access_123")
    }

    @Test fun `default provider sends only the apikey header`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        val provider = FakeAuthTokenProvider("access", null, "key_only")
        clientWith(provider).newCall(Request.Builder().url(server.url("/")).build()).execute().close()

        val recorded = server.takeRequest()
        assertThat(recorded.getHeader("apikey")).isEqualTo("key_only")
        assertThat(recorded.getHeader("s-api-key")).isNull()
    }

    @Test fun `provider headers merge with existing request headers`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        val provider = FakeAuthTokenProvider("token", null, "key")
        val request = Request.Builder().url(server.url("/")).header("X-Custom", "value").build()
        clientWith(provider).newCall(request).execute().close()

        val recorded = server.takeRequest()
        assertThat(recorded.getHeader("apikey")).isEqualTo("key")
        assertThat(recorded.getHeader("Authorization")).isEqualTo("Bearer token")
        assertThat(recorded.getHeader("X-Custom")).isEqualTo("value")
    }

    @Test fun `null provider adds no auth headers`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        clientWith(null).newCall(Request.Builder().url(server.url("/")).build()).execute().close()

        val recorded = server.takeRequest()
        assertThat(recorded.getHeader("apikey")).isNull()
        assertThat(recorded.getHeader("Authorization")).isNull()
    }
}
