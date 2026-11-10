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
    }.distinct()
}
