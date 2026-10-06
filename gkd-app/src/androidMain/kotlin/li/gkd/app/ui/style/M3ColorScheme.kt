package li.gkd.app.ui.style

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.kyant.m3color.dynamiccolor.DynamicScheme
import com.kyant.m3color.hct.Hct
import com.kyant.m3color.scheme.SchemeContent
import com.kyant.m3color.scheme.SchemeExpressive
import com.kyant.m3color.scheme.SchemeFidelity
import com.kyant.m3color.scheme.SchemeFruitSalad
import com.kyant.m3color.scheme.SchemeMonochrome
import com.kyant.m3color.scheme.SchemeNeutral
import com.kyant.m3color.scheme.SchemeRainbow
import com.kyant.m3color.scheme.SchemeTonalSpot
import com.kyant.m3color.scheme.SchemeVibrant

/**
 * 由主色生成完整的 M3 配色 (m3color = material-color-utilities 的 Java 实现).
 *
 * 只覆盖 ColorScheme 的颜色角色, 未涉及的字段沿用 M3 基线配色.
 */
@Composable
fun rememberM3ColorScheme(
    seedArgb: Long,
    dark: Boolean,
    variant: PaletteStyleOption,
    contrastLevel: Double,
): ColorScheme = remember(seedArgb, dark, variant, contrastLevel) {
    buildM3ColorScheme(seedArgb, dark, variant, contrastLevel)
}

fun buildM3ColorScheme(
    seedArgb: Long,
    dark: Boolean,
    variant: PaletteStyleOption,
    contrastLevel: Double,
): ColorScheme {
    val hct = Hct.fromInt(seedArgb.toInt())
    val contrast = contrastLevel.coerceIn(-1.0, 1.0)
    val scheme: DynamicScheme = when (variant) {
        PaletteStyleOption.TonalSpot -> SchemeTonalSpot(hct, dark, contrast)
        PaletteStyleOption.Neutral -> SchemeNeutral(hct, dark, contrast)
        PaletteStyleOption.Vibrant -> SchemeVibrant(hct, dark, contrast)
        PaletteStyleOption.Expressive -> SchemeExpressive(hct, dark, contrast)
        PaletteStyleOption.Rainbow -> SchemeRainbow(hct, dark, contrast)
        PaletteStyleOption.FruitSalad -> SchemeFruitSalad(hct, dark, contrast)
        PaletteStyleOption.Monochrome -> SchemeMonochrome(hct, dark, contrast)
        PaletteStyleOption.Fidelity -> SchemeFidelity(hct, dark, contrast)
        PaletteStyleOption.Content -> SchemeContent(hct, dark, contrast)
    }
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = scheme.primary.toColor(),
        onPrimary = scheme.onPrimary.toColor(),
        primaryContainer = scheme.primaryContainer.toColor(),
        onPrimaryContainer = scheme.onPrimaryContainer.toColor(),
        inversePrimary = scheme.inversePrimary.toColor(),
        secondary = scheme.secondary.toColor(),
        onSecondary = scheme.onSecondary.toColor(),
        secondaryContainer = scheme.secondaryContainer.toColor(),
        onSecondaryContainer = scheme.onSecondaryContainer.toColor(),
        tertiary = scheme.tertiary.toColor(),
        onTertiary = scheme.onTertiary.toColor(),
        tertiaryContainer = scheme.tertiaryContainer.toColor(),
        onTertiaryContainer = scheme.onTertiaryContainer.toColor(),
        error = scheme.error.toColor(),
        onError = scheme.onError.toColor(),
        errorContainer = scheme.errorContainer.toColor(),
        onErrorContainer = scheme.onErrorContainer.toColor(),
        background = scheme.background.toColor(),
        onBackground = scheme.onBackground.toColor(),
        surface = scheme.surface.toColor(),
        onSurface = scheme.onSurface.toColor(),
        surfaceVariant = scheme.surfaceVariant.toColor(),
        onSurfaceVariant = scheme.onSurfaceVariant.toColor(),
        surfaceTint = scheme.surfaceTint.toColor(),
        inverseSurface = scheme.inverseSurface.toColor(),
        inverseOnSurface = scheme.inverseOnSurface.toColor(),
        outline = scheme.outline.toColor(),
        outlineVariant = scheme.outlineVariant.toColor(),
        scrim = scheme.scrim.toColor(),
        surfaceBright = scheme.surfaceBright.toColor(),
        surfaceDim = scheme.surfaceDim.toColor(),
        surfaceContainer = scheme.surfaceContainer.toColor(),
        surfaceContainerHigh = scheme.surfaceContainerHigh.toColor(),
        surfaceContainerHighest = scheme.surfaceContainerHighest.toColor(),
        surfaceContainerLow = scheme.surfaceContainerLow.toColor(),
        surfaceContainerLowest = scheme.surfaceContainerLowest.toColor(),
    )
}

private fun Int.toColor(): Color = Color(this)
