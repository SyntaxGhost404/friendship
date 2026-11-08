package app.nearfolk.feature.journal

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.nearfolk.core.designsystem.component.NfButton
import app.nearfolk.core.designsystem.component.NfButtonSize
import app.nearfolk.core.designsystem.component.NfButtonStyle
import app.nearfolk.core.designsystem.component.NfChip
import app.nearfolk.core.designsystem.component.NfConfirmDialog
import app.nearfolk.core.designsystem.component.NfEmptyState
import app.nearfolk.core.designsystem.component.NfIconButton
import app.nearfolk.core.designsystem.component.NfNotice
import app.nearfolk.core.designsystem.component.NfNoticeTone
import app.nearfolk.core.designsystem.component.NfSectionHeader
import app.nearfolk.core.designsystem.component.NfSegmented
import app.nearfolk.core.designsystem.component.NfSpinner
import app.nearfolk.core.designsystem.component.NfTag
import app.nearfolk.core.designsystem.component.NfTextField
import app.nearfolk.core.designsystem.component.NfTopBar
import app.nearfolk.core.designsystem.component.pressScale
import app.nearfolk.core.designsystem.icon.NfIcons
import app.nearfolk.core.designsystem.theme.Nf
import app.nearfolk.core.designsystem.theme.NfShapes
import app.nearfolk.core.model.JournalEntry
import app.nearfolk.core.model.Mood
import app.nearfolk.core.model.Visibility
import app.nearfolk.core.ui.LocalSnackbarHost
import app.nearfolk.core.ui.labelRes
import app.nearfolk.core.ui.rememberTimeFormat
import kotlinx.serialization.Serializable

@Serializable
data object JournalRoute

@Serializable
data class JournalEditorRoute(val entryId: String? = null)

@Serializable
data class PasscodeSetupRoute(val flow: String)

@Serializable
data class PublicJournalRoute(val memberId: String)

@Composable
fun JournalRouteScreen(
    onBack: () -> Unit,
    onNewEntry: () -> Unit,
    onOpenEntry: (String) -> Unit,
    onSetPasscode: () -> Unit,
    viewModel: JournalViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val entry by viewModel.entry.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val locked = state.passcodeSet && !state.unlocked
    Column(Modifier.fillMaxSize().background(Nf.colors.canvas)) {
        NfTopBar(
            stringResource(R.string.journal_title),
            onBack = onBack,
            backDescription = stringResource(app.nearfolk.core.ui.R.string.ui_back),
            actions = {
                if (state.passcodeSet && state.unlocked) {
                    NfIconButton(Icons.Rounded.Lock, stringResource(R.string.journal_lock_now), viewModel::lock)
                }
                if (!locked && !state.loading) {
                    NfIconButton(Icons.Rounded.Add, stringResource(R.string.journal_new), onNewEntry, style = NfButtonStyle.Primary)
                }
            },
        )
        AnimatedContent(
            targetState = when {
                state.loading -> 0
                locked -> 1
                else -> 2
            },
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "journalGate",
            modifier = Modifier.weight(1f),
        ) { mode ->
            when (mode) {
                0 -> NfSpinner()
                1 -> Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 24.dp), contentAlignment = Alignment.TopCenter) {
                    PasscodePad(
                        title = stringResource(R.string.passcode_enter_title),
                        body = stringResource(R.string.passcode_enter_body),
                        entered = entry.length,
                        message = message,
                        shake = viewModel.shake,
                        onDigit = viewModel::digit,
                        onBackspace = viewModel::backspace,
                    )
                }
                else -> JournalList(state, onOpenEntry, onNewEntry, onSetPasscode)
            }
        }
    }
}

@Composable
private fun JournalList(state: JournalUiState, onOpen: (String) -> Unit, onNew: () -> Unit, onSetPasscode: () -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (!state.passcodeSet) {
            item {
                NfNotice(
                    title = stringResource(R.string.journal_protect_title),
                    text = stringResource(R.string.journal_protect_body),
                    icon = Icons.Rounded.LockOpen,
                    tone = NfNoticeTone.Accent,
                    action = { NfButton(stringResource(R.string.journal_protect_cta), onSetPasscode, size = NfButtonSize.Small) },
                )
            }
        }
        if (state.entries.isEmpty()) {
            item {
                NfEmptyState(
                    NfIcons.Journal,
                    stringResource(R.string.journal_empty_title),
                    stringResource(R.string.journal_empty_body),
                    action = { NfButton(stringResource(R.string.journal_new), onNew, size = NfButtonSize.Medium) },
                )
            }
        }
        items(state.entries, key = { it.id }) { entry ->
            EntryCard(entry, onClick = { onOpen(entry.id) }, showVisibility = true, modifier = Modifier.animateItem())
        }
    }
}

@Composable
internal fun EntryCard(entry: JournalEntry, onClick: (() -> Unit)?, showVisibility: Boolean, modifier: Modifier = Modifier) {
    val time = rememberTimeFormat()
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier
            .fillMaxWidth()
            .pressScale(interaction, pressed = 0.985f)
            .clip(NfShapes.large)
            .background(Nf.colors.surface)
            .then(if (onClick != null) Modifier.clickable(interactionSource = interaction, indication = ripple(), role = Role.Button, onClick = onClick) else Modifier)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(time.shortDate(time.localDate(entry.createdAt)), style = Nf.type.labelSmall, color = Nf.colors.textTertiary, modifier = Modifier.weight(1f))
            NfTag(stringResource(entry.mood.labelRes()))
            if (showVisibility) {
                NfTag(
                    stringResource(if (entry.visibility == Visibility.Public) R.string.journal_public else R.string.journal_private),
                    emphasized = entry.visibility == Visibility.Public,
                )
            }
        }
        if (entry.title.isNotBlank()) Text(entry.title, style = Nf.type.titleSmall, color = Nf.colors.textPrimary)
        Text(
            entry.body,
            style = Nf.type.body,
            color = Nf.colors.textSecondary,
            maxLines = if (onClick != null) 4 else Int.MAX_VALUE,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun JournalEditorRouteScreen(onBack: () -> Unit, viewModel: JournalEditorViewModel = hiltViewModel()) {
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    var deleting by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(viewModel) { viewModel.done.collect { onBack() } }
    Column(Modifier.fillMaxSize().background(Nf.colors.canvas).imePadding()) {
        NfTopBar(
            stringResource(if (draft.isNew) R.string.journal_editor_new else R.string.journal_editor_edit),
            onBack = onBack,
            backDescription = stringResource(app.nearfolk.core.ui.R.string.ui_back),
            actions = {
                NfButton(stringResource(R.string.journal_save), viewModel::save, enabled = draft.body.isNotBlank(), size = NfButtonSize.Small)
            },
        )
        if (!draft.loaded) {
            NfSpinner()
            return@Column
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            NfTextField(
                value = draft.title,
                onValueChange = { v -> viewModel.update { it.copy(title = v) } },
                placeholder = stringResource(R.string.journal_title_hint),
                maxLength = 80,
                textStyle = Nf.type.title,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
            Spacer(Modifier.height(10.dp))
            NfTextField(
                value = draft.body,
                onValueChange = { v -> viewModel.update { it.copy(body = v) } },
                placeholder = stringResource(R.string.journal_body_hint),
                singleLine = false,
                minLines = 8,
                maxLength = 5000,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
            NfSectionHeader(stringResource(R.string.journal_mood))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(Mood.entries) { mood ->
                    NfChip(stringResource(mood.labelRes()), selected = draft.mood == mood, onClick = { viewModel.update { it.copy(mood = mood) } })
                }
            }
            NfSectionHeader(stringResource(R.string.journal_visibility))
            NfSegmented(
                options = listOf(stringResource(R.string.journal_private), stringResource(R.string.journal_public)),
                selectedIndex = if (draft.visibility == Visibility.Private) 0 else 1,
                onSelect = { i -> viewModel.update { it.copy(visibility = if (i == 0) Visibility.Private else Visibility.Public) } },
            )
            Text(
                stringResource(if (draft.visibility == Visibility.Private) R.string.journal_visibility_private_body else R.string.journal_visibility_public_body),
                style = Nf.type.bodySmall,
                color = Nf.colors.textTertiary,
                modifier = Modifier.padding(4.dp),
            )
            if (!draft.isNew) {
                Spacer(Modifier.height(24.dp))
                NfButton(stringResource(R.string.journal_delete), { deleting = true }, style = NfButtonStyle.Outline, modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(24.dp).windowInsetsPadding(WindowInsets.navigationBars))
        }
    }
    if (deleting) {
        NfConfirmDialog(
            title = stringResource(R.string.journal_delete_title),
            message = stringResource(R.string.journal_delete_body),
            confirmLabel = stringResource(app.nearfolk.core.ui.R.string.ui_delete),
            dismissLabel = stringResource(app.nearfolk.core.ui.R.string.ui_cancel),
            onConfirm = {
                deleting = false
                viewModel.delete()
            },
            onDismiss = { deleting = false },
            destructive = true,
        )
    }
}

@Composable
fun PasscodeSetupRouteScreen(onDone: () -> Unit, viewModel: PasscodeSetupViewModel = hiltViewModel()) {
    val stage by viewModel.stage.collectAsStateWithLifecycle()
    val entry by viewModel.entry.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbarHost.current
    val context = LocalContext.current
    val resources = LocalResources.current
    LaunchedEffect(stage) {
        if (stage == PasscodeSetupViewModel.Stage.Done) {
            onDone()
            snackbar.showSnackbar(
                resources.getString(if (viewModel.flow == PasscodeFlow.Remove) R.string.passcode_removed else R.string.passcode_saved),
            )
        }
    }
    Column(Modifier.fillMaxSize().background(Nf.colors.canvas)) {
        NfTopBar("", onBack = onDone, backDescription = stringResource(app.nearfolk.core.ui.R.string.ui_back))
        Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 16.dp), contentAlignment = Alignment.TopCenter) {
            val (title, body) = when (stage) {
                PasscodeSetupViewModel.Stage.Current -> R.string.passcode_current_title to R.string.passcode_enter_body
                PasscodeSetupViewModel.Stage.Confirm -> R.string.passcode_confirm_title to R.string.passcode_confirm_body
                else -> R.string.passcode_create_title to R.string.passcode_create_body
            }
            PasscodePad(
                title = stringResource(title),
                body = stringResource(body),
                entered = entry.length,
                message = message,
                shake = viewModel.shake,
                onDigit = viewModel::digit,
                onBackspace = viewModel::backspace,
            )
        }
    }
}

@Composable
fun PublicJournalRouteScreen(onBack: () -> Unit, viewModel: PublicJournalViewModel = hiltViewModel()) {
    val member by viewModel.member.collectAsStateWithLifecycle()
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().background(Nf.colors.canvas)) {
        NfTopBar(
            member?.let { stringResource(R.string.journal_public_title, it.displayName) } ?: stringResource(R.string.journal_title),
            onBack = onBack,
            backDescription = stringResource(app.nearfolk.core.ui.R.string.ui_back),
        )
        val list = entries
        when {
            list == null -> NfSpinner()
            list.isEmpty() -> NfEmptyState(NfIcons.Journal, stringResource(R.string.journal_title), stringResource(R.string.journal_public_empty))
            else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(list, key = { it.id }) { EntryCard(it, onClick = null, showVisibility = false) }
            }
        }
    }
}
