package pushkar.chorus.music.ui.component.backdrop.backdrops

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.toSize
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.unit.LayoutDirection
import pushkar.chorus.music.ui.component.backdrop.Backdrop
import pushkar.chorus.music.ui.component.backdrop.BackdropEffectScope
import pushkar.chorus.music.ui.component.backdrop.PoolLens
import pushkar.chorus.music.ui.component.backdrop.internal.InverseLayerScope

private val DefaultOnDraw: ContentDrawScope.() -> Unit = { drawContent() }

@Composable
fun rememberLayerBackdrop(
    graphicsLayer: GraphicsLayer = rememberGraphicsLayer(),
    onDraw: ContentDrawScope.() -> Unit = DefaultOnDraw
): LayerBackdrop {
    return remember(graphicsLayer, onDraw) {
        LayerBackdrop(graphicsLayer, onDraw)
    }
}

@Stable
class LayerBackdrop internal constructor(
    val graphicsLayer: GraphicsLayer,
    internal val onDraw: ContentDrawScope.() -> Unit
) : Backdrop {

    override val isCoordinatesDependent: Boolean = true

    internal var layerCoordinates: LayoutCoordinates? by mutableStateOf(null)

    internal var contentVersion: Int = 0
        private set

    internal fun contentRecorded() {
        if (contentVersion == 0) geometryChanged()
        contentVersion++
    }

    internal var geometryVersion: Int = 0
        private set

    internal fun geometryChanged() {
        geometryVersion++
    }

    internal val staticRegions = ArrayList<Rect>()

    internal fun isFullyStatic(rect: Rect): Boolean =
        staticRegions.any { region ->
            region.left <= rect.left && region.top <= rect.top &&
                    region.right >= rect.right && region.bottom >= rect.bottom
        }

    internal val hasRecording: Boolean get() = contentVersion > 0

    internal var sourceScale: Float = 1f

    internal var sourceOrigin: androidx.compose.ui.geometry.Offset = androidx.compose.ui.geometry.Offset.Zero

    internal var coverageRequest: ((Rect) -> Unit)? = null

    internal var lensRequest: ((key: Any, rect: Rect, lens: PoolLens?, radii: FloatArray?, outline: Outline?) -> Unit)? =
        null

    internal var masksSamplers: Boolean = false

    private var inverseLayerScope: InverseLayerScope? = null

    override fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?
    ) {
        val coordinates = coordinates ?: return
        val layerCoordinates = layerCoordinates ?: return
        val sourceScale = sourceScale
        withTransform({
            if (layerBlock != null) {
                with(obtainInverseLayerScope()) { inverseTransform(density, layerBlock) }
            }
            val offset =
                try {
                    layerCoordinates.localPositionOf(coordinates)
                } catch (_: Exception) {

                    coordinates.positionInWindow() - layerCoordinates.positionInWindow()
                }
            val sampled = Rect(offset, coordinates.size.toSize())
            coverageRequest?.invoke(sampled)
            lensRequest?.let { request ->
                val scope = density as? BackdropEffectScope
                val lens = scope?.poolLens
                val radii = if (lens != null) scope.cornerRadii() else null
                val outline = scope?.let { it.shape.createOutline(it.size, it.layoutDirection, it) }
                request(scope ?: coordinates, sampled, lens, radii, outline)
            }

            val origin = sourceOrigin
            translate(origin.x - offset.x, origin.y - offset.y)
            if (sourceScale != 1f) {
                scale(1f / sourceScale, 1f / sourceScale, androidx.compose.ui.geometry.Offset.Zero)
            }
        }) {
            drawLayer(graphicsLayer)
        }
    }

    private fun BackdropEffectScope.cornerRadii(): FloatArray {
        val size = size
        val max = size.minDimension / 2f
        val shape = shape as? CornerBasedShape ?: return FloatArray(4) { max }
        val ltr = layoutDirection == LayoutDirection.Ltr
        val ts = shape.topStart.toPx(size, this).coerceAtMost(max)
        val te = shape.topEnd.toPx(size, this).coerceAtMost(max)
        val be = shape.bottomEnd.toPx(size, this).coerceAtMost(max)
        val bs = shape.bottomStart.toPx(size, this).coerceAtMost(max)
        return if (ltr) floatArrayOf(ts, te, be, bs) else floatArrayOf(te, ts, bs, be)
    }

    private fun obtainInverseLayerScope(): InverseLayerScope {
        return inverseLayerScope?.apply { reset() }
            ?: InverseLayerScope().also { inverseLayerScope = it }
    }
}
