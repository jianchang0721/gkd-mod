package li.gkd.app.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import li.gkd.app.shapes.Capsule
import li.gkd.app.ui.component.GkIcon
import li.gkd.app.ui.glass.LocalUiStyle
import li.gkd.app.ui.glass.gkGlassSurface
import li.gkd.app.ui.style.UiStyle

private val CapsuleHeight = 64.dp
private val IndicatorInset = 6.dp

/**
 * 悬浮胶囊底栏.
 *
 * 参考样式: 白色悬浮胶囊容器 + 选中项背后的动画胶囊指示器 + 图标+文字页签.
 * - 玻璃风格下容器与指示器都是模糊玻璃 (折射顶层环境渐变层)
 * - 其它风格下是带投影的实体胶囊
 * - 无障碍: 页签使用 selectable + Role.Tab, 并保留图标与文字的语义
 */
@Composable
fun GkFloatingCapsuleNav(
    selectedTab: BottomNavItem,
    onSelectTab: (BottomNavItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = BottomNavItem.allSubObjects
    val selectedIndex = items.indexOf(selectedTab).coerceAtLeast(0)
    val scheme = MaterialTheme.colorScheme
    val glassMode = LocalUiStyle.current == UiStyle.LiquidGlass
    val containerShape = Capsule()
    val indicatorShape = Capsule()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(
                LocalHomeNavigationInsets.current ?: NavigationBarDefaults.windowInsets,
            )
            // 悬浮感来自外边距, 让胶囊和屏幕边缘脱开
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(CapsuleHeight),
        ) {
            val itemWidth = maxWidth / items.size
            val indicatorOffset by animateDpAsState(
                targetValue = itemWidth * selectedIndex + IndicatorInset,
                animationSpec = spring(
                    dampingRatio = 0.78f,
                    stiffness = Spring.StiffnessMediumLow,
                ),
                label = "gk-nav-indicator",
            )
            // 悬浮胶囊容器
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (glassMode) {
                            Modifier.gkGlassSurface(
                                shape = containerShape,
                                fallbackColor = scheme.surfaceContainer,
                                blurRadius = 26.dp,
                                tintAlpha = 0.22f,
                            )
                        } else {
                            Modifier
                                .shadow(elevation = 12.dp, shape = containerShape)
                                .clip(containerShape)
                                .background(scheme.surfaceContainer)
                        },
                    ),
            )
            // 选中指示胶囊
            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .padding(vertical = IndicatorInset)
                    .width(itemWidth - IndicatorInset * 2)
                    .fillMaxHeight()
                    .then(
                        if (glassMode) {
                            Modifier.gkGlassSurface(
                                shape = indicatorShape,
                                fallbackColor = scheme.secondaryContainer,
                                blurRadius = 12.dp,
                                tintAlpha = 0.55f,
                            )
                        } else {
                            Modifier
                                .clip(indicatorShape)
                                .background(scheme.secondaryContainer)
                        },
                    ),
            )
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .selectableGroup(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEachIndexed { index, item ->
                    val selected = index == selectedIndex
                    val contentColor by animateColorAsState(
                        targetValue =
                            if (selected) scheme.onSecondaryContainer else scheme.onSurfaceVariant,
                        label = "gk-nav-content-color",
                    )
                    val iconScale by animateFloatAsState(
                        targetValue = if (selected) 1f else 0.92f,
                        label = "gk-nav-icon-scale",
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .selectable(
                                selected = selected,
                                role = Role.Tab,
                                onClick = { onSelectTab(item) },
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        CompositionLocalProvider(LocalContentColor provides contentColor) {
                            GkIcon(
                                imageVector = item.icon,
                                modifier = Modifier
                                    .size(24.dp)
                                    .graphicsLayer {
                                        scaleX = iconScale
                                        scaleY = iconScale
                                    },
                                tint = contentColor,
                                contentDescription = null,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = contentColor,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}
