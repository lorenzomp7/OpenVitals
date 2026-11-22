package tech.mmarca.openvitals.navigation

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDeepLink
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import tech.mmarca.openvitals.ui.components.multiTouchFlingGuard

/**
 * A destination painted on the app's background. The screens draw none of their own, so
 * during a transition the one underneath showed through; opaque, the screen arriving can sit
 * fully drawn under the one leaving, and no frame is ever empty.
 */
/** How far down each destination starts: under the app bar when it has one. */
internal val LocalDestinationTopPadding =
    androidx.compose.runtime.staticCompositionLocalOf<(NavBackStackEntry) -> androidx.compose.ui.unit.Dp> { { 0.dp } }

internal fun NavGraphBuilder.opaqueComposable(
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    deepLinks: List<NavDeepLink> = emptyList(),
    content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit,
) {
    composable(route = route, arguments = arguments, deepLinks = deepLinks) { entry ->
        val scope = this
        val top = LocalDestinationTopPadding.current(entry)
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    tech.mmarca.openvitals.ui.components.LocalAppBarState.current.of(entry)?.containerColor
                        ?: MaterialTheme.colorScheme.background,
                )
                .padding(top = top)
                // One guard for every screen's lists.
                .multiTouchFlingGuard(),
        ) { scope.content(entry) }
    }
}
