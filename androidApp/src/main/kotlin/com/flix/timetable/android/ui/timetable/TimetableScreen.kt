package com.flix.timetable.android.ui.timetable

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.flix.timetable.android.R
import com.flix.timetable.core.DataError
import com.flix.timetable.presentation.DepartureItem
import com.flix.timetable.presentation.DepartureSection
import com.flix.timetable.presentation.RelativeDay
import com.flix.timetable.presentation.StationHeader
import com.flix.timetable.presentation.TimetableUiState

internal object TimetableTestTags {
    const val LOADING = "timetable_loading"
    const val LIST = "timetable_list"
    const val EMPTY = "timetable_empty"
    const val ERROR = "timetable_error"
    const val RETRY = "timetable_retry"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(
    state: TimetableUiState,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    formatter: TimeFormatter = rememberTimeFormatter(),
) {
    Scaffold(
        modifier = modifier,
        topBar = { TimetableTopBar(station = (state as? TimetableUiState.Loaded)?.station) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when (state) {
            TimetableUiState.Loading -> LoadingContent(Modifier.padding(padding))
            is TimetableUiState.Error -> ErrorContent(state.error, onRetry, Modifier.padding(padding))
            is TimetableUiState.Loaded ->
                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.padding(top = padding.calculateTopPadding()).fillMaxSize(),
                ) {
                    LoadedContent(
                        state = state,
                        formatter = formatter,
                        contentPadding = PaddingValues(bottom = padding.calculateBottomPadding()),
                    )
                }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimetableTopBar(station: StationHeader?) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = station?.name ?: stringResource(R.string.timetable_default_title),
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (station != null) {
                    Text(
                        text =
                            if (station.isTimeZoneFallback) {
                                stringResource(R.string.timetable_time_zone_fallback)
                            } else {
                                stringResource(R.string.timetable_time_zone, station.timeZoneId, station.utcOffset)
                            },
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
    )
}

@Composable
private fun LoadedContent(
    state: TimetableUiState.Loaded,
    formatter: TimeFormatter,
    contentPadding: PaddingValues,
) {
    LazyColumn(
        modifier =
            Modifier.fillMaxSize().testTag(
                if (state.isEmpty) TimetableTestTags.EMPTY else TimetableTestTags.LIST,
            ),
        contentPadding = contentPadding,
    ) {
        if (state.isEmpty) {
            item(key = "empty") {
                EmptyContent(Modifier.fillParentMaxSize())
            }
        } else {
            state.sections.forEach { section -> departureSection(section, state.showDayHeaders, formatter) }
            item(key = "updated") {
                Text(
                    text = stringResource(R.string.timetable_updated_at, formatter.time(state.updatedAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                )
            }
        }
    }
}

private fun LazyListScope.departureSection(
    section: DepartureSection,
    showHeader: Boolean,
    formatter: TimeFormatter,
) {
    if (showHeader) {
        stickyHeader(key = "day-${section.date}") { DayHeader(section, formatter) }
    }
    items(section.items, key = { it.id }) { item ->
        DepartureRow(item, formatter)
        HorizontalDivider(
            modifier = Modifier.padding(start = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

@Composable
private fun DayHeader(
    section: DepartureSection,
    formatter: TimeFormatter,
) {
    val date = formatter.day(section.date)
    val text =
        when (section.day) {
            RelativeDay.TODAY -> stringResource(R.string.timetable_day_today, date)
            RelativeDay.TOMORROW -> stringResource(R.string.timetable_day_tomorrow, date)
            RelativeDay.OTHER -> date
        }
    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier =
                Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .semantics { heading() },
        )
    }
}

@Composable
internal fun DepartureRow(
    item: DepartureItem,
    formatter: TimeFormatter,
    modifier: Modifier = Modifier,
) {
    val time = formatter.time(item.scheduledTime)
    val description =
        buildString {
            append(
                if (item.direction != null) {
                    stringResource(R.string.timetable_departure_description, item.lineCode, item.direction!!, time)
                } else {
                    stringResource(R.string.timetable_departure_description_no_direction, item.lineCode, time)
                },
            )
            item.delay?.let {
                append(". ")
                append(
                    pluralStringResource(
                        R.plurals.timetable_delay_description,
                        it.minutes,
                        it.minutes,
                        formatter.time(it.estimatedTime),
                    ),
                )
            }
        }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.direction ?: stringResource(R.string.timetable_direction_unknown),
                style = MaterialTheme.typography.bodyLarge,
                color =
                    if (item.direction != null) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            LineCodeBadge(item.lineCode)
        }
        Spacer(Modifier.width(16.dp))
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.widthIn(min = 56.dp)) {
            Text(
                text = time,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            item.delay?.let { delay ->
                val colors = MaterialTheme.colorScheme
                val color = if (delay.isLate) colors.error else colors.onSurfaceVariant
                Text(
                    text = stringResource(R.string.timetable_delay, delay.minutes),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = color,
                )
                Text(
                    text = stringResource(R.string.timetable_expected_at, formatter.time(delay.estimatedTime)),
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                )
            }
        }
    }
}

@Composable
private fun LineCodeBadge(code: String) {
    Text(
        text = code,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier =
            Modifier
                .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.timetable_loading)
    val pulse by rememberInfiniteTransition(label = "skeleton").animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 800), RepeatMode.Reverse),
        label = "skeleton-alpha",
    )
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .testTag(TimetableTestTags.LOADING)
                .clearAndSetSemantics { contentDescription = description }
                .alpha(pulse),
    ) {
        repeat(8) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    SkeletonBlock(Modifier.fillMaxWidth(0.6f).height(16.dp))
                    Spacer(Modifier.height(8.dp))
                    SkeletonBlock(Modifier.width(48.dp).height(14.dp))
                }
                SkeletonBlock(Modifier.width(48.dp).height(18.dp))
            }
        }
    }
}

@Composable
private fun SkeletonBlock(modifier: Modifier) {
    Box(modifier.background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp)))
}

@Composable
private fun EmptyContent(modifier: Modifier = Modifier) {
    MessageContent(
        iconRes = R.drawable.ic_bus,
        title = stringResource(R.string.timetable_empty_title),
        message = stringResource(R.string.timetable_empty_message),
        modifier = modifier,
    )
}

@Composable
private fun ErrorContent(
    error: DataError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (title, message) = remember(error) { error.messages() }
    MessageContent(
        iconRes = if (error == DataError.NoConnection) R.drawable.ic_cloud_off else R.drawable.ic_error,
        title = stringResource(title),
        message = stringResource(message),
        modifier = modifier.fillMaxSize().testTag(TimetableTestTags.ERROR),
    ) {
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRetry, modifier = Modifier.testTag(TimetableTestTags.RETRY)) {
            Text(stringResource(R.string.timetable_retry))
        }
    }
}

private fun DataError.messages(): Pair<Int, Int> =
    when (this) {
        DataError.NoConnection ->
            R.string.timetable_error_no_connection_title to R.string.timetable_error_no_connection_message
        DataError.Timeout -> R.string.timetable_error_timeout_title to R.string.timetable_error_timeout_message
        is DataError.Server -> R.string.timetable_error_server_title to R.string.timetable_error_server_message
        is DataError.Client, DataError.InvalidResponse, DataError.Unknown ->
            R.string.timetable_error_generic_title to R.string.timetable_error_generic_message
    }

@Composable
private fun MessageContent(
    iconRes: Int,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: @Composable () -> Unit = {},
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        action()
    }
}
