package com.berberoglus.sbnetworking

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class ResponseValidationTest {

    private val body = ByteArray(0)

    @Test fun `2xx returns body`() {
        assertThat(validateAndExtractBody(200, "ok".toByteArray())).isEqualTo("ok".toByteArray())
        assertThat(validateAndExtractBody(299, body)).isEqualTo(body)
        assertThat(validateAndExtractBody(304, body)).isEqualTo(body)
    }

    @Test fun `204 returns null`() {
        assertThat(validateAndExtractBody(204, "ignored".toByteArray())).isNull()
    }

    @Test fun `401 throws Unauthorized`() {
        val e = assertThrows(HttpClientError.Unauthorized::class.java) { validateAndExtractBody(401, body) }
        assertThat(e).isEqualTo(HttpClientError.Unauthorized)
    }

    @Test fun `404 throws NotFound`() {
        assertThrows(HttpClientError.NotFound::class.java) { validateAndExtractBody(404, body) }
    }

    @Test fun `client error codes throw ClientError`() {
        for (code in listOf(400, 402, 403, 418, 499)) {
            val e = assertThrows(HttpClientError.ClientError::class.java) { validateAndExtractBody(code, body) }
            assertThat(e.statusCode).isEqualTo(code)
        }
    }

    @Test fun `server error codes throw ServerError`() {
        for (code in listOf(500, 599)) {
            val e = assertThrows(HttpClientError.ServerError::class.java) { validateAndExtractBody(code, body) }
            assertThat(e.statusCode).isEqualTo(code)
        }
    }

    @Test fun `unexpected code throws UnexpectedStatusCode`() {
        assertThrows(HttpClientError.UnexpectedStatusCode::class.java) { validateAndExtractBody(999, body) }
    }
}
