package tech.mmarca.openvitals.features.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.BuildConfig
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/** Settings is a routing surface: every section card is the only way into its screen. */
class SettingsRootTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun everySectionRendersACardThatOpensIt() {
        var opened: SettingsSection? = null
        composeRule.setContent {
            OpenVitalsTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    SettingsSection.entries.forEach { section ->
                        SettingsCategoryCard(section = section, onClick = { opened = section })
                    }
                }
            }
        }

        // Every section is reachable, not merely the first few that fit.
        SettingsSection.entries.forEach { section ->
            composeRule.onNodeWithText(string(section.titleRes)).performScrollTo().assertIsDisplayed()
        }

        val last = SettingsSection.entries.last()
        composeRule.onNodeWithText(string(last.titleRes)).performScrollTo().performClick()

        assertEquals(last, opened)
    }

    @Test
    fun theSupportCardOpensThePersonalIssueTracker() {
        var issues = 0
        composeRule.setContent {
            OpenVitalsTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    SupportOpenVitalsCard(
                        onOpenIssues = { issues++ },
                    )
                }
            }
        }

        composeRule.onNodeWithText(string(R.string.settings_support_title)).assertIsDisplayed()

        composeRule.onNodeWithText(string(R.string.settings_support_issues_action))
            .performScrollTo().performClick()

        assertEquals(1, issues)
    }

    @Test
    fun theVersionFooterNamesTheBuildABugReportWouldQuote() {
        composeRule.setContent {
            OpenVitalsTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) { SettingsVersionText() }
            }
        }

        composeRule
            .onNodeWithText(
                string(R.string.settings_app_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
            )
            .assertIsDisplayed()
    }
}
