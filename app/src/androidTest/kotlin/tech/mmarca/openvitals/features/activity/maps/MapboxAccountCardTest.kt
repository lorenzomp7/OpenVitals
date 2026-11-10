package tech.mmarca.openvitals.features.activity.maps

import android.graphics.Bitmap
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.After
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.preferences.AppThemeMode
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

class MapboxAccountCardTest {
    @get:Rule val composeRule = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    @After fun cleanup() { MapboxTokenStore.clear(context) }

    @Test fun loginAndRemovalReflectDeviceStorageWithoutDisplayingCredentials() {
        MapboxTokenStore.clear(context)
        composeRule.setContent {
            OpenVitalsTheme(themeMode = AppThemeMode.DARK, dynamicColor = false) {
                MapboxAccountCard(Modifier.size(360.dp, 320.dp).testTag("mapbox-account"))
            }
        }
        val signIn = context.getString(R.string.mapbox_sign_in)
        val remove = context.getString(R.string.action_remove)
        composeRule.waitUntil(10_000) { composeRule.onAllNodes(hasText(signIn)).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText(signIn).assertIsDisplayed()
        MapboxTokenStore.save(context, "pk.fixture.signature")
        composeRule.waitUntil(10_000) { composeRule.onAllNodes(hasText(remove)).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText(remove).performClick()
        composeRule.waitUntil(10_000) { composeRule.onAllNodes(hasText(signIn)).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText(signIn).assertIsDisplayed()
        File(context.getExternalFilesDir(null), "mapbox-account.png").outputStream().use {
            composeRule.onNodeWithTag("mapbox-account").captureToImage().asAndroidBitmap()
                .compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
