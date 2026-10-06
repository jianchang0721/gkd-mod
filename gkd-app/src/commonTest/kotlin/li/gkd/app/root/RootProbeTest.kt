package li.gkd.app.root

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RootProbeTest {
    @Test
    fun parsesUidFromIdOutput() {
        assertEquals(
            0,
            parseUidFromIdOutput("uid=0(root) gid=0(root) groups=0(root) context=u:r:su:s0"),
        )
        assertEquals(
            10123,
            parseUidFromIdOutput("uid=10123(u0_a123) gid=10123(u0_a123) groups=10123(u0_a123)"),
        )
        // KernelSU 拒绝授权时的输出没有 uid
        assertNull(parseUidFromIdOutput("su: permission denied"))
        assertNull(parseUidFromIdOutput(""))
    }

    @Test
    fun detectsInstalledRootManager() {
        assertEquals(
            RootManagerKind.Magisk,
            detectRootManager(listOf("com.topjohnwu.magisk", "com.android.shell")),
        )
        assertEquals(RootManagerKind.KernelSU, detectRootManager(listOf("me.weishu.kernelsu")))
        assertEquals(RootManagerKind.KernelSUNext, detectRootManager(listOf("com.rifsxd.ksunext")))
        assertEquals(RootManagerKind.SukiSU, detectRootManager(listOf("com.sukisu.ultra")))
        assertEquals(RootManagerKind.APatch, detectRootManager(listOf("me.bmax.apatch")))
        assertEquals(RootManagerKind.None, detectRootManager(listOf("com.android.settings")))
        // 大小写不敏感
        assertEquals(RootManagerKind.Magisk, detectRootManager(listOf("com.TopJohnWu.Magisk")))
    }

    @Test
    fun classifiesRootProbeResult() {
        assertEquals(RootAvailability.Authorized, classifyRootProbe(suStarted = true, uid = 0))
        assertEquals(RootAvailability.NotAuthorized, classifyRootProbe(suStarted = true, uid = null))
        assertEquals(RootAvailability.NotAuthorized, classifyRootProbe(suStarted = true, uid = 2000))
        assertEquals(RootAvailability.SuMissing, classifyRootProbe(suStarted = false, uid = null))
    }

    @Test
    fun suCandidatesPreferPathLookupAndAreUnique() {
        // 优先用 PATH 中的 su: Magisk/KernelSU 都会给已授权进程提供 su, 不能依赖固定路径
        assertEquals("su", suCandidates.first())
        assertEquals(suCandidates.size, suCandidates.distinct().size)
        assertEquals(suCandidates.size, suCandidates.map { it.lowercase() }.distinct().size)
    }
}
