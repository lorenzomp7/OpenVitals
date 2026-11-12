package tech.mmarca.openvitals.features.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.ui.components.HealthDatePickerDialog

@Composable
internal fun BodyProfileBirthDateField(birthDate: LocalDate?, birthYear: Int?, onValue: (LocalDate?) -> Unit) {
    var choosing by remember { mutableStateOf(false) }
    val label = stringResource(R.string.medical_field_birth_date)
    Box(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        OutlinedTextField(
            value = birthDate?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)).orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            shape = MaterialTheme.shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Box(Modifier.matchParentSize().clickable(onClickLabel = label) { choosing = true })
    }
    if (choosing) HealthDatePickerDialog(
        selectedDate = birthDate ?: LocalDate.of(birthYear ?: (LocalDate.now().year - 30), 1, 1),
        onDismiss = { choosing = false },
        onConfirm = { onValue(it); choosing = false },
    )
}
