package app.nearfolk.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.nearfolk.core.designsystem.component.NfIconButton
import app.nearfolk.core.designsystem.component.NfPlusMark
import app.nearfolk.core.designsystem.component.NfTag
import app.nearfolk.core.designsystem.component.pressScale
import app.nearfolk.core.designsystem.theme.HapticKind
import app.nearfolk.core.designsystem.theme.LocalAppHaptics
import app.nearfolk.core.designsystem.theme.Nf
import app.nearfolk.core.designsystem.theme.NfShapes
import app.nearfolk.core.model.Countries
import app.nearfolk.core.model.Member
import app.nearfolk.core.model.Presence
import java.time.Instant
import java.time.LocalDate

/**
 * Dense member row for Discover: avatar with presence, name, age, gender and
 * sign, a two line bio and exact last seen time. Flat on the canvas with a
 * hairline below, so dozens of people stay scannable.
 */
@Composable
fun MemberRow(
    member: Member,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    now: Instant = Instant.now(),
    today: LocalDate = LocalDate.now(),
) {
    val time = rememberTimeFormat()
    val interaction = remember { MutableInteractionSource() }
    val haptics = LocalAppHaptics.current
    val online = member.presence is Presence.Online
    Row(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(interaction, pressed = 0.985f)
            .clip(NfShapes.large)
            .clickable(interactionSource = interaction, indication = androidx.compose.material3.ripple(), role = Role.Button) {
                haptics.perform(HapticKind.Tap)
                onClick()
            }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        MemberAvatar(member.displayName, member.avatar, 58.dp, online = online)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        member.displayName,
                        style = Nf.type.titleSmall,
                        color = Nf.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (member.isPlus) NfPlusMark()
                }
                Text(
                    time.presenceShort(member.presence, now),
                    style = Nf.type.labelSmall,
                    color = if (online) Nf.colors.online else Nf.colors.textTertiary,
                    maxLines = 1,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "${member.ageOn(today)}  ·  ${stringResource(member.gender.labelRes())}",
                    style = Nf.type.bodySmall,
                    color = Nf.colors.textSecondary,
                    maxLines = 1,
                )
                NfTag(stringResource(member.zodiac.labelRes()))
                Text(
                    "${Countries.flag(member.countryCode)} ${member.city ?: Countries.displayName(member.countryCode)}",
                    style = Nf.type.bodySmall,
                    color = Nf.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
            val snippet = member.bio.ifBlank { member.catchphrase }
            if (snippet.isNotBlank()) {
                Text(
                    snippet,
                    style = Nf.type.bodySmall,
                    color = Nf.colors.textPrimary.copy(alpha = 0.86f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

/** Compact player for a voice intro: play button and a progress track. */
@Composable
fun VoiceIntroBar(
    durationMillis: Long,
    playing: Boolean,
    positionMillis: Long,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Nf.colors
    Row(
        modifier
            .fillMaxWidth()
            .clip(NfShapes.medium)
            .background(colors.surfaceRaised)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        NfIconButton(
            if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            stringResource(if (playing) R.string.ui_voice_pause else R.string.ui_voice_play),
            onToggle,
            style = app.nearfolk.core.designsystem.component.NfButtonStyle.Primary,
            size = 40.dp,
        )
        val progress = if (durationMillis > 0) (positionMillis.toFloat() / durationMillis).coerceIn(0f, 1f) else 0f
        Waveform(progress, Modifier.weight(1f).height(28.dp))
        Text(formatDuration(if (playing || positionMillis > 0) positionMillis else durationMillis), style = Nf.type.labelSmall, color = colors.textSecondary)
    }
}

/** Static bar pattern that fills as playback moves. */
@Composable
fun Waveform(progress: Float, modifier: Modifier = Modifier, levels: List<Float>? = null) {
    val colors = Nf.colors
    val bars = levels ?: remember { List(32) { i -> 0.3f + 0.7f * kotlin.math.abs(kotlin.math.sin(i * 1.7f) * kotlin.math.cos(i * 0.6f)) } }
    androidx.compose.foundation.Canvas(modifier) {
        val gap = size.width / bars.size
        val barWidth = gap * 0.55f
        bars.forEachIndexed { i, level ->
            val h = size.height * level.coerceIn(0.08f, 1f)
            val x = i * gap
            drawRoundRect(
                color = if ((i + 0.5f) / bars.size <= progress) colors.accent else colors.lineStrong,
                topLeft = androidx.compose.ui.geometry.Offset(x, (size.height - h) / 2),
                size = androidx.compose.ui.geometry.Size(barWidth, h),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2),
            )
        }
    }
}

fun formatDuration(millis: Long): String {
    val total = (millis / 1000).coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}

/** The day counter carried over from the original concept, shown big and calm. */
@Composable
fun StreakCounter(days: Long, since: LocalDate, modifier: Modifier = Modifier) {
    val time = rememberTimeFormat()
    Row(
        modifier
            .clip(NfShapes.large)
            .background(Nf.colors.surface)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(days.toString(), style = Nf.type.display, color = Nf.colors.textPrimary)
        Column {
            Text(stringResource(R.string.ui_streak_days), style = Nf.type.label, color = Nf.colors.textPrimary)
            Text(stringResource(R.string.ui_streak_since, time.shortDate(since)), style = Nf.type.labelSmall, color = Nf.colors.textSecondary)
        }
    }
}

@Composable
fun SquareTile(content: @Composable () -> Unit, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier
            .aspectRatio(1f)
            .pressScale(interaction)
            .clip(NfShapes.medium)
            .clickable(interactionSource = interaction, indication = androidx.compose.material3.ripple(), onClick = onClick),
    ) { content() }
}
