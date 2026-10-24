@file:Suppress("OPT_IN_USAGE_ERROR", "OPT_IN_USAGE")

package pushkar.chorus.music.ui.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import pushkar.chorus.music.ui.player.BottomSheetPlayer
import kotlinx.coroutines.launch
import pushkar.chorus.music.R
import pushkar.chorus.music.constants.FloatingToolbarBottomPadding
import pushkar.chorus.music.constants.FloatingToolbarHorizontalPadding
import pushkar.chorus.music.constants.NavigationBarAnimationSpec
import pushkar.chorus.music.constants.NavigationBarHeight
import pushkar.chorus.music.ui.screens.Screens
import pushkar.chorus.music.ui.utils.resetHeightOffset

@Composable
fun AppBottomBar(
    navController: NavController,
    currentRoute: String?,
    navigationItems: List<Screens>,
    playerBottomSheetState: BottomSheetState,
    shouldShowNavigationBar: Boolean,
    showRail: Boolean,
    isOffline: Boolean,
    useNewMiniPlayerDesign: Boolean,
    pureBlack: Boolean,
    bottomInset: Dp,
    bottomInsetDp: Dp,
    topAppBarScrollBehavior: TopAppBarScrollBehavior,
    onShuffleClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val baseBg = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer

    val navigationBarHeight by animateDpAsState(
        targetValue = if (shouldShowNavigationBar && !showRail && !isOffline) NavigationBarHeight else 0.dp,
        animationSpec = NavigationBarAnimationSpec,
        label = "navBarHeight",
    )

    val onNavItemClick: (Screens, Boolean) -> Unit = remember(navController, coroutineScope, topAppBarScrollBehavior, playerBottomSheetState) {
        { screen: Screens, isSelected: Boolean ->
            if (playerBottomSheetState.isExpanded) {
                playerBottomSheetState.collapseSoft()
            }

            if (isSelected) {
                navController.currentBackStackEntry?.savedStateHandle?.set("scrollToTop", true)
                coroutineScope.launch {
                    topAppBarScrollBehavior.state.resetHeightOffset()
                }
            } else {
                navController.navigate(screen.route) {
                    popUpTo(navController.graph.startDestinationId) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    }

    if (!showRail && currentRoute != "update" && currentRoute != "ambient_mode" && currentRoute != "uptime" && currentRoute?.startsWith("settings") != true) {
        Box(modifier = modifier) {
            BottomSheetPlayer(
                state = playerBottomSheetState,
                navController = navController,
                pureBlack = pureBlack
            )

            val navSlideDistance = bottomInset + FloatingToolbarBottomPadding + NavigationBarHeight

            val navOffsetY = if (navigationBarHeight == 0.dp) {
                navSlideDistance
            } else {
                val slideOffset = navSlideDistance * playerBottomSheetState.progress.coerceIn(0f, 1f)
                val hideOffset = navSlideDistance * (1 - navigationBarHeight.coerceAtMost(NavigationBarHeight) / NavigationBarHeight)
                slideOffset + hideOffset
            }

            if (!isOffline) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .height(navSlideDistance)
                        .offset(y = navOffsetY),
                ) {
                    FloatingNavigationToolbar(
                        items = navigationItems,
                        pureBlack = pureBlack,
                        isSelected = { screen ->
                            currentRoute == screen.route || currentRoute?.startsWith("${screen.route}/") == true
                        },
                        onItemClick = onNavItemClick,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(
                                start = FloatingToolbarHorizontalPadding,
                                end = FloatingToolbarHorizontalPadding,
                                bottom = bottomInset + FloatingToolbarBottomPadding,
                            )
                            .height(NavigationBarHeight)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .height(bottomInsetDp)
                    .graphicsLayer {
                        val progress = playerBottomSheetState.progress
                        alpha = if (progress > 0f || (useNewMiniPlayerDesign && !shouldShowNavigationBar)) 0f else 1f
                    }
                    .background(baseBg)
            )
        }
    } else {
        if (currentRoute != "update" && currentRoute != "ambient_mode" && currentRoute != "uptime" && currentRoute?.startsWith("settings") != true) {
            BottomSheetPlayer(
                state = playerBottomSheetState,
                navController = navController,
                pureBlack = pureBlack
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(bottomInsetDp)
                .graphicsLayer {
                    val progress = playerBottomSheetState.progress
                    alpha = if (progress > 0f || (useNewMiniPlayerDesign && !shouldShowNavigationBar)) 0f else 1f
                }
                .background(baseBg)
        )
    }
}
