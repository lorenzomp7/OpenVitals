package tech.mmarca.openvitals.features.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import kotlin.math.roundToInt
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.insights.JumpRopeSummary
import tech.mmarca.openvitals.domain.insights.PersonalRecord
import tech.mmarca.openvitals.domain.insights.PersonalRecordKind
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.theme.Spacing
import tech.mmarca.openvitals.ui.theme.WorkoutColor

private val RecordGold = Color(0xFFFFB300)

/** The jumps of a jump rope session, how many a minute, and in how many sets. */
@Composable
internal fun ActivityJumpRopeCard(summary: JumpRopeSummary, unitFormatter: UnitFormatter, modifier: Modifier = Modifier) {
    OpenVitalsCard(modifier = modifier) {
        Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Text(stringResource(R.string.activity_jumps_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            ActivityChartFigures(
                figures = listOf(
                    stringResource(R.string.activity_jumps_total) to unitFormatter.count(summary.jumps),
                    stringResource(R.string.activity_jumps_rate) to "${summary.jumpsPerMinute.roundToInt()}/min",
                    stringResource(R.string.activity_chart_sets_count) to unitFormatter.count(summary.sets),
                ),
                accent = WorkoutColor,
            )
        }
    }
}

/** The records this activity holds among those of its kind in the year up to it. */
@Composable
internal fun ActivityPersonalRecordsCard(
    records: List<PersonalRecord>,
    comparedWith: Int,
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
) {
    if (records.isEmpty()) return
    OpenVitalsCard(modifier = modifier) {
        Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Icon(Icons.Outlined.EmojiEvents, contentDescription = null, tint = RecordGold, modifier = Modifier.size(Spacing.xl))
                Text(stringResource(R.string.activity_records_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            records.forEach { record ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(recordLabel(record.kind), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text(
                        recordValue(record, unitFormatter),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = RecordGold,
                    )
                }
            }
            Text(
                stringResource(R.string.activity_records_note, comparedWith),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun recordLabel(kind: PersonalRecordKind): String = stringResource(
    when (kind) {
        PersonalRecordKind.DISTANCE -> R.string.activity_record_distance
        PersonalRecordKind.DURATION -> R.string.activity_record_duration
        PersonalRecordKind.PACE -> R.string.activity_record_pace
        PersonalRecordKind.SPEED -> R.string.activity_record_speed
        PersonalRecordKind.JUMPS -> R.string.activity_record_jumps
    },
)

private fun recordValue(record: PersonalRecord, unitFormatter: UnitFormatter): String = when (record.kind) {
    PersonalRecordKind.DISTANCE -> unitFormatter.distance(record.value).text
    PersonalRecordKind.DURATION -> unitFormatter.duration(record.value.toLong())
    PersonalRecordKind.PACE -> unitFormatter.averagePace(1_000.0, (record.value * 1_000).toLong())?.text ?: "–"
    PersonalRecordKind.SPEED -> unitFormatter.speed(record.value).text
    PersonalRecordKind.JUMPS -> unitFormatter.count(record.value.roundToInt())
}
