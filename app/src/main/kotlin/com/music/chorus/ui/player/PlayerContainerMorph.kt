package pushkar.chorus.music.ui.player

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import kotlin.math.roundToInt
import pushkar.chorus.music.constants.PLAYER_CONTENT_APPEAR_END
import pushkar.chorus.music.constants.PLAYER_CONTENT_APPEAR_START
import pushkar.chorus.music.constants.PLAYER_LAYER_HANDOFF_PROGRESS
import pushkar.chorus.music.constants.PLAYER_MINI_FADE_END

private val miniContainerSlot = MorphSlot<Rect>()

internal val miniPlayerContainerRect: Rect? get() = miniContainerSlot.value

@Composable
fun Modifier.miniContainerMorphSource(): Modifier {
    val owner = androidx.compose.runtime.remember { Any() }
    androidx.compose.runtime.DisposableEffect(owner) { onDispose { miniContainerSlot.release(owner) } }
    return onGloballyPositioned {
        if (it.isAttached) miniContainerSlot.report(owner, it.boundsInRoot())
    }
}

private val miniControlsPlayRect = mutableStateOf<Rect?>(null)
private val miniControlsNextRect = mutableStateOf<Rect?>(null)

internal val miniPlayerControlsRect: Rect?
    get() {
        val play = miniControlsPlayRect.value
        val next = miniControlsNextRect.value
        if (play == null) return next
        if (next == null) return play
        return Rect(
            left = minOf(play.left, next.left),
            top = minOf(play.top, next.top),
            right = maxOf(play.right, next.right),
            bottom = maxOf(play.bottom, next.bottom),
        )
    }

internal fun clearMiniContentControlsRects() {
    miniControlsPlayRect.value = null
    miniControlsNextRect.value = null
}

fun Modifier.registerMiniControlsPlayRect(): Modifier = onGloballyPositioned {
    if (it.isAttached) miniControlsPlayRect.value = it.boundsInRoot()
}

fun Modifier.registerMiniControlsNextRect(): Modifier = onGloballyPositioned {
    if (it.isAttached) miniControlsNextRect.value = it.boundsInRoot()
}

fun playerSheetMiniContentAlpha(progress: Float): Float =
    1f - easeOutCubic((progress / PLAYER_MINI_FADE_END).coerceIn(0f, 1f))

fun playerSheetPageAlpha(progress: Float): Float =
    easeInCubic(
        (
                (progress - PLAYER_CONTENT_APPEAR_START) /
                        (PLAYER_CONTENT_APPEAR_END - PLAYER_CONTENT_APPEAR_START)
                ).coerceIn(0f, 1f),
    )

fun playerSheetBackgroundAlpha(progress: Float): Float =
    easeInCubic((progress / PLAYER_LAYER_HANDOFF_PROGRESS).coerceIn(0f, 1f))

@Composable
fun PlayerContainerMorphOverlay(
    progress: () -> Float,
    color: Color,
    expandedCornerRadius: Dp,
) {
    Spacer(
        modifier = Modifier
            .fillMaxSize()
            .drawWithCache {
                val clipPath = Path()
                val shadowLayer = obtainGraphicsLayer()
                var flightMini: Rect? = null
                var flightControls: Rect? = null
                onDrawBehind {
                    val fraction = progress().coerceIn(0f, 1f)
                    val landing = fraction <= 0f && PlayerMorph.landingActive
                    if ((fraction <= 0f && !landing) || fraction >= 1f || !PlayerMorph.sharedLayersReady) {
                        flightMini = null
                        flightControls = null
                        return@onDrawBehind
                    }
                    if (flightMini == null) {
                        flightMini = miniContainerSlot.value
                        flightControls = miniPlayerControlsRect
                    }
                    val mini = (if (PlayerMorph.targetOpen) flightMini else miniContainerSlot.value ?: flightMini)
                        ?: return@onDrawBehind
                    val controlsNow =
                        if (PlayerMorph.targetOpen) flightControls else miniPlayerControlsRect ?: flightControls
                    if (mini.width <= 0f || mini.height <= 0f) return@onDrawBehind
                    val miniLayer = PlayerMorph.miniLayer ?: return@onDrawBehind
                    val fullBackgroundLayer = PlayerMorph.fullBackgroundLayer ?: return@onDrawBehind
                    val fullContentLayer = PlayerMorph.fullContentLayer ?: return@onDrawBehind
                    if (fullBackgroundLayer.size.width <= 0 || fullContentLayer.size.width <= 0) return@onDrawBehind

                    val full = Rect(0f, 0f, size.width, size.height)
                    val rect = if (landing) landingCardRect(mini) else sharedContainerRect(mini, full, fraction)
                    val radius = if (landing) rect.height / 2f else sharedContainerCornerRadii(
                        source = SharedContainerCornerRadii.uniform(mini.height / 2f),
                        target = SharedContainerCornerRadii.uniform(expandedCornerRadius.toPx()),
                        progress = fraction,
                    ).topStart

                    val shadowAlpha = if (PlayerMorph.targetOpen) 1f - playerSheetBackgroundAlpha(fraction) else 0f
                    if (shadowAlpha > 0f && PlayerMorph.miniShadowElevationPx > 0f) {
                        shadowLayer.topLeft = androidx.compose.ui.unit.IntOffset(
                            rect.left.roundToInt(),
                            rect.top.roundToInt(),
                        )
                        shadowLayer.setRoundRectOutline(Offset.Zero, rect.size, radius)
                        shadowLayer.shadowElevation = PlayerMorph.miniShadowElevationPx * shadowAlpha
                        shadowLayer.record(
                            androidx.compose.ui.unit.IntSize(rect.width.roundToInt(), rect.height.roundToInt()),
                        ) { }
                        drawLayer(shadowLayer)
                    }
                    val stay = PlayerMorph.stayInPlace
                    if (stay) {
                        if (fraction > 0f) {
                            fullBackgroundLayer.alpha = playerSheetBackgroundAlpha(fraction)
                            fullBackgroundLayer.renderEffect = null
                            drawLayer(fullBackgroundLayer)
                        }
                        if (fraction > PLAYER_CONTENT_APPEAR_START) {
                            fullContentLayer.alpha = playerSheetPageAlpha(fraction)
                            fullContentLayer.renderEffect = null
                            drawLayer(fullContentLayer)
                        }
                    }
                    clipPath.reset()
                    clipPath.addRoundRect(RoundRect(rect, CornerRadius(radius, radius)))
                    clipPath(clipPath) {
                        translate(left = rect.left, top = rect.top) {
                            val plateAlpha = 1f - playerSheetBackgroundAlpha(fraction)
                            if (plateAlpha > 0f) {
                                val surface = PlayerMorph.miniSurfaceLayer
                                if (surface != null && surface.size.width > 0 && surface.size.height > 0) {
                                    surface.alpha = plateAlpha
                                    scale(
                                        scaleX = rect.width / surface.size.width,
                                        scaleY = rect.height / surface.size.height,
                                        pivot = Offset.Zero,
                                    ) { drawLayer(surface) }
                                } else {
                                    drawRect(color = color, size = rect.size, alpha = plateAlpha)
                                }
                            }
                            if (miniLayer.size.width > 0 && fraction < PLAYER_MINI_FADE_END) {
                                miniLayer.alpha = playerSheetMiniContentAlpha(fraction)
                                miniLayer.renderEffect = null
                                if (landing) {
                                    scale(
                                        scaleX = rect.width / miniLayer.size.width,
                                        scaleY = rect.height / miniLayer.size.height,
                                        pivot = Offset.Zero,
                                    ) { drawLayer(miniLayer) }
                                } else {
                                    drawTrackedMiniLayer(
                                        miniLayer = miniLayer,
                                        mini = mini,
                                        container = rect,
                                        controls = controlsNow,
                                    )
                                }
                            }
                            val scale = rect.width / size.width
                            if (fraction > 0f && !stay) {
                                fullBackgroundLayer.alpha = playerSheetBackgroundAlpha(fraction)
                                fullBackgroundLayer.renderEffect = null
                                scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero) {
                                    drawLayer(fullBackgroundLayer)
                                }
                            }
                            if (fraction > PLAYER_CONTENT_APPEAR_START && !stay) {
                                fullContentLayer.alpha = playerSheetPageAlpha(fraction)
                                fullContentLayer.renderEffect = null
                                scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero) {
                                    drawLayer(fullContentLayer)
                                }
                            }
                        }
                    }
                }
            },
    )
}

private const val LANDING_SQUASH = 0.14f
private const val LANDING_SPREAD = 0.04f

private fun landingCardRect(mini: Rect): Rect {
    val dip = PlayerMorph.landingDipPx()
    val maxDip = PlayerMorph.landingMaxDipPx
    val squash = if (maxDip > 0f) (dip / maxDip).coerceIn(-1f, 1f) else 0f
    val height = mini.height * (1f - LANDING_SQUASH * squash)
    val width = mini.width * (1f + LANDING_SPREAD * squash)
    val bottom = mini.bottom + dip
    val centerX = mini.center.x
    return Rect(centerX - width / 2f, bottom - height, centerX + width / 2f, bottom)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTrackedMiniLayer(
    miniLayer: androidx.compose.ui.graphics.layer.GraphicsLayer,
    mini: Rect,
    container: Rect,
    controls: Rect?,
) {
    if (controls == null || controls.width <= 0f || controls.height <= 0f) {
        drawLayer(miniLayer)
        return
    }
    val split = controls.left - mini.left
    val titleShift = sharedMiniPlayerTitleShift(
        sourcePlayerBounds = mini,
        animatedPlayerBounds = container,
    )
    val edgeShift = sharedMiniPlayerControlsShift(
        sourcePlayerBounds = mini,
        animatedPlayerBounds = container,
        controlsBounds = controls,
    )
    clipRect(right = titleShift + split) {
        withTransform({
            translate(left = titleShift, top = 0f)
        }) {
            drawLayer(miniLayer)
        }
    }
    clipRect(left = titleShift + split + edgeShift) {
        withTransform({
            translate(left = titleShift + edgeShift, top = 0f)
        }) {
            drawLayer(miniLayer)
        }
    }
}
