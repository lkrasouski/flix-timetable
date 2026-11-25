package com.flix.timetable.core

/** Outcome of a data operation: either a value or a [DataError] — exceptions never cross the data layer. */
public sealed interface AppResult<out T> {
    public data class Success<out T>(
        public val value: T,
    ) : AppResult<T>

    public data class Failure(
        public val error: DataError,
    ) : AppResult<Nothing>
}
