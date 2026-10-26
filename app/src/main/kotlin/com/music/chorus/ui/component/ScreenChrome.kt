/**
 * Chorus Music Project
 * ScreenChrome: Collapsible large-title scaffolds and pages with FrostedTopBar and TopFadeBlur
 */

package pushkar.chorus.music.ui.component

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pushkar.chorus.music.LocalPlayerAwareWindowInsets
import pushkar.chorus.music.ui.fluid.foundation.FluidTheme
import pushkar.chorus.music.ui.fluid.foundation.LocalAppBackdrop
import pushkar.chorus.music.ui.theme.isFluidTheme

@Composable
fun ScreenChromeBox(
    title: String,
    titleAlpha: Float,
    pageColor: Color,
    chrome: GlassScreenChrome,
    onBack: (() -> Unit)?,
    onBackLongClick: (() -> Unit)?,
    actions: @Composable () -> Unit,
    titleAlphaProvider: (() -> Float)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(pageColor),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(pageColor)
                .then(chrome.screenModifier()),
            content = content,
        )

        TopFadeBlur(
            hazeState = chrome.hazeState,
            pageColor = pageColor,
            modifier = Modifier.align(Alignment.TopCenter),
        )

        FrostedTopBar(
            onBack = onBack,
            onBackLongClick = onBackLongClick,
            title = title,
            titleAlpha = titleAlpha,
            titleAlphaProvider = titleAlphaProvider,
            actions = actions,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

@Composable
fun ScreenScaffold(
    title: String,
    titleAlpha: Float,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onBackLongClick: (() -> Unit)? = null,
    pageColor: Color = if (isFluidTheme()) FluidTheme.colors.systemBackground else MaterialTheme.colorScheme.background,
    actions: @Composable () -> Unit = {},
    titleAlphaProvider: (() -> Float)? = null,
    content: @Composable BoxScope.(GlassScreenChrome) -> Unit,
) {
    val chrome = rememberGlassScreenChrome(background = pageColor)
    CompositionLocalProvider(LocalAppBackdrop provides chrome.backdrop) {
        ScreenChromeBox(
            title = title,
            titleAlpha = titleAlpha,
            titleAlphaProvider = titleAlphaProvider,
            pageColor = pageColor,
            chrome = chrome,
            onBack = onBack,
            onBackLongClick = onBackLongClick,
            actions = actions,
        ) {
            content(chrome)
        }
    }
}

@Composable
fun ScreenLargeTitle(title: String, modifier: Modifier = Modifier) {
    val isFluid = isFluidTheme()
    Text(
        text = title,
        style = if (isFluid) FluidTheme.typography.largeTitle else MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Bold,
        color = if (isFluid) FluidTheme.colors.label else MaterialTheme.colorScheme.onSurface,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 14.dp),
    )
}

@Composable
fun rememberTitleCollapseProgressState(state: LazyListState, titleItemKey: Any? = null): State<Float> =
    remember(state, titleItemKey) {
        derivedStateOf {
            val visible = state.layoutInfo.visibleItemsInfo
            val titleItem = if (titleItemKey != null) {
                visible.firstOrNull { it.key == titleItemKey }
            } else {
                visible.firstOrNull { it.index == 0 }
            }
            if (titleItem == null) {
                1f
            } else {
                (-titleItem.offset.toFloat() / titleItem.size.coerceAtLeast(1)).coerceIn(0f, 1f)
            }
        }
    }

@Composable
fun rememberTitleCollapseProgress(state: LazyListState, titleItemKey: Any? = null): Float =
    rememberTitleCollapseProgressState(state, titleItemKey).value

@Composable
fun rememberTitleCollapseProgressState(state: LazyGridState, thresholdPx: Float = 220f): State<Float> =
    remember(state) {
        derivedStateOf {
            val scrolled = if (state.firstVisibleItemIndex == 0) {
                state.firstVisibleItemScrollOffset.toFloat()
            } else {
                thresholdPx
            }
            (scrolled / thresholdPx).coerceIn(0f, 1f)
        }
    }

@Composable
fun rememberTitleCollapseProgress(state: LazyGridState, thresholdPx: Float = 220f): Float =
    rememberTitleCollapseProgressState(state, thresholdPx).value

@Composable
fun ScreenLazyPage(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onBackLongClick: (() -> Unit)? = null,
    pageColor: Color = if (isFluidTheme()) FluidTheme.colors.systemBackground else MaterialTheme.colorScheme.background,
    horizontalPadding: Dp = 0.dp,
    actions: @Composable () -> Unit = {},
    listState: LazyListState = rememberLazyListState(),
    largeTitle: @Composable () -> Unit = { ScreenLargeTitle(title) },
    content: LazyListScope.() -> Unit,
) {
    val chrome = rememberGlassScreenChrome(background = pageColor)
    val lazyTitleProgress = rememberTitleCollapseProgressState(listState, titleItemKey = "title")

    CompositionLocalProvider(LocalAppBackdrop provides chrome.backdrop) {
        ScreenChromeBox(
            title = title,
            titleAlpha = 0f,
            titleAlphaProvider = { lazyTitleProgress.value },
            pageColor = pageColor,
            chrome = chrome,
            onBack = onBack,
            onBackLongClick = onBackLongClick,
            actions = actions,
        ) {
            val sideInsets = LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal).asPaddingValues()
            val direction = LocalLayoutDirection.current
            LazyColumn(
                state = listState,
                modifier = modifier.then(chrome.contentModifier()),
                contentPadding = PaddingValues(
                    start = sideInsets.calculateStartPadding(direction) + horizontalPadding,
                    end = sideInsets.calculateEndPadding(direction) + horizontalPadding,
                ),
            ) {
                item(key = "title") {
                    Column {
                        Spacer(
                            Modifier.windowInsetsPadding(
                                LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top),
                            ),
                        )
                        largeTitle()
                    }
                }

                content()

                item(key = "screen_lazy_page_bottom_spacer") {
                    Spacer(
                        Modifier
                            .height(80.dp)
                            .windowInsetsPadding(
                                LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom),
                            ),
                    )
                }
            }
        }
    }
}
