package tech.mmarca.openvitals.features.imports.screenshot

import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** Body tiles may put label, value and unit in three different OCR lines. */
internal object ScreenshotBodyParser {
    private data class Metric(val kind: String, val aliases: List<String>, val units: Set<String>)
    private data class Label(val metric: Metric, val line: ScreenshotOcrLine, val unit: String?)
    private data class Number(val line: ScreenshotOcrLine, val value: String, val unit: String?)
    private val metrics = listOf(
        Metric("kg", listOf("weight", "peso", "peso corporeo"), setOf("kg")),
        Metric("bmi", listOf("BMI", "IMC"), setOf("")),
        Metric("body_fat", listOf("body fat", "body fat percentage", "body fat %", "grasso corporeo", "massa grassa %"), setOf("%")),
        Metric("lean_mass", listOf("lean mass", "lean body mass", "fat free mass", "massa magra"), setOf("kg")),
        Metric("body_water", listOf("body water", "body water mass", "total body water", "water", "acqua corporea"), setOf("kg", "%")),
        Metric("bone_mass", listOf("bone mass", "massa ossea"), setOf("kg")),
        Metric("bmr", listOf("BMR", "basal metabolic rate", "metabolismo basale"), setOf("kcal")),
    )
    val kinds = metrics.map { it.kind }.toSet() + "body_water_percent"
    private const val Numeric = "(\\d{1,4}(?:[.,]\\d+)?)"
    private const val Unit = "(kg|%|kcal(?:/(?:day|d|giorno))?)"
    private val numericLine = Regex("^$Numeric\\s*$Unit?$", RegexOption.IGNORE_CASE)
    private val unitLine = Regex("^$Unit$", RegexOption.IGNORE_CASE)
    private val suffixUnit = Regex("\\s*[(\\[]?\\s*$Unit\\s*[)\\]]?\\s*$", RegexOption.IGNORE_CASE)

    private fun normalized(text: String): String = text.lowercase(Locale.ROOT)
        .replace('％', '%').replace(Regex("[()\\[\\]:：_/-]"), " ")
        .replace(Regex("\\s+"), " ").trim()

    private fun canonicalUnit(text: String?): String? = text?.lowercase(Locale.ROOT)?.let {
        if (it.startsWith("kcal")) "kcal" else it
    }

    private fun value(metric: Metric, raw: String, unit: String?): ScreenshotValue? {
        val effectiveUnit = canonicalUnit(unit) ?: if (metric.kind == "bmi") "" else return null
        if (effectiveUnit !in metric.units) return null
        val kind = if (metric.kind == "body_water" && effectiveUnit == "%") "body_water_percent" else metric.kind
        return ScreenshotValue(kind, raw.replace(',', '.'))
    }

    fun parseText(text: String): List<ScreenshotValue> = metrics.flatMap { metric ->
        val aliases = metric.aliases.joinToString("|") { alias ->
            Regex.escape(alias).replace(" ", "\\E\\s+\\Q")
        }
        val prefix = Regex("(?i)(?<![\\p{L}])(?:$aliases)\\s*(?:[(\\[]\\s*$Unit\\s*[)\\]])?\\s*[:：]?\\s*$Numeric\\s*$Unit?(?![\\p{L}\\d])")
        prefix.findAll(text.replace('％', '%')).mapNotNull { match ->
            value(metric, match.groupValues[2], match.groupValues[3].ifBlank { match.groupValues[1] }.ifBlank { null })
        }.toList()
    }.distinct()

    fun parseLines(lines: List<ScreenshotOcrLine>): List<ScreenshotValue> {
        val expanded = lines + joinedLabels(lines)
        val labels = expanded.mapNotNull(::label)
        val numbers = lines.mapNotNull { line ->
            numericLine.matchEntire(line.text.trim().replace('％', '%'))?.let {
                Number(line, it.groupValues[1], it.groupValues[2].ifBlank { null })
            }
        }
        val units = lines.mapNotNull { line -> unitLine.matchEntire(line.text.trim().replace('％', '%'))?.let { line to it.groupValues[1] } }
        val spatial = labels.mapNotNull { label ->
            numbers.asSequence()
                .filter { nearby(label.line, it.line) && !crossesAnotherLabel(label, it.line, labels) }
                .mapNotNull { number ->
                    val unit = number.unit ?: label.unit ?: units.filter { (line, _) -> unitNear(number.line, line) }
                        .minByOrNull { (line, _) -> distance(number.line, line) }?.second
                    value(label.metric, number.value, unit)?.let { distance(label.line, number.line) to it }
                }
                .minByOrNull { it.first }?.second
        }
        // Inline values are parsed per physical line, never across neighbouring tiles.
        return (spatial + lines.flatMap { parseText(it.text) }).distinct()
    }

    private fun label(line: ScreenshotOcrLine): Label? {
        val raw = line.text.trim().replace('％', '%')
        val unit = suffixUnit.find(raw)?.groupValues?.get(1)
        val name = normalized(if (unit == null) raw else raw.replace(suffixUnit, "")).replace(" ", "")
        val metric = metrics.firstOrNull { metric -> metric.aliases.any { normalized(it).replace(" ", "") == name } } ?: return null
        return Label(metric, line, unit)
    }

    private fun joinedLabels(lines: List<ScreenshotOcrLine>): List<ScreenshotOcrLine> = buildList {
        val words = lines.filter { it.text.none(Char::isDigit) }
        words.forEach { first ->
            var joined = first
            repeat(2) {
                val next = words.filter { candidate ->
                    val height = max(joined.height, candidate.height)
                    candidate != first && (
                        (candidate.top >= joined.bottom && candidate.top - joined.bottom <= height &&
                            abs(candidate.centerX - joined.centerX) <= max(joined.width, candidate.width) * 0.75) ||
                        (candidate.left >= joined.right && candidate.left - joined.right <= height * 2 &&
                            min(candidate.bottom, joined.bottom) - max(candidate.top, joined.top) >= min(candidate.height, joined.height) / 2)
                    )
                }.minByOrNull { distance(joined, it) } ?: return@repeat
                joined = ScreenshotOcrLine(joined.text + " " + next.text,
                    min(joined.left, next.left), joined.top, max(joined.right, next.right), next.bottom)
                if (label(joined) != null) add(joined)
            }
        }
    }

    private fun nearby(label: ScreenshotOcrLine, number: ScreenshotOcrLine): Boolean {
        val height = max(label.height, number.height).coerceAtLeast(1)
        val sameColumn = abs(label.centerX - number.centerX) <= max(label.width, number.width) * 0.8 &&
            min(abs(number.top - label.bottom), abs(label.top - number.bottom)) <= height * 8
        val sameRow = min(label.bottom, number.bottom) - max(label.top, number.top) >= min(label.height, number.height) / 2 &&
            number.left >= label.right && number.left - label.right <= height * 18
        return sameColumn || sameRow
    }

    private fun unitNear(number: ScreenshotOcrLine, unit: ScreenshotOcrLine): Boolean {
        val height = max(number.height, unit.height).coerceAtLeast(1)
        val row = abs((number.top + number.bottom) - (unit.top + unit.bottom)) <= height &&
            unit.left >= number.right - height && unit.left - number.right <= height * 3
        val below = unit.top >= number.bottom && unit.top - number.bottom <= height &&
            abs(number.centerX - unit.centerX) <= max(number.width, unit.width)
        return row || below
    }

    private fun crossesAnotherLabel(label: Label, number: ScreenshotOcrLine, labels: List<Label>): Boolean = labels.any {
        it.metric.kind != label.metric.kind && abs(it.line.centerX - label.line.centerX) <= max(label.line.width, it.line.width) * 0.75 &&
            it.line.top > min(label.line.bottom, number.bottom) && it.line.bottom < max(label.line.top, number.top)
    }

    private fun distance(a: ScreenshotOcrLine, b: ScreenshotOcrLine): Double {
        val x = a.centerX - b.centerX
        val y = (a.top + a.bottom - b.top - b.bottom) / 2.0
        return x * x + y * y
    }
}
