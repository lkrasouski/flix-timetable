package com.flix.timetable.android.ui.timetable

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class TimeFormatterTest {
    @Test
    fun formats24HourTime() {
        assertEquals("07:05", TimeFormatter(is24Hour = true, locale = Locale.UK).time(LocalTime(7, 5)))
    }

    @Test
    fun formats12HourTime() {
        assertEquals("7:05 PM", TimeFormatter(is24Hour = false, locale = Locale.US).time(LocalTime(19, 5)))
    }

    @Test
    fun formatsDayInLocale() {
        assertEquals(
            "Do., 26. Nov.",
            TimeFormatter(is24Hour = true, locale = Locale.GERMANY).day(LocalDate(2026, 11, 26)),
        )
    }
}
