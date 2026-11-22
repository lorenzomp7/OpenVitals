package tech.mmarca.openvitals.ui.components

/**
 * 100: the opening greeting comes and goes like a cathode-ray television. It switches on as a
 * bright line that widens from the centre and then opens upwards and downwards; it switches off
 * by closing from the top and the bottom onto that line, which then shrinks to a dot and goes out.
 *
 * [scaleX] and [scaleY] squeeze the lettering about its centre, [beam] is the white of the
 * electron beam over the squeezed line (0 to 1), [alpha] the lettering's opacity and [presence]
 * how much of the pill the greeting holds: the toggle's wave shows through by `1 - presence`.
 */
internal data class GreetingCrtFrame(
    val scaleX: Float,
    val scaleY: Float,
    val beam: Float,
    val alpha: Float,
    val presence: Float,
)

/** The scan line's height as a share of the lettering's. */
internal const val GreetingCrtLine = 0.04f

internal const val GreetingCrtOnStart = 0.3f
internal const val GreetingCrtOnEnd = 1.0f
internal const val GreetingCrtOffStart = 2.9f
internal const val GreetingCrtOffEnd = 3.5f

/** The greeting at [seconds] after it began; null while it is not on the screen. */
internal fun greetingCrtAt(seconds: Float): GreetingCrtFrame? = when {
    seconds < GreetingCrtOnStart -> null
    seconds < GreetingCrtOnEnd -> {
        val p = (seconds - GreetingCrtOnStart) / (GreetingCrtOnEnd - GreetingCrtOnStart)
        if (p < 0.35f) {
            // A dot of light stretches into a line across the pill.
            val q = easeOut(p / 0.35f)
            GreetingCrtFrame(0.02f + 0.98f * q, GreetingCrtLine, beam = 1f, alpha = 1f, presence = q)
        } else {
            // The line opens upwards and downwards into the lettering; the beam fades as it opens.
            val q = easeOut((p - 0.35f) / 0.65f)
            GreetingCrtFrame(1f, GreetingCrtLine + (1f - GreetingCrtLine) * q, beam = (1f - q) * (1f - q), alpha = 1f, presence = 1f)
        }
    }
    seconds < GreetingCrtOffStart -> GreetingCrtFrame(1f, 1f, beam = 0f, alpha = 1f, presence = 1f)
    seconds < GreetingCrtOffEnd -> {
        val p = (seconds - GreetingCrtOffStart) / (GreetingCrtOffEnd - GreetingCrtOffStart)
        if (p < 0.55f) {
            // Top and bottom close onto the centre line, which brightens as it thins.
            val q = easeIn(p / 0.55f)
            GreetingCrtFrame(1f, 1f - (1f - GreetingCrtLine) * q, beam = q, alpha = 1f, presence = 1f)
        } else {
            // The line shrinks to a dot and goes out.
            val q = easeIn((p - 0.55f) / 0.45f)
            GreetingCrtFrame(1f - 0.98f * q, GreetingCrtLine, beam = 1f, alpha = 1f - q * q, presence = 1f - q)
        }
    }
    else -> null
}

private fun easeOut(t: Float): Float = 1f - (1f - t.coerceIn(0f, 1f)).let { it * it * it }

private fun easeIn(t: Float): Float = t.coerceIn(0f, 1f).let { it * it * it }
