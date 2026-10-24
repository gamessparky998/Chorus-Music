package zone.ien.hig

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun Modifier.haze(
    vararg area: Rect,
    backgroundColor: Color,
    tint: Color = HazeDefaults.tint(backgroundColor),
    blurRadius: Dp = HazeDefaults.blurRadius,
): Modifier =
    this then
        HazeNodeElement(
            areas = area.toList(),
            tint = tint,
            backgroundColor = backgroundColor,
            blurRadius = blurRadius,
            density = LocalDensity.current,
        )

@Immutable
object HazeDefaults {

    val blurRadius: Dp = 20.dp

    val tintAlpha: Float = 0.7f

    fun tint(color: Color): Color = color.copy(alpha = tintAlpha)
}

internal data class HazeNodeElement(
    val areas: List<Rect>,
    val backgroundColor: Color,
    val tint: Color,
    val blurRadius: Dp,
    val density: Density,
): ModifierNodeElement<HazeNode>() {
    override fun create(): HazeNode =
        HazeNode(
            areas = areas,
            backgroundColor = backgroundColor,
            tint = tint,
            blurRadius = blurRadius,
            density = density,
        )

    override fun update(node: HazeNode) {
        node.update(
            areas = areas,
            backgroundColor = backgroundColor,
            tint = tint,
            blurRadius = blurRadius,
        )
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "haze"
        properties["areas"] = areas
        properties["backgroundColor"] = backgroundColor
        properties["tint"] = tint
        properties["blurRadius"] = blurRadius
    }
}

internal class HazeNode(
    private var areas: List<Rect>,
    private var backgroundColor: Color,
    private var tint: Color,
    private var blurRadius: Dp,
    private val density: Density,
): Modifier.Node(),
    DrawModifierNode {

    fun update(
        areas: List<Rect>,
        backgroundColor: Color,
        tint: Color,
        blurRadius: Dp,
    ) {
        this.areas = areas
        this.backgroundColor = backgroundColor
        this.tint = tint
        this.blurRadius = blurRadius
    }

    override fun ContentDrawScope.draw() {
        drawContent()

        for (area in areas) {

            drawRect(
                color = tint.copy(alpha = 1f),
                topLeft = area.topLeft,
                size = area.size,
            )
        }
    }
}
