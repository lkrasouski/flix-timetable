package com.flix.timetable.testing

/** Trimmed real response of the departures endpoint (Berlin ZOB, 2026-11-25) plus the edge cases we must survive. */
internal const val DEPARTURES_JSON = """
{
  "rides": [
    {
      "id": "547795e7-f0bc-49ee-872b-33e5b3ce2b44",
      "status": {
        "segment": null,
        "has_arrived_at_next_stop": false,
        "deviation": {
          "deviation_timestamp": "2026-11-25T16:08:04Z",
          "deviation_seconds": 484,
          "reason": null,
          "deviation_class": "ON_TIME",
          "deviation_type": "ESTIMATED",
          "updated_at": "2026-11-25T15:04:06Z"
        },
        "scheduled_timestamp": "2026-11-25T16:00:00Z"
      },
      "platform": null,
      "line": {
        "code": "N3217",
        "direction": "Chernihiv (Chernihiv-1 Bus Station)",
        "name": "Chernihiv - Berlin - Cologne",
        "trip_number": 2,
        "means_of_transport": "BUS",
        "brand": { "id": "a18f138c-68fa-4b45-a42f-adb0378e10d3", "name": "FlixBus" }
      },
      "brand": null
    },
    {
      "id": "91d38988-f6cf-4557-a22c-cc12e40cb70c",
      "status": {
        "segment": null,
        "deviation": null,
        "scheduled_timestamp": "2026-11-25T15:20:00Z"
      },
      "line": {
        "code": "N958",
        "direction": "Rijeka bus station",
        "name": "Rijeka - Prague - Berlin",
        "means_of_transport": "BUS"
      }
    },
    {
      "id": "91d38988-f6cf-4557-a22c-cc12e40cb70c",
      "status": { "scheduled_timestamp": "2026-11-25T15:20:00Z" },
      "line": { "code": "N958", "direction": "Rijeka bus station" }
    },
    {
      "id": "no-time",
      "status": { "scheduled_timestamp": null },
      "line": { "code": "X1", "direction": "Nowhere" }
    },
    {
      "id": "bad-time",
      "status": { "scheduled_timestamp": "yesterday-ish" },
      "line": { "code": "X2", "direction": "Nowhere" }
    },
    {
      "id": "no-line",
      "status": { "scheduled_timestamp": "2026-11-25T15:30:00Z" },
      "line": null
    },
    {
      "id": "late-ride",
      "status": {
        "deviation": {
          "deviation_timestamp": "2026-11-25T16:47:00Z",
          "deviation_seconds": 1620,
          "deviation_class": "LATE"
        },
        "scheduled_timestamp": "2026-11-25T16:20:00Z"
      },
      "line": { "code": "N160", "direction": "  ", "name": "Berlin - Trieste" }
    }
  ],
  "station": {
    "id": "dcbaaee6-9603-11e6-9066-549f350fcb0c",
    "name": "Berlin central bus station",
    "timezone": "Europe/Berlin",
    "description": "Gate information available here: https://zob.berlin/en",
    "coordinates": { "latitude": 52.507171, "longitude": 13.279399 }
  }
}
"""
