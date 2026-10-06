package li.gkd.app.root

import android.os.Process
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import li.gkd.app.app
import li.gkd.app.appScope
import li.gkd.app.permission.AndroidPermissions
import li.gkd.app.priv.PrivilegeOwnerLifecycle
import li.gkd.app.service.A11yService
import li.gkd.app.settings.SettingsRepository
import li.gkd.app.settings.SettingsStore
import li.gkd.app.util.LogUtils
import li.gkd.app.util.launchLogged

enum class A11yGuardState {
    /** 总开关未开启. */
    Disabled,

    /** 总开关已开启, 但特权服务未连接 (没有 WRITE_SECURE_SETTINGS). */
    WaitingPrivilege,

    /** 特权已就绪, 无障碍服务尚未连接. */
    WaitingA11yService,

    /** 正常运行. */
    Running,

    /** 自动化模式下无障碍会被主动关闭, 保障自动跳过. */
    SkippedAutomation,

    /** 判定假死, 正在重启自身. */
    Restarting,
}

/**
 * 无障碍保障: 只用特权服务(Root/ADB/Shizuku)给的 WRITE_SECURE_SETTINGS 开关无障碍服务.
 *
 * 设计约束 (用户要求): Root 仅辅助无障碍, 不参与点击注入, 不申请额外权限.
 *
 * 行为:
 * - 总开关打开时立即确保无障碍服务开启;
 * - 无障碍服务由 "开启" 变为 "关闭" 时自动恢复 (可在设置里关掉);
 * - 无障碍服务在系统里是开启状态但长时间未连接时判定假死, 重启自身以重绑服务;
 * - 开机后按设置自动恢复.
 *
 * 所有自动动作都有频次上限, 避免与用户手动关闭形成反复拉锯.
 */
object A11yGuard {
    private const val TICK_MILLIS = 5_000L

    /** 系统重绑无障碍服务需要时间, 超过该窗口仍未连接才判定假死. */
    private const val NOT_RUNNING_GRACE_MILLIS = 25_000L
    private const val ACTION_WINDOW_MILLIS = 10 * 60 * 1000L
    private const val MAX_AUTO_ACTIONS_PER_WINDOW = 3
    private const val ACCESSIBILITY_ENABLED = "accessibility_enabled"

    private val mutableState = MutableStateFlow(A11yGuardState.Disabled)
    val state: StateFlow<A11yGuardState> = mutableState.asStateFlow()

    private var started = false
    private var lastMasterEnabled = false
    private var lastA11yEnabled: Boolean? = null
    private var notRunningSince: Long? = null
    private val autoRestoreTimestamps = ArrayDeque<Long>()
    private val autoRestartTimestamps = ArrayDeque<Long>()

    /** 由 Application.onCreate 调用一次. */
    fun start() {
        if (started) return
        started = true
        appScope.launchLogged(Dispatchers.Default) {
            while (true) {
                delay(TICK_MILLIS)
                runCatching { tick() }
                    .onFailure { LogUtils.d("a11y guard tick failed", it) }
            }
        }
    }

    /** 开机广播后调用; 特权服务由 priv-kit 的静默恢复负责重连. */
    fun onBoot() {
        appScope.launchLogged(Dispatchers.Default) {
            delay(6_000L)
            val settings = SettingsRepository.settings.value
            if (!settings.a11yGuardEnabled || !settings.a11yGuardRestoreOnBoot) return@launchLogged
            if (isAutomationMode(settings)) return@launchLogged
            if (!hasWriteSecureSettings()) return@launchLogged
            val ok = enableA11yService()
            LogUtils.d("a11y guard boot restore=$ok")
        }
    }

    fun isAutomationMode(settings: SettingsStore): Boolean =
        settings.enableAutomator && settings.useAutomation

    fun hasWriteSecureSettings(): Boolean =
        app.checkGrantedPermission(AndroidPermissions.WRITE_SECURE_SETTINGS)

    fun isA11yServiceEnabledInSystem(): Boolean =
        app.getSecureA11yServices().contains(A11yService.a11yCn)

    /**
     * 通过安全设置开启 GKD 的无障碍服务.
     *
     * 需要特权服务已连接并授予 WRITE_SECURE_SETTINGS, 否则写入会静默失败.
     */
    suspend fun enableA11yService(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val services = app.getSecureA11yServices()
            if (A11yService.a11yCn !in services) {
                services += A11yService.a11yCn
                app.putSecureA11yServices(services)
            }
            app.putSecureInt(ACCESSIBILITY_ENABLED, 1)
        }.onFailure { LogUtils.d("enable a11y service failed", it) }
        isA11yServiceEnabledInSystem()
    }

    /** 重启自身: 让系统重新绑定无障碍服务. */
    suspend fun restartApp(reason: String) {
        LogUtils.d("a11y guard restart app: $reason")
        runCatching { app.startLaunchActivity() }
        runCatching { PrivilegeOwnerLifecycle.prepareAppRestart() }
        delay(800L)
        Process.killProcess(Process.myPid())
    }

    private suspend fun tick() {
        val settings = SettingsRepository.settings.value
        if (!settings.a11yGuardEnabled) {
            lastMasterEnabled = false
            lastA11yEnabled = null
            notRunningSince = null
            mutableState.value = A11yGuardState.Disabled
            return
        }
        if (isAutomationMode(settings)) {
            lastMasterEnabled = true
            notRunningSince = null
            mutableState.value = A11yGuardState.SkippedAutomation
            return
        }
        if (!hasWriteSecureSettings()) {
            lastMasterEnabled = true
            mutableState.value = A11yGuardState.WaitingPrivilege
            return
        }
        if (!lastMasterEnabled) {
            lastMasterEnabled = true
            val ok = enableA11yService()
            LogUtils.d("a11y guard turned on, a11y service enabled=$ok")
        }
        val enabledInSystem = isA11yServiceEnabledInSystem()
        val running = A11yService.isRunning.value
        when {
            enabledInSystem && running -> {
                notRunningSince = null
                lastA11yEnabled = true
                mutableState.value = A11yGuardState.Running
            }

            enabledInSystem -> {
                val since = notRunningSince ?: System.currentTimeMillis().also { notRunningSince = it }
                val dead = System.currentTimeMillis() - since > NOT_RUNNING_GRACE_MILLIS
                if (
                    dead &&
                    settings.a11yGuardRestartOnDead &&
                    allowAutoAction(autoRestartTimestamps, "restart")
                ) {
                    mutableState.value = A11yGuardState.Restarting
                    restartApp("a11y-not-running")
                } else {
                    mutableState.value = A11yGuardState.WaitingA11yService
                }
            }

            else -> {
                notRunningSince = null
                val wasEnabled = lastA11yEnabled == true
                lastA11yEnabled = false
                if (
                    wasEnabled &&
                    settings.a11yGuardAutoRestore &&
                    allowAutoAction(autoRestoreTimestamps, "restore")
                ) {
                    val ok = enableA11yService()
                    LogUtils.d("a11y service was turned off, restored=$ok")
                }
                mutableState.value = A11yGuardState.WaitingA11yService
            }
        }
    }

    /** 同一时间窗内的自动动作次数上限, 超过后等待用户手动处理. */
    private fun allowAutoAction(timestamps: ArrayDeque<Long>, kind: String): Boolean {
        val now = System.currentTimeMillis()
        while (timestamps.isNotEmpty() && now - timestamps.first() > ACTION_WINDOW_MILLIS) {
            timestamps.removeFirst()
        }
        if (timestamps.size >= MAX_AUTO_ACTIONS_PER_WINDOW) {
            LogUtils.d("a11y guard auto $kind limited, waiting for manual action")
            return false
        }
        timestamps.addLast(now)
        return true
    }
}
