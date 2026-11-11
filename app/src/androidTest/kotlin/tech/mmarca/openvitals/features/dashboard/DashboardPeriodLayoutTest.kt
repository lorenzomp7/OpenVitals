package tech.mmarca.openvitals.features.dashboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.time.LocalDate
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.domain.preferences.AppThemeMode
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

class DashboardPeriodLayoutTest {
    @get:Rule val composeRule = createComposeRule()
    @Test fun pastWeekShowsYearAndDoesNotOverlapNavigationAtLargeFontScale() {
        composeRule.setContent {
            OpenVitalsTheme(themeMode = AppThemeMode.DARK, dynamicColor = false) {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, 1.3f)) {
                    Box(Modifier.width(360.dp).testTag("period-layout")) {
                        DashboardPeriodSelector(
                            state = DashboardUiState(selectedDate = LocalDate.of(2024, 12, 16), selectedRange = TimeRange.WEEK),
                            onSelectRange = {}, onPrevious = {}, onNext = {}, onOpenCalendar = {},
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()
        val title = composeRule.onAllNodesWithText("2024", substring = true).onFirst().assertIsDisplayed()
            .fetchSemanticsNode().boundsInRoot
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val arrow = composeRule.onNodeWithContentDescription(context.getString(R.string.cd_previous_period))
            .fetchSemanticsNode().boundsInRoot
        assertTrue(title.bottom <= arrow.top || title.right <= arrow.left)
        File(context.getExternalFilesDir(null), "past-week-large-font.png").outputStream().use {
            composeRule.onNodeWithTag("period-layout").captureToImage().asAndroidBitmap()
                .compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
