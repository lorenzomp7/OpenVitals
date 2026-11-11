package tech.mmarca.openvitals.features.dashboard

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.ui.theme.WorkoutActionMetrics

internal const val DashboardCarouselEdgeScrollDelayMillis = 450L
internal val DashboardCarouselEdgeScrollThreshold = 56.dp
internal val DashboardScreenPadding = WorkoutActionMetrics.screenGutter
internal val DashboardSectionSeparatorSpacing = 4.dp
internal val DashboardQuickActionHeight = WorkoutActionMetrics.height
internal val DashboardActionsSpacing = WorkoutActionMetrics.spacing
internal val DashboardQuickActionIconSize = 18.dp

// Tighter than the default, so "Start workout" fits on one line.
internal val DashboardQuickActionContentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
