package tech.mmarca.openvitals.features.settings

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthConnectSourcesTest {

    private val t1 = Instant.parse("2026-07-01T10:00:00Z")
    private val t2 = Instant.parse("2026-07-02T10:00:00Z")
    private val t3 = Instant.parse("2026-07-03T10:00:00Z")

    @Test
    fun `aggregates counts and metrics per package`() {
        val sources = aggregateHealthConnectSources(
            mapOf(
                "heart rate" to listOf(
                    "com.sec.android.app.shealth" to t1,
                    "com.sec.android.app.shealth" to t2,
                ),
                "sleep" to listOf(
                    "com.sec.android.app.shealth" to t3,
                ),
            ),
        )

        assertEquals(1, sources.size)
        val samsung = sources.single()
        assertEquals(3, samsung.recordCount)
        assertEquals(setOf("heart rate", "sleep"), samsung.metrics)
        assertEquals(t3, samsung.lastSeen)
        assertEquals("Samsung Health", samsung.displayName)
    }

    @Test
    fun `sorts most recent contributor first`() {
        val sources = aggregateHealthConnectSources(
            mapOf(
                "heart rate" to listOf(
                    "old.app" to t1,
                    "new.app" to t3,
                ),
            ),
        )

        assertEquals(listOf("new.app", "old.app"), sources.map { it.packageName })
        // Unrecognized packages fall back to the raw name.
        assertEquals("new.app", sources.first().displayName)
    }

    @Test
    fun `blank sources fold into the unknown bucket`() {
        val sources = aggregateHealthConnectSources(
            mapOf(
                "heart rate" to listOf(
                    "" to t1,
                    "   " to t2,
                ),
            ),
        )

        assertEquals(1, sources.size)
        assertEquals(HealthConnectSource.UNKNOWN_PACKAGE, sources.single().packageName)
        assertEquals("Unknown source", sources.single().displayName)
        assertEquals(2, sources.single().recordCount)
    }

    @Test
    fun `empty input yields an empty list`() {
        assertTrue(aggregateHealthConnectSources(emptyMap()).isEmpty())
        assertTrue(aggregateHealthConnectSources(mapOf("sleep" to emptyList())).isEmpty())
    }

    @Test
    fun `declared writes are named like the diagnostics metrics`() {
        assertEquals(
            setOf("heart rate", "sleep", "exercise route"),
            declaredHealthWrites(
                listOf(
                    "android.permission.health.WRITE_HEART_RATE",
                    "android.permission.health.WRITE_SLEEP",
                    "android.permission.health.WRITE_EXERCISE_ROUTE",
                    "android.permission.health.READ_STEPS",
                    "android.permission.INTERNET",
                ),
            ),
        )
    }

    @Test
    fun `a contributor carries its declared writes and what never arrived`() {
        val sources = aggregateHealthConnectSources(
            byMetric = mapOf(
                "heart rate" to listOf("com.huami.watch.hmwatchmanager" to t1),
                "sleep" to emptyList(),
            ),
            declarations = mapOf(
                "com.huami.watch.hmwatchmanager" to setOf(
                    "android.permission.health.WRITE_HEART_RATE",
                    "android.permission.health.WRITE_SLEEP",
                    "android.permission.health.WRITE_STEPS",
                ),
            ),
        )

        val zepp = sources.single()
        assertEquals(setOf("heart rate", "sleep", "steps"), zepp.declaredWrites)
        // Declares sleep, sent heart rate only: "writes it, nothing came".
        assertEquals(setOf("sleep"), zepp.declaredNotSeen(setOf("heart rate", "sleep")))
    }

    @Test
    fun `an app that declares a checked metric but sent none joins after the contributors`() {
        val sources = aggregateHealthConnectSources(
            byMetric = mapOf(
                "heart rate" to listOf("com.huami.watch.hmwatchmanager" to t1),
                "sleep" to emptyList(),
            ),
            declarations = mapOf(
                "nodomain.freeyourgadget.gadgetbridge" to setOf("android.permission.health.WRITE_SLEEP"),
                // Declares neither checked metric: not listed.
                "com.example.nutrition" to setOf("android.permission.health.WRITE_NUTRITION"),
            ),
        )

        assertEquals(
            listOf("com.huami.watch.hmwatchmanager", "nodomain.freeyourgadget.gadgetbridge"),
            sources.map { it.packageName },
        )
        val silent = sources.last()
        assertEquals(0, silent.recordCount)
        assertNull(silent.lastSeen)
        assertEquals(setOf("sleep"), silent.declaredNotSeen(setOf("heart rate", "sleep")))
    }

    @Test
    fun `a source not installed here has unknown declarations`() {
        val sources = aggregateHealthConnectSources(
            byMetric = mapOf("heart rate" to listOf("com.other.phone.app" to t1)),
            declarations = mapOf("com.other.phone.app" to null),
        )

        assertNull(sources.single().declaredWrites)
        assertTrue(sources.single().declaredNotSeen(setOf("heart rate", "sleep")).isEmpty())
    }
}
