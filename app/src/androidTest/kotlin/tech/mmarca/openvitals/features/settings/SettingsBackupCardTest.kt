package tech.mmarca.openvitals.features.settings

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

class SettingsBackupCardTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun exportAndImportHaveSeparateActions() {
        var exported = 0
        var imported = 0
        compose.setContent { OpenVitalsTheme { SettingsBackupCard(false, { exported++ }, { imported++ }) } }
        compose.onNodeWithText(context.getString(R.string.settings_backup_export)).assertIsEnabled().performClick()
        compose.onNodeWithText(context.getString(R.string.settings_backup_import)).assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(1, exported); assertEquals(1, imported) }
    }

    @Test fun controlsAreDisabledWhileTransferring() {
        compose.setContent { OpenVitalsTheme { SettingsBackupCard(true, {}, {}) } }
        compose.onNodeWithText(context.getString(R.string.settings_backup_export)).assertIsNotEnabled()
        compose.onNodeWithText(context.getString(R.string.settings_backup_import)).assertIsNotEnabled()
    }
}
