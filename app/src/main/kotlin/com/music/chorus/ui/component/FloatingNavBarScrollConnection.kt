package pushkar.chorus.music.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

class FloatingNavBarScrollConnection(
    private val scrollThresholdPx: Float,
    private val expandThresholdPx: Float = scrollThresholdPx,
) : NestedScrollConnection {
    var isNavBarHidden by mutableStateOf(false)
        private set

    private var accumulatedScroll = 0f

    fun expand() {
        isNavBarHidden = false
        accumulatedScroll = 0f
    }

    fun hide() {
        isNavBarHidden = true
        accumulatedScroll = 0f
    }

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        val scrollDelta = available.y

        // Reset accumulator if scrolling direction reverses
        if ((accumulatedScroll > 0 && scrollDelta < 0) || (accumulatedScroll < 0 && scrollDelta > 0)) {
            accumulatedScroll = 0f
        }

        accumulatedScroll += scrollDelta

        // Scroll down (negative available.y) -> hide toolbar
        if (accumulatedScroll <= -scrollThresholdPx && !isNavBarHidden) {
            isNavBarHidden = true
            accumulatedScroll = 0f
        } else if (accumulatedScroll >= expandThresholdPx && isNavBarHidden) {
            // Scroll up (positive available.y) -> show toolbar
            isNavBarHidden = false
            accumulatedScroll = 0f
        }

        return Offset.Zero
    }
}

@Composable
fun rememberFloatingNavBarScrollConnection(
    scrollThreshold: Dp = 48.dp,
    expandThreshold: Dp = 20.dp,
): FloatingNavBarScrollConnection {
    val density = LocalDensity.current
    val scrollThresholdPx = with(density) { scrollThreshold.toPx() }
    val expandThresholdPx = with(density) { expandThreshold.toPx() }

    return remember(density) {
        FloatingNavBarScrollConnection(
            scrollThresholdPx = scrollThresholdPx,
            expandThresholdPx = expandThresholdPx,
        )
    }
}
