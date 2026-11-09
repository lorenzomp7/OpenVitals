package tech.mmarca.openvitals.features.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.graphics.compositeOver
import tech.mmarca.openvitals.ui.theme.Radii
import tech.mmarca.openvitals.ui.theme.isAmoledColorScheme

private val MetricStatIconSize = 28.dp
private val MetricStatIconGlyphSize = 16.dp
private val MetricStatProgressHeight = 3.dp

/** The dashboard's tile corner: rounder than the default card. */
internal val DashboardTileShape = RoundedCornerShape(Radii.lg)

/** A tile washed in its metric's colour, over black on AMOLED. */
@Composable
internal fun dashboardTileContainerColor(accentColor: Color): Color =
    accentColor.copy(alpha = if (isAmoledColorScheme()) 0.16f else 0.14f).compositeOver(
        if (isAmoledColorScheme()) Color.Black else MaterialTheme.colorScheme.surfaceContainer,
    )

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
    val iconContainerColor = accentColor.copy(alpha = 0.22f)
    val progressFillColor = accentColor.copy(alpha = 0.7f)

    OpenVitalsCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        containerColor = dashboardTileContainerColor(accentColor),
        shape = DashboardTileShape,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                // fillMaxSize: a wrap-height Row in a fixed-height Box pins to the top.
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(MetricStatIconSize)
                        .background(color = iconContainerColor, shape = CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
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
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            if (progressFraction != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        // Inset, so the tile's round corners do not cut the bar.
                        .padding(start = 20.dp, end = 20.dp, bottom = 6.dp)
                        .height(MetricStatProgressHeight)
                        .fillMaxWidth(progressFraction.coerceIn(0f, 1f))
                        .clip(CircleShape)
                        .background(progressFillColor),
                )
            }
        }
    }
}
