package li.gkd.app.ui.root

import androidx.compose.runtime.Composable
import li.gkd.app.ui.navigation.AppWindow

/** Root 辅助 / 无障碍保障页面. Android 上是完整功能页, 桌面端只是占位. */
@Composable
expect fun RootAssistPage(window: AppWindow)
