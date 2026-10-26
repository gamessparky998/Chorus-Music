package pushkar.chorus.music.ui.fluid.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pushkar.chorus.music.ui.component.shapes.ContinuousRoundedRectangle
import pushkar.chorus.music.ui.fluid.foundation.FluidMetrics
import pushkar.chorus.music.ui.fluid.foundation.FluidTheme
import pushkar.chorus.music.ui.fluid.foundation.LocalFluidContentColor
import pushkar.chorus.music.ui.fluid.foundation.LocalFluidTextStyle

enum class FluidListStyle { Plain, Inset, Grouped, InsetGrouped, Sidebar }

enum class FluidSeparatorVisibility { Automatic, Visible, Hidden }

@Immutable
data class FluidRowStyle(
    val background: Color = Color.Unspecified,
    val insets: PaddingValues? = null,
    val separator: FluidSeparatorVisibility = FluidSeparatorVisibility.Automatic,
    val separatorTint: Color = Color.Unspecified,
    val minHeight: Dp = FluidMetrics.ListRowHeight,
)

class FluidListScope internal constructor(
    private val lazyScope: LazyListScope,
    private val style: FluidListStyle,
) {
    fun row(
        key: Any? = null,
        rowStyle: FluidRowStyle = FluidRowStyle(),
        content: @Composable RowScope.() -> Unit,
    ) = section(key = key) { row(rowStyle = rowStyle, content = content) }

    fun section(
        key: Any? = null,
        header: (@Composable () -> Unit)? = null,
        footer: (@Composable () -> Unit)? = null,
        content: FluidSectionScope.() -> Unit,
    ) {
        val rows = FluidSectionScope().apply(content).rows
        if (rows.isEmpty() && header == null && footer == null) return

        val grouped = style == FluidListStyle.InsetGrouped || style == FluidListStyle.Grouped ||
                style == FluidListStyle.Inset || style == FluidListStyle.Sidebar
        val horizontalInset = when (style) {
            FluidListStyle.InsetGrouped, FluidListStyle.Inset, FluidListStyle.Sidebar -> FluidMetrics.ListRowInset
            else -> 0.dp
        }

        if (header != null) {
            lazyScope.item(key = key?.let { "$it-header" }) {
                FluidSectionHeader(header, horizontalInset, grouped)
            }
        }

        rows.forEachIndexed { index, row ->
            lazyScope.item(key = key?.let { "$it-$index" }) {
                FluidListRowContainer(
                    style = style,
                    rowStyle = row.style,
                    isFirst = index == 0,
                    isLast = index == rows.lastIndex,
                    horizontalInset = horizontalInset,
                    content = row.content,
                )
            }
        }

        if (footer != null) {
            lazyScope.item(key = key?.let { "$it-footer" }) {
                FluidSectionFooter(footer, horizontalInset)
            }
        }

        lazyScope.item(key = key?.let { "$it-spacer" }) {
            Box(Modifier.height(if (grouped) FluidMetrics.SectionSpacing else 0.dp))
        }
    }
}

class FluidSectionScope internal constructor() {
    internal val rows = mutableListOf<FluidSectionRow>()

    fun row(rowStyle: FluidRowStyle = FluidRowStyle(), content: @Composable RowScope.() -> Unit) {
        rows += FluidSectionRow(rowStyle, content)
    }

    fun <T> rows(
        items: List<T>,
        rowStyle: FluidRowStyle = FluidRowStyle(),
        content: @Composable RowScope.(T) -> Unit,
    ) = items.forEach { item -> row(rowStyle) { content(item) } }
}

internal data class FluidSectionRow(
    val style: FluidRowStyle,
    val content: @Composable RowScope.() -> Unit,
)

@Composable
fun FluidList(
    modifier: Modifier = Modifier,
    style: FluidListStyle = FluidListStyle.InsetGrouped,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(vertical = 20.dp),
    content: FluidListScope.() -> Unit,
) {
    val colors = FluidTheme.colors
    val background = when (style) {
        FluidListStyle.Plain, FluidListStyle.Inset -> colors.systemBackground
        FluidListStyle.Grouped, FluidListStyle.InsetGrouped, FluidListStyle.Sidebar ->
            colors.systemGroupedBackground
    }

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .background(background),
        state = state,
        contentPadding = contentPadding,
    ) {
        FluidListScope(this, style).content()
    }
}

@Composable
private fun FluidSectionHeader(header: @Composable () -> Unit, inset: Dp, grouped: Boolean) {
    val colors = FluidTheme.colors
    Box(
        Modifier.padding(
            start = inset + FluidMetrics.ListRowInset,
            end = inset + FluidMetrics.ListRowInset,
            bottom = if (grouped) 8.dp else 6.dp,
        )
    ) {
        CompositionLocalProvider(
            LocalFluidTextStyle provides FluidTheme.typography.subheadline,
            LocalFluidContentColor provides colors.secondaryLabel,
        ) { header() }
    }
}

@Composable
private fun FluidSectionFooter(footer: @Composable () -> Unit, inset: Dp) {
    val colors = FluidTheme.colors
    Box(
        Modifier.padding(
            start = inset + FluidMetrics.ListRowInset,
            end = inset + FluidMetrics.ListRowInset,
            top = 8.dp,
        )
    ) {
        CompositionLocalProvider(
            LocalFluidTextStyle provides FluidTheme.typography.footnote,
            LocalFluidContentColor provides colors.secondaryLabel,
        ) { footer() }
    }
}

@Composable
private fun FluidListRowContainer(
    style: FluidListStyle,
    rowStyle: FluidRowStyle,
    isFirst: Boolean,
    isLast: Boolean,
    horizontalInset: Dp,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = FluidTheme.colors
    val carded = style == FluidListStyle.InsetGrouped || style == FluidListStyle.Inset ||
            style == FluidListStyle.Sidebar

    val radius = FluidMetrics.GroupedCornerRadius
    val shape: Shape = if (!carded) {
        RectangleShape
    } else {
        when {
            isFirst && isLast -> ContinuousRoundedRectangle(radius)
            isFirst -> ContinuousRoundedRectangle(topStart = radius, topEnd = radius)
            isLast -> ContinuousRoundedRectangle(bottomEnd = radius, bottomStart = radius)
            else -> RectangleShape
        }
    }

    val rowBackground = when {
        rowStyle.background.isSpecified -> rowStyle.background
        style == FluidListStyle.Plain || style == FluidListStyle.Grouped -> colors.systemBackground
        else -> colors.secondarySystemGroupedBackground
    }

    val insets = rowStyle.insets
        ?: PaddingValues(horizontal = FluidMetrics.ListRowInset, vertical = 11.dp)

    val separatorVisible = when (rowStyle.separator) {
        FluidSeparatorVisibility.Hidden -> false
        FluidSeparatorVisibility.Visible -> true
        FluidSeparatorVisibility.Automatic -> !isLast
    }

    Column(
        Modifier
            .padding(horizontal = horizontalInset)
            .clip(shape)
            .background(rowBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = rowStyle.minHeight)
                .padding(insets),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CompositionLocalProvider(
                LocalFluidTextStyle provides FluidTheme.typography.body,
                LocalFluidContentColor provides colors.label,
            ) { content() }
        }

        if (separatorVisible) {
            Box(
                Modifier
                    .padding(start = FluidMetrics.ListRowInset)
                    .fillMaxWidth()
                    .height(FluidMetrics.SeparatorThickness)
                    .background(
                        if (rowStyle.separatorTint.isSpecified) rowStyle.separatorTint
                        else colors.separator
                    )
            )
        }
    }
}

@Composable
fun FluidNavigationRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    value: String? = null,
    icon: (@Composable () -> Unit)? = null,
    iconBackground: Color = Color.Unspecified,
    badge: String? = null,
) {
    val colors = FluidTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(FluidMetrics.IconTileSize)
                    .clip(ContinuousRoundedRectangle(FluidMetrics.IconTileRadius))
                    .background(if (iconBackground.isSpecified) iconBackground else FluidTheme.tint),
                contentAlignment = Alignment.Center,
            ) {
                CompositionLocalProvider(LocalFluidContentColor provides Color.White) { icon() }
            }
        }

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = title,
                style = FluidTheme.typography.body,
                color = colors.label,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = colors.secondaryLabel,
                    style = FluidTheme.typography.footnote,
                )
            }
        }

        if (badge != null) {
            Box(
                modifier = Modifier
                    .clip(ContinuousRoundedRectangle(6.dp))
                    .background(colors.tertiarySystemFill)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = badge,
                    style = FluidTheme.typography.caption2,
                    color = colors.secondaryLabel,
                )
            }
        }
        if (value != null) {
            Text(
                text = value,
                color = colors.secondaryLabel,
                style = FluidTheme.typography.body,
            )
        }
        FluidChevron()
    }
}

@Composable
fun FluidChevron(modifier: Modifier = Modifier, color: Color = Color.Unspecified) {
    val colors = FluidTheme.colors
    val stroke = if (color.isSpecified) color else colors.tertiaryLabel
    Canvas(modifier.size(width = 8.dp, height = 13.dp)) {
        drawPath(
            path = Path().apply {
                moveTo(size.width * 0.15f, 0f)
                lineTo(size.width * 0.95f, size.height / 2f)
                lineTo(size.width * 0.15f, size.height)
            },
            color = stroke,
            style = Stroke(
                width = 2.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
    }
}
