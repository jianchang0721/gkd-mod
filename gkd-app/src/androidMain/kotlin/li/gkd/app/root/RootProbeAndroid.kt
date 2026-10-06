package li.gkd.app.root

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import li.gkd.app.app
import li.gkd.app.util.LogUtils
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * 首次授权时 Root 管理器会弹窗等待用户点击, 官方实现只等 3 秒, 很容易判成 "Root 不可用".
 * 这里给足窗口 (默认 30 秒), 由调用方提供可取消的 UI.
 */
const val DEFAULT_ROOT_PROBE_TIMEOUT_MILLIS = 30_000L

/** 通过包名判断安装了哪个 Root 管理器, 不依赖 /data/adb 是否可读. */
fun detectInstalledRootManager(): RootManagerKind = runCatching {
    val packages = app.packageManager.getInstalledPackages(0).map { it.packageName }
    detectRootManager(packages)
}.getOrDefault(RootManagerKind.None)

private data class SuAttempt(
    val command: String,
    val started: Boolean,
    val uid: Int? = null,
    val timedOut: Boolean = false,
    val output: String = "",
)

private fun probeSuCommand(command: String, waitMillis: Long): SuAttempt {
    val process = try {
        ProcessBuilder(command, "-c", "id").redirectErrorStream(true).start()
    } catch (e: Exception) {
        return SuAttempt(command = command, started = false, output = e.message.orEmpty())
    }
    val output = StringBuilder()
    val reader = thread(isDaemon = true, name = "gkd-root-probe") {
        runCatching {
            process.inputStream.bufferedReader().useLines { lines ->
                lines.forEach { output.appendLine(it) }
            }
        }
    }
    val finished = runCatching { process.waitFor(waitMillis, TimeUnit.MILLISECONDS) }
        .getOrDefault(false)
    if (!finished) {
        // 还在等用户点授权弹窗; 结束进程不会留下副作用
        runCatching { process.destroy() }
        reader.interrupt()
        return SuAttempt(
            command = command,
            started = true,
            timedOut = true,
            output = output.toString(),
        )
    }
    val text = output.toString()
    return SuAttempt(
        command = command,
        started = true,
        uid = parseUidFromIdOutput(text),
        output = text,
    )
}

/**
 * 依次尝试候选 su, 找到第一个能执行的即停止 (避免反复弹授权窗).
 */
suspend fun probeRootAuthorization(
    waitMillis: Long = DEFAULT_ROOT_PROBE_TIMEOUT_MILLIS,
): RootProbeResult = withContext(Dispatchers.IO) {
    val manager = detectInstalledRootManager()
    var anyStarted = false
    var timedOut = false
    var lastDetail = ""
    try {
        for (command in suCandidates) {
            val attempt = probeSuCommand(command, waitMillis)
            lastDetail = attempt.output.trim()
            if (attempt.uid == 0) {
                LogUtils.d("root probe ok: ${attempt.command}")
                return@withContext RootProbeResult(
                    availability = RootAvailability.Authorized,
                    manager = manager,
                    suCommand = attempt.command,
                    uid = 0,
                    detail = lastDetail,
                )
            }
            if (attempt.started) {
                anyStarted = true
                timedOut = attempt.timedOut
                break
            }
        }
    } catch (e: Exception) {
        LogUtils.d("root probe failed", e)
        lastDetail = e.message.orEmpty()
    }
    RootProbeResult(
        availability = classifyRootProbe(anyStarted, null),
        manager = manager,
        suCommand = null,
        uid = null,
        detail = if (timedOut && lastDetail.isBlank()) "su probe timed out" else lastDetail,
    )
}
