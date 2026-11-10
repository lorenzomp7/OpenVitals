package tech.mmarca.openvitals.testing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.domain.preferences.AppThemeMode
import tech.mmarca.openvitals.features.dashboard.DashboardContent
import tech.mmarca.openvitals.features.dashboard.DashboardDailyGoals
import tech.mmarca.openvitals.features.dashboard.DashboardPresentationMapper
import tech.mmarca.openvitals.features.dashboard.DashboardWidgetId
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * Photographs of the personal build's dashboard, on sample data, for review
 * before an APK is installed. Run in record mode: the pictures land with the
 * goldens on the device.
 */
class PersonalScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun dashboardLight() = photograph("personal_dashboard_light", isEditing = false, theme = AppThemeMode.LIGHT)

    @Test
    fun dashboardDark() = photograph("personal_dashboard_dark", isEditing = false)

    @Test
    fun dashboardDarkEditing() = photograph("personal_dashboard_dark_editing", isEditing = true)

    @Test
    fun dashboardDarkSizes() = photograph("personal_dashboard_dark_sizes", isEditing = false, sizes = mixedSizes)

    @Test
    fun dashboardDarkPaged() =
        photograph("personal_dashboard_dark_paged", isEditing = false, sizes = mixedSizes, paged = true)

    @Test
    fun dashboardDarkEditingSizes() =
        photograph("personal_dashboard_dark_editing_sizes", isEditing = true, sizes = mixedSizes)

    @Test
    fun dashboardDarkSixTiles() = photograph(
        "personal_dashboard_dark_six_tiles", isEditing = false,
        sizes = personalWidgetIds.associateWith { 2 }, paged = true,
    )

    private fun photograph(
        name: String,
        isEditing: Boolean,
        sizes: Map<DashboardWidgetId, Int> = emptyMap(),
        paged: Boolean = false,
        theme: AppThemeMode = AppThemeMode.DARK,
    ) {
        val unitFormatter = testUnitFormatter()
        val dateTimeFormatterProvider = DateTimeFormatterProvider()
        val data = dashboardFixtureData()
        val display = DashboardPresentationMapper.build(
            data = data,
            dailyGoals = DashboardDailyGoals(),
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
        )

        composeRule.setContent {
            PhoneSurface(theme) {
                DashboardContent(
                    data = data,
                    display = display,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    canGoForward = false,
                    isRefreshing = false,
                    dashboardWidgets = personalWidgetIds,
                    isEditingDashboard = isEditing,
                    widgetSizes = sizes,
                    pagedLayout = paged,
                    onPreviousDay = {},
                    onNextDay = {},
                    onOpenCalendar = {},
                    onMoveWidgetToTarget = { _, _ -> },
                    onRemoveWidget = {},
                    onAddWidget = {},
                    onOpenMetric = {},
                    onOpenActivities = {},
                    onOpenActivity = {},
                    onEditActivity = {},
                    onDeleteActivity = {},
                    onOpenLog = {},
                    onStartActivity = {},
                    onToggleDashboardEdit = {},
                )
            }
        }

        composeRule.assertVisualRootMatchesGolden(name)
    }
}

private val personalWidgetIds = listOf(
    DashboardWidgetId.STEPS,
    DashboardWidgetId.WEEKLY_CARDIO_LOAD,
    DashboardWidgetId.DISTANCE,
    DashboardWidgetId.CALORIES_OUT,
    DashboardWidgetId.AVG_HEART_RATE,
    DashboardWidgetId.WEIGHT,
    DashboardWidgetId.RESTING_HEART_RATE,
    DashboardWidgetId.HYDRATION,
    DashboardWidgetId.BODY_FAT,
    DashboardWidgetId.SPO2,
    DashboardWidgetId.ACTIVE_CALORIES,
    DashboardWidgetId.FLOORS,
    DashboardWidgetId.ELEVATION,
)

/** One tile of four spaces, a run of one-space tiles, the rest at two. */
private val mixedSizes = mapOf(
    DashboardWidgetId.CALORIES_OUT to 4,
    DashboardWidgetId.AVG_HEART_RATE to 1,
    DashboardWidgetId.WEIGHT to 1,
    DashboardWidgetId.RESTING_HEART_RATE to 1,
    DashboardWidgetId.HYDRATION to 1,
)

/** A Pixel-sized dark screen, at a phone's density. */
@Composable
private fun PhoneSurface(theme: AppThemeMode, content: @Composable () -> Unit) {
    OpenVitalsTheme(themeMode = theme, dynamicColor = false) {
        CompositionLocalProvider(
            LocalDensity provides Density(density = 2.625f, fontScale = 1f),
            LocalContentColor provides MaterialTheme.colorScheme.onBackground,
        ) {
            Box(
                modifier = Modifier
                    .requiredSize(411.dp, 880.dp)
                    .background(MaterialTheme.colorScheme.background)
                    .testTag(OpenVitalsVisualRootTag),
            ) {
                content()
            }
        }
    }
}
