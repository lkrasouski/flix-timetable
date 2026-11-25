package com.flix.timetable.android.ui.timetable

import android.content.res.Configuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.flix.timetable.android.ui.theme.FlixTheme
import com.flix.timetable.core.DataError
import com.flix.timetable.presentation.DelayInfo
import com.flix.timetable.presentation.DepartureItem
import com.flix.timetable.presentation.DepartureSection
import com.flix.timetable.presentation.RelativeDay
import com.flix.timetable.presentation.StationHeader
import com.flix.timetable.presentation.TimetableUiState
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import java.util.Locale

internal object PreviewData {
    val station = StationHeader("Berlin central bus station", "Europe/Berlin", "UTC+01:00", isTimeZoneFallback = false)

    val loaded =
        TimetableUiState.Loaded(
            station = station,
            sections =
                listOf(
                    DepartureSection(
                        date = LocalDate(2026, 11, 25),
                        day = RelativeDay.TODAY,
                        items =
                            listOf(
                                DepartureItem("1", "N958", "Rijeka bus station", LocalTime(23, 20), null),
                                DepartureItem(
                                    "2",
                                    "N3217",
                                    "Chernihiv (Chernihiv-1 Bus Station)",
                                    LocalTime(23, 40),
                                    DelayInfo(LocalTime(23, 48), minutes = 8, isLate = false),
                                ),
                            ),
                    ),
                    DepartureSection(
                        date = LocalDate(2026, 11, 26),
                        day = RelativeDay.TOMORROW,
                        items =
                            listOf(
                                DepartureItem(
                                    "3",
                                    "N160",
                                    "Trieste (Autostazione)",
                                    LocalTime(0, 20),
                                    DelayInfo(LocalTime(0, 47), minutes = 27, isLate = true),
                                ),
                                DepartureItem("4", "X06", null, LocalTime(0, 45), null),
                            ),
                    ),
                ),
            showDayHeaders = true,
            updatedAt = LocalTime(23, 5),
        )
}

@Composable
private fun Preview(state: TimetableUiState) {
    FlixTheme {
        TimetableScreen(
            state = state,
            onRefresh = {},
            onRetry = {},
            snackbarHostState = SnackbarHostState(),
            formatter = TimeFormatter(is24Hour = true, locale = Locale.UK),
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LoadedPreview() = Preview(PreviewData.loaded)

@Preview(showBackground = true)
@Composable
private fun EmptyPreview() = Preview(PreviewData.loaded.copy(sections = emptyList(), showDayHeaders = false))

@Preview(showBackground = true)
@Composable
private fun LoadingPreview() = Preview(TimetableUiState.Loading)

@Preview(showBackground = true)
@Composable
private fun ErrorPreview() = Preview(TimetableUiState.Error(DataError.NoConnection))
