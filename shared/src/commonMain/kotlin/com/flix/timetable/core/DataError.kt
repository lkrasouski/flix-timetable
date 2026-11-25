package com.flix.timetable.core

/** Failure reasons the UI can tell apart and explain to the user. */
public sealed interface DataError {
    /** The device is offline or the host cannot be reached. */
    public data object NoConnection : DataError

    /** The request took too long. */
    public data object Timeout : DataError

    /** 5xx: the problem is on the backend side; retrying later may help. */
    public data class Server(
        public val httpCode: Int,
    ) : DataError

    /** 4xx: the request was rejected (wrong key, unknown station, …); retrying will not help. */
    public data class Client(
        public val httpCode: Int,
    ) : DataError

    /** The backend answered with something we could not understand. */
    public data object InvalidResponse : DataError

    /** Anything else. */
    public data object Unknown : DataError
}
