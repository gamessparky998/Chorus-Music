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
            val targetRouteIndex = navigationItems.indexOfFirst { it.route == targetState.destination.route }
            val initialRouteIndex = navigationItems.indexOfFirst { it.route == initialState.destination.route }

            if (targetRouteIndex != -1 && initialRouteIndex != -1) {
                fadeIn(animationSpec = tween(180, easing = androidx.compose.animation.core.LinearOutSlowInEasing))
            } else {
                val transitionSpec = tween<Float>(240, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                val slideSpec = tween<androidx.compose.ui.unit.IntOffset>(240, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                if (targetRouteIndex == -1 || targetRouteIndex > initialRouteIndex)
                    slideInHorizontally(animationSpec = slideSpec) { it / 6 } + fadeIn(animationSpec = transitionSpec)
                else
                    slideInHorizontally(animationSpec = slideSpec) { -it / 6 } + fadeIn(animationSpec = transitionSpec)
            }
        },
        exitTransition = {
            val targetRouteIndex = navigationItems.indexOfFirst { it.route == targetState.destination.route }
            val initialRouteIndex = navigationItems.indexOfFirst { it.route == initialState.destination.route }

            if (targetRouteIndex != -1 && initialRouteIndex != -1) {
                fadeOut(animationSpec = tween(140, easing = androidx.compose.animation.core.FastOutLinearInEasing))
            } else {
                val transitionSpec = tween<Float>(240, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                val slideSpec = tween<androidx.compose.ui.unit.IntOffset>(240, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                if (targetRouteIndex == -1 || targetRouteIndex > initialRouteIndex)
                    slideOutHorizontally(animationSpec = slideSpec) { -it / 6 } + fadeOut(animationSpec = transitionSpec)
                else
                    slideOutHorizontally(animationSpec = slideSpec) { it / 6 } + fadeOut(animationSpec = transitionSpec)
            }
        },
        popEnterTransition = {
            val targetRouteIndex = navigationItems.indexOfFirst { it.route == targetState.destination.route }
            val initialRouteIndex = navigationItems.indexOfFirst { it.route == initialState.destination.route }

            if (targetRouteIndex != -1 && initialRouteIndex != -1) {
                fadeIn(animationSpec = tween(180, easing = androidx.compose.animation.core.LinearOutSlowInEasing))
            } else {
                val transitionSpec = tween<Float>(240, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                val slideSpec = tween<androidx.compose.ui.unit.IntOffset>(240, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                slideInHorizontally(animationSpec = slideSpec) { -it / 6 } + fadeIn(animationSpec = transitionSpec)
            }
        },
        popExitTransition = {
            val targetRouteIndex = navigationItems.indexOfFirst { it.route == targetState.destination.route }
            val initialRouteIndex = navigationItems.indexOfFirst { it.route == initialState.destination.route }

            if (targetRouteIndex != -1 && initialRouteIndex != -1) {
                fadeOut(animationSpec = tween(140, easing = androidx.compose.animation.core.FastOutLinearInEasing))
            } else {
                val transitionSpec = tween<Float>(240, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                val slideSpec = tween<androidx.compose.ui.unit.IntOffset>(240, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                slideOutHorizontally(animationSpec = slideSpec) { it / 6 } + fadeOut(animationSpec = transitionSpec)
            }
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
