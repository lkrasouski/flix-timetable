package com.flix.timetable.testing

import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

/** A clock that follows the virtual time of a coroutine test, so `delay` and `clock.now()` agree. */
internal class TestClock(
    private val start: Instant,
    private val scheduler: TestCoroutineScheduler,
) : Clock {
    override fun now(): Instant = start + scheduler.currentTime.milliseconds
}

internal class FixedClock(
    var instant: Instant,
) : Clock {
    override fun now(): Instant = instant
}
