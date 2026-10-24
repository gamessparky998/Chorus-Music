package zone.ien.hig.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp
import com.kyant.capsule.ContinuousRoundedRectangle
import zone.ien.hig.InternalCupertinoApi

@Stable
class Shapes(
    val extraSmall: ContinuousRoundedRectangle = ShapeDefaults.ExtraSmall,
    val small: ContinuousRoundedRectangle = ShapeDefaults.Small,
    val medium: ContinuousRoundedRectangle = ShapeDefaults.Medium,
    val large: ContinuousRoundedRectangle = ShapeDefaults.Large,
    val extraLarge: ContinuousRoundedRectangle = ShapeDefaults.ExtraLarge,
) {

    fun copy(
        extraSmall: ContinuousRoundedRectangle = this.extraSmall,
        small: ContinuousRoundedRectangle = this.small,
        medium: ContinuousRoundedRectangle = this.medium,
        large: ContinuousRoundedRectangle = this.large,
        extraLarge: ContinuousRoundedRectangle = this.extraLarge,
    ) = Shapes(
        extraSmall = extraSmall,
        small = small,
        medium = medium,
        large = large,
        extraLarge = extraLarge,
    )
}

@InternalCupertinoApi
val LocalShapes = staticCompositionLocalOf { Shapes() }

@Immutable
object ShapeDefaults {
    val ExtraSmall: ContinuousRoundedRectangle = ContinuousRoundedRectangle(4.dp)

    val Small: ContinuousRoundedRectangle = ContinuousRoundedRectangle(8.dp)

    val Medium: ContinuousRoundedRectangle = ContinuousRoundedRectangle(12.dp)

    val Large: ContinuousRoundedRectangle = ContinuousRoundedRectangle(16.dp)

    val ExtraLarge: ContinuousRoundedRectangle = ContinuousRoundedRectangle(24.dp)
}
