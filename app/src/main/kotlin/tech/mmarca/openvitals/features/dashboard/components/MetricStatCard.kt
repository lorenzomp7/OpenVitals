package tech.mmarca.openvitals.features.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.features.dashboard.dashboardDisplayValue
import tech.mmarca.openvitals.ui.components.AutoResizeText
import tech.mmarca.openvitals.ui.theme.Spacing
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.fillMaxHeight
import tech.mmarca.openvitals.ui.theme.Radii

private val MetricStatIconCapsuleWidth = 38.dp
private val MetricStatIconCapsuleShape = RoundedCornerShape(18.dp)
private val MetricStatIconGlyphSize = 20.dp

/** The dashboard's tile corner: rounder than the default card. */
internal val DashboardTileShape = RoundedCornerShape(24.dp)

/** The tones one dashboard tile draws with, all from its metric's hue. */
internal data class DashboardTileColors(
    val container: Color,
    /** The lighter band that fills the tile up to its progress. */
    val fill: Color,
    val iconContainer: Color,
    val icon: Color,
    val ring: Color,
    val ringTrack: Color,
)

/**
 * A tile in its metric's own colour, saturated: a deep container, a
 * lighter progress band and a bright ring. Light themes get the pastel
 * counterpart.
 */
@Composable
internal fun dashboardTileColors(accentColor: Color): DashboardTileColors {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    return remember(accentColor, dark) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(accentColor.toArgb(), hsv)
        val hue = hsv[0]
        val saturation = hsv[1].coerceAtLeast(0.62f)
        if (dark) {
            DashboardTileColors(
                container = Color.hsv(hue, saturation, 0.40f),
                fill = Color.hsv(hue, saturation, 0.58f),
                iconContainer = Color.hsv(hue, saturation, 0.24f),
                icon = Color.hsv(hue, 0.28f, 1f),
                ring = Color.hsv(hue, 0.55f, 1f),
                ringTrack = Color.hsv(hue, saturation, 0.24f),
            )
        } else {
            DashboardTileColors(
                container = Color.hsv(hue, 0.16f, 1f),
                fill = Color.hsv(hue, 0.34f, 1f),
                iconContainer = Color.hsv(hue, 0.30f, 0.98f),
                icon = Color.hsv(hue, saturation, 0.50f),
                ring = Color.hsv(hue, saturation, 0.68f),
                ringTrack = Color.hsv(hue, 0.30f, 0.94f),
            )
        }
    }
}

@Composable
internal fun MetricStatCard(
    title: String,
    value: DisplayValue,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    message: String? = null,
    subtitle: String? = null,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    showTitle: Boolean = true,
    progressFraction: Float? = null,
    /** Optional action pinned to the tile's trailing edge (the watch's sync). */
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val tileColors = dashboardTileColors(accentColor)

    OpenVitalsCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        containerColor = tileColors.container,
        shape = DashboardTileShape,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (progressFraction != null) {
                // The progress fills the tile from its leading edge, behind the text.
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progressFraction.coerceIn(0f, 1f))
                        .background(tileColors.fill),
                )
            }
            Row(
                // fillMaxSize: a wrap-height Row in a fixed-height Box pins to the top.
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 6.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // The icon sits in a darker capsule the height of the tile.
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(MetricStatIconCapsuleWidth)
                        .background(color = tileColors.iconContainer, shape = MetricStatIconCapsuleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tileColors.icon,
                        modifier = Modifier.size(MetricStatIconGlyphSize),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                ) {
                    if (showTitle) {
                        AutoResizeText(
                            text = title,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = tileColors.icon,
                            maxLines = 1,
                        )
                    }
                    AutoResizeText(
                        text = message ?: dashboardDisplayValue(value),
                        // titleMedium, measured off the shipping app; the design screenshot is stale.
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (message == null) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                    )
                    if (message == null && subtitle != null) {
                        AutoResizeText(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = subtitleColor,
                            maxLines = 1,
                        )
                    }
                }
                if (trailing != null) {
                    Spacer(Modifier.width(Spacing.xs))
                    trailing()
                }
            }
        }
    }
}
