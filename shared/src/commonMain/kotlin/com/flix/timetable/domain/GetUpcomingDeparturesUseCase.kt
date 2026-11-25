package com.flix.timetable.domain

import com.flix.timetable.core.AppResult
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

/** Departures from "now" until [window] ahead — the time-window rule of the product lives here, not in the UI. */
public class GetUpcomingDeparturesUseCase(
    private val repository: TimetableRepository,
    private val clock: Clock,
    private val window: Duration = DEFAULT_WINDOW,
) {
    public suspend operator fun invoke(): AppResult<Timetable> {
        val now = clock.now()
        return repository.departures(from = now, to = now + window)
    }

    public companion object {
        public val DEFAULT_WINDOW: Duration = 2.hours
    }
}
