package tech.mmarca.openvitals.features.dashboard.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.features.dashboard.DashboardWidgetProgress
import tech.mmarca.openvitals.ui.components.AutoResizeText
import tech.mmarca.openvitals.ui.components.ChartReveal
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import androidx.compose.foundation.shape.RoundedCornerShape
import tech.mmarca.openvitals.ui.theme.Radii

private val SummaryCardPadding = 10.dp
private val SummaryHeroRingSize = 200.dp
private val SummaryArcStartAngle = 130f
private val SummaryArcSweepAngle = 280f

@Composable
internal fun DashboardSummaryCard(
    title: String,
    value: DisplayValue,
    @Suppress("UNUSED_PARAMETER") icon: ImageVector,
    accentColor: Color,
    progress: DashboardWidgetProgress,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val progressTrackColor = accentColor.copy(alpha = 0.24f)
    val progressFillColor = accentColor
    val subtitle = summarySubtitle(value = value, progress = progress)

    OpenVitalsCard(
        modifier = modifier.fillMaxSize(),
        onClick = onClick,
        containerColor = dashboardTileContainerColor(accentColor),
        shape = RoundedCornerShape(Radii.xl),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(SummaryCardPadding),
            contentAlignment = Alignment.Center,
        ) {
            val ringSize = minOf(maxWidth, maxHeight)
            val strokeWidth = summaryRingStroke(ringSize)
            // The hero ring is large enough for display type.
            val isHero = ringSize >= SummaryHeroRingSize

            Box(
                modifier = Modifier.size(ringSize),
                contentAlignment = Alignment.Center,
            ) {
                // The arc sweeps round to its value: on a goal ring that is the point.
                ChartReveal { revealProgress ->
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokePx = strokeWidth.toPx()
                        val diameter = size.minDimension - strokePx
                        val topLeft = Offset(
                            x = (size.width - diameter) / 2f,
                            y = (size.height - diameter) / 2f,
                        )
                        val arcSize = Size(diameter, diameter)
                        drawArc(
                            color = progressTrackColor,
                            startAngle = SummaryArcStartAngle,
                            sweepAngle = SummaryArcSweepAngle,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokePx, cap = StrokeCap.Round),
                        )
                        drawArc(
                            color = progressFillColor,
                            sweepAngle = SummaryArcSweepAngle *
                                progress.fraction.coerceIn(0f, 1f) * revealProgress,
                            startAngle = SummaryArcStartAngle,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokePx, cap = StrokeCap.Round),
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(strokeWidth + 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    AutoResizeText(
                        text = title,
                        style = if (isHero) {
                            MaterialTheme.typography.titleSmall
                        } else {
                            MaterialTheme.typography.labelSmall
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                    AutoResizeText(
                        text = value.value,
                        style = if (isHero) {
                            MaterialTheme.typography.displayMedium
                        } else {
                            MaterialTheme.typography.headlineSmall
                        },
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                    if (subtitle.isNotBlank()) {
                        AutoResizeText(
                            text = subtitle,
                            style = if (isHero) {
                                MaterialTheme.typography.bodyMedium
                            } else {
                                MaterialTheme.typography.labelSmall
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                        )
                    }
                }
            }
        }
    }
}

private fun summarySubtitle(
    value: DisplayValue,
    progress: DashboardWidgetProgress,
): String = when {
    value.unit.isNotBlank() && progress.label.isNotBlank() -> "${value.unit} ${progress.label}"
    value.unit.isNotBlank() -> value.unit
    else -> progress.label
}

private fun summaryRingStroke(ringSize: Dp): Dp =
    (ringSize * 0.09f).coerceIn(5.dp, 18.dp)
