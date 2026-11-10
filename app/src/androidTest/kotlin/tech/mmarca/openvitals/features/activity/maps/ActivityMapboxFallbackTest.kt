package tech.mmarca.openvitals.features.activity.maps

import android.graphics.Bitmap
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.time.Instant
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.BuildConfig
import tech.mmarca.openvitals.domain.model.ExerciseRoutePoint
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

class ActivityMapboxFallbackTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun noTokenStillDisplaysTheLocalRoute() {
        assumeTrue("This test verifies a build without a public token", BuildConfig.MAPBOX_PUBLIC_TOKEN.isBlank())
        val points = listOf(45.13 to 10.02, 45.14 to 10.03, 45.135 to 10.04).mapIndexed { index, (lat, lon) ->
            ExerciseRoutePoint(time = Instant.EPOCH.plusSeconds(index.toLong()), latitude = lat, longitude = lon, altitudeMeters = null, horizontalAccuracyMeters = null, verticalAccuracyMeters = null)
        }
        composeRule.setContent {
            OpenVitalsTheme {
                ActivityMapboxMap(points, Modifier.size(320.dp, 260.dp).testTag("route"))
            }
        }
        composeRule.onNodeWithTag("route").assertIsDisplayed()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.getExternalFilesDir(null), "mapbox-local-fallback.png").outputStream().use {
            composeRule.onNodeWithTag("route").captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test
    fun invalidCoordinatesDoNotCreateAMap() {
        composeRule.setContent {
            OpenVitalsTheme {
                ActivityMapboxMap(listOf(ExerciseRoutePoint(Instant.EPOCH, Double.NaN, 10.0, null, null, null)), Modifier.testTag("route"))
            }
        }
        composeRule.onNodeWithTag("route").assertDoesNotExist()
    }
}
