package tech.mmarca.openvitals.features.imports.screenshot

import android.content.ActivityNotFoundException
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.google.mlkit.vision.common.InputImage
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runners.MethodSorters
import tech.mmarca.openvitals.healthconnect.openOwnHealthConnectPermissionSettings
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class ScreenshotDeviceTest {
    @get:Rule val compose = createComposeRule()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    @Test fun a_realOcrFindsSplitLeanMassAndBodyWaterWithSeparateUnits() = runBlocking {
        val bitmap = Bitmap.createBitmap(1080, 1300, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 46f }
        canvas.drawText("WEIGHT 69.9 kg", 70f, 110f, paint)
        canvas.drawText("BMI 23.6", 70f, 200f, paint)
        canvas.drawText("BODY FAT 13.4 %", 70f, 290f, paint)
        canvas.drawText("LEAN", 70f, 410f, paint)
        canvas.drawText("MASS", 70f, 470f, paint)
        canvas.drawText("60.53", 500f, 410f, paint)
        canvas.drawText("kg", 500f + paint.measureText("60.53") + 20f, 410f, paint)
        canvas.drawText("BODY WATER", 70f, 650f, paint)
        canvas.drawText("58.2", 500f, 650f, paint)
        canvas.drawText("%", 500f + paint.measureText("58.2") + 20f, 650f, paint)
        canvas.drawText("MUSCLE MASS 44.1 kg", 70f, 850f, paint)
        val result = screenshotOcrValues(recognizeScreenshot(InputImage.fromBitmap(bitmap, 0)))
        assertTrue("Missing lean mass: ${result.map { it.kind }}", result.contains(ScreenshotValue("lean_mass", "60.53")))
        assertTrue("Missing body water: ${result.map { it.kind }}", result.contains(ScreenshotValue("body_water_percent", "58.2")))
        assertTrue(result.contains(ScreenshotValue("kg", "69.9")))
        assertFalse(result.contains(ScreenshotValue("lean_mass", "44.1")))
        save("screenshot-ocr-fixture.png", bitmap)
    }

    @Test fun b_importReviewShowsAllBodyMetricsWithConsistentFieldsAndFixedActions() {
        val values = listOf(ScreenshotValue("bmi", "23.6"), ScreenshotValue("kg", "69.9"),
            ScreenshotValue("body_fat", "13.4"), ScreenshotValue("lean_mass", "60.53"), ScreenshotValue("body_water_percent", "58.2"))
        compose.setContent {
            OpenVitalsTheme(themeMode = tech.mmarca.openvitals.domain.preferences.AppThemeMode.AMOLED) {
                Surface(Modifier.fillMaxSize()) {
                    ScreenshotImportContent(ScreenshotImportState(values = values), "2026-11-13T23:22:16", "2026-11-13T23:22:16", false, ScreenshotImportActions())
                }
            }
        }
        compose.onNodeWithTag("screenshot_value_lean_mass").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("screenshot_value_body_water_percent").performScrollTo().assertIsDisplayed()
        save("screenshot-import-form.png", compose.onRoot().captureToImage().asAndroidBitmap())
        compose.onNodeWithTag("screenshot_write_permissions").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Confirm and save").assertIsDisplayed().assertIsNotEnabled()
        save("screenshot-import-actions.png", compose.onRoot().captureToImage().asAndroidBitmap())
    }

    @Test fun c_missingAppPermissionPageDoesNotFallBackToGenericHealthConnect() {
        val attempted = mutableListOf<Intent>()
        val context = object : ContextWrapper(instrumentation.targetContext) {
            override fun startActivity(intent: Intent) { attempted += intent; throw ActivityNotFoundException() }
        }
        assertFalse(openOwnHealthConnectPermissionSettings(context))
        assertEquals(1, attempted.size)
        assertEquals(instrumentation.targetContext.packageName, attempted.single().getStringExtra(Intent.EXTRA_PACKAGE_NAME))
        assertEquals(0, attempted.single().flags and Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
    }

    @Test fun d_opensThisOpenVitalsInstallInsideHealthConnect() {
        val app = instrumentation.targetContext
        instrumentation.uiAutomation.executeShellCommand("pm grant ${app.packageName} android.permission.health.WRITE_WEIGHT").use {
            android.os.ParcelFileDescriptor.AutoCloseInputStream(it).readBytes()
        }
        var launched = false
        compose.setContent {
            val context = LocalContext.current
            Button(onClick = { launched = openOwnHealthConnectPermissionSettings(context) }) { Text("Open this app permissions") }
        }
        compose.onNodeWithText("Open this app permissions").performClick()
        assertTrue(launched)
        val label = app.applicationInfo.loadLabel(app.packageManager).toString()
        val deadline = System.currentTimeMillis() + 20_000
        var text = ""
        while (System.currentTimeMillis() < deadline) {
            val root = instrumentation.uiAutomation.rootInActiveWindow
            text = texts(root).joinToString("\n")
            if (text.contains(label, true) && text.contains("Weight", true)) break
            click(root, setOf("Get started", "Continue", "Done", "Allow all"))
            Thread.sleep(250)
        }
        save("screenshot-openvitals-health-permissions.png", checkNotNull(instrumentation.uiAutomation.takeScreenshot()))
        assertTrue("Expected OpenVitals app access page, found: $text", text.contains(label, true) && text.contains("Weight", true))
    }

    private fun texts(node: AccessibilityNodeInfo?): List<String> = if (node == null) emptyList() else
        listOfNotNull(node.text?.toString()) + (0 until node.childCount).flatMap { texts(node.getChild(it)) }

    private fun click(node: AccessibilityNodeInfo?, choices: Set<String>): Boolean {
        if (node == null) return false
        if (node.text?.toString() in choices) {
            var target: AccessibilityNodeInfo? = node
            repeat(4) {
                if (target?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true) return true
                target = target?.parent
            }
        }
        return (0 until node.childCount).any { click(node.getChild(it), choices) }
    }

    private fun save(name: String, bitmap: Bitmap) {
        File(instrumentation.targetContext.getExternalFilesDir(null), name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
