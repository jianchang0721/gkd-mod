package li.gkd.app.ui.style

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.Shapes
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.kyant.shapes.RoundedRectangle

/**
 * 把任意 Shape 适配成 Material 的 CornerBasedShape.
 *
 * MaterialTheme.shapes 只接受 CornerBasedShape, 而 kyant 的连续曲率圆角只实现了 Shape,
 * 所以这里只做类型适配, 轮廓计算仍然交给被包装的形状.
 */
class GkDelegatedCornerShape(private val delegate: Shape) : CornerBasedShape(
    topStart = CornerSize.Zero,
    topEnd = CornerSize.Zero,
    bottomEnd = CornerSize.Zero,
    bottomStart = CornerSize.Zero,
) {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline = delegate.createOutline(size, layoutDirection, density)

    override fun copy(
        topStart: CornerSize,
        topEnd: CornerSize,
        bottomEnd: CornerSize,
        bottomStart: CornerSize,
    ): CornerBasedShape = this
}

/**
 * iOS 风格连续曲率圆角 (com.kyant.shapes) 的 Material 形状集.
 */
fun continuousCurvatureShapes(
    extraSmall: Dp = 6.dp,
    small: Dp = 10.dp,
    medium: Dp = 14.dp,
    large: Dp = 20.dp,
    extraLarge: Dp = 28.dp,
): Shapes = Shapes(
    extraSmall = GkDelegatedCornerShape(RoundedRectangle(extraSmall)),
    small = GkDelegatedCornerShape(RoundedRectangle(small)),
    medium = GkDelegatedCornerShape(RoundedRectangle(medium)),
    large = GkDelegatedCornerShape(RoundedRectangle(large)),
    extraLarge = GkDelegatedCornerShape(RoundedRectangle(extraLarge)),
)
