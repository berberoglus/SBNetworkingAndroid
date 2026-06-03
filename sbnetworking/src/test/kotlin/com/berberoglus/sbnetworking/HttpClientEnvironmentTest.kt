package com.berberoglus.sbnetworking

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HttpClientEnvironmentTest {

    @Test
    fun `https default scheme builds host url`() {
        val env = HttpClientEnvironment(baseUrl = "api.test.com")
        val url = env.baseHttpUrl()
        assertThat(url).isNotNull()
        assertThat(url!!.scheme).isEqualTo("https")
        assertThat(url.host).isEqualTo("api.test.com")
    }

    @Test
    fun `host with port parses scheme host and port`() {
        val env = HttpClientEnvironment(scheme = "http", baseUrl = "127.0.0.1:54321")
        val url = env.baseHttpUrl()!!
        assertThat(url.scheme).isEqualTo("http")
        assertThat(url.host).isEqualTo("127.0.0.1")
        assertThat(url.port).isEqualTo(54321)
    }

    @Test
    fun `blank base url yields null`() {
        assertThat(HttpClientEnvironment(baseUrl = "   ").baseHttpUrl()).isNull()
    }
}
