package pushkar.chorus.music.ui.fluid.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import java.util.Locale

fun formatPlaybackDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
    }
}

@Composable
fun RollingPlaybackTime(
    timeMs: Long,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
) {
    val formatted = formatPlaybackDuration(timeMs)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (char in formatted) {
            if (char.isDigit()) {
                AnimatedContent(
                    targetState = char,
                    transitionSpec = {
                        val direction = if (targetState > initialState) 1 else -1
                        (slideInVertically { height -> direction * height } + fadeIn())
                            .togetherWith(slideOutVertically { height -> -direction * height } + fadeOut())
                            .using(SizeTransform(clip = false))
                    },
                    label = "RollingDigit",
                ) { digit ->
                    Text(
                        text = digit.toString(),
                        style = style,
                        color = color,
                        maxLines = 1,
                    )
                }
            } else {
                Text(
                    text = char.toString(),
                    style = style,
                    color = color,
                    maxLines = 1,
                )
            }
        }
    }
}
