package tech.mmarca.openvitals.features.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import tech.mmarca.openvitals.ui.theme.Spacing

/**
 * 114: the figures over an activity chart, the same on every chart: the first at the left
 * edge, the last at the right edge, any between them in the middle, each a value over its label.
 */
@Composable
internal fun ActivityChartFigures(
    figures: List<Pair<String, String>>,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    if (figures.isEmpty()) return
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        figures.forEachIndexed { index, (label, value) ->
            val (alignment, textAlign) = when {
                index == 0 -> Alignment.Start to TextAlign.Start
                index == figures.lastIndex -> Alignment.End to TextAlign.End
                else -> Alignment.CenterHorizontally to TextAlign.Center
            }
            Column(Modifier.weight(1f), horizontalAlignment = alignment) {
                Text(value, style = MaterialTheme.typography.titleMedium, color = accent, textAlign = textAlign)
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = textAlign,
                )
            }
        }
    }
}

/**
 * 114: a range with its unit said once, at the end: "77 – 156 bpm", not "77 bpm-156 bpm", which
 * broke the column onto two lines. Two values in different units keep both.
 */
internal fun activityValueRange(low: String, high: String): String {
    val unit = high.substringAfter(' ', missingDelimiterValue = "").takeIf { it.isNotBlank() }
    val bareLow = if (unit != null && low.endsWith(" $unit")) low.removeSuffix(" $unit") else low
    return "$bareLow – $high"
}
