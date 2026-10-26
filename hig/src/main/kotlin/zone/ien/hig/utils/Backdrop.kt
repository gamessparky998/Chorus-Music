package zone.ien.hig.utils

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

import zone.ien.hig.InternalCupertinoApi
import zone.ien.hig.theme.CupertinoTheme
import zone.ien.hig.theme.isInitializedCupertinoTheme

@OptIn(InternalCupertinoApi::class)
@Composable
fun rememberDefaultBackdrop(): LayerBackdrop {
    val isDark = if (isInitializedCupertinoTheme()) CupertinoTheme.colorScheme.isDark else isSystemInDarkTheme()
    val background = if (isDark) Color.Black else Color.White
    return rememberLayerBackdrop {
        drawRect(background)
        drawContent()
    }
}
