package com.flix.timetable.data.remote

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

private val apiDateTimeFormat =
    LocalDateTime.Format {
        date(LocalDate.Formats.ISO)
        char('T')
        hour()
        char(':')
        minute()
        char(':')
        second()
        char('.')
        secondFraction(fixedLength = 3)
    }

/** ISO-8601 in UTC with millisecond precision, the format the API documents: `2026-03-05T14:11:51.485Z`. */
internal fun Instant.toApiTimestamp(): String = apiDateTimeFormat.format(toLocalDateTime(TimeZone.UTC)) + "Z"
