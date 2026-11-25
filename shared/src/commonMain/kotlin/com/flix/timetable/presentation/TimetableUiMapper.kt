package com.flix.timetable.presentation

import com.flix.timetable.domain.Departure
import com.flix.timetable.domain.Punctuality
import com.flix.timetable.domain.Timetable
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.offsetAt
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs
import kotlin.time.Instant

/** Converts the domain timetable into what the screen renders; all time-zone math happens here, in shared code. */
internal fun Timetable.toUiState(now: Instant): TimetableUiState.Loaded {
    val zone = station.timeZone
    val today = now.toLocalDateTime(zone).date
    val sections =
        departures
            .groupBy { it.scheduledAt.toLocalDateTime(zone).date }
            .map { (date, rides) ->
                DepartureSection(
                    date = date,
                    day = relativeDay(date, today),
                    items = rides.map { it.toItem(zone) },
                )
            }
    return TimetableUiState.Loaded(
        station =
            StationHeader(
                name = station.name,
                timeZoneId = zone.id,
                utcOffset = zone.offsetAt(now).toDisplayString(),
                isTimeZoneFallback = station.isTimeZoneFallback,
            ),
        sections = sections,
        showDayHeaders = sections.any { it.day != RelativeDay.TODAY },
        updatedAt = now.toLocalDateTime(zone).time,
    )
}

private fun Departure.toItem(zone: TimeZone): DepartureItem {
    val delayMinutes = realtime?.deviation?.inWholeMinutes ?: 0
    return DepartureItem(
        id = id,
        lineCode = lineCode,
        direction = direction,
        scheduledTime = scheduledAt.toLocalDateTime(zone).time,
        delay =
            realtime?.takeIf { delayMinutes >= 1 }?.let {
                DelayInfo(
                    estimatedTime = it.estimatedAt.toLocalDateTime(zone).time,
                    minutes = delayMinutes.toInt(),
                    isLate = it.punctuality == Punctuality.LATE,
                )
            },
    )
}

private fun relativeDay(
    date: LocalDate,
    today: LocalDate,
): RelativeDay =
    when (date) {
        today -> RelativeDay.TODAY
        today.plus(DatePeriod(days = 1)) -> RelativeDay.TOMORROW
        else -> RelativeDay.OTHER
    }

/** `UTC`, `UTC+01:00`, `UTC-03:30`. */
internal fun UtcOffset.toDisplayString(): String {
    if (totalSeconds == 0) return "UTC"
    val sign = if (totalSeconds > 0) "+" else "-"
    val minutes = abs(totalSeconds) / 60
    val hh = (minutes / 60).toString().padStart(2, '0')
    val mm = (minutes % 60).toString().padStart(2, '0')
    return "UTC$sign$hh:$mm"
}
