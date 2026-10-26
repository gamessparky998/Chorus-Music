package pushkar.chorus.music.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pushkar.chorus.music.constants.ReduceDynamicBlurKey
import pushkar.chorus.music.ui.fluid.foundation.FluidTheme
import pushkar.chorus.music.ui.fluid.foundation.GlassTintTokens
import pushkar.chorus.music.ui.theme.isFluidTheme
import pushkar.chorus.music.utils.rememberPreference

@Composable
fun FrostedTopBar(
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onBackLongClick: (() -> Unit)? = null,
    title: String = "",
    titleAlpha: Float = 0f,
    titleAlphaProvider: (() -> Float)? = null,
    titleColor: Color = Color.Unspecified,
    leading: @Composable () -> Unit = {},
    leadingInset: Dp = 16.dp,
    largeTitle: Boolean = false,
    actions: @Composable () -> Unit = {},
) {
    val reduceDynamicBlur by rememberPreference(ReduceDynamicBlurKey, false)
    val isFluid = isFluidTheme()

    val showDivider by remember(titleAlpha, titleAlphaProvider) {
        derivedStateOf { (titleAlphaProvider?.invoke() ?: titleAlpha) > 0.99f }
    }

    val dividerColor by animateColorAsState(
        targetValue = if (isFluid) {
            FluidTheme.colors.separator.copy(alpha = if (showDivider && reduceDynamicBlur) 0.6f else 0f)
        } else {
            MaterialTheme.colorScheme.outline.copy(alpha = if (showDivider && reduceDynamicBlur) 0.6f else 0f)
        },
        animationSpec = tween(220),
        label = "topBarDivider",
    )

    val resolvedTitleColor = when {
        titleColor.isSpecified -> titleColor
        isFluid -> FluidTheme.colors.label
        else -> MaterialTheme.colorScheme.onSurface
    }

    val resolvedIconColor = when {
        titleColor.isSpecified -> titleColor
        isFluid -> FluidTheme.colors.label
        else -> MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (reduceDynamicBlur && isFluid) {
                    Modifier.background(FluidTheme.colors.systemBackground)
                } else if (reduceDynamicBlur) {
                    Modifier.background(MaterialTheme.colorScheme.surface)
                } else {
                    Modifier
                }
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(if (largeTitle) 44.dp else TopBarContentHeight),
        ) {
            Text(
                text = title,
                style = if (isFluid) FluidTheme.typography.headline else MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = resolvedTitleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 96.dp)
                    .padding(top = 4.dp)
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = titleAlphaProvider?.invoke() ?: titleAlpha
                    },
            )

            if (onBack != null) {
                GlassCircleButton(
                    onClick = onBack,
                    onLongClick = onBackLongClick,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 12.dp),
                    size = 36.dp,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = resolvedIconColor,
                        modifier = Modifier.size(20.dp),
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .align(if (largeTitle) Alignment.TopStart else Alignment.CenterStart)
                        .padding(start = leadingInset),
                ) {
                    leading()
                }
            }

            Row(
                modifier = Modifier
                    .align(if (largeTitle) Alignment.TopEnd else Alignment.CenterEnd)
                    .padding(end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                actions()
            }
        }

        Box(Modifier.fillMaxWidth()) {
            HorizontalDivider(thickness = 0.5.dp, color = dividerColor)
        }
    }
}
