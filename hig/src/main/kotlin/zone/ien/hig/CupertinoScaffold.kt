package zone.ien.hig

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapNotNull
import androidx.compose.ui.util.fastMaxOfOrNull
import zone.ien.hig.theme.CupertinoTheme

@Composable
fun CupertinoScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    containerColor: Color = CupertinoScaffoldDefaults.containerColor,
    contentColor: Color = CupertinoScaffoldDefaults.contentColor,
    contentWindowInsets: WindowInsets = CupertinoScaffoldDefaults.contentWindowInsets,
    appBarsBlurAlpha: Float = CupertinoScaffoldDefaults.AppBarsBlurAlpha,
    appBarsBlurRadius: Dp = CupertinoScaffoldDefaults.AppBarsBlurRadius,
    hasNavigationTitle: Boolean = false,
    content: @Composable (PaddingValues) -> Unit,
) {
    val scaffoldCoordinates =
        remember {
            mutableStateOf<LayoutCoordinates?>(null)
        }
    val topBarHeight =
        remember {
            mutableStateOf(0f)
        }

    CupertinoSurface(
        modifier =
            modifier.onGloballyPositioned {
                scaffoldCoordinates.value = it
            },
        color = containerColor,
        contentColor = contentColor,
    ) {
        val appbarState = remember { AppBarsState() }

        CompositionLocalProvider(
            LocalNavigationTitleVisible provides
                rememberSaveable {
                    mutableStateOf(
                        hasNavigationTitle,
                    )
                },
            LocalScaffoldCoordinates provides scaffoldCoordinates,
            LocalTopBarHeight provides topBarHeight,
            LocalScaffoldInsets provides contentWindowInsets,
        ) {
            ScaffoldLayout(
                topBarHeightLocal = topBarHeight,
                fabPosition = floatingActionButtonPosition,
                topBar = {
                    CompositionLocalProvider(
                        LocalContainerColor provides Color.Transparent,
                        LocalAppBarsState provides appbarState,
                        content = topBar,
                    )
                },
                bottomBar = {
                    CompositionLocalProvider(
                        LocalContainerColor provides Color.Transparent,
                        LocalAppBarsState provides appbarState,
                        content = bottomBar,
                    )
                },
                content = content,
                snackbar = snackbarHost,
                contentWindowInsets = contentWindowInsets,
                fab = floatingActionButton,
                appBarsAlpha = appBarsBlurAlpha,
                appBarsBlurRadius = appBarsBlurRadius,
                appBarsState = appbarState,
            )
        }
    }
}

@Composable
private fun ScaffoldLayout(
    appBarsState: AppBarsState,
    topBarHeightLocal: MutableState<Float>,
    fabPosition: FabPosition,
    topBar: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
    snackbar: @Composable () -> Unit,
    fab: @Composable () -> Unit,
    contentWindowInsets: WindowInsets,
    appBarsAlpha: Float,
    appBarsBlurRadius: Dp,
    bottomBar: @Composable () -> Unit,
) {
    SubcomposeLayout { constraints ->
        val layoutWidth = constraints.maxWidth
        val layoutHeight = constraints.maxHeight

        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)

        layout(layoutWidth, layoutHeight) {
            val topBarPlaceables =
                subcompose(ScaffoldLayoutContent.TopBar, topBar).fastMap {
                    it.measure(looseConstraints)
                }

            val topBarHeight = topBarPlaceables.fastMaxOfOrNull { it.height } ?: 0
            topBarHeightLocal.value = topBarHeight.toFloat()
            val snackbarPlaceables =
                subcompose(ScaffoldLayoutContent.Snackbar, snackbar).fastMap {

                    val leftInset =
                        contentWindowInsets
                            .getLeft(this@SubcomposeLayout, layoutDirection)
                    val rightInset =
                        contentWindowInsets
                            .getRight(this@SubcomposeLayout, layoutDirection)
                    val bottomInset = contentWindowInsets.getBottom(this@SubcomposeLayout)

                    it.measure(
                        looseConstraints.offset(
                            -leftInset - rightInset,
                            -bottomInset,
                        ),
                    )
                }

            val snackbarHeight = snackbarPlaceables.fastMaxOfOrNull { it.height } ?: 0
            val snackbarWidth = snackbarPlaceables.fastMaxOfOrNull { it.width } ?: 0

            val fabPlaceables =
                subcompose(ScaffoldLayoutContent.Fab, fab).fastMapNotNull { measurable ->

                    val leftInset =
                        contentWindowInsets.getLeft(this@SubcomposeLayout, layoutDirection)
                    val rightInset =
                        contentWindowInsets.getRight(this@SubcomposeLayout, layoutDirection)
                    val bottomInset = contentWindowInsets.getBottom(this@SubcomposeLayout)
                    measurable
                        .measure(
                            looseConstraints.offset(
                                -leftInset - rightInset,
                                -bottomInset,
                            ),
                        ).takeIf { it.height != 0 && it.width != 0 }
                }

            val fabPlacement =
                if (fabPlaceables.isNotEmpty()) {
                    val fabWidth = fabPlaceables.fastMaxOfOrNull { it.width } ?: 0
                    val fabHeight = fabPlaceables.fastMaxOfOrNull { it.height } ?: 0

                    val fabLeftOffset =
                        if (fabPosition == FabPosition.End) {
                            if (layoutDirection == LayoutDirection.Ltr) {
                                layoutWidth - FabSpacing.roundToPx() - fabWidth
                            } else {
                                FabSpacing.roundToPx()
                            }
                        } else {
                            (layoutWidth - fabWidth) / 2
                        }

                    FabPlacement(
                        left = fabLeftOffset,
                        width = fabWidth,
                        height = fabHeight,
                    )
                } else {
                    null
                }

            val bottomBarPlaceables =
                subcompose(ScaffoldLayoutContent.BottomBar) {
                    CompositionLocalProvider(
                        LocalFabPlacement provides fabPlacement,
                        content = bottomBar,
                    )
                }.fastMap { it.measure(looseConstraints) }

            val bottomBarHeight = bottomBarPlaceables.fastMaxOfOrNull { it.height }
            val fabOffsetFromBottom =
                fabPlacement?.let {
                    if (bottomBarHeight == null) {
                        it.height + FabSpacing.roundToPx() +
                            contentWindowInsets.getBottom(this@SubcomposeLayout)
                    } else {

                        bottomBarHeight + it.height + FabSpacing.roundToPx()
                    }
                }

            val snackbarOffsetFromBottom =
                if (snackbarHeight != 0) {
                    snackbarHeight +
                        (
                            fabOffsetFromBottom ?: bottomBarHeight
                                ?: contentWindowInsets.getBottom(this@SubcomposeLayout)
                        )
                } else {
                    0
                }

            val bodyContentPlaceables =
                subcompose(ScaffoldLayoutContent.MainContent) {
                    val insets = contentWindowInsets.asPaddingValues(this@SubcomposeLayout)
                    val innerPadding =
                        PaddingValues(
                            top =
                                if (topBarPlaceables.isEmpty()) {
                                    insets.calculateTopPadding()
                                } else {
                                    topBarHeight.toDp()
                                },
                            bottom =
                                if (bottomBarPlaceables.isEmpty() || bottomBarHeight == null) {
                                    insets.calculateBottomPadding()
                                } else {
                                    bottomBarHeight.toDp()
                                },
                            start = insets.calculateStartPadding((this@SubcomposeLayout).layoutDirection),
                            end = insets.calculateEndPadding((this@SubcomposeLayout).layoutDirection),
                        )

                    val isTopBarTransparent =
                        appBarsState.isTopBarTransparent.value

                    val isBottomBarTransparent =
                        appBarsState.isBottomBarTransparent.value

                    val topBarColor =
                        appBarsState.topBarColor.value.takeIf {
                            it.isSpecified
                        }

                    val bottomBarColor =
                        appBarsState.bottomBarColor.value.takeIf {
                            it.isSpecified
                        }

                    val topColor =
                        if (topBarColor != null) {
                            if (isTopBarTransparent) {
                                topBarColor.copy(alpha = 0f)
                            } else {
                                topBarColor.copy(alpha = appBarsAlpha)
                            }
                        } else {
                            null
                        }

                    val bottomColor =
                        if (bottomBarColor != null) {
                            if (isBottomBarTransparent) {
                                bottomBarColor.copy(alpha = 0f)
                            } else {
                                bottomBarColor.copy(alpha = appBarsAlpha)
                            }
                        } else {
                            null
                        }

                    val topModifier =
                        if (
                            appBarsAlpha < Float.MIN_VALUE ||
                            isTopBarTransparent ||
                            topBarColor == null ||
                            topColor == null
                        ) {
                            Modifier
                        } else {
                            Modifier.haze(
                                Rect(
                                    left = 0f,
                                    top = 0f,
                                    right = layoutWidth.toFloat(),
                                    bottom = topBarHeight.toFloat(),
                                ),
                                backgroundColor = topBarColor,
                                tint = topColor,
                                blurRadius = appBarsBlurRadius,
                            )
                        }

                    val bottomModifier =
                        if (
                            appBarsAlpha < Float.MIN_VALUE ||
                            isBottomBarTransparent ||
                            bottomBarColor == null ||
                            bottomColor == null
                        ) {
                            Modifier
                        } else {
                            Modifier.haze(
                                Rect(
                                    left = 0f,
                                    top = layoutHeight - (bottomBarHeight ?: 0f).toFloat(),
                                    right = layoutWidth.toFloat(),
                                    bottom = layoutHeight.toFloat(),
                                ),
                                backgroundColor = bottomBarColor,
                                tint = bottomColor,
                                blurRadius = appBarsBlurRadius,
                            )
                        }

                    Box(
                        modifier =
                            topModifier
                                .then(bottomModifier),
                    ) {
                        content(innerPadding)
                    }
                }.fastMap { it.measure(looseConstraints) }

            bodyContentPlaceables.fastForEach {
                it.place(0, 0)
            }
            topBarPlaceables.fastForEach {
                it.place(0, 0)
            }
            snackbarPlaceables.fastForEach {
                it.place(
                    (layoutWidth - snackbarWidth) / 2 +
                        contentWindowInsets.getLeft(this@SubcomposeLayout, layoutDirection),
                    layoutHeight - snackbarOffsetFromBottom,
                )
            }

            bottomBarPlaceables.fastForEach {
                it.place(0, layoutHeight - (bottomBarHeight ?: 0))
            }

            fabPlacement?.let { placement ->
                fabPlaceables.fastForEach {
                    it.place(placement.left, layoutHeight - fabOffsetFromBottom!!)
                }
            }
        }
    }
}

@Immutable
object CupertinoScaffoldDefaults {
    val AppBarsBlurAlpha =
        if (Accessibility.isReduceTransparencyEnabled) {
            .85f
        } else {
            .5f
        }

    val AppBarsBlurRadius =
        if (Accessibility.isReduceTransparencyEnabled) {
            50.dp
        } else {
            40.dp
        }

    val contentWindowInsets: WindowInsets
        @Composable
        get() = WindowInsets.systemBars

    val containerColor: Color
        @Composable
        @ReadOnlyComposable
        get() = CupertinoTheme.colorScheme.systemBackground

    val contentColor: Color
        @Composable
        @ReadOnlyComposable
        get() = CupertinoTheme.colorScheme.label
}

@kotlin.jvm.JvmInline
value class FabPosition internal constructor(
    private val value: Int,
) {
    companion object {

        val Center = FabPosition(0)

        val End = FabPosition(1)
    }

    override fun toString(): String =
        when (this) {
            Center -> "FabPosition.Center"
            else -> "FabPosition.End"
        }
}

@Immutable
internal class FabPlacement(
    val left: Int,
    val width: Int,
    val height: Int,
)

internal val LocalFabPlacement = staticCompositionLocalOf<FabPlacement?> { null }

internal val LocalScaffoldInsets =
    compositionLocalOf<WindowInsets?> {
        null
    }

internal val LocalScaffoldCoordinates =
    compositionLocalOf<State<LayoutCoordinates?>> {
        mutableStateOf(null)
    }

internal val LocalTopBarHeight =
    compositionLocalOf<State<Float?>> {
        mutableStateOf(null)
    }

private val FabSpacing = 16.dp

private enum class ScaffoldLayoutContent { TopBar, MainContent, Snackbar, Fab, BottomBar }

internal class AppBarsState(
    val isTopBarTransparent: MutableState<Boolean> = mutableStateOf(true),
    val isBottomBarTransparent: MutableState<Boolean> = mutableStateOf(true),
    val topBarColor: MutableState<Color> = mutableStateOf(Color.Unspecified),
    val bottomBarColor: MutableState<Color> = mutableStateOf(Color.Unspecified),
)

internal val LocalAppBarsState =
    compositionLocalOf<AppBarsState?> {
        null
    }

val LocalContainerColor =
    compositionLocalOf {
        Color.Unspecified
    }

internal val LocalAppBarsBlurAlpha =
    compositionLocalOf {
        CupertinoScaffoldDefaults.AppBarsBlurAlpha
    }

internal val LocalAppBarsBlurRadius =
    compositionLocalOf {
        CupertinoScaffoldDefaults.AppBarsBlurRadius
    }
