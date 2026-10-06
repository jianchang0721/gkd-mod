package li.gkd.app.root

/**
 * Root 管理器类型.
 *
 * 只为把 "Root 不可用" 细分成用户能自己处理的诊断信息, 不参与任何权限提升逻辑.
 */
enum class RootManagerKind(val label: String) {
    Magisk("Magisk"),
    KernelSU("KernelSU"),
    KernelSUNext("KernelSU Next"),
    SukiSU("SukiSU Ultra"),
    APatch("APatch"),
    None("未安装"),
    ;

    val installed: Boolean get() = this != None
}

enum class RootAvailability {
    /** 尚未检测. */
    Unknown,

    /** su 可执行且 uid=0. */
    Authorized,

    /** su 存在但未授权 (含管理器弹窗未被确认/被拒绝). */
    NotAuthorized,

    /** 找不到任何可执行的 su (未 Root, 或 KernelSU 未授权且无 su 兼容路径). */
    SuMissing,
}

data class RootProbeResult(
    val availability: RootAvailability,
    val manager: RootManagerKind,
    /** 实际可执行的 su 路径/命令. */
    val suCommand: String? = null,
    val uid: Int? = null,
    /** su 的原始输出, 供用户复制反馈. */
    val detail: String = "",
)

/** 从 `id` 输出里取 uid, 例如 `uid=0(root) gid=0(root) groups=...`. */
fun parseUidFromIdOutput(text: String): Int? =
    Regex("""uid=(\d+)""").find(text)?.groupValues?.get(1)?.toIntOrNull()

/**
 * su 命令探测顺序.
 *
 * 优先使用 PATH 中的 `su`: Magisk 与 KernelSU 都会为已授权进程提供 `su`, 不能依赖固定路径。
 * 后面的绝对路径是各管理器的历史/兼容落地位置, 仅在 PATH 不可用时兜底。
 */
val suCandidates: List<String> = listOf(
    "su",
    "/system/bin/su",
    "/system/xbin/su",
    "/sbin/su",
    "/su/bin/su",
    "/debug_ramdisk/su",
    "/data/adb/ksu/bin/su",
    "/data/adb/ap/bin/su",
    "/magisk/.core/bin/su",
)

/** 管理器包名 -> 类型; 同一管理器的多个分支各有包名. */
val rootManagerPackages: List<Pair<String, RootManagerKind>> = listOf(
    "com.topjohnwu.magisk" to RootManagerKind.Magisk,
    "io.github.huskydg.magisk" to RootManagerKind.Magisk,
    "me.weishu.kernelsu" to RootManagerKind.KernelSU,
    "com.rifsxd.ksunext" to RootManagerKind.KernelSUNext,
    "com.sukisu.ultra" to RootManagerKind.SukiSU,
    "com.sukisu.ultra.debug" to RootManagerKind.SukiSU,
    "me.bmax.apatch" to RootManagerKind.APatch,
)

fun detectRootManager(installedPackages: Collection<String>): RootManagerKind {
    val installed = installedPackages.mapTo(HashSet()) { it.lowercase() }
    return rootManagerPackages.firstOrNull { it.first.lowercase() in installed }?.second
        ?: RootManagerKind.None
}

/**
 * 汇总一次 root 探测的结论.
 *
 * @param suStarted 是否成功启动过任何一个 su 进程 (即找到了可执行的 su)
 * @param uid su 返回的 uid
 */
fun classifyRootProbe(suStarted: Boolean, uid: Int?): RootAvailability = when {
    uid == 0 -> RootAvailability.Authorized
    !suStarted -> RootAvailability.SuMissing
    // su 能启动但不是 root: 授权弹窗未确认、被拒绝或授权已过期
    else -> RootAvailability.NotAuthorized
}
