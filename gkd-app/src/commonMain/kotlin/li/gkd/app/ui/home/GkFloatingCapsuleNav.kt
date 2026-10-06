package li.gkd.app.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt
import li.gkd.app.shapes.Capsule
import li.gkd.app.ui.component.GkIcon
import li.gkd.app.ui.glass.GkGlassLayer
import li.gkd.app.ui.glass.LocalGkLiveRefraction
import li.gkd.app.ui.glass.LocalUiStyle
import li.gkd.app.ui.glass.gkGlassSurface
import li.gkd.app.ui.style.UiStyle

private val CapsuleHeight = 64.dp
private val IndicatorInset = 6.dp

/** 气泡"游走"的速度参考值 (px/s), 用于把速度映射成拉伸程度. */
private const val BubbleSpeedReference = 3200f

/**
 * 悬浮胶囊底栏 + 气泡游走动效.
 *
 * 动效思路 (气泡在水柱中游走):
 * - 指示器位置用低阻尼弹簧跟随 -> 有"水阻"般的过冲与回弹
 * - 由实时速度驱动拉伸 (沿运动方向拉长、垂直方向压扁), 速度越快越像被水拖长
 * - 气泡持续做极轻微的上下浮动, 静止时也像悬在水中
 * - 气泡身后跟一条随速度出现的拉伸光晕, 形成"水痕"拖尾
 *
 * 玻璃风格下容器与气泡都是模糊玻璃, 且可折射页面真实内容 (实时折射开启时).
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
    val liveRefraction = LocalGkLiveRefraction.current
    val refractionLayer = if (liveRefraction) GkGlassLayer.Content else GkGlassLayer.Ambient
    val containerShape = Capsule()
    val indicatorShape = Capsule()
    val density = LocalDensity.current

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
            val bubbleWidth = itemWidth - IndicatorInset * 2
            val targetOffset = with(LocalDensity.current) {
                (itemWidth * selectedIndex).toPx()
            }
            val insetPx = with(LocalDensity.current) {
                IndicatorInset.toPx()
            }
            val bubbleOffset = remember { Animatable(0f) }
            var positioned by remember { mutableStateOf(false) }
            LaunchedEffect(targetOffset) {
                if (!positioned) {
                    positioned = true
                    bubbleOffset.snapTo(targetOffset)
                } else {
                    bubbleOffset.animateTo(
                        targetValue = targetOffset,
                        animationSpec = spring(
                            dampingRatio = 0.55f,
                            stiffness = Spring.StiffnessMediumLow,
                        ),
                    )
                }
            }
            val bob by rememberInfiniteTransition(label = "gk-bubble").animateFloat(
                initialValue = -1f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 2800, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "gk-bubble-bob",
            )
            val bobPx = with(LocalDensity.current) { 1.5.dp.toPx() }

            // 悬浮胶囊容器
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (glassMode) {
                            Modifier.gkGlassSurface(
                                shape = containerShape,
                                fallbackColor = scheme.surfaceContainer,
                                layer = refractionLayer,
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

            // 水痕拖尾: 速度越大越明显, 越被拉长
            Box(
                modifier = Modifier
                    .offset { IntOffset((bubbleOffset.value + insetPx).roundToInt(), 0) }
                    .padding(vertical = IndicatorInset)
                    .width(bubbleWidth)
                    .fillMaxHeight()
                    .graphicsLayer {
                        val stretch = (abs(bubbleOffset.velocity) / BubbleSpeedReference)
                            .coerceIn(0f, 1f)
                        alpha = 0.5f * stretch
                        scaleX = 1f + stretch * 0.55f
                        scaleY = 1f - stretch * 0.25f
                        translationY = bob * bobPx
                    }
                    .clip(indicatorShape)
                    .background(scheme.primary.copy(alpha = 0.45f)),
            )

            // 气泡本身: 速度驱动拉伸 + 悬浮浮动
            Box(
                modifier = Modifier
                    .offset { IntOffset((bubbleOffset.value + insetPx).roundToInt(), 0) }
                    .padding(vertical = IndicatorInset)
                    .width(bubbleWidth)
                    .fillMaxHeight()
                    .graphicsLayer {
                        val stretch = (abs(bubbleOffset.velocity) / BubbleSpeedReference)
                            .coerceIn(0f, 1f)
                        scaleX = 1f + stretch * 0.22f
                        scaleY = 1f - stretch * 0.16f
                        translationY = bob * bobPx
                    }
                    .then(
                        if (glassMode) {
                            Modifier.gkGlassSurface(
                                shape = indicatorShape,
                                fallbackColor = scheme.secondaryContainer,
                                layer = refractionLayer,
                                blurRadius = 12.dp,
                                tintAlpha = 0.5f,
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
