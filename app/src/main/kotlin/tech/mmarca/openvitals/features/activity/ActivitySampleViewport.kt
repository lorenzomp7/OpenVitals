package tech.mmarca.openvitals.features.activity

import tech.mmarca.openvitals.ui.components.ChartViewport

/** Focus sparse recordings without changing their timestamps or filling missing data. */
internal fun activitySampleViewport(fractions: List<Float>): ChartViewport {
    val valid = fractions.filter { it.isFinite() && it in 0f..1f }
    if (valid.size < 2) return ChartViewport.Full
    val first = valid.min()
    val last = valid.max()
    val span = last - first
    if (span <= 0f || span >= 0.25f) return ChartViewport.Full
    val width = (span * 1.2f).coerceAtLeast(ChartViewport.MinimumSpan).coerceAtMost(1f)
    val start = ((first + last - width) / 2f).coerceIn(0f, 1f - width)
    return ChartViewport(start, start + width)
}
