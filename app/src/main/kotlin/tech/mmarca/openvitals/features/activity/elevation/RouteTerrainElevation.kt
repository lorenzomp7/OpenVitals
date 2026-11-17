package tech.mmarca.openvitals.features.activity.elevation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.tan
import kotlinx.coroutines.withContext
import tech.mmarca.openvitals.core.performance.DispatcherProvider
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.domain.model.ExerciseRoutePoint
import tech.mmarca.openvitals.features.activity.maps.MapboxTokenStore

/**
 * Where a session's height profile came from. [GPS_MAPBOX_FAILED]: Mapbox is signed in but its
 * terrain could not be read (offline, a refused tile), so the GPS drew it; the footnote says so.
 */
enum class RouteElevationSource { TERRAIN_TILES, MAPBOX_TERRAIN, GPS, GPS_MAPBOX_FAILED }

/**
 * 104: a route's height profile from a terrain model rather than the GPS. A watch's GPS
 * altitude drifts tens of metres over a ride (one dropped 25 m over a flat hour), so its own
 * app shows a calibrated height and the raw one disagrees with it. The terrain comes from the
 * imported elevation tiles when they cover the route, else from Mapbox's terrain tiles when
 * Mapbox is signed in: requested by tile, as the map's own tiles are, never with the route.
 */
@Singleton
class RouteTerrainElevation @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val tiles: ElevationTileRepository,
    private val preferences: PreferencesRepository,
    private val dispatchers: DispatcherProvider,
) {
    private val cache = File(context.cacheDir, "terrain-dem")

    /**
     * The altitude at each point, or null when no terrain covers the whole route: then the
     * source says whether Mapbox was signed in and failed, so the footnote can tell.
     */
    suspend fun altitudes(points: List<ExerciseRoutePoint>): Pair<RouteElevationSource, List<Double>?> =
        withContext(dispatchers.io) {
            if (points.isEmpty()) return@withContext RouteElevationSource.GPS to null
            if (preferences.elevationCorrectionEnabled && tiles.hasTiles) {
                val dem = points.map { tiles.elevationAt(it.latitude, it.longitude) }
                if (dem.all { it != null }) {
                    return@withContext RouteElevationSource.TERRAIN_TILES to smoothTerrain(dem.map { it!! })
                }
            }
            val token = MapboxTokenStore.read(context).takeIf { it.isNotBlank() }
                ?: return@withContext RouteElevationSource.GPS to null
            runCatching { mapboxAltitudes(points, token) }.getOrNull()
                ?.let { RouteElevationSource.MAPBOX_TERRAIN to smoothTerrain(it) }
                ?: (RouteElevationSource.GPS_MAPBOX_FAILED to null)
        }

    private fun mapboxAltitudes(points: List<ExerciseRoutePoint>, token: String): List<Double>? {
        // 104: a long ride crosses more tiles than is reasonable at the finest zoom; a coarser
        // one still beats the GPS, so step out until the route fits rather than giving up.
        val (zoom, positions) = (TileZoom downTo MinTileZoom).asSequence()
            .map { z -> z to points.map { tilePosition(it.latitude, it.longitude, z) } }
            .firstOrNull { (_, positions) -> positions.flatMap { it.neighbours() }.distinct().size <= MaxTiles }
            ?: return null
        val keys = positions.flatMap { it.neighbours() }.distinct()
        // 104: one tile that will not come is no reason to drop the others: the points on it are
        // filled between their neighbours, as long as most of the route was read.
        val bitmaps = keys.mapNotNull { key -> tile(key, token)?.let { key to it } }.toMap()
        val heights = positions.map { position ->
            if (position.key in bitmaps) bilinear(position, zoom, bitmaps) else null
        }
        if (heights.count { it != null } < heights.size * MinCoveredShare) return null
        return fillGaps(heights)
    }

    /**
     * 104: the height between the four pixels around a point, not the one it falls in: a pixel
     * is ten metres wide, and reading the nearest drew the profile as stairs.
     */
    private fun bilinear(position: TilePosition, zoom: Int, bitmaps: Map<TileKey, Bitmap>): Double {
        val size = bitmaps.getValue(position.key).width
        // Pixel centres sit at half a pixel; work in the world's pixel grid so a neighbour
        // across a tile edge is read from the next tile.
        val worldX = (position.key.x + position.x) * size - 0.5
        val worldY = (position.key.y + position.y) * size - 0.5
        val x0 = floor(worldX)
        val y0 = floor(worldY)
        val fx = worldX - x0
        val fy = worldY - y0
        fun at(px: Double, py: Double): Double {
            val tiles = 1 shl zoom
            val tx = Math.floorDiv(px.toInt(), size).coerceIn(0, tiles - 1)
            val ty = Math.floorDiv(py.toInt(), size).coerceIn(0, tiles - 1)
            val bitmap = bitmaps[TileKey(zoom, tx, ty)] ?: bitmaps.getValue(position.key)
            val ix = (px.toInt() - tx * size).coerceIn(0, bitmap.width - 1)
            val iy = (py.toInt() - ty * size).coerceIn(0, bitmap.height - 1)
            return decodeHeight(bitmap.getPixel(ix, iy))
        }
        val top = at(x0, y0) * (1 - fx) + at(x0 + 1, y0) * fx
        val bottom = at(x0, y0 + 1) * (1 - fx) + at(x0 + 1, y0 + 1) * fx
        return top * (1 - fy) + bottom * fy
    }

    private fun decodeHeight(pixel: Int): Double {
        val red = (pixel shr 16) and 0xFF
        val green = (pixel shr 8) and 0xFF
        val blue = pixel and 0xFF
        return -10_000.0 + (red * 65_536 + green * 256 + blue) * 0.1
    }

    private fun tile(key: TileKey, token: String): Bitmap? {
        // One retry: a dropped connection is not a reason to draw the drifting GPS.
        repeat(2) { attempt ->
            runCatching { fetchTile(key, token) }.getOrNull()?.let { return it }
            if (attempt == 0) Thread.sleep(RetryDelayMillis)
        }
        return null
    }

    private fun fetchTile(key: TileKey, token: String): Bitmap? {
        val file = File(cache, "${key.z}/${key.x}/${key.y}.png")
        if (!file.isFile) {
            val url = URL("https://api.mapbox.com/v4/mapbox.mapbox-terrain-dem-v1/${key.z}/${key.x}/${key.y}.pngraw?access_token=$token")
            val connection = url.openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = TimeoutMillis
                connection.readTimeout = TimeoutMillis
                if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
                file.parentFile?.mkdirs()
                val partial = File(file.path + ".part")
                connection.inputStream.use { input -> partial.outputStream().use { input.copyTo(it) } }
                if (!partial.renameTo(file)) return null
            } finally {
                connection.disconnect()
            }
        }
        // The colours are numbers: no premultiplying and no colour-space conversion.
        val bitmap = BitmapFactory.decodeFile(
            file.path,
            BitmapFactory.Options().apply {
                inPremultiplied = false
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inPreferredColorSpace = android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.SRGB)
            },
        )
        if (bitmap == null) file.delete()
        return bitmap
    }

    private data class TileKey(val z: Int, val x: Int, val y: Int)

    private data class TilePosition(val key: TileKey, val x: Double, val y: Double) {
        /** The tile and the ones a bilinear read near its right and bottom edges reaches into. */
        fun neighbours(): List<TileKey> {
            val max = (1 shl key.z) - 1
            val nx = (key.x + 1).coerceAtMost(max)
            val ny = (key.y + 1).coerceAtMost(max)
            return listOf(key, key.copy(x = nx), key.copy(y = ny), key.copy(x = nx, y = ny)).distinct()
        }
    }

    /** Where a coordinate falls in the Web Mercator tile grid at [zoom], and within its tile. */
    private fun tilePosition(latitude: Double, longitude: Double, zoom: Int): TilePosition {
        val scale = (1 shl zoom).toDouble()
        val worldX = (longitude + 180.0) / 360.0 * scale
        val radians = latitude * PI / 180.0
        val worldY = (1.0 - ln(tan(radians) + 1.0 / kotlin.math.cos(radians)) / PI) / 2.0 * scale
        val tileX = floor(worldX)
        val tileY = floor(worldY)
        return TilePosition(TileKey(zoom, tileX.toInt(), tileY.toInt()), worldX - tileX, worldY - tileY)
    }

    private companion object {
        /** About ten metres a pixel: the model's own resolution. */
        const val TileZoom = 14
        /** The coarsest zoom tried: about eighty metres a pixel, still finer than a GPS drift. */
        const val MinTileZoom = 11
        /** A long day's ride crosses a few dozen at the finest zoom; more steps out a zoom. */
        const val MaxTiles = 120
        const val TimeoutMillis = 10_000
        const val RetryDelayMillis = 500L
        /** The share of the route whose own tile must have been read. */
        const val MinCoveredShare = 0.8
    }
}

/**
 * 104: a light running mean over the terrain heights. The model's own grid and the GPS track's
 * wander across it leave a metre or two of jitter that the chart drew as noise.
 */
internal fun smoothTerrain(heights: List<Double>, window: Int = 5): List<Double> {
    if (heights.size <= 2 || window <= 1) return heights
    val half = window / 2
    return heights.indices.map { index ->
        val from = (index - half).coerceAtLeast(0)
        val to = (index + half).coerceAtMost(heights.lastIndex)
        var sum = 0.0
        for (i in from..to) sum += heights[i]
        sum / (to - from + 1)
    }
}

/** 104: the heights a missing tile left out, drawn straight between the ones either side. */
internal fun fillGaps(heights: List<Double?>): List<Double> {
    val known = heights.indices.filter { heights[it] != null }
    if (known.isEmpty()) return emptyList()
    return heights.indices.map { index ->
        heights[index] ?: run {
            val insertion = -known.binarySearch(index) - 1
            val before = known.getOrNull(insertion - 1)
            val after = known.getOrNull(insertion)
            when {
                before == null -> heights[after!!]!!
                after == null -> heights[before]!!
                else -> heights[before]!! + (heights[after]!! - heights[before]!!) * (index - before) / (after - before)
            }
        }
    }
}
