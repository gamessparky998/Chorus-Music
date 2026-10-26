package pushkar.chorus.music.ui.fluid.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import zone.ien.hig.theme.ColorScheme
import zone.ien.hig.theme.CupertinoTheme as HigTheme
import zone.ien.hig.theme.Typography as HigTypography
import zone.ien.hig.theme.darkColorScheme
import zone.ien.hig.theme.lightColorScheme

@Composable
internal fun ProvideHigTheme(
    colors: FluidColors,
    typography: FluidTypography,
    content: @Composable () -> Unit,
) {
    val higColors = remember(colors) { colors.toHigColorScheme() }
    val higTypography = remember(typography) { typography.toHigTypography() }
    HigTheme(
        colorScheme = higColors,
        typography = higTypography,
        content = content
    )
}

private fun FluidColors.toHigColorScheme(): ColorScheme = if (isDark) {
    darkColorScheme(
        accent = accent,
        label = label,
        secondaryLabel = secondaryLabel,
        tertiaryLabel = tertiaryLabel,
        quaternaryLabel = quaternaryLabel,
        systemFill = systemFill,
        secondarySystemFill = secondarySystemFill,
        tertiarySystemFill = tertiarySystemFill,
        quaternarySystemFill = quaternarySystemFill,
        placeholderText = placeholderText,
        separator = separator,
        opaqueSeparator = opaqueSeparator,
        link = systemBlue,
        systemGroupedBackground = systemGroupedBackground,
        secondarySystemGroupedBackground = secondarySystemGroupedBackground,
        tertiarySystemGroupedBackground = tertiarySystemGroupedBackground,
        systemBackground = systemBackground,
        secondarySystemBackground = secondarySystemBackground,
        tertiarySystemBackground = tertiarySystemBackground,
    )
} else {
    lightColorScheme(
        accent = accent,
        label = label,
        secondaryLabel = secondaryLabel,
        tertiaryLabel = tertiaryLabel,
        quaternaryLabel = quaternaryLabel,
        systemFill = systemFill,
        secondarySystemFill = secondarySystemFill,
        tertiarySystemFill = tertiarySystemFill,
        quaternarySystemFill = quaternarySystemFill,
        placeholderText = placeholderText,
        separator = separator,
        opaqueSeparator = opaqueSeparator,
        link = systemBlue,
        systemGroupedBackground = systemGroupedBackground,
        secondarySystemGroupedBackground = secondarySystemGroupedBackground,
        tertiarySystemGroupedBackground = tertiarySystemGroupedBackground,
        systemBackground = systemBackground,
        secondarySystemBackground = secondarySystemBackground,
        tertiarySystemBackground = tertiarySystemBackground,
    )
}

private fun FluidTypography.toHigTypography() = HigTypography(
    largeTitle = largeTitle,
    title1 = title1,
    title2 = title2,
    title3 = title3,
    headline = headline,
    body = body,
    callout = callout,
    subhead = subheadline,
    footnote = footnote,
    caption1 = caption1,
    caption2 = caption2,
)
