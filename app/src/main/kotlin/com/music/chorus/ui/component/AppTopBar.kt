package pushkar.chorus.music.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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

    val topBarContainerColor = when {
        pureBlack -> Color.Black
        isFluid -> if (isDark) Color(0xD9000000) else Color(0xD9F9F9FB)
        else -> MaterialTheme.colorScheme.surfaceContainer
    }

    val topBarScrolledColor = when {
        pureBlack -> Color.Black
        isFluid -> if (isDark) Color(0xE61C1C1E) else Color(0xE6FFFFFF)
        else -> MaterialTheme.colorScheme.surfaceContainer
    }

    Row(modifier = modifier) {
        TopAppBar(
            title = {
                val titleStyle = if (isFluid) {
                    FluidTheme.typography.largeTitle.copy(fontWeight = FontWeight.Bold)
                } else {
                    MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                }
                Text(
                    text = title,
                    style = titleStyle,
                )
            },
            actions = {
                if (isOffline) {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            painter = painterResource(R.drawable.settings),
                            contentDescription = stringResource(R.string.settings)
                        )
                    }
                } else if (currentRoute == Screens.Library.route) {
                    Box {
                        IconButton(onClick = { showLibraryMenu = true }) {
                            Icon(
                                painter = painterResource(R.drawable.more_vert),
                                contentDescription = stringResource(R.string.more_options)
                            )
                        }

                        DropdownMenu(
                            expanded = showLibraryMenu,
                            onDismissRequest = { showLibraryMenu = false },
                            shape = if (isFluid) ContinuousRoundedRectangle(16.dp) else RoundedCornerShape(16.dp),
                            containerColor = if (isFluid) {
                                if (isDark) Color(0xF0202022) else Color(0xF0FFFFFF)
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
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
                    IconButton(onClick = onSettingsClick) {
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
                }
            },
            scrollBehavior = scrollBehavior,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = topBarContainerColor,
                scrolledContainerColor = topBarScrolledColor,
                titleContentColor = if (isFluid) FluidTheme.colors.label else MaterialTheme.colorScheme.onSurface,
                actionIconContentColor = if (isFluid) FluidTheme.colors.label else MaterialTheme.colorScheme.onSurfaceVariant,
                navigationIconContentColor = if (isFluid) FluidTheme.colors.label else MaterialTheme.colorScheme.onSurfaceVariant
            ),
            windowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Top),
            modifier = Modifier
                .let { mod ->
                    if (isFluid) mod.fluidGlass(intensity = GlassIntensity.Subtle) else mod
                }
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
}
