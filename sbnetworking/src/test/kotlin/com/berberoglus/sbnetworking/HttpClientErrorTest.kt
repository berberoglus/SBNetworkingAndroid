package com.berberoglus.sbnetworking

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HttpClientErrorTest {

    @Test fun `data object errors are equal by type`() {
        assertThat(HttpClientError.Unauthorized).isEqualTo(HttpClientError.Unauthorized)
        assertThat(HttpClientError.NotFound).isNotEqualTo(HttpClientError.Unauthorized)
    }

    @Test fun `client error equals by code and body`() {
        val a = HttpClientError.ClientError(400, "x".toByteArray())
        val b = HttpClientError.ClientError(400, "x".toByteArray())
        val c = HttpClientError.ClientError(400, "y".toByteArray())
        assertThat(a).isEqualTo(b)
        assertThat(a).isNotEqualTo(c)
    }

    @Test fun `messages are populated`() {
        assertThat(HttpClientError.Unauthorized.message).contains("401")
        assertThat(HttpClientError.ServerError(500, "oops".toByteArray()).message).contains("500")
    }
}
