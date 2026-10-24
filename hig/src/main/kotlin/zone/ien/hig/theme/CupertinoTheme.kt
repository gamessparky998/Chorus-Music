package zone.ien.hig.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import zone.ien.hig.InternalCupertinoApi

@OptIn(InternalCupertinoApi::class)
object CupertinoTheme {

    val colorScheme: ColorScheme
        @Composable
        @ReadOnlyComposable
        get() = LocalColorScheme.current

    val typography: Typography
        @Composable
        @ReadOnlyComposable
        get() = LocalTypography.current

    val shapes: Shapes
        @Composable
        @ReadOnlyComposable
        get() = LocalShapes.current
}
