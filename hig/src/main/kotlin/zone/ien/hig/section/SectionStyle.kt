package zone.ien.hig.section

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

enum class SectionStyle(
    val inset: Boolean,
    val grouped: Boolean,
) {
    Sidebar(true, true),
    InsetGrouped(true, true),
    Grouped(false, true),
    Inset(true, false),
    Plain(false, false),
}

val LocalSectionStyle =
    compositionLocalOf {
        SectionStyle.InsetGrouped
    }

@Composable
fun ProvideSectionStyle(
    style: SectionStyle,
    content: @Composable () -> Unit,
) = CompositionLocalProvider(LocalSectionStyle provides style, content = content)

@Composable
fun String.sectionTitle(style: SectionStyle = LocalSectionStyle.current) =
    if (style.shouldCapsTitle) uppercase() else this

fun Modifier.sectionContainerBackground(style: SectionStyle? = null) =
    composed {
        background(
            CupertinoSectionDefaults.containerColor(style ?: LocalSectionStyle.current),
        )
    }

internal val SectionStyle.shouldCapsTitle
    get() = this != SectionStyle.Sidebar && grouped

internal val SectionStyle.shouldFillContainer
    get() = !grouped
