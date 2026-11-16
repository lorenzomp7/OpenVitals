package tech.mmarca.openvitals.features.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.ui.components.AmbientFluidBrand
import tech.mmarca.openvitals.ui.components.BrandIconFrame
import tech.mmarca.openvitals.ui.components.FluidBrandTimeline
import tech.mmarca.openvitals.ui.components.OpenVitalsIconButton
import tech.mmarca.openvitals.ui.theme.WorkoutActionMetrics

/**
 * A normal list item: its position is owned only by the dashboard's scroll. 106: profile and
 * achievements on the left, the OpenVitals toggle in the middle, the pencil and settings on the
 * right, every button dressed like the toggle.
 */
@Composable
internal fun DashboardHeader(
    timeline: FluidBrandTimeline,
    photo: ImageBitmap?,
    hasDevices: Boolean,
    onOpenProfile: () -> Unit,
    onOpenDevices: () -> Unit,
    onOpenAchievements: () -> Unit,
    onOpenSettings: () -> Unit,
    day: tech.mmarca.openvitals.ui.components.BrandDay? = null,
    onOpenDay: (() -> Unit)? = null,
    isEditingDashboard: Boolean = false,
    onToggleDashboardEdit: (() -> Unit)? = null,
) {
    BoxWithConstraints(Modifier.fillMaxWidth().testTag("dashboard_header")) {
        val windowWidth = maxWidth
        // Both sides as wide as the busier one, so the toggle sits in the middle of the screen.
        val sideButtons = if (hasDevices) 3 else 2
        val side = HeaderButtonSize * sideButtons
        val toggleWidth = minOf(
            WorkoutActionMetrics.primaryWidth(windowWidth),
            windowWidth - HeaderGutter * 2 - side * 2 - HeaderGap * 2,
        ).coerceAtLeast(HeaderButtonSize * 2)
        Row(
            Modifier.fillMaxWidth().padding(horizontal = HeaderGutter, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                if (hasDevices) OpenVitalsIconButton(onClick = onOpenDevices) {
                    BrandIconFrame(timeline) {
                        Icon(Icons.Outlined.BatteryChargingFull, stringResource(R.string.cd_sensor_battery_status))
                    }
                }
                // 78: profile, achievements and settings dressed like the animated toggle.
                OpenVitalsIconButton(onClick = onOpenProfile) {
                    val description = stringResource(R.string.user_profile_title)
                    BrandIconFrame(timeline, tintContent = photo == null) {
                        if (photo != null) Image(photo, description,
                            Modifier.size(28.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                        else Icon(Icons.Outlined.AccountCircle, description)
                    }
                }
                OpenVitalsIconButton(onClick = onOpenAchievements) {
                    BrandIconFrame(timeline) {
                        Icon(Icons.Outlined.WorkspacePremium, stringResource(R.string.cd_achievements))
                    }
                }
            }
            AmbientFluidBrand(
                timeline,
                windowWidth,
                day = day,
                onClick = onOpenDay,
                width = toggleWidth,
                modifier = Modifier.padding(horizontal = HeaderGap),
            )
            Row(
                Modifier.weight(1f),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onToggleDashboardEdit != null) OpenVitalsIconButton(onClick = onToggleDashboardEdit) {
                    BrandIconFrame(timeline) {
                        Icon(
                            imageVector = if (isEditingDashboard) Icons.Outlined.Check else Icons.Outlined.Edit,
                            contentDescription = stringResource(
                                if (isEditingDashboard) R.string.cd_finish_dashboard_editing else R.string.cd_edit_dashboard,
                            ),
                        )
                    }
                }
                OpenVitalsIconButton(onClick = onOpenSettings) {
                    BrandIconFrame(timeline) {
                        Icon(Icons.Outlined.Settings, stringResource(R.string.cd_settings))
                    }
                }
            }
        }
    }
}

private val HeaderGutter = 16.dp
private val HeaderGap = 4.dp

/** The size of [OpenVitalsIconButton]. */
private val HeaderButtonSize = 44.dp

@Composable
internal fun DashboardPlaceholder(header: @Composable () -> Unit, content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Column {
        header()
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { content() }
    }
}
