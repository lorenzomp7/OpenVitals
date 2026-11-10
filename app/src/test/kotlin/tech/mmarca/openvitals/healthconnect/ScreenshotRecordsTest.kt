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
}
