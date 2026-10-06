package li.gkd.app.ui.home

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import li.gkd.app.ui.glass.LocalUiStyle
import li.gkd.app.ui.glass.gkGlassSurface
import li.gkd.app.ui.style.UiStyle
import li.gkd.app.ui.component.GkIcon
import li.gkd.app.ui.component.GkScaffold

// A desktop device profile supplies Android's bottom inset; Android uses native defaults.
val LocalHomeNavigationInsets = staticCompositionLocalOf<WindowInsets?> { null }

@Composable
fun GkHomeScaffold(
    page: ScaffoldExt,
    selectedTab: BottomNavItem,
    onSelectTab: (BottomNavItem) -> Unit
) {
    GkScaffold(
        modifier = page.modifier, topBar = page.topBar,
        floatingActionButton = page.floatingActionButton,
        bottomBar = {
            val glassMode = LocalUiStyle.current == UiStyle.LiquidGlass
            NavigationBar(
                windowInsets = LocalHomeNavigationInsets.current
                    ?: NavigationBarDefaults.windowInsets,
                containerColor =
                    if (glassMode) Color.Transparent else NavigationBarDefaults.containerColor,
                modifier =
                    if (glassMode) {
                        Modifier.gkGlassSurface(
                            shape = RectangleShape,
                            fallbackColor = MaterialTheme.colorScheme.surface,
                            blurRadius = 24.dp,
                            tintAlpha = 0.22f,
                        )
                    } else {
                        Modifier
                    },
            ) {
                BottomNavItem.allSubObjects.forEach { item ->
                    NavigationBarItem(
                        selected = item == selectedTab, onClick = { onSelectTab(item) },
                        icon = { GkIcon(item.icon, contentDescription = null) },
                        label = { Text(item.label) })
                }
            }
        },
        content = page.content,
    )
}
