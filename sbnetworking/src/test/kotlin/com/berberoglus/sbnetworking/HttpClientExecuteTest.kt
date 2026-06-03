package com.berberoglus.sbnetworking

import com.berberoglus.sbnetworking.support.DummyPayload
import com.berberoglus.sbnetworking.support.ITunesSearchResponseDto
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class HttpClientExecuteTest {

    private lateinit var server: MockWebServer
    private lateinit var client: HttpClient

    @Before fun setUp() {
        server = MockWebServer().also { it.start() }
        client = HttpClient.Builder(
            HttpClientEnvironment(scheme = "http", baseUrl = server.hostName + ":" + server.port),
        ).build()
    }

    @After fun tearDown() = server.shutdown()

    @Test fun `get with query parameters appends them`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"resultCount":0,"results":[]}"""))
        client.execute<ITunesSearchResponseDto>(
            EndpointSpec(path = "/search", method = HttpMethod.GET, queryParameters = mapOf("foo" to "bar", "baz" to "qux")),
        )
        val recorded = server.takeRequest()
        assertThat(recorded.path).contains("foo=bar")
        assertThat(recorded.path).contains("baz=qux")
    }

    @Test fun `empty query map adds no question mark`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"resultCount":0,"results":[]}"""))
        client.execute<ITunesSearchResponseDto>(EndpointSpec(path = "/search", method = HttpMethod.GET))
        assertThat(server.takeRequest().path).doesNotContain("?")
    }

    @Test fun `all http methods map to verb`() = runTest {
        for (method in HttpMethod.entries) {
            server.enqueue(MockResponse().setResponseCode(200).setBody("""{"resultCount":0,"results":[]}"""))
            client.execute<ITunesSearchResponseDto>(EndpointSpec(path = "/m", method = method, body = if (method == HttpMethod.GET) null else "{}".toByteArray()))
            assertThat(server.takeRequest().method).isEqualTo(method.value)
        }
    }

    @Test fun `post payload sets json body and content type`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"resultCount":1}"""))
        val payload = com.berberoglus.sbnetworking.internal.appJson.encodeToString(DummyPayload.serializer(), DummyPayload("John", 30))
        client.execute<com.berberoglus.sbnetworking.support.DummyResponseDto>(
            EndpointSpec(path = "/test", method = HttpMethod.POST, body = payload.toByteArray()),
        )
        val recorded = server.takeRequest()
        assertThat(recorded.getHeader("Content-Type")).contains("application/json")
        assertThat(recorded.body.readUtf8()).isEqualTo(payload)
    }

    @Test fun `nil payload sets no body`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"resultCount":1}"""))
        client.execute<com.berberoglus.sbnetworking.support.DummyResponseDto>(
            EndpointSpec(path = "/test", method = HttpMethod.POST),
        )
        assertThat(server.takeRequest().bodySize).isEqualTo(0)
    }

    @Test fun `204 yields null`() = runTest {
        server.enqueue(MockResponse().setResponseCode(204))
        val result = client.execute<com.berberoglus.sbnetworking.support.DummyResponseDto>(
            EndpointSpec(path = "/x", method = HttpMethod.GET),
        )
        assertThat(result).isNull()
    }

    @Test fun `execute of ByteArray returns raw body unchanged`() = runTest {
        // iOS `responseType is Data.Type` passthrough (behavior #10): bytes are NOT JSON-decoded.
        server.enqueue(MockResponse().setResponseCode(200).setBody("raw-not-json"))
        val bytes = client.execute<ByteArray>(EndpointSpec(path = "/raw", method = HttpMethod.GET))
        assertThat(bytes).isNotNull()
        assertThat(bytes!!.decodeToString()).isEqualTo("raw-not-json")
    }

    @Test fun `decodes real itunes fixture`() = runTest {
        val fixture = javaClass.classLoader!!.getResource("fixtures/ITunesSearch.json")!!.readText()
        server.enqueue(MockResponse().setResponseCode(200).setBody(fixture))
        val response = client.execute<ITunesSearchResponseDto>(
            EndpointSpec(path = "/search", method = HttpMethod.GET, queryParameters = mapOf("term" to "star wars")),
        )
        assertThat(response).isNotNull()
        assertThat(response!!.resultCount).isEqualTo(110)
        assertThat(response.results).isNotEmpty()
    }

    @Test fun `decode failure throws DecodingFailed`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("not-json"))
        assertThrows(HttpClientError.DecodingFailed::class.java) {
            kotlinx.coroutines.runBlocking {
                client.execute<com.berberoglus.sbnetworking.support.DummyResponseDto>(
                    EndpointSpec(path = "/x", method = HttpMethod.GET),
                )
            }
        }
    }

    @Test fun `server error maps to ServerError`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500).setBody("boom"))
        val e = assertThrows(HttpClientError.ServerError::class.java) {
            kotlinx.coroutines.runBlocking {
                client.execute<com.berberoglus.sbnetworking.support.DummyResponseDto>(
                    EndpointSpec(path = "/x", method = HttpMethod.GET),
                )
            }
        }
        assertThat(e.statusCode).isEqualTo(500)
    }
}
