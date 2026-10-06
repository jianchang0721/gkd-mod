package li.gkd.app.ui.style

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.Shapes
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import li.gkd.app.shapes.RoundedCornerStyle
import li.gkd.app.shapes.roundedRectangleOutline

/**
 * 连续曲率圆角 (iOS 风格 squircle) 的 Material 形状适配.
 *
 * MaterialTheme.shapes 只接受 CornerBasedShape, 而连续曲率形状只实现 Compose Shape,
 * 且本版本 CornerBasedShape 的 createOutline(Size, LayoutDirection, Density) 已是 final,
 * 只能在「已解析为像素的圆角半径」这个抽象方法里接入自己的轮廓算法.
 *
 * 轮廓与贝塞尔控制点算法来自 Kyant0/Shapes (Apache-2.0), 已内联到 li.gkd.app.shapes.
 */
class GkContinuousCornerShape(private val cornerSize: CornerSize) : CornerBasedShape(
    topStart = cornerSize,
    topEnd = cornerSize,
    bottomEnd = cornerSize,
    bottomStart = cornerSize,
) {
    override fun createOutline(
        size: Size,
        topStart: Float,
        topEnd: Float,
        bottomEnd: Float,
        bottomStart: Float,
        layoutDirection: LayoutDirection,
    ): Outline {
        val radius = minOf(topStart, topEnd, bottomEnd, bottomStart)
            .coerceIn(0f, size.minDimension * 0.5f)
        return roundedRectangleOutline(
            size = size,
            radius = radius,
            style = RoundedCornerStyle.Continuous,
        )
    }

    override fun copy(
        topStart: CornerSize,
        topEnd: CornerSize,
        bottomEnd: CornerSize,
        bottomStart: CornerSize,
    ): CornerBasedShape = GkContinuousCornerShape(topStart)
}

/** iOS 风格连续曲率圆角的 Material 形状集. */
fun continuousCurvatureShapes(
    extraSmall: Dp = 6.dp,
    small: Dp = 10.dp,
    medium: Dp = 14.dp,
    large: Dp = 20.dp,
    extraLarge: Dp = 28.dp,
): Shapes = Shapes(
    extraSmall = GkContinuousCornerShape(CornerSize(extraSmall)),
    small = GkContinuousCornerShape(CornerSize(small)),
    medium = GkContinuousCornerShape(CornerSize(medium)),
    large = GkContinuousCornerShape(CornerSize(large)),
    extraLarge = GkContinuousCornerShape(CornerSize(extraLarge)),
)
