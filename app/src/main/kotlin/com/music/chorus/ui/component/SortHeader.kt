@file:Suppress("OPT_IN_USAGE_ERROR", "OPT_IN_USAGE")

package pushkar.chorus.music.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import pushkar.chorus.music.ui.component.shapes.ContinuousRoundedRectangle
import pushkar.chorus.music.ui.fluid.foundation.GlassIntensity
import pushkar.chorus.music.ui.fluid.foundation.fluidGlass
import pushkar.chorus.music.ui.theme.isFluidTheme
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pushkar.chorus.music.R
import pushkar.chorus.music.constants.PlaylistSongSortType

@Composable
inline fun <reified T : Enum<T>> SortHeader(
    sortType: T,
    sortDescending: Boolean,
    crossinline onSortTypeChange: (T) -> Unit,
    crossinline onSortDescendingChange: (Boolean) -> Unit,
    crossinline sortTypeText: (T) -> Int,
    modifier: Modifier = Modifier,
    showDescending: Boolean? = true,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val displayDescending = showDescending == true && sortType != PlaylistSongSortType.CUSTOM
    val isFluid = isFluidTheme()
    val isDark = isSystemInDarkTheme()
    val leadingShape = ContinuousRoundedRectangle(topStart = 16.dp, bottomStart = 16.dp, topEnd = 6.dp, bottomEnd = 6.dp)
    val trailingShape = ContinuousRoundedRectangle(topStart = 6.dp, bottomStart = 6.dp, topEnd = 16.dp, bottomEnd = 16.dp)

    SplitButtonLayout(
        leadingButton = {
            SplitButtonDefaults.LeadingButton(
                onClick = { menuExpanded = !menuExpanded },
                colors = if (isFluid) {
                    ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = if (isDark) Color(0xFFEEEEEE) else Color(0xFF141416)
                    )
                } else {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier
                    .widthIn(min = 120.dp)
                    .let { mod ->
                        if (isFluid) {
                            mod.clip(leadingShape).fluidGlass(
                                shape = leadingShape,
                                intensity = GlassIntensity.Standard,
                                surfaceColor = MaterialTheme.colorScheme.primary,
                                surfaceAlpha = if (isDark) 0.16f else 0.10f
                            )
                        } else mod
                    }
            ) {
                Text(
                    text = stringResource(sortTypeText(sortType)),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        },
        trailingButton = {
            if (displayDescending) {
                val description = "Toggle sort order"
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                    tooltip = { PlainTooltip { Text(description) } },
                    state = rememberTooltipState(),
                ) {
                    SplitButtonDefaults.TrailingButton(
                        checked = sortDescending,
                        onCheckedChange = { onSortDescendingChange(it) },
                        colors = if (isFluid) {
                            ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = if (isDark) Color(0xFFEEEEEE) else Color(0xFF141416)
                            )
                        } else {
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier
                            .semantics {
                                stateDescription = if (sortDescending) "Descending" else "Ascending"
                                contentDescription = description
                            }
                            .let { mod ->
                                if (isFluid) {
                                    mod.clip(trailingShape).fluidGlass(
                                        shape = trailingShape,
                                        intensity = GlassIntensity.Standard,
                                        surfaceColor = MaterialTheme.colorScheme.primary,
                                        surfaceAlpha = if (isDark) 0.16f else 0.10f
                                    )
                                } else mod
                            },
                    ) {
                        val rotation: Float by animateFloatAsState(
                            targetValue = if (sortDescending) 180f else 0f,
                            label = "Trailing Icon Rotation",
                        )
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            modifier = Modifier
                                .size(SplitButtonDefaults.TrailingIconSize)
                                .graphicsLayer {
                                    this.rotationZ = rotation
                                },
                            contentDescription = null,
                        )
                    }
                }
            } else {
                SplitButtonDefaults.TrailingButton(
                    checked = menuExpanded,
                    onCheckedChange = { menuExpanded = it },
                    colors = if (isFluid) {
                        ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = if (isDark) Color(0xFFEEEEEE) else Color(0xFF141416)
                        )
                    } else {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier
                        .semantics {
                            stateDescription = if (menuExpanded) "Expanded" else "Collapsed"
                            contentDescription = "Show sort options"
                        }
                        .let { mod ->
                            if (isFluid) {
                                mod.clip(trailingShape).fluidGlass(
                                    shape = trailingShape,
                                    intensity = GlassIntensity.Standard,
                                    surfaceColor = MaterialTheme.colorScheme.primary,
                                    surfaceAlpha = if (isDark) 0.16f else 0.10f
                                )
                            } else mod
                        },
                ) {
                    val rotation: Float by animateFloatAsState(
                        targetValue = if (menuExpanded) 180f else 0f,
                        label = "Trailing Icon Rotation",
                    )
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        modifier = Modifier
                            .size(SplitButtonDefaults.TrailingIconSize)
                            .graphicsLayer {
                                this.rotationZ = rotation
                            },
                        contentDescription = null,
                    )
                }
            }
        },
        modifier = modifier.padding(vertical = 8.dp)
    )

    DropdownMenu(
        expanded = menuExpanded,
        onDismissRequest = { menuExpanded = false },
        shape = if (isFluid) ContinuousRoundedRectangle(16.dp) else RoundedCornerShape(16.dp),
        containerColor = if (isFluid) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .widthIn(min = 172.dp)
            .then(if (isFluid) Modifier.fluidGlass(intensity = GlassIntensity.Prominent) else Modifier),
    ) {
        enumValues<T>().forEach { type ->
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(sortTypeText(type)),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                    )
                },
                trailingIcon = {
                    Icon(
                        painter = painterResource(
                            if (sortType == type) R.drawable.radio_button_checked
                            else R.drawable.radio_button_unchecked
                        ),
                        contentDescription = null,
                    )
                },
                onClick = {
                    onSortTypeChange(type)
                    menuExpanded = false
                },
            )
        }
    }
}
