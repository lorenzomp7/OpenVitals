package tech.mmarca.openvitals.features.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import kotlin.math.roundToInt
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.ExerciseLapData
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.theme.Spacing

/**
 * 118: the laps the watch wrote, one row each: its length, its time and its pace (per 100 m for
 * a swim, per kilometre otherwise). A pool watch writes a lap per length swum.
 */
@Composable
internal fun ActivityLapsCard(
    laps: List<ExerciseLapData>,
    swim: Boolean,
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
) {
    val sorted = laps.sortedBy { it.startTime }
    if (sorted.isEmpty()) return
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    OpenVitalsCard(modifier = modifier) {
        Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(
                "${stringResource(R.string.detail_laps)} · ${sorted.size}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            LapRow(
                "#",
                stringResource(R.string.detail_length),
                stringResource(R.string.detail_duration),
                stringResource(R.string.activity_chart_pace_title),
                style = MaterialTheme.typography.labelSmall,
                color = muted,
            )
            sorted.forEachIndexed { index, lap ->
                val meters = lap.lengthMeters?.takeIf { it > 0.0 }
                val pace = meters?.let { length ->
                    if (swim) {
                        val per100 = (lap.durationMs / 1_000.0 / length * 100.0).roundToInt()
                        "${per100 / 60}:${(per100 % 60).toString().padStart(2, '0')} /100 m"
                    } else {
                        unitFormatter.averagePace(length, lap.durationMs)?.text
                    }
                } ?: "–"
                LapRow(
                    (index + 1).toString(),
                    meters?.let { unitFormatter.distance(it).text } ?: "–",
                    unitFormatter.duration(lap.durationMs),
                    pace,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun LapRow(
    index: String,
    length: String,
    time: String,
    pace: String,
    style: androidx.compose.ui.text.TextStyle,
    color: androidx.compose.ui.graphics.Color,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(index, style = style, color = color, modifier = Modifier.width(Spacing.xl))
        Text(length, style = style, color = color, modifier = Modifier.weight(1f))
        Text(time, style = style, color = color, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        Text(pace, style = style, color = color, modifier = Modifier.weight(1.3f), textAlign = TextAlign.End)
    }
}
