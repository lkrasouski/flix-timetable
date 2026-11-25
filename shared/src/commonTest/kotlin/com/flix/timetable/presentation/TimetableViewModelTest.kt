package com.flix.timetable.presentation

import app.cash.turbine.test
import com.flix.timetable.core.AppResult
import com.flix.timetable.core.DataError
import com.flix.timetable.domain.Departure
import com.flix.timetable.domain.GetUpcomingDeparturesUseCase
import com.flix.timetable.domain.Station
import com.flix.timetable.domain.Timetable
import com.flix.timetable.domain.TimetableRepository
import com.flix.timetable.testing.TestClock
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class TimetableViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val start = Instant.parse("2026-11-25T15:00:00Z")
    private val repository = FakeRepository()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.viewModel(): TimetableViewModel {
        val clock = TestClock(start, testScheduler)
        return TimetableViewModel(GetUpcomingDeparturesUseCase(repository, clock), clock, 60.seconds)
    }

    @Test
    fun `loads departures for the next two hours on start`() =
        runTest(dispatcher) {
            repository.result = success(1)
            val vm = viewModel()

            vm.state.test {
                assertEquals(TimetableUiState.Loading, awaitItem())
                val loaded = assertIs<TimetableUiState.Loaded>(awaitItem())
                assertEquals(
                    1,
                    loaded.sections
                        .single()
                        .items.size,
                )
            }
            assertEquals(start to start + 2.hours, repository.requests.single())
        }

    @Test
    fun `empty window is a loaded state without sections`() =
        runTest(dispatcher) {
            repository.result = success(0)
            val vm = viewModel()
            advanceUntilIdle()

            assertTrue(assertIs<TimetableUiState.Loaded>(vm.state.value).isEmpty)
        }

    @Test
    fun `first load failure shows the error and retry recovers`() =
        runTest(dispatcher) {
            repository.result = AppResult.Failure(DataError.NoConnection)
            val vm = viewModel()
            advanceUntilIdle()
            assertEquals(TimetableUiState.Error(DataError.NoConnection), vm.state.value)

            repository.result = success(2)
            vm.retry()
            runCurrent()

            assertIs<TimetableUiState.Loaded>(vm.state.value)
        }

    @Test
    fun `failed pull-to-refresh keeps the data and emits an event`() =
        runTest(dispatcher) {
            repository.result = success(2)
            val vm = viewModel()
            advanceUntilIdle()
            val loaded = vm.state.value

            vm.events.test {
                repository.result = AppResult.Failure(DataError.Server(503))
                vm.refresh()
                runCurrent()

                assertEquals(TimetableEvent.RefreshFailed(DataError.Server(503)), awaitItem())
                assertEquals(loaded, vm.state.value)
            }
        }

    @Test
    fun `pull-to-refresh shows the refresh indicator while loading`() =
        runTest(dispatcher) {
            repository.result = success(1)
            val vm = viewModel()
            advanceUntilIdle()

            val gate = CompletableDeferred<Unit>()
            repository.gate = gate
            vm.refresh()
            runCurrent()
            assertTrue(assertIs<TimetableUiState.Loaded>(vm.state.value).isRefreshing)

            gate.complete(Unit)
            runCurrent()
            assertEquals(false, assertIs<TimetableUiState.Loaded>(vm.state.value).isRefreshing)
        }

    @Test
    fun `parallel refresh requests are coalesced`() =
        runTest(dispatcher) {
            repository.result = success(1)
            val vm = viewModel()
            vm.refresh()
            vm.refresh()
            advanceUntilIdle()

            assertEquals(1, repository.requests.size)
        }

    @Test
    fun `auto refresh reloads every interval while started and stops when hidden`() =
        runTest(dispatcher) {
            repository.result = success(1)
            val vm = viewModel()
            advanceUntilIdle()
            vm.startAutoRefresh()

            advanceTimeBy(59.seconds)
            runCurrent()
            assertEquals(1, repository.requests.size)
            advanceTimeBy(1.seconds)
            runCurrent()
            assertEquals(2, repository.requests.size)
            advanceTimeBy(60.seconds)
            runCurrent()
            assertEquals(3, repository.requests.size)
            assertEquals(start + 2.minutes, repository.requests.last().first)

            vm.stopAutoRefresh()
            advanceTimeBy(10.minutes)
            runCurrent()
            assertEquals(3, repository.requests.size)
        }

    @Test
    fun `coming back after a long pause reloads immediately`() =
        runTest(dispatcher) {
            repository.result = success(1)
            val vm = viewModel()
            advanceUntilIdle()

            advanceTimeBy(5.minutes)
            vm.startAutoRefresh()
            runCurrent()

            assertEquals(2, repository.requests.size)
            vm.stopAutoRefresh()
        }

    @Test
    fun `silent auto refresh failure keeps data without an event`() =
        runTest(dispatcher) {
            repository.result = success(1)
            val vm = viewModel()
            advanceUntilIdle()
            val loaded = vm.state.value

            vm.events.test {
                repository.result = AppResult.Failure(DataError.Timeout)
                vm.startAutoRefresh()
                advanceTimeBy(61.seconds)
                runCurrent()
                vm.stopAutoRefresh()

                expectNoEvents()
                assertEquals(loaded, vm.state.value)
            }
        }

    private fun success(count: Int): AppResult<Timetable> =
        AppResult.Success(
            Timetable(
                Station("Berlin", TimeZone.of("Europe/Berlin"), isTimeZoneFallback = false),
                List(count) { Departure("$it", "N$it", "Somewhere", start + (10 * (it + 1)).minutes, null) },
            ),
        )

    private class FakeRepository : TimetableRepository {
        var result: AppResult<Timetable> = AppResult.Failure(DataError.Unknown)
        var gate: CompletableDeferred<Unit>? = null
        val requests = mutableListOf<Pair<Instant, Instant>>()

        override suspend fun departures(
            from: Instant,
            to: Instant,
        ): AppResult<Timetable> {
            requests += from to to
            gate?.await()
            return result
        }
    }
}
