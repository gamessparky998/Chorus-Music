package pushkar.chorus.music.ui.fluid.foundation

import android.app.ActivityManager
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import pushkar.chorus.music.ui.component.backdrop.Backdrop
import pushkar.chorus.music.ui.component.backdrop.BackdropEffectScope
import pushkar.chorus.music.ui.component.backdrop.backdrops.LayerBackdrop
import pushkar.chorus.music.ui.component.backdrop.drawBackdrop
import pushkar.chorus.music.ui.component.backdrop.effects.blur
import pushkar.chorus.music.ui.component.backdrop.effects.colorControls
import pushkar.chorus.music.ui.component.backdrop.effects.lens
import pushkar.chorus.music.ui.component.backdrop.highlight.Highlight
import pushkar.chorus.music.ui.component.backdrop.highlight.HighlightStyle
import pushkar.chorus.music.ui.component.backdrop.isRenderEffectSupported
import pushkar.chorus.music.ui.component.backdrop.poolLens
import pushkar.chorus.music.ui.component.backdrop.shadow.Shadow
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

enum class GlassIntensity(val blurRadius: Float, val highlightAlpha: Float) {
    Subtle(blurRadius = 1.5f, highlightAlpha = 0.35f),
    Standard(blurRadius = 3f, highlightAlpha = 0.55f),
    Prominent(blurRadius = 5f, highlightAlpha = 0.75f)
}

@Stable
data class GlassEffectConfig(
    val globalEnabled: Boolean = true,
    val vibrancy: Float = 2.0f,
    val blurRadius: Float = 3f,
    val lensHeight: Float = 0.60f,
    val lensAmount: Float = 0.69f,
    val chromaticAberration: Boolean = false,
    val depthEffect: Boolean = false,
    val surfaceTintColor: Color = Color.Unspecified,
    val highlightColor: Color = Color.Unspecified,
    val highlightOpacity: Float = EdgeHighlightAlpha,
    val style: GlassStyle = GlassStyle.LIQUID,
    val puckColor: Color = Color.Unspecified,
    val puckOpacity: Float = 0.8f,
    val surfaceOpacity: Float = 0.5f,
    val textColor: Color = Color.Unspecified,
    val miniPlayerEnabled: Boolean = true,
    val navBarEnabled: Boolean = true,
    val sidePanelEnabled: Boolean = true,
    val sidePanelVibrancy: Float = 1.2f,
    val sidePanelBlurRadius: Float = 2f,
    val sidePanelLensHeight: Float = 0.4f,
    val sidePanelLensAmount: Float = 0.6f,
    val sidePanelColor: Color = Color.Unspecified,
    val sidePanelSurfaceOpacity: Float = 0.75f,
    val sidePanelTextColor: Color = Color.Unspecified,
) {
    fun forSidePanel(isDark: Boolean? = null): GlassEffectConfig = copy(
        vibrancy = sidePanelVibrancy,
        blurRadius = sidePanelBlurRadius,
        lensHeight = sidePanelLensHeight,
        lensAmount = sidePanelLensAmount,
        surfaceTintColor = if (sidePanelColor.isSpecified || isDark == null) sidePanelColor
        else if (isDark) Color.Black else Color.White,
        surfaceOpacity = sidePanelSurfaceOpacity,
        textColor = sidePanelTextColor,
    )

    fun forAccessory(): GlassEffectConfig = this

    fun isEnabledFor(component: GlassComponent): Boolean =
        globalEnabled && when (component) {
            GlassComponent.MINI_PLAYER -> miniPlayerEnabled
            GlassComponent.NAV_BAR -> navBarEnabled
            GlassComponent.SIDE_PANEL -> sidePanelEnabled
        }

    val anyComponentEnabled: Boolean
        get() = globalEnabled
}

enum class GlassStyle {
    LIQUID,
    BLUR,
    TRANSPARENT,
}

enum class GlassComponent {
    MINI_PLAYER,
    NAV_BAR,
    SIDE_PANEL,
}

fun shouldUseTranslucentGlassFallback(style: GlassStyle, renderEffectSupported: Boolean): Boolean =
    style == GlassStyle.TRANSPARENT || !renderEffectSupported

private var isPopNav = false
fun markMorphDirection(isPop: Boolean) {
    isPopNav = isPop
}

fun lastNavWasPop(): Boolean = isPopNav

internal const val LENS_MAX_DP = 48f
internal const val MIN_GLASS_RESOLUTION_SCALE = 0.30f
internal const val FULL_QUALITY_BLUR_DP = 2.5f

fun glassResolutionScale(blurRadiusDp: Float): Float {
    val t = (blurRadiusDp / FULL_QUALITY_BLUR_DP).coerceIn(0f, 1f)
    return 1f - t * (1f - MIN_GLASS_RESOLUTION_SCALE)
}

fun isGlassSupported(sdkInt: Int = Build.VERSION.SDK_INT): Boolean = sdkInt >= Build.VERSION_CODES.S

@Composable
fun isLowRamDevice(): Boolean {
    val context = LocalContext.current
    return remember {
        context.getSystemService(ActivityManager::class.java)?.isLowRamDevice ?: false
    }
}

@Composable
fun isGlassAllowed(): Boolean = isGlassSupported() && !isLowRamDevice()

fun glassSaturation(vibrancy: Float): Float = 1f + 0.5f * vibrancy.coerceIn(0f, 2f)

fun glassContentColorFor(behind: Color, tint: Color, opacity: Float): Color {
    val effective = if (tint.isSpecified) {
        lerp(behind, tint, opacity.coerceIn(0f, 1f))
    } else {
        behind
    }
    return if (effective.luminance() > 0.5f) Color(0xFF1A1A1A) else Color.White
}

internal val EdgeHighlightWidth = 0.8f.dp
private const val EdgeHighlightAlpha = 0.55f
internal const val HighlightAngleFrozen = 45f

val LocalGlassEffectConfig = staticCompositionLocalOf { GlassEffectConfig() }
val LocalSampledChromeDark = compositionLocalOf<Boolean?> { null }

private val ambientBackgroundColorState = mutableStateOf(Color.Unspecified)
val LocalAmbientBackgroundColor = staticCompositionLocalOf { ambientBackgroundColorState }

private val ambientDarkChromeState = mutableStateOf(false)
val LocalAmbientDarkChrome = staticCompositionLocalOf { ambientDarkChromeState }

@Composable
fun PublishAmbientBackgroundColor(color: Color, darkChrome: Boolean = false) {
    val ambient = LocalAmbientBackgroundColor.current
    val ambientDark = LocalAmbientDarkChrome.current
    val owner = LocalLifecycleOwner.current
    val latest by rememberUpdatedState(color)
    val latestDark by rememberUpdatedState(darkChrome)
    LaunchedEffect(color, darkChrome) {
        if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            ambient.value = color
            ambientDark.value = darkChrome
        }
    }
    DisposableEffect(owner) {
        onDispose {
            if (ambient.value == latest) ambient.value = Color.Unspecified
            if (ambientDark.value == latestDark) ambientDark.value = false
        }
    }
}

@Composable
fun glassUsesDarkAppearance(): Boolean =
    LocalSampledChromeDark.current
        ?: (LocalAmbientDarkChrome.current.value ||
                androidx.compose.material3.MaterialTheme.colorScheme.surface.luminance() <= 0.5f)

fun GlassEffectConfig.withFixedStyle(): GlassEffectConfig = copy(globalEnabled = true)

@Composable
fun GlassEffectConfig.resolvedTextColor(
    activeTint: Color = Color.Unspecified,
    isSelected: Boolean = false,
    isTitle: Boolean = false,
    isUnselectedTabLabel: Boolean = false,
): Color {
    if (textColor.isSpecified) return textColor
    val isDark = glassUsesDarkAppearance()
    val ambient = if (activeTint.isSpecified) activeTint else LocalAmbientBackgroundColor.current.value
    val effectiveDark = LocalSampledChromeDark.current ?: (isDark || (ambient.isSpecified && !ambient.isHueless()))
    return GlassTintTokens.glassContentColor(
        isDark = effectiveDark,
        activeTint = ambient,
        isSelected = isSelected,
        isTitle = isTitle,
        isUnselectedTabLabel = isUnselectedTabLabel,
    )
}

val LocalAppBackdrop = staticCompositionLocalOf<Backdrop?> { null }

@Stable
class PooledGlass(
    val backdrop: LayerBackdrop,
    val blurRadiusDp: Float,
    val vibrancy: Float,
)

val LocalPooledGlass = staticCompositionLocalOf<PooledGlass?> { null }
val LocalAppBackdropFrozen = staticCompositionLocalOf<() -> Boolean> { { false } }
val LocalAppMotionFrozen = staticCompositionLocalOf<State<Boolean>> {
    object : State<Boolean> {
        override val value: Boolean get() = false
    }
}
val LocalBackdropLoopBucket = staticCompositionLocalOf<(() -> Int)?> { null }

fun Modifier.outerDarkRim(
    shape: Shape,
    width: Dp = 1.6.dp,
    alpha: Float = 0.32f,
    angle: Float = 45f,
    insideOnly: Boolean = false,
): Modifier = drawWithCache {
    if (size.width <= 0f || size.height <= 0f) {
        return@drawWithCache onDrawWithContent { drawContent() }
    }
    val inset = if (insideOnly) width.toPx() / 4f else 0f
    val targetWidth = (size.width - inset * 2f).coerceAtLeast(0f)
    val targetHeight = (size.height - inset * 2f).coerceAtLeast(0f)
    if (targetWidth <= 0f || targetHeight <= 0f) {
        return@drawWithCache onDrawWithContent { drawContent() }
    }
    val outline = try {
        shape.createOutline(
            androidx.compose.ui.geometry.Size(targetWidth, targetHeight),
            layoutDirection,
            this,
        )
    } catch (_: Throwable) {
        return@drawWithCache onDrawWithContent { drawContent() }
    }
    val stroke = Stroke(width = if (insideOnly) width.toPx() / 2f else width.toPx())
    val center = Offset(size.width / 2f, size.height / 2f)
    fun flankBrush(axisDeg: Float): Brush {
        val a = axisDeg * (PI / 180f).toFloat()
        val dir = Offset(cos(a), sin(a))
        val reach = dir * (abs(dir.x) * size.width + abs(dir.y) * size.height) / 2f
        return Brush.linearGradient(
            0f to Color.Black.copy(alpha = alpha),
            0.5f to Color.Transparent,
            1f to Color.Black.copy(alpha = alpha),
            start = center - reach,
            end = center + reach,
        )
    }

    val firstBrush = flankBrush(angle)
    val secondBrush = flankBrush(angle + 90f)
    onDrawWithContent {
        drawContent()
        translate(inset, inset) {
            drawOutline(outline = outline, brush = firstBrush, style = stroke)
            drawOutline(outline = outline, brush = secondBrush, style = stroke)
        }
    }
}

@Composable
fun Modifier.standaloneGlassPlate(
    shape: Shape,
    isDark: Boolean,
    intensity: GlassIntensity,
    tintColor: Color = Color.Unspecified,
    tintAlpha: Float = Float.NaN,
    rimTintColor: Color = Color.Unspecified,
    applyEdgeEffects: Boolean = true,
): Modifier = this
    .clip(shape)
    .drawWithCache {
        val outline = try {
            shape.createOutline(size, layoutDirection, this)
        } catch (_: Throwable) {
            null
        }

        val alphaFactor = when (intensity) {
            GlassIntensity.Subtle -> 0.35f
            GlassIntensity.Standard -> 0.50f
            GlassIntensity.Prominent -> 0.65f
        }

        val effectiveTint = when {
            tintColor.isSpecified && !tintColor.isHueless() -> tintColor
            rimTintColor.isSpecified && !rimTintColor.isHueless() -> rimTintColor
            else -> Color.Unspecified
        }

        val glassBaseBrush = if (isDark) {
            val baseTop = if (effectiveTint.isSpecified) lerp(Color(0xFF444454), effectiveTint, 0.22f) else Color(0xFF383844)
            val baseMid = if (effectiveTint.isSpecified) lerp(Color(0xFF2E2E3A), effectiveTint, 0.16f) else Color(0xFF282832)
            val baseBottom = if (effectiveTint.isSpecified) lerp(Color(0xFF20202A), effectiveTint, 0.12f) else Color(0xFF1C1C24)

            Brush.linearGradient(
                0.0f to baseTop.copy(alpha = 0.38f * alphaFactor),
                0.45f to baseMid.copy(alpha = 0.48f * alphaFactor),
                1.0f to baseBottom.copy(alpha = 0.58f * alphaFactor),
                start = Offset.Zero,
                end = Offset(size.width, size.height),
            )
        } else {
            val baseTop = if (effectiveTint.isSpecified) lerp(Color(0xFFFFFFFF), effectiveTint, 0.12f) else Color(0xFFFFFFFF)
            val baseMid = if (effectiveTint.isSpecified) lerp(Color(0xFFF6F6FC), effectiveTint, 0.16f) else Color(0xFFF7F7FA)
            val baseBottom = if (effectiveTint.isSpecified) lerp(Color(0xFFEBEBF5), effectiveTint, 0.18f) else Color(0xFFEBEBF2)

            Brush.linearGradient(
                0.0f to baseTop.copy(alpha = 0.55f * alphaFactor),
                0.5f to baseMid.copy(alpha = 0.45f * alphaFactor),
                1.0f to baseBottom.copy(alpha = 0.35f * alphaFactor),
                start = Offset.Zero,
                end = Offset(size.width, size.height),
            )
        }

        val tintBrush = if (effectiveTint.isSpecified) {
            val resolvedAlpha = if (!tintAlpha.isNaN()) tintAlpha else (if (isDark) 0.18f else 0.12f)
            Brush.linearGradient(
                0.0f to effectiveTint.copy(alpha = (resolvedAlpha * 1.15f).coerceIn(0f, 1f)),
                0.5f to effectiveTint.copy(alpha = (resolvedAlpha * 0.75f).coerceIn(0f, 1f)),
                1.0f to effectiveTint.copy(alpha = (resolvedAlpha * 0.35f).coerceIn(0f, 1f)),
                start = Offset.Zero,
                end = Offset(size.width, size.height),
            )
        } else null

        val sheenBrush = if (applyEdgeEffects) {
            Brush.linearGradient(
                0.0f to Color.White.copy(alpha = if (isDark) 0.35f else 0.50f),
                0.18f to Color.White.copy(alpha = if (isDark) 0.12f else 0.22f),
                0.45f to Color.White.copy(alpha = 0f),
                start = Offset.Zero,
                end = Offset(size.width * 0.45f, size.height * 0.65f),
            )
        } else null

        val strokeWidth = 1.2.dp.toPx()
        val outlineStroke = Stroke(width = strokeWidth)
        val rimBrush = if (applyEdgeEffects) {
            val topColor = if (effectiveTint.isSpecified) {
                lerp(Color.White, effectiveTint, 0.25f).copy(alpha = if (isDark) 0.45f else 0.62f)
            } else {
                Color.White.copy(alpha = if (isDark) 0.42f else 0.60f)
            }
            val midColor = Color.White.copy(alpha = if (isDark) 0.20f else 0.30f)
            val bottomColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.10f)

            Brush.linearGradient(
                0.0f to topColor,
                0.35f to midColor,
                1.0f to bottomColor,
                start = Offset.Zero,
                end = Offset(size.width, size.height),
            )
        } else null

        onDrawWithContent {
            if (outline != null) {
                drawOutline(outline, glassBaseBrush)
                if (tintBrush != null) drawOutline(outline, tintBrush)
                if (sheenBrush != null) drawOutline(outline, sheenBrush)
            } else {
                drawRect(glassBaseBrush)
                if (tintBrush != null) drawRect(tintBrush)
                if (sheenBrush != null) drawRect(sheenBrush)
            }

            drawContent()

            if (rimBrush != null && outline != null) {
                drawOutline(outline, rimBrush, style = outlineStroke)
            }
        }
    }

@Composable
fun Modifier.fluidGlass(
    config: GlassEffectConfig = LocalGlassEffectConfig.current,
    shape: Shape = RoundedCornerShape(0.dp),
    applyEdgeEffects: Boolean = true,
    intensity: GlassIntensity = GlassIntensity.Standard,
    blurRadiusDp: Float = if (intensity != GlassIntensity.Standard) intensity.blurRadius else config.blurRadius,
    highlightAlpha: Float = if (intensity != GlassIntensity.Standard) intensity.highlightAlpha else EdgeHighlightAlpha,
    backdropScale: Float = glassResolutionScale(blurRadiusDp),
    frozen: () -> Boolean = LocalAppBackdropFrozen.current,
    loopBucket: (() -> Int)? = LocalBackdropLoopBucket.current,
    rimTintColor: Color = Color.Unspecified,
    surfaceColor: Color = Color.Unspecified,
    surfaceAlpha: Float = Float.NaN,
    outerShadow: Boolean = true,
): Modifier {
    val pool = LocalPooledGlass.current
    val usePool = pool != null &&
            blurRadiusDp == pool.blurRadiusDp &&
            config.vibrancy == pool.vibrancy &&
            !config.chromaticAberration
    val backdrop = if (usePool) pool.backdrop else LocalAppBackdrop.current

    val isDark = glassUsesDarkAppearance()
    val explicitColor = surfaceColor.takeIf { it.isSpecified }
    val explicitAlpha = if (!surfaceAlpha.isNaN()) surfaceAlpha else (if (isDark) 0.22f else 0.14f)

    if (!isGlassAllowed() || backdrop == null) {
        val ambientColor = LocalAmbientBackgroundColor.current.value
        val resolvedTint = when {
            explicitColor != null -> explicitColor
            config.surfaceTintColor.isSpecified -> config.surfaceTintColor
            rimTintColor.isSpecified -> rimTintColor
            ambientColor.isSpecified && !ambientColor.isHueless() -> ambientColor
            else -> Color.Unspecified
        }
        val resolvedAlpha = if (explicitColor != null) explicitAlpha else config.surfaceOpacity.coerceIn(0f, 1f)

        return this.standaloneGlassPlate(
            shape = shape,
            isDark = isDark,
            intensity = intensity,
            tintColor = resolvedTint,
            tintAlpha = resolvedAlpha,
            rimTintColor = rimTintColor,
            applyEdgeEffects = applyEdgeEffects,
        )
    }
    val density = LocalDensity.current
    val resolutionScale = if (usePool) 1f else backdropScale.coerceIn(0.05f, 1f)
    val blurPx = with(density) { blurRadiusDp.dp.toPx() } * resolutionScale
    val saturation = glassSaturation(config.vibrancy)
    val lensHeightPx = with(density) { (config.lensHeight * LENS_MAX_DP).dp.toPx() } * resolutionScale
    val lensAmountPx = with(density) { (config.lensAmount * LENS_MAX_DP).dp.toPx() } * resolutionScale
    val ambientColor = LocalAmbientBackgroundColor.current.value
    val baseSurfaceColor = GlassTintTokens.glassBaseColor(isDark)
    val baseSurfaceOpacity = if (explicitColor != null || config.surfaceTintColor.isSpecified) 0f else if (isDark) {
        GlassTintTokens.BaseSurfaceOpacityDark
    } else {
        GlassTintTokens.BaseSurfaceOpacityLight
    }

    val isHeroContext = ambientColor.isSpecified && !ambientColor.isHueless()
    val tintColor = when {
        explicitColor != null -> explicitColor
        config.surfaceTintColor.isSpecified -> config.surfaceTintColor
        isHeroContext -> ambientColor
        else -> GlassTintTokens.neutralTintColor(isDark)
    }
    val tintOpacity = when {
        explicitColor != null -> explicitAlpha
        config.surfaceTintColor.isSpecified -> config.surfaceOpacity.coerceIn(0f, 1f)
        isHeroContext -> GlassTintTokens.heroTintOpacity(isDark)
        else -> 1f
    }
    val effectiveTintAlpha = (tintColor.alpha * tintOpacity).coerceIn(0f, 1f)

    val glassFade = tween<Color>(280)
    val animBaseColor =
        animateColorAsState(baseSurfaceColor.copy(alpha = baseSurfaceOpacity), glassFade, label = "glassBase")
    val animTintColor = animateColorAsState(tintColor.copy(alpha = effectiveTintAlpha), glassFade, label = "glassTint")

    if (shouldUseTranslucentGlassFallback(config.style, isRenderEffectSupported())) {
        return this.standaloneGlassPlate(
            shape = shape,
            isDark = isDark,
            intensity = intensity,
            tintColor = tintColor,
            tintAlpha = effectiveTintAlpha,
            rimTintColor = rimTintColor,
            applyEdgeEffects = applyEdgeEffects,
        )
    }

    val animatedHighlightAngle = HighlightAngleFrozen
    val shapeBlock: () -> Shape = remember(shape) { { shape } }
    val plainBlur = config.style == GlassStyle.BLUR

    val effectsBlock: BackdropEffectScope.() -> Unit = remember(
        saturation,
        blurPx,
        applyEdgeEffects,
        plainBlur,
        lensHeightPx,
        lensAmountPx,
        config.depthEffect,
        config.chromaticAberration,
        usePool,
    ) {
        {
            if (!usePool && saturation != 1f) {
                colorControls(saturation = saturation)
            }
            if (!usePool && blurPx > 0f) {
                blur(blurPx)
            }
            if (!plainBlur &&
                applyEdgeEffects &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                (lensHeightPx > 0f || lensAmountPx > 0f)
            ) {
                if (usePool) {
                    poolLens(
                        refractionHeight = lensHeightPx,
                        refractionAmount = lensAmountPx,
                        depthEffect = config.depthEffect,
                    )
                } else lens(
                    refractionHeight = lensHeightPx,
                    refractionAmount = lensAmountPx,
                    depthEffect = config.depthEffect,
                    chromaticAberration = config.chromaticAberration,
                )
            }
        }
    }

    val isHeroContextForPluck = (ambientColor.isSpecified && !ambientColor.isHueless()) || rimTintColor.isSpecified
    val rimColor = when {
        config.highlightColor.isSpecified -> config.highlightColor
        rimTintColor.isSpecified -> rimTintColor
        explicitColor != null -> explicitColor
        else -> GlassTintTokens.pluckColor(isDark, ambientColor)
    }
    val baseHighlightAlpha = when {
        highlightAlpha != EdgeHighlightAlpha -> highlightAlpha
        else -> GlassTintTokens.pluckOpacity(isDark, isHeroContextForPluck)
    }
    val rimAlpha = (baseHighlightAlpha * (config.highlightOpacity / EdgeHighlightAlpha)).coerceIn(0f, 1f)

    val animRim = animateColorAsState(rimColor.copy(alpha = rimAlpha), glassFade, label = "glassRim")
    val highlightBlock: (() -> Highlight?)? =
        remember(applyEdgeEffects, plainBlur, rimTintColor, animatedHighlightAngle) {
            if (applyEdgeEffects && !plainBlur) {
                {
                    Highlight(
                        width = EdgeHighlightWidth,
                        style = HighlightStyle.Default(
                            color = animRim.value,
                            angle = animatedHighlightAngle,
                        ),
                    )
                }
            } else {
                null
            }
        }

    val shadowBlock: (() -> Shadow?)? = remember(applyEdgeEffects, usePool, outerShadow) {
        if (applyEdgeEffects && !usePool && outerShadow) ({ Shadow.Default }) else null
    }

    val darkRimAlpha = (0.32f * (config.highlightOpacity / EdgeHighlightAlpha)).coerceIn(0f, 1f)

    val surfaceBlock: DrawScope.() -> Unit = remember(usePool, shape) {
        {
            if (size.width > 0f && size.height > 0f) {
                try {
                    val outline = shape.createOutline(size, layoutDirection, this)
                    val base = animBaseColor.value
                    if (base.alpha > 0f) {
                        drawOutline(outline, base)
                    }
                    val tint = animTintColor.value
                    if (tint.alpha > 0f) {
                        drawOutline(outline, tint)
                    }
                } catch (_: Throwable) {}
            }
        }
    }

    return drawBackdrop(
        backdrop = backdrop,
        shape = shapeBlock,
        effects = effectsBlock,
        highlight = highlightBlock,
        shadow = shadowBlock,
        onDrawSurface = surfaceBlock,
        backdropScale = resolutionScale,
        frozen = frozen,
        loopBucket = loopBucket,
    ).then(
        if (applyEdgeEffects && !plainBlur && darkRimAlpha > 0f && !isDark) {
            Modifier.outerDarkRim(
                shape = shape,
                width = 1.6.dp,
                alpha = darkRimAlpha,
                angle = animatedHighlightAngle,
                insideOnly = usePool,
            )
        } else {
            Modifier
        }
    )
}

@Composable
fun Modifier.liquidGlass(
    config: GlassEffectConfig = LocalGlassEffectConfig.current,
    shape: Shape = RoundedCornerShape(0.dp),
    applyEdgeEffects: Boolean = true,
    blurRadiusDp: Float = config.blurRadius,
    highlightAlpha: Float = EdgeHighlightAlpha,
    backdropScale: Float = glassResolutionScale(blurRadiusDp),
    frozen: () -> Boolean = LocalAppBackdropFrozen.current,
    loopBucket: (() -> Int)? = LocalBackdropLoopBucket.current,
    rimTintColor: Color = Color.Unspecified,
    outerShadow: Boolean = true,
): Modifier = fluidGlass(
    config = config,
    shape = shape,
    applyEdgeEffects = applyEdgeEffects,
    blurRadiusDp = blurRadiusDp,
    highlightAlpha = highlightAlpha,
    backdropScale = backdropScale,
    frozen = frozen,
    loopBucket = loopBucket,
    rimTintColor = rimTintColor,
    outerShadow = outerShadow,
)

@Composable
fun Modifier.fluidGlassSurface(
    shape: Shape,
    tint: Color = Color.Unspecified,
): Modifier = fluidGlass(shape = shape, rimTintColor = tint)
