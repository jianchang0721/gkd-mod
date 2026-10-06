package li.gkd.app.ui.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import li.gkd.app.ui.glass.LocalUiStyle
import li.gkd.app.ui.glass.GkGlassLayer
import li.gkd.app.ui.glass.LocalGkImmersiveTopBar
import li.gkd.app.ui.glass.gkGlassSurface
import li.gkd.app.ui.glass.gkTopScrim
import li.gkd.app.ui.style.UiStyle
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

val LocalTopBarWindowInsets = staticCompositionLocalOf<WindowInsets?> { null }

@Composable
fun GkTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    expandedHeight: Dp = TopAppBarDefaults.TopAppBarExpandedHeight,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
    scrollBehavior: TopAppBarScrollBehavior? = null,
    canScroll: Boolean = true,
) {
    val actualScrollBehavior = if (canScroll || scrollBehavior == null) {
        scrollBehavior
    } else {
        remember(scrollBehavior) {
            object : TopAppBarScrollBehavior by scrollBehavior {
                // disable inner scroll effect
                override val isPinned: Boolean
                    get() = true
            }
        }
    }
    // SingleRowTopAppBar 内部 containerColor+scrolledContainerColor 合成了一个动画
    // 应用主题颜色更新时形成叠加动画，导致和周围正常组件视觉变换效果表现割裂
    // 玻璃风格: 顶栏自己画模糊玻璃 (可折射滚动内容), 内部容器色设为透明
    // 沉浸模式: 顶栏完全不画底, 只用一层柔和渐变保证标题可读, 因此不会出现分割线
    val glassMode = LocalUiStyle.current == UiStyle.LiquidGlass
    val immersive = LocalGkImmersiveTopBar.current
    val actualColors = if (glassMode || immersive) {
        TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent,
            titleContentColor = colors.titleContentColor,
            navigationIconContentColor = colors.navigationIconContentColor,
            actionIconContentColor = colors.actionIconContentColor,
        )
    } else {
        colors
    }
    val actualModifier = when {
        glassMode -> modifier.gkGlassSurface(
            shape = RectangleShape,
            fallbackColor = MaterialTheme.colorScheme.surface,
            layer = GkGlassLayer.Content,
            blurRadius = 26.dp,
            tintAlpha = if (immersive) 0.1f else 0.2f,
            highlight = !immersive,
        )

        immersive -> modifier.gkTopScrim(MaterialTheme.colorScheme.surface)
        else -> modifier
    }
    key(MaterialTheme.colorScheme.primary) {
        TopAppBar(
            title = title,
            modifier = actualModifier,
            navigationIcon = navigationIcon,
            actions = actions,
            expandedHeight = expandedHeight,
            windowInsets = LocalTopBarWindowInsets.current ?: TopAppBarDefaults.windowInsets,
            colors = actualColors,
            scrollBehavior = actualScrollBehavior,
        )
    }
}
