package pushkar.chorus.music.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import pushkar.chorus.music.ui.component.backdrop.backdrops.BackdropFreeze
import pushkar.chorus.music.ui.component.backdrop.backdrops.LayerBackdrop
import pushkar.chorus.music.ui.component.backdrop.backdrops.layerBackdrop
import pushkar.chorus.music.ui.component.backdrop.backdrops.rememberBackdropFreeze
import pushkar.chorus.music.ui.component.backdrop.backdrops.rememberLayerBackdrop

@Stable
class GlassScreenChrome internal constructor(
    val backdrop: LayerBackdrop,
    val hazeState: HazeState,
    private val freeze: BackdropFreeze,
) {
    val frozen: () -> Boolean get() = freeze.frozen

    fun screenModifier(): Modifier = Modifier.haze(hazeState)

    fun contentModifier(): Modifier = Modifier
        .nestedScroll(freeze.connection)
        .graphicsLayer()
        .layerBackdrop(backdrop, frozen = freeze.frozen)
        .graphicsLayer()
}

@Composable
fun rememberGlassScreenChrome(background: Color): GlassScreenChrome {
    val freeze = rememberBackdropFreeze()
    val backdrop = rememberLayerBackdrop(
        onDraw = remember(background) {
            val bg = background
            { drawRect(bg); drawContent() }
        }
    )
    val hazeState = remember { HazeState() }
    return remember(backdrop, hazeState, freeze) { GlassScreenChrome(backdrop, hazeState, freeze) }
}
