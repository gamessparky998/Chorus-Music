package pushkar.chorus.music.ui.fluid.foundation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pushkar.chorus.music.ui.component.shapes.ContinuousRoundedRectangle

@Immutable
data class FluidMenuColors(
    val fill: Color,
    val label: Color,
    val secondaryLabel: Color,
    val separator: Color,
    val highlight: Color,
)

object FluidMenuTokens {
    val DarkPanelFill = Color(0xFF1C1C1E)
    val CornerRadius = 26.dp
    val PanelShape = FluidShapes.continuous(CornerRadius)

    val Light = FluidMenuColors(
        fill = Color(0xFFF5F5F7).copy(alpha = 0.85f),
        label = Color(0xFF111111),
        secondaryLabel = Color(0xFF111111).copy(alpha = 0.6f),
        separator = Color.Black.copy(alpha = 0.08f),
        highlight = Color.Black.copy(alpha = 0.06f),
    )

    val Dark = FluidMenuColors(
        fill = DarkPanelFill.copy(alpha = 0.85f),
        label = Color(0xFFFFFFFF),
        secondaryLabel = Color.White.copy(alpha = 0.65f),
        separator = Color.White.copy(alpha = 0.08f),
        highlight = Color.White.copy(alpha = 0.08f),
    )

    fun colors(isDark: Boolean): FluidMenuColors = if (isDark) Dark else Light
}

@Composable
fun FluidMenuGroup(
    modifier: Modifier = Modifier,
    shape: ContinuousRoundedRectangle = FluidMenuTokens.PanelShape,
    elevation: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val isDark = glassUsesDarkAppearance()
    val colors = FluidMenuTokens.colors(isDark)

    Column(
        modifier = modifier
            .shadow(elevation, shape = shape, clip = false)
            .clip(shape)
            .background(colors.fill)
            .rimLight(shape = shape)
            .padding(vertical = 4.dp),
        content = content,
    )
}

@Composable
fun FluidMenuItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    destructive: Boolean = false,
) {
    val isDark = glassUsesDarkAppearance()
    val colors = FluidMenuTokens.colors(isDark)
    val labelColor = if (destructive) GlassTintTokens.selectedRedColor(isDark) else colors.label

    Row(
        modifier = modifier
            .fillMaxWidth()
            .fluidBounceClick(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false),
        ) {
            if (leadingIcon != null) {
                Box(
                    modifier = Modifier.size(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    leadingIcon()
                }
                Spacer(Modifier.width(14.dp))
            }
            Column {
                Text(
                    text = title,
                    color = labelColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                )
                if (subtitle != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        color = colors.secondaryLabel,
                        fontSize = 12.sp,
                    )
                }
            }
        }
        if (trailingContent != null) {
            trailingContent()
        }
    }
}
