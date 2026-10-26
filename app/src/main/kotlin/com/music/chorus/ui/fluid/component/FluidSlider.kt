package pushkar.chorus.music.ui.fluid.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import pushkar.chorus.music.ui.fluid.foundation.FluidTheme
import kotlin.math.roundToInt

@Immutable
data class FluidSliderTick(val value: Float, val label: String? = null)

@Composable
fun FluidSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    step: Float? = null,
    ticks: List<FluidSliderTick> = emptyList(),
    tint: Color = Color.Unspecified,
    minimumValueLabel: (@Composable () -> Unit)? = null,
    maximumValueLabel: (@Composable () -> Unit)? = null,
    onEditingChanged: ((Boolean) -> Unit)? = null,
    enabled: Boolean = true,
) {
    val colors = FluidTheme.colors
    val activeColor = if (tint.isSpecified) tint else FluidTheme.tint
    val span = (valueRange.endInclusive - valueRange.start).takeIf { it > 0f } ?: 1f

    fun report(rawFraction: Float) {
        val clamped = rawFraction.coerceIn(0f, 1f)
        val raw = valueRange.start + clamped * span
        val snapped = if (step != null && step > 0f) {
            valueRange.start + ((raw - valueRange.start) / step).roundToInt() * step
        } else {
            raw
        }
        onValueChange(snapped.coerceIn(valueRange.start, valueRange.endInclusive))
    }

    val resolvedTicks = when {
        ticks.isNotEmpty() -> ticks
        step != null && step > 0f -> {
            val count = (span / step).roundToInt()
            if (count in 1..20) {
                (0..count).map { FluidSliderTick(valueRange.start + it * step) }
            } else {
                emptyList()
            }
        }

                else -> emptyList()
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        minimumValueLabel?.invoke()

        var dragging by remember { mutableStateOf(false) }
        val knobScale by animateFloatAsState(
            targetValue = if (dragging) 1.12f else 1f,
            animationSpec = spring(dampingRatio = 0.7f, stiffness = 900f),
            label = "FluidSliderKnob",
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .height(28.dp)
                .pointerInput(enabled, valueRange, step) {
                    if (!enabled) return@pointerInput
                    detectTapGestures { offset -> report(offset.x / size.width) }
                }
                .pointerInput(enabled, valueRange, step) {
                    if (!enabled) return@pointerInput
                    detectDragGestures(
                        onDragStart = {
                            dragging = true
                            onEditingChanged?.invoke(true)
                        },
                        onDragEnd = {
                            dragging = false
                            onEditingChanged?.invoke(false)
                        },
                        onDragCancel = {
                            dragging = false
                            onEditingChanged?.invoke(false)
                        },
                    ) { change, _ ->
                        change.consume()
                        report(change.position.x / size.width)
                    }
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            val fraction = ((value - valueRange.start) / span).coerceIn(0f, 1f)

            Canvas(Modifier.fillMaxWidth().height(28.dp)) {
                val trackHeight = 4.dp.toPx()
                val knobRadius = 13.5f.dp.toPx() * knobScale
                val usable = size.width - knobRadius * 2f
                val knobX = knobRadius + usable * fraction
                val centerY = size.height / 2f

                drawLine(
                    color = colors.systemGray5,
                    start = Offset(knobRadius, centerY),
                    end = Offset(size.width - knobRadius, centerY),
                    strokeWidth = trackHeight,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = if (enabled) activeColor else activeColor.copy(alpha = 0.4f),
                    start = Offset(knobRadius, centerY),
                    end = Offset(knobX, centerY),
                    strokeWidth = trackHeight,
                    cap = StrokeCap.Round,
                )

                resolvedTicks.forEach { tick ->
                    val tickFraction = ((tick.value - valueRange.start) / span).coerceIn(0f, 1f)
                    val x = knobRadius + usable * tickFraction
                    drawLine(
                        color = colors.tertiaryLabel,
                        start = Offset(x, centerY - 5.dp.toPx()),
                        end = Offset(x, centerY + 5.dp.toPx()),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }

                drawCircle(
                    color = Color.Black.copy(alpha = 0.12f),
                    radius = knobRadius,
                    center = Offset(knobX, centerY + 1.dp.toPx()),
                )
                drawCircle(
                    color = Color.White,
                    radius = knobRadius,
                    center = Offset(knobX, centerY),
                )
            }
        }

        maximumValueLabel?.invoke()
    }
}
