package tech.mmarca.openvitals.data.repository.contract

/** The order the user arranged the manual entry widgets and the metric detail sections in. */
interface WidgetOrderPreferences {

    /** Null before the first arrangement: the screen shows its default order. */
    fun manualEntryWidgetOrder(): List<String>?

    fun setManualEntryWidgetOrder(widgetIds: List<String>)

    /** Null before the first arrangement: the screen shows its default order. */
    fun metricDetailSectionOrder(): List<String>?

    fun setMetricDetailSectionOrder(sectionIds: List<String>)

    /** The sections the user took off the screens they appear on; shown again from edit mode. */
    fun hiddenMetricDetailSections(): Set<String> = emptySet()

    fun setHiddenMetricDetailSections(sectionIds: Set<String>) = Unit

    /** The version of the activity charts' general order the saved order was last put in. */
    fun activityDetailGeneralOrderVersion(): Int = Int.MAX_VALUE

    fun setActivityDetailGeneralOrderVersion(version: Int) = Unit
}
