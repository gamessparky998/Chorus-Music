package zone.ien.hig

import androidx.compose.animation.core.Animatable as FloatAnimatable
import androidx.compose.animation.Animatable as ColorAnimatable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawPlainBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.runtimeShaderEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import zone.ien.hig.section.CupertinoSectionDefaults
import zone.ien.hig.theme.CupertinoTheme
import zone.ien.hig.theme.darkColorScheme
import zone.ien.hig.theme.lightColorScheme
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

inline val ScrollableState.isTopBarTransparent: Boolean
    get() = !canScrollBackward

@Composable
fun LazyListState.isTopBarTransparent(topPadding: Dp = 0.dp): Boolean {
    val topPaddingPx =
        LocalDensity.current.run {
            remember(topPadding) {
                topPadding.toPx()
            }
        }

    layoutInfo.visibleItemsInfo.first().offset
    return remember {
        derivedStateOf {
            !canScrollBackward ||
                firstVisibleItemIndex == 0 &&
                firstVisibleItemScrollOffset < topPaddingPx
        }
    }.value
}

@Composable
fun cupertinoTranslucentTopBarColor(
    color: Color,
    isTranslucent: Boolean,
    isTransparent: Boolean,
): Color {
    if (!isTranslucent) {
        return color
    }

    val appBarsState = LocalAppBarsState.current ?: return color

    DisposableEffect(appBarsState, color) {
        appBarsState.topBarColor.value = color
        onDispose {
            appBarsState.topBarColor.value = Color.Unspecified
        }
    }

    DisposableEffect(isTransparent, appBarsState) {
        appBarsState.isTopBarTransparent.value = isTransparent
        onDispose {
            appBarsState.isTopBarTransparent.value = true
        }
    }
    return color.copy(alpha = 0.0f)
}

@Composable
fun CupertinoTopAppBar(
    title: @Composable () -> Unit,
    subtitle: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable (RowScope.() -> Unit) = {},
    windowInsets: WindowInsets = LocalTopAppBarInsets.current ?: CupertinoTopAppBarDefaults.windowInsets,
    isCenterAligned: Boolean = true,
    isBackgroundAdaptive: Boolean = true,
    isBackgroundGradient: Boolean = false,
    backdrop: LayerBackdrop = rememberLayerBackdrop(),
    colors: CupertinoTopAppBarColors = CupertinoTopAppBarDefaults.topAppBarColors(),
) {
    InlineTopAppBar(
        title = title,
        subtitle = subtitle,
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
        windowInsets = windowInsets,
        isCenterAligned = isCenterAligned,
        colors = colors,
        isBackgroundAdaptive = isBackgroundAdaptive,
        isBackgroundGradient = isBackgroundGradient,
        backdrop = backdrop
    )
}

internal val LocalNavigationTitleVisible = compositionLocalOf { mutableStateOf(false) }

private class ClipShape(
    private val offsetDifference: Float,
): Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline =
        Outline.Rectangle(
            Rect(
                top = offsetDifference.coerceAtMost(size.height),
                left = 0f,
                right = size.width,
                bottom = size.height,
            ),
        )
}

private const val NavTitleMaxFontScale = 1.1f
private val NavTitleMaxFontScaleDistance = 150.dp

@Composable
fun CupertinoNavigationTitle(
    modifier: Modifier = Modifier,
    maxFontScale: Float = NavTitleMaxFontScale,
    maxFontScaleDistance: Dp = NavTitleMaxFontScaleDistance,
    paddingValues: PaddingValues = CupertinoSectionDefaults.PaddingValues,
    subtitle: @Composable (() -> Unit)? = null,
    title: @Composable () -> Unit,
) {
    require(maxFontScale >= 1) {
        "maxFontScale must be >= 1."
    }
    var visible by LocalNavigationTitleVisible.current

    val density = LocalDensity.current
    val scaffoldCoordinates by LocalScaffoldCoordinates.current
    val topBarHeightPx = (LocalTopBarHeight.current.value ?: 0f)
    val topAppBarExists = topBarHeightPx > 0

    var offsetDifference by remember { mutableStateOf(0f) }
    var actualTopBarHeight by remember { mutableStateOf(0f) }

    val maxSizeIncreaseDistancePx = density.run { maxFontScaleDistance.toPx() }

    val insets = LocalScaffoldInsets.current?.getTop(density) ?: 0
    val fontIncrease by remember(maxSizeIncreaseDistancePx, maxFontScale) {
        derivedStateOf {
            val d = offsetDifference + topBarHeightPx - insets
            if (d >= 0) {
                1f
            } else {
                1f + (-d / maxSizeIncreaseDistancePx).coerceIn(0f, 1f) * (maxFontScale - 1)
            }
        }
    }

    val font = CupertinoTheme.typography.largeTitle.copy(fontWeight = FontWeight.Bold)
    val subtitleFont = CupertinoTheme.typography.subhead

    val titleAlpha by remember {
        derivedStateOf {
                if (!topAppBarExists) {
                    1f
                } else {
                    val d = offsetDifference - actualTopBarHeight + 50

                    if (d <= 0) {
                        1f
                    } else {

                        val fadeDistance = 100f
                        (1f - (d / fadeDistance)).coerceIn(0f, 1f)
                    }
                }
        }
    }

    val animatedAlpha by animateFloatAsState(
        targetValue = titleAlpha,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "NavigationTitleAlpha"
    )

    Column(
        modifier
            .padding(paddingValues)
            .alpha(animatedAlpha)
            .onGloballyPositioned {
                actualTopBarHeight = it.size.height.toFloat()
                val scaffoldTop = (scaffoldCoordinates?.boundsInWindow()?.top ?: 0f)

                offsetDifference = (topBarHeightPx - it.boundsInWindow().top) + scaffoldTop

                visible = !topAppBarExists || offsetDifference < it.size.height
            },
    ) {
        ProvideTextStyle(value = font.copy(fontSize = font.fontSize * fontIncrease)) {
            title()
        }
        ProvideTextStyle(value = subtitleFont.copy(fontSize = subtitleFont.fontSize * fontIncrease)) {
            CompositionLocalProvider(
                LocalContentColor provides CupertinoTheme.colorScheme.secondaryLabel,
            ) {
                subtitle?.invoke()
            }
        }
    }
}

@Stable
class CupertinoTopAppBarColors internal constructor(
    private val lightGradientColor: Color,
    private val darkGradientColor: Color,
    internal val navigationIconContentColor: Color,
    internal val lightTitleContentColor: Color,
    internal val darkTitleContentColor: Color,
    internal val lightSubtitleContentColor: Color,
    internal val darkSubtitleContentColor: Color,
    internal val actionIconContentColor: Color,
) {
    @Composable
    fun gradientColor(isDark: Boolean = isSystemInDarkTheme()): State<Color> {
        return rememberUpdatedState(if (isDark) darkGradientColor else lightGradientColor)
    }
    @Composable
    fun titleContentColor(isDark: Boolean = isSystemInDarkTheme()): State<Color> {
        return rememberUpdatedState(if (isDark) darkTitleContentColor else lightTitleContentColor)
    }

    @Composable
    fun subtitleContentColor(isDark: Boolean = isSystemInDarkTheme()): State<Color> {
        return rememberUpdatedState(if (isDark) darkSubtitleContentColor else lightSubtitleContentColor)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is CupertinoTopAppBarColors) return false

        if (lightGradientColor != other.lightGradientColor) return false
        if (darkGradientColor != other.darkGradientColor) return false
        if (navigationIconContentColor != other.navigationIconContentColor) return false
        if (lightTitleContentColor != other.lightTitleContentColor) return false
        if (darkTitleContentColor != other.darkTitleContentColor) return false
        if (lightSubtitleContentColor != other.lightSubtitleContentColor) return false
        if (darkSubtitleContentColor != other.darkSubtitleContentColor) return false
        if (actionIconContentColor != other.actionIconContentColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = lightGradientColor.hashCode()
        result = 31 * result + darkGradientColor.hashCode()
        result = 31 * result + navigationIconContentColor.hashCode()
        result = 31 * result + lightTitleContentColor.hashCode()
        result = 31 * result + darkTitleContentColor.hashCode()
        result = 31 * result + darkSubtitleContentColor.hashCode()
        result = 31 * result + darkSubtitleContentColor.hashCode()
        result = 31 * result + actionIconContentColor.hashCode()

        return result
    }
}

internal val LocalTopAppBarInsets = compositionLocalOf<WindowInsets?> { null }

@Composable
private fun InlineTopAppBar(
    title: @Composable () -> Unit,
    subtitle: @Composable (() -> Unit)? = null,
    modifier: Modifier,
    navigationIcon: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    windowInsets: WindowInsets,
    isCenterAligned: Boolean,
    colors: CupertinoTopAppBarColors,
    isBackgroundAdaptive: Boolean,
    isBackgroundGradient: Boolean,
    backdrop: LayerBackdrop
) {
    val navTitleVisible by LocalNavigationTitleVisible.current
    val isLightTheme = !isSystemInDarkTheme()
    val layer = rememberGraphicsLayer()
    var titleX by remember { mutableStateOf(0) }
    var titleWidth by remember { mutableStateOf(0) }
    val topAppBarHeightPx = LocalDensity.current.run { TopAppBarHeight.toPx() }

    val lightGradientColor by colors.gradientColor(isDark = false)
    val darkGradientColor by colors.gradientColor(isDark = true)
    val lightTitleColor by colors.titleContentColor(isDark = false)
    val darkTitleColor by colors.titleContentColor(isDark = true)
    val lightSubtitleColor by colors.subtitleContentColor(isDark = false)
    val darkSubtitleColor by colors.subtitleContentColor(isDark = true)

    val luminanceAnimation = remember { FloatAnimatable(if (isLightTheme) 1f else 0f) }
    val gradientColorAnimation = remember { ColorAnimatable(if (isLightTheme) lightGradientColor else darkGradientColor) }
    val titleColorAnimation = remember { ColorAnimatable(if (isLightTheme) lightTitleColor else darkTitleColor) }
    val subtitleColorAnimation = remember { ColorAnimatable(if (isLightTheme) lightSubtitleColor else darkSubtitleColor) }

    if (isBackgroundAdaptive) {
        val defaultColor = CupertinoTheme.colorScheme.systemBackground

        LaunchedEffect(layer) {
            while (isActive) {
                if (layer.size != IntSize.Zero) {
                    try {
                        val averageLuminance = layer.toImageBitmap().averageLuminance(cropX = titleX, cropWidth = titleWidth.takeIf { it != 0 } ?: layer.size.width, sampleWidth = 5, defaultColor = defaultColor)

                        launch {
                            gradientColorAnimation.animateTo(
                                if (averageLuminance > 0.5f) lightGradientColor else darkGradientColor,
                                tween(300)
                            )
                            titleColorAnimation.animateTo(
                                if (averageLuminance > 0.5f) lightTitleColor else darkTitleColor,
                                tween(300)
                            )
                            subtitleColorAnimation.animateTo(
                                if (averageLuminance > 0.5f) lightSubtitleColor else darkSubtitleColor,
                                tween(300)
                            )
                        }
                        luminanceAnimation.animateTo(
                            averageLuminance.toFloat(),
                            tween(300)
                        )
                    } catch (e: RuntimeException) {

                        break
                    }
                }

                delay(300.milliseconds)
            }
        }
    }

    Box {
        Box(
            modifier = Modifier
                .drawPlainBackdrop(
                    backdrop = backdrop,
                    shape = { RectangleShape },
                    effects = {
                        blur(8.dp.toPx())
                        runtimeShaderEffect(
                            "AlphaMask",
                            """
    uniform shader content;

    uniform float2 size;
    layout(color) uniform half4 tint;
    uniform float tintIntensity;

    half4 main(float2 coord) {
        float blurAlpha = smoothstep(size.y, size.y * 0.5, coord.y);
        float tintAlpha = smoothstep(size.y, size.y * 0.5, coord.y);
        return mix(content.eval(coord) * blurAlpha, tint * tintAlpha, tintIntensity);
    }""",
                            "content"
                        ) {
                            setFloatUniform("size", size.width, size.height)
                            setColorUniform("tint", gradientColorAnimation.value)
                            setFloatUniform("tintIntensity", if (isBackgroundGradient) 0.8f else 0f)
                        }
                    },
                    onDrawBackdrop = { drawBackdrop ->
                        drawBackdrop()
                        layer.record { drawBackdrop() }
                    }
                )
                .fillMaxWidth()
                .windowInsetsPadding(windowInsets)
                .height(TopAppBarHeight)
        )
        TopAppBarLayout(
            modifier = modifier.windowInsetsPadding(windowInsets),
            heightPx = topAppBarHeightPx,
            navigationIconContentColor = colors.navigationIconContentColor,
            actionIconContentColor = colors.actionIconContentColor,
            title = {
                val blurRadius by animateFloatAsState(
                    targetValue = if (!navTitleVisible) 0f else 4f,
                    animationSpec = tween(700)
                )

                AnimatedVisibility(
                    visible = !navTitleVisible,
                    enter = fadeIn(tween(700)) + slideInVertically(tween(700)) { it / 2 },
                    exit = fadeOut(tween(700)) + slideOutVertically(tween(700)) { it / 2 },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Column(
                        horizontalAlignment = if (isCenterAligned) Alignment.CenterHorizontally else Alignment.Start,
                        modifier = Modifier
                            .blur(
                                radius = blurRadius.dp,
                                edgeTreatment = BlurredEdgeTreatment.Unbounded,
                            )
                            .onGloballyPositioned {
                                val position = it.positionInRoot()
                                val size = it.size

                                titleX = position.x.roundToInt()
                                titleWidth = size.width
                            }
                    ) {
                        ProvideTextStyle(value =
                            if (subtitle == null) CupertinoTheme.typography.headline
                            else CupertinoTheme.typography.subhead.copy(fontWeight = FontWeight.Bold)
                        ) {
                            CompositionLocalProvider(
                                LocalContentColor provides titleColorAnimation.value,
                            ) {
                                title()
                            }
                        }
                        ProvideTextStyle(value = CupertinoTheme.typography.subhead) {
                            CompositionLocalProvider(
                                LocalContentColor provides subtitleColorAnimation.value,
                            ) {
                                subtitle?.invoke()
                            }
                        }
                    }
                }
            },
            titleVerticalArrangement = Arrangement.Center,
            titleHorizontalArrangement = if (isCenterAligned) Arrangement.Center else Arrangement.Start,
            titleBottomPadding = LocalDensity.current.run { 16.dp.roundToPx() },
            hideTitleSemantics = false,
            navigationIcon = navigationIcon,
            actions = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    content = actions,
                )
            },
        )
    }
}

@Composable
private fun TopAppBarLayout(
    modifier: Modifier,
    heightPx: Float,
    navigationIconContentColor: Color,
    actionIconContentColor: Color,
    title: @Composable () -> Unit,
    titleVerticalArrangement: Arrangement.Vertical,
    titleHorizontalArrangement: Arrangement.Horizontal,
    titleBottomPadding: Int,
    hideTitleSemantics: Boolean,
    navigationIcon: @Composable () -> Unit,
    actions: @Composable () -> Unit,
) {
    Layout(
        {
            Box(
                Modifier.layoutId("navigationIcon"),
            ) {
                CompositionLocalProvider(
                    LocalContentColor provides navigationIconContentColor,
                    content = navigationIcon,
                )
            }
            Box(
                modifier =
                    Modifier
                        .layoutId("title")
                        .padding(horizontal = TopAppBarHorizontalPadding)
                        .then(if (hideTitleSemantics) Modifier.clearAndSetSemantics { } else Modifier),
            ) {
                title()
            }
            Box(
                modifier =
                    Modifier
                        .layoutId("actionIcons")
                        .padding(end = TopAppBarHorizontalPadding),
            ) {
                CompositionLocalProvider(
                    LocalContentColor provides actionIconContentColor,
                    content = actions,
                )
            }
        },
        modifier = modifier,
    ) { measurables, constraints ->
        val zeroConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val navigationIconPlaceable = measurables
            .firstOrNull { it.layoutId == "navigationIcon" }
            ?.measure(zeroConstraints)
        val actionIconsPlaceable = measurables
            .firstOrNull { it.layoutId == "actionIcons" }
            ?.measure(zeroConstraints)

        val navIconWidth = navigationIconPlaceable?.width ?: 0
        val navIconHeight = navigationIconPlaceable?.height ?: 0
        val actionIconsWidth = actionIconsPlaceable?.width ?: 0
        val actionIconsHeight = actionIconsPlaceable?.height ?: 0

        val maxTitleWidth =
            if (constraints.maxWidth == Constraints.Infinity) {
                constraints.maxWidth
            } else {
                 val actionReservedWidth = actionIconsWidth + TopAppBarHorizontalPadding.roundToPx() * 2
                (constraints.maxWidth - navIconWidth - actionReservedWidth)
                    .coerceAtLeast(0)
            }

        val layoutHeight = heightPx.roundToInt()

        val titlePlaceable = measurables
            .firstOrNull { it.layoutId == "title" }
            ?.measure(constraints.copy(minWidth = 0, maxWidth = maxTitleWidth))

        val titleWidth = titlePlaceable?.width ?: 0
        val titleHeight = titlePlaceable?.height ?: 0
        val titleBaseline =
            if (titlePlaceable != null && titlePlaceable[LastBaseline] != AlignmentLine.Unspecified) {
                titlePlaceable[LastBaseline]
            } else {
                0
            }

        layout(constraints.maxWidth, layoutHeight) {
            navigationIconPlaceable?.placeRelative(
                x = 0,
                y = (layoutHeight - navIconHeight) / 2,
            )
            val safeMargin = 4.dp.roundToPx()
            titlePlaceable?.placeRelative(
                x =
                    when (titleHorizontalArrangement) {
                        Arrangement.Center -> if (constraints.maxWidth / 2 - (TopAppBarHorizontalPadding.roundToPx() + actionIconsWidth + safeMargin) >= titleWidth / 2) {
                            (constraints.maxWidth - titleWidth) / 2
                        } else {
                            constraints.maxWidth - (TopAppBarHorizontalPadding.roundToPx() + actionIconsWidth + safeMargin) - titleWidth
                        }
                        Arrangement.End ->
                            constraints.maxWidth - titleWidth - actionIconsWidth

                        else -> max(TopAppBarTitleInset.roundToPx(), navIconWidth)
                    },
                y =
                    when (titleVerticalArrangement) {
                        Arrangement.Center -> (layoutHeight - titleHeight) / 2

                        Arrangement.Bottom ->
                            if (titleBottomPadding == 0) {
                                layoutHeight - titleHeight
                            } else {
                                layoutHeight - titleHeight -
                                    max(
                                        0,
                                        titleBottomPadding - titleHeight + titleBaseline,
                                    )
                            }

                        else -> 0
                    },
            )

            actionIconsPlaceable?.placeRelative(
                x = constraints.maxWidth - actionIconsWidth,
                y = (layoutHeight - actionIconsHeight) / 2,
            )
        }
    }
}

private val TopAppBarHorizontalPadding = 4.dp
private val TopAppBarHeight = 44.dp

private val TopAppBarTitleInset = 16.dp - TopAppBarHorizontalPadding

@Immutable
object CupertinoTopAppBarDefaults {

    val windowInsets: WindowInsets @Composable get() = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)

    @Composable
    @ReadOnlyComposable
    fun topAppBarColors(
        lightGradientColor: Color = lightColorScheme().tertiarySystemBackground,
        darkGradientColor: Color = darkColorScheme().tertiarySystemBackground,
        navigationIconContentColor: Color = CupertinoTheme.colorScheme.accent,
        lightTitleContentColor: Color = lightColorScheme().label,
        darkTitleContentColor: Color = darkColorScheme().label,
        lightSubtitleContentColor: Color = lightColorScheme().secondaryLabel,
        darkSubtitleContentColor: Color = darkColorScheme().secondaryLabel,
        actionIconContentColor: Color = CupertinoTheme.colorScheme.accent,
    ): CupertinoTopAppBarColors =
        CupertinoTopAppBarColors(
            lightGradientColor,
            darkGradientColor,
            navigationIconContentColor,
            lightTitleContentColor,
            darkTitleContentColor,
            lightSubtitleContentColor,
            darkSubtitleContentColor,
            actionIconContentColor,
        )
}
