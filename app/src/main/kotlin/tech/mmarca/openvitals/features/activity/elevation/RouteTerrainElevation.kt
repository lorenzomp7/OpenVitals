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

/** Where a session's height profile came from. */
enum class RouteElevationSource { TERRAIN_TILES, MAPBOX_TERRAIN, GPS }

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

    /** The altitude at each point, or null when no terrain covers the whole route. */
    suspend fun altitudes(points: List<ExerciseRoutePoint>): Pair<RouteElevationSource, List<Double>>? =
        withContext(dispatchers.io) {
            if (points.isEmpty()) return@withContext null
            if (preferences.elevationCorrectionEnabled && tiles.hasTiles) {
                val dem = points.map { tiles.elevationAt(it.latitude, it.longitude) }
                if (dem.all { it != null }) return@withContext RouteElevationSource.TERRAIN_TILES to dem.map { it!! }
            }
            val token = MapboxTokenStore.read(context).takeIf { it.isNotBlank() } ?: return@withContext null
            runCatching { mapboxAltitudes(points, token) }.getOrNull()?.let { RouteElevationSource.MAPBOX_TERRAIN to it }
        }

    private fun mapboxAltitudes(points: List<ExerciseRoutePoint>, token: String): List<Double>? {
        val positions = points.map { tilePosition(it.latitude, it.longitude) }
        val keys = positions.map { it.key }.distinct()
        if (keys.size > MaxTiles) return null
        val bitmaps = keys.associateWith { key -> tile(key, token) ?: return null }
        return positions.map { position ->
            val bitmap = bitmaps.getValue(position.key)
            val x = (position.x * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
            val y = (position.y * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
            val pixel = bitmap.getPixel(x, y)
            val red = (pixel shr 16) and 0xFF
            val green = (pixel shr 8) and 0xFF
            val blue = pixel and 0xFF
            -10_000.0 + (red * 65_536 + green * 256 + blue) * 0.1
        }
    }

    private fun tile(key: TileKey, token: String): Bitmap? {
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
        return BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inPremultiplied = false })
    }

    private data class TileKey(val z: Int, val x: Int, val y: Int)

    private data class TilePosition(val key: TileKey, val x: Double, val y: Double)

    /** Where a coordinate falls in the Web Mercator tile grid at [TileZoom], and within its tile. */
    private fun tilePosition(latitude: Double, longitude: Double): TilePosition {
        val scale = (1 shl TileZoom).toDouble()
        val worldX = (longitude + 180.0) / 360.0 * scale
        val radians = latitude * PI / 180.0
        val worldY = (1.0 - ln(tan(radians) + 1.0 / kotlin.math.cos(radians)) / PI) / 2.0 * scale
        val tileX = floor(worldX)
        val tileY = floor(worldY)
        return TilePosition(TileKey(TileZoom, tileX.toInt(), tileY.toInt()), worldX - tileX, worldY - tileY)
    }

    private companion object {
        /** About ten metres a pixel: the model's own resolution. */
        const val TileZoom = 14
        /** A long day's ride crosses a few dozen; a route needing more is left on its GPS. */
        const val MaxTiles = 80
        const val TimeoutMillis = 10_000
    }
}
