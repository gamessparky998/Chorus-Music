package pushkar.chorus.music.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import pushkar.chorus.music.constants.PLAYER_GLASS_UNFREEZE_PROGRESS

internal class MorphSlot<T : Any> {
    private val owners = LinkedHashMap<Any, T>()
    private val state = androidx.compose.runtime.mutableStateOf<T?>(null)
    val value: T? get() = state.value

    fun report(owner: Any, value: T) {
        owners.remove(owner)
        owners[owner] = value
        state.value = value
    }

    fun release(owner: Any) {
        owners.remove(owner) ?: return
        state.value = owners.values.lastOrNull()
    }
}

object PlayerMorph {
    @JvmField
    var enabled: Boolean = true
    @JvmField
    var miniLayer: GraphicsLayer? = null
    @JvmField
    var miniSurfaceLayer: GraphicsLayer? = null
    @JvmField
    var fullBackgroundLayer: GraphicsLayer? = null
    @JvmField
    var fullContentLayer: GraphicsLayer? = null
    @JvmField
    var stayInPlace: Boolean = false
    @JvmField
    var miniShadowElevationPx: Float = 0f

    val pillShadow = androidx.compose.animation.core.Animatable(1f)
    val landing = androidx.compose.animation.core.Animatable(0f)
    var landingActive: Boolean by androidx.compose.runtime.mutableStateOf(false)
    @JvmField
    var landingTravelPx: Float = 0f
    @JvmField
    var landingMaxDipPx: Float = 0f

    fun landingDipPx(): Float {
        val overshoot = landing.value
        if (overshoot >= 0f || landingMaxDipPx <= 0f) return 0f
        return landingMaxDipPx * kotlin.math.tanh(-overshoot * landingTravelPx / landingMaxDipPx)
    }

    @JvmField
    var miniFrameReady: Boolean = false
    @JvmField
    var fullBackgroundFrameReady: Boolean = false
    @JvmField
    var fullContentFrameReady: Boolean = false
    var progressProvider: () -> Float by androidx.compose.runtime.mutableStateOf({ 0f })

    @JvmField
    var onDragStart: (fromMiniPlayer: Boolean) -> Unit = {}
    @JvmField
    var onDragDelta: (Float) -> Unit = {}
    @JvmField
    var onDragEnd: (Float) -> Unit = {}
    @JvmField
    var onDragCancel: () -> Unit = {}

    val progress: Float get() = progressProvider().coerceIn(0f, 1f)

    @JvmField
    var targetOpen: Boolean = false
    @JvmField
    var isDragging: Boolean = false
    @JvmField
    var dragFromMiniPlayer: Boolean = false

    internal val inputOwner: PlayerInputOwner
        get() = playerSheetInputOwner(
            targetOpen = targetOpen,
            isDragging = isDragging,
            dragStartedFromMiniPlayer = dragFromMiniPlayer,
            progress = progress,
            sharedLayersReady = sharedLayersReady,
        )

    internal val miniAcceptsInput: Boolean get() = inputOwner == PlayerInputOwner.MINI
    val active: Boolean get() = activeState.value

    private val activeState = androidx.compose.runtime.derivedStateOf {
        val fraction = progress
        fraction > 0f && fraction < 1f && sharedLayersReady
    }

    val glassFrozen: Boolean get() = glassFrozenState.value

    private val glassFrozenState = androidx.compose.runtime.derivedStateOf {
        activeState.value && progress < PLAYER_GLASS_UNFREEZE_PROGRESS
    }

    val sharedLayersReady: Boolean
        get() {
            if (!enabled) return false
            if (miniLayer == null || fullBackgroundLayer == null || fullContentLayer == null) return false
            if (!miniFrameReady || !fullBackgroundFrameReady || !fullContentFrameReady) return false
            val mini = miniPlayerContainerRect ?: return false
            return mini.width > 0f && mini.height > 0f
        }
}

@Composable
fun InstallPlayerMorphLayers() {
    val mini = rememberGraphicsLayer()
    val miniSurface = rememberGraphicsLayer()
    val fullBackground = rememberGraphicsLayer()
    val fullContent = rememberGraphicsLayer()
    DisposableEffect(mini, miniSurface, fullBackground, fullContent) {
        PlayerMorph.miniLayer = mini
        PlayerMorph.miniSurfaceLayer = miniSurface
        PlayerMorph.fullBackgroundLayer = fullBackground
        PlayerMorph.fullContentLayer = fullContent
        onDispose {
            PlayerMorph.miniLayer = null
            PlayerMorph.miniSurfaceLayer = null
            PlayerMorph.fullBackgroundLayer = null
            PlayerMorph.fullContentLayer = null
        }
    }
}

fun Modifier.recordPlayerLayer(
    layer: () -> GraphicsLayer?,
    drawInPlace: () -> Boolean,
    markReady: () -> Unit = {},
): Modifier = drawWithContent {
    val target = layer()
    if (target == null) {
        drawContent()
        return@drawWithContent
    }
    target.record { this@drawWithContent.drawContent() }
    if (target.size.width > 0 && target.size.height > 0) markReady()
    if (drawInPlace()) {
        target.alpha = 1f
        target.renderEffect = null
        drawLayer(target)
    }
}

fun Modifier.hideWhileMorphing(): Modifier = graphicsLayer {
    alpha = if (PlayerMorph.active) 0f else 1f
}
