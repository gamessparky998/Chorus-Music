package zone.ien.hig

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalContentColor: ProvidableCompositionLocal<Color>
    @Composable
    @ReadOnlyComposable
    get() = LocalContentColorProvider.current

val LocalContentColorProvider =
    staticCompositionLocalOf {
        EmptyLocalColor
    }

private val EmptyLocalColor =
    compositionLocalOf {
        Color.Black
    }
