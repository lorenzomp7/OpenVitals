package tech.mmarca.openvitals.features.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.ui.theme.Spacing
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.ExerciseData
import tech.mmarca.openvitals.ui.components.MetricCardPlaceholder
import tech.mmarca.openvitals.ui.theme.WorkoutColor
import java.time.ZoneId

internal fun LazyListScope.dashboardActivitiesToday(
    workouts: List<ExerciseData>,
    selectedDate: java.time.LocalDate? = null,
    isLoading: Boolean = false,
    zone: ZoneId,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onOpenActivities: () -> Unit,
    onOpenActivity: (String) -> Unit,
    onEditActivity: (String) -> Unit,
    onRequestDeleteActivity: (ExerciseData) -> Unit,
) {
    // Always the same four symbols: running, strength training, biking and swimming. They
    // were the kinds of the day's activities, so a day with two kinds drew two of the four.
    val recentKinds = ActivityBarKinds
    item(key = "activities_header") {
        DashboardActivitiesSectionHeader(recentKinds = recentKinds, onClick = onOpenActivities)
    }
    if (workouts.isNotEmpty()) {
        items(
            count = workouts.size,
            key = { index -> workouts[index].id.ifBlank { "workout_$index" } },
        ) { index ->
            val workout = workouts[index]
            // A date heading where the list crosses into another day.
            val day = workout.startTime.atZone(zone).toLocalDate()
            val previousDay = workouts.getOrNull(index - 1)?.startTime?.atZone(zone)?.toLocalDate()
            val dayHeading = day.takeIf { (index > 0 && it != previousDay) || (index == 0 && selectedDate != null && it != selectedDate) }
            val editable = workout.isOpenVitalsEntry && workout.id.isNotBlank()
            val openAction = workout.id.takeIf { it.isNotBlank() }?.let { activityId ->
                { onOpenActivity(activityId) }
            }
            val editAction = if (editable) {
                { onEditActivity(workout.id) }
            } else {
                null
            }
            val cardContent: @Composable (Modifier) -> Unit = { cardModifier ->
                WorkoutCard(
                    workout = workout,
                    zone = zone,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    modifier = cardModifier,
                    onClick = openAction,
                    onEdit = editAction,
                )
            }
            if (dayHeading != null) {
                Text(
                    text = dateTimeFormatterProvider.mediumDate().format(dayHeading),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        start = DashboardScreenPadding,
                        end = DashboardScreenPadding,
                        top = 10.dp,
                    ),
                )
            }
            if (editable) {
                DashboardSwipeToDeleteActivityCard(
                    onDelete = { onRequestDeleteActivity(workout) },
                    modifier = Modifier.padding(
                        horizontal = DashboardScreenPadding,
                        vertical = 6.dp,
                    ),
                ) {
                    cardContent(Modifier)
                }
            } else {
                cardContent(
                    Modifier.padding(
                        horizontal = DashboardScreenPadding,
                        vertical = 6.dp,
                    )
                )
            }
        }
    } else {
        item(key = "activities_empty") {
            MetricCardPlaceholder(
                title = stringResource(R.string.section_activities),
                icon = Icons.AutoMirrored.Outlined.DirectionsRun,
                accentColor = WorkoutColor,
                message = stringResource(
                    if (isLoading) R.string.loading else R.string.message_no_workouts_day,
                ),
                modifier = Modifier.padding(
                    horizontal = DashboardScreenPadding,
                    vertical = 6.dp,
                ),
                showHeader = false,
                onClick = onOpenActivities,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DashboardSwipeToDeleteActivityCard(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val currentOnDelete by rememberUpdatedState(onDelete)
    val scope = rememberCoroutineScope()
    val dismissState = rememberSwipeToDismissBoxState()
    val shape = MaterialTheme.shapes.medium

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        onDismiss = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                currentOnDelete()
            }
            scope.launch {
                dismissState.reset()
            }
        },
        backgroundContent = {
            // Painted only while displaced, as in SwipeToDeleteEntryRow.
            if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(shape)
                        .background(MaterialTheme.colorScheme.errorContainer, shape)
                        .padding(horizontal = Spacing.xxl),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.cd_delete_entry),
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        },
        modifier = modifier.clip(shape),
        content = { content() },
    )
}

/**
 * The Activities bar runs the width of the screen. Its label stays over the left column,
 * centred in it, and the right half shows running, strength training, biking and swimming,
 * dimmed, lighting up one after the other on the toggle's clock.
 */
@Composable
private fun DashboardActivitiesSectionHeader(
    recentKinds: List<Int>,
    onClick: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth().padding(start = DashboardScreenPadding, end = DashboardScreenPadding, top = DashboardSectionGap, bottom = Spacing.md / 2)) {
        // The pill pads its content by Spacing.md each side; the left half is the left column less that.
        val column = (maxWidth - DashboardTileGap) / 2 - Spacing.md
        val rest = maxWidth - Spacing.md * 2 - column
        val timeline = tech.mmarca.openvitals.ui.components.LocalFluidBrandTimeline.current
            ?: androidx.compose.runtime.remember { tech.mmarca.openvitals.ui.components.FluidBrandTimeline() }
        val clock = tech.mmarca.openvitals.ui.components.rememberBrandClock(timeline)
        tech.mmarca.openvitals.ui.components.BrandPillButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().height(DashboardQuickActionHeight),
        ) {
            Row(Modifier.width(column), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Outlined.DirectionsRun, null, Modifier.size(DashboardQuickActionIconSize))
                Spacer(Modifier.width(Spacing.md / 2))
                Text(stringResource(R.string.dashboard_activities_today), maxLines = 1)
            }
            Row(
                Modifier.width(rest),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                recentKinds.forEachIndexed { index, kind ->
                    Icon(
                        imageVector = tech.mmarca.openvitals.features.activity.exerciseTypeIcon(kind),
                        contentDescription = tech.mmarca.openvitals.features.activity.exerciseTypeLabel(kind),
                        modifier = Modifier
                            .size(DashboardQuickActionIconSize)
                            .graphicsLayer {
                                alpha = recentKindAlpha(timeline.secondsAt(clock.value), index, recentKinds.size)
                            },
                    )
                }
            }
        }
    }
}

private val ActivityBarKinds = listOf(
    androidx.health.connect.client.records.ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
    androidx.health.connect.client.records.ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
    androidx.health.connect.client.records.ExerciseSessionRecord.EXERCISE_TYPE_BIKING,
    androidx.health.connect.client.records.ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL,
)

/** How long each kind stays lit before the next takes over. */
private const val RecentKindSeconds = 1.6f
private const val RecentKindDim = 0.35f

/** Lit at its turn, easing up and back down, dim the rest of the time. */
private fun recentKindAlpha(seconds: Float, index: Int, count: Int): Float {
    if (count <= 1) return 1f
    val position = (seconds / RecentKindSeconds) % count
    // Distance from this icon's turn, round the cycle, in turns.
    val distance = kotlin.math.abs(((position - index) % count + count + count / 2f) % count - count / 2f)
    val lit = (1f - distance).coerceIn(0f, 1f)
    return RecentKindDim + (1f - RecentKindDim) * lit
}
