package li.gkd.app.ui.style

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ColorMathTest {
    @Test
    fun parsesHexInSeveralForms() {
        assertEquals(0xFFFF0000L, parseHexColor("#FF0000"))
        assertEquals(0xFFFF0000L, parseHexColor("ff0000"))
        assertEquals(0xFFFF0000L, parseHexColor("  #FF0000 "))
        // 允许 8 位带 alpha
        assertEquals(0x80FF0000L, parseHexColor("#80FF0000"))
        assertNull(parseHexColor("#12345"))
        assertNull(parseHexColor("不合法"))
        assertNull(parseHexColor(""))
    }

    @Test
    fun formatsHexWithoutAlpha() {
        assertEquals("#FF0000", formatHexColor(0xFFFF0000L))
        assertEquals("#6750A4", formatHexColor(0xFF6750A4L))
        // alpha 不参与显示
        assertEquals("#00FF00", formatHexColor(0x0000FF00L))
    }

    @Test
    fun hsvRoundTripKeepsPrimaryColors() {
        for (argb in listOf(0xFFFF0000L, 0xFF00FF00L, 0xFF0000FFL, 0xFF6750A4L, 0xFFFFFFFFL, 0xFF000000L)) {
            val hsv = argbToHsv(argb)
            val back = hsvToArgb(hsv)
            // 纯黑/纯白的色相无意义, 只比较颜色分量
            assertEquals(
                argb and 0xFFFFFFL,
                back and 0xFFFFFFL,
                "round trip failed for " + formatHexColor(argb),
            )
        }
    }

    @Test
    fun hsvMatchesKnownValues() {
        val red = argbToHsv(0xFFFF0000L)
        assertEquals(0f, red.hue)
        assertEquals(1f, red.saturation)
        assertEquals(1f, red.value)

        val gray = argbToHsv(0xFF808080L)
        assertEquals(0f, gray.saturation)
        assertTrue(abs(gray.value - 128f / 255f) < 0.001f)

        val half = hsvToArgb(HsvColor(hue = 120f, saturation = 1f, value = 1f))
        assertEquals("#00FF00", formatHexColor(half))
    }

    @Test
    fun outOfRangeInputIsClamped() {
        val argb = hsvToArgb(HsvColor(hue = -30f, saturation = 2f, value = 2f))
        assertNotNull(argb)
        assertEquals(0xFFFFFFFFL, argb and 0xFFFFFFFFL)
    }
}
