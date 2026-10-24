package pushkar.chorus.music.ui.fluid.foundation

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Stable
fun fluidColorScheme(colors: FluidColors): ColorScheme {
    val base = if (colors.isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = colors.accent,
        onPrimary = colors.onAccent,
        primaryContainer = colors.accent.copy(alpha = 0.18f),
        onPrimaryContainer = colors.accent,
        inversePrimary = colors.accent,

        secondary = colors.accent,
        onSecondary = colors.onAccent,
        secondaryContainer = colors.secondarySystemFill,
        onSecondaryContainer = colors.label,

        tertiary = colors.systemIndigo,
        onTertiary = colors.onAccent,
        tertiaryContainer = colors.tertiarySystemFill,
        onTertiaryContainer = colors.label,

        background = colors.systemGroupedBackground,
        onBackground = colors.label,

        surface = colors.systemBackground,
        onSurface = colors.label,
        surfaceVariant = colors.secondarySystemGroupedBackground,
        onSurfaceVariant = colors.secondaryLabel,
        surfaceTint = colors.accent,
        inverseSurface = colors.label,
        inverseOnSurface = colors.systemBackground,

        surfaceDim = colors.systemGroupedBackground,
        surfaceBright = colors.systemBackground,
        surfaceContainerLowest = colors.systemBackground,
        surfaceContainerLow = colors.secondarySystemGroupedBackground,
        surfaceContainer = colors.secondarySystemGroupedBackground,
        surfaceContainerHigh = colors.tertiarySystemGroupedBackground,
        surfaceContainerHighest = colors.tertiarySystemGroupedBackground,

        error = colors.systemRed,
        onError = colors.onAccent,
        errorContainer = colors.systemRed.copy(alpha = 0.18f),
        onErrorContainer = colors.systemRed,

        outline = colors.separator,
        outlineVariant = colors.opaqueSeparator,
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
