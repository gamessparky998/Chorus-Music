package pushkar.chorus.music.ui.fluid.component

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import pushkar.chorus.music.ui.component.shapes.ContinuousRoundedRectangle
import pushkar.chorus.music.ui.fluid.foundation.FluidTheme
import pushkar.chorus.music.ui.fluid.foundation.LocalFluidContentColor
import pushkar.chorus.music.ui.fluid.foundation.LocalFluidTextStyle
import kotlin.math.roundToInt

@Immutable
data class FluidMenuItemData(
    val title: String,
    val description: String? = null,
    val icon: (@Composable (Color) -> Unit)? = null,
    val role: FluidButtonRole? = null,
    val selected: Boolean = false,
    val enabled: Boolean = true,
    val onClick: () -> Unit = {},
)

@Immutable
data class FluidMenuSectionData(
    val items: List<FluidMenuItemData>,
    val header: String? = null,
)

private const val PressScale = 0.96f
private const val LandFade = 0.4f
private val CardPadding = 12.dp

private object WindowOrigin : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset = IntOffset.Zero
}

@Composable
private fun Transition<Boolean>.menuAxis(
    dampingRatio: Float,
    stiffness: Float,
    label: String,
) = animateFloat(
    transitionSpec = {
        if (targetState) spring(dampingRatio = dampingRatio, stiffness = stiffness)
        else spring(dampingRatio = 1f, stiffness = 520f)
    },
    label = label,
) { if (it) 1f else 0f }

@Composable
fun AppleContextMenu(
    sections: List<FluidMenuSectionData>,
    preview: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    quickActions: List<FluidMenuItemData> = emptyList(),
    onClick: (() -> Unit)? = null,
    controller: AppleMenuController? = null,
    content: @Composable () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val menu = controller?.state ?: remember { AppleContextMenuState() }
    val anchor = remember { arrayOf(Rect.Zero) }

    Box(
        modifier = modifier
            .onGloballyPositioned { anchor[0] = it.boundsInWindow() }
            .graphicsLayer {
                val s = 1f - (1f - PressScale) * menu.press.value
                scaleX = s
                scaleY = s
                alpha = menu.itemAlpha()
            }
            .drawWithContent {
                drawContent()
                val p = menu.press.value
                if (p > 0f) drawRect(Color.Black.copy(alpha = 0.14f * p))
            }
            .then(if (controller != null) Modifier else Modifier.pointerInput(onClick) {
                val dismissDown = with(density) { 64.dp.toPx() }
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val sink = scope.launch { menu.press.animateTo(1f, tween(300)) }
                    val lifted = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                        while (true) {
                            val change = awaitPointerEvent().changes.firstOrNull() ?: return@withTimeoutOrNull false
                            if (!change.pressed) return@withTimeoutOrNull !change.isConsumed
                            if (change.isConsumed ||
                                (change.position - down.position).getDistance() > viewConfiguration.touchSlop
                            ) return@withTimeoutOrNull false
                        }
                        @Suppress("UNREACHABLE_CODE") false
                    }
                    sink.cancel()
                    scope.launch { menu.press.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = 600f)) }
                    when (lifted) {
                        true -> onClick?.invoke()
                        false -> Unit
                        null -> {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            scope.launch { menu.drag.snapTo(Offset.Zero) }
                            menu.open()
                            val start = down.position
                            while (true) {
                                val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull() ?: break
                                change.consume()
                                if (!change.pressed) break
                                val total = change.position - start
                                scope.launch { menu.drag.snapTo(rubberBand(total)) }
                            }
                            val drag = menu.drag.value
                            val home = menu.towardHome()
                            val homeLen = home.getDistance()
                            val backHome = homeLen > 1f &&
                                    (drag.x * home.x + drag.y * home.y) / homeLen > 0.4f * homeLen
                            if (drag.y > dismissDown || backHome) {
                                menu.close()
                            } else {
                                scope.launch {
                                    menu.drag.animateTo(
                                        Offset.Zero,
                                        spring(dampingRatio = 0.75f, stiffness = 500f)
                                    )
                                }
                            }
                        }
                    }
                }
            }),
    ) { content() }

    AppleContextMenuOverlay(
        menu = menu,
        anchorOf = { anchor[0] },
        sections = sections,
        quickActions = quickActions,
        preview = preview,
        onPreviewClick = onClick,
    )
}

private fun rubberBand(d: Offset) = Offset(d.x * 0.6f, if (d.y < 0f) d.y * 0.3f else d.y)

@Stable
class AppleMenuController internal constructor() {
    internal val state = AppleContextMenuState()
    val isOpen: Boolean get() = state.visible
    fun open() = state.open()
    fun cancel() = state.cancel()
}

@Composable
fun rememberAppleMenuController(): AppleMenuController = remember { AppleMenuController() }

@Stable
internal class AppleContextMenuState {
    var transition by mutableStateOf(MutableTransitionState(false))
    val press = Animatable(0f)
    val drag = Animatable(Offset.Zero, Offset.VectorConverter)
    val placed = FloatArray(4)
    var rightSide = false
    var progress: State<Float>? = null

    fun itemAlpha(): Float {
        if (!visible) return 1f
        if (transition.targetState) return 0f
        val p = progress?.value ?: return 0f
        return (1f - p / LandFade).coerceIn(0f, 1f)
    }

    fun previewAlpha(p: Float): Float =
        if (transition.targetState) 1f else (p / LandFade).coerceIn(0f, 1f)

    val visible: Boolean get() = transition.currentState || transition.targetState

    fun open() {
        transition.targetState = true
    }

    fun close() {
        transition.targetState = false
    }

    fun cancel() {
        transition = MutableTransitionState(false)
    }

    fun towardHome() = Offset(placed[2] - placed[0], placed[3] - placed[1])
}

@Composable
private fun AppleContextMenuOverlay(
    menu: AppleContextMenuState,
    anchorOf: () -> Rect,
    sections: List<FluidMenuSectionData>,
    quickActions: List<FluidMenuItemData>,
    preview: @Composable () -> Unit,
    onPreviewClick: (() -> Unit)?,
) {
    if (!menu.visible) return
    val anchor = remember(menu.transition) { anchorOf() }

    val transition = rememberTransition(menu.transition, label = "appleContextMenu")
    val p = transition.animateFloat(
        transitionSpec = {
            if (targetState) spring(dampingRatio = 0.8f, stiffness = 420f) else spring(
                dampingRatio = 1f,
                stiffness = 520f
            )
        },
        label = "lift",
    ) { if (it) 1f else 0f }
    menu.progress = p

    val mx = transition.menuAxis(dampingRatio = 0.62f, stiffness = 560f, "menuX")
    val my = transition.menuAxis(dampingRatio = 0.78f, stiffness = 330f, "menuY")

    val colors = FluidTheme.colors
    val dark = colors.isDark
    val cardColor = if (dark) Color(0xFF1C1C1E) else Color.White
    val dim = Color.Black.copy(alpha = if (dark) 0.35f else 0.18f)
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val density = LocalDensity.current
    val padPx = with(density) { CardPadding.toPx() }
    val menuFadePx = with(density) { 120.dp.toPx() }

    Popup(
        popupPositionProvider = WindowOrigin,
        properties = PopupProperties(focusable = true, usePlatformDefaultWidth = false),
        onDismissRequest = menu::close,
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .drawBehind { drawRect(dim, alpha = p.value.coerceIn(0f, 1f)) }
                    .pointerInput(Unit) { detectTapGestures { menu.close() } },
            )

            CompositionLocalProvider(
                LocalFluidTextStyle provides FluidTheme.typography.body,
                LocalFluidContentColor provides colors.label,
            ) {
                Layout(
                    modifier = Modifier.fillMaxSize(),
                    content = {
                        Box(
                            Modifier
                                .graphicsLayer {
                                    val v = p.value
                                    val inner = size.width - 2 * padPx
                                    val s0 = (anchor.width / inner).coerceIn(0.05f, 1f)
                                    val s = s0 + (1f - s0) * v
                                    transformOrigin = TransformOrigin(0f, 0f)
                                    scaleX = s
                                    scaleY = s
                                    val d = menu.drag.value
                                    translationX = (anchor.left - padPx * s0 - menu.placed[0]) * (1f - v) + d.x * v
                                    translationY = (anchor.top - padPx * s0 - menu.placed[1]) * (1f - v) + d.y * v
                                    alpha = menu.previewAlpha(v)
                                }
                                .clip(ContinuousRoundedRectangle(22.dp))
                                .drawBehind { drawRect(cardColor, alpha = p.value.coerceIn(0f, 1f)) }
                                .pointerInput(onPreviewClick) {
                                    detectTapGestures {
                                        onPreviewClick?.invoke()
                                        menu.close()
                                    }
                                }
                                .padding(CardPadding),
                        ) { Column { preview() } }

                        Box(
                            Modifier.graphicsLayer {
                                val x = mx.value
                                val y = my.value
                                val dragFade = 1f - (menu.drag.value.getDistance() / menuFadePx).coerceIn(0f, 1f)
                                alpha = (p.value * 3f).coerceIn(0f, 1f) * dragFade
                                scaleX = 0.25f + 0.75f * x
                                scaleY = 0.08f + 0.92f * y
                                transformOrigin = TransformOrigin(if (menu.rightSide) 1f else 0f, 0f)
                                val r = if (transition.targetState) 20.dp.toPx() * (1f - minOf(x, y)).coerceIn(
                                    0f,
                                    1f
                                ) else 0f
                                if (r > 0.01f && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    renderEffect = BlurEffect(r, r)
                                }
                            },
                        ) {
                            Column(
                                modifier = Modifier
                                    .width(260.dp)
                                    .clip(ContinuousRoundedRectangle(16.dp))
                                    .background(cardColor)
                                    .padding(vertical = 4.dp),
                            ) {
                                sections.forEachIndexed { sIdx, section ->
                                    if (sIdx > 0) {
                                        Box(
                                            Modifier
                                                .fillMaxWidth()
                                                .height(0.5.dp)
                                                .background(colors.separator)
                                        )
                                    }
                                    if (section.header != null) {
                                        Text(
                                            text = section.header,
                                            style = FluidTheme.typography.caption1,
                                            color = colors.secondaryLabel,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                        )
                                    }
                                    section.items.forEach { item ->
                                        val itemColor = when {
                                            item.role == FluidButtonRole.Destructive -> colors.systemRed
                                            else -> colors.label
                                        }
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable(enabled = item.enabled) {
                                                    item.onClick()
                                                    menu.close()
                                                }
                                                .padding(horizontal = 16.dp, vertical = 11.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = item.title,
                                                    style = FluidTheme.typography.body,
                                                    color = itemColor,
                                                )
                                                if (item.description != null) {
                                                    Text(
                                                        text = item.description,
                                                        style = FluidTheme.typography.footnote,
                                                        color = colors.secondaryLabel,
                                                    )
                                                }
                                            }
                                            if (item.icon != null) {
                                                Spacer(Modifier.width(12.dp))
                                                item.icon.invoke(itemColor)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                ) { measurables, constraints ->
                    val w = constraints.maxWidth
                    val h = constraints.maxHeight
                    val gap = 8.dp.roundToPx()
                    val top = statusTop.roundToPx() + 8.dp.roundToPx()
                    val bottom = h - navBottom.roundToPx() - 16.dp.roundToPx()

                    val menuPlaceable = measurables[1].measure(Constraints())
                    val textBlock = 76.dp.roundToPx()
                    val ideal = minOf((w * 0.78f).roundToInt(), 340.dp.roundToPx())
                    val fit = bottom - top - gap - menuPlaceable.height - textBlock
                    val cardW = ideal.coerceAtMost(fit).coerceAtLeast(140.dp.roundToPx())
                    val cardPlaceable = measurables[0].measure(Constraints.fixedWidth(cardW))

                    val cardLeft = ((w - cardW) / 2).coerceIn(16.dp.roundToPx(), w - cardW - 16.dp.roundToPx())
                    val cardTop = ((h - cardPlaceable.height - menuPlaceable.height - gap) / 2).coerceIn(
                        top,
                        bottom - cardPlaceable.height
                    )

                    menu.placed[0] = cardLeft.toFloat()
                    menu.placed[1] = cardTop.toFloat()
                    menu.placed[2] = anchor.left
                    menu.placed[3] = anchor.top
                    menu.rightSide = anchor.center.x > w / 2

                    layout(w, h) {
                        cardPlaceable.place(cardLeft, cardTop)
                        val menuLeft = if (menu.rightSide) {
                            cardLeft + cardW - menuPlaceable.width
                        } else {
                            cardLeft
                        }.coerceIn(16.dp.roundToPx(), w - menuPlaceable.width - 16.dp.roundToPx())
                        val menuTop = (cardTop + cardPlaceable.height + gap).coerceAtMost(bottom - menuPlaceable.height)
                        menuPlaceable.place(menuLeft, menuTop)
                    }
                }
            }
        }
    }
}
