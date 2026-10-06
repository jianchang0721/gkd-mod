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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import li.gkd.app.ui.glass.LocalGkContentBackdrop
import li.gkd.app.ui.glass.LocalGkLiveRefraction
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
    val liveRefraction = LocalGkLiveRefraction.current
    // 实时折射: 录制页面内容供顶栏/底栏折射.
    // 顶栏/底栏是 Scaffold 里内容槽之外的兄弟节点, 所以不会被录进这一层, 不存在自折射.
    val contentLayer = if (glassMode && liveRefraction) rememberLayerBackdrop() else null
    CompositionLocalProvider(
        LocalGkContentBackdrop provides (contentLayer ?: LocalGkContentBackdrop.current),
    ) {
        Scaffold(
            modifier, topBar, bottomBar, snackbarHost, floatingActionButton,
            floatingActionButtonPosition,
            if (glassMode) Color.Transparent else containerColor,
            contentColor, contentWindowInsets,
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (contentLayer != null) Modifier.layerBackdrop(contentLayer) else Modifier,
                    ),
            ) {
                content(padding)
            }
        }
    }
}
