package pushkar.chorus.music.ui.fluid.foundation

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils

fun Color.isHueless(): Boolean {
    if (!isSpecified) return true
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(toArgb(), hsl)
    return hsl[1] < 0.08f
}

object GlassTintTokens {
    val SurfaceBaseDark = Color(0xFFFFFFFF)
    val SurfaceBaseLight = Color(0xFFFFFFFF)

    val TintNeutralDark = Color(0xFFFFFFFF).copy(alpha = 0f)
    val TintNeutralLight = Color(0xFFFFFFFF).copy(alpha = 0.12f)

    const val HeroTintOpacityDark = 0f
    const val HeroTintOpacityLight = 0.20f

    val AccentSelectedBlueDark = Color(0xFF0A84FF)
    val AccentSelectedBlueLight = Color(0xFF007AFF)

    val DestructiveRedDark = Color(0xFFFF453A)
    val DestructiveRedLight = Color(0xFFFF3B30)

    val IconOpaqueBlackishLight = Color(0xFF111111)
    val TitleOpaqueBlackishLight = Color(0xFF000000)
    val TabLabelOpaqueBlackishLight = Color(0xFF1C1C1E)
    val PluckDefaultDark = Color(0xFFFFFFFF)
    val PluckDefaultLight = Color(0xFFFFFFFF)
    const val PluckAlphaNeutralDark = 0.30f
    const val PluckAlphaNeutralLight = 0.45f
    const val PluckAlphaHeroDark = 0.48f
    const val PluckAlphaHeroLight = 0.55f

    val BorderDark = Color(0xFFFFFFFF).copy(alpha = 0.14f)
    val BorderLight = Color(0xFF000000).copy(alpha = 0.08f)

    const val IconDimOpacity = 0.85f
    const val TitleDimOpacity = 0.92f
    const val UnselectedTabLabelOpacity = 0.75f
    const val SelectedTabLabelOpacity = 0.92f
    const val DragHandleOpacity = 0.30f
    const val SecondaryLabelAlpha = 0.60f

    val DefaultSurfaceDark = Color(0xFFFFFFFF)
    const val DefaultSurfaceOpacityDark = 0.12f
    val DefaultPuckDark = Color(0xFFFFFFFF)
    const val DefaultPuckOpacityDark = 0.20f
    val DefaultHighlightDark = Color(0xFFFFFFFF)
    const val DefaultHighlightOpacityDark = 0.35f
    val DefaultSurfaceLight = Color(0xFFFFFFFF)
    const val DefaultSurfaceOpacityLight = 0.35f
    val DefaultPuckLight = Color(0xFFFFFFFF)
    const val DefaultPuckOpacityLight = 0.45f
    val DefaultHighlightLight = Color(0xFFFFFFFF)
    const val DefaultHighlightOpacityLight = 0.55f

    const val NavVibrancy = 2.0f
    const val NavBlurDp = 3f
    const val NavLensHeight = 0.60f
    const val NavLensAmount = 0.69f
    const val OtherLensHeight = 0.35f
    const val OtherLensAmount = 0.40f

    const val BaseSurfaceOpacityDark = 0.12f
    const val BaseSurfaceOpacityLight = 0.28f

    const val NavPillFillOpacityDark = 0.15f
    const val NavPillFillOpacityLight = 0.18f

    const val SearchFieldFillOpacityDark = 0.14f
    const val SearchFieldFillOpacityLight = 0.18f

    const val PillSelectedFillOpacityDark = 0.22f
    const val PillSelectedFillOpacityLight = 0.25f

    val NavBarBlurDark: Dp = 3.dp
    val NavBarBlurLight: Dp = 3.dp
    val TabBarBlurDark: Dp = 3.dp
    val TabBarBlurLight: Dp = 3.dp
    val SheetBlurDark: Dp = 14.dp
    val SheetBlurLight: Dp = 12.dp

    const val NavBarSaturationDark = 1.70f
    const val NavBarSaturationLight = 1.50f

    val ShadowColorDark = Color(0x59000000)
    val ShadowColorLight = Color(0x1F000000)

    fun glassBaseColor(isDark: Boolean): Color = if (isDark) SurfaceBaseDark else SurfaceBaseLight

    fun baseSurfaceColor(isDark: Boolean): Color =
        if (isDark) SurfaceBaseDark else SurfaceBaseLight

    fun neutralTintColor(isDark: Boolean): Color =
        if (isDark) TintNeutralDark else TintNeutralLight

    fun heroTintOpacity(isDark: Boolean): Float =
        if (isDark) HeroTintOpacityDark else HeroTintOpacityLight

    fun selectedRedColor(isDark: Boolean): Color =
        if (isDark) DestructiveRedDark else DestructiveRedLight

    fun selectedColor(isDark: Boolean, activeTint: Color = Color.Unspecified): Color =
        if (activeTint.isSpecified && !activeTint.isHueless()) activeTint
        else if (isDark) AccentSelectedBlueDark else AccentSelectedBlueLight

    fun borderColor(isDark: Boolean): Color =
        if (isDark) BorderDark else BorderLight

    fun navBarBlur(isDark: Boolean): Dp =
        if (isDark) NavBarBlurDark else NavBarBlurLight

    fun tabBarBlur(isDark: Boolean): Dp =
        if (isDark) TabBarBlurDark else TabBarBlurLight

    fun navBarSaturation(isDark: Boolean): Float =
        if (isDark) NavBarSaturationDark else NavBarSaturationLight

    fun pluckColor(
        isDark: Boolean,
        ambientColor: Color = Color.Unspecified,
        rimTintColor: Color = Color.Unspecified,
    ): Color {
        return when {
            rimTintColor.isSpecified -> rimTintColor
            ambientColor.isSpecified && !ambientColor.isHueless() -> {
                val hsl = FloatArray(3)
                ColorUtils.colorToHSL(ambientColor.toArgb(), hsl)
                hsl[1] = hsl[1].coerceIn(0.40f, 0.55f)
                hsl[2] = if (isDark) hsl[2].coerceIn(0.62f, 0.74f) else hsl[2].coerceIn(0.75f, 0.90f)
                Color(ColorUtils.HSLToColor(hsl))
            }

            else -> if (isDark) PluckDefaultDark else PluckDefaultLight
        }
    }

    fun pluckOpacity(isDark: Boolean, isHeroContext: Boolean): Float {
        return if (isDark) {
            if (isHeroContext) PluckAlphaHeroDark else PluckAlphaNeutralDark
        } else {
            if (isHeroContext) PluckAlphaHeroLight else PluckAlphaNeutralLight
        }
    }

    fun puckOpacity(isDark: Boolean, isHeroContext: Boolean): Float {
        return if (isDark) {
            if (isHeroContext) 0.80f else 0.55f
        } else {
            if (isHeroContext) 0.70f else 0.45f
        }
    }

    fun glassContentColor(
        isDark: Boolean,
        activeTint: Color = Color.Unspecified,
        isSelected: Boolean = false,
        isTitle: Boolean = false,
        isUnselectedTabLabel: Boolean = false,
        overrideRedForSelection: Boolean = true,
    ): Color {
        if (isSelected && overrideRedForSelection) {
            val tint = selectedColor(isDark, activeTint)
            val alpha = if (isDark) {
                if (isTitle || isUnselectedTabLabel) SelectedTabLabelOpacity else IconDimOpacity
            } else {
                1.0f
            }
            return tint.copy(alpha = alpha)
        }

        if (!isDark) {
            return when {
                isTitle -> TitleOpaqueBlackishLight
                isUnselectedTabLabel -> TabLabelOpaqueBlackishLight
                else -> IconOpaqueBlackishLight
            }
        }

        val dimAlpha = when {
            isTitle -> TitleDimOpacity
            isUnselectedTabLabel -> UnselectedTabLabelOpacity
            else -> IconDimOpacity
        }

        val effectiveTint = if (activeTint.isSpecified && !activeTint.isHueless()) {
            activeTint
        } else {
            neutralTintColor(isDark = true)
        }

        val shifted = if (effectiveTint.isSpecified && !effectiveTint.isHueless()) {
            val hsl = FloatArray(3)
            ColorUtils.colorToHSL(effectiveTint.toArgb(), hsl)
            val targetHsl = FloatArray(3)
            targetHsl[0] = hsl[0]
            targetHsl[1] = hsl[1].coerceIn(0.18f, 0.38f)
            targetHsl[2] = if (isTitle) 0.96f else 0.92f
            Color(ColorUtils.HSLToColor(targetHsl))
        } else {
            if (isTitle) Color(0xFFFFFFFF) else Color(0xFFEBEBF5)
        }

        return shifted.copy(alpha = dimAlpha)
    }
}
