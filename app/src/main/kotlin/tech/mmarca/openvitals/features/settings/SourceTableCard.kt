package tech.mmarca.openvitals.features.settings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.HealthConnectSourceResolver
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.OpenVitalsOutlinedButton

private val TypeColumnWidth = 132.dp
private val SourceColumnWidth = 84.dp

/**
 * Diagnostics: the "what does each source write" table. Rows are data types, columns are
 * the source apps; a cell is the date of the latest record, "declared" when the app declares
 * writing the type but none arrived, or a dash. Read only when the button is tapped.
 */
@Composable
internal fun SourceTableCard(
    table: SourceTable?,
    loading: Boolean,
    onLoad: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OpenVitalsCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Outlined.TableChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 2.dp).size(20.dp),
                )
                Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_hc_source_table_title),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = stringResource(R.string.settings_hc_source_table_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            OpenVitalsOutlinedButton(
                onClick = onLoad,
                enabled = !loading,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            ) {
                Text(
                    stringResource(
                        when {
                            loading -> R.string.settings_hc_source_table_loading
                            table == null -> R.string.settings_hc_source_table_load
                            else -> R.string.settings_hc_source_table_reload
                        },
                    ),
                )
            }
            if (table != null) SourceTableContent(table)
        }
    }
}

@Composable
private fun SourceTableContent(table: SourceTable) {
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant
    if (table.rows.isEmpty()) {
        Text(
            text = stringResource(R.string.settings_hc_source_table_empty),
            style = MaterialTheme.typography.bodySmall,
            color = secondary,
            modifier = Modifier.padding(top = 12.dp),
        )
    } else {
        SourceGrid(table)
        Text(
            text = stringResource(R.string.settings_hc_source_table_legend),
            style = MaterialTheme.typography.bodySmall,
            color = secondary,
            modifier = Modifier.padding(top = 8.dp),
        )
        if (table.rows.any { it.truncated }) {
            Text(
                text = stringResource(R.string.settings_hc_source_table_truncated),
                style = MaterialTheme.typography.bodySmall,
                color = secondary,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
    if (table.unreadableTypes.isNotEmpty()) {
        Text(
            text = stringResource(
                R.string.settings_hc_source_table_unreadable,
                table.unreadableTypes.joinToString(", "),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = secondary,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
    if (table.showsZeppNote) {
        Text(
            text = stringResource(R.string.settings_hc_source_table_zepp_note),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun SourceGrid(table: SourceTable) {
    val context = LocalContext.current.applicationContext
    val labels = remember(table.sources) {
        val resolver = HealthConnectSourceResolver(context)
        table.sources.map { resolver.resolve(it).label }
    }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd/MM").withZone(ZoneId.systemDefault()) }
    val declared = stringResource(R.string.settings_hc_source_table_declared)
    Column(modifier = Modifier.padding(top = 12.dp).horizontalScroll(rememberScrollState())) {
        Row {
            GridCell(stringResource(R.string.settings_hc_source_table_type), TypeColumnWidth, header = true)
            labels.forEach { GridCell(it, SourceColumnWidth, header = true) }
        }
        table.rows.forEach { row ->
            Row {
                GridCell(if (row.truncated) "${row.label} *" else row.label, TypeColumnWidth)
                row.cells.forEach { cell ->
                    when (cell) {
                        is SourceTableCell.Seen -> GridCell(dateFormatter.format(cell.lastSeen), SourceColumnWidth)
                        SourceTableCell.DeclaredNotSeen -> GridCell(declared, SourceColumnWidth, alert = true)
                        SourceTableCell.None -> GridCell("—", SourceColumnWidth, muted = true)
                    }
                }
            }
        }
    }
}

@Composable
private fun GridCell(
    text: String,
    width: androidx.compose.ui.unit.Dp,
    header: Boolean = false,
    alert: Boolean = false,
    muted: Boolean = false,
) {
    Text(
        text = text,
        style = if (header) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodySmall,
        color = when {
            alert -> MaterialTheme.colorScheme.error
            muted -> MaterialTheme.colorScheme.onSurfaceVariant
            else -> MaterialTheme.colorScheme.onSurface
        },
        maxLines = if (header) 2 else 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.width(width).padding(vertical = 4.dp, horizontal = 2.dp),
    )
}
