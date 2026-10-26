package pushkar.chorus.music.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pushkar.chorus.music.ui.component.shapes.ContinuousRoundedRectangle
import pushkar.chorus.music.ui.fluid.foundation.BounceIndication
import pushkar.chorus.music.ui.fluid.foundation.GlassIntensity
import pushkar.chorus.music.ui.fluid.foundation.GlassTintTokens
import pushkar.chorus.music.ui.fluid.foundation.fluidGlass
import pushkar.chorus.music.ui.fluid.foundation.glassUsesDarkAppearance
import pushkar.chorus.music.ui.fluid.foundation.isGlassAllowed
import pushkar.chorus.music.ui.theme.isFluidTheme

val GlassCircleShape = ContinuousRoundedRectangle(percent = 50)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlassCircleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    size: Dp = 44.dp,
    shape: Shape = GlassCircleShape,
    rimTintColor: Color = Color.Unspecified,
    surfaceColor: Color = Color.Unspecified,
    surfaceAlpha: Float = Float.NaN,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val isFluid = isFluidTheme()
    val isDark = glassUsesDarkAppearance()
    val useGlass = isFluid && isGlassAllowed()

    val fallbackFill = GlassTintTokens.baseSurfaceColor(isDark).copy(
        alpha = if (isDark) GlassTintTokens.NavPillFillOpacityDark else GlassTintTokens.NavPillFillOpacityLight
    )

    val resolvedSurface = if (surfaceColor.isSpecified) surfaceColor else Color.Transparent

    val backgroundModifier = if (useGlass) {
        Modifier.fluidGlass(
            shape = shape,
            intensity = GlassIntensity.Standard,
            rimTintColor = rimTintColor,
            surfaceColor = resolvedSurface,
            surfaceAlpha = surfaceAlpha,
        )
    } else {
        Modifier
            .clip(shape)
            .background(fallbackFill)
            .border(
                width = 0.5.dp,
                color = if (isDark) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.08f),
                shape = shape
            )
    }

    val contentColor = if (isDark) Color.White else Color(0xFF111111)
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(shape)
            .then(backgroundModifier)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                role = Role.Button,
                enabled = enabled,
                indication = if (isFluid) BounceIndication else ripple(bounded = false, radius = size / 2),
                interactionSource = interactionSource,
            )
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}
