package pushkar.chorus.music.ui.fluid.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pushkar.chorus.music.ui.component.shapes.ContinuousCapsule
import pushkar.chorus.music.ui.component.shapes.ContinuousRoundedRectangle
import pushkar.chorus.music.ui.fluid.foundation.FluidTheme
import pushkar.chorus.music.ui.fluid.foundation.LocalFluidContentColor
import pushkar.chorus.music.ui.fluid.foundation.LocalFluidTextStyle
import pushkar.chorus.music.ui.fluid.foundation.fluidGlassSurface

enum class FluidButtonStyle { Plain, Borderless, Bordered, BorderedProminent, Glass, GlassProminent }

enum class FluidButtonRole { Cancel, Destructive, Confirm }

enum class FluidButtonSizing { Fitted, Flexible }

enum class FluidControlSize { Mini, Small, Regular, Large, ExtraLarge }

@Immutable
sealed interface FluidButtonBorderShape {
    data object Capsule : FluidButtonBorderShape
    data object Circle : FluidButtonBorderShape
    data class RoundedRectangle(val radius: Dp? = null) : FluidButtonBorderShape
}

@Immutable
private data class ButtonMetrics(val height: Dp, val padding: Dp, val radius: Dp)

private fun metricsFor(size: FluidControlSize): ButtonMetrics = when (size) {
    FluidControlSize.Mini -> ButtonMetrics(24.dp, 10.dp, 7.dp)
    FluidControlSize.Small -> ButtonMetrics(28.dp, 12.dp, 8.dp)
    FluidControlSize.Regular -> ButtonMetrics(34.dp, 14.dp, 10.dp)
    FluidControlSize.Large -> ButtonMetrics(44.dp, 20.dp, 12.dp)
    FluidControlSize.ExtraLarge -> ButtonMetrics(50.dp, 24.dp, 14.dp)
}

@Composable
fun FluidButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: FluidButtonStyle = FluidButtonStyle.Plain,
    role: FluidButtonRole? = null,
    tint: Color = Color.Unspecified,
    sizing: FluidButtonSizing = FluidButtonSizing.Fitted,
    borderShape: FluidButtonBorderShape = FluidButtonBorderShape.Capsule,
    enabled: Boolean = true,
    controlSize: FluidControlSize = FluidControlSize.Regular,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = FluidTheme.colors
    val metrics = metricsFor(controlSize)

    val resolvedTint = when {
        role == FluidButtonRole.Destructive -> colors.systemRed
        tint.isSpecified -> tint
        else -> FluidTheme.tint
    }

    val shape: Shape = when (borderShape) {
        FluidButtonBorderShape.Capsule -> ContinuousCapsule()
        FluidButtonBorderShape.Circle -> CircleShape
        is FluidButtonBorderShape.RoundedRectangle ->
            ContinuousRoundedRectangle(borderShape.radius ?: metrics.radius)
    }

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 900f),
        label = "FluidButtonPress",
    )

    val contentColor = when (style) {
        FluidButtonStyle.Plain -> colors.label
        FluidButtonStyle.Borderless, FluidButtonStyle.Bordered -> resolvedTint
        FluidButtonStyle.BorderedProminent, FluidButtonStyle.GlassProminent -> colors.onAccent
        FluidButtonStyle.Glass ->
            if (tint.isSpecified || role == FluidButtonRole.Destructive) resolvedTint else colors.label
    }

    val surface = when (style) {
        FluidButtonStyle.Plain, FluidButtonStyle.Borderless -> Modifier
        FluidButtonStyle.Bordered -> Modifier.clip(shape).background(colors.secondarySystemFill)
        FluidButtonStyle.BorderedProminent -> Modifier.clip(shape).background(resolvedTint)
        FluidButtonStyle.Glass -> Modifier.fluidGlassSurface(
            shape = shape,
            tint = if (tint.isSpecified) tint else Color.Unspecified,
        )

        FluidButtonStyle.GlassProminent -> Modifier.fluidGlassSurface(
            shape = shape,
            tint = resolvedTint,
        )
    }

    val hasChrome = style != FluidButtonStyle.Plain && style != FluidButtonStyle.Borderless
    val sizeModifier = when {
        borderShape == FluidButtonBorderShape.Circle -> Modifier.size(metrics.height)
        sizing == FluidButtonSizing.Flexible ->
            Modifier.fillMaxWidth().defaultMinSize(minHeight = metrics.height)

        hasChrome -> Modifier.defaultMinSize(minHeight = metrics.height)
        else -> Modifier
    }

    Row(
        modifier = modifier
            .scale(pressScale)
            .then(sizeModifier)
            .then(surface)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .then(
                if (hasChrome && borderShape != FluidButtonBorderShape.Circle) {
                    Modifier.padding(horizontal = metrics.padding)
                } else {
                    Modifier
                }
            ),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(
            LocalFluidContentColor provides contentColor.copy(alpha = if (enabled) 1f else 0.3f),
            LocalFluidTextStyle provides FluidTheme.typography.body,
        ) {
            content()
        }
    }
}

@Composable
fun FluidButton(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: FluidButtonStyle = FluidButtonStyle.Plain,
    role: FluidButtonRole? = null,
    tint: Color = Color.Unspecified,
    sizing: FluidButtonSizing = FluidButtonSizing.Fitted,
    borderShape: FluidButtonBorderShape = FluidButtonBorderShape.Capsule,
    enabled: Boolean = true,
    controlSize: FluidControlSize = FluidControlSize.Regular,
) = FluidButton(
    onClick = onClick,
    modifier = modifier,
    style = style,
    role = role,
    tint = tint,
    sizing = sizing,
    borderShape = borderShape,
    enabled = enabled,
    controlSize = controlSize,
) {
    Text(
        text = title,
        style = LocalFluidTextStyle.current,
        color = LocalFluidContentColor.current,
    )
}
