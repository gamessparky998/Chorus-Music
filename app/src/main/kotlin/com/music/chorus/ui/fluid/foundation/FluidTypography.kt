package pushkar.chorus.music.ui.fluid.foundation

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Immutable
data class FluidTypography(
    val largeTitle: TextStyle,
    val title1: TextStyle,
    val title2: TextStyle,
    val title3: TextStyle,
    val headline: TextStyle,
    val body: TextStyle,
    val callout: TextStyle,
    val subheadline: TextStyle,
    val footnote: TextStyle,
    val caption1: TextStyle,
    val caption2: TextStyle,
)

fun fluidTypography(fontFamily: FontFamily = FontFamily.Default): FluidTypography {
    fun style(size: Int, lineHeight: Int, weight: FontWeight, letterSpacing: Float = 0f) = TextStyle(
        fontFamily = fontFamily,
        fontSize = size.sp,
        lineHeight = lineHeight.sp,
        fontWeight = weight,
        letterSpacing = letterSpacing.sp,
    )

    return FluidTypography(
        largeTitle = style(34, 41, FontWeight.Bold, 0.37f),
        title1 = style(28, 34, FontWeight.Bold, 0.36f),
        title2 = style(22, 28, FontWeight.Bold, 0.35f),
        title3 = style(20, 25, FontWeight.SemiBold, 0.38f),
        headline = style(17, 22, FontWeight.SemiBold, -0.41f),
        body = style(17, 22, FontWeight.Normal, -0.41f),
        callout = style(16, 21, FontWeight.Normal, -0.32f),
        subheadline = style(15, 20, FontWeight.Normal, -0.24f),
        footnote = style(13, 18, FontWeight.Normal, -0.08f),
        caption1 = style(12, 16, FontWeight.Normal, 0f),
        caption2 = style(11, 13, FontWeight.Normal, 0.07f),
    )
}
