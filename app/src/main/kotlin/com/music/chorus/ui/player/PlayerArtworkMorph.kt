package pushkar.chorus.music.ui.player

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.lerp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Size as CoilSize
import pushkar.chorus.music.constants.PLAYER_MINI_FADE_END
import pushkar.chorus.music.ui.utils.resize
import kotlin.math.roundToInt

internal data class MorphArtTarget(
    val rect: Rect,
    val radiusPx: Float,
    val travels: Boolean = true,
    val heroMask: Boolean = false,
    val shadowPx: Float = 0f,
    val pauseShrinks: Boolean = false,
    val videoPoster: Boolean = true,
    val fadeEndpoints: Boolean = false,
    val fadeOnly: Boolean = false,
)

internal val miniArtworkSlot = MorphSlot<MorphArtTarget>()
internal val fullArtworkSlot = MorphSlot<MorphArtTarget>()

internal val artworkRestScaleState = mutableFloatStateOf(1f)

internal object CanvasPosterSource {
    var captureCallback: ((String?) -> Bitmap?)? = null
    fun capture(mediaId: String?): Bitmap? =
        androidx.compose.runtime.snapshots.Snapshot.withoutReadObservation {
            captureCallback?.invoke(mediaId)
        }
}

@Composable
fun Modifier.miniArtworkMorphSource(cornerRadiusPx: Float): Modifier {
    val owner = remember { Any() }
    DisposableEffect(owner) { onDispose { miniArtworkSlot.release(owner) } }
    return this
        .onGloballyPositioned {
            if (it.isAttached) miniArtworkSlot.report(owner, MorphArtTarget(it.boundsInRoot(), cornerRadiusPx))
        }
        .graphicsLayer {
            alpha = if (!PlayerMorph.active) 1f
            else if (fullArtworkSlot.value?.fadeEndpoints == true && fullArtworkSlot.value?.travels == true) {
                miniArtworkEndpointAlpha(PlayerMorph.progress)
            } else 0f
        }
}

@Composable
fun Modifier.fullArtworkMorphTarget(
    cornerRadiusPx: Float,
    travels: Boolean = true,
    heroMask: Boolean = false,
    shadowPx: Float = 0f,
    pauseShrinks: Boolean = false,
    videoPoster: Boolean = true,
    fadeEndpoints: Boolean = false,
    fadeOnly: Boolean = false,
): Modifier {
    val owner = remember { Any() }
    DisposableEffect(owner) { onDispose { fullArtworkSlot.release(owner) } }
    return this
        .onGloballyPositioned {
            if (!it.isAttached || it.boundsInWindow().isEmpty) return@onGloballyPositioned
            fullArtworkSlot.report(
                owner,
                MorphArtTarget(
                    rect = it.boundsInRoot(),
                    radiusPx = cornerRadiusPx,
                    travels = travels,
                    heroMask = heroMask,
                    shadowPx = shadowPx,
                    pauseShrinks = pauseShrinks,
                    videoPoster = videoPoster,
                    fadeEndpoints = fadeEndpoints,
                    fadeOnly = fadeOnly,
                ),
            )
        }
        .graphicsLayer {
            alpha = if (!PlayerMorph.active) {
                1f
            } else if (fadeOnly) {
                1f
            } else if (fadeEndpoints) {
                fullArtworkEndpointAlpha(PlayerMorph.progress)
            } else if (travels) {
                0f
            } else {
                playerSheetPageAlpha(PlayerMorph.progress)
            }
        }
}

@Composable
fun PlayerArtworkMorphOverlay(
    thumbnailUrl: String?,
    mediaId: String?,
    progress: () -> Float,
) {
    val flying = PlayerMorph.active
    val (liveMini, liveFull) = if (flying) {
        miniArtworkSlot.value to fullArtworkSlot.value
    } else {
        androidx.compose.runtime.snapshots.Snapshot.withoutReadObservation {
            miniArtworkSlot.value to fullArtworkSlot.value
        }
    }
    val frozen = remember(flying) { if (flying) liveMini to liveFull else null }
    val mini = if (PlayerMorph.targetOpen) frozen?.first ?: liveMini else liveMini ?: frozen?.first
    val full = frozen?.second ?: liveFull
    if (mini == null || full == null || thumbnailUrl == null) return
    val poster = remember(flying, mediaId, full.videoPoster) {
        if (flying && full.videoPoster) CanvasPosterSource.capture(mediaId) else null
    }

    if (mini.rect.width <= 0f || mini.rect.height <= 0f || full.rect.width <= 0f || full.rect.height <= 0f) return

    if (!full.travels) {
        FancyArtworkFadeOverlay(
            thumbnailUrl = thumbnailUrl,
            mini = mini,
            progress = progress,
            shrink = !full.fadeOnly,
        )
        return
    }

    val shrink = if (full.pauseShrinks) artworkRestScaleState.floatValue.coerceIn(0.1f, 1f) else 1f
    val target = full.rect.scaledAboutCenter(shrink)
    val side = maxOf(target.width, target.height)
    val density = LocalDensity.current
    val sideDp = with(density) { side.toDp() }
    val heroFade = if (full.heroMask) remember { heroFadeInverseStops() } else null

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    (target.center.x - side / 2f).roundToInt(),
                    (target.center.y - side / 2f).roundToInt(),
                )
            }
            .size(sideDp)
            .graphicsLayer {
                val fraction = progress().coerceIn(0f, 1f)
                if (fraction <= 0f || fraction >= 1f || !PlayerMorph.active) {
                    alpha = 0f
                    shadowElevation = 0f
                    return@graphicsLayer
                }
                alpha = if (full.fadeEndpoints) flyingArtworkEndpointAlpha(fraction) else 1f
                val window = sharedArtworkWindow(mini.rect, target, fraction)
                val imageSide = lerp(maxOf(mini.rect.width, mini.rect.height), side, fraction)
                val scale = imageSide / side
                transformOrigin = TransformOrigin.Center
                scaleX = scale
                scaleY = scale
                translationX = window.center.x - target.center.x
                translationY = window.center.y - target.center.y
                val radius = lerp(mini.radiusPx, full.radiusPx * shrink, fraction)
                shape = CenteredWindowShape(
                    windowWidth = window.width / scale,
                    windowHeight = window.height / scale,
                    radius = radius / scale,
                )
                clip = true
                shadowElevation = full.shadowPx * fraction
                compositingStrategy = if (heroFade != null) CompositingStrategy.Offscreen else CompositingStrategy.Auto
            }
            .then(
                if (heroFade != null) {
                    Modifier.drawWithContent {
                        drawContent()
                        val fraction = progress().coerceIn(0f, 1f)
                        val top = (size.height - target.height) / 2f
                        drawRect(
                            brush = Brush.verticalGradient(
                                colorStops = heroFade,
                                startY = top,
                                endY = top + target.height,
                            ),
                            alpha = fraction,
                            blendMode = BlendMode.DstOut,
                        )
                    }
                } else {
                    Modifier
                },
            ),
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(thumbnailUrl.resize(1200, 1200))
                .size(CoilSize.ORIGINAL)
                .placeholderMemoryCacheKey(thumbnailUrl.resize(1200, 1200))
                .crossfade(false)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(sideDp),
        )
        if (poster != null) {
            val targetWidthDp = with(density) { target.width.toDp() }
            val targetHeightDp = with(density) { target.height.toDp() }
            val posterBitmap = remember(poster) { poster.asImageBitmap() }
            Image(
                bitmap = posterBitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(targetWidthDp, targetHeightDp)
                    .graphicsLayer { alpha = posterAlpha(progress()) },
            )
        }
    }
}

internal fun posterAlpha(progress: Float): Float {
    val t = ((sanitizedProgress(progress) - 0.35f) / 0.55f).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

private fun artworkEndpointEase(value: Float): Float {
    val t = value.coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

internal fun miniArtworkEndpointAlpha(progress: Float): Float =
    1f - artworkEndpointEase(sanitizedProgress(progress) / 0.18f)

internal fun fullArtworkEndpointAlpha(progress: Float): Float =
    artworkEndpointEase((sanitizedProgress(progress) - 0.82f) / 0.18f)

internal fun flyingArtworkEndpointAlpha(progress: Float): Float =
    (1f - miniArtworkEndpointAlpha(progress) - fullArtworkEndpointAlpha(progress)).coerceIn(0f, 1f)

private fun Rect.scaledAboutCenter(factor: Float): Rect =
    if (factor >= 1f) this else Rect(
        left = center.x - width * factor / 2f,
        top = center.y - height * factor / 2f,
        right = center.x + width * factor / 2f,
        bottom = center.y + height * factor / 2f,
    )

private class CenteredWindowShape(
    private val windowWidth: Float,
    private val windowHeight: Float,
    private val radius: Float,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val left = (size.width - windowWidth) / 2f
        val top = (size.height - windowHeight) / 2f
        val r = radius.coerceIn(0f, minOf(windowWidth, windowHeight) / 2f)
        return Outline.Rounded(
            RoundRect(left, top, left + windowWidth, top + windowHeight, CornerRadius(r, r)),
        )
    }
}

private fun heroFadeInverseStops(start: Float = 0.78f, steps: Int = 24): Array<Pair<Float, Color>> {
    val stops = ArrayList<Pair<Float, Color>>(steps + 2)
    stops.add(0f to Color.Transparent)
    stops.add(start to Color.Transparent)
    for (i in 0..steps) {
        val t = i / steps.toFloat()
        val eased = t * t * (3f - 2f * t)
        stops.add((start + (1f - start) * t).coerceIn(0f, 1f) to Color.Black.copy(alpha = eased))
    }
    return stops.toTypedArray()
}

@Composable
private fun FancyArtworkFadeOverlay(
    thumbnailUrl: String,
    mini: MorphArtTarget,
    progress: () -> Float,
    shrink: Boolean,
) {
    val density = LocalDensity.current
    val boxWidthDp = with(density) { mini.rect.width.toDp() }
    val boxHeightDp = with(density) { mini.rect.height.toDp() }
    val miniRadiusDp = with(density) { mini.radiusPx.toDp() }
    Box(
        modifier = Modifier
            .offset { IntOffset(mini.rect.left.roundToInt(), mini.rect.top.roundToInt()) }
            .size(boxWidthDp, boxHeightDp)
            .graphicsLayer {
                val fraction = progress().coerceIn(0f, 1f)
                if (fraction <= 0f || fraction >= PLAYER_MINI_FADE_END || !PlayerMorph.active) {
                    alpha = 0f
                    return@graphicsLayer
                }
                val fade = easeOutCubic((fraction / PLAYER_MINI_FADE_END).coerceIn(0f, 1f))
                alpha = 1f - fade
                val shrinkScale = if (shrink) 1f - 0.15f * fade else 1f
                scaleX = shrinkScale
                scaleY = shrinkScale
                transformOrigin = TransformOrigin.Center
                shape = RoundedCornerShape(miniRadiusDp)
                clip = true
            },
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(thumbnailUrl.resize(1200, 1200))
                .size(CoilSize.ORIGINAL)
                .placeholderMemoryCacheKey(thumbnailUrl.resize(1200, 1200))
                .crossfade(false)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(boxWidthDp, boxHeightDp),
        )
    }
}

fun Modifier.hideWhileArtworkMorphing(progress: () -> Float): Modifier = graphicsLayer {
    val fraction = progress()
    alpha = if (fraction > 0f && fraction < 1f && PlayerMorph.active) 0f else 1f
}
