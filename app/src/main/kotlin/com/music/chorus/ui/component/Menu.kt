package pushkar.chorus.music.ui.component

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import pushkar.chorus.music.ui.component.shapes.ContinuousRoundedRectangle
import pushkar.chorus.music.ui.fluid.foundation.BounceIndication
import pushkar.chorus.music.ui.fluid.foundation.FluidTheme
import pushkar.chorus.music.ui.fluid.foundation.GlassIntensity
import pushkar.chorus.music.ui.fluid.foundation.fluidGlass
import pushkar.chorus.music.ui.theme.isFluidTheme

@Composable
fun Material3MenuGroup(
    items: List<Material3MenuItemData>,
    modifier: Modifier = Modifier
) {
    val isFluid = isFluidTheme()
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()

    Column(
        modifier = modifier.fillMaxWidth(),
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

            val content = @Composable {
                if (item.customComposable != null) {
                    item.customComposable.invoke()
                } else {
                    Material3MenuItemRow(item = item, isFluid = isFluid)
                }
            }

            val defaultCardColors = if (isFluid) {
                CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xCC2C2C2E) else Color(0xCCF2F2F7)
                )
            } else {
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            val interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
                    .let { mod ->
                        if (isFluid) {
                            mod.clip(shape).fluidGlass(intensity = GlassIntensity.Subtle)
                        } else {
                            mod
                        }
                    }
                    .let { mod ->
                        if (item.onClick != null) {
                            if (isFluid) {
                                mod.clickable(
                                    interactionSource = interactionSource,
                                    indication = BounceIndication,
                                    onClick = item.onClick
                                )
                            } else {
                                mod.clickable(onClick = item.onClick)
                            }
                        } else mod
                    },
                shape = shape,
                colors = item.cardColors ?: defaultCardColors,
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun Material3MenuItemRow(
    item: Material3MenuItemData,
    isFluid: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = if (isFluid) 14.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item.icon?.let { icon ->
            icon()
            Spacer(modifier = Modifier.width(16.dp))
        }

        Column(
            modifier = Modifier.weight(1f)
        ) {
            val titleStyle = if (isFluid) FluidTheme.typography.headline else MaterialTheme.typography.titleMedium
            ProvideTextStyle(titleStyle) {
                item.title?.invoke()
            }

            item.description?.let { desc ->
                Spacer(modifier = Modifier.height(2.dp))
                val descStyle = if (isFluid) {
                    FluidTheme.typography.subheadline.copy(color = FluidTheme.colors.secondaryLabel)
                } else {
                    MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                ProvideTextStyle(descStyle) {
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

data class Material3MenuItemData(
    val icon: (@Composable () -> Unit)? = null,
    val title: (@Composable () -> Unit)? = null,
    val description: (@Composable () -> Unit)? = null,
    val onClick: (() -> Unit)? = null,
    val cardColors: CardColors? = null,
    val trailingContent: (@Composable () -> Unit)? = null,
    val customComposable: (@Composable () -> Unit)? = null
)
