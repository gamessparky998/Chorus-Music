package pushkar.chorus.music.ui.fluid.foundation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Indication
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

fun Modifier.pressWobble(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.86f,
): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = 0.38f,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "pressWobble",
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

private const val BouncePressedScale = 0.94f

object BounceIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        BounceIndicationNode(interactionSource)

    override fun equals(other: Any?): Boolean = other === this
    override fun hashCode(): Int = 0x426F756E
}

private class BounceIndicationNode(
    private val interactionSource: InteractionSource,
) : Modifier.Node(), DrawModifierNode {
    private val scaleAnimation = Animatable(1f, 0.001f)
    private val activePresses = mutableSetOf<PressInteraction.Press>()
    private var interactionJob: Job? = null
    private var animationJob: Job? = null

    private fun animatePressed(pressed: Boolean) {
        animationJob?.cancel()
        if (!isAttached) return
        animationJob = coroutineScope.launch {
            scaleAnimation.animateTo(
                targetValue = if (pressed) BouncePressedScale else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                    visibilityThreshold = 0.001f,
                ),
            )
        }
    }

    override fun onAttach() {
        interactionJob = coroutineScope.launch {
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> {
                        activePresses += interaction
                        animatePressed(true)
                    }

                    is PressInteraction.Release -> {
                        activePresses -= interaction.press
                        animatePressed(activePresses.isNotEmpty())
                    }

                    is PressInteraction.Cancel -> {
                        activePresses -= interaction.press
                        animatePressed(activePresses.isNotEmpty())
                    }
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        val scaleFactor = scaleAnimation.value
        if (scaleFactor == 1f) {
            drawContent()
        } else {
            scale(scaleFactor, pivot = center) { this@draw.drawContent() }
        }
    }

    override fun onDetach() {
        interactionJob?.cancel()
        animationJob?.cancel()
        interactionJob = null
        animationJob = null
        activePresses.clear()
    }
}

fun Modifier.fluidBounceClick(
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    indication: Indication? = null,
    onClick: () -> Unit,
): Modifier = clickable(
    interactionSource = interactionSource,
    indication = if (enabled) BounceIndication else indication,
    enabled = enabled,
    onClick = onClick,
)

fun Modifier.combinedFluidBounceClick(
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    indication: Indication? = null,
    onLongClick: (() -> Unit)? = null,
    onDoubleClick: (() -> Unit)? = null,
    onClick: () -> Unit,
): Modifier = combinedClickable(
    interactionSource = interactionSource,
    indication = if (enabled) BounceIndication else indication,
    enabled = enabled,
    onLongClick = onLongClick,
    onDoubleClick = onDoubleClick,
    onClick = onClick,
)

fun Modifier.rimLight(
    shape: Shape,
    color: Color = Color.White,
    strength: Float = 0.55f,
    width: Dp = 1.dp,
): Modifier = drawWithCache {
    val outline = shape.createOutline(size, layoutDirection, this)
    val stroke = Stroke(width = width.toPx())
    val brush = Brush.verticalGradient(
        0f to color.copy(alpha = strength),
        0.5f to color.copy(alpha = strength * 0.12f),
        1f to Color.Transparent,
    )
    onDrawWithContent {
        drawContent()
        drawOutline(
            outline = outline,
            brush = brush,
            style = stroke,
            blendMode = BlendMode.Plus,
        )
    }
}
