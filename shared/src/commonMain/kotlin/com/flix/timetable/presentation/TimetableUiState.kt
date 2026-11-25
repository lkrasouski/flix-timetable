package com.flix.timetable.presentation

import com.flix.timetable.core.DataError
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

public sealed interface TimetableUiState {
    /** First load, nothing to show yet. */
    public data object Loading : TimetableUiState

    /** First load failed; there is no data to fall back to. */
    public data class Error(
        public val error: DataError,
    ) : TimetableUiState

    public data class Loaded(
        public val station: StationHeader,
        /** Departures grouped by their local date at the station; empty when nothing departs in the window. */
        public val sections: List<DepartureSection>,
        /** Day headers only add noise when every departure is today, so they show up only when needed. */
        public val showDayHeaders: Boolean,
        /** Wall-clock time of the last successful refresh, in the station time zone. */
        public val updatedAt: LocalTime,
        public val isRefreshing: Boolean = false,
    ) : TimetableUiState {
        public val isEmpty: Boolean get() = sections.isEmpty()
    }
}

public data class StationHeader(
    public val name: String?,
    /** IANA id the times are shown in, e.g. `Europe/Berlin`. */
    public val timeZoneId: String,
    /** Current offset of that zone, e.g. `UTC+01:00`. */
    public val utcOffset: String,
    /** The backend sent no usable zone and times are shown in UTC. */
    public val isTimeZoneFallback: Boolean,
)

public data class DepartureSection(
    public val date: LocalDate,
    public val day: RelativeDay,
    public val items: List<DepartureItem>,
)

public enum class RelativeDay { TODAY, TOMORROW, OTHER }

public data class DepartureItem(
    public val id: String,
    public val lineCode: String,
    public val direction: String?,
    /** Scheduled departure in the station time zone. */
    public val scheduledTime: LocalTime,
    /** Present only when the bus is expected at least a minute after schedule. */
    public val delay: DelayInfo?,
)

public data class DelayInfo(
    public val estimatedTime: LocalTime,
    public val minutes: Int,
    /** The operator classifies the ride as late (as opposed to a small deviation still counted as on time). */
    public val isLate: Boolean,
)

public sealed interface TimetableEvent {
    /** A refresh the user asked for failed while older data stays on screen. */
    public data class RefreshFailed(
        public val error: DataError,
    ) : TimetableEvent
}
