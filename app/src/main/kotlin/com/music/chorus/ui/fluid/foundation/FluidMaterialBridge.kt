package pushkar.chorus.music.ui.fluid.foundation

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Stable
fun fluidColorScheme(colors: FluidColors): ColorScheme {
    val base = if (colors.isDark) darkColorScheme() else lightColorScheme()
    val accentColor = colors.accent
    val isDark = colors.isDark
    val textPrimary = if (isDark) Color.White else Color(0xFF1C1C1E)
    val textSecondary = if (isDark) Color(0x99EBEBF5) else Color(0x993C3C43)

    val dynamicGlassBase = if (isDark) Color(0xFF282832) else Color(0xFFF2F2F7)

    return base.copy(
        primary = accentColor,
        onPrimary = colors.onAccent,
        primaryContainer = accentColor.copy(alpha = if (isDark) 0.32f else 0.20f),
        onPrimaryContainer = if (isDark) Color.White else Color(0xFF141416),
        inversePrimary = colors.systemBackground,

        secondary = if (!accentColor.isHueless()) accentColor.copy(alpha = 0.85f) else textSecondary,
        onSecondary = colors.onAccent,
        secondaryContainer = dynamicGlassBase.copy(alpha = if (isDark) 0.55f else 0.65f),
        onSecondaryContainer = textPrimary,

        tertiary = colors.systemIndigo,
        onTertiary = colors.onAccent,
        tertiaryContainer = colors.tertiarySystemFill,
        onTertiaryContainer = textPrimary,

        background = colors.systemGroupedBackground,
        onBackground = textPrimary,

        surface = colors.systemBackground,
        onSurface = textPrimary,
        surfaceVariant = dynamicGlassBase.copy(alpha = if (isDark) 0.40f else 0.50f),
        onSurfaceVariant = textSecondary,
        surfaceTint = Color.Transparent,
        inverseSurface = textPrimary,
        inverseOnSurface = colors.systemBackground,

        surfaceDim = colors.systemGroupedBackground,
        surfaceBright = colors.systemBackground,
        surfaceContainerLowest = colors.systemBackground,
        surfaceContainerLow = dynamicGlassBase.copy(alpha = if (isDark) 0.45f else 0.55f),
        surfaceContainer = dynamicGlassBase.copy(alpha = if (isDark) 0.65f else 0.75f),
        surfaceContainerHigh = dynamicGlassBase.copy(alpha = if (isDark) 0.80f else 0.88f),
        surfaceContainerHighest = dynamicGlassBase.copy(alpha = if (isDark) 0.92f else 0.96f),

        error = colors.systemRed,
        onError = colors.onAccent,
        errorContainer = colors.systemRed.copy(alpha = 0.18f),
        onErrorContainer = colors.systemRed,

        outline = if (isDark) Color.White.copy(alpha = 0.20f) else Color.Black.copy(alpha = 0.15f),
        outlineVariant = if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.08f),
        scrim = Color.Black.copy(alpha = 0.32f),
    )
}

@Stable
fun fluidMaterialTypography(fontFamily: FontFamily = FontFamily.Default): Typography {
    val t = fluidTypography(fontFamily)

    fun display(size: Int, weight: FontWeight, tracking: Float) = TextStyle(
        fontFamily = fontFamily,
        fontSize = size.sp,
        lineHeight = (size * 1.2f).sp,
        fontWeight = weight,
        letterSpacing = tracking.sp,
    )

    return Typography(
        displayLarge = display(34, FontWeight.W800, -0.8f),
        displayMedium = display(30, FontWeight.W800, -0.7f),
        displaySmall = display(26, FontWeight.W700, -0.5f),
        headlineLarge = display(30, FontWeight.W800, -0.7f),
        headlineMedium = display(22, FontWeight.W700, -0.4f),
        headlineSmall = display(20, FontWeight.W700, -0.3f),
        titleLarge = display(20, FontWeight.W700, -0.3f),
        titleMedium = display(16, FontWeight.W600, -0.2f),
        titleSmall = t.subheadline,
        bodyLarge = display(16, FontWeight.W400, 0f),
        bodyMedium = display(14, FontWeight.W400, 0f),
        bodySmall = t.footnote,
        labelLarge = t.headline,
        labelMedium = display(12, FontWeight.W600, 0f),
        labelSmall = display(11, FontWeight.W600, 0f),
    )
}
