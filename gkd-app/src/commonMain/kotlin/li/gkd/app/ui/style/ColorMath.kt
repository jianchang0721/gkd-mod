package li.gkd.app.ui.style

import kotlin.math.abs

/**
 * 颜色换算的无平台依赖实现, 便于在 JVM 上做单元测试.
 * 只处理 8 位 ARGB, 颜色值用 Long 承载以避免 Int 符号位问题.
 */
data class HsvColor(val hue: Float, val saturation: Float, val value: Float)

fun argbToHsv(argb: Long): HsvColor {
    val r = ((argb shr 16) and 0xFF).toFloat() / 255f
    val g = ((argb shr 8) and 0xFF).toFloat() / 255f
    val b = (argb and 0xFF).toFloat() / 255f
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min
    val hue = when {
        delta == 0f -> 0f
        max == r -> 60f * (((g - b) / delta) % 6f)
        max == g -> 60f * (((b - r) / delta) + 2f)
        else -> 60f * (((r - g) / delta) + 4f)
    }
    return HsvColor(
        hue = ((hue % 360f) + 360f) % 360f,
        saturation = if (max == 0f) 0f else delta / max,
        value = max,
    )
}

fun hsvToArgb(color: HsvColor): Long {
    val hue = ((color.hue % 360f) + 360f) % 360f
    val saturation = color.saturation.coerceIn(0f, 1f)
    val value = color.value.coerceIn(0f, 1f)
    val c = value * saturation
    val x = c * (1f - abs(((hue / 60f) % 2f) - 1f))
    val m = value - c
    val (r1, g1, b1) = when {
        hue < 60f -> Triple(c, x, 0f)
        hue < 120f -> Triple(x, c, 0f)
        hue < 180f -> Triple(0f, c, x)
        hue < 240f -> Triple(0f, x, c)
        hue < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    val r = ((r1 + m) * 255f + 0.5f).toInt().coerceIn(0, 255)
    val g = ((g1 + m) * 255f + 0.5f).toInt().coerceIn(0, 255)
    val b = ((b1 + m) * 255f + 0.5f).toInt().coerceIn(0, 255)
    return 0xFF000000L or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong()
}

/** 接受 `#RRGGBB` / `RRGGBB` / `#AARRGGBB`, 失败返回 null. */
fun parseHexColor(text: String): Long? {
    val raw = text.trim().removePrefix("#").removePrefix("0x").removePrefix("0X")
    val hex = when (raw.length) {
        6 -> "FF$raw"
        8 -> raw
        else -> return null
    }
    return hex.toLongOrNull(16)?.takeIf { it in 0..0xFFFFFFFFL }
}

fun formatHexColor(argb: Long): String {
    val rgb = argb and 0xFFFFFFL
    return "#" + rgb.toString(16).uppercase().padStart(6, '0')
}
