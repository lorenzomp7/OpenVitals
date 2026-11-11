package tech.mmarca.openvitals.features.dashboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

class DashboardTileOptionsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun availableDetailCountsFollowSizeAndChangesAreApplied() {
        var selectedCount = 2
        compose.setContent {
            var size by remember { mutableStateOf(DashboardTileSize.SMALL) }
            var count by remember { mutableStateOf(2) }
            OpenVitalsTheme {
                DashboardTileOptionsDialog("Steps", size, count,
                    onSize = { size = it }, onCount = { count = it; selectedCount = it }, onDismiss = {})
            }
        }
        compose.onNodeWithText("1").assertIsDisplayed()
        compose.onNodeWithText("3").assertDoesNotExist()
        compose.onNodeWithText("2×1").performClick()
        compose.onNodeWithText("1").assertDoesNotExist()
        compose.onNodeWithText("2").assertIsDisplayed()
        compose.onNodeWithText("3").performClick()
        compose.runOnIdle { assertEquals(3, selectedCount) }
        compose.onNodeWithText("2×2").performClick()
        compose.onNodeWithText("3").assertIsDisplayed()
    }
}
