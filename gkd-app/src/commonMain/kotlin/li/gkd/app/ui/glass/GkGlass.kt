package li.gkd.app.ui.glass

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.isRenderEffectSupported
import li.gkd.app.ui.style.UiStyle

/** 当前生效的界面风格; 由 GkGlassRoot 注入. */
val LocalUiStyle = staticCompositionLocalOf { UiStyle.Default }

/** 环境背景层: 装饰渐变, 供所有玻璃表面折射. */
val LocalGkAmbientBackdrop = staticCompositionLocalOf<LayerBackdrop?> { null }

/**
 * 玻璃风格下用模糊+高光绘制表面, 其它情况退回普通背景色.
 *
 * @param fallbackColor 非玻璃风格 (或设备不支持渲染效果) 时使用的背景色
 * @param blurRadius 模糊半径; 越大越"厚"
 * @param tintAlpha 玻璃表面上的色调透明度, 0 表示完全透明只留模糊
 */
@Composable
fun Modifier.gkGlassSurface(
    shape: Shape,
    fallbackColor: Color,
    blurRadius: Dp = 18.dp,
    tintAlpha: Float = 0.25f,
    highlight: Boolean = true,
): Modifier {
    val style = LocalUiStyle.current
    val backdrop = LocalGkAmbientBackdrop.current
    // Android 12 以下没有 RenderEffect, backdrop 的 blur 会静默失效; 此时退回不透明背景保证可读性
    if (style != UiStyle.LiquidGlass || backdrop == null || !isRenderEffectSupported()) {
        val color =
            if (style == UiStyle.LiquidGlass) fallbackColor.copy(alpha = 0.94f) else fallbackColor
        return this.clip(shape).background(color)
    }
    val blurPx = with(LocalDensity.current) { blurRadius.toPx() }
    return this.drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = { blur(blurPx) },
        highlight = if (highlight) ({ Highlight.Default }) else null,
        shadow = null,
        onDrawSurface = {
            if (tintAlpha > 0f) {
                drawRect(fallbackColor.copy(alpha = tintAlpha))
            }
        },
    )
}

/**
 * 玻璃风格下的配色: 把表面色系调成半透明.
 *
 * 卡片/列表/弹窗都用 surfaceContainer 系列作为容器色, 所以只改配色就能让整个应用
 * 的容器一起透出背景渐变, 不需要逐个组件改造. 文字色不动, 保证可读性.
 */
fun glassColorScheme(base: androidx.compose.material3.ColorScheme): androidx.compose.material3.ColorScheme =
    base.copy(
        surface = base.surface.copy(alpha = 0.5f),
        surfaceVariant = base.surfaceVariant.copy(alpha = 0.5f),
        surfaceDim = base.surfaceDim.copy(alpha = 0.6f),
        surfaceBright = base.surfaceBright.copy(alpha = 0.6f),
        surfaceContainer = base.surfaceContainer.copy(alpha = 0.5f),
        surfaceContainerLow = base.surfaceContainerLow.copy(alpha = 0.45f),
        surfaceContainerLowest = base.surfaceContainerLowest.copy(alpha = 0.4f),
        surfaceContainerHigh = base.surfaceContainerHigh.copy(alpha = 0.55f),
        surfaceContainerHighest = base.surfaceContainerHighest.copy(alpha = 0.62f),
    )

/** 顶层玻璃容器: 记录环境渐变层, 并把风格注入整棵树. */
@Composable
fun GkGlassRoot(
    style: UiStyle,
    content: @Composable () -> Unit,
) {
    if (style != UiStyle.LiquidGlass) {
        CompositionLocalProvider(LocalUiStyle provides style) { content() }
        return
    }
    val ambientBackdrop = rememberLayerBackdrop()
    CompositionLocalProvider(
        LocalUiStyle provides style,
        LocalGkAmbientBackdrop provides ambientBackdrop,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 只有背景被录制进玻璃层, 玻璃表面本身不参与录制, 避免自折射
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(ambientBackdrop),
            ) {
                GkGlassBackground()
            }
            content()
        }
    }
}

/**
 * 环境渐变: 三个主题色光斑缓慢浮动.
 *
 * 玻璃本身是模糊, 没有可折射的内容就没有视觉效果, 所以这一层是玻璃风格的视觉基础.
 */
@Composable
fun GkGlassBackground() {
    val scheme = MaterialTheme.colorScheme
    val transition = rememberInfiniteTransition(label = "gk-glass-bg")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 24_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "gk-glass-phase",
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(color = scheme.background)
                val radius = size.minDimension * 0.85f
                val shift = phase * size.minDimension * 0.25f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(scheme.primary.copy(alpha = 0.45f), Color.Transparent),
                        center = Offset(size.width * 0.12f + shift, size.height * 0.08f + shift * 0.5f),
                        radius = radius,
                    ),
                    radius = radius,
                    center = Offset(size.width * 0.12f + shift, size.height * 0.08f + shift * 0.5f),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(scheme.tertiary.copy(alpha = 0.4f), Color.Transparent),
                        center = Offset(size.width * 0.9f - shift, size.height * 0.32f - shift * 0.4f),
                        radius = radius * 0.9f,
                    ),
                    radius = radius * 0.9f,
                    center = Offset(size.width * 0.9f - shift, size.height * 0.32f - shift * 0.4f),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(scheme.secondary.copy(alpha = 0.35f), Color.Transparent),
                        center = Offset(size.width * 0.45f, size.height * 0.95f - shift * 0.6f),
                        radius = radius,
                    ),
                    radius = radius,
                    center = Offset(size.width * 0.45f, size.height * 0.95f - shift * 0.6f),
                )
            },
    )
}
