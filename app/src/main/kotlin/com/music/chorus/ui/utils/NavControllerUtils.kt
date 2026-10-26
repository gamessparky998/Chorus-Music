package pushkar.chorus.music.ui.utils

import androidx.navigation.NavController
import pushkar.chorus.music.ui.screens.Screens

fun NavController.backToMain() {
    val mainRoutes = Screens.MainScreens.map { it.route }
    val targetEntry = currentBackStack.value.lastOrNull { entry ->
        entry.destination.route in mainRoutes
    }

    if (targetEntry != null) {
        popBackStack(targetEntry.destination.id, inclusive = false)
    } else {
        navigate(Screens.Home.route) {
            popUpTo(graph.startDestinationId) {
                inclusive = false
            }
            launchSingleTop = true
        }
    }
}