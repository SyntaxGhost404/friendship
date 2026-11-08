package app.nearfolk.feature.settings

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.nearfolk.core.designsystem.component.NfButton
import app.nearfolk.core.designsystem.component.NfButtonSize
import app.nearfolk.core.designsystem.component.NfButtonStyle
import app.nearfolk.core.designsystem.component.NfChoiceRow
import app.nearfolk.core.designsystem.component.NfConfirmDialog
import app.nearfolk.core.designsystem.component.NfEmptyState
import app.nearfolk.core.designsystem.component.NfNotice
import app.nearfolk.core.designsystem.component.NfNoticeTone
import app.nearfolk.core.designsystem.component.NfSectionHeader
import app.nearfolk.core.designsystem.component.NfSpinner
import app.nearfolk.core.designsystem.component.NfSwitchRow
import app.nearfolk.core.designsystem.component.NfTextField
import app.nearfolk.core.designsystem.component.NfTopBar
import app.nearfolk.core.designsystem.theme.HapticKind
import app.nearfolk.core.designsystem.theme.LocalAppHaptics
import app.nearfolk.core.designsystem.theme.Nf
import app.nearfolk.core.designsystem.theme.NfShapes
import app.nearfolk.core.model.DeletionReason
import app.nearfolk.core.model.PlusLimits
import app.nearfolk.core.model.PlusPlan
import app.nearfolk.core.ui.AppDocument
import app.nearfolk.core.ui.DocumentBody
import app.nearfolk.core.ui.LocalAppInfo
import app.nearfolk.core.ui.LocalSnackbarHost
import app.nearfolk.core.ui.MemberAvatar
import app.nearfolk.core.ui.rememberTimeFormat
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun PlusRouteScreen(onBack: () -> Unit, viewModel: PlusViewModel = hiltViewModel()) {
    val status by viewModel.status.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbarHost.current
    val context = LocalContext.current
    val resources = LocalResources.current
    val time = rememberTimeFormat()
    var plan by rememberSaveable { mutableStateOf(PlusPlan.Yearly) }
    var confirmCancel by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(viewModel) { viewModel.purchased.collect { snackbar.showSnackbar(resources.getString(R.string.plus_welcome)) } }
    Column(Modifier.fillMaxSize().background(Nf.colors.canvas)) {
        NfTopBar("", onBack = onBack, backDescription = stringResource(app.nearfolk.core.ui.R.string.ui_back))
        val s = status
        if (s == null) {
            NfSpinner()
            return@Column
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Box(
                Modifier.size(64.dp).clip(NfShapes.large).background(Nf.colors.accent),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Verified, null, tint = Nf.colors.onAccent, modifier = Modifier.size(32.dp)) }
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.plus_title), style = Nf.type.headline, color = Nf.colors.textPrimary)
            Text(stringResource(R.string.plus_tagline), style = Nf.type.body, color = Nf.colors.textSecondary)
            Spacer(Modifier.height(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Feature(Icons.Rounded.Tune, stringResource(R.string.plus_feature_age_title), stringResource(R.string.plus_feature_age_body))
                Feature(Icons.Rounded.Mic, stringResource(R.string.plus_feature_voice_title), stringResource(R.string.plus_feature_voice_body, PlusLimits.PLUS_VOICE_SECONDS, PlusLimits.FREE_VOICE_SECONDS))
                Feature(Icons.Rounded.Collections, stringResource(R.string.plus_feature_highlights_title), stringResource(R.string.plus_feature_highlights_body, PlusLimits.PLUS_HIGHLIGHTS, PlusLimits.FREE_HIGHLIGHTS))
                Feature(Icons.Rounded.Verified, stringResource(R.string.plus_feature_mark_title), stringResource(R.string.plus_feature_mark_body))
            }
            Spacer(Modifier.height(24.dp))
            if (s.active) {
                NfNotice(
                    title = stringResource(R.string.plus_active_title),
                    text = stringResource(
                        R.string.plus_active_body,
                        stringResource(if (s.plan == PlusPlan.Yearly) R.string.plus_yearly else R.string.plus_monthly),
                        s.renewsOnEpochDay?.let { time.longDate(LocalDate.ofEpochDay(it)) }.orEmpty(),
                    ),
                    tone = NfNoticeTone.Accent,
                )
                Spacer(Modifier.height(12.dp))
                NfButton(stringResource(R.string.plus_cancel), { confirmCancel = true }, style = NfButtonStyle.Outline, modifier = Modifier.fillMaxWidth())
            } else {
                PlanOption(stringResource(R.string.plus_yearly), stringResource(R.string.plus_yearly_body), plan == PlusPlan.Yearly) { plan = PlusPlan.Yearly }
                Spacer(Modifier.height(10.dp))
                PlanOption(stringResource(R.string.plus_monthly), stringResource(R.string.plus_monthly_body), plan == PlusPlan.Monthly) { plan = PlusPlan.Monthly }
                if (!viewModel.storeConnected) {
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.plus_store_note), style = Nf.type.bodySmall, color = Nf.colors.textTertiary)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
        if (!s.active) {
            NfButton(
                stringResource(R.string.plus_start),
                { viewModel.purchase(plan) },
                loading = busy,
                modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = 20.dp, vertical = 12.dp),
            )
        }
    }
    if (confirmCancel) {
        NfConfirmDialog(
            title = stringResource(R.string.plus_cancel_title),
            message = stringResource(R.string.plus_cancel_body),
            confirmLabel = stringResource(R.string.plus_cancel_confirm),
            dismissLabel = stringResource(R.string.plus_keep),
            onConfirm = {
                confirmCancel = false
                viewModel.cancel()
            },
            onDismiss = { confirmCancel = false },
            destructive = true,
        )
    }
}

@Composable
private fun Feature(icon: ImageVector, title: String, body: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(40.dp).clip(NfShapes.medium).background(Nf.colors.surfaceRaised), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Nf.colors.textPrimary, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = Nf.type.titleSmall, color = Nf.colors.textPrimary)
            Text(body, style = Nf.type.bodySmall, color = Nf.colors.textSecondary)
        }
    }
}

@Composable
private fun PlanOption(title: String, body: String, selected: Boolean, onClick: () -> Unit) {
    val haptics = LocalAppHaptics.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(NfShapes.large)
            .background(if (selected) Nf.colors.accentSoft else Nf.colors.surface)
            .border(1.5.dp, if (selected) Nf.colors.accent else Nf.colors.line, NfShapes.large)
            .clickable(role = Role.RadioButton) {
                haptics.perform(HapticKind.Tick)
                onClick()
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = Nf.type.titleSmall, color = if (selected) Nf.colors.onAccentSoft else Nf.colors.textPrimary)
            Text(body, style = Nf.type.bodySmall, color = if (selected) Nf.colors.onAccentSoft else Nf.colors.textSecondary)
        }
        if (selected) Icon(Icons.Rounded.Check, null, tint = Nf.colors.accent)
    }
}

@Composable
fun BlockedRouteScreen(onBack: () -> Unit, viewModel: BlockedViewModel = hiltViewModel()) {
    val blocked by viewModel.blocked.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbarHost.current
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().background(Nf.colors.canvas)) {
        NfTopBar(stringResource(R.string.blocked_title), onBack = onBack, backDescription = stringResource(app.nearfolk.core.ui.R.string.ui_back))
        val list = blocked
        when {
            list == null -> NfSpinner()
            list.isEmpty() -> NfEmptyState(Icons.Rounded.Block, stringResource(R.string.blocked_empty_title), stringResource(R.string.blocked_empty_body))
            else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(list, key = { it.id }) { m ->
                    Row(
                        Modifier.fillMaxWidth().animateItem().clip(NfShapes.large).background(Nf.colors.surface).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        MemberAvatar(m.displayName, m.avatar, 44.dp)
                        Text(m.displayName, style = Nf.type.bodyStrong, color = Nf.colors.textPrimary, modifier = Modifier.weight(1f))
                        NfButton(stringResource(R.string.blocked_unblock), {
                            viewModel.unblock(m.id)
                            scope.launch { snackbar.showSnackbar(resources.getString(R.string.blocked_unblocked, m.displayName)) }
                        }, size = NfButtonSize.Small, style = NfButtonStyle.Secondary)
                    }
                }
            }
        }
    }
}

@Composable
fun DocumentRouteScreen(document: AppDocument, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Nf.colors.canvas)) {
        NfTopBar(stringResource(document.title), onBack = onBack, backDescription = stringResource(app.nearfolk.core.ui.R.string.ui_back))
        DocumentBody(document, Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 32.dp))
    }
}

@Composable
fun BugReportRouteScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val info = LocalAppInfo.current
    val snackbar = LocalSnackbarHost.current
    val scope = rememberCoroutineScope()
    var text by rememberSaveable { mutableStateOf("") }
    var includeDevice by rememberSaveable { mutableStateOf(true) }
    Column(Modifier.fillMaxSize().background(Nf.colors.canvas).imePadding()) {
        NfTopBar(stringResource(R.string.bug_title), onBack = onBack, backDescription = stringResource(app.nearfolk.core.ui.R.string.ui_back))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.bug_body), style = Nf.type.body, color = Nf.colors.textSecondary)
            NfTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = stringResource(R.string.bug_hint),
                singleLine = false,
                minLines = 6,
                maxLength = 3000,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
            Box(Modifier.clip(NfShapes.large).background(Nf.colors.surface)) {
                NfSwitchRow(stringResource(R.string.bug_include_device), includeDevice, { includeDevice = it })
            }
        }
        NfButton(
            stringResource(R.string.bug_send),
            onClick = {
                val details = if (includeDevice) {
                    "\n\nApp ${info.versionName}\nAndroid ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n${Build.MANUFACTURER} ${Build.MODEL}"
                } else {
                    ""
                }
                val sent = sendEmail(context, info.supportEmail, resources.getString(R.string.bug_subject), text.trim() + details)
                if (sent) onBack() else scope.launch { snackbar.showSnackbar(resources.getString(R.string.settings_no_email)) }
            },
            enabled = text.isNotBlank(),
            modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing).padding(16.dp),
        )
    }
}

private fun DeletionReason.label(): Int = when (this) {
    DeletionReason.NotUseful -> R.string.delete_reason_not_useful
    DeletionReason.PrivacyConcerns -> R.string.delete_reason_privacy
    DeletionReason.TooManyMessages -> R.string.delete_reason_messages
    DeletionReason.FoundSomeone -> R.string.delete_reason_found
    DeletionReason.TakingABreak -> R.string.delete_reason_break
    DeletionReason.Other -> R.string.delete_reason_other
}

@Composable
fun DeleteAccountRouteScreen(onBack: () -> Unit, viewModel: DeleteAccountViewModel = hiltViewModel()) {
    val deleting by viewModel.deleting.collectAsStateWithLifecycle()
    var step by rememberSaveable { mutableIntStateOf(0) }
    var reason by rememberSaveable { mutableStateOf(DeletionReason.Other) }
    var details by rememberSaveable { mutableStateOf("") }
    var typed by rememberSaveable { mutableStateOf("") }
    val word = stringResource(R.string.delete_confirm_word)
    Column(Modifier.fillMaxSize().background(Nf.colors.canvas).imePadding()) {
        NfTopBar(stringResource(R.string.delete_title), onBack = { if (step > 0) step-- else onBack() }, backDescription = stringResource(app.nearfolk.core.ui.R.string.ui_back))
        AnimatedContent(step, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "deleteStep", modifier = Modifier.weight(1f)) { s ->
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                when (s) {
                    0 -> {
                        NfSectionHeader(stringResource(R.string.delete_what_title))
                        listOf(
                            R.string.delete_what_profile,
                            R.string.delete_what_posts,
                            R.string.delete_what_chats,
                            R.string.delete_what_journal,
                            R.string.delete_what_settings,
                        ).forEach { res ->
                            Row(Modifier.padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(Modifier.padding(top = 7.dp).size(6.dp).clip(NfShapes.badge).background(Nf.colors.danger))
                                Text(stringResource(res), style = Nf.type.body, color = Nf.colors.textPrimary)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        NfNotice(
                            stringResource(R.string.delete_consider),
                            icon = Icons.Rounded.PauseCircle,
                            action = { NfButton(stringResource(R.string.delete_deactivate_instead), { viewModel.deactivate() }, size = NfButtonSize.Small, style = NfButtonStyle.Secondary) },
                        )
                    }
                    1 -> {
                        NfSectionHeader(stringResource(R.string.delete_reason_title))
                        Text(stringResource(R.string.delete_reason_body), style = Nf.type.bodySmall, color = Nf.colors.textTertiary, modifier = Modifier.padding(horizontal = 4.dp))
                        Spacer(Modifier.height(8.dp))
                        DeletionReason.entries.forEach { r -> NfChoiceRow(stringResource(r.label()), reason == r, { reason = r }) }
                        NfTextField(
                            value = details,
                            onValueChange = { details = it },
                            label = stringResource(R.string.delete_details),
                            singleLine = false,
                            minLines = 3,
                            maxLength = 500,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                    else -> {
                        NfSectionHeader(stringResource(R.string.delete_confirm_title))
                        Text(stringResource(R.string.delete_confirm_body), style = Nf.type.body, color = Nf.colors.textSecondary, modifier = Modifier.padding(horizontal = 4.dp))
                        Spacer(Modifier.height(12.dp))
                        NfTextField(
                            value = typed,
                            onValueChange = { typed = it },
                            placeholder = word,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        )
                    }
                }
            }
        }
        NfButton(
            text = if (step < 2) stringResource(R.string.delete_continue) else stringResource(R.string.delete_confirm_button),
            onClick = { if (step < 2) step++ else viewModel.delete(reason, details) },
            style = if (step < 2) NfButtonStyle.Secondary else NfButtonStyle.Danger,
            enabled = step < 2 || typed.trim() == word,
            loading = deleting,
            modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing).padding(16.dp),
        )
    }
}

@Composable
fun ReactivateScreen(onDelete: () -> Unit, viewModel: ReactivateViewModel = hiltViewModel()) {
    Column(
        Modifier.fillMaxSize().background(Nf.colors.canvas).windowInsetsPadding(WindowInsets.safeDrawing).padding(24.dp),
    ) {
        Spacer(Modifier.weight(1f))
        Box(Modifier.size(72.dp).clip(NfShapes.large).background(Nf.colors.surfaceRaised), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.PauseCircle, null, tint = Nf.colors.textPrimary, modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text(stringResource(R.string.reactivate_title), style = Nf.type.headline, color = Nf.colors.textPrimary)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.reactivate_body), style = Nf.type.body, color = Nf.colors.textSecondary)
        Spacer(Modifier.weight(1.4f))
        NfButton(stringResource(R.string.reactivate_cta), { viewModel.reactivate() }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        NfButton(stringResource(R.string.settings_sign_out), { viewModel.signOut() }, style = NfButtonStyle.Secondary, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(4.dp))
        NfButton(stringResource(R.string.settings_delete), onDelete, style = NfButtonStyle.Quiet, modifier = Modifier.fillMaxWidth())
    }
}
