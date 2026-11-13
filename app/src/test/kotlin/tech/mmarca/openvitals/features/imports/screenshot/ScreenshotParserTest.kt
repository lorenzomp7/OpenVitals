package tech.mmarca.openvitals.features.imports.screenshot

import org.junit.Assert.*
import org.junit.Test

class ScreenshotParserTest {
    @Test fun `recognizes Italian decimals and grouped steps without importing timestamps`() {
        val values = ScreenshotParser.parse("10/11/2026 15:11 Peso 75,4 kg 10.235 passi 4,25 km 480 kcal 72 bpm")
        assertEquals(setOf(ScreenshotValue("kg", "75.4"), ScreenshotValue("steps", "10235"),
            ScreenshotValue("km", "4.25"), ScreenshotValue("kcal", "480"), ScreenshotValue("bpm", "72")), values.toSet())
    }
    @Test fun `ignores unsupported numbers and duplicate OCR labels`() {
        assertTrue(ScreenshotParser.parse("2026 15:11 69% 4G 13 of 13 selected").isEmpty())
        assertTrue(ScreenshotParser.parse("75.4 kg\n75.4 kg").isEmpty())
    }
    @Test fun `extracts Pixel and Samsung screenshot timestamps and rejects invalid dates`() {
        val expected = java.time.LocalDateTime.of(2026, 11, 10, 14, 30, 25)
        assertEquals(expected, ScreenshotParser.filenameTime("Screenshot_20261110-143025.png"))
        assertEquals(expected, ScreenshotParser.filenameTime("Screenshot_2026-11-10-14-30-25-123_com.zepp.jpg"))
        assertNull(ScreenshotParser.filenameTime("Screenshot_20260230-143025.png"))
        assertNull(ScreenshotParser.filenameTime("37446.png"))
        assertEquals(java.time.Instant.ofEpochMilli(1794318592415).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime(),
            ScreenshotParser.filenameTime("1794318592415.png"))
    }
    @Test fun `duration requires a label and preserves seconds`() {
        assertEquals("45.5", ScreenshotParser.parse("Durata 45:30").single().value)
        assertEquals("80.0", ScreenshotParser.parse("Duration 1 h 20 min").single().value)
        assertEquals("80.5", ScreenshotParser.parse("Durata allenamento 1:20:30").single().value)
        assertTrue(ScreenshotParser.parse("14:30\nPasso 5:20 /km").isEmpty())
        assertTrue(ScreenshotParser.parse("Durata 45:99").isEmpty())
    }
    @Test fun `recognizes specific workout labels and leaves unknown types for review`() {
        val labels = mapOf("Corsa" to 1, "Corsa su tapis roulant" to 2, "Camminata" to 3)
        assertEquals(1, ScreenshotParser.workoutType("CORSA\nDurata 45:30", labels))
        assertEquals(2, ScreenshotParser.workoutType("Corsa su tapis roulant\n45:30", labels))
        assertNull(ScreenshotParser.workoutType("Corso di cucina", labels))
        assertNull(ScreenshotParser.workoutType("Attività sconosciuta", labels))
    }
    @Test fun `body tiles keep weight separate from fat and muscle masses`() {
        val lines = listOf(
            ScreenshotOcrLine("WEIGHT", 50, 50, 150, 80),
            ScreenshotOcrLine("BMI", 300, 50, 350, 80),
            ScreenshotOcrLine("70.0 kg", 50, 110, 170, 140),
            ScreenshotOcrLine("23.4", 300, 110, 350, 140),
            ScreenshotOcrLine("BODY FAT %", 40, 210, 180, 240),
            ScreenshotOcrLine("BODY FAT MASS", 280, 210, 450, 240),
            ScreenshotOcrLine("18.0 %", 50, 260, 150, 290),
            ScreenshotOcrLine("12.6 kg", 300, 260, 400, 290),
            ScreenshotOcrLine("MUSCLE MASS", 50, 350, 190, 380),
            ScreenshotOcrLine("50.0 kg", 50, 400, 150, 430),
        )
        assertEquals(setOf(ScreenshotValue("kg", "70.0"), ScreenshotValue("body_fat", "18.0"), ScreenshotValue("bmi", "23.4")),
            ScreenshotParser.parseLines(lines).toSet())
    }
    @Test fun `body composition labels do not become weight or workout calories`() {
        val values = ScreenshotParser.parse("BMI 23.4 BMR 1700 kcal Lean mass 55.2 kg Body water 40.1 kg Bone mass 3.1 kg")
        assertEquals(setOf("bmi", "bmr", "lean_mass", "body_water", "bone_mass"), values.map { it.kind }.toSet())
        assertEquals("1700", values.single { it.kind == "bmr" }.value)
    }

    @Test fun `column matched BMR does not become workout calories`() {
        val values = ScreenshotParser.parseLines(listOf(
            ScreenshotOcrLine("BMR", 0, 0, 100, 30),
            ScreenshotOcrLine("LEAN MASS", 300, 0, 450, 30),
            ScreenshotOcrLine("1700 kcal", 0, 40, 100, 70),
            ScreenshotOcrLine("55.2 kg", 300, 40, 450, 70),
        ))
        assertEquals(setOf(ScreenshotValue("bmr", "1700"), ScreenshotValue("lean_mass", "55.2")), values.toSet())
    }

    @Test fun `recognizes lean mass and body water above labels and on inline tiles`() {
        val result = ScreenshotParser.parseLines(listOf(
            ScreenshotOcrLine("55.2 kg", 0, 0, 100, 30),
            ScreenshotOcrLine("LEAN MASS", 0, 40, 100, 70),
            ScreenshotOcrLine("60.0 %", 300, 0, 410, 30),
            ScreenshotOcrLine("BODY WATER", 300, 40, 410, 70),
            ScreenshotOcrLine("Weight 70.0 kg", 0, 120, 120, 150),
        ))
        assertTrue(result.contains(ScreenshotValue("lean_mass", "55.2")))
        assertTrue(result.contains(ScreenshotValue("body_water_percent", "60.0")))
        assertTrue(result.contains(ScreenshotValue("kg", "70.0")))
        assertEquals("55.2", ScreenshotParser.parse("Fat-free mass 55.2 kg").single().value)
    }
}
