package pushkar.chorus.music.ui.component.backdrop

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.node.requireLayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import android.os.Build
import pushkar.chorus.music.ui.component.backdrop.internal.MultiRefractionMaxShapes
import pushkar.chorus.music.ui.component.backdrop.internal.MultiRefractionShaderString
import pushkar.chorus.music.ui.component.backdrop.internal.RuntimeShaderEffect
import pushkar.chorus.music.ui.component.backdrop.internal.chain
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.IntSize
import pushkar.chorus.music.ui.component.backdrop.backdrops.LayerBackdrop
import pushkar.chorus.music.ui.component.backdrop.backdrops.rememberLayerBackdrop

const val GlassPoolScale = 0.35f

fun BackdropEffectScope.poolLens(refractionHeight: Float, refractionAmount: Float, depthEffect: Boolean) {
    if (refractionHeight <= 0f || refractionAmount <= 0f) return
    poolLens = PoolLens(refractionHeight, refractionAmount, depthEffect)
}

private const val CoverageMarginDp = 32f

@Composable
fun rememberGlassPool(): LayerBackdrop = rememberLayerBackdrop().also { it.masksSamplers = true }

fun Modifier.glassPool(
    source: Backdrop,
    pooled: LayerBackdrop,
    effects: BackdropEffectScope.() -> Unit,
    scale: Float,
): Modifier = this then GlassPoolElement(source, pooled, effects, scale.coerceIn(0.05f, 1f))

private class GlassPoolElement(
    val source: Backdrop,
    val pooled: LayerBackdrop,
    val effects: BackdropEffectScope.() -> Unit,
    val scale: Float,
) : ModifierNodeElement<GlassPoolNode>() {

    override fun create(): GlassPoolNode = GlassPoolNode(source, pooled, effects, scale)

    override fun update(node: GlassPoolNode) {
        node.update(source, pooled, effects, scale)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "glassPool"
        properties["scale"] = scale
    }

    override fun equals(other: Any?): Boolean =
        other is GlassPoolElement && source == other.source && pooled == other.pooled &&
                effects == other.effects && scale == other.scale

    override fun hashCode(): Int {
        var result = source.hashCode()
        result = 31 * result + pooled.hashCode()
        result = 31 * result + effects.hashCode()
        result = 31 * result + scale.hashCode()
        return result
    }
}

private class GlassPoolNode(
    var source: Backdrop,
    var pooled: LayerBackdrop,
    var effects: BackdropEffectScope.() -> Unit,
    var scale: Float,
) : DrawModifierNode, GlobalPositionAwareModifierNode, Modifier.Node() {

    private val effectScope = object : BackdropEffectScopeImpl() {
        override val shape get() = androidx.compose.ui.graphics.RectangleShape
    }

    private var captureLayer: GraphicsLayer? = null

    private var coordinates: LayoutCoordinates? = null
    private var recordedSize: IntSize = IntSize.Zero
    private var recordedSourceVersion: Int? = null
    private var recordedScale = Float.NaN
    private var captureDirty = true

    private var region = Rect.Zero
    private var effectsReady = false

    private var baseEffect: androidx.compose.ui.graphics.RenderEffect? = null

    private class LensTarget(val rect: Rect, val lens: PoolLens?, val radii: FloatArray?, val outline: Outline)

    private val lensTargets = LinkedHashMap<Any, LensTarget>()

    private val lensRequest: (Any, Rect, PoolLens?, FloatArray?, Outline?) -> Unit =
        { key, rect, lens, radii, outline ->
            if (outline == null || rect.isEmpty) {
                if (lensTargets.remove(key) != null) {
                    applyLens()
                    recordFlatten()
                }
            } else {
                val old = lensTargets[key]
                val lensChanged = old == null || old.rect != rect || old.lens != lens ||
                        !(old.radii contentEquals radii)
                val maskChanged = old == null || old.rect != rect || old.outline != outline
                if (lensChanged || maskChanged) {
                    lensTargets[key] = LensTarget(rect, lens, radii, outline)
                    if (lensChanged) applyLens()
                    if (maskChanged) recordFlatten()
                }
            }
        }

    private fun applyLens() {
        val capture = captureLayer ?: return
        val scale = recordedScale
        if (lensTargets.values.none { it.lens != null } || scale.isNaN() ||
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
        ) {
            capture.renderEffect = baseEffect
            return
        }
        val origin = region
        val max = MultiRefractionMaxShapes
        val rects = FloatArray(max * 4)
        val radii = FloatArray(max * 4)
        val lens = FloatArray(max * 4)
        var n = 0
        for (t in lensTargets.values) {
            if (n == max) break
            if (t.lens == null || t.radii == null) continue
            val o = n * 4
            rects[o] = (t.rect.left - origin.left) * scale
            rects[o + 1] = (t.rect.top - origin.top) * scale
            rects[o + 2] = (t.rect.right - origin.left) * scale
            rects[o + 3] = (t.rect.bottom - origin.top) * scale
            for (c in 0 until 4) radii[o + c] = t.radii[c] * scale
            lens[o] = t.lens.refractionHeight * scale
            lens[o + 1] = -t.lens.refractionAmount * scale
            lens[o + 2] = if (t.lens.depthEffect) 1f else 0f
            n++
        }
        val shader = effectScope.obtainRuntimeShader("MultiRefraction", MultiRefractionShaderString)
        shader.setFloatUniform("rects", rects)
        shader.setFloatUniform("radii", radii)
        shader.setFloatUniform("lens", lens)
        shader.setIntUniform("count", n)
        capture.renderEffect = baseEffect.chain(RuntimeShaderEffect(shader, "content"))
    }

    private val coverage: (Rect) -> Unit = coverage@{ rect ->
        if (!effectsReady || rect.isEmpty) return@coverage
        val margin = requireDensity().run { CoverageMarginDp.dp.toPx() }
        val bounds = coordinates?.size ?: return@coverage
        val want = rect.inflate(margin)
            .intersect(Rect(0f, 0f, bounds.width.toFloat(), bounds.height.toFloat()))
        if (want.isEmpty) return@coverage
        val r = region
        if (!r.isEmpty && r.left <= want.left && r.top <= want.top &&
            r.right >= want.right && r.bottom >= want.bottom
        ) return@coverage
        region = if (r.isEmpty) want else Rect(
            minOf(r.left, want.left), minOf(r.top, want.top),
            maxOf(r.right, want.right), maxOf(r.bottom, want.bottom),
        )
        record()
        applyLens()
        pooled.geometryChanged()
    }

    fun update(
        source: Backdrop,
        pooled: LayerBackdrop,
        effects: BackdropEffectScope.() -> Unit,
        scale: Float,
    ) {
        if (this.source != source || this.pooled != pooled ||
            this.effects !== effects || this.scale != scale
        ) {
            captureDirty = true
        }
        this.source = source
        this.pooled = pooled
        this.effects = effects
        this.scale = scale
    }

    override fun ContentDrawScope.draw() {
        val capture = captureLayer ?: return
        val coordinates = coordinates ?: return
        if (size.width <= 0f || size.height <= 0f) return

        val recordSize = IntSize(
            (size.width * scale).toInt().coerceAtLeast(1),
            (size.height * scale).toInt().coerceAtLeast(1),
        )
        val sourceGeometry = (source as? LayerBackdrop)?.geometryVersion
        val sourceChanged = sourceGeometry == null || sourceGeometry != recordedSourceVersion
        if (!captureDirty && !sourceChanged && recordSize == recordedSize &&
            scale == recordedScale && pooled.hasRecording
        ) {
            return
        }

        effectScope.update(this, scale)
        effectScope.apply(effects)
        baseEffect = effectScope.renderEffect
        capture.colorFilter = effectScope.compositeColorFilter
        effectsReady = true

        recordedSize = recordSize
        recordedScale = scale
        applyLens()
        record()
        pooled.sourceScale = scale
        pooled.layerCoordinates = coordinates
        pooled.coverageRequest = coverage
        pooled.lensRequest = lensRequest
        pooled.contentRecorded()
        pooled.geometryChanged()

        recordedSourceVersion = sourceGeometry
        captureDirty = false
    }

    private fun regionSize(): IntSize {
        val clip = region
        if (clip.isEmpty || recordedScale.isNaN()) return IntSize(1, 1)
        return IntSize(
            ceil(clip.width * recordedScale).toInt().coerceAtLeast(1),
            ceil(clip.height * recordedScale).toInt().coerceAtLeast(1),
        )
    }

    private fun record() {
        val capture = captureLayer ?: return
        val coordinates = coordinates ?: return
        val scale = recordedScale
        val clip = region
        capture.record(requireDensity(), requireLayoutDirection(), regionSize()) {
            if (clip.isEmpty) return@record
            scale(scale, scale, Offset.Zero) {
                translate(-clip.left, -clip.top) {
                    with(source) {
                        drawBackdrop(
                            density = effectScope,
                            coordinates = coordinates,
                            layerBlock = null,
                        )
                    }
                }
            }
        }
        pooled.sourceOrigin = if (clip.isEmpty) Offset.Zero else clip.topLeft
        recordFlatten()
    }

    private fun recordFlatten() {
        val capture = captureLayer ?: return
        if (recordedScale.isNaN()) return
        val scale = recordedScale
        val clip = region
        val targets = lensTargets.values.toList()
        capture.blendMode = BlendMode.SrcIn
        pooled.graphicsLayer.compositingStrategy =
            androidx.compose.ui.graphics.layer.CompositingStrategy.Offscreen
        pooled.graphicsLayer.record(requireDensity(), requireLayoutDirection(), regionSize()) {
            if (clip.isEmpty || targets.isEmpty()) return@record
            run {
                scale(scale, scale, Offset.Zero) {
                    translate(-clip.left, -clip.top) {
                        for (t in targets) {
                            translate(t.rect.left, t.rect.top) { drawOutline(t.outline, Color.Black) }
                        }
                    }
                }
            }
            drawLayer(capture)
        }
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        if (coordinates.isAttached) {
            if (this.coordinates != coordinates) captureDirty = true
            this.coordinates = coordinates
        }
    }

    override fun onAttach() {
        captureLayer = requireGraphicsContext().createGraphicsLayer()
        captureDirty = true
    }

    override fun onDetach() {
        captureLayer?.let { requireGraphicsContext().releaseGraphicsLayer(it) }
        captureLayer = null
        if (pooled.coverageRequest === coverage) pooled.coverageRequest = null
        if (pooled.lensRequest === lensRequest) pooled.lensRequest = null
        lensTargets.clear()
        baseEffect = null
        region = Rect.Zero
        pooled.sourceOrigin = Offset.Zero
        effectsReady = false
        coordinates = null
        recordedSize = IntSize.Zero
        recordedScale = Float.NaN
        recordedSourceVersion = null
        captureDirty = true
        effectScope.reset()
    }
}
