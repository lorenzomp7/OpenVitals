package tech.mmarca.openvitals.features.dashboard

import android.graphics.Bitmap
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

class PersonalCarouselTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun sixRectanglesOccupyTwoColumnsAndThreeRows() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val preferences = context.getSharedPreferences("dashboard_widget_layout", 0)
        val saved = preferences.all
        try {
            preferences.edit().clear().putBoolean("six_tile_carousel", true).commit()
            val ids = DashboardWidgetId.entries.filter { it.dashboardWidgetRowSpan() == 1 }.take(12)
            composeRule.setContent {
                OpenVitalsTheme {
                    DashboardWidgetCarousel(
                        visibleIds = ids,
                        specsById = ids.associateWith { id ->
                            DashboardWidgetSpec(id, id.name) { modifier ->
                                Text(id.name, modifier = modifier.testTag(id.name))
                            }
                        },
                        isEditingDashboard = false,
                        pagedLayout = true,
                        onMoveWidgetToTarget = { _, _ -> }, onRemoveWidget = {},
                        actionContent = {}, hiddenContent = {},
                    )
                }
            }
            composeRule.waitForIdle()
            val bounds = ids.drop(4).take(6).map { id ->
                composeRule.onNodeWithTag(id.name).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            }
            assertEquals("Two columns", 2, bounds.map { it.left.toInt() }.distinct().size)
            assertEquals("Three rows", 3, bounds.map { it.top.toInt() }.distinct().size)
            assertEquals("Equal tile heights", 1, bounds.map { it.height.toInt() }.distinct().size)
            val file = File(context.getExternalFilesDir(null), "carousel-six.png")
            file.outputStream().use { composeRule.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
        } finally {
            preferences.edit().clear().apply {
                saved.forEach { (key, value) -> when (value) {
                    is Int -> putInt(key, value)
                    is Boolean -> putBoolean(key, value)
                    else -> Unit
                } }
            }.commit()
        }
    }
}
