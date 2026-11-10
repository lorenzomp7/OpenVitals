package tech.mmarca.openvitals.features.activity.maps

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.mapbox.common.MapboxOptions
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createCircleAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPolylineAnnotationManager
import tech.mmarca.openvitals.BuildConfig
import tech.mmarca.openvitals.domain.model.ExerciseRoutePoint
import tech.mmarca.openvitals.features.activity.RoutePreview

/** Saved workouts only. No location provider and no upload of workout samples. */
@Composable
internal fun ActivityMapboxMap(
    points: List<ExerciseRoutePoint>,
    modifier: Modifier = Modifier,
) {
    val validPoints = remember(points) { points.filter {
        it.latitude.isFinite() && it.longitude.isFinite() &&
            it.latitude in -90.0..90.0 && it.longitude in -180.0..180.0
    }.sortedBy { it.time } }
    if (validPoints.isEmpty()) return
    if (BuildConfig.MAPBOX_PUBLIC_TOKEN.isBlank()) {
        OfflineRouteMapOrPreview(points = validPoints, modifier = modifier)
        return
    }
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    key(validPoints, dark) {
        MapboxRouteContent(validPoints, dark, modifier)
    }
}

@Composable
private fun MapboxRouteContent(points: List<ExerciseRoutePoint>, dark: Boolean, modifier: Modifier) {
    val context = LocalContext.current
    var ready by remember { mutableStateOf(false) }
    val coordinates = remember(points) { points.map { Point.fromLngLat(it.longitude, it.latitude) } }
    val mapView = remember {
        MapboxOptions.accessToken = BuildConfig.MAPBOX_PUBLIC_TOKEN
        MapView(context)
    }
    DisposableEffect(mapView) {
        // Keep the local preview visible until a cartographic style has actually loaded.
        val errorSubscription = mapView.mapboxMap.subscribeMapLoadingError { ready = false }
        mapView.mapboxMap.loadStyle(if (dark) Style.DARK else Style.LIGHT) {
            val lineManager = mapView.annotations.createPolylineAnnotationManager()
            if (coordinates.size > 1) {
                lineManager.create(PolylineAnnotationOptions().withPoints(coordinates)
                    .withLineColor("#29B6F6").withLineWidth(4.0))
            }
            val markers = mapView.annotations.createCircleAnnotationManager()
            listOf(coordinates.first() to "#22C55E", coordinates.last() to "#EF4444").forEach { (point, color) ->
                markers.create(CircleAnnotationOptions().withPoint(point).withCircleColor(color)
                    .withCircleRadius(7.0).withCircleStrokeColor("#FFFFFF").withCircleStrokeWidth(2.0))
            }
            mapView.post {
                mapView.mapboxMap.cameraForCoordinates(
                    coordinates, CameraOptions.Builder().build(), EdgeInsets(48.0, 48.0, 48.0, 48.0), 16.0, null,
                ) { camera ->
                    mapView.mapboxMap.setCamera(camera)
                    ready = true
                }
            }
        }
        onDispose {
            errorSubscription.cancel()
            mapView.onDestroy()
        }
    }
    Box(modifier) {
        // Also covers offline first launch, missing styles, revoked token and tile failures.
        RoutePreview(points = points, modifier = Modifier.fillMaxSize())
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize().alpha(if (ready) 1f else 0f))
    }
}
