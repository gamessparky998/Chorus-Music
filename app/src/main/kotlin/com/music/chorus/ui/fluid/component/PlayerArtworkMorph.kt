package pushkar.chorus.music.ui.fluid.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import pushkar.chorus.music.ui.fluid.foundation.FluidShapes
import pushkar.chorus.music.ui.fluid.foundation.rimLight
import kotlin.math.exp

object ArtworkMotion {
    const val DurationMillis = 350
    private const val Endpoint = 9f
    private val normalization = 1f / (1f - (1f + Endpoint) * exp(-Endpoint))

    val Easing = Easing { fraction ->
        if (!fraction.isFinite()) return@Easing 1f
        if (fraction <= 0f) return@Easing 0f
        if (fraction >= 1f) return@Easing 1f
        val position = Endpoint * fraction
        (1f - (1f + position) * exp(-position)) * normalization
    }
}

@Composable
fun PlayerArtworkMorph(
    artworkUrl: String?,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp,
    elevation: Dp = 16.dp,
    contentDescription: String? = null,
) {
    val shape = FluidShapes.continuous(cornerRadius)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .shadow(elevation, shape = shape, clip = false)
            .clip(shape)
            .rimLight(shape = shape),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = artworkUrl,
            transitionSpec = {
                (fadeIn(animationSpec = tween(ArtworkMotion.DurationMillis, easing = ArtworkMotion.Easing)) +
                        scaleIn(initialScale = 0.94f, animationSpec = tween(ArtworkMotion.DurationMillis, easing = ArtworkMotion.Easing)))
                    .togetherWith(
                        fadeOut(animationSpec = tween(ArtworkMotion.DurationMillis, easing = ArtworkMotion.Easing)) +
                                scaleOut(targetScale = 1.04f, animationSpec = tween(ArtworkMotion.DurationMillis, easing = ArtworkMotion.Easing))
                    )
            },
            label = "PlayerArtworkTransition",
            modifier = Modifier.fillMaxSize(),
        ) { url ->
            AsyncImage(
                model = url,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
