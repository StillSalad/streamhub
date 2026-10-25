package com.lollomarta.streamhub

import com.lollomarta.streamhub.data.catalog.TmdbCatalogRepository
import com.lollomarta.streamhub.data.network.TmdbClient
import com.lollomarta.streamhub.domain.AppError
import com.lollomarta.streamhub.domain.toAppError
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class TmdbClientTest {
    private lateinit var server: MockWebServer

    @Before fun setUp() { server = MockWebServer().apply { start() } }
    @After fun tearDown() { server.shutdown() }

    private fun client(key: String? = "abc123") = TmdbClient(
        http = TmdbClient.buildHttpClient(cache = null),
        apiKeyProvider = { key },
        baseUrl = server.url("/3/").toString(),
        maxRetries = 1,
        backoffMillis = 1,
    )

    private suspend fun expectError(block: suspend () -> Unit): AppError {
        try { block() } catch (e: Throwable) { return e.toAppError() }
        fail("attesa eccezione"); throw IllegalStateException()
    }

    @Test fun missingKeyIsReportedWithoutNetworkCall() = runTest {
        assertEquals(AppError.MissingApiKey, expectError { client(key = null).get("movie/popular") })
        assertEquals(0, server.requestCount)
    }

    @Test fun v3KeyGoesInQueryAndV4TokenInHeader() = runTest {
        server.enqueue(MockResponse().setBody("{}"))
        server.enqueue(MockResponse().setBody("{}"))
        client("abc123").get("x")
        assertTrue(server.takeRequest().path!!.contains("api_key=abc123"))
        client("eyJtoken").get("x")
        val r = server.takeRequest()
        assertEquals("Bearer eyJtoken", r.getHeader("Authorization"))
        assertTrue(!r.path!!.contains("api_key"))
    }

    @Test fun unauthorizedIsNotRetried() = runTest {
        server.enqueue(MockResponse().setResponseCode(401))
        assertEquals(AppError.InvalidApiKey, expectError { client().get("x") })
        assertEquals(1, server.requestCount)
    }

    @Test fun serverErrorIsRetriedThenSucceeds() = runTest {
        server.enqueue(MockResponse().setResponseCode(503))
        server.enqueue(MockResponse().setBody("""{"results":[]}"""))
        assertEquals("""{"results":[]}""", client().get("x"))
        assertEquals(2, server.requestCount)
    }

    @Test fun networkFailureBecomesNetworkError() = runTest {
        repeat(3) { server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START)) }
        assertEquals(AppError.Network, expectError { client().get("x") })
    }

    @Test fun malformedJsonDoesNotCrash() = runTest {
        server.enqueue(MockResponse().setBody("<html>not json</html>"))
        val error = expectError { TmdbCatalogRepository(client()).search("matrix") }
        assertTrue(error is AppError.Parsing)
    }
}
