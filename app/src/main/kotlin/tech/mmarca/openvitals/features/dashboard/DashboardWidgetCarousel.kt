package tech.mmarca.openvitals.features.dashboard

import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import tech.mmarca.openvitals.R
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import tech.mmarca.openvitals.features.dashboard.components.DashboardMultiRingHero

/** The shipped layout's section rhythm, top to bottom. */
private val DashboardHeroSectionTopPadding = 16.dp
private val DashboardHeroSectionBottomPadding = 0.dp
private val DashboardGridTopPadding = 32.dp
private val DashboardHeroRingHeight = 200.dp
private val DashboardHeroTopPadding = 6.dp
private const val DashboardHeroRingCount = 3

@Composable
internal fun DashboardWidgetCarousel(
    visibleIds: List<DashboardWidgetId>,
    specsById: Map<DashboardWidgetId, DashboardWidgetSpec>,
    isEditingDashboard: Boolean,
    onMoveWidgetToTarget: (DashboardWidgetId, DashboardWidgetId) -> Unit,
    onRemoveWidget: (DashboardWidgetId) -> Unit,
    widgetSizes: Map<DashboardWidgetId, Int> = emptyMap(),
    pagedLayout: Boolean = false,
    onSetWidgetDetailCount: (DashboardWidgetId, Int) -> Unit = { _, _ -> },
    onSetWidgetSize: (DashboardWidgetId, Int) -> Unit = { _, _ -> },
    onSetPagedLayout: (Boolean) -> Unit = {},
    actionContent: @Composable () -> Unit,
    hiddenContent: @Composable () -> Unit,
) {
    // Outside edit mode the first three widgets share the hero: the first is
    // the centre ring, the next two the arcs beside it. Edit mode keeps them
    // in the grid so they can be dragged, which is how the user picks them.
    val topIds = visibleIds.take(DashboardHeroRingCount)
    val heroIds = if (!isEditingDashboard && topIds.all { specsById[it]?.hero != null }) topIds else emptyList()
    val fixedIds = if (heroIds.isEmpty()) topIds else emptyList()
    val sizeOf: (DashboardWidgetId) -> DashboardTileSize? = { id ->
        specsById[id]?.let { dashboardTileSize(it, widgetSizes) }
    }
    var configuringId by remember { mutableStateOf<DashboardWidgetId?>(null) }
    val cycleSize: (DashboardWidgetId) -> Unit = { configuringId = it }
    configuringId?.let { id ->
        specsById[id]?.let { spec ->
            DashboardTileOptionsDialog(
                title = spec.title,
                size = sizeOf(id) ?: DashboardTileSize.MEDIUM,
                count = spec.detailCount,
                onSize = { onSetWidgetSize(id, it.spaces) },
                onCount = { onSetWidgetDetailCount(id, it) },
                onDismiss = { configuringId = null },
            )
        }
    }
    val fixedIdSet = fixedIds.toSet()
    // Every other widget, in one grid that scrolls with the page.
    val carouselIds = visibleIds.filterNot { it in fixedIdSet || it in heroIds }
    val carouselRows = dashboardWidgetGridRows(carouselIds)
    val widgetBounds = remember { mutableMapOf<DashboardWidgetId, Rect>() }
    var contentBounds by remember { mutableStateOf<Rect?>(null) }
    var fixedSectionBounds by remember { mutableStateOf<Rect?>(null) }
    var carouselSectionBounds by remember { mutableStateOf<Rect?>(null) }
    var draggingWidgetId by remember { mutableStateOf<DashboardWidgetId?>(null) }
    var draggingWidgetStartBounds by remember { mutableStateOf<Rect?>(null) }
    var draggingWidgetDragOffset by remember { mutableStateOf(Offset.Zero) }
    var draggingWidgetBounds by remember { mutableStateOf<Rect?>(null) }

    LaunchedEffect(visibleIds) {
        val visibleSet = visibleIds.toSet()
        widgetBounds.keys.toList().forEach { widgetId ->
            if (widgetId !in visibleSet) {
                widgetBounds.remove(widgetId)
            }
        }
        if (draggingWidgetId !in visibleSet) {
            draggingWidgetId = null
            draggingWidgetStartBounds = null
            draggingWidgetDragOffset = Offset.Zero
            draggingWidgetBounds = null
        }
    }

    LaunchedEffect(isEditingDashboard) {
        if (!isEditingDashboard) {
            draggingWidgetId = null
            draggingWidgetStartBounds = null
            draggingWidgetDragOffset = Offset.Zero
            draggingWidgetBounds = null
        }
    }

    fun projectedDraggingBounds(widgetId: DashboardWidgetId): Rect? {
        val startBounds = draggingWidgetStartBounds ?: widgetBounds[widgetId] ?: return null
        return startBounds.translateBy(draggingWidgetDragOffset)
    }

    fun beginDraggingWidget(widgetId: DashboardWidgetId) {
        if (draggingWidgetId != widgetId) {
            draggingWidgetStartBounds = widgetBounds[widgetId]
            draggingWidgetDragOffset = Offset.Zero
        } else if (draggingWidgetStartBounds == null) {
            draggingWidgetStartBounds = widgetBounds[widgetId]
        }
        draggingWidgetId = widgetId
        draggingWidgetBounds = projectedDraggingBounds(widgetId)
    }

    fun dragWidgetBy(widgetId: DashboardWidgetId, dragAmount: Offset) {
        if (draggingWidgetId != widgetId) return
        draggingWidgetDragOffset += dragAmount
        draggingWidgetBounds = projectedDraggingBounds(widgetId)
    }

    fun clearDraggingWidget() {
        draggingWidgetId = null
        draggingWidgetStartBounds = null
        draggingWidgetDragOffset = Offset.Zero
        draggingWidgetBounds = null
    }

    fun dropTargetIdsFor(draggedId: DashboardWidgetId, draggedBounds: Rect): List<DashboardWidgetId> {
        val currentPageIds = carouselIds.filter { widgetBounds[it]?.overlaps(draggedBounds) == true }.ifEmpty { carouselIds }
        val dropCenter = draggedBounds.center
        val isOverFixedSection = fixedSectionBounds?.containsPoint(dropCenter) == true
        val isOverCarouselSection = carouselSectionBounds?.containsPoint(dropCenter) == true

        return when {
            draggedId in fixedIds && isOverCarouselSection -> currentPageIds
            draggedId in fixedIds -> fixedIds
            isOverFixedSection -> fixedIds
            else -> currentPageIds
        }
    }

    fun dropWidget(widgetId: DashboardWidgetId) {
        val draggedBounds = projectedDraggingBounds(widgetId) ?: draggingWidgetBounds ?: widgetBounds[widgetId] ?: return
        val targetIds = dropTargetIdsFor(widgetId, draggedBounds)

        closestDashboardWidgetId(
            draggedId = widgetId,
            dropCenter = draggedBounds.center,
            targetIds = targetIds,
            widgetBounds = widgetBounds,
        )?.let { targetId ->
            onMoveWidgetToTarget(widgetId, targetId)
        }
    }

    val onGridDraggingWidgetChanged: (DashboardWidgetId?) -> Unit = { widgetId ->
        if (widgetId == null) {
            clearDraggingWidget()
        } else {
            beginDraggingWidget(widgetId)
        }
    }

    val dropTargetWidgetId = draggingWidgetId?.let { id ->
        draggingWidgetBounds?.let { bounds ->
            closestDashboardWidgetId(id, bounds.center, dropTargetIdsFor(id, bounds), widgetBounds)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates -> contentBounds = coordinates.boundsInRoot() },
    ) {
        Column {
            if (heroIds.isNotEmpty()) {
                val rings = heroIds.mapNotNull { specsById[it]?.hero }
                DashboardMultiRingHero(
                    center = rings[0],
                    left = rings.getOrNull(1),
                    right = rings.getOrNull(2),
                    modifier = Modifier
                        .padding(
                            start = DashboardScreenPadding,
                            end = DashboardScreenPadding,
                            top = DashboardHeroTopPadding,
                        )
                        .fillMaxWidth()
                        .height(DashboardHeroRingHeight),
                )
            }
            DashboardWidgetGrid(
                ids = fixedIds,
                rows = DashboardFixedWidgetRows,
                specsById = specsById,
                isEditingDashboard = isEditingDashboard,
                widgetBounds = widgetBounds,
                dragOverlayWidgetId = draggingWidgetId,
                dropTargetWidgetId = dropTargetWidgetId,
                onDraggingWidgetChanged = onGridDraggingWidgetChanged,
                onDraggingWidgetBoundsChanged = { widgetId, bounds ->
                    if (draggingWidgetId == widgetId && draggingWidgetStartBounds == null) {
                        draggingWidgetStartBounds = bounds
                        draggingWidgetBounds = bounds
                    }
                },
                onDragWidgetBy = ::dragWidgetBy,
                onDropWidget = ::dropWidget,
                onMoveWidgetToTarget = onMoveWidgetToTarget,
                onRemoveWidget = onRemoveWidget,
                sizeOf = sizeOf,
                onCycleWidgetSize = cycleSize,
                modifier = Modifier
                    .onGloballyPositioned { coordinates -> fixedSectionBounds = coordinates.boundsInRoot() }
                    .zIndex(if (draggingWidgetId in fixedIds) 2f else 0f),
                // The shipped layout: rings 16dp under the date row, drawn square.
                topPadding = DashboardHeroSectionTopPadding,
                bottomPadding = DashboardHeroSectionBottomPadding,
                squareRowSpan = true,
            )

            actionContent()

            if (isEditingDashboard) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = DashboardScreenPadding,
                            end = DashboardScreenPadding,
                            top = DashboardGridTopPadding,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.dashboard_paged_layout_toggle),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(checked = pagedLayout, onCheckedChange = onSetPagedLayout)
                }
            }
            if (carouselIds.isNotEmpty() && !isEditingDashboard) {
                DashboardPackedTiles(
                    ids = carouselIds,
                    specsById = specsById,
                    sizes = widgetSizes,
                    paged = pagedLayout,
                    rowHeight = dashboardWidgetRowHeight() + 12.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = DashboardScreenPadding,
                            end = DashboardScreenPadding,
                            top = DashboardGridTopPadding,
                            bottom = 0.dp,
                        ),
                )
            } else if (carouselIds.isNotEmpty()) {
                DashboardWidgetGrid(
                    ids = carouselIds,
                    rows = carouselRows,
                    specsById = specsById,
                    isEditingDashboard = isEditingDashboard,
                    widgetBounds = widgetBounds,
                    dragOverlayWidgetId = draggingWidgetId,
                dropTargetWidgetId = dropTargetWidgetId,
                    onDraggingWidgetChanged = onGridDraggingWidgetChanged,
                    onDraggingWidgetBoundsChanged = { widgetId, bounds ->
                        if (draggingWidgetId == widgetId && draggingWidgetStartBounds == null) {
                            draggingWidgetStartBounds = bounds
                            draggingWidgetBounds = bounds
                        }
                    },
                    onDragWidgetBy = ::dragWidgetBy,
                    onDropWidget = ::dropWidget,
                    onMoveWidgetToTarget = onMoveWidgetToTarget,
                    onRemoveWidget = onRemoveWidget,
                    sizeOf = sizeOf,
                    onCycleWidgetSize = cycleSize,
                    modifier = Modifier
                        .onGloballyPositioned { coordinates -> carouselSectionBounds = coordinates.boundsInRoot() }
                        .zIndex(if (draggingWidgetId in carouselIds) 2f else 0f),
                    topPadding = DashboardGridTopPadding,
                    bottomPadding = DashboardSectionSeparatorSpacing,
                )
            }

            hiddenContent()
        }

        DashboardDraggedWidgetOverlay(
            widgetId = draggingWidgetId,
            draggedBounds = draggingWidgetBounds,
            contentBounds = contentBounds,
            specsById = specsById,
            widgetSizes = widgetSizes,
        )
    }
}

@Composable
private fun DashboardDraggedWidgetOverlay(
    widgetId: DashboardWidgetId?,
    draggedBounds: Rect?,
    contentBounds: Rect?,
    specsById: Map<DashboardWidgetId, DashboardWidgetSpec>,
    widgetSizes: Map<DashboardWidgetId, Int>,
) {
    val widgetId = widgetId ?: return
    val spec = specsById[widgetId] ?: return
    val bounds = draggedBounds ?: return
    val containerBounds = contentBounds ?: return
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .wrapContentSize(Alignment.TopStart, unbounded = true)
            .requiredSize(with(density) { bounds.width.toDp() }, with(density) { bounds.height.toDp() })
            .zIndex(10f)
            .graphicsLayer {
                translationX = bounds.left - containerBounds.left
                translationY = bounds.top - containerBounds.top
            },
    ) {
        DashboardSizedTile(spec, dashboardTileSize(spec, widgetSizes))
    }
}

private fun closestDashboardWidgetId(
    draggedId: DashboardWidgetId,
    dropCenter: Offset,
    targetIds: List<DashboardWidgetId>,
    widgetBounds: Map<DashboardWidgetId, Rect>,
): DashboardWidgetId? =
    targetIds
        .filter { widgetId -> widgetId != draggedId && widgetId in widgetBounds }
        .minByOrNull { widgetId ->
            val center = widgetBounds.getValue(widgetId).center
            val delta = dropCenter - center
            delta.x * delta.x + delta.y * delta.y
        }

private fun Rect.containsPoint(point: Offset): Boolean =
    point.x >= left && point.x <= right && point.y >= top && point.y <= bottom

private fun Rect.translateBy(offset: Offset): Rect =
    Rect(
        left = left + offset.x,
        top = top + offset.y,
        right = right + offset.x,
        bottom = bottom + offset.y,
    )
