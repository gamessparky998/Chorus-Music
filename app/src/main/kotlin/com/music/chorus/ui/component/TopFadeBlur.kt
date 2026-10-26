package pushkar.chorus.music.ui.component

import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild
import pushkar.chorus.music.constants.ReduceDynamicBlurKey
import pushkar.chorus.music.utils.rememberPreference

private val FADE_RUN = 64.dp
private const val SCRIM_PEAK = 0f
private const val SCRIM_STOPS = 12

val TopBarContentHeight = 52.dp
val TopBarContentGap = 12.dp

@Composable
fun topBarHeight(): Dp =
    WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + TopBarContentHeight

@Composable
fun topBarContentPadding(): Dp = topBarHeight() + TopBarContentGap

@OptIn(ExperimentalHazeApi::class)
@Composable
fun TopFadeBlur(
    hazeState: HazeState,
    pageColor: Color,
    modifier: Modifier = Modifier,
    scrimColor: Color = pageColor,
    heightOverride: Dp? = null,
) {
    val reduceDynamicBlur by rememberPreference(ReduceDynamicBlurKey, false)
    if (reduceDynamicBlur) return

    val barHeight = if (heightOverride != null) 0.dp else topBarHeight()
    val height = heightOverride ?: (barHeight + FADE_RUN)
    val solid = (barHeight / height).coerceIn(0f, 0.9f)

    val progressiveBlur = remember(height, solid) {
        HazeProgressive.verticalGradient(
            startIntensity = 1f,
            endIntensity = 0f,
            easing = EaseOutCubic,
            preferPerformance = true,
        )
    }

    val scrim = remember(scrimColor) {
        Brush.verticalGradient(
            colorStops = Array(SCRIM_STOPS) { i ->
                val t = i / (SCRIM_STOPS - 1f)
                t to scrimColor.copy(alpha = SCRIM_PEAK * (1f - EaseOutCubic.transform(t)))
            },
        )
    }

    Box(modifier.fillMaxWidth().height(height)) {
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer()
                .hazeChild(state = hazeState) {
                    progressive = progressiveBlur
                    noiseFactor = 0f
                },
        )
        Box(Modifier.matchParentSize().background(scrim))
    }
}
