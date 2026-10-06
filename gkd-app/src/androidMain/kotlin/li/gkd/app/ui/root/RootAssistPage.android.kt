package li.gkd.app.ui.root

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import li.gkd.app.a11y.useEnabledA11yServicesFlow
import li.gkd.app.app
import li.gkd.app.permission.AndroidPermissions
import li.gkd.app.priv.privilegeContextFlow
import li.gkd.app.resources.Res
import li.gkd.app.resources.a11y_guard
import li.gkd.app.resources.a11y_guard_a11y_enable_failed
import li.gkd.app.resources.a11y_guard_a11y_enable_success
import li.gkd.app.resources.a11y_guard_a11y_not_running
import li.gkd.app.resources.a11y_guard_a11y_running
import li.gkd.app.resources.a11y_guard_a11y_state
import li.gkd.app.resources.a11y_guard_actions
import li.gkd.app.resources.a11y_guard_auto_restore
import li.gkd.app.resources.a11y_guard_automation_notice
import li.gkd.app.resources.a11y_guard_copied
import li.gkd.app.resources.a11y_guard_description
import li.gkd.app.resources.a11y_guard_detect_hint
import li.gkd.app.resources.a11y_guard_detect_root
import li.gkd.app.resources.a11y_guard_detecting
import li.gkd.app.resources.a11y_guard_enable
import li.gkd.app.resources.a11y_guard_enable_a11y
import li.gkd.app.resources.a11y_guard_enable_description
import li.gkd.app.resources.a11y_guard_granted
import li.gkd.app.resources.a11y_guard_not_granted
import li.gkd.app.resources.a11y_guard_privilege_connected
import li.gkd.app.resources.a11y_guard_privilege_disconnected
import li.gkd.app.resources.a11y_guard_privilege_start_failed
import li.gkd.app.resources.a11y_guard_privilege_start_success
import li.gkd.app.resources.a11y_guard_privilege_state
import li.gkd.app.resources.a11y_guard_restart_app
import li.gkd.app.resources.a11y_guard_restart_on_dead
import li.gkd.app.resources.a11y_guard_restarting_app
import li.gkd.app.resources.a11y_guard_restore_on_boot
import li.gkd.app.resources.a11y_guard_root_authorized
import li.gkd.app.resources.a11y_guard_root_not_authorized
import li.gkd.app.resources.a11y_guard_root_state
import li.gkd.app.resources.a11y_guard_root_su_missing
import li.gkd.app.resources.a11y_guard_root_unknown
import li.gkd.app.resources.a11y_guard_start_privilege_root
import li.gkd.app.resources.a11y_guard_state
import li.gkd.app.resources.a11y_guard_state_disabled
import li.gkd.app.resources.a11y_guard_state_restarting
import li.gkd.app.resources.a11y_guard_state_running
import li.gkd.app.resources.a11y_guard_state_skipped_automation
import li.gkd.app.resources.a11y_guard_state_waiting_a11y
import li.gkd.app.resources.a11y_guard_state_waiting_privilege
import li.gkd.app.resources.a11y_guard_write_secure_state
import li.gkd.app.resources.turned_off
import li.gkd.app.resources.turned_on
import li.gkd.app.root.A11yGuard
import li.gkd.app.root.A11yGuardState
import li.gkd.app.root.DEFAULT_ROOT_PROBE_TIMEOUT_MILLIS
import li.gkd.app.root.RootAvailability
import li.gkd.app.root.RootProbeResult
import li.gkd.app.root.probeRootAuthorization
import li.gkd.app.service.A11yService
import li.gkd.app.settings.SettingsRepository
import li.gkd.app.ui.MainViewModel
import li.gkd.app.ui.component.GkCopyTextCard
import li.gkd.app.ui.component.GkIconButton
import li.gkd.app.ui.component.GkIcons
import li.gkd.app.ui.component.GkPageBottomSpace
import li.gkd.app.ui.component.GkScaffold
import li.gkd.app.ui.component.GkSettingItem
import li.gkd.app.ui.component.GkTextSwitch
import li.gkd.app.ui.component.GkTopAppBar
import li.gkd.app.ui.component.rememberColumnScrollState
import li.gkd.app.ui.navigation.AppWindow
import li.gkd.app.ui.style.titleItemPadding
import li.gkd.app.ui.text.getSync
import li.gkd.app.util.ToastUtils
import org.jetbrains.compose.resources.stringResource
import priv.kit.core.Privilege

@Composable
actual fun RootAssistPage(window: AppWindow) {
    val mainVm = MainViewModel.requireCurrent()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val store by SettingsRepository.settings.collectAsStateWithLifecycle()
    val privilegeConnected = privilegeContextFlow.collectAsStateWithLifecycle().value != null
    val enabledServices by useEnabledA11yServicesFlow(scope).collectAsStateWithLifecycle()
    val a11yRunning by A11yService.isRunning.collectAsStateWithLifecycle()
    val guardState by A11yGuard.state.collectAsStateWithLifecycle()
    var probeResult by remember { mutableStateOf<RootProbeResult?>(null) }
    var busy by remember { mutableStateOf(false) }

    val writeSecureSettings = app.checkGrantedPermission(AndroidPermissions.WRITE_SECURE_SETTINGS)
    val a11yEnabled = A11yService.a11yCn in enabledServices
    val scroll = rememberColumnScrollState()
    val detectHint = stringResource(
        Res.string.a11y_guard_detect_hint,
        (DEFAULT_ROOT_PROBE_TIMEOUT_MILLIS / 1000L).toString(),
    )

    fun toast(text: String) = ToastUtils.show(text, forced = true)

    val guardStateText = when (guardState) {
        A11yGuardState.Disabled -> stringResource(Res.string.a11y_guard_state_disabled)
        A11yGuardState.WaitingPrivilege -> stringResource(Res.string.a11y_guard_state_waiting_privilege)
        A11yGuardState.WaitingA11yService -> stringResource(Res.string.a11y_guard_state_waiting_a11y)
        A11yGuardState.Running -> stringResource(Res.string.a11y_guard_state_running)
        A11yGuardState.SkippedAutomation -> stringResource(Res.string.a11y_guard_state_skipped_automation)
        A11yGuardState.Restarting -> stringResource(Res.string.a11y_guard_state_restarting)
    }
    val managerLabel = probeResult?.manager?.label.orEmpty()
    val rootStateText = when (probeResult?.availability) {
        RootAvailability.Authorized ->
            stringResource(Res.string.a11y_guard_root_authorized, managerLabel)

        RootAvailability.NotAuthorized ->
            stringResource(Res.string.a11y_guard_root_not_authorized, managerLabel)

        RootAvailability.SuMissing -> stringResource(Res.string.a11y_guard_root_su_missing)
        else -> stringResource(Res.string.a11y_guard_root_unknown)
    }
    val a11yStateText = listOf(
        if (a11yRunning) stringResource(Res.string.a11y_guard_a11y_running)
        else stringResource(Res.string.a11y_guard_a11y_not_running),
        if (a11yEnabled) stringResource(Res.string.turned_on)
        else stringResource(Res.string.turned_off),
    ).joinToString(" · ")

    fun runProbe(thenStartPrivilege: Boolean) {
        if (busy) return
        scope.launch {
            busy = true
            try {
                val result = probeRootAuthorization(DEFAULT_ROOT_PROBE_TIMEOUT_MILLIS)
                probeResult = result
                if (!thenStartPrivilege) return@launch
                if (result.availability != RootAvailability.Authorized) return@launch
                // 先用自己的长窗口拿到授权, 管理器会缓存授权, 再交给 priv-kit 启动
                runCatching { Privilege.startRoot() }
                    .onSuccess { toast(Res.string.a11y_guard_privilege_start_success.getSync()) }
                    .onFailure { e ->
                        toast(
                            Res.string.a11y_guard_privilege_start_failed.getSync(
                                e.message ?: e.toString(),
                            ),
                        )
                    }
            } finally {
                busy = false
            }
        }
    }

    GkScaffold(
        modifier = Modifier.nestedScroll(scroll.scrollBehavior.nestedScrollConnection),
        topBar = {
            GkTopAppBar(
                scrollBehavior = scroll.scrollBehavior,
                navigationIcon = {
                    GkIconButton(
                        imageVector = GkIcons.ArrowBack,
                        onClick = { mainVm.popPage() },
                    )
                },
                title = { Text(text = stringResource(Res.string.a11y_guard)) },
            )
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll.scrollState)
                .padding(contentPadding),
        ) {
            Text(
                text = stringResource(Res.string.a11y_guard_description),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                text = stringResource(Res.string.a11y_guard_state),
                modifier = Modifier.titleItemPadding(showTop = false),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            GkSettingItem(
                title = stringResource(Res.string.a11y_guard_root_state),
                subtitle = rootStateText,
                imageVector = null,
            )
            GkSettingItem(
                title = stringResource(Res.string.a11y_guard_privilege_state),
                subtitle = if (privilegeConnected) {
                    stringResource(Res.string.a11y_guard_privilege_connected)
                } else {
                    stringResource(Res.string.a11y_guard_privilege_disconnected)
                },
                imageVector = null,
            )
            GkSettingItem(
                title = stringResource(Res.string.a11y_guard_a11y_state),
                subtitle = a11yStateText,
                imageVector = null,
            )
            GkSettingItem(
                title = stringResource(Res.string.a11y_guard_write_secure_state),
                subtitle = if (writeSecureSettings) {
                    stringResource(Res.string.a11y_guard_granted)
                } else {
                    stringResource(Res.string.a11y_guard_not_granted)
                },
                imageVector = null,
            )
            GkSettingItem(
                title = stringResource(Res.string.a11y_guard_state),
                subtitle = guardStateText,
                imageVector = null,
            )

            if (!probeResult?.detail.isNullOrBlank()) {
                GkCopyTextCard(
                    text = probeResult?.detail.orEmpty(),
                    onCopy = { text ->
                        val manager =
                            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        manager.setPrimaryClip(ClipData.newPlainText("gkd-root-probe", text))
                        toast(Res.string.a11y_guard_copied.getSync())
                    },
                )
            }

            Text(
                text = stringResource(Res.string.a11y_guard_actions),
                modifier = Modifier.titleItemPadding(),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            GkSettingItem(
                title = stringResource(Res.string.a11y_guard_detect_root),
                subtitle = if (busy) stringResource(Res.string.a11y_guard_detecting) else detectHint,
                onClick = if (busy) null else ({ runProbe(thenStartPrivilege = false) }),
            )
            GkSettingItem(
                title = stringResource(Res.string.a11y_guard_start_privilege_root),
                subtitle = if (busy) stringResource(Res.string.a11y_guard_detecting) else detectHint,
                onClick = if (busy) null else ({ runProbe(thenStartPrivilege = true) }),
            )
            GkSettingItem(
                title = stringResource(Res.string.a11y_guard_enable_a11y),
                onClick = {
                    scope.launch {
                        val ok = A11yGuard.enableA11yService()
                        toast(
                            if (ok) Res.string.a11y_guard_a11y_enable_success.getSync()
                            else Res.string.a11y_guard_a11y_enable_failed.getSync(),
                        )
                    }
                },
            )
            GkSettingItem(
                title = stringResource(Res.string.a11y_guard_restart_app),
                onClick = {
                    toast(Res.string.a11y_guard_restarting_app.getSync())
                    scope.launch { A11yGuard.restartApp("manual") }
                },
            )

            Text(
                text = stringResource(Res.string.a11y_guard_enable),
                modifier = Modifier.titleItemPadding(),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            GkTextSwitch(
                title = stringResource(Res.string.a11y_guard_enable),
                subtitle = stringResource(Res.string.a11y_guard_enable_description),
                checked = store.a11yGuardEnabled,
                onCheckedChange = { enabled ->
                    SettingsRepository.updateSettings { it.copy(a11yGuardEnabled = enabled) }
                },
            )
            GkTextSwitch(
                title = stringResource(Res.string.a11y_guard_auto_restore),
                checked = store.a11yGuardAutoRestore,
                enabled = store.a11yGuardEnabled,
                onCheckedChange = { enabled ->
                    SettingsRepository.updateSettings { it.copy(a11yGuardAutoRestore = enabled) }
                },
            )
            GkTextSwitch(
                title = stringResource(Res.string.a11y_guard_restart_on_dead),
                checked = store.a11yGuardRestartOnDead,
                enabled = store.a11yGuardEnabled,
                onCheckedChange = { enabled ->
                    SettingsRepository.updateSettings { it.copy(a11yGuardRestartOnDead = enabled) }
                },
            )
            GkTextSwitch(
                title = stringResource(Res.string.a11y_guard_restore_on_boot),
                checked = store.a11yGuardRestoreOnBoot,
                enabled = store.a11yGuardEnabled,
                onCheckedChange = { enabled ->
                    SettingsRepository.updateSettings { it.copy(a11yGuardRestoreOnBoot = enabled) }
                },
            )
            Text(
                text = stringResource(Res.string.a11y_guard_automation_notice),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            GkPageBottomSpace()
        }
    }
}
