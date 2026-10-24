package zone.ien.hig

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animate
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.DragScope
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.OnRemeasuredModifier
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.InspectorValueInfo
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

internal fun <T> Modifier.swipeableV2(
    state: SwipeableV2State<T>,
    orientation: Orientation,
    enabled: Boolean = true,
    reverseDirection: Boolean = false,
    interactionSource: MutableInteractionSource? = null,
) = draggable(
    state = state.swipeDraggableState,
    orientation = orientation,
    enabled = enabled,
    interactionSource = interactionSource,
    reverseDirection = reverseDirection,
    startDragImmediately = state.isAnimationRunning,
    onDragStopped = { velocity -> launch { state.settle(velocity) } },
)

internal fun <T> Modifier.swipeAnchors(
    state: SwipeableV2State<T>,
    possibleValues: Set<T>,
    anchorChangeHandler: AnchorChangeHandler<T>? = null,
    calculateAnchor: (value: T, layoutSize: IntSize) -> Float?,
) = this.then(
    SwipeAnchorsModifier(
        onDensityChanged = { state.density = it },
        onSizeChanged = { layoutSize ->
            val previousAnchors = state.anchors
            val newAnchors = mutableMapOf<T, Float>()
            possibleValues.forEach {
                val anchorValue = calculateAnchor(it, layoutSize)
                if (anchorValue != null) {
                    newAnchors[it] = anchorValue
                }
            }
            if (previousAnchors != newAnchors) {
                val previousTarget = state.targetValue
                val stateRequiresCleanup = state.updateAnchors(newAnchors)
                if (stateRequiresCleanup) {
                    anchorChangeHandler?.onAnchorsChanged(
                        previousTarget,
                        previousAnchors,
                        newAnchors,
                    )
                }
            }
        },
        inspectorInfo =
            debugInspectorInfo {
                name = "swipeAnchors"
                properties["state"] = state
                properties["possibleValues"] = possibleValues
                properties["anchorChangeHandler"] = anchorChangeHandler
                properties["calculateAnchor"] = calculateAnchor
            },
    ),
)

@Stable
internal class SwipeableV2State<T>(
    initialValue: T,
    internal val animationSpec: AnimationSpec<Float> = SwipeableV2Defaults.AnimationSpec,
    internal val confirmValueChange: (newValue: T) -> Boolean = { true },
    internal val positionalThreshold: Density.(totalDistance: Float) -> Float =
        SwipeableV2Defaults.PositionalThreshold,
    internal val velocityThreshold: Dp = SwipeableV2Defaults.VelocityThreshold,
) {
    private val swipeMutex = InternalMutatorMutex()

    internal val swipeDraggableState =
        object: DraggableState {
            private val dragScope =
                object: DragScope {
                    override fun dragBy(pixels: Float) {
                        this@SwipeableV2State.dispatchRawDelta(pixels)
                    }
                }

            override suspend fun drag(
                dragPriority: MutatePriority,
                block: suspend DragScope.() -> Unit,
            ) {
                swipe(dragPriority) { dragScope.block() }
            }

            override fun dispatchRawDelta(delta: Float) {
                this@SwipeableV2State.dispatchRawDelta(delta)
            }
        }

    var currentValue: T by mutableStateOf(initialValue)
        private set

    val targetValue: T by derivedStateOf {
        animationTarget ?: run {
            val currentOffset = offset
            if (currentOffset != null) {
                computeTarget(currentOffset, currentValue, velocity = 0f)
            } else {
                currentValue
            }
        }
    }

    @get:Suppress("AutoBoxing")
    var offset: Float? by mutableStateOf(null)
        private set

    fun requireOffset(): Float =
        checkNotNull(offset) {
            "The offset was read before being initialized. Did you access the offset in a phase " +
                "before layout, like effects or composition?"
        }

    val isAnimationRunning: Boolean get() = animationTarget != null

    val progress: Float by derivedStateOf {
        val a = anchors[currentValue] ?: 0f
        val b = anchors[targetValue] ?: 0f
        val distance = abs(b - a)
        if (distance > 1e-6f) {
            val progress = (this.requireOffset() - a) / (b - a)

            if (progress < 1e-6f) {
                0f
            } else if (progress > 1 - 1e-6f) {
                1f
            } else {
                progress
            }
        } else {
            1f
        }
    }

    var lastVelocity: Float by mutableStateOf(0f)
        private set

    val minOffset by derivedStateOf { anchors.minOrNull() ?: Float.NEGATIVE_INFINITY }

    val maxOffset by derivedStateOf { anchors.maxOrNull() ?: Float.POSITIVE_INFINITY }

    private var animationTarget: T? by mutableStateOf(null)

    internal var anchors by mutableStateOf(emptyMap<T, Float>())

    internal var density: Density? = null

    internal fun updateAnchors(newAnchors: Map<T, Float>): Boolean {
        val previousAnchorsEmpty = anchors.isEmpty()
        anchors = newAnchors
        val initialValueHasAnchor =
            if (previousAnchorsEmpty) {
                val initialValue = currentValue
                val initialValueAnchor = anchors[initialValue]
                val initialValueHasAnchor = initialValueAnchor != null
                if (initialValueHasAnchor) trySnapTo(initialValue)
                initialValueHasAnchor
            } else {
                true
            }
        return !initialValueHasAnchor || !previousAnchorsEmpty
    }

    fun hasAnchorForValue(value: T): Boolean = anchors.containsKey(value)

    suspend fun snapTo(targetValue: T) {
        swipe { snap(targetValue) }
    }

    suspend fun animateTo(
        targetValue: T,
        velocity: Float = lastVelocity,
    ) {
        val targetOffset = anchors[targetValue]
        if (targetOffset != null) {
            try {
                swipe {
                    animationTarget = targetValue
                    var prev = offset ?: 0f
                    animate(prev, targetOffset, velocity, animationSpec) { value, velocity ->

                        offset = value
                        prev = value
                        lastVelocity = velocity
                    }
                    lastVelocity = 0f
                }
            } finally {
                animationTarget = null
                val endOffset = requireOffset()
                val endState =
                    anchors
                        .entries
                        .firstOrNull { (_, anchorOffset) -> abs(anchorOffset - endOffset) < 0.5f }
                        ?.key
                this.currentValue = endState ?: currentValue
            }
        } else {
            currentValue = targetValue
        }
    }

    suspend fun settle(velocity: Float) {
        val previousValue = this.currentValue
        val targetValue =
            computeTarget(
                offset = requireOffset(),
                currentValue = previousValue,
                velocity = velocity,
            )
        if (confirmValueChange(targetValue)) {
            animateTo(targetValue, velocity)
        } else {

            animateTo(previousValue, velocity)
        }
    }

    fun dispatchRawDelta(delta: Float): Float {
        val currentDragPosition = offset ?: 0f
        val potentiallyConsumed = currentDragPosition + delta
        val clamped = potentiallyConsumed.coerceIn(minOffset, maxOffset)
        val deltaToConsume = clamped - currentDragPosition
        if (abs(deltaToConsume) >= 0) {
            offset = ((offset ?: 0f) + deltaToConsume).coerceIn(minOffset, maxOffset)
        }
        return deltaToConsume
    }

    private fun computeTarget(
        offset: Float,
        currentValue: T,
        velocity: Float,
    ): T {
        val currentAnchors = anchors
        val currentAnchor = currentAnchors[currentValue]
        val currentDensity = requireDensity()
        val velocityThresholdPx = with(currentDensity) { velocityThreshold.toPx() }
        return if (currentAnchor == offset || currentAnchor == null) {
            currentValue
        } else if (currentAnchor < offset) {

            if (velocity >= velocityThresholdPx) {
                currentAnchors.closestAnchor(offset, true)
            } else {
                val upper = currentAnchors.closestAnchor(offset, true)
                val distance = abs(currentAnchors.getValue(upper) - currentAnchor)
                val relativeThreshold = abs(positionalThreshold(currentDensity, distance))
                val absoluteThreshold = abs(currentAnchor + relativeThreshold)
                if (offset < absoluteThreshold) currentValue else upper
            }
        } else {

            if (velocity <= -velocityThresholdPx) {
                currentAnchors.closestAnchor(offset, false)
            } else {
                val lower = currentAnchors.closestAnchor(offset, false)
                val distance = abs(currentAnchor - currentAnchors.getValue(lower))
                val relativeThreshold = abs(positionalThreshold(currentDensity, distance))
                val absoluteThreshold = abs(currentAnchor - relativeThreshold)
                if (offset < 0) {

                    if (abs(offset) < absoluteThreshold) currentValue else lower
                } else {
                    if (offset > absoluteThreshold) currentValue else lower
                }
            }
        }
    }

    private fun requireDensity() =
        requireNotNull(density) {
            "SwipeableState did not have a density attached. Are you using Modifier.swipeable with " +
                "this=$this SwipeableState?"
        }

    private suspend fun swipe(
        swipePriority: MutatePriority = MutatePriority.Default,
        action: suspend () -> Unit,
    ): Unit = coroutineScope { swipeMutex.mutate(swipePriority, action) }

    internal fun trySnapTo(targetValue: T): Boolean = swipeMutex.tryMutate { snap(targetValue) }

    private fun snap(targetValue: T) {
        val targetOffset = anchors[targetValue]
        if (targetOffset != null) {
            dispatchRawDelta(targetOffset - (offset ?: 0f))
            currentValue = targetValue
            animationTarget = null
        } else {
            currentValue = targetValue
        }
    }

    companion object {

        fun <T: Any> Saver(
            animationSpec: AnimationSpec<Float>,
            confirmValueChange: (T) -> Boolean,
            positionalThreshold: Density.(distance: Float) -> Float,
            velocityThreshold: Dp,
        ) = Saver<SwipeableV2State<T>, T>(
            save = { it.currentValue },
            restore = {
                SwipeableV2State(
                    initialValue = it,
                    animationSpec = animationSpec,
                    confirmValueChange = confirmValueChange,
                    positionalThreshold = positionalThreshold,
                    velocityThreshold = velocityThreshold,
                )
            },
        )
    }
}

@Composable
internal fun <T: Any> rememberSwipeableV2State(
    initialValue: T,
    animationSpec: AnimationSpec<Float> = SwipeableV2Defaults.AnimationSpec,
    confirmValueChange: (newValue: T) -> Boolean = { true },
): SwipeableV2State<T> =
    rememberSaveable(
        initialValue,
        animationSpec,
        confirmValueChange,
        saver =
            SwipeableV2State.Saver(
                animationSpec = animationSpec,
                confirmValueChange = confirmValueChange,
                positionalThreshold = SwipeableV2Defaults.PositionalThreshold,
                velocityThreshold = SwipeableV2Defaults.VelocityThreshold,
            ),
    ) {
        SwipeableV2State(
            initialValue = initialValue,
            animationSpec = animationSpec,
            confirmValueChange = confirmValueChange,
            positionalThreshold = SwipeableV2Defaults.PositionalThreshold,
            velocityThreshold = SwipeableV2Defaults.VelocityThreshold,
        )
    }

internal fun fixedPositionalThreshold(threshold: Dp): Density.(distance: Float) -> Float =
    {
        threshold.toPx()
    }

internal fun fractionalPositionalThreshold(fraction: Float): Density.(distance: Float) -> Float = { distance -> distance * fraction }

@Stable
internal object SwipeableV2Defaults {

    val AnimationSpec = SpringSpec<Float>()

    val VelocityThreshold: Dp = 125.dp

    val PositionalThreshold: Density.(totalDistance: Float) -> Float =
        fixedPositionalThreshold(56.dp)

    internal fun <T> ReconcileAnimationOnAnchorChangeHandler(
        state: SwipeableV2State<T>,
        animate: (target: T, velocity: Float) -> Unit,
        snap: (target: T) -> Unit,
    ) = AnchorChangeHandler { previousTarget, previousAnchors, newAnchors ->
        val previousTargetOffset = previousAnchors[previousTarget]
        val newTargetOffset = newAnchors[previousTarget]
        if (previousTargetOffset != newTargetOffset) {
            if (newTargetOffset != null) {
                animate(previousTarget, state.lastVelocity)
            } else {
                snap(newAnchors.closestAnchor(offset = state.requireOffset()))
            }
        }
    }
}

internal fun interface AnchorChangeHandler<T> {

    fun onAnchorsChanged(
        previousTargetValue: T,
        previousAnchors: Map<T, Float>,
        newAnchors: Map<T, Float>,
    )
}

@Stable
private class SwipeAnchorsModifier(
    private val onDensityChanged: (density: Density) -> Unit,
    private val onSizeChanged: (layoutSize: IntSize) -> Unit,
    inspectorInfo: InspectorInfo.() -> Unit,
): InspectorValueInfo(inspectorInfo),
    LayoutModifier,
    OnRemeasuredModifier {
    private var lastDensity: Float = -1f
    private var lastFontScale: Float = -1f

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        if (density != lastDensity || fontScale != lastFontScale) {
            onDensityChanged(Density(density, fontScale))
            lastDensity = density
            lastFontScale = fontScale
        }
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }

    override fun onRemeasured(size: IntSize) {
        onSizeChanged(size)
    }

    override fun toString() =
        "SwipeAnchorsModifierImpl(updateDensity=$onDensityChanged, " +
            "onSizeChanged=$onSizeChanged)"
}

private fun <T> Map<T, Float>.closestAnchor(
    offset: Float = 0f,
    searchUpwards: Boolean = false,
): T {
    require(isNotEmpty()) { "The anchors were empty when trying to find the closest anchor" }
    return minBy { (_, anchor) ->
        val delta = if (searchUpwards) anchor - offset else offset - anchor
        if (delta < 0) Float.POSITIVE_INFINITY else delta
    }.key
}

private fun <T> Map<T, Float>.minOrNull() = minOfOrNull { (_, offset) -> offset }

private fun <T> Map<T, Float>.maxOrNull() = maxOfOrNull { (_, offset) -> offset }
