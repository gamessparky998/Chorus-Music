@file:Suppress("OPT_IN_USAGE_ERROR", "OPT_IN_USAGE")

package pushkar.chorus.music.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetDefaults
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

import pushkar.chorus.music.ui.component.shapes.ContinuousRoundedRectangle
import pushkar.chorus.music.ui.fluid.foundation.GlassIntensity
import pushkar.chorus.music.ui.fluid.foundation.fluidGlass
import pushkar.chorus.music.ui.theme.isFluidTheme

import pushkar.chorus.music.ui.component.backdrop.Backdrop
import pushkar.chorus.music.ui.fluid.component.FluidCenteredPopup
import pushkar.chorus.music.ui.fluid.foundation.LocalAppBackdrop
import androidx.compose.runtime.CompositionLocalProvider

val LocalMenuState = compositionLocalOf { MenuState() }

@Stable
class MenuState(
    isVisible: Boolean = false,
    content: @Composable ColumnScope.() -> Unit = {},
    backdrop: Backdrop? = null,
) {
    var isVisible by mutableStateOf(isVisible)
    var content by mutableStateOf(content)
    var backdrop by mutableStateOf(backdrop)

    fun show(content: @Composable ColumnScope.() -> Unit) {
        show(null, content)
    }

    fun show(backdrop: Backdrop? = null, content: @Composable ColumnScope.() -> Unit) {
        this.backdrop = backdrop
        this.content = content
        this.isVisible = true
    }

    fun dismiss() {
        isVisible = false
        backdrop = null
    }
}

@Composable
fun AnimatedBottomSheet(
    isVisible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
    sheetMaxWidth: Dp = BottomSheetDefaults.SheetMaxWidth,
    shape: Shape = BottomSheetDefaults.ExpandedShape,
    containerColor: Color = BottomSheetDefaults.ContainerColor,
    contentColor: Color = contentColorFor(containerColor),
    tonalElevation: Dp = 0.dp,
    scrimColor: Color = BottomSheetDefaults.ScrimColor,
    dragHandle: @Composable (() -> Unit)? = { BottomSheetDefaults.DragHandle() },
    contentWindowInsets: @Composable () -> WindowInsets = { BottomSheetDefaults.modalWindowInsets },
    properties: ModalBottomSheetProperties = ModalBottomSheetDefaults.properties,
    content: @Composable ColumnScope.() -> Unit,
) {
    var lastContent by remember { mutableStateOf(content) }

    LaunchedEffect(content) {
        if (isVisible) {
            lastContent = content
        }
    }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            sheetState.show()
        } else {
            sheetState.hide()
        }
    }

    if (!sheetState.isVisible && !isVisible) {
        return
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        sheetMaxWidth = sheetMaxWidth,
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        tonalElevation = tonalElevation,
        scrimColor = scrimColor,
        dragHandle = dragHandle,
        contentWindowInsets = contentWindowInsets,
        properties = properties,
        content = lastContent,
    )
}

@Composable
fun BottomSheetMenu(
    modifier: Modifier = Modifier,
    state: MenuState,
    background: Color = BottomSheetDefaults.ContainerColor,
) {
    val focusManager = LocalFocusManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val isFluid = isFluidTheme()

    if (isFluid) {
        val targetBackdrop = state.backdrop ?: LocalAppBackdrop.current
        CompositionLocalProvider(LocalAppBackdrop provides targetBackdrop) {
            FluidCenteredPopup(
                isVisible = state.isVisible,
                onDismissRequest = {
                    focusManager.clearFocus()
                    state.dismiss()
                },
                modifier = modifier,
                maxHeightRatio = 0.85f,
                maxWidth = 420.dp
            ) {
                state.content(this)
            }
        }
        return
    }

    val sheetShape = BottomSheetDefaults.ExpandedShape
    val sheetColor = background

    AnimatedBottomSheet(
        isVisible = state.isVisible,
        onDismissRequest = {
            focusManager.clearFocus()
            state.isVisible = false
        },
        sheetState = sheetState,
        shape = sheetShape,
        containerColor = sheetColor,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = {
            val handleColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(handleColor)
            )
        },
        modifier = modifier
            .fillMaxHeight()
            .clip(sheetShape)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            state.content(this)
        }
    }
}
