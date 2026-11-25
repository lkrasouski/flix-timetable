package com.flix.timetable.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flix.timetable.core.AppResult
import com.flix.timetable.domain.GetUpcomingDeparturesUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * Drives the timetable screen.
 *
 * - Loads on creation.
 * - [refresh] (pull-to-refresh) keeps the current list on screen and reports a failure as a one-off [events] item.
 * - [startAutoRefresh]/[stopAutoRefresh] bind periodic reloads to screen visibility: the list is a live board, and
 *   the window "now … now + 2h" moves on. Coming back after a long pause reloads immediately.
 */
public class TimetableViewModel(
    private val getUpcomingDepartures: GetUpcomingDeparturesUseCase,
    private val clock: Clock,
    private val autoRefreshInterval: Duration = DEFAULT_AUTO_REFRESH_INTERVAL,
) : ViewModel() {
    private val _state = MutableStateFlow<TimetableUiState>(TimetableUiState.Loading)
    public val state: StateFlow<TimetableUiState> = _state.asStateFlow()

    private val _events = Channel<TimetableEvent>(Channel.BUFFERED)
    public val events: Flow<TimetableEvent> = _events.receiveAsFlow()

    private var loadJob: Job? = null
    private var autoRefreshJob: Job? = null
    private var lastAttemptAt: Instant? = null

    init {
        load(userInitiated = false)
    }

    /** Pull-to-refresh. */
    public fun refresh() {
        load(userInitiated = true)
    }

    /** Retry button of the error state. */
    public fun retry() {
        load(userInitiated = true)
    }

    public fun startAutoRefresh() {
        if (autoRefreshJob?.isActive == true) return
        autoRefreshJob =
            viewModelScope.launch {
                while (true) {
                    val sinceLastAttempt = lastAttemptAt?.let { clock.now() - it } ?: Duration.ZERO
                    delay((autoRefreshInterval - sinceLastAttempt).coerceAtLeast(Duration.ZERO))
                    load(userInitiated = false)
                    loadJob?.join()
                }
            }
    }

    public fun stopAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = null
    }

    private fun load(userInitiated: Boolean) {
        if (loadJob?.isActive == true) return
        loadJob =
            viewModelScope.launch {
                lastAttemptAt = clock.now()
                val previous = _state.value
                _state.value =
                    when (previous) {
                        is TimetableUiState.Loaded -> previous.copy(isRefreshing = userInitiated)
                        else -> TimetableUiState.Loading
                    }

                when (val result = getUpcomingDepartures()) {
                    is AppResult.Success -> _state.value = result.value.toUiState(clock.now())
                    is AppResult.Failure ->
                        if (previous is TimetableUiState.Loaded) {
                            _state.update { (it as? TimetableUiState.Loaded)?.copy(isRefreshing = false) ?: it }
                            if (userInitiated) _events.send(TimetableEvent.RefreshFailed(result.error))
                        } else {
                            _state.value = TimetableUiState.Error(result.error)
                        }
                }
            }
    }

    public companion object {
        public val DEFAULT_AUTO_REFRESH_INTERVAL: Duration = 60.seconds
    }
}
