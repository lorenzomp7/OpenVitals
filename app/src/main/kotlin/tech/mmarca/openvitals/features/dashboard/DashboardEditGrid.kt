package tech.mmarca.openvitals.features.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.zIndex
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.features.dashboard.components.dashboardTileColors
import tech.mmarca.openvitals.ui.theme.LayoutMetrics
import tech.mmarca.openvitals.ui.theme.Spacing

/** Where a ring tile lands in the hero. */
internal enum class HeroRole { LEFT, CENTRE, RIGHT }

/** How many tiles become the rings at the top outside edit mode. */
private const val DashboardFixedRingCount = 3

/**
 * The hero draws the first tile in the centre, the second on the left and the third on the
 * right. Edit mode lists them left, centre, right: the first two swap places in the column.
 * The swap is its own inverse, so it turns the list either way.
 */
internal fun <T> heroDisplayOrder(order: List<T>): List<T> =
    if (order.size >= 2) listOf(order[1], order[0]) + order.drop(2) else order

/** One cell of the edit sheet, on the dashboard's four columns; [row] counts label rows too. */
internal sealed interface DashboardEditCell {
    val key: Any
    val column: Int
    val row: Int
    val columns: Int
    val rows: Int

    data class Label(override val key: String, override val row: Int, val textRes: Int, val page: Int? = null) : DashboardEditCell {
        override val column: Int get() = 0
        override val columns: Int get() = DashboardTileColumns
        override val rows: Int get() = 1
    }

    /** 82: the small hero beside a ring tile, with the ring that tile becomes picked out. */
    data class RingDiagram(override val key: String, override val row: Int, val role: HeroRole, val ringOf: DashboardWidgetId) :
        DashboardEditCell {
        override val column: Int get() = 2
        override val columns: Int get() = 2
        override val rows: Int get() = 1
    }

    data class Tile(
        val id: DashboardWidgetId,
        val size: DashboardTileSize,
        override val column: Int,
        override val row: Int,
        val ring: Boolean,
    ) : DashboardEditCell {
        override val key: Any get() = id
        override val columns: Int get() = size.columns
        override val rows: Int get() = size.rows
    }
}

internal class DashboardEditPlan(val cells: List<DashboardEditCell>, val labelRows: Set<Int>, val rowCount: Int)

/**
 * 79: the edit sheet places every tile as the dashboard does (a 2×2 beside two stacked
 * 2×1s), from the same packing. 82: the three ring tiles come first, in a column on the
 * left, left, centre, right, each beside a small hero showing the ring it becomes.
 */
internal fun dashboardEditPlan(
    displayIds: List<DashboardWidgetId>,
    sizes: List<DashboardTileSize>,
    pagedLayout: Boolean,
): DashboardEditPlan {
    val cells = mutableListOf<DashboardEditCell>()
    val labelRows = mutableSetOf<Int>()
    var row = 0
    fun label(key: String, textRes: Int, page: Int? = null) {
        cells += DashboardEditCell.Label(key, row, textRes, page)
        labelRows += row
        row += 1
    }
    val tiles = displayIds.zip(sizes)
    val rings = tiles.take(DashboardFixedRingCount)
    if (rings.isNotEmpty()) {
        label("label-rings", R.string.dashboard_edit_rings_label)
        val roles = if (rings.size == 1) listOf(HeroRole.CENTRE) else HeroRole.entries
        rings.forEachIndexed { index, (id, _) ->
            cells += DashboardEditCell.Tile(id, DashboardTileSize.MEDIUM, column = 0, row = row, ring = true)
            cells += DashboardEditCell.RingDiagram("ring-diagram-$index", row, roles[index], id)
            row += 1
        }
    }
    val rest = tiles.drop(DashboardFixedRingCount)
    if (rest.isNotEmpty()) {
        val pages = packDashboardTiles(rest, rowsPerPage = DashboardPagedRows.takeIf { pagedLayout })
            .groupBy { it.page }
            .toSortedMap()
        pages.forEach { (page, placements) ->
            if (pagedLayout) {
                label("label-page-$page", R.string.dashboard_edit_page_label, page + 1)
            } else {
                label("label-tiles", R.string.dashboard_edit_tiles_label)
            }
            val base = row
            placements.forEach { cells += DashboardEditCell.Tile(it.id, it.size, it.column, base + it.row, ring = false) }
            row = base + placements.maxOf { it.row + it.size.rows }
        }
    }
    return DashboardEditPlan(cells, labelRows, row)
}

private val DashboardEditLabelHeight = Spacing.xl + Spacing.md

/** 121: how near the list's edge a held tile starts the scroll, and its fastest step per frame. */
private val AutoScrollEdge = 72.dp
private val AutoScrollMaxStep = 14.dp

/**
 * 75: the dashboard's edit sheet. Hold a tile and it follows the finger; the others make
 * room as it passes over them, sliding to their new places.
 */
@Composable
internal fun DashboardReorderableEditGrid(
    visibleIds: List<DashboardWidgetId>,
    specsById: Map<DashboardWidgetId, DashboardWidgetSpec>,
    sizeOf: (DashboardWidgetId) -> DashboardTileSize?,
    pagedLayout: Boolean,
    onReorder: (List<DashboardWidgetId>) -> Unit,
    onRemoveWidget: (DashboardWidgetId) -> Unit,
    onCycleWidgetSize: (DashboardWidgetId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val displayIds = remember(visibleIds, specsById.keys) { heroDisplayOrder(visibleIds.filter { it in specsById }) }
    val sizes = displayIds.map { id -> sizeOf(id) ?: DashboardTileSize.MEDIUM }
    val plan = remember(displayIds, sizes, pagedLayout) { dashboardEditPlan(displayIds, sizes, pagedLayout) }
    val latestDisplay by rememberUpdatedState(displayIds)
    val latestReorder by rememberUpdatedState(onReorder)
    val haptics = LocalHapticFeedback.current

    // Where each tile sits now: written at placement, read by the drag.
    val bounds = remember { HashMap<DashboardWidgetId, Rect>() }
    var dragging by remember { mutableStateOf<DashboardWidgetId?>(null) }
    var dragStart by remember { mutableStateOf(Rect.Zero) }
    var dragAmount by remember { mutableStateOf(Offset.Zero) }
    var lastTarget by remember { mutableStateOf<DashboardWidgetId?>(null) }
    // 121: where the finger holds the tile, and where the grid sits on the window, so the held
    // tile carried to the top or bottom edge scrolls the dashboard and keeps under the finger.
    var grab by remember { mutableStateOf(Offset.Zero) }
    var gridTopInWindow by remember { mutableStateOf(0f) }
    val autoScroll = tech.mmarca.openvitals.ui.components.LocalDragAutoScroll.current
    val density = androidx.compose.ui.platform.LocalDensity.current

    fun followFinger(id: DashboardWidgetId) {
        val centre = dragStart.center + dragAmount
        // The tile the last move went to slides away; it can be a target again once left.
        lastTarget?.let { if (bounds[it]?.contains(centre) != true) lastTarget = null }
        val target = bounds.entries.firstOrNull { (other, rect) -> other != id && rect.contains(centre) }?.key
        if (target == null || target == lastTarget) return
        val order = latestDisplay.toMutableList()
        val from = order.indexOf(id)
        val to = order.indexOf(target)
        if (from == -1 || to == -1) return
        order.add(to, order.removeAt(from))
        lastTarget = target
        latestReorder(heroDisplayOrder(order))
    }

    val heldTile = dragging
    androidx.compose.runtime.LaunchedEffect(heldTile, autoScroll) {
        if (heldTile == null || autoScroll == null) return@LaunchedEffect
        val edge = with(density) { AutoScrollEdge.toPx() }
        val maxStep = with(density) { AutoScrollMaxStep.toPx() }
        while (dragging == heldTile) {
            androidx.compose.runtime.withFrameNanos { }
            val fingerY = gridTopInWindow + dragStart.top + dragAmount.y + grab.y
            val step = autoScroll.stepFor(fingerY, edge, maxStep)
            if (step == 0f) continue
            val moved = autoScroll.listState.scrollBy(step)
            if (moved != 0f) {
                // The grid moved by [moved] under the finger: the tile follows the finger, not the grid.
                dragAmount += Offset(0f, moved)
                gridTopInWindow -= moved
                followFinger(heldTile)
            }
        }
    }

    val rowHeight = dashboardWidgetRowHeight() + LayoutMetrics.minTouchTarget
    Layout(
        modifier = modifier.then(
            androidx.compose.ui.Modifier.onGloballyPositioned { gridTopInWindow = it.positionInWindow().y },
        ),
        content = {
            plan.cells.forEach { cell ->
                key(cell.key) {
                    when (cell) {
                        is DashboardEditCell.Label -> Box(contentAlignment = Alignment.BottomStart) {
                            Text(
                                text = cell.page?.let { stringResource(cell.textRes, it) } ?: stringResource(cell.textRes),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        is DashboardEditCell.RingDiagram -> DashboardHeroRoleDiagram(
                            role = cell.role,
                            accent = specsById[cell.ringOf]?.hero?.accentColor?.let { dashboardTileColors(it).ring }
                                ?: MaterialTheme.colorScheme.primary,
                        )
                        is DashboardEditCell.Tile -> {
                            val held = dragging == cell.id
                            val spec = specsById.getValue(cell.id)
                            val sizedSpec = spec.copy(content = { tileModifier ->
                                Box(tileModifier) { DashboardSizedTile(spec, cell.size) }
                            })
                            DashboardWidgetTile(
                                spec = sizedSpec,
                                specsById = specsById + (spec.id to sizedSpec),
                                isEditingDashboard = true,
                                isDragging = held,
                                dragHandleModifier = Modifier.pointerInput(cell.id) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { at ->
                                            grab = at
                                            bounds[cell.id]?.let { start ->
                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                dragStart = start
                                                dragAmount = Offset.Zero
                                                lastTarget = null
                                                dragging = cell.id
                                            }
                                        },
                                        onDrag = { change, amount ->
                                            if (dragging == cell.id) {
                                                change.consume()
                                                dragAmount += amount
                                                followFinger(cell.id)
                                            }
                                        },
                                        onDragEnd = { dragging = null },
                                        onDragCancel = { dragging = null },
                                    )
                                },
                                onRemove = { onRemoveWidget(cell.id) },
                                onMovePrevious = null,
                                onMoveNext = null,
                                // 81: the shape, not a count of spaces. A ring has no shape of its own.
                                sizeLabel = if (cell.ring) null else "${cell.size.columns}×${cell.size.rows}",
                                onCycleSize = { onCycleWidgetSize(cell.id) },
                                // 116: the values the tile shows now, as the options dialog counts them:
                                // a 1×1 tile and a ring show their main value only.
                                detailCount = if (cell.ring) 1 else dashboardDetailCount(cell.size, spec.detailCount),
                                modifier = if (held) {
                                    // The held tile is drawn under the finger, not in its slot.
                                    Modifier.zIndex(1f).offset {
                                        (dragStart.topLeft + dragAmount - (bounds[cell.id]?.topLeft ?: dragStart.topLeft)).round()
                                    }
                                } else {
                                    Modifier.animateTilePlacement()
                                },
                            )
                        }
                    }
                }
            }
        },
    ) { measurables, constraints ->
        val gap = DashboardTileGap.roundToPx()
        val rowPx = rowHeight.roundToPx()
        val labelPx = DashboardEditLabelHeight.roundToPx()
        val cellWidth = (constraints.maxWidth - gap * (DashboardTileColumns - 1)) / DashboardTileColumns
        val tops = IntArray(plan.rowCount + 1)
        for (r in 0 until plan.rowCount) {
            tops[r + 1] = tops[r] + (if (r in plan.labelRows) labelPx else rowPx) + gap
        }
        val placeables = measurables.mapIndexed { index, measurable ->
            val c = plan.cells[index]
            measurable.measure(
                Constraints.fixed(
                    width = c.columns * cellWidth + (c.columns - 1) * gap,
                    height = tops[c.row + c.rows] - gap - tops[c.row],
                ),
            )
        }
        layout(constraints.maxWidth, (tops[plan.rowCount] - gap).coerceAtLeast(0)) {
            bounds.clear()
            plan.cells.forEachIndexed { index, c ->
                val x = c.column * (cellWidth + gap)
                val y = tops[c.row]
                if (c is DashboardEditCell.Tile) {
                    val p = placeables[index]
                    bounds[c.id] = Rect(Offset(x.toFloat(), y.toFloat()), Size(p.width.toFloat(), p.height.toFloat()))
                }
            }
            plan.cells.forEachIndexed { index, c -> placeables[index].place(c.column * (cellWidth + gap), tops[c.row]) }
        }
    }
}

/** A small hero: the centre ring and the two side arcs, the [role]'s one in the tile's colour. */
@Composable
private fun DashboardHeroRoleDiagram(role: HeroRole, accent: Color, modifier: Modifier = Modifier) {
    val idle = MaterialTheme.colorScheme.outlineVariant
    val description = stringResource(
        when (role) {
            HeroRole.LEFT -> R.string.dashboard_edit_ring_left
            HeroRole.CENTRE -> R.string.dashboard_edit_ring_center
            HeroRole.RIGHT -> R.string.dashboard_edit_ring_right
        },
    )
    Canvas(modifier.fillMaxSize().semantics { contentDescription = description }) {
        val stroke = Spacing.sm.toPx()
        val radius = size.height * 0.24f
        val side = radius * 1.55f
        val centre = Offset(size.width / 2f, size.height / 2f)
        fun width(active: Boolean) = if (active) stroke * 1.5f else stroke
        fun arc(start: Float, active: Boolean) = drawArc(
            color = if (active) accent else idle,
            startAngle = start,
            sweepAngle = HeroDiagramSideSweep,
            useCenter = false,
            topLeft = Offset(centre.x - side, centre.y - side),
            size = Size(side * 2f, side * 2f),
            style = Stroke(width(active), cap = StrokeCap.Round),
        )
        arc(180f - HeroDiagramSideSweep / 2f, role == HeroRole.LEFT)
        arc(-HeroDiagramSideSweep / 2f, role == HeroRole.RIGHT)
        val centreActive = role == HeroRole.CENTRE
        drawCircle(
            color = if (centreActive) accent else idle,
            radius = radius,
            center = centre,
            style = Stroke(width(centreActive)),
        )
    }
}

/** The side arcs' sweep, as the hero draws them. */
private const val HeroDiagramSideSweep = 100f
