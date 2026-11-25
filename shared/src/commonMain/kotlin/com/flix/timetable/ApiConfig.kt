package com.flix.timetable

/**
 * Everything the shared layer needs to know about the backend. Supplied by the host app (BuildConfig on Android),
 * so keys and endpoints never get hard-coded into shared code.
 */
public data class ApiConfig(
    public val baseUrl: String,
    public val apiKey: String,
    public val stationId: String,
    public val isDebug: Boolean = false,
)
