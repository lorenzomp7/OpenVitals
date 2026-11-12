package tech.mmarca.openvitals.features.dashboard

import androidx.compose.foundation.Image
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
import tech.mmarca.openvitals.ui.components.FluidBrandTimeline
import tech.mmarca.openvitals.ui.components.OpenVitalsIconButton

/** A normal list item: its position is owned only by the dashboard's scroll. */
@Composable
internal fun DashboardHeader(
    timeline: FluidBrandTimeline,
    photo: ImageBitmap?,
    hasDevices: Boolean,
    onOpenProfile: () -> Unit,
    onOpenDevices: () -> Unit,
    onOpenAchievements: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth().testTag("dashboard_header")) {
        val windowWidth = maxWidth
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f)) { AmbientFluidBrand(timeline, windowWidth) }
            if (hasDevices) OpenVitalsIconButton(onClick = onOpenDevices) {
                Icon(Icons.Outlined.BatteryChargingFull, stringResource(R.string.cd_sensor_battery_status))
            }
            OpenVitalsIconButton(onClick = onOpenProfile) {
                val description = stringResource(R.string.user_profile_title)
                if (photo != null) Image(photo, description,
                    Modifier.size(28.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                else Icon(Icons.Outlined.AccountCircle, description)
            }
            OpenVitalsIconButton(onClick = onOpenAchievements) {
                Icon(Icons.Outlined.WorkspacePremium, stringResource(R.string.cd_achievements))
            }
            OpenVitalsIconButton(onClick = onOpenSettings) {
                Icon(Icons.Outlined.Settings, stringResource(R.string.cd_settings))
            }
        }
    }
}

@Composable
internal fun DashboardPlaceholder(header: @Composable () -> Unit, content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Column {
        header()
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { content() }
    }
}
