package pushkar.chorus.music.ui.component

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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

    if (isFluid) {
        val groupShape = ContinuousRoundedRectangle(18.dp)
        androidx.compose.foundation.layout.Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(groupShape)
                .background(
                    if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.035f),
                    shape = groupShape
                )
                .border(
                    width = 0.5.dp,
                    color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f),
                    shape = groupShape
                )
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                items.forEachIndexed { index, item ->
                    val interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .let { mod ->
                                if (item.onClick != null) {
                                    mod.clickable(
                                        interactionSource = interactionSource,
                                        indication = BounceIndication,
                                        onClick = item.onClick
                                    )
                                } else mod
                            }
                    ) {
                        if (item.customComposable != null) {
                            item.customComposable.invoke()
                        } else {
                            Material3MenuItemRow(item = item, isFluid = true)
                        }
                    }

                    if (index < items.size - 1) {
                        androidx.compose.material3.HorizontalDivider(
                            color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.07f),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
        return
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items.forEachIndexed { index, item ->
            val shape = when {
                items.size == 1 -> RoundedCornerShape(24.dp)
                index == 0 -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 6.dp, bottomEnd = 6.dp)
                index == items.size - 1 -> RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
                else -> RoundedCornerShape(6.dp)
            }

            val defaultCardColors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
                    .let { mod ->
                        if (item.onClick != null) {
                            mod.clickable(onClick = item.onClick)
                        } else mod
                    },
                shape = shape,
                colors = item.cardColors ?: defaultCardColors,
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                if (item.customComposable != null) {
                    item.customComposable.invoke()
                } else {
                    Material3MenuItemRow(item = item, isFluid = false)
                }
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
