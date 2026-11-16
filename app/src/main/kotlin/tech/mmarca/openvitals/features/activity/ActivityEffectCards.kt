package tech.mmarca.openvitals.features.activity

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import java.time.Duration
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.insights.FlatGradeLimit
import tech.mmarca.openvitals.domain.insights.GradientDistribution
import tech.mmarca.openvitals.domain.insights.TrainingEffect
import tech.mmarca.openvitals.domain.insights.TrainingEffectLevel
import tech.mmarca.openvitals.domain.insights.trainingEffectLevel
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.chartSemantics
import tech.mmarca.openvitals.ui.components.formatElapsedChartLabel
import tech.mmarca.openvitals.ui.theme.Spacing

private val UphillColor = Color(0xFFE57373)
private val FlatColor = Color(0xFFFFB74D)
private val DownhillColor = Color(0xFF64B5F6)
private val DonutSize = Spacing.giant * 3

@Composable
private fun EffectCardFrame(title: String, modifier: Modifier, note: String?, content: @Composable () -> Unit) {
    OpenVitalsCard(modifier = modifier) {
        Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
            note?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

/** 117: the time uphill, on the flat and downhill, as a ring and a legend with time and share. */
@Composable
internal fun ActivityGradientCard(distribution: GradientDistribution, modifier: Modifier = Modifier) {
    val total = distribution.totalSeconds.coerceAtLeast(1)
    val parts = listOf(
        Triple(stringResource(R.string.activity_chart_gradient_uphill), distribution.uphillSeconds, UphillColor),
        Triple(stringResource(R.string.activity_chart_gradient_flat), distribution.flatSeconds, FlatColor),
        Triple(stringResource(R.string.activity_chart_gradient_downhill), distribution.downhillSeconds, DownhillColor),
    )
    val title = stringResource(R.string.activity_chart_gradient_title)
    val percent = "${(FlatGradeLimit * 100).toInt()}%"
    val spoken = parts.joinToString(", ") { (label, seconds, _) -> "$label ${seconds * 100 / total}%" }
    EffectCardFrame(
        title = title,
        modifier = modifier,
        note = stringResource(R.string.activity_chart_gradient_note, "+$percent", "−$percent"),
    ) {
        Row(
            Modifier.fillMaxWidth().chartSemantics("$title: $spoken"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xl),
        ) {
            Canvas(Modifier.size(DonutSize)) {
                val stroke = size.minDimension * 0.14f
                val inset = stroke / 2f
                val arcSize = Size(size.width - stroke, size.height - stroke)
                val gap = if (parts.count { it.second > 0 } > 1) 4f else 0f
                var start = -90f
                parts.forEach { (_, seconds, color) ->
                    val sweep = 360f * seconds / total
                    if (sweep > 0f) {
                        drawArc(
                            color = color,
                            startAngle = start + gap / 2f,
                            sweepAngle = (sweep - gap).coerceAtLeast(0.5f),
                            useCenter = false,
                            topLeft = Offset(inset, inset),
                            size = arcSize,
                            style = Stroke(stroke, cap = StrokeCap.Butt),
                        )
                    }
                    start += sweep
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                parts.forEach { (label, seconds, color) ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        Box(Modifier.size(Spacing.sm).background(color, CircleShape))
                        Column(Modifier.weight(1f)) {
                            Text(label, style = MaterialTheme.typography.labelLarge)
                            Text(
                                formatElapsedChartLabel(Duration.ofSeconds(seconds)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text("${seconds * 100 / total}%", style = MaterialTheme.typography.labelLarge, color = color)
                    }
                }
            }
        }
    }
}

/** The colours of the effect gauge from no effect to overreaching, as the watches paint it. */
private val EffectGaugeColors = listOf(Color(0xFF64B5F6), Color(0xFF4DB6AC), Color(0xFFFFD54F), Color(0xFFFF8A65), Color(0xFFE57373))

@Composable
private fun effectLevelLabel(level: TrainingEffectLevel): String = stringResource(
    when (level) {
        TrainingEffectLevel.NONE -> R.string.activity_chart_effect_level_none
        TrainingEffectLevel.MINOR -> R.string.activity_chart_effect_level_minor
        TrainingEffectLevel.MAINTAINING -> R.string.activity_chart_effect_level_maintaining
        TrainingEffectLevel.IMPROVING -> R.string.activity_chart_effect_level_improving
        TrainingEffectLevel.HIGHLY_IMPROVING -> R.string.activity_chart_effect_level_highly
        TrainingEffectLevel.OVERREACHING -> R.string.activity_chart_effect_level_overreaching
    },
)

private fun effectColor(value: Double): Color =
    EffectGaugeColors[(value.toInt()).coerceIn(0, EffectGaugeColors.lastIndex)]

/**
 * 117: the aerobic and anaerobic effect, each a half-dial from 0 to 5 with its value and what
 * it means, and the session's load beside them. An estimate of OpenVitals, and said so.
 */
@Composable
internal fun ActivityTrainingEffectCard(effect: TrainingEffect, load: Int?, modifier: Modifier = Modifier) {
    val title = stringResource(R.string.activity_chart_effect_title)
    val aerobic = stringResource(R.string.activity_chart_effect_aerobic)
    val anaerobic = stringResource(R.string.activity_chart_effect_anaerobic)
    EffectCardFrame(title = title, modifier = modifier, note = stringResource(R.string.activity_chart_effect_note)) {
        Row(
            Modifier.fillMaxWidth().chartSemantics("$title: $aerobic ${effect.aerobic}, $anaerobic ${effect.anaerobic}"),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.Bottom,
        ) {
            EffectGauge(aerobic, effect.aerobic, Modifier.weight(1f))
            EffectGauge(anaerobic, effect.anaerobic, Modifier.weight(1f))
            if (load != null) {
                Column(Modifier.weight(0.7f), horizontalAlignment = Alignment.End) {
                    Text(load.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        stringResource(R.string.activity_chart_effect_load),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                    )
                }
            }
        }
    }
}

@Composable
private fun EffectGauge(label: String, value: Double, modifier: Modifier) {
    val track = MaterialTheme.colorScheme.outlineVariant
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(Modifier.fillMaxWidth().height(Spacing.giant * 1.4f), contentAlignment = Alignment.BottomCenter) {
            Canvas(Modifier.fillMaxWidth().height(Spacing.giant * 1.4f)) {
                val stroke = size.height * 0.14f
                val diameter = minOf(size.width, size.height * 2f) - stroke
                val topLeft = Offset((size.width - diameter) / 2f, stroke / 2f)
                val arc = Size(diameter, diameter)
                // Five bands, one per point, then the pointer at the value.
                val band = 180f / EffectGaugeColors.size
                EffectGaugeColors.forEachIndexed { index, color ->
                    drawArc(color.copy(alpha = 0.35f), 180f + index * band + 1f, band - 2f, false, topLeft, arc, style = Stroke(stroke))
                }
                drawArc(effectColor(value), 180f, (180f * (value / 5.0)).toFloat().coerceAtLeast(1f), false, topLeft, arc,
                    style = Stroke(stroke, cap = StrokeCap.Round))
                if (value <= 0.0) drawArc(track, 180f, 1f, false, topLeft, arc, style = Stroke(stroke))
            }
            Text(
                String.format(androidx.compose.ui.platform.LocalConfiguration.current.locales[0], "%.1f", value),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(effectLevelLabel(trainingEffectLevel(value)), style = MaterialTheme.typography.labelMedium, color = effectColor(value))
    }
}
