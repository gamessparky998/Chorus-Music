package pushkar.chorus.music.ui.fluid.foundation

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val LocalFluidColors = staticCompositionLocalOf { lightFluidColors() }
val LocalFluidTypography = staticCompositionLocalOf { fluidTypography() }
val LocalFluidTint = compositionLocalOf { Color.Unspecified }
val LocalFluidTextStyle = compositionLocalOf { fluidTypography().body }
val LocalFluidContentColor = compositionLocalOf { Color.Unspecified }

object FluidTheme {
    val colors: FluidColors
        @Composable @ReadOnlyComposable get() = LocalFluidColors.current

    val typography: FluidTypography
        @Composable @ReadOnlyComposable get() = LocalFluidTypography.current

    val tint: Color
        @Composable @ReadOnlyComposable get() = LocalFluidTint.current
            .takeIf { it != Color.Unspecified } ?: LocalFluidColors.current.accent

    val contentColor: Color
        @Composable @ReadOnlyComposable get() = LocalFluidContentColor.current
            .takeIf { it != Color.Unspecified } ?: LocalFluidColors.current.label
}

object FluidMetrics {
    val ListRowHeight: Dp = 44.dp
    val ListRowMinHeightWithSubtitle: Dp = 58.dp
    val ListRowInset: Dp = 16.dp
    val SectionSpacing: Dp = 35.dp
    val SeparatorThickness: Dp = 0.33.dp
    val GroupedCornerRadius: Dp = 26.dp
    val SheetCornerRadius: Dp = 38.dp
    val TabBarHeight: Dp = 56.dp
    val TabBarMargin: Dp = 12.dp
    val MenuCornerRadius: Dp = 14.dp
    val ButtonCornerRadius: Dp = 12.dp
    val IconTileSize: Dp = 29.dp
    val IconTileRadius: Dp = 7.dp
}

@Composable
fun FluidTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accent: Color = Color.Unspecified,
    typography: FluidTypography = fluidTypography(),
    content: @Composable () -> Unit,
) {
    val colors = remember(darkTheme, accent) {
        when {
            darkTheme && accent != Color.Unspecified -> darkFluidColors(accent)
            darkTheme -> darkFluidColors()
            accent != Color.Unspecified -> lightFluidColors(accent)
            else -> lightFluidColors()
        }
    }

    val materialScheme = remember(colors) { fluidColorScheme(colors) }
    val materialTypography = remember { fluidMaterialTypography() }

    CompositionLocalProvider(
        LocalFluidColors provides colors,
        LocalFluidTypography provides typography,
        LocalFluidTextStyle provides typography.body,
        LocalFluidContentColor provides colors.label,
    ) {
        MaterialTheme(
            colorScheme = materialScheme,
            typography = materialTypography,
        ) {
            content()
        }
    }
}
