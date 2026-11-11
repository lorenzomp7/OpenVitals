package tech.mmarca.openvitals.data.repository

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import tech.mmarca.openvitals.core.period.PeriodRangePreferenceKey
import tech.mmarca.openvitals.domain.insights.MetricDailyGoalKey
import tech.mmarca.openvitals.domain.preferences.UnitQuantity

class SettingsBackupCodecTest {
    private val main = "openvitals_prefs"

    @Test fun roundTripPreservesTypesAndLayouts() {
        val values = mapOf(
            "dynamic_color" to true,
            "mindfulness_timer_duration_minutes" to 25,
            "body_profile_weight_kg" to 73.5f,
            "hydration_container_volume_milliliters" to setOf("250", "500"),
            "dashboard_widget_sizes" to "STEPS=4,SLEEP=2",
            "dashboard_widget_details" to "STEPS=3,SLEEP=2",
            "activity_recording_dashboard_layout_cycling" to "distance=2|time=1",
        )
        val cycle = mapOf("pill_updated_at_millis" to 1_730_000_000_123L)
        val snapshot = mapOf(main to values, "openvitals_cycle_preferences" to cycle,
            "openvitals_user_profile" to mapOf("name" to "Lori", "age" to 30))
        assertEquals(snapshot, SettingsBackupCodec.decode(SettingsBackupCodec.encode(snapshot)))
    }

    @Test fun everyDailyGoalPeriodAndUnitOverrideIsIncluded() {
        MetricDailyGoalKey.entries.forEach { assertEquals("Float", SettingsBackupSchema.type(main, it.storageKey)) }
        PeriodRangePreferenceKey.entries.forEach { assertEquals("String", SettingsBackupSchema.type(main, it.storageKey)) }
        UnitQuantity.entries.forEach { assertEquals("String", SettingsBackupSchema.type(main, "unit_override_" + it.storageKey)) }
    }

    @Test fun excludesRuntimePermissionAndCredentialState() {
        val text = SettingsBackupCodec.encode(mapOf(main to mapOf(
            "dynamic_color" to true,
            "health_connect_permission_cancel_count" to 99,
            "body_energy_chain_seed_mirror" to "private sample",
            "step_distance_purge_pending" to true,
            "token" to "private credential",
        )))
        assertFalse(text.contains("private"))
        assertFalse(text.contains("purge_pending"))
        assertEquals(mapOf(main to mapOf("dynamic_color" to true)), SettingsBackupCodec.decode(text))
        assertNull(SettingsBackupSchema.type("activity_recording", "status"))
        assertNull(SettingsBackupSchema.type("openwatch_link", "profile_signature"))
    }

    @Test fun rejectsForeignFileAndFutureVersion() {
        assertThrows(Exception::class.java) { SettingsBackupCodec.decode("{\"format\":\"other-app\",\"version\":1,\"settings\":{}}") }
        val future = document("dynamic_color", "Boolean", true).put("version", 2)
        assertThrows(Exception::class.java) { SettingsBackupCodec.decode(future.toString()) }
    }

    @Test fun rejectsIncorrectPreferenceTypeRatherThanCrashingReadersLater() {
        val wrongType = document("dynamic_color", "String", "true")
        assertThrows(Exception::class.java) { SettingsBackupCodec.decode(wrongType.toString()) }
        val stringBoolean = document("dynamic_color", "Boolean", "true")
        assertThrows(Exception::class.java) { SettingsBackupCodec.decode(stringBoolean.toString()) }
    }

    @Test fun rejectsOverflowAndFractionalIntegers() {
        listOf(2147483648L, -2147483649L, 1.5).forEach { bad ->
            assertThrows(Exception::class.java) { SettingsBackupCodec.decode(document("home_widget_refresh_minutes", "Int", bad).toString()) }
        }
        assertThrows(Exception::class.java) {
            SettingsBackupCodec.decode(document("body_profile_weight_kg", "Float", 1e100).toString())
        }
    }

    @Test fun rejectsUnknownStoreUnknownKeyAndNonStringSetEntries() {
        val unknownStore = document("dynamic_color", "Boolean", true)
        unknownStore.getJSONObject("settings").put("../../another-file", JSONObject())
        assertThrows(Exception::class.java) { SettingsBackupCodec.decode(unknownStore.toString()) }
        assertThrows(Exception::class.java) { SettingsBackupCodec.decode(document("unknown_setting", "String", "x").toString()) }
        assertThrows(Exception::class.java) {
            SettingsBackupCodec.decode(document("hydration_container_volume_milliliters", "StringSet", org.json.JSONArray().put(250)).toString())
        }
    }

    @Test fun rejectsOversizedFiles() {
        assertThrows(Exception::class.java) { SettingsBackupCodec.decode(" ".repeat(SettingsBackupCodec.MAX_BYTES + 1)) }
    }

    @Test fun defaultOnlyGroupsRemainPresentForRestoration() {
        val stores = SettingsBackupSchema.stores.keys.associateWith { emptyMap<String, Any>() }
        assertEquals(stores, SettingsBackupCodec.decode(SettingsBackupCodec.encode(stores)))
    }

    private fun document(key: String, type: String, value: Any) = JSONObject()
        .put("format", "openvitals-settings").put("version", 1)
        .put("settings", JSONObject().put(main, JSONObject().put(key, JSONObject().put("type", type).put("value", value))))
}
