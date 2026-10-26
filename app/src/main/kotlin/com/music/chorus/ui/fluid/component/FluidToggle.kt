package pushkar.chorus.music.ui.fluid.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import pushkar.chorus.music.ui.component.shapes.ContinuousCapsule
import pushkar.chorus.music.ui.component.shapes.ContinuousRoundedRectangle
import pushkar.chorus.music.ui.fluid.foundation.FluidTheme

enum class FluidToggleStyle { Switch, Checkbox, Button }

@Composable
fun FluidToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    style: FluidToggleStyle = FluidToggleStyle.Switch,
    tint: Color = Color.Unspecified,
    enabled: Boolean = true,
) {
    val colors = FluidTheme.colors
    val onColor = when {
        tint.isSpecified -> tint
        style == FluidToggleStyle.Switch -> colors.systemGreen
        else -> FluidTheme.tint
    }

    val toggleModifier = Modifier.toggleable(
        value = checked,
        enabled = enabled,
        role = Role.Switch,
        onValueChange = onCheckedChange,
    )

    when (style) {
        FluidToggleStyle.Switch -> Row(
            modifier = modifier.fillMaxWidth().then(toggleModifier),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            if (label != null) {
                Text(
                    text = label,
                    style = FluidTheme.typography.body,
                    color = colors.label,
                )
            }
            FluidSwitchTrack(checked, onColor, enabled)
        }

        FluidToggleStyle.Checkbox -> Row(
            modifier = modifier.then(toggleModifier),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(ContinuousRoundedRectangle(6.dp))
                    .background(if (checked) onColor else Color.Transparent)
                    .border(
                        width = if (checked) 0.dp else 1.5.dp,
                        color = colors.tertiaryLabel,
                        shape = ContinuousRoundedRectangle(6.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (checked) {
                    Canvas(Modifier.size(width = 13.dp, height = 10.dp)) {
                        drawPath(
                            path = Path().apply {
                                moveTo(size.width * 0.15f, size.height * 0.5f)
                                lineTo(size.width * 0.45f, size.height * 0.9f)
                                lineTo(size.width * 0.95f, size.height * 0.15f)
                            },
                            color = Color.White,
                            style = Stroke(
                                width = 2.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round,
                            ),
                        )
                    }
                }
            }
            if (label != null) {
                Text(
                    text = label,
                    style = FluidTheme.typography.body,
                    color = colors.label,
                )
            }
        }

        FluidToggleStyle.Button -> FluidButton(
            onClick = { onCheckedChange(!checked) },
            modifier = modifier,
            style = if (checked) FluidButtonStyle.BorderedProminent else FluidButtonStyle.Bordered,
            tint = onColor,
            enabled = enabled,
        ) {
            if (label != null) {
                Text(text = label)
            }
        }
    }
}

@Composable
private fun FluidSwitchTrack(checked: Boolean, onColor: Color, enabled: Boolean) {
    val colors = FluidTheme.colors
    val track by animateColorAsState(
        targetValue = if (checked) onColor else colors.systemGray5,
        label = "FluidSwitchTrack",
    )
    val knobOffset by animateDpAsState(
        targetValue = if (checked) 20.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 900f),
        label = "FluidSwitchKnob",
    )

    Box(
        modifier = Modifier
            .size(width = 51.dp, height = 31.dp)
            .clip(ContinuousCapsule())
            .background(if (enabled) track else track.copy(alpha = 0.4f))
            .padding(2.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .offset(x = knobOffset)
                .size(27.dp)
                .shadow(elevation = 2.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}
