package pushkar.chorus.music.ui.component.backdrop.backdrops

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import pushkar.chorus.music.ui.fluid.foundation.lastNavWasPop
import kotlinx.coroutines.flow.collectLatest

private const val NavTransitionFreezeWindowNs = 330_000_000L

class NavTransitionFreeze {
    private val startedAtNs = longArrayOf(0L)

    val frozen: () -> Boolean = {
        val started = startedAtNs[0]
        started != 0L && System.nanoTime() - started < NavTransitionFreezeWindowNs
    }

    fun markTransitionStarted() {
        startedAtNs[0] = System.nanoTime()
    }
}

@Composable
fun rememberNavTransitionFreeze(currentRoute: String?): NavTransitionFreeze {
    val freeze = remember { NavTransitionFreeze() }
    LaunchedEffect(currentRoute) {
        if (!lastNavWasPop()) {
            freeze.markTransitionStarted()
        }
    }
    return freeze
}

@Composable
fun rememberPagerTransitionFreeze(pagerState: PagerState): NavTransitionFreeze {
    val freeze = remember { NavTransitionFreeze() }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.isScrollInProgress }
            .collectLatest { scrolling ->
                while (scrolling) {
                    freeze.markTransitionStarted()
                    withFrameNanos { }
                }
            }
    }
    return freeze
}
