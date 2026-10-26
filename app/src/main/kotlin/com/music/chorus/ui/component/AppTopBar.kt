package pushkar.chorus.music.ui.component

import android.os.Build
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import pushkar.chorus.music.R
import pushkar.chorus.music.constants.NavigationBarHeight
import pushkar.chorus.music.ui.component.shapes.ContinuousRoundedRectangle
import pushkar.chorus.music.ui.fluid.foundation.FluidTheme
import pushkar.chorus.music.ui.fluid.foundation.GlassIntensity
import pushkar.chorus.music.ui.fluid.foundation.fluidGlass
import pushkar.chorus.music.ui.screens.Screens
import pushkar.chorus.music.ui.theme.isFluidTheme

@Composable
fun AppTopBar(
    title: String,
    currentRoute: String?,
    isOffline: Boolean,
    showHistoryButton: Boolean,
    accountImageUrl: String?,
    scrollBehavior: TopAppBarScrollBehavior,
    pureBlack: Boolean,
    showRail: Boolean,
    cutoutInsets: WindowInsets,
    onHistoryClick: () -> Unit,
    onStatsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onCreatePlaylistClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isFluid = isFluidTheme()
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    var showLibraryMenu by remember { mutableStateOf(false) }

    CenterAlignedTopAppBar(
        title = {
            val titleStyle = if (isFluid) {
                FluidTheme.typography.title1.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
            } else {
                MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
            }
            Text(
                text = title,
                style = titleStyle,
                maxLines = 1,
            )
        },
        navigationIcon = {},
        actions = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 12.dp)
            ) {
                if (isOffline) {
                    TopBarIconButton(onClick = onSettingsClick) {
                        Icon(
                            painter = painterResource(R.drawable.settings),
                            contentDescription = stringResource(R.string.settings),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else if (currentRoute == Screens.Home.route) {
                    TopBarIconButton(onClick = onSettingsClick) {
                        if (accountImageUrl != null) {
                            AsyncImage(
                                model = accountImageUrl,
                                contentDescription = stringResource(R.string.settings),
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.settings),
                                contentDescription = stringResource(R.string.settings),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                } else if (currentRoute == Screens.Library.route) {
                    Box {
                        TopBarIconButton(onClick = { showLibraryMenu = true }) {
                            Icon(
                                painter = painterResource(R.drawable.more_vert),
                                contentDescription = stringResource(R.string.more_options),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showLibraryMenu,
                            onDismissRequest = { showLibraryMenu = false },
                            shape = if (isFluid) ContinuousRoundedRectangle(16.dp) else RoundedCornerShape(16.dp),
                            containerColor = if (isFluid) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = if (isFluid) Modifier.fluidGlass(intensity = GlassIntensity.Prominent) else Modifier
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.create_playlist)) },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(R.drawable.add),
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                onClick = {
                                    showLibraryMenu = false
                                    onCreatePlaylistClick()
                                }
                            )

                            if (showHistoryButton) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.history)) },
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(R.drawable.music_history),
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    onClick = {
                                        showLibraryMenu = false
                                        onHistoryClick()
                                    }
                                )
                            }

                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.stats)) },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(R.drawable.stats),
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                onClick = {
                                    showLibraryMenu = false
                                    onStatsClick()
                                }
                            )
                        }
                    }
                } else {
                    TopBarIconButton(onClick = onSettingsClick) {
                        if (accountImageUrl != null) {
                            AsyncImage(
                                model = accountImageUrl,
                                contentDescription = stringResource(R.string.settings),
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.settings),
                                contentDescription = stringResource(R.string.settings),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        },
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent,
            titleContentColor = if (isFluid) FluidTheme.colors.label else MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = if (isFluid) FluidTheme.colors.label else MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = if (isFluid) FluidTheme.colors.label else MaterialTheme.colorScheme.onSurface
        ),
        windowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Top),
        modifier = modifier
            .windowInsetsPadding(
                if (showRail) {
                    WindowInsets(left = NavigationBarHeight)
                        .add(cutoutInsets.only(WindowInsetsSides.Start))
                } else {
                    cutoutInsets.only(WindowInsetsSides.Start + WindowInsetsSides.End)
                }
            )
    )
}

