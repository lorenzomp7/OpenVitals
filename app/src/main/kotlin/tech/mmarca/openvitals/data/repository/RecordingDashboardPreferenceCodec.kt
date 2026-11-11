package tech.mmarca.openvitals.data.repository

import tech.mmarca.openvitals.domain.preferences.ActivityRecordingDashboardField
import tech.mmarca.openvitals.domain.preferences.ActivityRecordingDashboardItemSize
import tech.mmarca.openvitals.domain.preferences.ActivityRecordingDashboardLayout
import tech.mmarca.openvitals.domain.preferences.ActivityRecordingDashboardTemplate

internal fun ActivityRecordingDashboardLayout.encodeRecordingDashboardLayout(): String {
    val normalized = normalized()
    return normalized.template.name + "|" +
        normalized.items.joinToString(",") { item ->
            item.field.name + "=" + item.size.toPreferenceString()
        }
}

internal fun String.decodeRecordingDashboardLayout(): ActivityRecordingDashboardLayout? {
    val sections = split("|", limit = 2)
    val template = sections.firstOrNull()
        ?.let { value ->
            runCatching { ActivityRecordingDashboardTemplate.valueOf(value) }.getOrNull()
        }
        ?: return null
    val items = sections.getOrNull(1)
        ?.split(",")
        ?.mapNotNull { value ->
            val itemSections = value.split("=", limit = 2)
            val field = itemSections.firstOrNull()
                ?.let { fieldName ->
                    runCatching { ActivityRecordingDashboardField.valueOf(fieldName) }.getOrNull()
                }
                ?: return@mapNotNull null
            val size = itemSections.getOrNull(1)
                ?.let { sizeName ->
                    ActivityRecordingDashboardItemSize.fromPreferenceString(sizeName)
                }
            field to size
        }
        .orEmpty()
    return ActivityRecordingDashboardLayout(
        template = template,
        fields = items.map { it.first },
        sizes = items.mapNotNull { (field, size) -> size?.let { field to it } }.toMap(),
    ).normalized()
}
