package com.flix.timetable.data

import co.touchlab.kermit.Logger
import com.flix.timetable.data.remote.ApiJson
import com.flix.timetable.data.remote.DeparturesResponseDto
import com.flix.timetable.data.remote.StationDto
import com.flix.timetable.domain.Punctuality
import com.flix.timetable.testing.DEPARTURES_JSON
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class DeparturesMapperTest {
    private val mapper = DeparturesMapper(Logger.withTag("test"))

    private fun mapFixture() = mapper.map(ApiJson.decodeFromString<DeparturesResponseDto>(DEPARTURES_JSON))

    @Test
    fun `maps station with its time zone`() {
        val station = mapFixture().station

        assertEquals("Berlin central bus station", station.name)
        assertEquals(TimeZone.of("Europe/Berlin"), station.timeZone)
        assertFalse(station.isTimeZoneFallback)
    }

    @Test
    fun `keeps complete rides only without duplicates sorted by departure time`() {
        val departures = mapFixture().departures

        assertEquals(listOf("N958", "N3217", "N160"), departures.map { it.lineCode })
        assertEquals(Instant.parse("2026-11-25T15:20:00Z"), departures.first().scheduledAt)
    }

    @Test
    fun `maps line code and direction while blank direction becomes null`() {
        val departures = mapFixture().departures

        assertEquals("Rijeka bus station", departures[0].direction)
        assertNull(departures[2].direction)
    }

    @Test
    fun `maps real-time deviation`() {
        val departures = mapFixture().departures

        assertNull(departures[0].realtime)
        val onTime = departures[1].realtime!!
        assertEquals(Instant.parse("2026-11-25T16:08:04Z"), onTime.estimatedAt)
        assertEquals(484.seconds, onTime.deviation)
        assertEquals(Punctuality.ON_TIME, onTime.punctuality)
        assertEquals(27.minutes, departures[2].realtime!!.deviation)
        assertEquals(Punctuality.LATE, departures[2].realtime!!.punctuality)
    }

    @Test
    fun `unknown time zone falls back to UTC and is flagged`() {
        val station = mapper.map(DeparturesResponseDto(station = StationDto(timezone = "Mars/Olympus_Mons"))).station

        assertEquals(TimeZone.UTC, station.timeZone)
        assertTrue(station.isTimeZoneFallback)
    }

    @Test
    fun `missing station and rides give an empty timetable in UTC`() {
        val timetable = mapper.map(ApiJson.decodeFromString<DeparturesResponseDto>("""{"rides":null}"""))

        assertTrue(timetable.departures.isEmpty())
        assertNull(timetable.station.name)
        assertTrue(timetable.station.isTimeZoneFallback)
    }
}
