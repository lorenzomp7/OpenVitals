package tech.mmarca.openvitals.data.repository

import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.util.FakeSharedPreferences

class SettingsBackupRepositoryTest {
    @Test fun `invalid input is rejected before any preferences are opened`() = runTest {
        val resolver = mockk<ContentResolver>()
        val uri = mockk<Uri>()
        every { resolver.openInputStream(uri) } returns ByteArrayInputStream("not a backup".toByteArray())
        val context = mockk<Context> { every { contentResolver } returns resolver }
        assertTrue(runCatching { SettingsBackupRepository(context).preview(uri) }.isFailure)
        verify(exactly = 0) { context.getSharedPreferences(any(), any()) }
    }

    @Test fun `restoring configuration preserves device-local permissions and resets absent settings`() = runTest {
        val prefs = FakeSharedPreferences()
        prefs.edit().putString("app_theme_mode", "dark").putBoolean("dynamic_color", false)
            .putString("acknowledged_feature_permissions_activity", "local grant").commit()
        val context = mockk<Context> {
            every { getSharedPreferences("openvitals_prefs", Context.MODE_PRIVATE) } returns prefs
        }
        SettingsBackupRepository(context).restore(SettingsBackupPreview(mapOf(
            "openvitals_prefs" to mapOf("app_theme_mode" to "light"),
        )))
        assertEquals("light", prefs.getString("app_theme_mode", null))
        assertTrue(!prefs.contains("dynamic_color"))
        assertEquals("local grant", prefs.getString("acknowledged_feature_permissions_activity", null))
    }

    @Test fun `a failed second group restores the first group instead of leaving a partial import`() = runTest {
        val main = FakeSharedPreferences()
        val cycle = FakeSharedPreferences()
        main.edit().putString("app_theme_mode", "dark").commit()
        cycle.edit().putBoolean("reminders_enabled", false).commit()
        var failOnce = true
        val failingCycle = object : SharedPreferences by cycle {
            override fun edit(): SharedPreferences.Editor {
                val editor = cycle.edit()
                return object : SharedPreferences.Editor by editor {
                    override fun commit(): Boolean = if (failOnce) { failOnce = false; false } else editor.commit()
                }
            }
        }
        val context = mockk<Context> {
            every { getSharedPreferences("openvitals_prefs", Context.MODE_PRIVATE) } returns main
            every { getSharedPreferences("openvitals_cycle_preferences", Context.MODE_PRIVATE) } returns failingCycle
        }
        val result = runCatching {
            SettingsBackupRepository(context).restore(SettingsBackupPreview(linkedMapOf(
                "openvitals_prefs" to mapOf("app_theme_mode" to "light"),
                "openvitals_cycle_preferences" to mapOf("reminders_enabled" to true),
            )))
        }
        assertTrue(result.isFailure)
        assertEquals("dark", main.getString("app_theme_mode", null))
        assertEquals(false, cycle.getBoolean("reminders_enabled", true))
    }

    @Test fun `export excludes unapproved preference values and produces an importable file`() = runTest {
        val prefs = FakeSharedPreferences()
        prefs.edit().putString("app_theme_mode", "dark").putString("mapbox_token", "credential").commit()
        val resolver = mockk<ContentResolver>()
        val uri = mockk<Uri>()
        val output = ByteArrayOutputStream()
        every { resolver.openOutputStream(uri, "wt") } returns output
        val context = mockk<Context> {
            every { contentResolver } returns resolver
            every { filesDir } returns java.io.File(System.getProperty("java.io.tmpdir"), "openvitals-backup-test-no-photo")
            every { getSharedPreferences(any(), Context.MODE_PRIVATE) } returns FakeSharedPreferences()
            every { getSharedPreferences("openvitals_prefs", Context.MODE_PRIVATE) } returns prefs
        }
        SettingsBackupRepository(context).export(uri)
        val text = output.toString("UTF-8")
        assertTrue(!text.contains("credential"))
        assertEquals("dark", SettingsBackupCodec.decode(text).getValue("openvitals_prefs")["app_theme_mode"])
    }
}
