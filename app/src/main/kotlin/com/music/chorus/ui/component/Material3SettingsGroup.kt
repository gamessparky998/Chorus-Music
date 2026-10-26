package pushkar.chorus.music.ui.component

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

import pushkar.chorus.music.ui.utils.scrollToOnHighlight
import androidx.compose.foundation.ScrollState

import pushkar.chorus.music.ui.component.shapes.ContinuousRoundedRectangle
import pushkar.chorus.music.ui.fluid.foundation.BounceIndication
import pushkar.chorus.music.ui.fluid.foundation.FluidTheme
import pushkar.chorus.music.ui.fluid.foundation.GlassIntensity
import pushkar.chorus.music.ui.fluid.foundation.fluidGlass
import pushkar.chorus.music.ui.theme.isFluidTheme

@Composable
fun Material3SettingsGroup(
    title: String? = null,
    compact: Boolean = false,
    scrollState: ScrollState? = null,
    items: List<Material3SettingsItem>
) {
    val isFluid = isFluidTheme()
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()

    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {

        title?.let {
            val titleStyle = if (isFluid) FluidTheme.typography.subheadline.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold) else MaterialTheme.typography.labelLarge
            val titleColor = if (isFluid) FluidTheme.colors.secondaryLabel else MaterialTheme.colorScheme.primary
            Text(
                text = it,
                style = titleStyle,
                color = titleColor,
                modifier = Modifier.padding(bottom = if (compact) 4.dp else 8.dp, top = if (compact) 4.dp else 8.dp)
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(if (isFluid) 6.dp else 4.dp)
        ) {
            items.forEachIndexed { index, item ->
                val shape = when {
                    items.size == 1 -> if (isFluid) ContinuousRoundedRectangle(20.dp) else RoundedCornerShape(24.dp)
                    index == 0 -> if (isFluid) {
                        ContinuousRoundedRectangle(topStart = 20.dp, topEnd = 20.dp, bottomStart = 6.dp, bottomEnd = 6.dp)
                    } else {
                        RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 6.dp, bottomEnd = 6.dp)
                    }

                    index == items.size - 1 -> if (isFluid) {
                        ContinuousRoundedRectangle(topStart = 6.dp, topEnd = 6.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
                    } else {
                        RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
                    }

                    else -> if (isFluid) ContinuousRoundedRectangle(6.dp) else RoundedCornerShape(6.dp)
                }

                val cardContainerColor = when {
                    isFluid -> Color.Transparent
                    item.isHighlighted -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                        .let { mod ->
                            if (isFluid) {
                                mod
                                    .clip(shape)
                                    .fluidGlass(
                                        shape = shape,
                                        intensity = GlassIntensity.Standard,
                                        surfaceColor = if (item.isHighlighted) MaterialTheme.colorScheme.primary else Color.Unspecified,
                                        surfaceAlpha = if (item.isHighlighted) 0.25f else if (isDark) 0.18f else 0.10f
                                    )
                            } else {
                                mod
                            }
                        },
                    shape = shape,
                    colors = CardDefaults.cardColors(containerColor = cardContainerColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Material3SettingsItemRow(item = item, compact = compact, scrollState = scrollState, isFluid = isFluid)
                }
            }
        }
    }
}

@Composable
private fun Material3SettingsItemRow(
    item: Material3SettingsItem,
    compact: Boolean = false,
    scrollState: ScrollState? = null,
    isFluid: Boolean = false
) {
    val interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { mod ->
                if (item.enabled && item.onClick != null) {
                    if (isFluid) {
                        mod.clickable(
                            interactionSource = interactionSource,
                            indication = BounceIndication,
                            onClick = { item.onClick.invoke() }
                        )
                    } else {
                        mod.clickable(
                            enabled = true,
                            onClick = { item.onClick.invoke() }
                        )
                    }
                } else mod
            }
            .then(if (scrollState != null) Modifier.scrollToOnHighlight(scrollState, item.isHighlighted) else Modifier)
            .padding(
                horizontal = if (compact) 14.dp else 20.dp,
                vertical = if (compact) 10.dp else 16.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        val resolvedIconShape = item.iconShape ?: if (isFluid) ContinuousRoundedRectangle(12.dp) else RoundedCornerShape(12.dp)

        if (item.customIcon != null) {
            Box(
                modifier = Modifier
                    .size(if (compact) 34.dp else 40.dp)
                    .clip(resolvedIconShape)
                    .let { mod ->
                        if (isFluid && item.tintIcon) {
                            mod.fluidGlass(
                                shape = resolvedIconShape,
                                intensity = GlassIntensity.Subtle,
                                surfaceColor = MaterialTheme.colorScheme.primary,
                                surfaceAlpha = if (item.isHighlighted) 0.28f else 0.16f
                            )
                        } else {
                            mod.background(
                                if (item.tintIcon) {
                                    MaterialTheme.colorScheme.primary.copy(
                                        alpha = if (item.isHighlighted) 0.15f else 0.1f
                                    )
                                } else {
                                    androidx.compose.ui.graphics.Color.Transparent
                                }
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                item.customIcon.invoke()
            }
            Spacer(modifier = Modifier.width(if (compact) 14.dp else 20.dp))
        } else item.icon?.let { icon ->
            Box(
                modifier = Modifier
                    .size(if (compact) 34.dp else 40.dp)
                    .clip(resolvedIconShape)
                    .let { mod ->
                        if (isFluid && item.tintIcon) {
                            mod.fluidGlass(
                                shape = resolvedIconShape,
                                intensity = GlassIntensity.Subtle,
                                surfaceColor = MaterialTheme.colorScheme.primary,
                                surfaceAlpha = if (item.isHighlighted) 0.28f else 0.16f
                            )
                        } else {
                            mod.background(
                                if (item.tintIcon) {
                                    MaterialTheme.colorScheme.primary.copy(
                                        alpha = if (item.isHighlighted) 0.15f else 0.1f
                                    )
                                } else {
                                    androidx.compose.ui.graphics.Color.Transparent
                                }
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (item.showBadge) {
                    BadgedBox(
                        badge = {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        }
                    ) {
                        if (item.tintIcon) {
                            Icon(
                                painter = icon,
                                contentDescription = null,
                                tint = if (!item.enabled)
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                else if (item.isHighlighted)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                                modifier = Modifier.size(if (compact) 20.dp else 24.dp)
                            )
                        } else {
                            Image(
                                painter = icon,
                                contentDescription = null,
                                modifier = Modifier.size(if (compact) 34.dp else 40.dp),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                } else {
                    if (item.tintIcon) {
                        Icon(
                            painter = icon,
                            contentDescription = null,
                            tint = if (!item.enabled)
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            else if (item.isHighlighted)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                            modifier = Modifier.size(if (compact) 20.dp else 24.dp)
                        )
                    } else {
                        Image(
                            painter = icon,
                            contentDescription = null,
                            modifier = Modifier.size(if (compact) 34.dp else 40.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(if (compact) 12.dp else 16.dp))
        }

        Column(
            modifier = Modifier.weight(1f)
        ) {

            ProvideTextStyle(
                MaterialTheme.typography.titleMedium.copy(
                    color = if (!item.enabled)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    else
                        MaterialTheme.colorScheme.onSurface
                )
            ) {
                item.title()
            }

            item.description?.let { desc ->
                Spacer(modifier = Modifier.height(2.dp))
                ProvideTextStyle(
                    MaterialTheme.typography.bodyMedium.copy(
                        color = if (!item.enabled)
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    desc()
                }
            }
        }

        item.trailingContent?.let { trailing ->
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
        }
    }
}

data class Material3SettingsItem(
    val icon: Painter? = null,
    val customIcon: (@Composable () -> Unit)? = null,
    val title: @Composable () -> Unit,
    val description: (@Composable () -> Unit)? = null,
    val trailingContent: (@Composable () -> Unit)? = null,
    val showBadge: Boolean = false,
    val isHighlighted: Boolean = false,
    val tintIcon: Boolean = true,
    val iconShape: Shape? = null,
    val enabled: Boolean = true,
    val onClick: (() -> Unit)? = null
)
