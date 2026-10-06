package li.gkd.app.ui.style

import android.view.accessibility.AccessibilityManager
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.view.WindowInsetsControllerCompat
import li.gkd.app.app
import li.gkd.app.settings.SettingsRepository
import li.gkd.app.ui.glass.GkGlassRoot
import li.gkd.app.ui.glass.glassColorScheme
import li.gkd.app.ui.share.LocalDarkTheme
import li.gkd.app.ui.share.LocalIsTalkbackEnabled
import li.gkd.app.ui.theme.GkTheme
import li.gkd.app.ui.theme.rememberAppearance
import li.gkd.app.util.AndroidTarget

private val LightColorScheme = lightColorScheme()
private val DarkColorScheme = darkColorScheme()

@Composable
fun AppTheme(
    invertedTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val appearance by rememberAppearance()
    val darkTheme = appearance.isDark(isSystemInDarkTheme(), invertedTheme)
    val enableDynamicColor = appearance.dynamicColor
    val store by SettingsRepository.settings.collectAsStateWithLifecycle()
    val uiStyle = UiStyle.fromValue(store.uiStyle)
    val colorScheme = when {
        // M3 动态配色: 由主色生成整套配色, 优先于系统动态取色
        uiStyle == UiStyle.M3Color -> rememberM3ColorScheme(
            seedArgb = store.m3ColorSeed,
            dark = darkTheme,
            variant = PaletteStyleOption.fromValue(store.m3ColorVariant),
            contrastLevel = store.m3ColorContrast,
        )

        AndroidTarget.S && enableDynamicColor && darkTheme -> dynamicDarkColorScheme(app)
        AndroidTarget.S && enableDynamicColor && !darkTheme -> dynamicLightColorScheme(app)
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val styledColorScheme =
        if (uiStyle == UiStyle.LiquidGlass) glassColorScheme(colorScheme) else colorScheme
    val shapes = when (uiStyle) {
        // 形状风格与液态玻璃都用连续曲率圆角
        UiStyle.Shapes, UiStyle.LiquidGlass -> continuousCurvatureShapes()
        else -> Shapes()
    }

    val activity = LocalActivity.current
    if (activity != null) {
        LaunchedEffect(darkTheme) {
            // https://github.com/gkd-kit/gkd/pull/421
            WindowInsetsControllerCompat(activity.window, activity.window.decorView).apply {
                isAppearanceLightStatusBars = !darkTheme
            }
        }
        val bg = colorScheme.background.toArgb()
        LaunchedEffect(darkTheme, bg) {
            activity.window.decorView.setBackgroundColor(bg)
        }
    }

    var isTalkbackEnabled by remember { mutableStateOf(app.a11yManager.isTouchExplorationEnabled) }
    DisposableEffect(null) {
        val listener = AccessibilityManager.TouchExplorationStateChangeListener {
            isTalkbackEnabled = it
        }
        app.a11yManager.addTouchExplorationStateChangeListener(listener)
        onDispose {
            app.a11yManager.removeTouchExplorationStateChangeListener(listener)
        }
    }
    CompositionLocalProvider(
        LocalDarkTheme provides darkTheme,
        LocalIsTalkbackEnabled provides isTalkbackEnabled
    ) {
        GkTheme(
            colorScheme = styledColorScheme,
            shapes = shapes,
        ) {
            GkGlassRoot(style = uiStyle) {
                content()
            }
        }
    }
}
