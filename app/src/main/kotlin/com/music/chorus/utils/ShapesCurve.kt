package pushkar.chorus.music.utils

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import pushkar.chorus.music.ui.component.shapes.ContinuousRoundedRectangle
import pushkar.chorus.music.ui.theme.isFluidTheme

private const val ConnectedCornerRadius = 4
private const val EndCornerRadius = 16

@Composable
fun listItemColors(): ListItemColors {
    return ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
}

fun leadingItemShape(): RoundedCornerShape = RoundedCornerShape(
    topStart = EndCornerRadius.dp,
    topEnd = EndCornerRadius.dp,
    bottomStart = ConnectedCornerRadius.dp,
    bottomEnd = ConnectedCornerRadius.dp
)

fun middleItemShape(): RoundedCornerShape = RoundedCornerShape(ConnectedCornerRadius.dp)

fun endItemShape(): RoundedCornerShape = RoundedCornerShape(
    topStart = ConnectedCornerRadius.dp,
    topEnd = ConnectedCornerRadius.dp,
    bottomStart = EndCornerRadius.dp,
    bottomEnd = EndCornerRadius.dp
)

@Composable
fun getGroupedShape(index: Int, count: Int): Shape {
    if (isFluidTheme()) {
        if (count <= 1) {
            return ContinuousRoundedRectangle(16.dp)
        }
        return when {
            index == 0 -> ContinuousRoundedRectangle(
                topStart = EndCornerRadius.dp,
                topEnd = EndCornerRadius.dp,
                bottomStart = ConnectedCornerRadius.dp,
                bottomEnd = ConnectedCornerRadius.dp
            )
            index == count - 1 -> ContinuousRoundedRectangle(
                topStart = ConnectedCornerRadius.dp,
                topEnd = ConnectedCornerRadius.dp,
                bottomStart = EndCornerRadius.dp,
                bottomEnd = EndCornerRadius.dp
            )
            else -> ContinuousRoundedRectangle(ConnectedCornerRadius.dp)
        }
    }
    if (count <= 1) {
        return RoundedCornerShape(EndCornerRadius.dp)
    }
    return when {
        index == 0 -> leadingItemShape()
        index == count - 1 -> endItemShape()
        else -> middleItemShape()
    }
}
