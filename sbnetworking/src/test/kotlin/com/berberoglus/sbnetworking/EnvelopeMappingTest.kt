package com.berberoglus.sbnetworking

import com.berberoglus.sbnetworking.envelope.EnvelopeDto
import com.berberoglus.sbnetworking.support.ITunesEntityDto
import com.berberoglus.sbnetworking.support.toDomain
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

class EnvelopeMappingTest {

    private lateinit var server: MockWebServer
    private lateinit var client: HttpClient

    @Before fun setUp() {
        server = MockWebServer().also { it.start() }
        client = HttpClient.Builder(
            HttpClientEnvironment(scheme = "http", baseUrl = server.hostName + ":" + server.port),
        ).build()
    }

    @After fun tearDown() = server.shutdown()

    @Test fun `envelope decodes and maps data to domain`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"data":[{"artistId":1,"trackId":2,"artistName":"A","trackName":"T"}],
                   "meta":{"page":1,"total_pages":5}}""".trimIndent(),
            ),
        )
        val envelope = client.execute<EnvelopeDto<ITunesEntityDto>>(
            EndpointSpec(path = "/items", method = HttpMethod.GET),
        )
        assertThat(envelope).isNotNull()
        assertThat(envelope!!.meta?.totalPages).isEqualTo(5)
        val domain = envelope.data.map { it.toDomain() }
        assertThat(domain.single().artistName).isEqualTo("A")
    }
}
