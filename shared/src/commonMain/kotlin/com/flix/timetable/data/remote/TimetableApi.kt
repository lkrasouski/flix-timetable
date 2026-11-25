package com.flix.timetable.data.remote

import com.flix.timetable.ApiConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.appendPathSegments
import kotlin.time.Instant

internal class TimetableApi(
    private val client: HttpClient,
    private val config: ApiConfig,
) {
    suspend fun departures(
        from: Instant,
        to: Instant,
    ): DeparturesResponseDto =
        client
            .get {
                url { appendPathSegments("gis", "v2", "timetable", config.stationId, "departures") }
                parameter("from", from.toApiTimestamp())
                parameter("to", to.toApiTimestamp())
                parameter("apiKey", config.apiKey)
            }.body()
}
