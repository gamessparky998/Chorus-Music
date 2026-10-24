package pushkar.chorus.music.ui.component.shapes

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.abs

@Immutable
object NavContinuousCapsule : CornerBasedShape(
    topStart = CornerSize(50),
    topEnd = CornerSize(50),
    bottomEnd = CornerSize(50),
    bottomStart = CornerSize(50),
) {
    override fun createOutline(
        size: Size,
        topStart: Float,
        topEnd: Float,
        bottomEnd: Float,
        bottomStart: Float,
        layoutDirection: LayoutDirection,
    ): Outline {
        val radius = size.minDimension / 2f
        if (abs(size.width - size.height) < 1f || radius <= 0f) {
            return Outline.Rounded(RoundRect(0f, 0f, size.width, size.height, CornerRadius(radius, radius)))
        }
        return Continuity.Default.createCapsuleOutline(size)
    }

    override fun copy(
        topStart: CornerSize,
        topEnd: CornerSize,
        bottomEnd: CornerSize,
        bottomStart: CornerSize,
    ): CornerBasedShape = this
}

private val CircularCapsule = RoundedCornerShape(50)

val Shape.shadowShape: Shape get() = if (this === NavContinuousCapsule) CircularCapsule else this
