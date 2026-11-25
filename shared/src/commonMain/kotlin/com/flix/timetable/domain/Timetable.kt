package com.flix.timetable.domain

import kotlinx.datetime.TimeZone
import kotlin.time.Duration
import kotlin.time.Instant

public data class Timetable(
    public val station: Station,
    /** Sorted by scheduled departure time, without duplicates. */
    public val departures: List<Departure>,
)

public data class Station(
    public val name: String?,
    public val timeZone: TimeZone,
    /**
     * `true` when the backend did not send a valid IANA time zone and [timeZone] fell back to UTC.
     * The UI must then say so, otherwise times would silently be shown in the wrong zone.
     */
    public val isTimeZoneFallback: Boolean,
)

public data class Departure(
    public val id: String,
    public val lineCode: String,
    /** Final destination of the ride; `null` when the backend did not provide one. */
    public val direction: String?,
    public val scheduledAt: Instant,
    /** Live information; `null` when the ride has no real-time data. */
    public val realtime: Realtime?,
)

public data class Realtime(
    public val estimatedAt: Instant,
    /** Positive when the bus is late. */
    public val deviation: Duration,
    public val punctuality: Punctuality,
)

public enum class Punctuality { ON_TIME, LATE, EARLY, UNKNOWN }
