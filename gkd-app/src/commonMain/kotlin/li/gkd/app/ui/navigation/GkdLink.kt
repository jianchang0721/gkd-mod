package li.gkd.app.ui.navigation

import java.net.URI
import java.net.URLDecoder

sealed interface GkdLink {
    data class Home(val tab: Int?) : GkdLink
    data class Page(val route: AppRoute) : GkdLink
    data object WeChatScanner : GkdLink

    companion object {
        /** 官方 GKD 使用 gkd://, 修改版额外注册 gkdmod:// 以便与官方版共存时互不抢占. */
        private val schemes = setOf("gkd", "gkdmod")

        fun parse(value: String): GkdLink? = runCatching {
            val uri = URI(value)
            if (uri.scheme?.lowercase() !in schemes) null
            else when (uri.host) {
                "page" -> when (uri.path.orEmpty()) {
                    "" -> Home(
                        uri.rawQuery?.split('&')?.firstOrNull { it.substringBefore('=') == "tab" }
                            ?.substringAfter('=', "")
                            ?.let { URLDecoder.decode(it, "UTF-8").toIntOrNull() })

                    "/1" -> Page(AdvancedPageRoute)
                    "/2" -> Page(SnapshotPageRoute)
                    "/3", "/4" -> Page(PrivilegeServiceRoute)
                    else -> null
                }

                "invoke" -> if (uri.path == "/1") WeChatScanner else null
                else -> null
            }
        }.getOrNull()
    }
}
