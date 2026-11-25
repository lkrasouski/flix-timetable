package com.flix.timetable.android.ui.timetable

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.flix.timetable.android.ui.theme.FlixTheme
import com.flix.timetable.core.DataError
import com.flix.timetable.presentation.StationHeader
import com.flix.timetable.presentation.TimetableUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class TimetableScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private fun setContent(
        state: TimetableUiState,
        onRetry: () -> Unit = {},
    ) {
        compose.setContent {
            FlixTheme {
                TimetableScreen(
                    state = state,
                    onRefresh = {},
                    onRetry = onRetry,
                    snackbarHostState = SnackbarHostState(),
                    formatter = TimeFormatter(is24Hour = true, locale = Locale.UK),
                )
            }
        }
    }

    @Test
    fun loadedStateShowsLineCodeDirectionAndStationLocalTime() {
        setContent(PreviewData.loaded)

        compose.onNodeWithText("Berlin central bus station").assertIsDisplayed()
        compose.onNodeWithText("Times in Europe/Berlin (UTC+01:00)").assertIsDisplayed()
        compose
            .onNodeWithContentDescription("Line N958 to Rijeka bus station, departs at 23:20")
            .assertIsDisplayed()
        compose.onNodeWithTag(TimetableTestTags.LIST).performScrollToNode(hasText("Tomorrow", substring = true))
        compose.onNodeWithText("Tomorrow", substring = true).assertIsDisplayed()
    }

    @Test
    fun delayIsAnnouncedWithExpectedTime() {
        setContent(PreviewData.loaded)

        compose
            .onNodeWithContentDescription(
                "Line N3217 to Chernihiv (Chernihiv-1 Bus Station), departs at 23:40. 8 minutes late, expected at 23:48",
            ).assertIsDisplayed()
    }

    @Test
    fun missingDirectionFallsBackToLineOnly() {
        setContent(PreviewData.loaded)

        val description = "Line X06, departs at 00:45"
        compose.onNodeWithTag(TimetableTestTags.LIST).performScrollToNode(hasContentDescription(description))
        compose.onNodeWithContentDescription(description).assertIsDisplayed()
    }

    @Test
    fun emptyStateExplainsTheWindow() {
        setContent(PreviewData.loaded.copy(sections = emptyList(), showDayHeaders = false))

        compose.onNodeWithText("No departures in the next 2 hours").assertIsDisplayed()
    }

    @Test
    fun errorStateRetries() {
        var retries = 0
        setContent(TimetableUiState.Error(DataError.NoConnection), onRetry = { retries++ })

        compose.onNodeWithText("You’re offline").assertIsDisplayed()
        compose.onNodeWithTag(TimetableTestTags.RETRY).performClick()

        assertEquals(1, retries)
    }

    @Test
    fun loadingStateIsAnnounced() {
        setContent(TimetableUiState.Loading)

        compose.onNodeWithContentDescription("Loading departures").assertExists()
    }

    @Test
    fun fallbackTimeZoneIsCalledOut() {
        setContent(
            PreviewData.loaded.copy(
                station = StationHeader(null, "UTC", "UTC", isTimeZoneFallback = true),
            ),
        )

        compose.onNodeWithText("Departures").assertIsDisplayed()
        compose.onNodeWithText("Station time zone unavailable · times in UTC").assertIsDisplayed()
    }
}
