package pushkar.chorus.music.ui.player

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pushkar.chorus.music.constants.PlayerArtworkStyle
import pushkar.chorus.music.ui.component.shapes.ContinuousRoundedRectangle

internal enum class CoverMorph { SHARED, FADE }

@Stable
internal interface PlayerCoverStyle {
    val morph: CoverMorph
    val spins: Boolean
    val artSpinsWithShape: Boolean

    @Composable
    fun shape(cornerRadius: Dp): Shape

    @Composable
    fun Decoration(modifier: Modifier)
}

internal object CardCoverStyle : PlayerCoverStyle {
    override val morph = CoverMorph.SHARED
    override val spins = false
    override val artSpinsWithShape = true

    @Composable
    override fun shape(cornerRadius: Dp): Shape = ContinuousRoundedRectangle(cornerRadius)

    @Composable
    override fun Decoration(modifier: Modifier) = Unit
}

internal object VinylCoverStyle : PlayerCoverStyle {
    override val morph = CoverMorph.SHARED
    override val spins = true
    override val artSpinsWithShape = true

    @Composable
    override fun shape(cornerRadius: Dp): Shape = CircleShape

    @Composable
    override fun Decoration(modifier: Modifier) {
        Canvas(modifier) {
            val r = size.minDimension / 2f
            val c = Offset(size.width / 2f, size.height / 2f)
            drawCircle(
                color = Color.Black.copy(alpha = 0.82f),
                radius = r * 0.67f,
                center = c,
                style = Stroke(width = r * 0.66f),
            )
            for (i in 1..7) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.05f),
                    radius = r * (0.40f + i * 0.083f),
                    center = c,
                    style = Stroke(width = 1.dp.toPx()),
                )
            }
            drawCircle(Color.Black, radius = r * 0.055f, center = c)
        }
    }
}

internal object CloverCoverStyle : PlayerCoverStyle {
    override val morph = CoverMorph.SHARED
    override val spins = true
    override val artSpinsWithShape = false

    @Composable
    override fun shape(cornerRadius: Dp): Shape = MaterialShapes.Clover8Leaf.toShape()

    @Composable
    override fun Decoration(modifier: Modifier) = Unit
}

internal fun PlayerArtworkStyle.coverStyle(): PlayerCoverStyle = when (this) {
    PlayerArtworkStyle.CARD -> CardCoverStyle
    PlayerArtworkStyle.VINYL -> VinylCoverStyle
    PlayerArtworkStyle.CLOVER -> CloverCoverStyle
}

@Composable
internal fun rememberCoverRotation(style: PlayerCoverStyle, isPlaying: Boolean): State<Float> =
    if (isPlaying && style.spins) {
        val angle = remember { mutableFloatStateOf(0f) }
        androidx.compose.runtime.LaunchedEffect(Unit) {
            while (true) {
                androidx.compose.runtime.withFrameMillis { frame ->
                    angle.floatValue = (frame % CoverRotationMillis) / CoverRotationMillis.toFloat() * 360f
                }
            }
        }
        angle
    } else {
        remember { mutableFloatStateOf(0f) }
    }

private const val CoverRotationMillis = 20_000L
