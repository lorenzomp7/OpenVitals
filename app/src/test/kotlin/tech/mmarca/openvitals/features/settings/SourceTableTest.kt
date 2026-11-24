package tech.mmarca.openvitals.features.settings

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.model.SourceInventory
import tech.mmarca.openvitals.domain.model.SourceInventoryRow

class SourceTableTest {

    private val t1 = Instant.parse("2026-11-23T08:00:00Z")
    private val t2 = Instant.parse("2026-11-20T08:00:00Z")

    private val inventory = SourceInventory(
        windowDays = 30,
        rows = listOf(
            SourceInventoryRow(type = "heart rate", lastSeenBySource = mapOf(ZEPP to t1)),
            SourceInventoryRow(type = "sleep", lastSeenBySource = mapOf(ZEPP to t2)),
            SourceInventoryRow(type = "sleep", label = "sleep stages"),
            SourceInventoryRow(type = "oxygen saturation"),
            SourceInventoryRow(type = "nutrition"),
            SourceInventoryRow(type = "weight", readable = false),
        ),
    )

    private val declarations = mapOf(
        ZEPP to setOf(
            "android.permission.health.WRITE_HEART_RATE",
            "android.permission.health.WRITE_SLEEP",
            "android.permission.health.WRITE_OXYGEN_SATURATION",
        ),
        GADGETBRIDGE to setOf("android.permission.health.WRITE_HEART_RATE"),
        // Declares nothing the table can read: no column.
        "com.example.scale" to setOf("android.permission.health.WRITE_WEIGHT"),
    )

    @Test
    fun `cells tell seen, declared but not seen, and neither apart`() {
        val table = buildSourceTable(inventory, declarations)

        assertEquals(listOf(ZEPP, GADGETBRIDGE), table.sources)
        val rows = table.rows.associateBy { it.label }
        assertEquals(listOf(SourceTableCell.Seen(t1), SourceTableCell.DeclaredNotSeen), rows.getValue("heart rate").cells)
        // Zepp declares sleep, sent sessions, but none with stages.
        assertEquals(listOf(SourceTableCell.DeclaredNotSeen, SourceTableCell.None), rows.getValue("sleep stages").cells)
        assertEquals(listOf(SourceTableCell.DeclaredNotSeen, SourceTableCell.None), rows.getValue("oxygen saturation").cells)
        assertEquals(3, table.declaredNotSeenCount)
    }

    @Test
    fun `a type nobody sent or declared is not a row, an unreadable one is listed apart`() {
        val table = buildSourceTable(inventory, declarations)

        assertFalse(table.rows.any { it.label == "nutrition" })
        assertFalse(table.rows.any { it.label == "weight" })
        assertEquals(listOf("weight"), table.unreadableTypes)
    }

    @Test
    fun `the Zepp note shows only with a Zepp source`() {
        assertTrue(buildSourceTable(inventory, declarations).showsZeppNote)
        val withoutZepp = SourceInventory(
            windowDays = 30,
            rows = listOf(SourceInventoryRow(type = "steps", lastSeenBySource = mapOf(GADGETBRIDGE to t1))),
        )
        assertFalse(buildSourceTable(withoutZepp, emptyMap()).showsZeppNote)
    }

    @Test
    fun `declared writers are grouped by type for the targeted read`() {
        assertEquals(
            mapOf(
                "heart rate" to setOf(ZEPP, GADGETBRIDGE),
                "sleep" to setOf(ZEPP),
                "oxygen saturation" to setOf(ZEPP),
                "weight" to setOf("com.example.scale"),
            ),
            declaredWritersByType(declarations + ("com.other.phone" to null)),
        )
    }

    private companion object {
        const val ZEPP = "com.huami.watch.hmwatchmanager"
        const val GADGETBRIDGE = "nodomain.freeyourgadget.gadgetbridge"
    }
}
