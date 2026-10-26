@file:Suppress("OPT_IN_USAGE_ERROR", "OPT_IN_USAGE")

package pushkar.chorus.music.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.ui.draw.clip
import pushkar.chorus.music.ui.component.shapes.ContinuousRoundedRectangle
import pushkar.chorus.music.ui.fluid.foundation.BounceIndication
import pushkar.chorus.music.ui.fluid.foundation.GlassIntensity
import pushkar.chorus.music.ui.fluid.foundation.fluidGlass
import pushkar.chorus.music.ui.theme.isFluidTheme

@Composable
fun NewActionButton(
    icon: @Composable () -> Unit,
    text: String,
    onClick: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    val isFluid = isFluidTheme()
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()

    val targetBg = if (isFluid && backgroundColor == MaterialTheme.colorScheme.surfaceVariant) {
        if (isDark) Color(0xCC2C2C2E) else Color(0xCCF2F2F7)
    } else {
        backgroundColor
    }

    val animatedBackground by animateColorAsState(
        targetValue = if (enabled) targetBg else targetBg.copy(alpha = 0.5f),
        animationSpec = tween(200),
        label = "background"
    )

    val animatedContent by animateColorAsState(
        targetValue = if (enabled) contentColor else contentColor.copy(alpha = 0.5f),
        animationSpec = tween(200),
        label = "content"
    )

    var performAction by remember { mutableStateOf(false) }

    if (performAction) {
        onClick()
        LaunchedEffect(Unit) {
            performAction = false
        }
    }

    val shape = if (isFluid) ContinuousRoundedRectangle(16.dp) else RoundedCornerShape(16.dp)
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }

    val resolvedContentColor = if (isFluid && !isDark && contentColor == MaterialTheme.colorScheme.onSurfaceVariant) {
        Color(0xFF141416)
    } else {
        animatedContent
    }

    Card(
        modifier = modifier
            .let { mod ->
                if (isFluid) {
                    val tint = if (backgroundColor != MaterialTheme.colorScheme.surfaceVariant && backgroundColor != Color.Unspecified) backgroundColor else Color.Unspecified
                    mod
                        .clip(shape)
                        .fluidGlass(
                            shape = shape,
                            intensity = GlassIntensity.Standard,
                            surfaceColor = tint,
                            surfaceAlpha = if (tint != Color.Unspecified) (if (isDark) 0.35f else 0.20f) else (if (isDark) 0.22f else 0.12f)
                        )
                } else {
                    mod
                }
            }
            .let { mod ->
                if (isFluid) {
                    mod.clickable(
                        enabled = enabled,
                        interactionSource = interactionSource,
                        indication = BounceIndication
                    ) { performAction = true }
                } else {
                    mod.clickable(enabled = enabled) { performAction = true }
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = if (isFluid) Color.Transparent else animatedBackground
        ),
        shape = shape,
        elevation = CardDefaults.cardElevation()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(28.dp),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = animatedContent,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.basicMarquee()
            )
        }
    }
}

@Composable
fun NewMenuItem(
    headlineContent: @Composable () -> Unit,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    supportingContent: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val isFluid = isFluidTheme()
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }

    androidx.compose.material3.ListItem(
        headlineContent = headlineContent,
        leadingContent = leadingContent,
        trailingContent = trailingContent,
        supportingContent = supportingContent,
        modifier = modifier
            .let { mod ->
                if (onClick != null) {
                    if (isFluid) {
                        mod.clickable(
                            enabled = enabled,
                            interactionSource = interactionSource,
                            indication = BounceIndication,
                            onClick = onClick
                        )
                    } else {
                        mod.clickable(enabled = enabled, onClick = onClick)
                    }
                } else mod
            }
            .padding(horizontal = 4.dp),
        tonalElevation = 0.dp
    )
}

@Composable
fun NewMenuSectionHeader(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp
        ),
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(horizontal = 20.dp, vertical = 12.dp)
    )
}

@Composable
fun NewActionGrid(
    actions: List<NewAction>,
    modifier: Modifier = Modifier,
    columns: Int = 3
) {
    val isFluid = isFluidTheme()
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val indexedActions = actions.mapIndexed { index, action -> index to action }
        val chunks = indexedActions.chunked(columns)
        chunks.forEach { rowIndexedActions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(if (isFluid) 8.dp else ButtonGroupDefaults.ConnectedSpaceBetween)
            ) {
                rowIndexedActions.forEach { (index, action) ->
                    var performAction by remember { mutableStateOf(false) }

                    if (performAction) {
                        action.onClick()
                        LaunchedEffect(Unit) {
                            performAction = false
                        }
                    }

                    val bgColor =
                        if (action.backgroundColor != Color.Unspecified) action.backgroundColor else MaterialTheme.colorScheme.surfaceVariant
                    val contentCol =
                        if (action.contentColor != Color.Unspecified) {
                            action.contentColor
                        } else if (isFluid && !isDark) {
                            Color(0xFF141416)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }

                    val fluidBtnShape = when {
                        rowIndexedActions.size == 1 -> ContinuousRoundedRectangle(16.dp)
                        index == 0 -> ContinuousRoundedRectangle(topStart = 16.dp, bottomStart = 16.dp, topEnd = 6.dp, bottomEnd = 6.dp)
                        index == rowIndexedActions.lastIndex -> ContinuousRoundedRectangle(topStart = 6.dp, bottomStart = 6.dp, topEnd = 16.dp, bottomEnd = 16.dp)
                        else -> ContinuousRoundedRectangle(6.dp)
                    }

                    ToggleButton(
                        checked = false,
                        onCheckedChange = { performAction = true },
                        enabled = action.enabled,
                        shapes = when {
                            actions.size == 1 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            index == 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            index == actions.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        },
                        colors = ToggleButtonDefaults.toggleButtonColors(
                            containerColor = if (isFluid) Color.Transparent else bgColor,
                            contentColor = contentCol,
                            disabledContainerColor = (if (isFluid) Color.Transparent else bgColor).copy(alpha = 0.5f),
                            disabledContentColor = contentCol.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .let { mod ->
                                if (isFluid) {
                                    val tint = if (action.backgroundColor.isSpecified && action.backgroundColor != Color.Unspecified) {
                                        action.backgroundColor.copy(alpha = if (isDark) 0.16f else 0.12f)
                                    } else {
                                        if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)
                                    }
                                    mod
                                        .clip(fluidBtnShape)
                                        .background(tint, fluidBtnShape)
                                        .border(
                                            width = 0.5.dp,
                                            color = if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.08f),
                                            shape = fluidBtnShape
                                        )
                                } else {
                                    mod
                                }
                            }
                            .semantics { role = Role.Button }
                    ) {
                        action.icon()
                        Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                        Text(
                            text = action.text,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isFluid) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

data class NewAction(
    val icon: @Composable () -> Unit,
    val text: String,
    val onClick: @Composable () -> Unit,
    val enabled: Boolean = true,
    val backgroundColor: Color = Color.Unspecified,
    val contentColor: Color = Color.Unspecified
)

@Composable
fun NewMenuContent(
    headerContent: @Composable (() -> Unit)? = null,
    actionGrid: @Composable (() -> Unit)? = null,
    menuItems: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        headerContent?.invoke()

        actionGrid?.invoke()

        if (headerContent != null && actionGrid != null) {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }

        menuItems?.invoke()
    }
}

@Composable
fun NewIconButton(
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    val animatedBackground by animateColorAsState(
        targetValue = if (enabled) backgroundColor else backgroundColor.copy(alpha = 0.5f),
        animationSpec = tween(200),
        label = "background"
    )

    val animatedContent by animateColorAsState(
        targetValue = if (enabled) contentColor else contentColor.copy(alpha = 0.5f),
        animationSpec = tween(200),
        label = "content"
    )

    Card(
        modifier = modifier
            .clickable(enabled = enabled) { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = animatedBackground
        ),
        shape = CircleShape,
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
    }
}

@Composable
fun NewMenuContainer(
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp)
    ) {
        content()
    }
}
