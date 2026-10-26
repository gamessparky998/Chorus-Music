package pushkar.chorus.music.ui.component.shimmer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import pushkar.chorus.music.constants.ListItemHeight
import pushkar.chorus.music.constants.ListThumbnailSize
import pushkar.chorus.music.constants.ThumbnailCornerRadius
import pushkar.chorus.music.ui.component.shapes.ContinuousRoundedRectangle
import pushkar.chorus.music.ui.theme.isFluidTheme

@Composable
fun ListItemPlaceHolder(
    modifier: Modifier = Modifier,
    thumbnailShape: Shape = RoundedCornerShape(ThumbnailCornerRadius),
) {
    val effectiveShape = if (isFluidTheme() && thumbnailShape is RoundedCornerShape) {
        ContinuousRoundedRectangle(ThumbnailCornerRadius)
    } else {
        thumbnailShape
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
        modifier
            .padding(vertical = 2.dp)
            .height(ListItemHeight)
            .padding(horizontal = 16.dp),
    ) {
        Spacer(
            modifier =
            Modifier
                .padding(6.dp)
                .size(ListThumbnailSize)
                .clip(effectiveShape)
                .background(MaterialTheme.colorScheme.onSurface),
        )

        Column(
            modifier =
            Modifier
                .weight(1f)
                .padding(horizontal = 6.dp),
        ) {
            TextPlaceholder()
            TextPlaceholder()
        }
    }
}
