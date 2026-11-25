package com.flix.timetable.domain

import com.flix.timetable.core.AppResult
import kotlin.time.Instant

public interface TimetableRepository {
    /** Departures of the configured station scheduled within [from, to]. */
    public suspend fun departures(
        from: Instant,
        to: Instant,
    ): AppResult<Timetable>
}
