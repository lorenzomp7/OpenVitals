package tech.mmarca.openvitals.features.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.features.dashboard.components.DashboardTileShape
import tech.mmarca.openvitals.features.dashboard.components.MetricStatCard
import tech.mmarca.openvitals.features.dashboard.components.dashboardTileColors
import tech.mmarca.openvitals.ui.components.AutoResizeText
import tech.mmarca.openvitals.ui.components.OpenVitalsCard

/** The dashboard is four spaces wide; a tile takes one, two or four of them. */
internal const val DashboardTileColumns = 4

/** A page of the sideways layout holds three rows: six two-space tiles. */
internal const val DashboardPagedRows = 3

internal enum class DashboardTileSize(val spaces: Int, val columns: Int, val rows: Int) {
    SMALL(spaces = 1, columns = 1, rows = 1),
    MEDIUM(spaces = 2, columns = 2, rows = 1),
    LARGE(spaces = 4, columns = 2, rows = 2),
    ;

    /** The size the edit button moves to next. */
    fun next(): DashboardTileSize = entries[(ordinal + 1) % entries.size]

    companion object {
        fun fromSpaces(spaces: Int?): DashboardTileSize? = entries.firstOrNull { it.spaces == spaces }
    }
}

/** A widget's size: the one the user chose, else a ring widget is large and the rest medium. */
internal fun dashboardTileSize(
    spec: DashboardWidgetSpec,
    chosen: Map<DashboardWidgetId, Int>,
): DashboardTileSize =
    DashboardTileSize.fromSpaces(chosen[spec.id])
        ?: if (spec.style == DashboardWidgetStyle.CIRCLE) DashboardTileSize.LARGE else DashboardTileSize.MEDIUM

internal data class DashboardTilePlacement(
    val id: DashboardWidgetId,
    val size: DashboardTileSize,
    val column: Int,
    val row: Int,
    val page: Int,
)

/**
 * Places tiles in reading order, each in the first gap it fits. With
 * [rowsPerPage] a tile that does not fit the last page opens a new one.
 */
internal fun packDashboardTiles(
    tiles: List<Pair<DashboardWidgetId, DashboardTileSize>>,
    rowsPerPage: Int? = null,
): List<DashboardTilePlacement> {
    val placements = mutableListOf<DashboardTilePlacement>()
    var page = 0
    var taken = mutableListOf<BooleanArray>()
    var cursor = 0

    fun fits(size: DashboardTileSize, row: Int, column: Int): Boolean {
        if (column + size.columns > DashboardTileColumns) return false
        if (rowsPerPage != null && row + size.rows > rowsPerPage) return false
        for (r in row until row + size.rows) {
            val line = taken.getOrNull(r) ?: continue
            for (c in column until column + size.columns) if (line[c]) return false
        }
        return true
    }

    fun firstGap(size: DashboardTileSize): Pair<Int, Int>? {
        val lastRow = rowsPerPage ?: (taken.size + size.rows)
        for (row in 0 until lastRow) {
            for (column in 0 until DashboardTileColumns) {
                if (row * DashboardTileColumns + column >= cursor && fits(size, row, column)) return row to column
            }
        }
        return null
    }

    tiles.forEach { (id, size) ->
        var gap = firstGap(size)
        if (gap == null) {
            page += 1
            taken = mutableListOf()
            cursor = 0
            gap = firstGap(size) ?: (0 to 0)
        }
        val (row, column) = gap
        cursor = row * DashboardTileColumns + column + size.columns
        while (taken.size < row + size.rows) taken.add(BooleanArray(DashboardTileColumns))
        for (r in row until row + size.rows) {
            for (c in column until column + size.columns) taken[r][c] = true
        }
        placements += DashboardTilePlacement(id, size, column, row, page)
    }
    return placements
}

private val DashboardTileGap = 12.dp
private val DashboardPageDotSize = 7.dp

/** The dashboard's tiles outside edit mode: packed by size, in one sheet or in pages to swipe. */
@Composable
internal fun DashboardPackedTiles(
    ids: List<DashboardWidgetId>,
    specsById: Map<DashboardWidgetId, DashboardWidgetSpec>,
    sizes: Map<DashboardWidgetId, Int>,
    paged: Boolean,
    rowHeight: Dp,
    modifier: Modifier = Modifier,
) {
    val placements = remember(ids, specsById.keys, sizes, paged) {
        packDashboardTiles(
            tiles = ids.mapNotNull { id -> specsById[id]?.let { id to dashboardTileSize(it, sizes) } },
            rowsPerPage = DashboardPagedRows.takeIf { paged },
        )
    }
    if (placements.isEmpty()) return

    if (!paged) {
        DashboardTileSheet(placements, rows = null, specsById = specsById, rowHeight = rowHeight, modifier = modifier)
        return
    }
    val pages = placements.groupBy { it.page }.toSortedMap().values.toList()
    val pagerState = rememberPagerState(pageCount = { pages.size })
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 1,
            pageSpacing = DashboardTileGap,
            verticalAlignment = Alignment.Top,
        ) { page ->
            DashboardTileSheet(
                placements = pages[page],
                rows = DashboardPagedRows,
                specsById = specsById,
                rowHeight = rowHeight,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (pages.size > 1) {
            Row(
                modifier = Modifier.padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                repeat(pages.size) { index ->
                    val current = index == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .size(DashboardPageDotSize)
                            .background(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (current) 0.9f else 0.3f),
                                shape = CircleShape,
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardTileSheet(
    placements: List<DashboardTilePlacement>,
    /** A fixed row count, so every page of the pager is as tall as the fullest. */
    rows: Int?,
    specsById: Map<DashboardWidgetId, DashboardWidgetSpec>,
    rowHeight: Dp,
    modifier: Modifier = Modifier,
) {
    DashboardPlacedTileGrid(placements, rowHeight, modifier, minimumRows = rows ?: 0) { placement ->
        specsById[placement.id]?.let { DashboardSizedTile(it, placement.size) }
    }
}

@Composable
internal fun DashboardPlacedTileGrid(
    placements: List<DashboardTilePlacement>,
    rowHeight: Dp,
    modifier: Modifier = Modifier,
    minimumRows: Int = 0,
    tile: @Composable (DashboardTilePlacement) -> Unit,
) {
    Layout(
        modifier = modifier,
        content = {
            placements.forEach { placement ->
                androidx.compose.runtime.key(placement.id) { Box { tile(placement) } }
            }
        },
    ) { measurables, constraints ->
        val gap = DashboardTileGap.roundToPx()
        val rowPx = rowHeight.roundToPx()
        val cell = (constraints.maxWidth - gap * (DashboardTileColumns - 1)) / DashboardTileColumns
        val rowCount = maxOf(minimumRows, placements.maxOf { it.row + it.size.rows })
        val height = rowCount * rowPx + (rowCount - 1).coerceAtLeast(0) * gap
        val placeables = measurables.mapIndexed { index, measurable ->
            val size = placements[index].size
            measurable.measure(
                Constraints.fixed(
                    width = size.columns * cell + (size.columns - 1) * gap,
                    height = size.rows * rowPx + (size.rows - 1) * gap,
                ),
            )
        }
        layout(constraints.maxWidth, height) {
            placeables.forEachIndexed { index, placeable ->
                val placement = placements[index]
                placeable.place(x = placement.column * (cell + gap), y = placement.row * (rowPx + gap))
            }
        }
    }
}

/** One widget drawn at a size: its own drawing where that fits the size, else one from its numbers. */
@Composable
internal fun DashboardSizedTile(spec: DashboardWidgetSpec, size: DashboardTileSize) {
    val ring = spec.hero
    if (ring == null) { spec.content(Modifier.fillMaxSize()); return }
    when (size) {
        DashboardTileSize.SMALL -> DashboardSmallTile(ring)
        DashboardTileSize.MEDIUM -> MetricStatCard(
            title = ring.title, value = DisplayValue(ring.value, ""), icon = ring.icon,
            accentColor = ring.accentColor, modifier = Modifier.fillMaxSize(),
            trailing = spec.trailing,
            subtitle = spec.details.take(dashboardDetailCount(size, spec.detailCount) - 1).joinToString("\n").ifBlank { null }, onClick = ring.onClick,
        )
        DashboardTileSize.LARGE -> DashboardLargeTile(ring, spec.details.take(dashboardDetailCount(size, spec.detailCount) - 1))
    }
}

/** One space: the icon and the number, nothing else. */
@Composable
private fun DashboardSmallTile(ring: DashboardHeroRing) {
    val colors = dashboardTileColors(ring.accentColor)
    OpenVitalsCard(
        modifier = Modifier.fillMaxSize(),
        onClick = ring.onClick,
        containerColor = colors.container,
        shape = DashboardTileShape,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = ring.icon,
                contentDescription = ring.title,
                tint = colors.icon,
                modifier = Modifier.size(20.dp),
            )
            AutoResizeText(
                text = ring.value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                minFontSize = 11.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/** Four spaces: the number inside its ring, for a widget that has no ring drawing of its own. */
@Composable
private fun DashboardLargeTile(ring: DashboardHeroRing, details: List<String>) {
    val colors = dashboardTileColors(ring.accentColor)
    OpenVitalsCard(
        modifier = Modifier.fillMaxSize(),
        onClick = ring.onClick,
        containerColor = colors.container,
        shape = DashboardTileShape,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 12.dp.toPx()
                val diameter = minOf(size.width, size.height) - stroke
                val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                val arc = Size(diameter, diameter)
                val style = Stroke(width = stroke, cap = StrokeCap.Round)
                drawArc(colors.ringTrack, 135f, 270f, useCenter = false, topLeft = topLeft, size = arc, style = style)
                val filled = 270f * ring.fraction.coerceIn(0f, 1f)
                if (filled > 0f) {
                    drawArc(colors.ring, 135f, filled, useCenter = false, topLeft = topLeft, size = arc, style = style)
                }
            }
            Column(
                modifier = Modifier.padding(horizontal = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AutoResizeText(
                    text = ring.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.icon,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
                AutoResizeText(
                    text = ring.value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    minFontSize = 13.sp,
                )
                details.forEach { detail ->
                    AutoResizeText(text = detail, style = MaterialTheme.typography.bodySmall,
                        color = colors.icon, maxLines = 1, textAlign = TextAlign.Center)
                }
            }
        }
    }
}
