package pushkar.chorus.music.ui.theme

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import com.materialkolor.score.Score

import pushkar.chorus.music.constants.AppThemeMode
import pushkar.chorus.music.ui.fluid.foundation.DefaultFluidAccent
import pushkar.chorus.music.ui.fluid.foundation.FluidTheme

val LocalAppThemeMode = androidx.compose.runtime.compositionLocalOf { AppThemeMode.MaterialUI }

@Composable
@androidx.compose.runtime.ReadOnlyComposable
fun isFluidTheme(): Boolean = LocalAppThemeMode.current == AppThemeMode.FluidUI

val DefaultThemeColor = Color.Black

@Composable
fun chorusmusicTheme(
    themeMode: AppThemeMode = AppThemeMode.MaterialUI,
    darkTheme: Boolean = isSystemInDarkTheme(),
    pureBlack: Boolean = true,
    themeColor: Color = DefaultThemeColor,
    content: @Composable () -> Unit,
) {
    androidx.compose.runtime.CompositionLocalProvider(
        LocalAppThemeMode provides themeMode
    ) {
        if (themeMode == AppThemeMode.FluidUI) {
            val fluidAccent = if (themeColor != DefaultThemeColor) {
                themeColor
            } else {
                if (darkTheme) Color(0xFFF2F2F7) else Color(0xFF1C1C1E)
            }
            FluidTheme(
                darkTheme = darkTheme,
                accent = fluidAccent,
                content = content
            )
        } else {
            val context = LocalContext.current
        val useSystemDynamicColor = false

        val baseColorScheme = if (useSystemDynamicColor && themeColor != DefaultThemeColor) {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            val style = if (themeColor == DefaultThemeColor) PaletteStyle.Monochrome else PaletteStyle.TonalSpot
            rememberDynamicColorScheme(
                seedColor = themeColor,
                isDark = darkTheme,
                specVersion = ColorSpec.SpecVersion.SPEC_2025,
                style = style
            )
        }

        val colorScheme = remember(baseColorScheme, pureBlack, darkTheme) {
            if (darkTheme) {
                baseColorScheme.pureBlack(true)
            } else {
                baseColorScheme
            }
        }

        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content
        )
    }
    }
}

fun Bitmap.extractThemeColor(): Color {
    val colorsToPopulation = Palette.from(this)
        .maximumColorCount(8)
        .generate()
        .swatches
        .associate { it.rgb to it.population }
    val rankedColors = Score.score(colorsToPopulation)
    return Color(rankedColors.first())
}

fun Bitmap.extractGradientColors(): List<Color> {
    val extractedColors = Palette.from(this)
        .maximumColorCount(64)
        .generate()
        .swatches
        .associate { it.rgb to it.population }

    val orderedColors = Score.score(extractedColors, 2, 0xff4285f4.toInt(), true)
        .sortedByDescending { Color(it).luminance() }

    return if (orderedColors.size >= 2)
        listOf(Color(orderedColors[0]), Color(orderedColors[1]))
    else
        listOf(Color(0xFF595959), Color(0xFF0D0D0D))
}

fun ColorScheme.pureBlack(apply: Boolean) =
    if (apply) copy(
        surface = Color.Black,
        background = Color.Black
    ) else this

val ColorSaver = object : Saver<Color, Int> {
    override fun restore(value: Int): Color = Color(value)
    override fun SaverScope.save(value: Color): Int = value.toArgb()
}
