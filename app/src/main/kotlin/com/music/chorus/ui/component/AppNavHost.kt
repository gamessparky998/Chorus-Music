package pushkar.chorus.music.ui.component

import androidx.activity.ComponentActivity
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import pushkar.chorus.music.ui.screens.Screens
import pushkar.chorus.music.ui.screens.navigationBuilder

@Composable
fun AppNavHost(
    navController: NavHostController,
    navigationItems: List<Screens>,
    startDestination: String,
    topAppBarScrollBehavior: TopAppBarScrollBehavior,
    activity: ComponentActivity,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            val currentRouteIndex = navigationItems.indexOfFirst {
                it.route == targetState.destination.route
            }
            val previousRouteIndex = navigationItems.indexOfFirst {
                it.route == initialState.destination.route
            }

            val transitionSpec = tween<Float>(240, easing = androidx.compose.animation.core.FastOutSlowInEasing)
            val slideSpec = tween<androidx.compose.ui.unit.IntOffset>(240, easing = androidx.compose.animation.core.FastOutSlowInEasing)
            if (currentRouteIndex == -1 || currentRouteIndex > previousRouteIndex)
                slideInHorizontally(animationSpec = slideSpec) { it / 8 } + fadeIn(animationSpec = transitionSpec)
            else
                slideInHorizontally(animationSpec = slideSpec) { -it / 8 } + fadeIn(animationSpec = transitionSpec)
        },
        exitTransition = {
            val currentRouteIndex = navigationItems.indexOfFirst {
                it.route == initialState.destination.route
            }
            val targetRouteIndex = navigationItems.indexOfFirst {
                it.route == targetState.destination.route
            }

            val transitionSpec = tween<Float>(240, easing = androidx.compose.animation.core.FastOutSlowInEasing)
            val slideSpec = tween<androidx.compose.ui.unit.IntOffset>(240, easing = androidx.compose.animation.core.FastOutSlowInEasing)
            if (targetRouteIndex == -1 || targetRouteIndex > currentRouteIndex)
                slideOutHorizontally(animationSpec = slideSpec) { -it / 8 } + fadeOut(animationSpec = transitionSpec)
            else
                slideOutHorizontally(animationSpec = slideSpec) { it / 8 } + fadeOut(animationSpec = transitionSpec)
        },
        popEnterTransition = {
            val currentRouteIndex = navigationItems.indexOfFirst {
                it.route == targetState.destination.route
            }
            val previousRouteIndex = navigationItems.indexOfFirst {
                it.route == initialState.destination.route
            }

            val transitionSpec = tween<Float>(240, easing = androidx.compose.animation.core.FastOutSlowInEasing)
            val slideSpec = tween<androidx.compose.ui.unit.IntOffset>(240, easing = androidx.compose.animation.core.FastOutSlowInEasing)
            if (previousRouteIndex != -1 && previousRouteIndex < currentRouteIndex)
                slideInHorizontally(animationSpec = slideSpec) { it / 8 } + fadeIn(animationSpec = transitionSpec)
            else
                slideInHorizontally(animationSpec = slideSpec) { -it / 8 } + fadeIn(animationSpec = transitionSpec)
        },
        popExitTransition = {
            val currentRouteIndex = navigationItems.indexOfFirst {
                it.route == initialState.destination.route
            }
            val targetRouteIndex = navigationItems.indexOfFirst {
                it.route == targetState.destination.route
            }

            val transitionSpec = tween<Float>(240, easing = androidx.compose.animation.core.FastOutSlowInEasing)
            val slideSpec = tween<androidx.compose.ui.unit.IntOffset>(240, easing = androidx.compose.animation.core.FastOutSlowInEasing)
            if (currentRouteIndex != -1 && currentRouteIndex < targetRouteIndex)
                slideOutHorizontally(animationSpec = slideSpec) { -it / 8 } + fadeOut(animationSpec = transitionSpec)
            else
                slideOutHorizontally(animationSpec = slideSpec) { it / 8 } + fadeOut(animationSpec = transitionSpec)
        },
        modifier = modifier,
    ) {
        navigationBuilder(
            navController = navController,
            scrollBehavior = topAppBarScrollBehavior,
            activity = activity,
            snackbarHostState = snackbarHostState,
        )
    }
}
