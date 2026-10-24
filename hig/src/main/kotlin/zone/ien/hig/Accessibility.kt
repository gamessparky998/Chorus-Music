@file:Suppress("unused")

package zone.ien.hig

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import zone.ien.hig.theme.CupertinoColors
import zone.ien.hig.theme.White
import zone.ien.hig.theme.isDark

object Accessibility

val Accessibility.isHighContrastEnabled: Boolean get() = false

val Accessibility.isReduceTransparencyEnabled: Boolean get() = false

val Color.accessible: Color
    @Composable
    get() = accessible(isDark())

fun Color.accessible(isDark: Boolean): Color =
    if (Accessibility.isHighContrastEnabled) {
        lerp(this, if (isDark) CupertinoColors.White else Color.Black, .2f)
    } else {
        this
    }
