package com.flix.timetable.data

import co.touchlab.kermit.Logger
import com.flix.timetable.data.remote.DeparturesResponseDto
import com.flix.timetable.data.remote.DeviationDto
import com.flix.timetable.data.remote.RideDto
import com.flix.timetable.data.remote.StationDto
import com.flix.timetable.domain.Departure
import com.flix.timetable.domain.Punctuality
import com.flix.timetable.domain.Realtime
import com.flix.timetable.domain.Station
import com.flix.timetable.domain.Timetable
import kotlinx.datetime.IllegalTimeZoneException
import kotlinx.datetime.TimeZone
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * Turns the wire model into the domain model. Lenient by design: a ride that misses what the screen must show
 * (id, line code, departure time) is skipped and logged instead of failing the whole timetable.
 */
internal class DeparturesMapper(
    private val logger: Logger,
) {
    fun map(dto: DeparturesResponseDto): Timetable =
        Timetable(
            station = mapStation(dto.station),
            departures =
                dto.rides
                    .orEmpty()
                    .mapNotNull(::mapRide)
                    .distinctBy { it.id }
                    .sortedWith(compareBy<Departure> { it.scheduledAt }.thenBy { it.lineCode }),
        )

    private fun mapStation(dto: StationDto?): Station {
        val zoneId = dto?.timezone?.trim()
        val zone =
            zoneId
                ?.takeIf { it.isNotEmpty() }
                ?.let { id ->
                    try {
                        TimeZone.of(id)
                    } catch (e: IllegalTimeZoneException) {
                        logger.w(e) { "Unknown station time zone '$id', falling back to UTC" }
                        null
                    }
                }
        if (zoneId.isNullOrEmpty()) logger.w { "Station time zone is missing, falling back to UTC" }
        return Station(
            name = dto?.name?.trim()?.takeIf { it.isNotEmpty() },
            timeZone = zone ?: TimeZone.UTC,
            isTimeZoneFallback = zone == null,
        )
    }

    private fun mapRide(dto: RideDto): Departure? {
        val id = dto.id?.takeIf { it.isNotBlank() }
        val lineCode = dto.line?.code?.trim()?.takeIf { it.isNotEmpty() }
        val scheduledAt = dto.status?.scheduledTimestamp?.parseInstantOrNull()
        if (id == null || lineCode == null || scheduledAt == null) {
            logger.w { "Skipping incomplete ride id=${dto.id} line=${dto.line?.code}" }
            return null
        }
        return Departure(
            id = id,
            lineCode = lineCode,
            direction = dto.line.direction?.trim()?.takeIf { it.isNotEmpty() },
            scheduledAt = scheduledAt,
            realtime = dto.status.deviation?.let { mapRealtime(it, scheduledAt) },
        )
    }

    private fun mapRealtime(
        dto: DeviationDto,
        scheduledAt: Instant,
    ): Realtime? {
        val estimatedAt =
            dto.deviationTimestamp?.parseInstantOrNull()
                ?: dto.deviationSeconds?.let { scheduledAt + it.seconds }
                ?: return null
        return Realtime(
            estimatedAt = estimatedAt,
            deviation = estimatedAt - scheduledAt,
            punctuality =
                when (dto.deviationClass?.uppercase()) {
                    "ON_TIME" -> Punctuality.ON_TIME
                    "LATE" -> Punctuality.LATE
                    "EARLY" -> Punctuality.EARLY
                    else -> Punctuality.UNKNOWN
                },
        )
    }

    private fun String.parseInstantOrNull(): Instant? =
        try {
            Instant.parse(this)
        } catch (_: IllegalArgumentException) {
            null
        }
}
