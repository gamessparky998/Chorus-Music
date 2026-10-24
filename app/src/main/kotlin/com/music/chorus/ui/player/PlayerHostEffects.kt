package pushkar.chorus.music.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import pushkar.chorus.music.ui.component.BottomSheetState

@Composable
fun PlayerMorphHostEffects(state: BottomSheetState) {
    DisposableEffect(state) {
        PlayerMorph.progressProvider = { state.progress }
        onDispose {
            PlayerMorph.progressProvider = { 0f }
            PlayerMorph.landingActive = false
        }
    }
}
