package app.nearfolk.feature.you

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.nearfolk.core.designsystem.component.NfButton
import app.nearfolk.core.designsystem.component.NfButtonSize
import app.nearfolk.core.designsystem.component.NfButtonStyle
import app.nearfolk.core.designsystem.component.NfConfirmDialog
import app.nearfolk.core.designsystem.component.NfNotice
import app.nearfolk.core.designsystem.component.NfNoticeTone
import app.nearfolk.core.designsystem.component.NfSectionHeader
import app.nearfolk.core.designsystem.component.NfTopBar
import app.nearfolk.core.designsystem.theme.HapticKind
import app.nearfolk.core.designsystem.theme.LocalAppHaptics
import app.nearfolk.core.designsystem.theme.Nf
import app.nearfolk.core.designsystem.theme.NfMotion
import app.nearfolk.core.designsystem.theme.NfShapes
import app.nearfolk.core.model.PlusLimits
import app.nearfolk.core.ui.VoiceIntroBar
import app.nearfolk.core.ui.Waveform
import app.nearfolk.core.ui.formatDuration

@Composable
fun VoiceIntroRouteScreen(onBack: () -> Unit, viewModel: VoiceIntroViewModel = hiltViewModel()) {
    val me by viewModel.me.collectAsStateWithLifecycle()
    val stage by viewModel.stage.collectAsStateWithLifecycle()
    val playback by viewModel.playback.collectAsStateWithLifecycle()
    val problem by viewModel.problem.collectAsStateWithLifecycle()
    val isPlus by viewModel.isPlus.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptics = LocalAppHaptics.current
    var permissionDenied by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionDenied = !granted
        if (granted) viewModel.start()
    }
    fun record() {
        haptics.perform(HapticKind.Confirm)
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            viewModel.start()
        } else {
            permission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Column(Modifier.fillMaxSize().background(Nf.colors.canvas)) {
        NfTopBar(stringResource(R.string.voice_title), onBack = onBack, backDescription = stringResource(app.nearfolk.core.ui.R.string.ui_back))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Text(stringResource(R.string.voice_body), style = Nf.type.body, color = Nf.colors.textSecondary)
            Text(
                if (isPlus) stringResource(R.string.voice_limit_plus, PlusLimits.PLUS_VOICE_SECONDS)
                else stringResource(R.string.voice_limit, PlusLimits.FREE_VOICE_SECONDS, PlusLimits.PLUS_VOICE_SECONDS),
                style = Nf.type.bodySmall,
                color = Nf.colors.textTertiary,
                modifier = Modifier.padding(top = 4.dp),
            )
            if (permissionDenied) {
                NfNotice(
                    stringResource(R.string.voice_permission),
                    tone = NfNoticeTone.Danger,
                    modifier = Modifier.padding(top = 12.dp),
                    action = {
                        NfButton(stringResource(R.string.voice_open_settings), {
                            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)))
                        }, size = NfButtonSize.Small, style = NfButtonStyle.Secondary)
                    },
                )
            }
            problem?.let {
                NfNotice(
                    stringResource(if (it == VoiceIntroViewModel.Problem.TooShort) R.string.voice_too_short else R.string.voice_failed),
                    tone = NfNoticeTone.Danger,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            me?.voiceIntro?.let { intro ->
                if (stage is VoiceStage.Idle) {
                    NfSectionHeader(stringResource(R.string.voice_current))
                    val mine = playback.path == intro.path
                    VoiceIntroBar(intro.durationMillis, mine && playback.playing, if (mine) playback.positionMillis else 0, { viewModel.toggle(intro.path) })
                    NfButton(
                        stringResource(R.string.voice_delete),
                        { confirmDelete = true },
                        style = NfButtonStyle.Quiet,
                        size = NfButtonSize.Small,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            AnimatedContent(
                targetState = stage,
                contentKey = { it::class },
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "voiceStage",
            ) { s ->
                when (s) {
                    VoiceStage.Idle -> RecordButton(recording = false, onClick = ::record)
                    is VoiceStage.Recording -> Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Waveform(1f, Modifier.fillMaxWidth().height(64.dp), levels = s.levels.ifEmpty { listOf(0.1f) }.let { l -> List(48) { i -> l.getOrElse(i - (48 - l.size)) { 0.05f } } })
                        Spacer(Modifier.height(16.dp))
                        Text(
                            stringResource(R.string.voice_recording, formatDuration(s.elapsedMillis), formatDuration(viewModel.limitMillis)),
                            style = Nf.type.label,
                            color = Nf.colors.textSecondary,
                        )
                        Spacer(Modifier.height(24.dp))
                        RecordButton(recording = true, onClick = viewModel::stop)
                    }
                    is VoiceStage.Review -> Column {
                        NfSectionHeader(stringResource(R.string.voice_new))
                        val mine = playback.path == s.path
                        VoiceIntroBar(s.durationMillis, mine && playback.playing, if (mine) playback.positionMillis else 0, { viewModel.toggle(s.path) })
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            NfButton(stringResource(R.string.voice_discard), viewModel::discardReview, style = NfButtonStyle.Secondary, modifier = Modifier.weight(1f))
                            NfButton(stringResource(R.string.voice_save), viewModel::save, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
    if (confirmDelete) {
        NfConfirmDialog(
            title = stringResource(R.string.voice_delete_title),
            message = stringResource(R.string.voice_delete_body),
            confirmLabel = stringResource(app.nearfolk.core.ui.R.string.ui_delete),
            dismissLabel = stringResource(app.nearfolk.core.ui.R.string.ui_cancel),
            onConfirm = {
                confirmDelete = false
                viewModel.delete()
            },
            onDismiss = { confirmDelete = false },
            destructive = true,
        )
    }
}

@Composable
private fun RecordButton(recording: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(if (recording) 0.92f else 1f, NfMotion.snappy(), label = "record")
    val label = stringResource(if (recording) R.string.voice_stop else R.string.voice_record)
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(96.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(NfShapes.extraLarge)
                .background(if (recording) Nf.colors.danger else Nf.colors.accent)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics { contentDescription = label },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (recording) Icons.Rounded.Stop else Icons.Rounded.Mic,
                null,
                tint = if (recording) Nf.colors.onDanger else Nf.colors.onAccent,
                modifier = Modifier.size(40.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(label, style = Nf.type.label, color = Nf.colors.textPrimary)
    }
}
