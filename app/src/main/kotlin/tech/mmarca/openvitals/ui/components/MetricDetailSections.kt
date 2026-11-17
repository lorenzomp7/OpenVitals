package tech.mmarca.openvitals.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import tech.mmarca.openvitals.ui.theme.Motion
import tech.mmarca.openvitals.ui.theme.loopingMotionAllowed
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.preferences.MetricDetailSectionId
import tech.mmarca.openvitals.core.presentation.MetricDetailSectionContext
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.ReorderableLazyListState

private const val MetricSectionEditWiggleDegrees = 0.35f

val LocalMetricSectionEditMode = staticCompositionLocalOf { false }

@Composable
fun metricSectionEditModeActive(): Boolean = LocalMetricSectionEditMode.current

class MetricDetailSectionBuilder {
    internal val sections = linkedMapOf<MetricDetailSectionId, @Composable () -> Unit>()

    fun section(
        id: MetricDetailSectionId,
        visible: Boolean = true,
        content: @Composable () -> Unit,
    ) {
        if (visible) {
            sections[id] = content
        }
    }
}

class MetricDetailSectionListState(
    val lazyListState: LazyListState,
) {
    internal var isEditingSections by mutableStateOf(false)
    internal var onMoveSectionToTarget: ((MetricDetailSectionId, MetricDetailSectionId) -> Unit)? = null

    /** The sections the screen shows now; a move keeps the others in their slots. */
    var visibleSectionIds: Set<MetricDetailSectionId> = emptySet()
        internal set
    internal var reorderableState by mutableStateOf<ReorderableLazyListState?>(null)
}

@Composable
fun rememberMetricDetailSectionListState(): MetricDetailSectionListState {
    val lazyListState = rememberLazyListState()
    return remember(lazyListState) { MetricDetailSectionListState(lazyListState) }
}

fun LazyListScope.renderOrderedMetricDetailSections(
    sectionContext: MetricDetailSectionContext,
    builder: MetricDetailSectionBuilder.() -> Unit,
) {
    orderedMetricDetailSections(
        listState = sectionContext.listState,
        order = sectionContext.order,
        isEditingSections = sectionContext.isEditingSections,
        onMoveSectionToTarget = sectionContext.onMoveSectionToTarget,
        onMoveSection = sectionContext.onMoveSection,
        hidden = sectionContext.hidden,
        onHideSection = sectionContext.onHideSection,
        onShowSection = sectionContext.onShowSection,
        builder = builder,
    )
}

fun LazyListScope.orderedMetricDetailSections(
    listState: MetricDetailSectionListState,
    order: List<MetricDetailSectionId>,
    isEditingSections: Boolean,
    onMoveSectionToTarget: (MetricDetailSectionId, MetricDetailSectionId) -> Unit,
    onMoveSection: (MetricDetailSectionId, Int) -> Unit,
    hidden: Set<MetricDetailSectionId> = emptySet(),
    onHideSection: ((MetricDetailSectionId) -> Unit)? = null,
    onShowSection: ((MetricDetailSectionId) -> Unit)? = null,
    builder: MetricDetailSectionBuilder.() -> Unit,
) {
    val sectionBuilder = MetricDetailSectionBuilder().apply(builder)
    // 131: a hidden section is off the screen; edit mode lists it at the bottom to put back.
    val visibleOrder = order.filter { it in sectionBuilder.sections && it !in hidden }
    listState.visibleSectionIds = visibleOrder.toSet()

    // For a caller that builds the list without rememberMetricDetailSectionOrdering, which sets
    // the same two values where a scroll cannot skip them.
    item(key = "metric_section_drag_config") {
        SideEffect {
            listState.isEditingSections = isEditingSections
            listState.onMoveSectionToTarget = onMoveSectionToTarget
        }
    }

    visibleOrder.forEachIndexed { index, sectionId ->
        item(key = sectionId) {
            val reorderableState = listState.reorderableState
            if (reorderableState != null) {
                ReorderableItem(
                    state = reorderableState,
                    key = sectionId,
                    enabled = isEditingSections,
                ) { isDragging ->
                    ReorderableMetricDetailSection(
                        sectionId = sectionId,
                        isDragging = isDragging,
                        isEditingSections = isEditingSections,
                        dragHandleModifier = Modifier.longPressDraggableHandle(
                            enabled = isEditingSections,
                        ),
                        onHide = onHideSection?.let { hide -> { hide(sectionId) } },
                        onMovePrevious = if (index > 0) {
                            { onMoveSection(sectionId, -1) }
                        } else {
                            null
                        },
                        onMoveNext = if (index < visibleOrder.lastIndex) {
                            { onMoveSection(sectionId, 1) }
                        } else {
                            null
                        },
                    ) {
                        sectionBuilder.sections.getValue(sectionId).invoke()
                    }
                }
            } else {
                ReorderableMetricDetailSection(
                    sectionId = sectionId,
                    isDragging = false,
                    isEditingSections = isEditingSections,
                    dragHandleModifier = Modifier,
                    onHide = onHideSection?.let { hide -> { hide(sectionId) } },
                    onMovePrevious = if (index > 0) {
                        { onMoveSection(sectionId, -1) }
                    } else {
                        null
                    },
                    onMoveNext = if (index < visibleOrder.lastIndex) {
                        { onMoveSection(sectionId, 1) }
                    } else {
                        null
                    },
                ) {
                    sectionBuilder.sections.getValue(sectionId).invoke()
                }
            }
        }
    }

    // 131: the hidden sections this screen would show, dimmed under a header, each with a "+".
    val hiddenHere = order.filter { it in sectionBuilder.sections && it in hidden }
    if (isEditingSections && onShowSection != null && hiddenHere.isNotEmpty()) {
        item(key = "metric_sections_hidden_header") {
            androidx.compose.material3.Text(
                text = stringResource(R.string.metric_sections_hidden),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp).padding(top = 16.dp, bottom = 4.dp),
            )
        }
        hiddenHere.forEach { sectionId ->
            item(key = "hidden_$sectionId") {
                HiddenMetricDetailSection(onShow = { onShowSection(sectionId) }) {
                    sectionBuilder.sections.getValue(sectionId).invoke()
                }
            }
        }
    }
}

/** 131: a hidden section in edit mode: drawn faint, untouchable, with a "+" to put it back. */
@Composable
private fun HiddenMetricDetailSection(onShow: () -> Unit, content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth()) {
        CompositionLocalProvider(LocalMetricSectionEditMode provides true) {
            Column(modifier = Modifier.fillMaxWidth().graphicsLayer { alpha = HiddenSectionAlpha }) { content() }
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(Unit) { detectTapGestures(onTap = { onShow() }) },
        )
        SectionCornerButton(
            icon = androidx.compose.material.icons.Icons.Outlined.Add,
            contentDescription = stringResource(R.string.cd_show_section),
            onClick = onShow,
            modifier = Modifier.align(androidx.compose.ui.Alignment.TopEnd),
        )
    }
}

/** 131: the small round button at a section's corner: "x" to hide, "+" to show. */
@Composable
private fun SectionCornerButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.material3.FilledTonalIconButton(
        onClick = onClick,
        modifier = modifier.padding(top = 8.dp, end = 12.dp).size(36.dp),
    ) {
        androidx.compose.material3.Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(20.dp))
    }
}

private const val HiddenSectionAlpha = 0.4f

@Composable
private fun ReorderableMetricDetailSection(
    sectionId: MetricDetailSectionId,
    isDragging: Boolean,
    isEditingSections: Boolean,
    dragHandleModifier: Modifier,
    onMovePrevious: (() -> Unit)?,
    onMoveNext: (() -> Unit)?,
    onHide: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val wiggleRotation = if (isEditingSections && loopingMotionAllowed()) {
        val wiggleTransition = rememberInfiniteTransition(label = "MetricSectionWiggle")
        val rotation by wiggleTransition.animateFloat(
            initialValue = -MetricSectionEditWiggleDegrees,
            targetValue = MetricSectionEditWiggleDegrees,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = Motion.editWiggleMillis,
                    delayMillis = (sectionId.ordinal % 4) * 35,
                    easing = LinearEasing,
                ),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "MetricSectionWiggleRotation",
        )
        rotation
    } else {
        0f
    }
    val movePreviousLabel = stringResource(R.string.cd_move_section_up)
    val moveNextLabel = stringResource(R.string.cd_move_section_down)

    val editSemanticsModifier = if (isEditingSections) {
        Modifier.semantics {
            contentDescription = sectionId.name
            customActions = buildList {
                onMovePrevious?.let { action ->
                    add(
                        CustomAccessibilityAction(movePreviousLabel) {
                            action()
                            true
                        },
                    )
                }
                onMoveNext?.let { action ->
                    add(
                        CustomAccessibilityAction(moveNextLabel) {
                            action()
                            true
                        },
                    )
                }
            }
        }
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(if (isDragging) 1f else 0f)
            .graphicsLayer {
                rotationZ = if (isEditingSections && !isDragging) wiggleRotation else 0f
                scaleX = if (isDragging) 1.01f else 1f
                scaleY = if (isDragging) 1.01f else 1f
                shadowElevation = if (isDragging) 12.dp.toPx() else 0f
            }
            .then(editSemanticsModifier)
            .then(dragHandleModifier)
            .then(
                if (isEditingSections) {
                    Modifier
                        .padding(horizontal = 4.dp)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = MaterialTheme.shapes.medium,
                        )
                } else {
                    Modifier
                },
            ),
    ) {
        CompositionLocalProvider(LocalMetricSectionEditMode provides isEditingSections) {
            // Stacked, not layered: a section may hold a header and cards, and the Box drew them on top of each other.
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
            if (isEditingSections && !isDragging) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { /* block card taps while reordering */ })
                        },
                )
                // 131: the "x" that takes the section off, as the home screen's tiles have.
                if (onHide != null) SectionCornerButton(
                    icon = androidx.compose.material.icons.Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.cd_hide_section),
                    onClick = onHide,
                    modifier = Modifier.align(androidx.compose.ui.Alignment.TopEnd),
                )
            }
        }
    }
}
