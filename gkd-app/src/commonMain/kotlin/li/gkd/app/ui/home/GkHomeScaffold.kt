package li.gkd.app.ui.home

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
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
            GkFloatingCapsuleNav(
                selectedTab = selectedTab,
                onSelectTab = onSelectTab,
            )
        },
        content = page.content,
    )
}
