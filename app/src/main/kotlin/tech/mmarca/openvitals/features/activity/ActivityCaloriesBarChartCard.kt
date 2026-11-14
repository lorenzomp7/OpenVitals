package tech.mmarca.openvitals.features.activity

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import java.time.Duration
import java.time.Instant
import java.util.Locale
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.insights.CalorieIntervalSource
import tech.mmarca.openvitals.domain.insights.SessionCalories
import tech.mmarca.openvitals.ui.components.chartSemantics
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.formatElapsedChartLabel
import tech.mmarca.openvitals.ui.theme.CaloriesColor
import tech.mmarca.openvitals.ui.theme.Spacing

private val CaloriesChartHeight = Spacing.xl * 8
private const val CaloriesAxisLabels = 4

/**
 * 64: the energy burned in each slice of the session (a minute, or more for a
 * long one) as bars: how hard each part was, not a running total. From heart
 * rate with the Keytel equation when the profile allows it, else from the
 * calories an app recorded.
 */
@Composable
internal fun ActivityCaloriesBarChartCard(
    calories: SessionCalories,
    sessionStart: Instant,
    sessionEnd: Instant,
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
) {
    if (calories.intervals.isEmpty() || calories.totalKcal < 1.0) return
    val bucketMinutes = calories.bucket.toMinutes().coerceAtLeast(1L)
    val sessionMinutes = Duration.between(sessionStart, sessionEnd).seconds / 60.0
    val averagePerMinute = if (sessionMinutes > 0.0) calories.totalKcal / sessionMinutes else 0.0
    val peakPerMinute = calories.peakKcal / bucketMinutes
    val spanMillis = Duration.between(sessionStart, sessionEnd).toMillis().coerceAtLeast(1L).toFloat()
    val bars = remember(calories, sessionStart) {
        calories.intervals.map { interval ->
            Triple(
                Duration.between(sessionStart, interval.start).toMillis() / spanMillis,
                Duration.between(sessionStart, interval.end).toMillis() / spanMillis,
                interval.kcal,
            )
        }
    }
    val maxKcal = calories.peakKcal.coerceAtLeast(0.1)
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    OpenVitalsCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            val title = stringResource(R.string.activity_calories_per_interval_title, bucketMinutes.toInt())
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                CaloriesStat(stringResource(R.string.stat_total), unitFormatter.energy(calories.totalKcal).text, Modifier.weight(1f))
                CaloriesStat(stringResource(R.string.summary_average), kcalPerMinute(averagePerMinute), Modifier.weight(1f))
                CaloriesStat(stringResource(R.string.activity_calories_peak), kcalPerMinute(peakPerMinute), Modifier.weight(1f))
            }
            val spoken = "$title, ${unitFormatter.energy(calories.totalKcal).text}, ${kcalPerMinute(averagePerMinute)}"
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(CaloriesChartHeight)
                    .chartSemantics(spoken),
            ) {
                val gap = (Spacing.xs / 4).toPx()
                for (line in 1..3) {
                    val y = size.height * line / 4f
                    drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                }
                bars.forEach { (from, to, kcal) ->
                    val left = from * size.width + gap / 2f
                    val width = ((to - from) * size.width - gap).coerceAtLeast(1f)
                    val height = (kcal / maxKcal).toFloat() * size.height
                    drawRoundRect(
                        color = CaloriesColor,
                        topLeft = Offset(left, size.height - height),
                        size = Size(width, height),
                        cornerRadius = CornerRadius(width / 4f, width / 4f),
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                for (index in 0..CaloriesAxisLabels) {
                    val elapsed = Duration.ofMillis((spanMillis * index / CaloriesAxisLabels).toLong())
                    Text(
                        text = formatElapsedChartLabel(elapsed),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = when (index) {
                            0 -> TextAlign.Start
                            CaloriesAxisLabels -> TextAlign.End
                            else -> TextAlign.Center
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Text(
                text = stringResource(
                    if (calories.source == CalorieIntervalSource.HEART_RATE) {
                        R.string.activity_calories_from_heart_rate
                    } else {
                        R.string.activity_calories_from_recorded
                    },
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun kcalPerMinute(value: Double): String = String.format(Locale.getDefault(), "%.1f kcal/min", value)

@Composable
private fun CaloriesStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = value, style = MaterialTheme.typography.titleMedium, color = CaloriesColor)
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
