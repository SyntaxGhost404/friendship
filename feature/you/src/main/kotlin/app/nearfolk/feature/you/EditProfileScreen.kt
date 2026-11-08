package app.nearfolk.feature.you

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.nearfolk.core.designsystem.component.NfButton
import app.nearfolk.core.designsystem.component.NfButtonSize
import app.nearfolk.core.designsystem.component.NfButtonStyle
import app.nearfolk.core.designsystem.component.NfConfirmDialog
import app.nearfolk.core.designsystem.component.NfSectionHeader
import app.nearfolk.core.designsystem.component.NfSegmented
import app.nearfolk.core.designsystem.component.NfSpinner
import app.nearfolk.core.designsystem.component.NfTextField
import app.nearfolk.core.designsystem.component.NfTopBar
import app.nearfolk.core.designsystem.theme.HapticKind
import app.nearfolk.core.designsystem.theme.LocalAppHaptics
import app.nearfolk.core.designsystem.theme.Nf
import app.nearfolk.core.designsystem.theme.NfShapes
import app.nearfolk.core.model.Countries
import app.nearfolk.core.model.Gender
import app.nearfolk.core.model.ProfileRules
import app.nearfolk.core.ui.CountryPickerSheet
import app.nearfolk.core.ui.LocalSnackbarHost
import app.nearfolk.core.ui.MediaImage
import app.nearfolk.core.ui.MemberAvatar
import app.nearfolk.core.ui.NfDatePickerDialog
import app.nearfolk.core.ui.labelRes
import app.nearfolk.core.ui.rememberTimeFormat

@Composable
fun EditProfileRouteScreen(
    onBack: () -> Unit,
    onCrop: (uri: String, target: CropTarget) -> Unit,
    viewModel: EditProfileViewModel = hiltViewModel(),
) {
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val me by viewModel.me.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbarHost.current
    val context = LocalContext.current
    val resources = LocalResources.current
    val time = rememberTimeFormat()
    val today = viewModel.today
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    var pickBirth by rememberSaveable { mutableStateOf(false) }
    var pickStreak by rememberSaveable { mutableStateOf(false) }
    var pickCountry by rememberSaveable { mutableStateOf(false) }
    var pendingTarget by rememberSaveable { mutableStateOf<CropTarget?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val target = pendingTarget
        if (uri != null && target != null) onCrop(uri.toString(), target)
        pendingTarget = null
    }
    fun pick(target: CropTarget) {
        pendingTarget = target
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    val leave = { if (viewModel.hasChanges) confirmDiscard = true else onBack() }
    BackHandler(onBack = leave)
    LaunchedEffect(viewModel) {
        viewModel.saved.collect {
            onBack()
            snackbar.showSnackbar(resources.getString(R.string.edit_saved))
        }
    }

    Column(Modifier.fillMaxSize().background(Nf.colors.canvas).imePadding()) {
        NfTopBar(
            stringResource(R.string.edit_title),
            onBack = leave,
            backDescription = stringResource(app.nearfolk.core.ui.R.string.ui_back),
            actions = {
                NfButton(stringResource(R.string.edit_save), viewModel::save, enabled = viewModel.canSave(draft), size = NfButtonSize.Small)
            },
        )
        val m = me
        if (!draft.loaded || m == null) {
            NfSpinner()
            return@Column
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            NfSectionHeader(stringResource(R.string.edit_banner))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(NfShapes.large)
                    .background(Nf.colors.surfaceRaised)
                    .clickable(role = Role.Button) { pick(CropTarget.Banner) },
            ) {
                m.banner?.let { MediaImage(it, Modifier.fillMaxSize()) }
            }
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NfButton(stringResource(R.string.edit_change), { pick(CropTarget.Banner) }, size = NfButtonSize.Small, style = NfButtonStyle.Secondary)
                if (m.banner != null) NfButton(stringResource(R.string.edit_remove), { viewModel.removeBanner() }, size = NfButtonSize.Small, style = NfButtonStyle.Quiet)
            }

            NfSectionHeader(stringResource(R.string.edit_photo))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.clip(NfShapes.avatar).clickable(role = Role.Button) { pick(CropTarget.Avatar) }) {
                    MemberAvatar(draft.name.ifBlank { m.displayName }, m.avatar, 84.dp)
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    NfButton(stringResource(R.string.edit_change), { pick(CropTarget.Avatar) }, size = NfButtonSize.Small, style = NfButtonStyle.Secondary)
                    if (m.avatar != null) NfButton(stringResource(R.string.edit_remove), { viewModel.removeAvatar() }, size = NfButtonSize.Small, style = NfButtonStyle.Quiet)
                }
            }

            Spacer(Modifier.height(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                NfTextField(
                    value = draft.name,
                    onValueChange = { v -> viewModel.update { it.copy(name = v) } },
                    label = stringResource(R.string.edit_name),
                    error = if (!draft.nameValid) stringResource(R.string.edit_name_invalid) else null,
                    maxLength = ProfileRules.NAME_MAX,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                )
                NfTextField(
                    value = draft.status,
                    onValueChange = { v -> viewModel.update { it.copy(status = v) } },
                    label = stringResource(R.string.edit_status),
                    placeholder = stringResource(R.string.edit_status_hint),
                    maxLength = ProfileRules.CATCHPHRASE_MAX,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
                NfTextField(
                    value = draft.bio,
                    onValueChange = { v -> viewModel.update { it.copy(bio = v) } },
                    label = stringResource(R.string.edit_bio),
                    placeholder = stringResource(R.string.edit_bio_hint),
                    singleLine = false,
                    minLines = 4,
                    maxLength = ProfileRules.BIO_MAX,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.edit_gender), style = Nf.type.labelSmall, color = Nf.colors.textSecondary)
                    NfSegmented(
                        options = Gender.entries.map { stringResource(it.labelRes()) },
                        selectedIndex = Gender.entries.indexOf(draft.gender),
                        onSelect = { i -> viewModel.update { it.copy(gender = Gender.entries[i]) } },
                    )
                }
                Field(
                    stringResource(R.string.edit_birth),
                    time.longDate(draft.birth),
                    error = if (!viewModel.isOldEnough(draft.birth)) stringResource(R.string.edit_too_young) else null,
                ) { pickBirth = true }
                Field(stringResource(R.string.edit_country), "${Countries.flag(draft.country)}  ${Countries.displayName(draft.country)}") { pickCountry = true }
                NfTextField(
                    value = draft.city,
                    onValueChange = { v -> viewModel.update { it.copy(city = v) } },
                    label = stringResource(R.string.edit_city),
                    placeholder = stringResource(R.string.edit_city_hint),
                    maxLength = 40,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                )
                Field(stringResource(R.string.edit_streak), time.longDate(draft.streakStart), helper = stringResource(R.string.edit_streak_body)) { pickStreak = true }
            }
            Spacer(Modifier.height(32.dp).windowInsetsPadding(WindowInsets.navigationBars))
        }
    }

    if (pickBirth) {
        NfDatePickerDialog(draft.birth, today.minusYears(100), today, onConfirm = { d ->
            viewModel.update { it.copy(birth = d) }
            pickBirth = false
        }, onDismiss = { pickBirth = false })
    }
    if (pickStreak) {
        NfDatePickerDialog(draft.streakStart, today.minusYears(30), today, onConfirm = { d ->
            viewModel.update { it.copy(streakStart = d) }
            pickStreak = false
        }, onDismiss = { pickStreak = false })
    }
    if (pickCountry) {
        CountryPickerSheet(setOf(draft.country), onDismiss = { pickCountry = false }, onConfirm = { set ->
            set.firstOrNull()?.let { c -> viewModel.update { it.copy(country = c) } }
            pickCountry = false
        }, multiSelect = false)
    }
    if (confirmDiscard) {
        NfConfirmDialog(
            title = stringResource(R.string.edit_discard_title),
            message = stringResource(R.string.edit_discard_body),
            confirmLabel = stringResource(R.string.edit_discard),
            dismissLabel = stringResource(R.string.edit_keep),
            onConfirm = {
                confirmDiscard = false
                onBack()
            },
            onDismiss = { confirmDiscard = false },
            destructive = true,
        )
    }
}

@Composable
private fun Field(label: String, value: String, error: String? = null, helper: String? = null, onClick: () -> Unit) {
    val haptics = LocalAppHaptics.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = Nf.type.labelSmall, color = Nf.colors.textSecondary)
        Row(
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(NfShapes.medium)
                .background(Nf.colors.surfaceRaised)
                .clickable(role = Role.Button) {
                    haptics.perform(HapticKind.Tap)
                    onClick()
                }
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) { Text(value, style = Nf.type.body, color = Nf.colors.textPrimary) }
        if (error != null) Text(error, style = Nf.type.labelSmall, color = Nf.colors.danger)
        else if (helper != null) Text(helper, style = Nf.type.labelSmall, color = Nf.colors.textTertiary)
    }
}
