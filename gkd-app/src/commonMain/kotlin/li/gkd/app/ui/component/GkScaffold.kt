package li.gkd.app.ui.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import li.gkd.app.ui.glass.LocalUiStyle
import li.gkd.app.ui.style.UiStyle

/** Native Scaffold on Android; the development window supplies equivalent safe insets. */
@Composable
fun GkScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {}, bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {}, floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = contentColorFor(containerColor),
    contentWindowInsets: WindowInsets = LocalEditorWindowInsets.current
        ?: ScaffoldDefaults.contentWindowInsets,
    content: @Composable (PaddingValues) -> Unit,
) {
    // 玻璃风格下让页面容器透明, 露出顶层的环境渐变层
    val glassMode = LocalUiStyle.current == UiStyle.LiquidGlass
    Scaffold(
        modifier, topBar, bottomBar, snackbarHost, floatingActionButton,
        floatingActionButtonPosition,
        if (glassMode) Color.Transparent else containerColor,
        contentColor, contentWindowInsets, content
    )
}
