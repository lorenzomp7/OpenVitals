package tech.mmarca.openvitals.navigation

import androidx.compose.animation.ExitTransition
import androidx.compose.animation.EnterTransition
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.systemBars
import tech.mmarca.openvitals.ui.components.OpenVitalsTopBarHeight
import androidx.compose.animation.core.tween
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import java.time.LocalDate
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.features.achievements.AchievementsScreen
import tech.mmarca.openvitals.features.achievements.AchievementsViewModel
import tech.mmarca.openvitals.features.activity.ActivityDetailScreen
import tech.mmarca.openvitals.features.activity.ActivityDetailViewModel
import tech.mmarca.openvitals.features.activity.ActivityOverviewViewModel
import tech.mmarca.openvitals.features.activity.ActivitiesScreen
import tech.mmarca.openvitals.features.activity.ActivitiesViewModel
import tech.mmarca.openvitals.features.activity.CaloriesScreen
import tech.mmarca.openvitals.features.activity.CaloriesViewModel
import tech.mmarca.openvitals.features.activity.CardioLoadDetailScreen
import tech.mmarca.openvitals.features.bodyenergy.BodyEnergyDetailsScreen
import tech.mmarca.openvitals.features.bodyenergy.BodyEnergyViewModel
import tech.mmarca.openvitals.features.caffeine.CaffeineDrinkScreen
import tech.mmarca.openvitals.features.caffeine.CaffeineViewModel
import tech.mmarca.openvitals.features.dashboard.DashboardScreen
import tech.mmarca.openvitals.features.dashboard.DashboardViewModel
import tech.mmarca.openvitals.features.dashboard.DashboardWidgetId
import tech.mmarca.openvitals.features.heart.HeartViewModel
import tech.mmarca.openvitals.domain.preferences.AppThemeMode
import tech.mmarca.openvitals.domain.model.BodyMeasurementType
import tech.mmarca.openvitals.domain.model.VitalsMeasurementType
import tech.mmarca.openvitals.features.manualentry.body.titleRes
import tech.mmarca.openvitals.features.manualentry.vitals.titleRes
import tech.mmarca.openvitals.features.nutrition.NutritionScreen
import tech.mmarca.openvitals.features.nutrition.NutritionViewModel
import tech.mmarca.openvitals.features.onboarding.OnboardingScreen
import tech.mmarca.openvitals.features.onboarding.OnboardingViewModel
import tech.mmarca.openvitals.features.readiness.DailyReadinessViewModel
import tech.mmarca.openvitals.features.readiness.StressDetailsScreen
import tech.mmarca.openvitals.features.readiness.TrainingReadinessDetailsScreen
import tech.mmarca.openvitals.features.recovery.HeartRateRecoveryScreen
import tech.mmarca.openvitals.features.recovery.HeartRateRecoveryViewModel
import tech.mmarca.openvitals.features.recovery.RecoveryViewModel
import tech.mmarca.openvitals.features.recovery.SleepEfficiencyDetailScreen
import tech.mmarca.openvitals.features.recovery.SleepScoreDetailScreen
import tech.mmarca.openvitals.features.settings.OpenWatchMoveNoticeViewModel
import tech.mmarca.openvitals.features.sleep.SleepDetailScreen
import tech.mmarca.openvitals.features.sleep.SleepDetailViewModel
import tech.mmarca.openvitals.features.sleep.SleepScreen
import tech.mmarca.openvitals.features.sleep.SleepViewModel
import tech.mmarca.openvitals.ui.components.MetricAction
import tech.mmarca.openvitals.ui.components.OpenVitalsAdaptiveScaffold
import tech.mmarca.openvitals.ui.components.AmbientFluidBrand
import tech.mmarca.openvitals.ui.components.rememberFluidBrandTimeline
import tech.mmarca.openvitals.ui.components.OpenVitalsNavigationDestination
import tech.mmarca.openvitals.ui.components.OpenVitalsIconButton
import tech.mmarca.openvitals.ui.components.HealthConnectNewPermissionsPrompt
import tech.mmarca.openvitals.ui.components.LocalAppBarState
import tech.mmarca.openvitals.ui.components.OpenWatchMoveNoticePrompt
import tech.mmarca.openvitals.ui.components.PrivacyReconsentPrompt

private const val NavTransitionMillis = tech.mmarca.openvitals.ui.theme.ScreenMotion.DurationMillis

/**
 * 63: a screen opening fades and grows in over the one it leaves, which stays drawn underneath;
 * going back, the screen returned to is there at once and the one left fades away over it.
 * Every destination is opaque ([opaqueComposable]), so no frame is ever empty: the old fade
 * through emptied the screen between the two halves, a black page with only a title.
 */
private fun navOpenEnter(): EnterTransition = tech.mmarca.openvitals.ui.theme.ScreenMotion.openEnter()

/** The screen being covered stays as it is until the one opening has covered it. */
private fun navOpenExit(): ExitTransition = tech.mmarca.openvitals.ui.theme.ScreenMotion.openExit()

private fun navBackExit(): ExitTransition = tech.mmarca.openvitals.ui.theme.ScreenMotion.closeExit()

/** Destinations drawn without the app bar: the dashboard carries its own header. */
private fun hasAppBarRoute(entry: androidx.navigation.NavBackStackEntry?): Boolean {
    val route = entry?.destination?.route?.substringBefore('?') ?: return false
    return route != Screen.Dashboard.route && route != Screen.Onboarding.route
}

internal const val CardioLoadDetailRoute = "activity/cardio_load"
internal const val SleepEfficiencyDetailRoute = "recovery/sleep_efficiency"
internal const val SleepScoreDetailRoute = "recovery/sleep_score"
internal const val HeartRecoveryDetailRoute = "heart/recovery"

/** Titles of the detail routes that are plain strings, not [Screen] objects. */
@StringRes
internal fun detailRouteTitleRes(route: String?): Int? = when (route) {
    CardioLoadDetailRoute -> R.string.metric_cardio_load
    SleepEfficiencyDetailRoute -> R.string.recovery_sleep_efficiency
    SleepScoreDetailRoute -> R.string.recovery_sleep_score
    HeartRecoveryDetailRoute -> R.string.heart_rate_recovery_history_title
    else -> null
}

@Composable
fun AppNavigation(
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    startDestination: String,
    appThemeMode: AppThemeMode = AppThemeMode.SYSTEM,
    routeImportRequest: ExternalRouteImportRequest? = null,
    externalNavigationRoute: String? = null,
    onRouteImportRequestHandled: (Long) -> Unit = {},
    onExternalNavigationHandled: () -> Unit = {},
    onOnboardingComplete: () -> Unit = {},
) {
    val fluidBrandTimeline = rememberFluidBrandTimeline()
    // 83: every time the app comes to the front, the animated toggle opens afresh.
    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_START) {
        fluidBrandTimeline.restart(android.os.SystemClock.uptimeMillis())
    }
    val navController = rememberNavController()
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val liveBackStackEntry by navController.currentBackStackEntryAsState()
    // 63: the app bar and its title follow the screen on view. The content no longer takes its
    // padding from the bar (each destination pads itself), so switching the bar moves nothing.
    // Going back to a screen without a bar, the bar fades out with the screen it belongs to;
    // opening a screen with a bar from one without, it fades in with it.
    var navBackStackEntry by remember { mutableStateOf(liveBackStackEntry) }
    val topBarAlpha = remember { androidx.compose.animation.core.Animatable(1f) }
    LaunchedEffect(liveBackStackEntry?.id) {
        val live = liveBackStackEntry
        val previous = navBackStackEntry
        if (previous == null || live == null || previous.id == live.id) {
            navBackStackEntry = live
            return@LaunchedEffect
        }
        if (!hasAppBarRoute(live) && hasAppBarRoute(previous)) {
            topBarAlpha.animateTo(0f, tween(NavTransitionMillis))
            navBackStackEntry = live
            topBarAlpha.snapTo(1f)
        } else {
            navBackStackEntry = live
            if (!hasAppBarRoute(previous)) {
                topBarAlpha.snapTo(0f)
                topBarAlpha.animateTo(1f, tween(NavTransitionMillis))
            }
        }
    }
    val currentDestination = navBackStackEntry?.destination
    // Day-accepting destinations register with a query pattern appended; compare
    // against the bare route or nothing matches.
    val currentRoute = currentDestination?.route?.substringBefore('?')
    val currentMetricId = if (currentRoute == Screen.Metric.route) {
        navBackStackEntry?.arguments?.getString(METRIC_ID_ARG)?.toDashboardWidgetIdOrNull()
    } else {
        null
    }
    val currentBodyMeasurementType = if (
        currentRoute == Screen.BodyMeasurementEntry.route ||
        currentRoute == Screen.BodyMeasurementEntryEdit.route
    ) {
        navBackStackEntry?.arguments?.getString(BODY_MEASUREMENT_TYPE_ARG)?.toBodyMeasurementTypeOrNull()
    } else {
        null
    }
    val currentVitalsMeasurementType = if (
        currentRoute == Screen.VitalsMeasurementEntry.route ||
        currentRoute == Screen.VitalsMeasurementEntryEdit.route
    ) {
        navBackStackEntry?.arguments?.getString(VITALS_MEASUREMENT_TYPE_ARG)?.toVitalsMeasurementTypeOrNull()
    } else {
        null
    }
    // What the screen on this destination asked the app bar for. It is withdrawn when
    // the screen leaves, so nothing here has to be cleared by route.
    val appBarState = LocalAppBarState.current
    val screenAppBar = appBarState.of(navBackStackEntry)
    var dashboardRefreshRequest by remember { mutableIntStateOf(0) }
    var settingsRouteImportRequest by remember { mutableStateOf<ExternalRouteImportRequest?>(null) }
    var nextSettingsRouteImportRequestId by remember { mutableStateOf(0L) }
    val activeRouteImportRequest = settingsRouteImportRequest ?: routeImportRequest

    fun markDashboardDirty() {
        tech.mmarca.openvitals.features.activity.ActivitiesReturnCache.invalidateAll()
        dashboardRefreshRequest += 1
    }

    fun markDashboardDirtyAndPopBack() {
        markDashboardDirty()
        navController.popBackStack()
    }

    fun navigateToDashboardAfterActivitySave() {
        navController.navigate(Screen.Dashboard.route) {
            popUpTo(Screen.Dashboard.route)
            launchSingleTop = true
            restoreState = true
        }
    }

    fun finishActivityEntrySave() {
        markDashboardDirty()
        val previousRoute =
            navController.previousBackStackEntry?.destination?.route?.substringBefore('?')
        if (
            previousRoute == Screen.Dashboard.route ||
            previousRoute == Screen.Activity.route ||
            previousRoute == Screen.Metric.route
        ) {
            if (navController.popBackStack()) return
        }
        navigateToDashboardAfterActivitySave()
    }

    val topLevelDestinations = remember {
        listOf(
            OpenVitalsNavigationDestination(
                route = Screen.Dashboard.route,
                labelRes = R.string.bottom_nav_dashboard,
                icon = Icons.Outlined.Dashboard,
            ),
        )
    }
    val topLevelRoutes = remember {
        setOf(
            Screen.Dashboard.route,
        )
    }
    val isActivityEntryRoute =
        currentRoute == Screen.ActivityEntry.basePath ||
            currentRoute == Screen.ActivityEntryEdit.basePath
    val showTopBar = currentRoute != null &&
        currentRoute != Screen.Onboarding.route &&
        screenAppBar?.hidesAppBar != true
    val showNavigation =
        topLevelDestinations.size > 1 && currentRoute?.let { it in topLevelRoutes } == true
    // 63: while the bar fades out with the screen being left, it is still that screen's bar: the
    // stack has already popped (nothing behind the dashboard), and reading it took the arrow away
    // at the first frame of the fade.
    val showingLeavingScreen = navBackStackEntry?.id != liveBackStackEntry?.id
    val canNavigateBack =
        currentRoute != null &&
            currentRoute != Screen.Onboarding.route &&
            currentRoute !in topLevelRoutes &&
            (showingLeavingScreen || navController.previousBackStackEntry != null)
    val navigationSelectedRoute = currentRoute?.takeIf { it in topLevelRoutes }
    val addEntryAction = addEntryActionForCurrentRoute(
        currentRoute = currentRoute,
        currentMetricId = currentMetricId,
        onNavigate = { route -> navController.navigate(route) },
    )

    LaunchedEffect(activeRouteImportRequest, currentRoute) {
        if (
            activeRouteImportRequest != null &&
            currentRoute != null &&
            currentRoute != Screen.Onboarding.route
        ) {
            navController.navigate(Screen.ActivityEntry.createRoute()) {
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(externalNavigationRoute, currentRoute) {
        if (
            externalNavigationRoute != null &&
            currentRoute != null &&
            currentRoute != Screen.Onboarding.route
        ) {
            navController.navigate(externalNavigationRoute) {
                // singleTop only for argument-less destinations: a parameterized
                // route would reuse the top entry's ViewModel with old arguments.
                launchSingleTop = externalNavigationRoute == Screen.Dashboard.route ||
                    externalNavigationRoute == Screen.HydrationEntry.route ||
                    externalNavigationRoute == Screen.ActivityEntry.createRoute()
            }
            onExternalNavigationHandled()
        }
    }

    // A screen that knows a better title than its fixed one names it here.
    val screenOwnTitle = when (currentRoute) {
        Screen.BodyMeasurementEntry.route,
        Screen.BodyMeasurementEntryEdit.route ->
            currentBodyMeasurementType?.let { stringResource(it.titleRes()) }
        Screen.VitalsMeasurementEntry.route,
        Screen.VitalsMeasurementEntryEdit.route ->
            currentVitalsMeasurementType?.let { stringResource(it.titleRes()) }
        Screen.Metric.route -> currentMetricId?.let { stringResource(metricTitleRes(it)) }
        else -> null
    }
    val topBarTitle = screenAppBar?.title
        ?: screenOwnTitle
        ?: detailRouteTitleRes(currentRoute)?.let { stringResource(it) }
        ?: Screen.titleResFor(currentRoute)?.let { stringResource(it) }
        ?: ""

    OpenVitalsAdaptiveScaffold(
        title = topBarTitle,
        titleContent = if (currentRoute == Screen.Dashboard.route) {
            { windowWidth -> AmbientFluidBrand(fluidBrandTimeline, windowWidth) }
        } else null,
        navigationDestinations = topLevelDestinations,
        currentRoute = navigationSelectedRoute,
        showTopBar = showTopBar && currentRoute != Screen.Dashboard.route,
        showNavigation = showNavigation,
        canNavigateBack = canNavigateBack,
        // The arrow is Back. Through the dispatcher a screen's BackHandler guards it too: it
        // used to pop the stack directly, past "unsaved changes" and "an import is running".
        onNavigateBack = { backDispatcher?.onBackPressed() ?: navController.popBackStack() },
        onNavigate = { route ->
            if (route in topLevelRoutes) {
                navController.navigate(route) {
                    popUpTo(Screen.Dashboard.route) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        },
        navigationIcon = Icons.AutoMirrored.Outlined.ArrowBack,
        navigationContentDescription = stringResource(R.string.cd_back),
        action = addEntryAction,
        containerColor = screenAppBar?.containerColor,
        topBarAlpha = { topBarAlpha.value },
        topBarActions = {
            // The hydration screen's app-bar add-drink shortcut.
            if (currentRoute == Screen.Metric.route &&
                currentMetricId == DashboardWidgetId.HYDRATION
            ) {
                OpenVitalsIconButton(
                    onClick = { navController.navigate(Screen.HydrationEntry.route) },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = stringResource(R.string.cd_add_drink),
                        tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            // Whatever the screen declared, in the order it declared it. The app bar
            // does not know which screen sent it.
            screenAppBar?.actions?.forEach { action ->
                OpenVitalsIconButton(onClick = action.onClick) {
                    val photo = action.image
                    if (photo != null) {
                        androidx.compose.foundation.Image(
                            bitmap = photo,
                            contentDescription = stringResource(action.contentDescription),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.size(28.dp).clip(androidx.compose.foundation.shape.CircleShape),
                        )
                    } else {
                        Icon(
                            imageVector = action.icon,
                            contentDescription = stringResource(action.contentDescription),
                            tint = action.tint
                                ?: androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (currentRoute == Screen.Dashboard.route) {
                OpenVitalsIconButton(
                    onClick = {
                        navController.navigate(Screen.Achievements.route) {
                            launchSingleTop = true
                        }
                    },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.WorkspacePremium,
                        contentDescription = stringResource(R.string.cd_achievements),
                    )
                }
                OpenVitalsIconButton(
                    onClick = {
                        navController.navigate(Screen.Settings.route) {
                            launchSingleTop = true
                        }
                    },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = stringResource(R.string.cd_settings),
                    )
                }
            }
        },
    ) { innerPadding ->
        // 63: the top is padded by each destination for itself (opaqueComposable), from whether it
        // has the bar, so the bar coming or going during a transition shifts neither screen.
        val layoutDirection = androidx.compose.ui.platform.LocalLayoutDirection.current
        val barTop = androidx.compose.foundation.layout.WindowInsets.systemBars
            .asPaddingValues().calculateTopPadding() + OpenVitalsTopBarHeight
        val destinationTopPadding: (androidx.navigation.NavBackStackEntry) -> androidx.compose.ui.unit.Dp = { entry ->
            if (hasAppBarRoute(entry) && appBarState.of(entry)?.hidesAppBar != true) barTop else 0.dp
        }
        androidx.compose.runtime.CompositionLocalProvider(LocalDestinationTopPadding provides destinationTopPadding) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(
                start = innerPadding.calculateStartPadding(layoutDirection),
                end = innerPadding.calculateEndPadding(layoutDirection),
                bottom = innerPadding.calculateBottomPadding(),
            ),
            // 63: Material "fade through" for every screen, the dashboard included: the screen being
            // left fades out first, then the new one fades and scales in. The screens draw no background
            // of their own, so a cross-fade showed both at once. The system's predictive back is off
            // (manifest): it drew the dashboard over the screen being left mid-gesture.
            enterTransition = { navOpenEnter() },
            exitTransition = { navOpenExit() },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { navBackExit() },
        ) {
            opaqueComposable(Screen.Onboarding.route) {
                val onboardingViewModel = hiltViewModel<OnboardingViewModel>()
                OnboardingScreen(
                    viewModel = onboardingViewModel,
                    onOnboardingComplete = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                            launchSingleTop = true
                        }
                        onOnboardingComplete()
                    },
                )
            }

            opaqueComposable(Screen.Dashboard.route) {
                val dashboardViewModel = hiltViewModel<DashboardViewModel>()
                DashboardScreen(
                    brandTimeline = fluidBrandTimeline,
                    onOpenAchievements = { navController.navigate(Screen.Achievements.route) { launchSingleTop = true } },
                    onOpenSettings = { navController.navigate(Screen.Settings.route) { launchSingleTop = true } },
                    onOpenBodyProfile = { navController.navigate(Screen.SettingsBodyProfile.route) },
                    viewModel = dashboardViewModel,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    refreshRequest = dashboardRefreshRequest,
                    onOpenMetric = { metricId ->
                        // Body Energy takes a date: a tile opened from yesterday shows yesterday.
                        val dashboardSelectedDate = dashboardViewModel.uiState.value.selectedDate
                        navController.navigate(
                            dashboardTileDestination(metricId, dashboardSelectedDate),
                        )
                    },
                    onOpenActivities = {
                        val selected = dashboardViewModel.uiState.value
                        navController.navigate(Screen.Activity.route.withSelectedDay(selected.selectedDate) +
                            (if (selected.selectedDate == java.time.LocalDate.now()) "?" else "&") +
                            "range=${selected.selectedRange.name}")
                    },
                    onOpenActivity = { activityId ->
                        navController.navigate(Screen.ActivityDetail.createRoute(activityId))
                    },
                    onEditActivity = { activityId ->
                        navController.navigate(Screen.ActivityEntryEdit.createRoute(activityId))
                    },
                    onOpenLog = { navController.navigate(Screen.ManualEntry.route) },
                    onStartActivity = {
                        navController.navigate(
                            Screen.ActivityEntry.createRoute(mode = Screen.ActivityEntryMode.RECORD),
                        )
                    },
                )
            }

            opaqueComposable(
                route = Screen.BodyEnergyDetails.route,
                arguments = listOf(navArgument(BODY_ENERGY_DATE_ARG) { type = NavType.StringType }),
            ) { entry ->
                val bodyEnergyViewModel = hiltViewModel<BodyEnergyViewModel>()
                // The readiness verdict is a card here, so its view-model is scoped to this entry.
                val dailyReadinessViewModel = hiltViewModel<DailyReadinessViewModel>()
                val selectedDate = entry.arguments
                    ?.getString(BODY_ENERGY_DATE_ARG)
                    ?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() }
                    ?: java.time.LocalDate.now()
                BodyEnergyDetailsScreen(
                    viewModel = bodyEnergyViewModel,
                    readinessViewModel = dailyReadinessViewModel,
                    selectedDate = selectedDate,
                    onOpenTrainingReadinessDetails = { date ->
                        navController.navigate(Screen.TrainingReadinessDetails.createRoute(date.toString()))
                    },
                    onOpenStressDetails = { date ->
                        navController.navigate(Screen.StressDetails.createRoute(date.toString()))
                    },
                )
            }

            opaqueComposable(
                route = Screen.TrainingReadinessDetails.route,
                arguments = listOf(navArgument(TRAINING_READINESS_DATE_ARG) { type = NavType.StringType }),
            ) { entry ->
                val dailyReadinessViewModel = hiltViewModel<DailyReadinessViewModel>()
                val selectedDate = entry.arguments
                    ?.getString(TRAINING_READINESS_DATE_ARG)
                    ?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() }
                    ?: java.time.LocalDate.now()
                TrainingReadinessDetailsScreen(
                    viewModel = dailyReadinessViewModel,
                    selectedDate = selectedDate,
                )
            }

            opaqueComposable(
                route = Screen.StressDetails.route,
                arguments = listOf(navArgument(STRESS_DATE_ARG) { type = NavType.StringType }),
            ) { entry ->
                val dailyReadinessViewModel = hiltViewModel<DailyReadinessViewModel>()
                val selectedDate = entry.arguments
                    ?.getString(STRESS_DATE_ARG)
                    ?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() }
                    ?: java.time.LocalDate.now()
                StressDetailsScreen(
                    viewModel = dailyReadinessViewModel,
                    selectedDate = selectedDate,
                )
            }

            opaqueComposable(
                route = CardioLoadDetailRoute + SELECTED_DAY_QUERY_PATTERN,
                arguments = listOf(navArgument(SELECTED_DAY_ARG) { type = NavType.StringType; nullable = true; defaultValue = null }),
            ) {
                val activityOverviewViewModel = hiltViewModel<ActivityOverviewViewModel>()
                CardioLoadDetailScreen(
                    viewModel = activityOverviewViewModel,
                    unitFormatter = unitFormatter,
                )
            }

            opaqueComposable(SleepEfficiencyDetailRoute) {
                val recoveryViewModel = hiltViewModel<RecoveryViewModel>()
                SleepEfficiencyDetailScreen(
                    viewModel = recoveryViewModel,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                )
            }

            opaqueComposable(SleepScoreDetailRoute) {
                val recoveryViewModel = hiltViewModel<RecoveryViewModel>()
                SleepScoreDetailScreen(
                    viewModel = recoveryViewModel,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                )
            }

            opaqueComposable(HeartRecoveryDetailRoute) {
                val heartRateRecoveryViewModel = hiltViewModel<HeartRateRecoveryViewModel>()
                HeartRateRecoveryScreen(
                    viewModel = heartRateRecoveryViewModel,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                )
            }

            manualEntryRoutes(
                navController = navController,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                appThemeMode = appThemeMode,
                routeImportRequest = activeRouteImportRequest,
                onRouteImportRequestHandled = { requestId ->
                    if (settingsRouteImportRequest?.id == requestId) {
                        settingsRouteImportRequest = null
                    } else {
                        onRouteImportRequestHandled(requestId)
                    }
                },
                onEntrySaved = ::markDashboardDirty,
                onEntrySavedAndPopBack = ::markDashboardDirtyAndPopBack,
                onActivityEntrySaved = ::finishActivityEntrySave,
            )

            opaqueComposable(
                route = Screen.Metric.route + SELECTED_DAY_QUERY_PATTERN,
                arguments = listOf(
                    navArgument(METRIC_ID_ARG) { type = NavType.StringType },
                    navArgument(SELECTED_DAY_ARG) { type = NavType.StringType; nullable = true; defaultValue = null },
                ),
            ) { backStackEntry ->
                val metricId = backStackEntry.arguments
                    ?.getString(METRIC_ID_ARG)
                    ?.toDashboardWidgetIdOrNull()
                val pinnedDay = backStackEntry.arguments
                    ?.getString(SELECTED_DAY_ARG)
                    ?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() }
                MetricRouteContent(
                    metricId = metricId,
                    pinnedDay = pinnedDay,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    onOpenMetric = { targetMetricId ->
                        when (targetMetricId) {
                            DashboardWidgetId.CALORIES_OUT,
                            DashboardWidgetId.ACTIVE_CALORIES,
                            DashboardWidgetId.BMR -> navController.navigate(Screen.Calories.route)
                            DashboardWidgetId.CALORIES_IN,
                            DashboardWidgetId.PROTEIN,
                            DashboardWidgetId.CARBS,
                            DashboardWidgetId.FAT -> navController.navigate(Screen.Nutrition.route)
                            DashboardWidgetId.SLEEP -> navController.navigate(Screen.Sleep.route)
                            DashboardWidgetId.BODY_ENERGY -> {
                                navController.navigate(
                                    Screen.BodyEnergyDetails.createRoute((pinnedDay ?: java.time.LocalDate.now()).toString()),
                                )
                            }
                            DashboardWidgetId.WEEKLY_CARDIO_LOAD,
                            DashboardWidgetId.CARDIO_LOAD -> navController.navigate(CardioLoadDetailRoute)
                            else -> navController.navigate(Screen.Metric.createRoute(targetMetricId.name))
                        }
                    },
                    onOpenCardioLoad = {
                        navController.navigate(CardioLoadDetailRoute)
                    },
                    onOpenActivity = { activityId ->
                        navController.navigate(Screen.ActivityDetail.createRoute(activityId))
                    },
                    onEditActivity = { activityId ->
                        navController.navigate(Screen.ActivityEntryEdit.createRoute(activityId))
                    },
                    onStartPlannedWorkout = { planId ->
                        navController.navigate(
                            Screen.ActivityEntry.createRoute(mode = Screen.ActivityEntryMode.RECORD, planId = planId),
                        )
                    },
                    onManageWorkoutPlans = {
                        navController.navigate(Screen.WorkoutPlans.route)
                    },
                    onOpenSleepSession = { sleepId ->
                        navController.navigate(Screen.SleepDetail.createRoute(sleepId))
                    },
                    onOpenSleepScore = {
                        navController.navigate(SleepScoreDetailRoute)
                    },
                    onOpenSleepEfficiency = {
                        navController.navigate(SleepEfficiencyDetailRoute)
                    },
                    onOpenHeartRateRecovery = {
                        navController.navigate(HeartRecoveryDetailRoute)
                    },
                    onOpenTrainingReadinessDetails = { date ->
                        navController.navigate(Screen.TrainingReadinessDetails.createRoute(date.toString()))
                    },
                    onOpenStressDetails = { date ->
                        navController.navigate(Screen.StressDetails.createRoute(date.toString()))
                    },
                    onEditHydrationEntry = { entryId ->
                        navController.navigate(Screen.HydrationEntryEdit.createRoute(entryId))
                    },
                    onEditNutritionEntry = { entryId ->
                        navController.navigate(Screen.NutritionEntryEdit.createRoute(entryId))
                    },
                    onOpenCaffeineDrink = { entryId, day ->
                        navController.navigate(Screen.CaffeineDrink.createRoute(entryId, day))
                    },
                    onEditMindfulnessSession = { entryId ->
                        navController.navigate(Screen.MindfulnessEntryEdit.createRoute(entryId))
                    },
                    onEditBodyMeasurement = { type, entryId ->
                        navController.navigate(Screen.BodyMeasurementEntryEdit.createRoute(type.name, entryId))
                    },
                    onEditVitalsMeasurement = { type, entryId ->
                        navController.navigate(Screen.VitalsMeasurementEntryEdit.createRoute(type.name, entryId))
                    },
                    onLogCycleEntry = { date ->
                        navController.navigate(Screen.CycleEntry.createRoute(date))
                    },
                    onChooseCycleEntry = {
                        navController.navigate(Screen.CycleEntryChooser.route)
                    },
                    onOpenCycleSettings = {
                        navController.navigate(Screen.SettingsCycle.route)
                    },
                )
            }

            opaqueComposable(
                route = Screen.CaffeineDrink.route + SELECTED_DAY_QUERY_PATTERN,
                arguments = listOf(
                    navArgument(CAFFEINE_ENTRY_ID_ARG) { type = NavType.StringType },
                    navArgument(SELECTED_DAY_ARG) { type = NavType.StringType; nullable = true; defaultValue = null },
                ),
            ) { backStackEntry ->
                val caffeineViewModel = hiltViewModel<CaffeineViewModel>()
                CaffeineDrinkScreen(
                    viewModel = caffeineViewModel,
                    entryId = backStackEntry.arguments?.getString(CAFFEINE_ENTRY_ID_ARG).orEmpty(),
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                )
            }

            opaqueComposable(
                route = Screen.Calories.route + SELECTED_DAY_QUERY_PATTERN,
                arguments = listOf(navArgument(SELECTED_DAY_ARG) { type = NavType.StringType; nullable = true; defaultValue = null }),
            ) {
                val caloriesViewModel = hiltViewModel<CaloriesViewModel>()
                CaloriesScreen(
                    viewModel = caloriesViewModel,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                )
            }

            opaqueComposable(
                route = Screen.Nutrition.route + SELECTED_DAY_QUERY_PATTERN,
                arguments = listOf(navArgument(SELECTED_DAY_ARG) { type = NavType.StringType; nullable = true; defaultValue = null }),
            ) {
                val nutritionViewModel = hiltViewModel<NutritionViewModel>()
                NutritionScreen(
                    viewModel = nutritionViewModel,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    onEditNutritionEntry = { entryId ->
                        navController.navigate(Screen.NutritionEntryEdit.createRoute(entryId))
                    },
                    onEditHydrationEntry = { entryId ->
                        navController.navigate(Screen.HydrationEntryEdit.createRoute(entryId))
                    },
                )
            }

            opaqueComposable(
                route = Screen.Activity.route + SELECTED_DAY_QUERY_PATTERN + "&range={range}",
                arguments = listOf(navArgument(SELECTED_DAY_ARG) { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("range") { type = NavType.StringType; nullable = true; defaultValue = null }),
            ) {
                val activitiesViewModel = hiltViewModel<ActivitiesViewModel>()
                ActivitiesScreen(
                    viewModel = activitiesViewModel,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    onOpenActivity = { activityId ->
                        navController.navigate(Screen.ActivityDetail.createRoute(activityId))
                    },
                    onEditActivity = { activityId ->
                        navController.navigate(Screen.ActivityEntryEdit.createRoute(activityId))
                    },
                    onStartPlannedWorkout = { planId ->
                        navController.navigate(
                            Screen.ActivityEntry.createRoute(mode = Screen.ActivityEntryMode.RECORD, planId = planId),
                        )
                    },
                    onManageWorkoutPlans = {
                        navController.navigate(Screen.WorkoutPlans.route)
                    },
                    onOpenCardioLoad = {
                        navController.navigate(CardioLoadDetailRoute)
                    },
                    onOpenSteps = {
                        navController.navigate(Screen.Metric.createRoute(DashboardWidgetId.STEPS.name))
                    },
                    onOpenDistance = {
                        navController.navigate(Screen.Metric.createRoute(DashboardWidgetId.DISTANCE.name))
                    },
                    onOpenEnergyBurned = {
                        navController.navigate(Screen.Calories.route)
                    },
                    onOpenHrv = {
                        navController.navigate(Screen.Metric.createRoute(DashboardWidgetId.HRV.name))
                    },
                )
            }

            opaqueComposable(
                route = Screen.ActivityDetail.route,
                arguments = listOf(navArgument(ACTIVITY_DETAIL_ID_ARG) { type = NavType.StringType }),
            ) {
                val activityDetailViewModel = hiltViewModel<ActivityDetailViewModel>()
                ActivityDetailScreen(
                    viewModel = activityDetailViewModel,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    onEditActivity = { activityId ->
                        navController.navigate(Screen.ActivityEntryEdit.createRoute(activityId))
                    },
                    onDeleteActivity = {
                        markDashboardDirty()
                        navController.popBackStack()
                    },
                    onOpenPlan = { planId ->
                        navController.navigate(Screen.WorkoutPlanBuilder.createRoute(planId))
                    },
                )
            }

            opaqueComposable(
                route = Screen.Sleep.route + SELECTED_DAY_QUERY_PATTERN,
                arguments = listOf(navArgument(SELECTED_DAY_ARG) { type = NavType.StringType; nullable = true; defaultValue = null }),
            ) {
                val sleepViewModel = hiltViewModel<SleepViewModel>()
                SleepScreen(
                    viewModel = sleepViewModel,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    onOpenSleepSession = { sleepId ->
                        navController.navigate(Screen.SleepDetail.createRoute(sleepId))
                    },
                    onOpenSleepScore = {
                        navController.navigate(SleepScoreDetailRoute)
                    },
                    onOpenSleepEfficiency = {
                        navController.navigate(SleepEfficiencyDetailRoute)
                    },
                )
            }

            opaqueComposable(
                route = Screen.SleepDetail.route,
                arguments = listOf(navArgument(SLEEP_DETAIL_ID_ARG) { type = NavType.StringType }),
            ) {
                val sleepDetailViewModel = hiltViewModel<SleepDetailViewModel>()
                SleepDetailScreen(
                    viewModel = sleepDetailViewModel,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                )
            }

            opaqueComposable(Screen.Achievements.route) {
                val achievementsViewModel = hiltViewModel<AchievementsViewModel>()
                AchievementsScreen(
                    viewModel = achievementsViewModel,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                )
            }

            settingsRoutes(
                navController = navController,
                onImportRouteFile = { uri ->
                    settingsRouteImportRequest = ExternalRouteImportRequest(
                        id = --nextSettingsRouteImportRequestId,
                        uri = uri,
                    )
                },
                onImportFitFile = { uri ->
                    settingsRouteImportRequest = ExternalRouteImportRequest(
                        id = --nextSettingsRouteImportRequestId,
                        uri = uri,
                    )
                },
                onRouteFilesImported = ::markDashboardDirty,
            )
            medicalRecordsRoutes(navController, dateTimeFormatterProvider)
        }
        }
    }
    HealthConnectNewPermissionsPrompt()
    // The move notice explains the change that also updated the policy, so it goes first.
    val openWatchMoveNoticeViewModel = hiltViewModel<OpenWatchMoveNoticeViewModel>()
    val openWatchMoveNoticeVisible by openWatchMoveNoticeViewModel.visible.collectAsStateWithLifecycle()
    OpenWatchMoveNoticePrompt(openWatchMoveNoticeViewModel)
    PrivacyReconsentPrompt(blocked = openWatchMoveNoticeVisible)
}

private fun addEntryActionForCurrentRoute(
    currentRoute: String?,
    currentMetricId: DashboardWidgetId?,
    onNavigate: (String) -> Unit,
): MetricAction? {
    // The only floating add affordance is the mindfulness FAB.
    if (currentRoute != Screen.Metric.route) return null
    if (currentMetricId != DashboardWidgetId.MINDFULNESS) return null
    return MetricAction(
        labelRes = R.string.mindfulness_log_session,
        icon = Icons.Outlined.Add,
        onClick = { onNavigate(Screen.MindfulnessEntry.route) },
    )
}

private fun metricTitleRes(metricId: DashboardWidgetId): Int =
    when (metricId) {
        DashboardWidgetId.STEPS -> R.string.metric_steps
        DashboardWidgetId.DISTANCE -> R.string.metric_distance
        DashboardWidgetId.CALORIES_OUT -> R.string.screen_calories
        DashboardWidgetId.ACTIVE_CALORIES -> R.string.screen_calories
        DashboardWidgetId.FLOORS -> R.string.metric_floors_climbed
        DashboardWidgetId.ELEVATION -> R.string.metric_elevation_gained
        DashboardWidgetId.WHEELCHAIR_PUSHES -> R.string.metric_wheelchair_pushes
        // The WORKOUT tile lands on the activities overview.
        DashboardWidgetId.WORKOUT -> R.string.screen_activities
        DashboardWidgetId.SLEEP -> R.string.metric_sleep
        DashboardWidgetId.BODY_ENERGY -> R.string.metric_body_energy
        DashboardWidgetId.HYDRATION -> R.string.screen_hydration_title
        DashboardWidgetId.CALORIES_IN -> R.string.metric_calories_in
        DashboardWidgetId.PROTEIN -> R.string.metric_protein
        DashboardWidgetId.CARBS -> R.string.metric_carbs
        DashboardWidgetId.FAT -> R.string.metric_fat
        DashboardWidgetId.CAFFEINE -> R.string.metric_caffeine
        DashboardWidgetId.WEIGHT -> R.string.metric_weight
        DashboardWidgetId.HEIGHT -> R.string.metric_height
        DashboardWidgetId.BMI -> R.string.metric_bmi
        DashboardWidgetId.FFMI -> R.string.metric_ffmi
        DashboardWidgetId.BODY_FAT -> R.string.metric_body_fat
        DashboardWidgetId.LEAN_MASS -> R.string.metric_lean_mass
        DashboardWidgetId.BMR -> R.string.screen_calories
        DashboardWidgetId.BONE_MASS -> R.string.metric_bone_mass
        DashboardWidgetId.BODY_WATER_MASS -> R.string.metric_body_water_mass
        DashboardWidgetId.AVG_HEART_RATE -> R.string.screen_heart_rate
        DashboardWidgetId.RESTING_HEART_RATE -> R.string.metric_resting_heart_rate
        DashboardWidgetId.HRV -> R.string.screen_hrv
        DashboardWidgetId.BLOOD_PRESSURE -> R.string.metric_blood_pressure
        DashboardWidgetId.SPO2 -> R.string.screen_spo2
        DashboardWidgetId.VO2_MAX -> R.string.metric_vo2_max
        DashboardWidgetId.RESPIRATORY_RATE -> R.string.metric_respiratory_rate
        DashboardWidgetId.BODY_TEMPERATURE -> R.string.screen_body_temperature
        DashboardWidgetId.BLOOD_GLUCOSE -> R.string.metric_blood_glucose
        DashboardWidgetId.SKIN_TEMPERATURE -> R.string.metric_skin_temperature
        DashboardWidgetId.WEEKLY_CARDIO_LOAD -> R.string.metric_cardio_load
        DashboardWidgetId.CARDIO_LOAD -> R.string.metric_cardio_load
        DashboardWidgetId.MINDFULNESS -> R.string.metric_mindfulness
        DashboardWidgetId.CYCLE -> R.string.metric_cycle
        DashboardWidgetId.MEDICAL_RECORDS -> R.string.medical_records_title
        DashboardWidgetId.OPENWATCH -> R.string.metric_watch
    }

