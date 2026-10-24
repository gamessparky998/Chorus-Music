package zone.ien.hig

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import zone.ien.hig.CupertinoSheetState.Companion.Saver
import zone.ien.hig.theme.CupertinoTheme
import kotlin.jvm.JvmName

@Composable
fun CupertinoBottomSheetContent(
    modifier: Modifier = Modifier,
    containerColor: Color =
        LocalContainerColor.current.takeOrElse {
            CupertinoBottomSheetDefaults.containerColor
        },
    contentColor: Color =
        LocalContentColor.current.takeOrElse {
            CupertinoBottomSheetDefaults.contentColor
        },
    appBarsAlpha: Float = LocalAppBarsBlurAlpha.current,
    appBarsBlurRadius: Dp = LocalAppBarsBlurRadius.current,
    hasNavigationTitle: Boolean = false,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    CupertinoScaffold(
        modifier = modifier,
        containerColor = containerColor,
        contentColor = contentColor,
        appBarsBlurAlpha = appBarsAlpha,
        appBarsBlurRadius = appBarsBlurRadius,
        hasNavigationTitle = hasNavigationTitle,
        topBar = {
            Box(
                Modifier.pointerInput(0) {},
            ) {
                topBar()
            }
        },
        bottomBar = bottomBar,
        contentWindowInsets =
            CupertinoScaffoldDefaults.contentWindowInsets.union(
                WindowInsets(bottom = CupertinoBottomSheetTokens.MaxOverflow),
            ),
        content = content,
    )
}

@Immutable
object CupertinoBottomSheetDefaults {
    val ShadowElevation: Dp = 4.dp

    val contentColor: Color
        @Composable
        @ReadOnlyComposable
        get() = CupertinoTheme.colorScheme.label

    val containerColor: Color
        @Composable
        @ReadOnlyComposable
        get() = CupertinoTheme.colorScheme.secondarySystemGroupedBackground

    val shape: Shape
        @Composable
        @ReadOnlyComposable
        get() =
            CupertinoTheme.shapes.large.copy(bottomStart = CornerSize(0.dp), bottomEnd = CornerSize(0.dp))

    @Composable
    fun DragHandle(
        modifier: Modifier = Modifier,
        width: Dp = DragHandleWidth,
        height: Dp = DragHandleHeight,
        shape: Shape = CircleShape,
        color: Color = CupertinoTheme.colorScheme.systemFill,
    ) {
        CupertinoSurface(
            modifier =
                modifier
                    .padding(vertical = DragHandlePadding),
            shape = shape,
            color = color,
        ) {
            Spacer(
                modifier =
                    Modifier
                        .size(
                            width = width,
                            height = height,
                        ),
            )
        }
    }
}

@Stable
@Immutable
enum class PresentationContentInteraction {
    Scroll,
    Resize,
}

@Serializable
@Immutable
sealed interface PresentationDetent {

    fun calculate(
        density: Density,
        height: Float,
    ): Float

    @Immutable
    @Serializable
    data object Large : PresentationDetent {
        override fun calculate(
            density: Density,
            height: Float,
        ): Float {
            return height + Float.MIN_VALUE
        }
    }

    @Immutable
    @Serializable
    data object Medium : PresentationDetent by Fraction(.5f)

    @Immutable
    @Serializable(with = HeightSerializer::class)
    class Height(
        internal val height: Dp,
    ) : PresentationDetent {
        override fun hashCode(): Int = height.hashCode()

        override fun equals(other: Any?): Boolean = height == (other as? Height)?.height

        override fun calculate(
            density: Density,
            height: Float,
        ): Float = (density.run { this@Height.height.toPx() } * height).coerceAtMost(height)

        override fun toString(): String = "Height(height=$height)"
    }

    @Immutable
    @Serializable
    class Fraction(
        private val fraction: Float,
    ) : PresentationDetent {
        init {
            require(fraction in 0f..1f) {
                "fraction must be between 0 and 1"
            }
        }

        override fun hashCode(): Int = fraction.hashCode()

        override fun equals(other: Any?): Boolean = fraction == (other as? Fraction)?.fraction

        override fun toString(): String = "Fraction(fraction=$fraction)"

        override fun calculate(
            density: Density,
            height: Float,
        ): Float = fraction * height
    }
}

@Serializable
@Immutable
sealed interface CupertinoSheetValue {

    @Serializable
    @Immutable
    data object Hidden : CupertinoSheetValue

    @Serializable
    @Immutable
    class PartiallyExpanded(
        val detent: PresentationDetent,
    ) : CupertinoSheetValue {
        override fun equals(other: Any?): Boolean = detent == (other as? PartiallyExpanded)?.detent

        override fun hashCode(): Int = detent.hashCode()

        override fun toString(): String = "PartiallyExpanded(detent = $detent)"
    }

    @Serializable
    @Immutable
    data object Expanded : CupertinoSheetValue
}

@Immutable
sealed interface PresentationStyle {
    @Immutable
    data object Fullscreen : PresentationStyle

    @Immutable
    class Modal(
        val detents: Set<PresentationDetent> = setOf(PresentationDetent.Large),
        val contentInteraction: PresentationContentInteraction = PresentationContentInteraction.Resize,
        val isBackgroundInteractive: (PresentationDetent) -> Boolean = { false },
        val dismissOnClickOutside: Boolean = true,
    ) : PresentationStyle {
        init {
            require(detents.isNotEmpty()) {
                "Modal predentation style must have at least one detent"
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as Modal

            if (detents != other.detents) return false
            if (contentInteraction != other.contentInteraction) return false
            if (isBackgroundInteractive != other.isBackgroundInteractive) return false
            if (dismissOnClickOutside != other.dismissOnClickOutside) return false

            return true
        }

        override fun hashCode(): Int {
            var result = detents.hashCode()
            result = 31 * result + contentInteraction.hashCode()
            result = 31 * result + isBackgroundInteractive.hashCode()
            result = 31 * result + dismissOnClickOutside.hashCode()
            return result
        }

        override fun toString(): String =
            "Modal(detents=$detents, contentInteraction=$contentInteraction,  dismissOnClickOutside=$dismissOnClickOutside)"
    }
}

@Stable
class CupertinoSheetState(
    initialValue: CupertinoSheetValue = CupertinoSheetValue.Hidden,
    internal val presentationStyle: PresentationStyle = PresentationStyle.Modal(),
    internal val confirmValueChange: (CupertinoSheetValue) -> Boolean = { true },
) {
    init {
        require(
            initialValue !is CupertinoSheetValue.PartiallyExpanded ||
                    (
                            presentationStyle is PresentationStyle.Modal &&
                                    presentationStyle.detents.any { it == initialValue.detent }
                            ),
        ) {
            "initialValue must be presented in modal detents"
        }
    }

    val currentValue: CupertinoSheetValue get() = swipeableState.currentValue
    val targetValue: CupertinoSheetValue get() = swipeableState.targetValue
    val isVisible: Boolean
        get() = swipeableState.currentValue !is CupertinoSheetValue.Hidden

    fun requireOffset(): Float = swipeableState.requireOffset()

    internal val hasPartiallyExpandedState: Boolean
        get() =
            (presentationStyle as? PresentationStyle.Modal)?.detents?.let {
                it.size > 1
            } == true

    internal var expandedDetent: PresentationDetent? by mutableStateOf(null)

    suspend fun expand() {
        swipeableState.animateTo(CupertinoSheetValue.Expanded)
    }

    suspend fun partialExpand(detent: PresentationDetent) {
        require(presentationStyle is PresentationStyle.Modal) {
            "Sheet can be partially expanded only in Modal PresentationStyle"
        }

        require(presentationStyle.detents.any { it == detent }) {
            "Detent $detent is not in detents list"
        }
        animateTo(CupertinoSheetValue.PartiallyExpanded(detent))
    }

    suspend fun show() {
        val targetValue =
            when (presentationStyle) {
                is PresentationStyle.Fullscreen -> CupertinoSheetValue.Expanded
                is PresentationStyle.Modal -> {
                    val partialDetent = presentationStyle.detents.firstOrNull { it !is PresentationDetent.Large }
                    if (partialDetent != null) {
                        CupertinoSheetValue.PartiallyExpanded(partialDetent)
                    } else {
                        CupertinoSheetValue.Expanded
                    }
                }
            }
        animateTo(targetValue)
    }

    suspend fun hide() {
        animateTo(CupertinoSheetValue.Hidden)
    }

    internal suspend fun animateTo(
        targetValue: CupertinoSheetValue,
        velocity: Float = swipeableState.lastVelocity,
    ) {
        swipeableState.animateTo(targetValue, velocity)
    }

    internal suspend fun snapTo(targetValue: CupertinoSheetValue) {
        swipeableState.snapTo(targetValue)
    }

    internal fun trySnapTo(targetValue: CupertinoSheetValue) = swipeableState.trySnapTo(targetValue)
    internal suspend fun settle(velocity: Float) {
        swipeableState.settle(velocity)
    }

    internal var swipeableState =
        SwipeableV2State(
            initialValue = initialValue,
            animationSpec = cupertinoTween(),
            confirmValueChange = confirmValueChange,
            positionalThreshold = { .5f * it },
        )

    internal val offset: Float? get() = swipeableState.offset

    companion object {
        fun Saver(
            presentationStyle: PresentationStyle = PresentationStyle.Modal(),
            confirmValueChange: (CupertinoSheetValue) -> Boolean,
        ) = Saver<CupertinoSheetState, String>(
            save = { Json.encodeToString(it.swipeableState.currentValue) },
            restore = { savedValue ->
                CupertinoSheetState(
                    presentationStyle = presentationStyle,
                    initialValue = Json.decodeFromString(savedValue),
                    confirmValueChange = confirmValueChange,
                )
            },
        )
    }
}

@Composable
fun rememberCupertinoSheetState(
    initialValue: CupertinoSheetValue = CupertinoSheetValue.Hidden,
    presentationStyle: PresentationStyle = PresentationStyle.Modal(),
    confirmValueChange: (CupertinoSheetValue) -> Boolean = { true },
): CupertinoSheetState {
    val updatedConfirm by rememberUpdatedState(confirmValueChange)

    return rememberSaveable(
        presentationStyle,
        saver =
            CupertinoSheetState.Saver(
                presentationStyle = presentationStyle,
                confirmValueChange = {
                    updatedConfirm.invoke(it)
                },
            ),
    ) {
        CupertinoSheetState(
            initialValue = initialValue,
            confirmValueChange = {
                updatedConfirm.invoke(it)
            },
            presentationStyle = presentationStyle,
        )
    }
}

class HeightSerializer : KSerializer<PresentationDetent.Height> {
    override val descriptor: SerialDescriptor
        get() = Float.serializer().descriptor

    override fun deserialize(decoder: Decoder): PresentationDetent.Height =
        PresentationDetent.Height(decoder.decodeFloat().dp)

    override fun serialize(
        encoder: Encoder,
        value: PresentationDetent.Height,
    ) {
        encoder.encodeFloat(value.height.value)
    }
}

internal fun ConsumeSwipeWithinBottomSheetBoundsNestedScrollConnection(
    sheetState: CupertinoSheetState,
    orientation: Orientation,
    onFling: (velocity: Float) -> Unit,
): NestedScrollConnection =
    object : NestedScrollConnection {
        private val enabled: Boolean
            get() =
                sheetState.presentationStyle.let {
                    it is PresentationStyle.Modal && it.contentInteraction == PresentationContentInteraction.Resize
                }

        override fun onPreScroll(
            available: Offset,
            source: NestedScrollSource,
        ): Offset {
            if (!enabled) {
                return Offset.Zero
            }
            val delta = available.toFloat()

            return if (delta < 0 && source == NestedScrollSource.UserInput) {
                sheetState.swipeableState.dispatchRawDelta(delta).toOffset()
            } else {
                Offset.Zero
            }
        }

        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource,
        ): Offset {
            if (!enabled) {
                return Offset.Zero
            }

            return if (source == NestedScrollSource.UserInput) {
                sheetState.swipeableState.dispatchRawDelta(available.toFloat()).toOffset()
            } else {
                Offset.Zero
            }
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            if (!enabled) {
                return Velocity.Zero
            }

            val toFling = available.toFloat()
            val currentOffset = sheetState.requireOffset()
            return if (toFling < 0 && currentOffset > sheetState.swipeableState.minOffset) {
                onFling(toFling)

                available
            } else {
                Velocity.Zero
            }
        }

        override suspend fun onPostFling(
            consumed: Velocity,
            available: Velocity,
        ): Velocity {
            if (!enabled) {
                return Velocity.Zero
            }

            onFling(available.toFloat())
            return available
        }

        private fun Float.toOffset(): Offset =
            Offset(
                x = if (orientation == Orientation.Horizontal) this else 0f,
                y = if (orientation == Orientation.Vertical) this else 0f,
            )

        @JvmName("velocityToFloat")
        private fun Velocity.toFloat() = if (orientation == Orientation.Horizontal) x else y

        @JvmName("offsetToFloat")
        private fun Offset.toFloat(): Float = if (orientation == Orientation.Horizontal) x else y
    }

internal val CupertinoSheetState.isBackgroundInteractive: Boolean
    get() =
        with(targetValue) {

            if (this == CupertinoSheetValue.Hidden) {
                return true
            }

            if (presentationStyle !is PresentationStyle.Modal) {
                return false
            }

            if (this is CupertinoSheetValue.PartiallyExpanded) {
                return presentationStyle.isBackgroundInteractive(detent)
            }

            expandedDetent?.let {
                presentationStyle.isBackgroundInteractive(it)
            } == true
        }

internal object CupertinoBottomSheetTokens {
    internal val MaxOverflow = 5.dp
}

internal val DragHandleHeight = 5.dp
internal val DragHandleWidth = 38.dp
internal val DragHandlePadding = 4.dp
