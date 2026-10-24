package pushkar.chorus.music.ui.player

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.util.lerp

const val ARTWORK_VERTICAL_LINEAR_WEIGHT = 0.4f

const val SCREEN_CORNER_EXPANSION_MILLIS = 140

internal fun sanitizedProgress(progress: Float): Float =
    if (progress.isNaN()) 0f else progress.coerceIn(0f, 1f)

fun easeInCubic(value: Float): Float {
    val clamped = sanitizedProgress(value)
    return clamped * clamped * clamped
}

fun easeOutCubic(value: Float): Float {
    val clamped = sanitizedProgress(value)
    val inverse = 1f - clamped
    return 1f - inverse * inverse * inverse
}

fun sharedContainerRect(source: Rect, target: Rect, progress: Float): Rect {
    val fraction = sanitizedProgress(progress)
    return Rect(
        left = lerp(source.left, target.left, fraction),
        top = lerp(source.top, target.top, fraction),
        right = lerp(source.right, target.right, fraction),
        bottom = lerp(source.bottom, target.bottom, fraction),
    )
}

internal enum class PlayerInputOwner { MINI, FULL, NONE }

internal const val PLAYER_INPUT_MINI_END_PROGRESS = 0.2f

internal fun playerSheetInputOwner(
    targetOpen: Boolean,
    isDragging: Boolean,
    dragStartedFromMiniPlayer: Boolean,
    progress: Float,
    sharedLayersReady: Boolean,
): PlayerInputOwner {
    val safeProgress = sanitizedProgress(progress)
    return when {
        isDragging -> if (dragStartedFromMiniPlayer) PlayerInputOwner.MINI else PlayerInputOwner.FULL
        !targetOpen && safeProgress <= PLAYER_INPUT_MINI_END_PROGRESS ->
            PlayerInputOwner.MINI

        sharedLayersReady || targetOpen && safeProgress >= 1f -> PlayerInputOwner.FULL
        else -> PlayerInputOwner.NONE
    }
}

fun sharedMiniPlayerTitleShift(
    sourcePlayerBounds: Rect,
    animatedPlayerBounds: Rect,
): Float = sourcePlayerBounds.left - animatedPlayerBounds.left

fun sharedMiniPlayerControlsShift(
    sourcePlayerBounds: Rect,
    animatedPlayerBounds: Rect,
    controlsBounds: Rect,
): Float {
    val sourceRightInset = sourcePlayerBounds.right - controlsBounds.right
    return animatedPlayerBounds.right - sourceRightInset - controlsBounds.right
}

fun sharedArtworkRect(source: Rect, target: Rect, progress: Float): Rect {
    val fraction = sanitizedProgress(progress)
    val centerX = lerp(source.center.x, target.center.x, easeOutCubic(fraction))
    val verticalFraction = lerp(
        easeInCubic(fraction),
        fraction,
        ARTWORK_VERTICAL_LINEAR_WEIGHT,
    )
    val centerY = lerp(source.center.y, target.center.y, verticalFraction)
    val sourceWidth = source.width.coerceAtLeast(1f)
    val targetWidth = target.width.coerceAtLeast(sourceWidth)
    val scale = lerp(1f, targetWidth / sourceWidth, fraction)
    val width = sourceWidth * scale
    val height = source.height.coerceAtLeast(1f) * scale
    return Rect(
        left = centerX - width / 2f,
        top = centerY - height / 2f,
        right = centerX + width / 2f,
        bottom = centerY + height / 2f,
    )
}

fun sharedArtworkWindow(source: Rect, target: Rect, progress: Float): Rect {
    val fraction = sanitizedProgress(progress)
    val centerX = lerp(source.center.x, target.center.x, easeOutCubic(fraction))
    val centerY = lerp(
        source.center.y,
        target.center.y,
        lerp(easeInCubic(fraction), fraction, ARTWORK_VERTICAL_LINEAR_WEIGHT),
    )
    val halfWidth = lerp(source.width, target.width, fraction).coerceAtLeast(1f) / 2f
    val halfHeight = lerp(source.height, target.height, fraction).coerceAtLeast(1f) / 2f
    return Rect(centerX - halfWidth, centerY - halfHeight, centerX + halfWidth, centerY + halfHeight)
}

val SeamlessEasing = androidx.compose.animation.core.CubicBezierEasing(0.37f, 0f, 0.63f, 1f)

fun ambientTransitionMillis(crossfadeMs: Long): Int =
    if (crossfadeMs > 0L) crossfadeMs.coerceIn(900L, 8_000L).toInt() else AMBIENT_TRANSITION_DEFAULT_MS

const val AMBIENT_TRANSITION_DEFAULT_MS = 1_100

const val CORNER_EXPANSION_PROGRESS_BAND = 0.02f

internal data class SharedContainerCornerRadii(
    val topStart: Float,
    val topEnd: Float,
    val bottomEnd: Float,
    val bottomStart: Float,
) {
    companion object {
        fun uniform(cornerRadius: Float): SharedContainerCornerRadii =
            SharedContainerCornerRadii(
                topStart = cornerRadius,
                topEnd = cornerRadius,
                bottomEnd = cornerRadius,
                bottomStart = cornerRadius,
            )
    }
}

internal fun sharedContainerCornerRadii(
    source: SharedContainerCornerRadii,
    target: SharedContainerCornerRadii,
    progress: Float,
): SharedContainerCornerRadii {
    val fraction = sanitizedProgress(progress)
    fun interpolate(sourceRadius: Float, targetRadius: Float): Float = if (fraction < 1f) {
        lerp(sourceRadius, targetRadius, fraction)
    } else {
        0f
    }
    return SharedContainerCornerRadii(
        topStart = interpolate(source.topStart, target.topStart),
        topEnd = interpolate(source.topEnd, target.topEnd),
        bottomEnd = interpolate(source.bottomEnd, target.bottomEnd),
        bottomStart = interpolate(source.bottomStart, target.bottomStart),
    )
}

fun sharedContainerCornerRadius(
    collapsedCornerRadius: Float,
    expandedCornerRadius: Float,
    progress: Float,
    screenCornerExpansionProgress: Float,
): Float {
    val fraction = sanitizedProgress(progress)
    val collapsed = if (collapsedCornerRadius.isFinite()) collapsedCornerRadius else 0f
    val expanded = if (expandedCornerRadius.isFinite()) expandedCornerRadius else 0f
    val transitionRadius = lerp(collapsed, expanded, fraction)
    val settled = ((fraction - (1f - CORNER_EXPANSION_PROGRESS_BAND)) / CORNER_EXPANSION_PROGRESS_BAND)
        .coerceIn(0f, 1f)
    val expansion = sanitizedProgress(screenCornerExpansionProgress) * settled
    return lerp(transitionRadius, 0f, expansion)
}

fun playerWindowUsesPhysicalScreenCorners(
    currentWidth: Int,
    currentHeight: Int,
    maximumWidth: Int,
    maximumHeight: Int,
    isInMultiWindowMode: Boolean,
    isInPictureInPictureMode: Boolean,
): Boolean = !isInMultiWindowMode &&
        !isInPictureInPictureMode &&
        currentWidth >= maximumWidth &&
        currentHeight >= maximumHeight
