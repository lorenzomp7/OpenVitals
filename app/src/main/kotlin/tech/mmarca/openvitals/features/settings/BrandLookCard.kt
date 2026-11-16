package tech.mmarca.openvitals.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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

/**
 * 97: which colours the home screen's toggle, header buttons and action pills use, and whether they move.
 * 105: built as the cards around it are: chips for the choice, as the chart grouping card, and an
 * icon, title and switch row for the motion, as the empty-tiles card.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BrandLookCard(modifier: Modifier = Modifier, viewModel: BrandLookViewModel = hiltViewModel()) {
    val palette by viewModel.palette.collectAsStateWithLifecycle()
    val animated by viewModel.animated.collectAsStateWithLifecycle()
    OpenVitalsCard(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Palette,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(Spacing.xl),
                )
                Column(Modifier.padding(horizontal = Spacing.md).weight(1f)) {
                    Text(stringResource(R.string.settings_brand_colors_title), style = MaterialTheme.typography.titleSmall)
                    Text(
                        stringResource(R.string.settings_brand_colors_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Spacing.xs),
                    )
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
            ) {
                BrandPaletteSource.entries.forEach { source ->
                    FilterChip(
                        selected = palette == source,
                        onClick = { viewModel.selectPalette(source) },
                        label = { Text(stringResource(source.labelRes())) },
                    )
                }
            }
            HorizontalDivider(Modifier.padding(vertical = Spacing.md))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(Spacing.xl),
                )
                Column(Modifier.padding(horizontal = Spacing.md).weight(1f)) {
                    Text(stringResource(R.string.settings_brand_animated_title), style = MaterialTheme.typography.titleSmall)
                    Text(
                        stringResource(R.string.settings_brand_animated_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Spacing.xs),
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
