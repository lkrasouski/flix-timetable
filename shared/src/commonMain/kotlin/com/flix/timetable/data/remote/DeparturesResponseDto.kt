package com.flix.timetable.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Wire models of `GET gis/v2/timetable/{stationId}/departures`. Only the fields the app uses are declared and every one
// of them is optional: the real payload has plenty of nulls, and one broken ride must not break the whole timetable.
// Timestamps stay strings here and are parsed per ride in DeparturesMapper for the same reason.

@Serializable
internal data class DeparturesResponseDto(
    val rides: List<RideDto>? = null,
    val station: StationDto? = null,
)

@Serializable
internal data class RideDto(
    val id: String? = null,
    val status: RideStatusDto? = null,
    val line: LineDto? = null,
)

@Serializable
internal data class RideStatusDto(
    @SerialName("scheduled_timestamp") val scheduledTimestamp: String? = null,
    val deviation: DeviationDto? = null,
)

@Serializable
internal data class DeviationDto(
    @SerialName("deviation_timestamp") val deviationTimestamp: String? = null,
    @SerialName("deviation_seconds") val deviationSeconds: Long? = null,
    @SerialName("deviation_class") val deviationClass: String? = null,
)

@Serializable
internal data class LineDto(
    val code: String? = null,
    val direction: String? = null,
    val name: String? = null,
)

@Serializable
internal data class StationDto(
    val id: String? = null,
    val name: String? = null,
    val timezone: String? = null,
)
