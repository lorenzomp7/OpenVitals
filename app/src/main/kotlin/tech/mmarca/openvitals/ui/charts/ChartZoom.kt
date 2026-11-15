package tech.mmarca.openvitals.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.ui.theme.Emphasis
import tech.mmarca.openvitals.ui.theme.Spacing

/** What [ChartZoom] hands its content: the visible slice, and whether a pinch is live. */
@Immutable
data class ChartZoomState(
    val viewport: ChartViewport,
    /** True while a second finger is down. Charts no longer pinch, so this stays false. */
    val multiTouch: Boolean,
)

/**
 * Every chart's frame. 83: there is no pinch any more: two fingers on a chart moved the page
 * under them, and lifting them flung it (85). A screen reader still gets zoom in, zoom out
 * and reset. 84: a button opens the chart full screen, where it can be turned on its side.
 * [keys] reset the viewport when the chart's subject changes.
 */
@Composable
fun ChartZoom(
    vararg keys: Any?,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    initialViewport: ChartViewport = ChartViewport.Full,
    content: @Composable (ChartZoomState) -> Unit,
) {
    var viewport by remember(*keys, initialViewport) { mutableStateOf(initialViewport) }
    var fullScreen by rememberSaveable { mutableStateOf(false) }
    val state = ChartZoomState(if (enabled) viewport else ChartViewport.Full, multiTouch = false)

    val zoomInLabel = stringResource(R.string.chart_zoom_in)
    val zoomOutLabel = stringResource(R.string.chart_zoom_out)
    val resetLabel = stringResource(R.string.chart_zoom_reset)
    val zoomActions = if (!enabled) Modifier else Modifier.semantics {
        customActions = buildList {
            if (viewport.span > ChartViewport.MinimumSpan) {
                add(CustomAccessibilityAction(zoomInLabel) { viewport = viewport.zoomed(2f, 0.5f); true })
            }
            if (viewport.isZoomed) {
                add(CustomAccessibilityAction(zoomOutLabel) { viewport = viewport.zoomed(0.5f, 0.5f); true })
                add(CustomAccessibilityAction(resetLabel) { viewport = ChartViewport.Full; true })
            }
        }
    }

    Box(modifier = modifier.then(zoomActions)) {
        content(state)
        if (enabled) {
            ChartFrameButton(
                icon = { Icon(Icons.Outlined.Fullscreen, contentDescription = null) },
                label = stringResource(R.string.chart_full_screen),
                onClick = { fullScreen = true },
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }
    }
    if (fullScreen) {
        ChartFullScreen(onDismiss = { fullScreen = false }) { content(state) }
    }
}

/** The chart alone on the screen; [content] is laid out across the long side once turned. */
@Composable
private fun ChartFullScreen(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    var turned by rememberSaveable { mutableStateOf(false) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(Spacing.lg),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                ChartFrameButton(
                    icon = { Icon(Icons.Outlined.Close, contentDescription = null) },
                    label = stringResource(R.string.action_close),
                    onClick = onDismiss,
                )
                Spacer(Modifier.weight(1f))
                ChartFrameButton(
                    icon = { Icon(Icons.Outlined.ScreenRotation, contentDescription = null) },
                    label = stringResource(R.string.chart_rotate),
                    onClick = { turned = !turned },
                )
            }
            TurnableBox(turned = turned, modifier = Modifier.weight(1f).fillMaxWidth().padding(top = Spacing.lg)) {
                content()
            }
        }
    }
}

/**
 * Lays [content] out across the box, or, [turned], across its height and then a quarter turn
 * round, so a long chart reads along the long side of the phone. Touch follows the turn.
 */
@Composable
private fun TurnableBox(turned: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(
        modifier = modifier,
        content = { Box(Modifier.graphicsLayer { rotationZ = if (turned) 90f else 0f }) { content() } },
    ) { measurables, constraints ->
        val across = if (turned) constraints.maxHeight else constraints.maxWidth
        val along = if (turned) constraints.maxWidth else constraints.maxHeight
        val placeable = measurables.first().measure(Constraints(minWidth = across, maxWidth = across, maxHeight = along))
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeable.place(
                x = (constraints.maxWidth - placeable.width) / 2,
                y = (constraints.maxHeight - placeable.height) / 2,
            )
        }
    }
}

@Composable
private fun ChartFrameButton(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(Spacing.xxxl)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = Emphasis.strong))
            .clickable(onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(Spacing.xl)) { icon() }
    }
}
