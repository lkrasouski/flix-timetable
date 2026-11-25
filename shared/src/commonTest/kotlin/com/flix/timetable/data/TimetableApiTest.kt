package com.flix.timetable.data

import co.touchlab.kermit.Logger
import com.flix.timetable.ApiConfig
import com.flix.timetable.core.AppResult
import com.flix.timetable.core.DataError
import com.flix.timetable.data.remote.RetryPolicy
import com.flix.timetable.data.remote.TimetableApi
import com.flix.timetable.data.remote.createHttpClient
import com.flix.timetable.data.remote.redactApiKey
import com.flix.timetable.data.remote.toApiTimestamp
import com.flix.timetable.testing.DEPARTURES_JSON
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Instant

class TimetableApiTest {
    private val config =
        ApiConfig(
            baseUrl = "https://global.api.flixbus.com/",
            apiKey = "secret-key",
            stationId = "dcbaaee6-9603-11e6-9066-549f350fcb0c",
        )
    private val from = Instant.parse("2026-03-05T14:11:51.485Z")
    private val to = Instant.parse("2026-03-05T16:11:51.485Z")
    private val json = headersOf(HttpHeaders.ContentType, "application/json")

    private fun repository(
        retryPolicy: RetryPolicy = RetryPolicy.None,
        handler: MockRequestHandler,
    ): DefaultTimetableRepository {
        val logger = Logger.withTag("test")
        val client = createHttpClient(MockEngine(handler), config, logger, retryPolicy)
        return DefaultTimetableRepository(TimetableApi(client, config), DeparturesMapper(logger), logger)
    }

    @Test
    fun `requests the documented path and query`() =
        runTest {
            var requested = ""
            val repo =
                repository { request ->
                    requested = request.url.toString()
                    respond(DEPARTURES_JSON, HttpStatusCode.OK, json)
                }

            repo.departures(from, to)

            assertEquals(
                "https://global.api.flixbus.com/gis/v2/timetable/dcbaaee6-9603-11e6-9066-549f350fcb0c/departures" +
                    "?from=2026-03-05T14%3A11%3A51.485Z&to=2026-03-05T16%3A11%3A51.485Z&apiKey=secret-key",
                requested,
            )
        }

    @Test
    fun `successful response is mapped`() =
        runTest {
            val result = repository { respond(DEPARTURES_JSON, HttpStatusCode.OK, json) }.departures(from, to)

            assertIs<AppResult.Success<*>>(result)
        }

    @Test
    fun `5xx is a server error`() =
        runTest {
            val result = repository { respondError(HttpStatusCode.BadGateway) }.departures(from, to)

            assertEquals(AppResult.Failure(DataError.Server(502)), result)
        }

    @Test
    fun `4xx is a client error`() =
        runTest {
            val result = repository { respondError(HttpStatusCode.Forbidden) }.departures(from, to)

            assertEquals(AppResult.Failure(DataError.Client(403)), result)
        }

    @Test
    fun `malformed body is an invalid response`() =
        runTest {
            val result = repository { respond("""{"rides": 42""", HttpStatusCode.OK, json) }.departures(from, to)

            assertEquals(AppResult.Failure(DataError.InvalidResponse), result)
        }

    @Test
    fun `io failure means no connection`() =
        runTest {
            val result = repository { throw IOException("Unable to resolve host") }.departures(from, to)

            assertEquals(AppResult.Failure(DataError.NoConnection), result)
        }

    @Test
    fun `transient server errors are retried`() =
        runTest {
            var calls = 0
            val repo =
                repository(RetryPolicy(maxRetries = 2, baseDelayMillis = 0)) {
                    calls++
                    if (calls < 3) {
                        respondError(HttpStatusCode.ServiceUnavailable)
                    } else {
                        respond(DEPARTURES_JSON, HttpStatusCode.OK, json)
                    }
                }

            val result = repo.departures(from, to)

            assertEquals(3, calls)
            assertIs<AppResult.Success<*>>(result)
        }

    @Test
    fun `timestamps always carry milliseconds and UTC designator`() {
        assertEquals("2026-03-05T14:00:00.000Z", Instant.parse("2026-03-05T14:00:00Z").toApiTimestamp())
        assertEquals("2026-03-05T14:11:51.485Z", Instant.parse("2026-03-05T15:11:51.485123+01:00").toApiTimestamp())
    }

    @Test
    fun `api key is redacted from logged urls`() {
        assertEquals(
            "GET https://host/departures?from=a&apiKey=***&to=b",
            "GET https://host/departures?from=a&apiKey=secret-key&to=b".redactApiKey(),
        )
    }
}
