package tech.mmarca.openvitals.healthconnect

import androidx.health.connect.client.records.StepsRecord
import org.junit.Assert.*
import org.junit.Test

class ScreenshotRecordsTest {
    @Test fun `repeat import uses the same identity and preserves totals`() {
        val values = listOf("steps" to "10235")
        val a = screenshotRecords(values, "2025-01-01T00:00", "2025-01-02T00:00").single() as StepsRecord
        val b = screenshotRecords(values, "2025-01-01T00:00", "2025-01-02T00:00").single()
        assertEquals(10235L, a.count)
        assertEquals(a.metadata.clientRecordId, b.metadata.clientRecordId)
    }
    @Test(expected = IllegalArgumentException::class)
    fun `rejects intervals with zero duration`() {
        screenshotRecords(listOf("steps" to "100"), "2025-01-01T00:00", "2025-01-01T00:00")
    }
    @Test(expected = IllegalArgumentException::class)
    fun `rejects fractional steps`() {
        screenshotRecords(listOf("steps" to "10.5"), "2025-01-01T00:00", "2025-01-02T00:00")
    }
    @Test fun `duration writes an idempotent generic exercise session`() {
        val values = listOf("duration_min" to "45.5")
        val a = screenshotRecords(values, "2025-01-01T10:00", "2025-01-01T10:45:30").single()
            as androidx.health.connect.client.records.ExerciseSessionRecord
        assertEquals(2730L, java.time.Duration.between(a.startTime, a.endTime).seconds)
        assertEquals(a.metadata.clientRecordId,
            screenshotRecords(values, "2025-01-01T10:00", "2025-01-01T10:45:30").single().metadata.clientRecordId)
    }
    @Test(expected = IllegalArgumentException::class)
    fun `rejects duration inconsistent with confirmed interval`() {
        screenshotRecords(listOf("duration_min" to "45"), "2025-01-01T10:00", "2025-01-01T11:00")
    }
    @Test fun `preserves confirmed running type in imported workout`() {
        val type = androidx.health.connect.client.records.ExerciseSessionRecord.EXERCISE_TYPE_RUNNING
        val record = screenshotRecords(listOf("duration_min" to "45"),
            "2025-01-01T10:00", "2025-01-01T10:45", type).single()
            as androidx.health.connect.client.records.ExerciseSessionRecord
        assertEquals(type, record.exerciseType)
    }
    @Test fun `body fat is stored as percentage and never as body weight`() {
        val record = screenshotRecords(listOf("body_fat" to "18.0"),
            "2025-01-01T10:00", "2025-01-01T10:00").single()
        assertTrue(record is androidx.health.connect.client.records.BodyFatRecord)
        assertEquals(18.0, (record as androidx.health.connect.client.records.BodyFatRecord).percentage.value, 0.0)
    }
}
