package app.nearfolk.feature.journal

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.nearfolk.core.common.Passcode
import app.nearfolk.core.designsystem.component.pressScale
import app.nearfolk.core.designsystem.theme.HapticKind
import app.nearfolk.core.designsystem.theme.LocalAppHaptics
import app.nearfolk.core.designsystem.theme.Nf
import app.nearfolk.core.designsystem.theme.NfShapes
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlin.math.roundToInt

/**
 * Passcode entry: six squircle pips above a squircle keypad. A wrong code
 * shakes the pips on a spring and gives a reject haptic.
 */
@Composable
fun PasscodePad(
    title: String,
    body: String,
    entered: Int,
    message: PasscodeMessage?,
    shake: Flow<Unit>,
    onDigit: (Int) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalAppHaptics.current
    val offset = remember { Animatable(0f) }
    LaunchedEffect(shake) {
        shake.collect {
            haptics.perform(HapticKind.Reject)
            offset.snapTo(28f)
            offset.animateTo(0f, spring(dampingRatio = 0.22f, stiffness = 900f))
        }
    }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val locked = message as? PasscodeMessage.Locked
    LaunchedEffect(locked) {
        while (locked != null && System.currentTimeMillis() < locked.untilEpochMillis) {
            now = System.currentTimeMillis()
            delay(500)
        }
        now = System.currentTimeMillis()
    }
    val isLocked = locked != null && now < locked.untilEpochMillis
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = Nf.type.title, color = Nf.colors.textPrimary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(body, style = Nf.type.body, color = Nf.colors.textSecondary, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 300.dp))
        Spacer(Modifier.height(28.dp))
        val progress = stringResource(R.string.passcode_progress, entered)
        Row(
            Modifier
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .semantics { contentDescription = progress },
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            repeat(Passcode.LENGTH) { i ->
                val filled = i < entered
                val fill by animateColorAsState(if (filled) Nf.colors.textPrimary else Color.Transparent, label = "pip")
                Box(
                    Modifier
                        .size(16.dp)
                        .clip(NfShapes.badge)
                        .background(fill)
                        .border(1.5.dp, if (filled) Nf.colors.textPrimary else Nf.colors.lineStrong, NfShapes.badge),
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        val text = when (message) {
            null -> " "
            is PasscodeMessage.Wrong -> if (message.triesLeft > 0) pluralStringResource(R.plurals.passcode_wrong, message.triesLeft, message.triesLeft) else stringResource(R.string.passcode_wrong_plain)
            is PasscodeMessage.Locked -> if (isLocked) {
                val seconds = ((message.untilEpochMillis - now) / 1000).coerceAtLeast(1)
                stringResource(R.string.passcode_locked, "%d:%02d".format(seconds / 60, seconds % 60))
            } else {
                " "
            }
            PasscodeMessage.Mismatch -> stringResource(R.string.passcode_mismatch)
            PasscodeMessage.TooSimple -> stringResource(R.string.passcode_too_simple)
        }
        Text(text, style = Nf.type.bodySmall, color = Nf.colors.danger, textAlign = TextAlign.Center, modifier = Modifier.height(40.dp).widthIn(max = 300.dp))
        Spacer(Modifier.height(12.dp))
        val rows = listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9), listOf(-1, 0, -2))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    row.forEach { key ->
                        when (key) {
                            -1 -> Spacer(Modifier.size(76.dp))
                            -2 -> Key(enabled = entered > 0, description = stringResource(R.string.passcode_delete_digit), onClick = onBackspace) {
                                Icon(Icons.AutoMirrored.Rounded.Backspace, null, tint = Nf.colors.textPrimary)
                            }
                            else -> Key(enabled = !isLocked, description = stringResource(R.string.passcode_digit, key), onClick = { onDigit(key) }) {
                                Text(key.toString(), style = Nf.type.headline, color = Nf.colors.textPrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Key(enabled: Boolean, description: String, onClick: () -> Unit, content: @Composable () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val haptics = LocalAppHaptics.current
    Box(
        Modifier
            .pressScale(interaction, pressed = 0.9f)
            .size(76.dp)
            .clip(NfShapes.large)
            .background(Nf.colors.surface)
            .clickable(interactionSource = interaction, indication = ripple(), enabled = enabled) {
                haptics.perform(HapticKind.Tap)
                onClick()
            }
            .semantics { contentDescription = description }
            .padding(4.dp),
        contentAlignment = Alignment.Center,
    ) { content() }
}

