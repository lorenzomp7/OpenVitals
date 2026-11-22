package tech.mmarca.openvitals.features.dashboard

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.components.rememberFluidBrandTimeline
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/** The header holds profile, achievements, the toggle, the pencil and settings, each named. */
class DashboardHeaderSemanticsTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun headerButtonsAreNamedAndTappable() {
        composeRule.setContent {
            OpenVitalsTheme {
                DashboardHeader(
                    timeline = rememberFluidBrandTimeline(),
                    photo = null,
                    onOpenProfile = {},
                    onOpenAchievements = {},
                    onOpenSettings = {},
                    onToggleDashboardEdit = {},
                )
            }
        }
        listOf(R.string.user_profile_title, R.string.cd_achievements, R.string.cd_edit_dashboard, R.string.cd_settings)
            .forEach { composeRule.onNodeWithContentDescription(string(it)).assertHasClickAction() }
    }
}
