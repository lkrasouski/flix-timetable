package com.flix.timetable.presentation

import com.flix.timetable.domain.Departure
import com.flix.timetable.domain.Punctuality
import com.flix.timetable.domain.Realtime
import com.flix.timetable.domain.Station
import com.flix.timetable.domain.Timetable
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class TimetableUiMapperTest {
    private val berlin = Station("Berlin central bus station", TimeZone.of("Europe/Berlin"), isTimeZoneFallback = false)

    private fun departure(
        id: String,
        utc: String,
        realtime: Realtime? = null,
    ) = Departure(id, "N$id", "Somewhere", Instant.parse(utc), realtime)

    @Test
    fun `times are converted to the station time zone in winter`() {
        val state =
            Timetable(
                berlin,
                listOf(departure("1", "2026-11-25T15:20:00Z")),
            ).toUiState(now("2026-11-25T15:00:00Z"))

        assertEquals(
            LocalTime(16, 20),
            state.sections
                .single()
                .items
                .single()
                .scheduledTime,
        )
        assertEquals("UTC+01:00", state.station.utcOffset)
        assertEquals("Europe/Berlin", state.station.timeZoneId)
    }

    @Test
    fun `times are converted to the station time zone in summer`() {
        val state =
            Timetable(
                berlin,
                listOf(departure("1", "2026-07-01T10:00:00Z")),
            ).toUiState(now("2026-07-01T09:30:00Z"))

        assertEquals(
            LocalTime(12, 0),
            state.sections
                .single()
                .items
                .single()
                .scheduledTime,
        )
        assertEquals("UTC+02:00", state.station.utcOffset)
    }

    @Test
    fun `window across the DST switch uses the right offset for each departure`() {
        // 2026-10-25 03:00 CEST -> 02:00 CET
        val rides = listOf(departure("1", "2026-10-25T00:30:00Z"), departure("2", "2026-10-25T01:30:00Z"))

        val items =
            Timetable(berlin, rides)
                .toUiState(now("2026-10-25T00:00:00Z"))
                .sections
                .single()
                .items

        assertEquals(listOf(LocalTime(2, 30), LocalTime(2, 30)), items.map { it.scheduledTime })
    }

    @Test
    fun `departures after local midnight go to a tomorrow section with headers shown`() {
        val rides = listOf(departure("1", "2026-11-25T22:40:00Z"), departure("2", "2026-11-25T23:10:00Z"))

        val state = Timetable(berlin, rides).toUiState(now("2026-11-25T22:30:00Z"))

        assertTrue(state.showDayHeaders)
        assertEquals(listOf(RelativeDay.TODAY, RelativeDay.TOMORROW), state.sections.map { it.day })
        assertEquals(LocalDate(2026, 11, 26), state.sections[1].date)
        assertEquals(
            LocalTime(0, 10),
            state.sections[1]
                .items
                .single()
                .scheduledTime,
        )
    }

    @Test
    fun `same-day window hides day headers`() {
        val state =
            Timetable(
                berlin,
                listOf(departure("1", "2026-11-25T15:20:00Z")),
            ).toUiState(now("2026-11-25T15:00:00Z"))

        assertFalse(state.showDayHeaders)
    }

    @Test
    fun `delay is shown from one minute on in station time`() {
        val late =
            Realtime(Instant.parse("2026-11-25T16:47:00Z"), deviation = 1620.seconds, punctuality = Punctuality.LATE)
        val tiny =
            Realtime(Instant.parse("2026-11-25T16:00:40Z"), deviation = 40.seconds, punctuality = Punctuality.ON_TIME)
        val rides = listOf(departure("1", "2026-11-25T16:20:00Z", late), departure("2", "2026-11-25T16:00:00Z", tiny))

        val items =
            Timetable(berlin, rides)
                .toUiState(now("2026-11-25T15:00:00Z"))
                .sections
                .single()
                .items

        assertEquals(DelayInfo(LocalTime(17, 47), minutes = 27, isLate = true), items[0].delay)
        assertNull(items[1].delay)
    }

    @Test
    fun `fallback zone is reported to the UI`() {
        val station = Station(null, TimeZone.UTC, isTimeZoneFallback = true)

        val header = Timetable(station, emptyList()).toUiState(now("2026-11-25T15:00:00Z")).station

        assertTrue(header.isTimeZoneFallback)
        assertEquals("UTC", header.utcOffset)
    }

    @Test
    fun `last update time is in station time`() {
        val state = Timetable(berlin, emptyList()).toUiState(now("2026-11-25T15:05:30Z"))

        assertTrue(state.isEmpty)
        assertEquals(LocalTime(16, 5, 30), state.updatedAt)
    }

    private fun now(utc: String) = Instant.parse(utc)
}
