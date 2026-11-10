package tech.mmarca.openvitals.features.imports.screenshot

import org.junit.Assert.*
import org.junit.Test

class ScreenshotParserTest {
    @Test fun `recognizes Italian decimals and grouped steps without importing timestamps`() {
        val values = ScreenshotParser.parse("10/11/2026 15:11 75,4 kg 10.235 passi 4,25 km 480 kcal 72 bpm")
        assertEquals(listOf(ScreenshotValue("kg", "75.4"), ScreenshotValue("steps", "10235"),
            ScreenshotValue("km", "4.25"), ScreenshotValue("kcal", "480"), ScreenshotValue("bpm", "72")), values)
    }
    @Test fun `ignores unsupported numbers and duplicate OCR labels`() {
        assertTrue(ScreenshotParser.parse("2026 15:11 69% 4G 13 of 13 selected").isEmpty())
        assertEquals(listOf(ScreenshotValue("kg", "75.4")), ScreenshotParser.parse("75.4 kg\n75.4 kg"))
    }
}
