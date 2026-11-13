package tech.mmarca.openvitals.features.imports.screenshot

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import tech.mmarca.openvitals.ui.theme.Spacing

@Composable
internal fun ScreenshotTimePicker(value: String, label: String, enabled: Boolean, onValue: (String) -> Unit) {
    val context = LocalContext.current
    val date = runCatching { java.time.LocalDateTime.parse(value) }.getOrNull()
    OutlinedButton(enabled = enabled, shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.md),
        modifier = Modifier.fillMaxWidth(), onClick = {
            val seed = date ?: java.time.LocalDateTime.now()
            android.app.DatePickerDialog(context, { _, year, month, day ->
                android.app.TimePickerDialog(context, { _, hour, minute ->
                    onValue(java.time.LocalDateTime.of(year, month + 1, day, hour, minute, seed.second).toString())
                }, seed.hour, seed.minute, android.text.format.DateFormat.is24HourFormat(context)).show()
            }, seed.year, seed.monthValue - 1, seed.dayOfMonth).show()
        }) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(date?.format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm:ss")) ?: "—",
                style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
