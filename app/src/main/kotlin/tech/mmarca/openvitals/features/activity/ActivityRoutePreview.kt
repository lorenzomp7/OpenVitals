package tech.mmarca.openvitals.features.activity

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Path
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.domain.model.ExerciseRoutePoint
import tech.mmarca.openvitals.ui.theme.WorkoutColor

@Composable
internal fun RoutePreview(
    points: List<ExerciseRoutePoint>,
    routeBreakIndexes: List<Int> = emptyList(),
    modifier: Modifier = Modifier,
) {
    val routeGeometry = remember(points, routeBreakIndexes) {
        points.toRoutePreviewGeometry(routeBreakIndexes)
    }
    val routeColor = androidx.compose.ui.graphics.Color(0xFF29B6F6)
    val startColor = androidx.compose.ui.graphics.Color(0xFF22C55E)
    val endColor = androidx.compose.ui.graphics.Color(0xFFEF4444)
    val backgroundColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    val borderColor = androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)

    Box(modifier = modifier.drawWithCache {
        val padding = 18.dp.toPx()
        val drawWidth = size.width - padding * 2
        val drawHeight = size.height - padding * 2
        if (drawWidth <= 0f || drawHeight <= 0f || routeGeometry.points.isEmpty()) {
            return@drawWithCache onDrawBehind { }
        }
        fun project(point: ExerciseRoutePoint): Offset = Offset(
            padding + ((point.longitude - routeGeometry.minLongitude) / routeGeometry.longitudeSpan).toFloat() * drawWidth,
            padding + ((routeGeometry.maxLatitude - point.latitude) / routeGeometry.latitudeSpan).toFloat() * drawHeight,
        )
        val projected = routeGeometry.segments.map { segment -> segment.map(::project) }
        val paths = projected.map { segment -> Path().apply {
            segment.forEachIndexed { index, point ->
                if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
            }
        } }
        val corner = CornerRadius(10.dp.toPx(), 10.dp.toPx())
        val lineStyle = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
        val borderStyle = Stroke(width = 1.dp.toPx())
        val markerRadius = 6.dp.toPx()
        onDrawBehind {
            drawRoundRect(color = backgroundColor, cornerRadius = corner)
            paths.forEach { drawPath(it, routeColor, style = lineStyle) }
            projected.firstOrNull()?.firstOrNull()?.let { drawCircle(startColor, markerRadius, it) }
            projected.lastOrNull()?.lastOrNull()?.let { drawCircle(endColor, markerRadius, it) }
            drawRoundRect(color = borderColor, style = borderStyle, cornerRadius = corner)
        }
    })
}

private data class RoutePreviewGeometry(
    val points: List<ExerciseRoutePoint>,
    val segments: List<List<ExerciseRoutePoint>>,
    val maxLatitude: Double,
    val minLongitude: Double,
    val longitudeSpan: Double,
    val latitudeSpan: Double,
)

private fun List<ExerciseRoutePoint>.toRoutePreviewGeometry(routeBreakIndexes: List<Int>): RoutePreviewGeometry {
    val orderedPoints = sortedBy { it.time }
    if (orderedPoints.isEmpty()) {
        return RoutePreviewGeometry(
            points = emptyList(),
            segments = emptyList(),
            maxLatitude = 0.0,
            minLongitude = 0.0,
            longitudeSpan = 1.0,
            latitudeSpan = 1.0,
        )
    }
    val minLatitude = orderedPoints.minOf { it.latitude }
    val maxLatitude = orderedPoints.maxOf { it.latitude }
    val minLongitude = orderedPoints.minOf { it.longitude }
    val maxLongitude = orderedPoints.maxOf { it.longitude }
    val longitudeSpan = (maxLongitude - minLongitude).takeIf { it > 0.0 } ?: 0.00001
    val latitudeSpan = (maxLatitude - minLatitude).takeIf { it > 0.0 } ?: 0.00001
    return RoutePreviewGeometry(
        points = orderedPoints,
        segments = orderedPoints.toRouteSegments(routeBreakIndexes),
        maxLatitude = if (maxLatitude == minLatitude) maxLatitude + latitudeSpan / 2.0 else maxLatitude,
        minLongitude = if (maxLongitude == minLongitude) minLongitude - longitudeSpan / 2.0 else minLongitude,
        longitudeSpan = longitudeSpan,
        latitudeSpan = latitudeSpan,
    )
}

private fun List<ExerciseRoutePoint>.toRouteSegments(routeBreakIndexes: List<Int>): List<List<ExerciseRoutePoint>> {
    if (isEmpty()) return emptyList()
    val breakIndexes = routeBreakIndexes
        .filter { it in 1 until size }
        .toSet()
    val segments = mutableListOf<MutableList<ExerciseRoutePoint>>()
    forEachIndexed { index, point ->
        if (index == 0 || index in breakIndexes) {
            segments += mutableListOf(point)
        } else {
            segments.lastOrNull()?.add(point)
        }
    }
    return segments
}
