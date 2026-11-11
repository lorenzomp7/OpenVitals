package tech.mmarca.openvitals.ui.components

/** Owned by the app shell, never by a navigation destination or a player instance. */
class FluidBrandTimeline(val initialStartMillis: Long = NotStarted) {
    var startedAtMillis: Long = initialStartMillis
        private set

    fun start(nowMillis: Long) {
        if (startedAtMillis == NotStarted) startedAtMillis = nowMillis
    }

    fun secondsAt(nowMillis: Long): Float =
        if (startedAtMillis == NotStarted) 0f
        else (nowMillis - startedAtMillis).coerceAtLeast(0L) / 1_000f

    fun fillAt(nowMillis: Long): Float {
        val fraction = (secondsAt(nowMillis) / FillSeconds).coerceIn(0f, 1f)
        return fraction * fraction * (3f - 2f * fraction)
    }

    companion object {
        const val NotStarted = -1L
        private const val FillSeconds = 2.8f
    }
}
