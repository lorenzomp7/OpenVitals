package tech.mmarca.openvitals.features.activity.maps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.ui.theme.Spacing
import tech.mmarca.openvitals.domain.model.ExerciseRoutePoint

@Composable
internal fun ActivityMapboxMap(points: List<ExerciseRoutePoint>, modifier: Modifier = Modifier) {
    if (points.none { it.latitude.isFinite() && it.longitude.isFinite() &&
            it.latitude in -90.0..90.0 && it.longitude in -180.0..180.0 }) return
    var fullscreen by rememberSaveable { mutableStateOf(false) }
    Box(modifier) {
        // Only one native map is alive; closing the dialog restores the card.
        if (!fullscreen) ActivityRouteMapContent(points, Modifier.fillMaxSize())
        FilledIconButton(
            onClick = { fullscreen = true },
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).testTag("map-fullscreen-open"),
        ) {
            Icon(Icons.Outlined.Fullscreen, stringResource(R.string.activity_map_fullscreen))
        }
    }
    if (fullscreen) {
        Dialog(
            onDismissRequest = { fullscreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
        ) {
            Surface(Modifier.fillMaxSize().testTag("map-fullscreen")) {
                Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                    var recenter by rememberSaveable { mutableStateOf(0) }
                    ActivityRouteMapContent(points, Modifier.fillMaxSize(), recenterRequest = recenter)
                    // 71: the buttons sit at the bottom right, clear of Mapbox's compass (top right),
                    // scale bar (top left) and logo (bottom left). 72: the upper one brings the route back.
                    Column(
                        modifier = Modifier.align(Alignment.BottomEnd).padding(Spacing.md),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        FilledTonalIconButton(
                            onClick = { recenter += 1 },
                            modifier = Modifier.testTag("map-fullscreen-recenter"),
                        ) {
                            Icon(Icons.Outlined.CenterFocusStrong, stringResource(R.string.activity_map_recenter))
                        }
                        FilledIconButton(
                            onClick = { fullscreen = false },
                            modifier = Modifier.testTag("map-fullscreen-close"),
                        ) {
                            Icon(Icons.Outlined.Close, stringResource(R.string.action_close))
                        }
                    }
                }
            }
        }
    }
}
