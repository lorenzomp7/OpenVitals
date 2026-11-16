package tech.mmarca.openvitals.features.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.data.repository.BrandLookPreferences
import tech.mmarca.openvitals.domain.preferences.BrandPaletteSource
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.theme.Spacing

/** 97: the home screen's colours and motion, read by the settings card and the dashboard. */
@HiltViewModel
class BrandLookViewModel @Inject constructor(private val preferences: BrandLookPreferences) : ViewModel() {
    val palette = preferences.palette
    val animated = preferences.animated
    fun selectPalette(source: BrandPaletteSource) = preferences.setPalette(source)
    fun setAnimated(animated: Boolean) = preferences.setAnimated(animated)
}

/** 97: which colours the home screen's toggle, header buttons and action pills use, and whether they move. */
@Composable
internal fun BrandLookCard(modifier: Modifier = Modifier, viewModel: BrandLookViewModel = hiltViewModel()) {
    val palette by viewModel.palette.collectAsStateWithLifecycle()
    val animated by viewModel.animated.collectAsStateWithLifecycle()
    OpenVitalsCard(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.lg)) {
            Text(stringResource(R.string.settings_brand_colors_title), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(R.string.settings_brand_colors_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.xs),
            )
            Column(Modifier.padding(top = Spacing.sm).selectableGroup()) {
                BrandPaletteSource.entries.forEach { source ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = palette == source, role = Role.RadioButton) { viewModel.selectPalette(source) }
                            .padding(vertical = Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = palette == source, onClick = null)
                        Text(
                            stringResource(source.labelRes()),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = Spacing.md),
                        )
                    }
                }
            }
            HorizontalDivider(Modifier.padding(vertical = Spacing.sm))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_brand_animated_title), style = MaterialTheme.typography.titleSmall)
                    Text(
                        stringResource(R.string.settings_brand_animated_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = animated, onCheckedChange = viewModel::setAnimated)
            }
        }
    }
}

private fun BrandPaletteSource.labelRes(): Int = when (this) {
    BrandPaletteSource.GOOGLE -> R.string.settings_brand_colors_google
    BrandPaletteSource.PHONE -> R.string.settings_brand_colors_phone
    BrandPaletteSource.HERO -> R.string.settings_brand_colors_hero
    BrandPaletteSource.TILES -> R.string.settings_brand_colors_tiles
    BrandPaletteSource.HERO_AND_TILES -> R.string.settings_brand_colors_hero_tiles
}
