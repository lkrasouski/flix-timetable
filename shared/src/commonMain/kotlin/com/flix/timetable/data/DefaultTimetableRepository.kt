package com.flix.timetable.data

import co.touchlab.kermit.Logger
import com.flix.timetable.core.AppResult
import com.flix.timetable.data.remote.TimetableApi
import com.flix.timetable.domain.Timetable
import com.flix.timetable.domain.TimetableRepository
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Instant

internal class DefaultTimetableRepository(
    private val api: TimetableApi,
    private val mapper: DeparturesMapper,
    private val logger: Logger,
) : TimetableRepository {
    override suspend fun departures(
        from: Instant,
        to: Instant,
    ): AppResult<Timetable> =
        try {
            AppResult.Success(mapper.map(api.departures(from, to)))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val error = e.toDataError()
            logger.w(e) { "Loading departures failed: $error" }
            AppResult.Failure(error)
        }
}
