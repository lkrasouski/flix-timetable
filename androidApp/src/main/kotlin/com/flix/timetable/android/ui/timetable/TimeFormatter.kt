package com.flix.timetable.android.ui.timetable

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toJavaLocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Formats station-local times for display. The values are already converted to the station time zone in shared code;
 * this only applies the user's locale and 12/24-hour preference.
 */
@Immutable
class TimeFormatter(
    is24Hour: Boolean,
    locale: Locale,
) {
    private val timeFormat =
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, if (is24Hour) "Hm" else "hm"), locale)
    private val dayFormat = DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "EEEdMMM"), locale)

    fun time(time: LocalTime): String = timeFormat.format(time.toJavaLocalTime())

    fun day(date: LocalDate): String = dayFormat.format(date.toJavaLocalDate())
}

@Composable
fun rememberTimeFormatter(): TimeFormatter {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val is24Hour = DateFormat.is24HourFormat(context)
    return remember(locale, is24Hour) { TimeFormatter(is24Hour, locale) }
}
