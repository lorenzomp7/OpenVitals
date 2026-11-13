package tech.mmarca.openvitals.features.imports.screenshot

internal data class ScreenshotValue(val kind: String, val value: String)

/** Only explicit units/labels produce suggestions; dates and times require user confirmation. */
internal object ScreenshotParser {
    private val patterns = linkedMapOf(
        "kg" to Regex("""(?i)(\d{1,3}(?:[.,]\d{1,2})?)\s*kg\b"""),
        "steps" to Regex("""(?i)(\d[\d., ]*)\s*(?:steps|passi)\b"""),
        "km" to Regex("""(?i)(\d+(?:[.,]\d+)?)\s*km\b"""),
        "kcal" to Regex("""(?i)(\d+(?:[.,]\d+)?)\s*kcal\b"""),
        "bpm" to Regex("""(?i)(\d{2,3})\s*bpm\b"""),
    )
    fun parse(text: String): List<ScreenshotValue> = patterns.flatMap { (kind, regex) ->
        regex.findAll(text).map { match ->
            val raw = match.groupValues[1].trim()
            ScreenshotValue(kind, if (kind == "steps") raw.replace(Regex("[., ]"), "") else raw.replace(',', '.'))
        }.toList()
    }.filterNot { it.kind == "kg" || (it.kind == "kcal" && extraBodyValues(text).any { v -> v.kind == "bmr" && v.value == it.value }) }.distinct() + labeledBodyValues(text) +
        listOfNotNull(durationMinutes(text)?.let { ScreenshotValue("duration_min", it.toString()) })

    private fun labeledBodyValues(text: String) = ScreenshotBodyParser.parseText(text)
    private fun extraBodyValues(text: String) = ScreenshotBodyParser.parseText(text)

    fun parseLines(lines: List<ScreenshotOcrLine>): List<ScreenshotValue> {
        val body = ScreenshotBodyParser.parseLines(lines)
        val basal = body.filter { it.kind == "bmr" }.map { it.value }.toSet()
        val other = parse(lines.joinToString("\n") { it.text }).filterNot {
            it.kind in ScreenshotBodyParser.kinds || (it.kind == "kcal" && it.value in basal)
        }
        return (body + other).distinct()
    }

    /** Require a duration label: an unlabelled clock could be the phone status bar. */
    private fun durationMinutes(text: String): Double? {
        val label = "(?:durata(?:\\s+(?:allenamento|totale))?|duration|workout time|tempo(?:\\s+(?:totale|allenamento))?)"
        val clock = Regex("""(?i)$label\s*[:：]?\s*(\d{1,3}):(\d{2})(?::(\d{2}))?\b""").find(text)
        if (clock != null) {
            val a = clock.groupValues[1].toInt()
            val b = clock.groupValues[2].toInt()
            val c = clock.groupValues[3].toIntOrNull()
            if (b > 59 || (c != null && c > 59)) return null
            return if (c == null) a + b / 60.0 else a * 60.0 + b + c / 60.0
        }
        val units = Regex("""(?i)$label\s*[:：]?\s*(?:(\d+)\s*(?:h|ore?|hours?)\b\s*)?(?:(\d+(?:[.,]\d+)?)\s*(?:min(?:uti|utes?)?|m)\b\s*)?(?:(\d+)\s*(?:s|sec(?:ondi|onds?)?)\b)?""").find(text)
            ?: return null
        if (units.groupValues.drop(1).all { it.isBlank() }) return null
        return (units.groupValues[1].toDoubleOrNull() ?: 0.0) * 60 +
            (units.groupValues[2].replace(',', '.').toDoubleOrNull() ?: 0.0) +
            (units.groupValues[3].toDoubleOrNull() ?: 0.0) / 60
    }

    /** Screenshot_20261110-143025 and Screenshot_2026-11-10-14-30-25, including Pixel suffixes. */
    fun filenameTime(name: String): java.time.LocalDateTime? {
        val epoch = Regex("""^(1\d{12})(?:\.[A-Za-z0-9]+)?$""").matchEntire(name)
        if (epoch != null) return runCatching {
            java.time.Instant.ofEpochMilli(epoch.groupValues[1].toLong())
                .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
        }.getOrNull()
        val match = Regex("""(?<!\d)(20\d{2})[-_]?(\d{2})[-_]?(\d{2})[T _-]+(\d{2})[-_:]?(\d{2})[-_:]?(\d{2})(?!\d)""").find(name)
            ?: return null
        return runCatching {
            val parts = match.groupValues.drop(1).map(String::toInt)
            java.time.LocalDateTime.of(parts[0], parts[1], parts[2], parts[3], parts[4], parts[5])
        }.getOrNull()
    }
    /** Prefer the most specific label; equally specific competing types need manual selection. */
    fun workoutType(text: String, labels: Map<String, Int>): Int? {
        fun normalized(value: String) = value.lowercase(java.util.Locale.ROOT)
            .replace(Regex("""[^\p{L}\p{N}]+"""), " ").trim()
        val source = " ${normalized(text)} "
        val matches = labels.mapNotNull { (label, type) ->
            val key = normalized(label)
            if (key.isNotEmpty() && source.contains(" $key ")) key.length to type else null
        }
        val length = matches.maxOfOrNull { it.first } ?: return null
        return matches.filter { it.first == length }.map { it.second }.distinct().singleOrNull()
    }
}

internal data class ScreenshotOcrLine(val text: String, val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val centerX get() = (left + right) / 2.0
    val width get() = right - left
    val height get() = bottom - top
}
