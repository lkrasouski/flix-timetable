package com.flix.timetable.android.ui.timetable

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flix.timetable.android.R
import com.flix.timetable.core.DataError
import com.flix.timetable.presentation.TimetableEvent
import com.flix.timetable.presentation.TimetableViewModel
import org.koin.compose.viewmodel.koinViewModel

/** Connects [TimetableViewModel] to the stateless [TimetableScreen]. */
@Composable
fun TimetableRoute(viewModel: TimetableViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current

    // The board is live only while visible: no polling in the background.
    LifecycleStartEffect(viewModel) {
        viewModel.startAutoRefresh()
        onStopOrDispose { viewModel.stopAutoRefresh() }
    }

    LaunchedEffect(viewModel, resources) {
        viewModel.events.collect { event ->
            when (event) {
                is TimetableEvent.RefreshFailed -> {
                    val message =
                        if (event.error == DataError.NoConnection) {
                            R.string.timetable_refresh_failed_offline
                        } else {
                            R.string.timetable_refresh_failed
                        }
                    snackbarHostState.showSnackbar(resources.getString(message))
                }
            }
        }
    }

    TimetableScreen(
        state = state,
        onRefresh = viewModel::refresh,
        onRetry = viewModel::retry,
        snackbarHostState = snackbarHostState,
    )
}
