package pushkar.chorus.music.ui.player

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

@Composable
fun BoxScope.PlayerMorphHost(
    progress: () -> Float,
    thumbnailUrl: String?,
    mediaId: String?,
    containerColor: Color,
    expandedCornerRadius: Dp,
) {
    PlayerContainerMorphOverlay(
        progress = progress,
        color = containerColor,
        expandedCornerRadius = expandedCornerRadius,
    )
    PlayerArtworkMorphOverlay(
        thumbnailUrl = thumbnailUrl,
        mediaId = mediaId,
        progress = progress,
    )
}
