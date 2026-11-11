package tech.mmarca.openvitals.features.dashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import android.graphics.Bitmap
import tech.mmarca.openvitals.ui.components.AmbientFluidBrand
import tech.mmarca.openvitals.ui.components.rememberFluidBrandTimeline
import tech.mmarca.openvitals.ui.theme.LocalReducedMotion
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

class AmbientFluidBrandTest {
    @get:Rule val compose = createComposeRule()

    @Test fun brandMatchesTheActualStartWorkoutButton() {
        compose.setContent {
            OpenVitalsTheme {
                CompositionLocalProvider(LocalReducedMotion provides true) {
                    val timeline = rememberFluidBrandTimeline()
                    Column(Modifier.width(393.dp)) {
                        AmbientFluidBrand(timeline, 393.dp)
                        DashboardQuickActions(false, {}, {}, {}, Modifier.padding(horizontal = DashboardScreenPadding))
                    }
                }
            }
        }
        val brand = compose.onNodeWithTag("ambient_fluid_brand").getUnclippedBoundsInRoot()
        val action = compose.onNodeWithTag("start_workout_action").getUnclippedBoundsInRoot()
        assertEquals((action.right - action.left).value, (brand.right - brand.left).value, 0.5f)
        assertEquals((action.bottom - action.top).value, (brand.bottom - brand.top).value, 0.5f)
        val image = compose.onNodeWithTag("ambient_fluid_brand").captureToImage().asAndroidBitmap()
        val directory = checkNotNull(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null))
        File(directory, "ambient-fluid-brand.png").outputStream().use {
            check(image.compress(Bitmap.CompressFormat.PNG, 100, it))
        }
    }
}
