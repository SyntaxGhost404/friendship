package app.nearfolk.feature.settings

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.nearfolk.core.designsystem.component.NfButton
import app.nearfolk.core.designsystem.component.NfButtonSize
import app.nearfolk.core.designsystem.component.NfConfirmDialog
import app.nearfolk.core.designsystem.component.NfDivider
import app.nearfolk.core.designsystem.component.NfGroup
import app.nearfolk.core.designsystem.component.NfLargeHeader
import app.nearfolk.core.designsystem.component.NfListRow
import app.nearfolk.core.designsystem.component.NfNotice
import app.nearfolk.core.designsystem.component.NfNoticeTone
import app.nearfolk.core.designsystem.component.NfSectionHeader
import app.nearfolk.core.designsystem.component.NfSegmented
import app.nearfolk.core.designsystem.component.NfSwitchRow
import app.nearfolk.core.designsystem.theme.Nf
import app.nearfolk.core.model.PlusPlan
import app.nearfolk.core.model.ThemeMode
import app.nearfolk.core.ui.AppDocument
import app.nearfolk.core.ui.LocalAppInfo
import app.nearfolk.core.ui.rememberTimeFormat
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable data object SettingsRoute
@Serializable data object PlusRoute
@Serializable data object BlockedRoute
@Serializable data object DeleteAccountRoute
@Serializable data object BugReportRoute
@Serializable data class DocumentRoute(val document: String)

enum class PasscodeAction { Create, Change, Remove }

@Composable
fun SettingsRouteScreen(
    onOpenPlus: () -> Unit,
    onOpenBlocked: () -> Unit,
    onOpenDelete: () -> Unit,
    onOpenBugReport: () -> Unit,
    onOpenDocument: (AppDocument) -> Unit,
    onPasscode: (PasscodeAction) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val info = LocalAppInfo.current
    val time = rememberTimeFormat()
    var confirmDeactivate by rememberSaveable { mutableStateOf(false) }
    var confirmSignOut by rememberSaveable { mutableStateOf(false) }
    var systemNotificationsOn by remember { mutableStateOf(true) }
    LifecycleResumeEffect(Unit) {
        systemNotificationsOn = NotificationManagerCompat.from(context).areNotificationsEnabled()
        onPauseOrDispose { }
    }
    var pendingToggle by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        systemNotificationsOn = granted
        pendingToggle?.invoke(true)
        pendingToggle = null
    }
    fun enableNotifications(setter: (Boolean) -> Unit, value: Boolean) {
        val needsPermission = value && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            pendingToggle = setter
            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            setter(value)
        }
    }
    val prefs = state.preferences

    Column(Modifier.fillMaxSize().background(Nf.colors.canvas)) {
        NfLargeHeader(stringResource(R.string.settings_title))
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 32.dp)) {
            NfSectionHeader(stringResource(R.string.settings_plan))
            NfGroup {
                val plus = state.plus
                NfListRow(
                    stringResource(R.string.settings_plus),
                    subtitle = if (plus.active && plus.plan != null && plus.renewsOnEpochDay != null) {
                        stringResource(
                            R.string.settings_plus_active,
                            stringResource(if (plus.plan == PlusPlan.Yearly) R.string.plus_yearly else R.string.plus_monthly),
                            time.shortDate(LocalDate.ofEpochDay(plus.renewsOnEpochDay!!)),
                        )
                    } else {
                        stringResource(R.string.settings_plus_inactive)
                    },
                    icon = Icons.Rounded.WorkspacePremium,
                    iconTint = Nf.colors.accent,
                    onClick = onOpenPlus,
                )
            }

            NfSectionHeader(stringResource(R.string.settings_appearance))
            NfSegmented(
                options = listOf(
                    stringResource(R.string.settings_theme_system),
                    stringResource(R.string.settings_theme_light),
                    stringResource(R.string.settings_theme_dark),
                ),
                selectedIndex = ThemeMode.entries.indexOf(prefs.themeMode),
                onSelect = { viewModel.setTheme(ThemeMode.entries[it]) },
            )
            Spacer(Modifier.height(10.dp))
            NfGroup {
                NfSwitchRow(
                    stringResource(R.string.settings_pure_black),
                    prefs.pureBlack,
                    { viewModel.setPureBlack(it) },
                    subtitle = stringResource(R.string.settings_pure_black_body),
                    icon = Icons.Rounded.DarkMode,
                    enabled = prefs.themeMode != ThemeMode.Light,
                )
            }

            NfSectionHeader(stringResource(R.string.settings_notifications))
            if (!systemNotificationsOn && (prefs.notifyMessages || prefs.notifyActivity)) {
                NfNotice(
                    stringResource(R.string.settings_notify_blocked),
                    tone = NfNoticeTone.Accent,
                    modifier = Modifier.padding(bottom = 10.dp),
                    action = {
                        NfButton(stringResource(R.string.settings_notify_open), { openNotificationSettings(context) }, size = NfButtonSize.Small)
                    },
                )
            }
            NfGroup {
                NfSwitchRow(
                    stringResource(R.string.settings_notify_messages),
                    prefs.notifyMessages,
                    { enableNotifications({ v -> viewModel.setNotifyMessages(v) }, it) },
                    subtitle = stringResource(R.string.settings_notify_messages_body),
                    icon = Icons.Rounded.Notifications,
                )
                NfDivider(inset = true)
                NfSwitchRow(
                    stringResource(R.string.settings_notify_activity),
                    prefs.notifyActivity,
                    { enableNotifications({ v -> viewModel.setNotifyActivity(v) }, it) },
                    subtitle = stringResource(R.string.settings_notify_activity_body),
                    icon = Icons.Rounded.Favorite,
                )
            }

            NfSectionHeader(stringResource(R.string.settings_feel))
            NfGroup {
                NfSwitchRow(
                    stringResource(R.string.settings_haptics),
                    prefs.haptics,
                    { viewModel.setHaptics(it) },
                    subtitle = stringResource(R.string.settings_haptics_body),
                    icon = Icons.Rounded.Vibration,
                )
            }

            NfSectionHeader(stringResource(R.string.settings_privacy))
            NfGroup {
                if (state.passcodeSet) {
                    NfListRow(stringResource(R.string.settings_passcode_change), subtitle = stringResource(R.string.settings_passcode_on), icon = Icons.Rounded.Lock, onClick = { onPasscode(PasscodeAction.Change) })
                    NfDivider(inset = true)
                    NfListRow(stringResource(R.string.settings_passcode_remove), icon = Icons.Rounded.Lock, onClick = { onPasscode(PasscodeAction.Remove) })
                } else {
                    NfListRow(stringResource(R.string.settings_passcode), subtitle = stringResource(R.string.settings_passcode_off), icon = Icons.Rounded.Lock, onClick = { onPasscode(PasscodeAction.Create) })
                }
                NfDivider(inset = true)
                NfListRow(
                    stringResource(R.string.settings_blocked),
                    subtitle = if (state.blockedCount > 0) pluralStringResource(R.plurals.settings_blocked_count, state.blockedCount, state.blockedCount) else stringResource(R.string.settings_blocked_none),
                    icon = Icons.Rounded.Block,
                    onClick = onOpenBlocked,
                )
            }

            NfSectionHeader(stringResource(R.string.settings_support))
            NfGroup {
                NfListRow(stringResource(R.string.settings_contact), icon = Icons.Rounded.Mail, onClick = {
                    sendEmail(context, info.supportEmail, resources.getString(R.string.support_subject), "")
                })
                NfDivider(inset = true)
                NfListRow(stringResource(R.string.settings_bug), icon = Icons.Rounded.BugReport, onClick = onOpenBugReport)
                NfDivider(inset = true)
                NfListRow(stringResource(R.string.settings_guidelines), icon = Icons.Rounded.Gavel, onClick = { onOpenDocument(AppDocument.Guidelines) })
                NfDivider(inset = true)
                NfListRow(stringResource(R.string.settings_privacy_policy), icon = Icons.Rounded.PrivacyTip, onClick = { onOpenDocument(AppDocument.Privacy) })
                NfDivider(inset = true)
                NfListRow(stringResource(R.string.settings_terms), icon = Icons.Rounded.Description, onClick = { onOpenDocument(AppDocument.Terms) })
                NfDivider(inset = true)
                NfListRow(stringResource(R.string.settings_licenses), icon = Icons.Rounded.Code, onClick = { onOpenDocument(AppDocument.Licenses) })
            }

            NfSectionHeader(stringResource(R.string.settings_account))
            NfGroup {
                NfListRow(stringResource(R.string.settings_deactivate), subtitle = stringResource(R.string.settings_deactivate_body), icon = Icons.Rounded.PauseCircle, onClick = { confirmDeactivate = true })
                NfDivider(inset = true)
                NfListRow(
                    stringResource(R.string.settings_delete),
                    subtitle = stringResource(R.string.settings_delete_body),
                    icon = Icons.Rounded.DeleteForever,
                    iconTint = Nf.colors.danger,
                    titleColor = Nf.colors.danger,
                    onClick = onOpenDelete,
                )
                NfDivider(inset = true)
                NfListRow(stringResource(R.string.settings_sign_out), icon = Icons.AutoMirrored.Rounded.Logout, onClick = { confirmSignOut = true }, showChevron = false)
            }
            Spacer(Modifier.height(24.dp))
            Text(stringResource(R.string.settings_version, info.versionName), style = Nf.type.labelSmall, color = Nf.colors.textTertiary, modifier = Modifier.padding(horizontal = 4.dp))
            Text(stringResource(R.string.settings_network_local), style = Nf.type.labelSmall, color = Nf.colors.textTertiary, modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp))
        }
    }

    if (confirmDeactivate) {
        NfConfirmDialog(
            title = stringResource(R.string.deactivate_title),
            message = stringResource(R.string.deactivate_body),
            confirmLabel = stringResource(R.string.deactivate_confirm),
            dismissLabel = stringResource(app.nearfolk.core.ui.R.string.ui_cancel),
            onConfirm = {
                confirmDeactivate = false
                viewModel.deactivate()
            },
            onDismiss = { confirmDeactivate = false },
        )
    }
    if (confirmSignOut) {
        NfConfirmDialog(
            title = stringResource(R.string.sign_out_title),
            message = stringResource(R.string.sign_out_body),
            confirmLabel = stringResource(R.string.sign_out_confirm),
            dismissLabel = stringResource(app.nearfolk.core.ui.R.string.ui_cancel),
            onConfirm = {
                confirmSignOut = false
                viewModel.signOut()
            },
            onDismiss = { confirmSignOut = false },
        )
    }
}

internal fun openNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    runCatching { context.startActivity(intent) }
}

/** Opens an email draft. Returns false when no email app is installed. */
internal fun sendEmail(context: Context, to: String, subject: String, body: String): Boolean {
    val intent = Intent(Intent.ACTION_SENDTO, "mailto:".toUri()).apply {
        putExtra(Intent.EXTRA_EMAIL, arrayOf(to))
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
    }
    return try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(subject, body.ifBlank { to }))
        false
    }
}

