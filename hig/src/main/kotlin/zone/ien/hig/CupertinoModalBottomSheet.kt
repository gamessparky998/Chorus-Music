package zone.ien.hig

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun CupertinoModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: CupertinoSheetState = rememberCupertinoSheetState(),
    sheetMaxWidth: Dp = 640.dp,
    shape: Shape = CupertinoBottomSheetDefaults.shape,
    containerColor: Color = CupertinoBottomSheetDefaults.containerColor,
    contentColor: Color = CupertinoBottomSheetDefaults.contentColor,
    scrimColor: Color = Color.Black.copy(alpha = 0.32f),
    dragHandle: @Composable (() -> Unit)? = { CupertinoBottomSheetDefaults.DragHandle() },
    content: @Composable ColumnScope.() -> Unit,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val modalStyle = sheetState.presentationStyle as? PresentationStyle.Modal
    val detents = remember(modalStyle) { modalStyle?.detents?.toList() ?: listOf(PresentationDetent.Large) }
    val dismissOnClickOutside = modalStyle?.dismissOnClickOutside ?: true

    val topInsetPx = with(density) { WindowInsets.statusBars.getTop(density).toFloat() + 24.dp.toPx() }

    val possibleValues = remember(detents) {
        buildSet {
            add(CupertinoSheetValue.Hidden)
            detents.forEach { d ->
                add(if (d is PresentationDetent.Large) CupertinoSheetValue.Expanded else CupertinoSheetValue.PartiallyExpanded(d))
            }
        }
    }

    val swipeableState = sheetState.swipeableState
    var hasExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(sheetState) {
        if (swipeableState.currentValue == CupertinoSheetValue.Hidden) {
            sheetState.show()
        }
        hasExpanded = true
    }

    var settledHidden by remember { mutableStateOf(false) }
    LaunchedEffect(swipeableState.currentValue, hasExpanded) {
        if (hasExpanded && swipeableState.currentValue == CupertinoSheetValue.Hidden) {
            settledHidden = true
            onDismissRequest()
        }
    }
    if (settledHidden) return

    var containerHeightPx by remember { mutableFloatStateOf(0f) }

    Box(modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val offset = swipeableState.offset
                    alpha = if (offset == null || containerHeightPx <= 0f) {
                        0f
                    } else {
                        (1f - (offset / containerHeightPx).coerceIn(0f, 1f))
                    }
                }
                .background(scrimColor)
                .pointerInput(dismissOnClickOutside) {
                    detectTapGestures {
                        if (dismissOnClickOutside) scope.launch { sheetState.hide() }
                    }
                }
                .swipeAnchors(
                    state = swipeableState,
                    possibleValues = possibleValues,
                ) { value, layoutSize ->
                    val h = layoutSize.height.toFloat()
                    containerHeightPx = h
                    val detentHeight = when (value) {
                        CupertinoSheetValue.Hidden -> return@swipeAnchors h
                        CupertinoSheetValue.Expanded ->
                            (detents.firstOrNull { it is PresentationDetent.Large } ?: PresentationDetent.Large)
                                .calculate(density, h)
                        is CupertinoSheetValue.PartiallyExpanded -> value.detent.calculate(density, h)
                    }
                    h - detentHeight.coerceIn(0f, h - topInsetPx)
                },
        )

        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .widthIn(max = sheetMaxWidth)
                .fillMaxWidth()
                .layout { measurable, constraints ->
                    val offset = swipeableState.offset
                    val h = (containerHeightPx - (offset ?: containerHeightPx)).coerceAtLeast(0f).roundToInt()
                    val placeable = measurable.measure(constraints.copy(minHeight = h, maxHeight = h))
                    layout(placeable.width, h) { placeable.place(0, 0) }
                }
                .swipeableV2(swipeableState, Orientation.Vertical)
                .clip(shape)
                .background(containerColor),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                dragHandle?.invoke()
                CompositionLocalProvider(LocalContentColor provides contentColor) {
                    content()
                }
            }
        }
    }
}
