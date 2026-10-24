package pushkar.chorus.music.ui.fluid.foundation

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pushkar.chorus.music.ui.component.shapes.Continuity
import pushkar.chorus.music.ui.component.shapes.ContinuousRoundedRectangle

@Immutable
object FluidShapes {
    val Capsule = ContinuousRoundedRectangle(9999.dp, continuity = Continuity.Default)
    val ExtraLarge = ContinuousRoundedRectangle(28.dp, continuity = Continuity.Default)

    val Large = ContinuousRoundedRectangle(22.dp, continuity = Continuity.Default)
    val Medium = ContinuousRoundedRectangle(16.dp, continuity = Continuity.Default)
    val Small = ContinuousRoundedRectangle(12.dp, continuity = Continuity.Default)
    val ExtraSmall = ContinuousRoundedRectangle(8.dp, continuity = Continuity.Default)
    fun sheetTopCorners(radius: Dp = 24.dp): ContinuousRoundedRectangle =
        ContinuousRoundedRectangle(
            topStart = CornerSize(radius),
            topEnd = CornerSize(radius),
            bottomEnd = CornerSize(0.dp),
            bottomStart = CornerSize(0.dp),
            continuity = Continuity.Default,
        )

    fun continuous(radius: Dp, continuity: Continuity = Continuity.Default): ContinuousRoundedRectangle =
        ContinuousRoundedRectangle(radius, continuity = continuity)
}
