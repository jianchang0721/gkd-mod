package li.gkd.app.ui.style

/**
 * 可切换的界面风格; 三者互斥, 同一时刻只有一套生效.
 *
 * 风格只影响呈现层, 不改变任何功能逻辑.
 */
enum class UiStyle(val value: Int) {
    /** 上游原版外观. */
    Default(0),

    /** iOS 风格连续曲率圆角 (com.kyant.shapes). */
    Shapes(1),

    /** 由主色生成的 M3 配色 (m3color). */
    M3Color(2),

    /** 液态玻璃 (io.github.kyant0:backdrop). */
    LiquidGlass(3),
    ;

    val isDefault: Boolean get() = this == Default

    companion object {
        fun fromValue(value: Int): UiStyle = entries.firstOrNull { it.value == value } ?: Default
    }
}

/** M3 配色方案; 具体映射到 m3color 的 Scheme* 类在 Android 侧完成. */
enum class PaletteStyleOption(val value: Int) {
    TonalSpot(0),
    Neutral(1),
    Vibrant(2),
    Expressive(3),
    Rainbow(4),
    FruitSalad(5),
    Monochrome(6),
    Fidelity(7),
    Content(8),
    ;

    companion object {
        fun fromValue(value: Int): PaletteStyleOption =
            entries.firstOrNull { it.value == value } ?: TonalSpot
    }
}
