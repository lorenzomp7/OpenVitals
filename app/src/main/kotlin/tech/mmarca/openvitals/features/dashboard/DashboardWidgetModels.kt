package tech.mmarca.openvitals.features.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class DashboardWidgetStyle {
    PILL,
    CIRCLE,
}

internal data class DashboardWidgetSpec(
    val id: DashboardWidgetId,
    val title: String,
    val style: DashboardWidgetStyle = DashboardWidgetStyle.PILL,
    /** What the widget shows as one ring of the multi-ring hero. */
    val hero: DashboardHeroRing? = null,
    val details: List<String> = emptyList(),
    val content: @Composable (Modifier) -> Unit,
)

/** One metric of the multi-ring hero: a headline number and how far round its ring goes. */
internal data class DashboardHeroRing(
    val title: String,
    val value: String,
    val fraction: Float,
    val accentColor: Color,
    val icon: ImageVector,
    val onClick: (() -> Unit)?,
)

internal fun DashboardWidgetDisplayModel.toHeroRing(
    title: String,
    accentColor: Color,
    icon: ImageVector,
    onClick: (() -> Unit)?,
): DashboardHeroRing {
    val cardio = weeklyCardioLoad
    val sleep = sleepScore
    return when {
        cardio != null -> DashboardHeroRing(title, "${cardio.progressPercent}%", cardio.progressFraction, accentColor, icon, onClick)
        sleep != null -> DashboardHeroRing(title, sleep.score.toString(), sleep.score / 100f, accentColor, icon, onClick)
        else -> DashboardHeroRing(
            title = title,
            // With its unit: "6.4" alone does not say kilometres.
            value = value?.takeIf { hasValue && !isLoading }?.text?.takeIf { it.isNotBlank() } ?: "–",
            fraction = progress?.fraction ?: 0f,
            accentColor = accentColor,
            icon = icon,
            onClick = onClick,
        )
    }
}

