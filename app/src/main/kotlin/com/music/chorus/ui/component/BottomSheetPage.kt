@file:Suppress("OPT_IN_USAGE_ERROR", "OPT_IN_USAGE")

package pushkar.chorus.music.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp

import pushkar.chorus.music.ui.component.shapes.ContinuousRoundedRectangle
import pushkar.chorus.music.ui.fluid.foundation.GlassIntensity
import pushkar.chorus.music.ui.fluid.foundation.fluidGlass
import pushkar.chorus.music.ui.theme.isFluidTheme

import pushkar.chorus.music.ui.component.backdrop.Backdrop
import pushkar.chorus.music.ui.fluid.component.FluidCenteredPopup
import pushkar.chorus.music.ui.fluid.foundation.LocalAppBackdrop
import androidx.compose.runtime.CompositionLocalProvider

val LocalBottomSheetPageState = compositionLocalOf { BottomSheetPageState() }

@Stable
class BottomSheetPageState(
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
fun BottomSheetPage(
    modifier: Modifier = Modifier,
    state: BottomSheetPageState,
    background: Color = MaterialTheme.colorScheme.surfaceColorAtElevation(NavigationBarDefaults.Elevation),
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
                maxHeightRatio = 0.90f,
                maxWidth = 480.dp
            ) {
                state.content(this)
            }
        }
        return
    }

    val sheetShape = androidx.compose.material3.BottomSheetDefaults.ExpandedShape
    val containerColor = background

    AnimatedBottomSheet(
        isVisible = state.isVisible,
        onDismissRequest = {
            focusManager.clearFocus()
            state.isVisible = false
        },
        sheetState = sheetState,
        shape = sheetShape,
        containerColor = containerColor,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = {
            val handleColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 32.dp, height = 4.dp)
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
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        ) {
            state.content(this)
        }
    }
}
