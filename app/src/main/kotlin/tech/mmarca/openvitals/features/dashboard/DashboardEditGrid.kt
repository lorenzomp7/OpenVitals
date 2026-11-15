package tech.mmarca.openvitals.features.dashboard

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.ui.theme.LayoutMetrics
import tech.mmarca.openvitals.ui.theme.Spacing

/** One row of the edit grid: a section label or a tile. */
private sealed interface DashboardEditEntry {
    val key: Any

    /** [page] is the 1-based page number for a page label, null for the rings and tiles labels. */
    data class Label(override val key: String, val textRes: Int, val page: Int? = null) : DashboardEditEntry

    /** [ringRole] is 0, 1, 2 for the rings' centre, left and right; null for an ordinary tile. */
    data class Tile(val id: DashboardWidgetId, val size: DashboardTileSize, val ringRole: Int? = null) : DashboardEditEntry {
        override val key: Any get() = id
    }
}

private val DashboardEditLabelHeight = Spacing.xl + Spacing.md

/**
 * 75: the dashboard's edit grid, built like the "+ log" grid: a reorderable lazy
 * grid where the held tile follows the finger and the others make room as it
 * passes them. The first three tiles are the rings at the top; with the sideways
 * pages on, the tiles after them are labelled by the page they will land on (74).
 */
@Composable
internal fun DashboardReorderableEditGrid(
    visibleIds: List<DashboardWidgetId>,
    specsById: Map<DashboardWidgetId, DashboardWidgetSpec>,
    sizeOf: (DashboardWidgetId) -> DashboardTileSize?,
    pagedLayout: Boolean,
    onMoveWidget: (DashboardWidgetId, DashboardWidgetId) -> Unit,
    onRemoveWidget: (DashboardWidgetId) -> Unit,
    onCycleWidgetSize: (DashboardWidgetId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val entries = remember(visibleIds, specsById, pagedLayout) {
        val tiles = visibleIds.mapNotNull { id ->
            specsById[id]?.let { spec -> DashboardEditEntry.Tile(id, sizeOf(id) ?: dashboardTileSize(spec, emptyMap())) }
        }
        buildList {
            val rings = tiles.take(DashboardFixedRingCount)
            val rest = tiles.drop(DashboardFixedRingCount)
            if (rings.isNotEmpty()) {
                add(DashboardEditEntry.Label("label-rings", R.string.dashboard_edit_rings_label))
                rings.forEachIndexed { index, tile -> add(tile.copy(ringRole = index)) }
            }
            if (rest.isEmpty()) return@buildList
            if (!pagedLayout) {
                add(DashboardEditEntry.Label("label-tiles", R.string.dashboard_edit_tiles_label))
                addAll(rest)
            } else {
                val pageOf = packDashboardTiles(rest.map { it.id to it.size }, rowsPerPage = DashboardPagedRows)
                    .associate { it.id to it.page }
                var page = -1
                rest.forEach { tile ->
                    val tilePage = pageOf[tile.id] ?: page
                    if (tilePage != page) {
                        page = tilePage
                        add(DashboardEditEntry.Label("label-page-$page", R.string.dashboard_edit_page_label, page + 1))
                    }
                    add(tile)
                }
            }
        }
    }
    val gridState = rememberLazyGridState()
    val reorderState = rememberReorderableLazyGridState(gridState) { from, to ->
        val fromId = from.key as? DashboardWidgetId
        val toId = to.key as? DashboardWidgetId
        if (fromId != null && toId != null && fromId != toId) onMoveWidget(fromId, toId)
    }
    val rowHeight = dashboardWidgetRowHeight() + LayoutMetrics.minTouchTarget
    val gap = DashboardTileGap

    Box(modifier = modifier.fillMaxWidth()) {
        val gridHeight = dashboardEditGridHeight(entries, rowHeight, gap)
        LazyVerticalGrid(
            columns = GridCells.Fixed(DashboardTileColumns),
            state = gridState,
            userScrollEnabled = false,
            modifier = Modifier
                .fillMaxWidth()
                .height(gridHeight)
                .animateContentSize(),
            horizontalArrangement = Arrangement.spacedBy(gap),
            verticalArrangement = Arrangement.spacedBy(gap),
        ) {
            entries.forEach { entry ->
                when (entry) {
                    is DashboardEditEntry.Label -> item(
                        key = entry.key,
                        span = { GridItemSpan(maxLineSpan) },
                    ) {
                        Box(Modifier.fillMaxWidth().height(DashboardEditLabelHeight), contentAlignment = Alignment.BottomStart) {
                            Text(
                                text = entry.page?.let { stringResource(entry.textRes, it) } ?: stringResource(entry.textRes),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    is DashboardEditEntry.Tile -> item(
                        key = entry.id,
                        span = { GridItemSpan(entry.size.columns) },
                    ) {
                        val spec = specsById.getValue(entry.id)
                        val sizedSpec = spec.copy(content = { tileModifier ->
                            Box(tileModifier) { DashboardSizedTile(spec, entry.size) }
                        })
                        ReorderableItem(reorderState, key = entry.id) { isDragging ->
                            DashboardWidgetTile(
                                spec = sizedSpec,
                                specsById = specsById + (spec.id to sizedSpec),
                                isEditingDashboard = true,
                                isDragging = isDragging,
                                dragHandleModifier = Modifier.longPressDraggableHandle(),
                                onRemove = { onRemoveWidget(entry.id) },
                                onMovePrevious = null,
                                onMoveNext = null,
                                // 81: the shape, not a count of spaces; and the values the tile shows.
                                sizeLabel = "${entry.size.columns}×${entry.size.rows}",
                                onCycleSize = { onCycleWidgetSize(entry.id) },
                                detailCount = spec.detailCount,
                                // 82: which ring of the top hero each of the first three becomes.
                                roleLabel = entry.ringRole?.let { role ->
                                    stringResource(
                                        when (role) {
                                            0 -> R.string.dashboard_edit_ring_center
                                            1 -> R.string.dashboard_edit_ring_left
                                            else -> R.string.dashboard_edit_ring_right
                                        },
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(rowHeight * entry.size.rows + gap * (entry.size.rows - 1)),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** How many tiles become the rings at the top outside edit mode. */
private const val DashboardFixedRingCount = 3

/** The grid's height for [entries], laid out as the lazy grid will: lines of four columns. */
private fun dashboardEditGridHeight(entries: List<DashboardEditEntry>, rowHeight: Dp, gap: Dp): Dp {
    var total = Dp(0f)
    var lineHeight = Dp(0f)
    var used = 0
    var lines = 0
    fun closeLine() {
        if (used > 0) {
            total += lineHeight
            lines += 1
        }
        used = 0
        lineHeight = Dp(0f)
    }
    entries.forEach { entry ->
        val (span, height) = when (entry) {
            is DashboardEditEntry.Label -> DashboardTileColumns to DashboardEditLabelHeight
            is DashboardEditEntry.Tile -> entry.size.columns to (rowHeight * entry.size.rows + gap * (entry.size.rows - 1))
        }
        if (used + span > DashboardTileColumns) closeLine()
        used += span
        if (height > lineHeight) lineHeight = height
    }
    closeLine()
    return total + gap * (lines - 1).coerceAtLeast(0)
}
