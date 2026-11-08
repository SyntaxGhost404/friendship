package app.nearfolk.feature.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PanTool
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.nearfolk.core.designsystem.component.NfButton
import app.nearfolk.core.designsystem.component.NfButtonStyle
import app.nearfolk.core.designsystem.component.NfBottomSheet
import app.nearfolk.core.designsystem.component.NfChoiceMark
import app.nearfolk.core.designsystem.component.NfIconButton
import app.nearfolk.core.designsystem.component.NfSegmented
import app.nearfolk.core.designsystem.component.NfTextField
import app.nearfolk.core.designsystem.theme.HapticKind
import app.nearfolk.core.designsystem.theme.LocalAppHaptics
import app.nearfolk.core.designsystem.theme.Nf
import app.nearfolk.core.designsystem.theme.NfMotion
import app.nearfolk.core.designsystem.theme.NfShapes
import app.nearfolk.core.model.Countries
import app.nearfolk.core.model.Gender
import app.nearfolk.core.ui.AppDocument
import app.nearfolk.core.ui.CountryPickerSheet
import app.nearfolk.core.ui.DocumentBody
import app.nearfolk.core.ui.NfDatePickerDialog
import app.nearfolk.core.ui.labelRes
import app.nearfolk.core.ui.rememberTimeFormat
import androidx.compose.material.icons.automirrored.rounded.ArrowBack

private const val STEP_WELCOME = 0
private const val STEP_ABOUT = 1
private const val STEP_BIRTH = 2
private const val STEP_RULES = 3
private const val STEP_NOTIFY = 4

@Composable
fun OnboardingRoute(viewModel: OnboardingViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val stored by viewModel.storedProfileName.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val needsNotificationStep = remember {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    }
    val lastStep = if (needsNotificationStep) STEP_NOTIFY else STEP_RULES
    OnboardingScreen(
        state = state,
        storedProfileName = stored,
        lastStep = lastStep,
        isOldEnough = viewModel::isOldEnough,
        today = viewModel.today,
        onStep = viewModel::setStep,
        onName = viewModel::setName,
        onGender = viewModel::setGender,
        onBirth = viewModel::setBirthDate,
        onCountry = viewModel::setCountry,
        onAgree = viewModel::setAgreed,
        onContinueStored = viewModel::continueStored,
        onCreate = viewModel::create,
    )
}

@Composable
fun OnboardingScreen(
    state: OnboardingState,
    storedProfileName: String?,
    lastStep: Int,
    isOldEnough: (java.time.LocalDate) -> Boolean,
    today: java.time.LocalDate,
    onStep: (Int) -> Unit,
    onName: (String) -> Unit,
    onGender: (Gender) -> Unit,
    onBirth: (java.time.LocalDate) -> Unit,
    onCountry: (String) -> Unit,
    onAgree: (Boolean) -> Unit,
    onContinueStored: () -> Unit,
    onCreate: () -> Unit,
) {
    BackHandler(enabled = state.step > STEP_WELCOME) { onStep(state.step - 1) }
    Box(
        Modifier
            .fillMaxSize()
            .background(Nf.colors.canvas)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        AnimatedContent(
            targetState = state.step,
            transitionSpec = {
                val forward = targetState > initialState
                (slideInHorizontally(NfMotion.slide) { if (forward) it / 4 else -it / 4 } + fadeIn()) togetherWith
                    (slideOutHorizontally(NfMotion.slide) { if (forward) -it / 4 else it / 4 } + fadeOut())
            },
            label = "onboardingStep",
        ) { step ->
            when (step) {
                STEP_WELCOME -> Welcome(storedProfileName, onCreate = { onStep(STEP_ABOUT) }, onContinueStored = onContinueStored)
                STEP_ABOUT -> StepScaffold(
                    step = 1,
                    total = lastStep,
                    title = stringResource(R.string.onb_about_title),
                    body = stringResource(R.string.onb_about_body),
                    canContinue = state.nameValid && state.gender != null,
                    onBack = { onStep(STEP_WELCOME) },
                    onContinue = { onStep(STEP_BIRTH) },
                ) { AboutFields(state, onName, onGender) }
                STEP_BIRTH -> StepScaffold(
                    step = 2,
                    total = lastStep,
                    title = stringResource(R.string.onb_birth_title),
                    body = stringResource(R.string.onb_birth_body),
                    canContinue = state.birthDate?.let(isOldEnough) == true && state.countryCode != null,
                    onBack = { onStep(STEP_ABOUT) },
                    onContinue = { onStep(STEP_RULES) },
                ) { BirthFields(state, isOldEnough, today, onBirth, onCountry) }
                STEP_RULES -> StepScaffold(
                    step = 3,
                    total = lastStep,
                    title = stringResource(R.string.onb_rules_title),
                    body = stringResource(R.string.onb_rules_body),
                    canContinue = state.agreed,
                    loading = state.creating,
                    continueLabel = if (lastStep == STEP_RULES) stringResource(R.string.onb_finish) else null,
                    onBack = { onStep(STEP_BIRTH) },
                    onContinue = { if (lastStep == STEP_RULES) onCreate() else onStep(STEP_NOTIFY) },
                ) { Rules(state.agreed, onAgree) }
                else -> NotifyStep(onDone = onCreate, creating = state.creating, onBack = { onStep(STEP_RULES) })
            }
        }
    }
}

@Composable
private fun Welcome(storedProfileName: String?, onCreate: () -> Unit, onContinueStored: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Spacer(Modifier.weight(1f))
        BrandMark()
        Spacer(Modifier.height(36.dp))
        Text(stringResource(R.string.onb_app_name), style = Nf.type.display, color = Nf.colors.textPrimary)
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.onb_tagline), style = Nf.type.title.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Normal), color = Nf.colors.textSecondary)
        Spacer(Modifier.weight(1.2f))
        NfButton(stringResource(R.string.onb_create), onCreate, modifier = Modifier.fillMaxWidth())
        if (storedProfileName != null) {
            Spacer(Modifier.height(10.dp))
            NfButton(
                stringResource(R.string.onb_continue_as, storedProfileName),
                onContinueStored,
                style = NfButtonStyle.Secondary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Two overlapping squircles: the mark, drawn large. */
@Composable
private fun BrandMark() {
    Box(Modifier.size(width = 132.dp, height = 96.dp)) {
        Box(
            Modifier
                .size(84.dp)
                .clip(NfShapes.extraLarge)
                .background(Nf.colors.accent),
        )
        Box(
            Modifier
                .offset(x = 48.dp, y = 12.dp)
                .size(84.dp)
                .clip(NfShapes.extraLarge)
                .background(Nf.colors.ink.copy(alpha = 0.88f)),
        )
    }
}

@Composable
private fun StepScaffold(
    step: Int,
    total: Int,
    title: String,
    body: String,
    canContinue: Boolean,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    loading: Boolean = false,
    continueLabel: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            NfIconButton(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.onb_back), onBack)
            Spacer(Modifier.weight(1f))
            StepDots(step, total)
            Spacer(Modifier.width(12.dp))
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(12.dp))
            Text(title, style = Nf.type.headline, color = Nf.colors.textPrimary)
            Spacer(Modifier.height(8.dp))
            Text(body, style = Nf.type.body, color = Nf.colors.textSecondary)
            Spacer(Modifier.height(28.dp))
            content()
            Spacer(Modifier.height(24.dp))
        }
        NfButton(
            continueLabel ?: stringResource(R.string.onb_next),
            onContinue,
            enabled = canContinue,
            loading = loading,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
        )
    }
}

@Composable
private fun StepDots(step: Int, total: Int) {
    val description = stringResource(R.string.onb_step, step, total)
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.semantics { contentDescription = description },
    ) {
        for (i in 1..total) {
            val width by androidx.compose.animation.core.animateDpAsState(if (i == step) 22.dp else 8.dp, NfMotion.glide(), label = "dot")
            Box(
                Modifier
                    .height(8.dp)
                    .width(width)
                    .clip(NfShapes.badge)
                    .background(if (i <= step) Nf.colors.textPrimary else Nf.colors.lineStrong),
            )
        }
    }
}

@Composable
private fun AboutFields(state: OnboardingState, onName: (String) -> Unit, onGender: (Gender) -> Unit) {
    NfTextField(
        value = state.name,
        onValueChange = onName,
        label = stringResource(R.string.onb_name_label),
        placeholder = stringResource(R.string.onb_name_hint),
        error = if (state.name.isNotEmpty() && state.name.trim().length < 2) stringResource(R.string.onb_name_short) else null,
        maxLength = 30,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
    )
    Spacer(Modifier.height(24.dp))
    Text(stringResource(R.string.onb_gender_label), style = Nf.type.labelSmall, color = Nf.colors.textSecondary)
    Spacer(Modifier.height(6.dp))
    val genders = Gender.entries
    NfSegmented(
        options = genders.map { stringResource(it.labelRes()) },
        selectedIndex = state.gender?.let(genders::indexOf) ?: -1,
        onSelect = { onGender(genders[it]) },
    )
}

@Composable
private fun BirthFields(
    state: OnboardingState,
    isOldEnough: (java.time.LocalDate) -> Boolean,
    today: java.time.LocalDate,
    onBirth: (java.time.LocalDate) -> Unit,
    onCountry: (String) -> Unit,
) {
    val time = rememberTimeFormat()
    var showDate by rememberSaveable { mutableStateOf(false) }
    var showCountry by rememberSaveable { mutableStateOf(false) }
    PickerField(
        label = stringResource(R.string.onb_birth_label),
        value = state.birthDate?.let(time::longDate) ?: stringResource(R.string.onb_birth_pick),
        hasValue = state.birthDate != null,
        error = state.birthDate?.takeIf { !isOldEnough(it) }?.let { stringResource(R.string.onb_too_young) },
        onClick = { showDate = true },
    )
    Spacer(Modifier.height(20.dp))
    PickerField(
        label = stringResource(R.string.onb_country_label),
        value = state.countryCode?.let { "${Countries.flag(it)}  ${Countries.displayName(it)}" } ?: stringResource(R.string.onb_country_pick),
        hasValue = state.countryCode != null,
        onClick = { showCountry = true },
    )
    if (showDate) {
        NfDatePickerDialog(
            initial = state.birthDate ?: today.minusYears(25),
            min = today.minusYears(100),
            max = today,
            onConfirm = {
                onBirth(it)
                showDate = false
            },
            onDismiss = { showDate = false },
        )
    }
    if (showCountry) {
        CountryPickerSheet(
            selected = setOfNotNull(state.countryCode),
            onDismiss = { showCountry = false },
            onConfirm = {
                it.firstOrNull()?.let(onCountry)
                showCountry = false
            },
            multiSelect = false,
        )
    }
}

@Composable
internal fun PickerField(label: String, value: String, hasValue: Boolean, onClick: () -> Unit, error: String? = null) {
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
        ) {
            Text(value, style = Nf.type.body, color = if (hasValue) Nf.colors.textPrimary else Nf.colors.textTertiary)
        }
        if (error != null) Text(error, style = Nf.type.labelSmall, color = Nf.colors.danger)
    }
}

@Composable
private fun Rules(agreed: Boolean, onAgree: (Boolean) -> Unit) {
    var document by remember { mutableStateOf<AppDocument?>(null) }
    val haptics = LocalAppHaptics.current
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Rule(Icons.Rounded.Favorite, stringResource(R.string.onb_rule_kind_title), stringResource(R.string.onb_rule_kind_body))
        Rule(Icons.Rounded.VerifiedUser, stringResource(R.string.onb_rule_adults_title), stringResource(R.string.onb_rule_adults_body))
        Rule(Icons.Rounded.PanTool, stringResource(R.string.onb_rule_consent_title), stringResource(R.string.onb_rule_consent_body))
        Rule(Icons.Rounded.Lock, stringResource(R.string.onb_rule_safe_title), stringResource(R.string.onb_rule_safe_body))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LinkText(stringResource(R.string.onb_read_full)) { document = AppDocument.Guidelines }
            LinkText(stringResource(R.string.onb_read_privacy)) { document = AppDocument.Privacy }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .clip(NfShapes.medium)
                .background(Nf.colors.surface)
                .clickable(role = Role.Checkbox) {
                    haptics.perform(if (!agreed) HapticKind.Confirm else HapticKind.Tick)
                    onAgree(!agreed)
                }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            NfChoiceMark(agreed)
            Text(stringResource(R.string.onb_agree), style = Nf.type.bodyStrong, color = Nf.colors.textPrimary)
        }
    }
    document?.let { doc ->
        NfBottomSheet(onDismiss = { document = null }, title = stringResource(doc.title)) {
            DocumentBody(doc, Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp))
        }
    }
}

@Composable
private fun LinkText(text: String, onClick: () -> Unit) {
    Text(
        text,
        style = Nf.type.label,
        color = Nf.colors.accent,
        modifier = Modifier.clip(NfShapes.small).clickable(role = Role.Button, onClick = onClick).padding(vertical = 4.dp),
    )
}

@Composable
private fun Rule(icon: ImageVector, title: String, body: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(
            Modifier
                .size(40.dp)
                .clip(NfShapes.medium)
                .background(Nf.colors.surfaceRaised),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = Nf.colors.textPrimary, modifier = Modifier.size(20.dp)) }
        Column(Modifier.weight(1f)) {
            Text(title, style = Nf.type.titleSmall, color = Nf.colors.textPrimary)
            Text(body, style = Nf.type.bodySmall, color = Nf.colors.textSecondary)
        }
    }
}

@Composable
private fun NotifyStep(onDone: () -> Unit, creating: Boolean, onBack: () -> Unit) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { onDone() }
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        NfIconButton(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.onb_back), onBack, modifier = Modifier.offset(x = (-12).dp))
        Spacer(Modifier.weight(1f))
        Box(
            Modifier
                .size(72.dp)
                .clip(NfShapes.large)
                .background(Nf.colors.accentSoft),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.NotificationsActive, null, tint = Nf.colors.onAccentSoft, modifier = Modifier.size(32.dp)) }
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.onb_notify_title), style = Nf.type.headline, color = Nf.colors.textPrimary)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.onb_notify_body), style = Nf.type.body, color = Nf.colors.textSecondary)
        Spacer(Modifier.weight(1.4f))
        NfButton(
            stringResource(R.string.onb_notify_allow),
            onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) launcher.launch(Manifest.permission.POST_NOTIFICATIONS) else onDone()
            },
            loading = creating,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        NfButton(stringResource(R.string.onb_notify_skip), onDone, style = NfButtonStyle.Quiet, modifier = Modifier.fillMaxWidth())
    }
}
