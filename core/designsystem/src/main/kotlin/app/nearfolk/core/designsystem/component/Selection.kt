package app.nearfolk.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.nearfolk.core.designsystem.theme.HapticKind
import app.nearfolk.core.designsystem.theme.LocalAppHaptics
import app.nearfolk.core.designsystem.theme.Nf
import app.nearfolk.core.designsystem.theme.NfMotion
import app.nearfolk.core.designsystem.theme.NfShapes

@Composable
fun NfChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
) {
    val colors = Nf.colors
    val container by animateColorAsState(if (selected) colors.ink else colors.surfaceRaised, label = "chipContainer")
    val content by animateColorAsState(if (selected) colors.onInk else colors.textPrimary, label = "chipContent")
    val interaction = remember { MutableInteractionSource() }
    val haptics = LocalAppHaptics.current
    Row(
        modifier = modifier
            .pressScale(interaction)
            .height(34.dp)
            .clip(NfShapes.small)
            .background(container)
            .selectable(
                selected = selected,
                interactionSource = interaction,
                indication = ripple(color = content),
                role = Role.Tab,
                onClick = {
                    haptics.perform(HapticKind.Tick)
                    onClick()
                },
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (leadingIcon != null) Icon(leadingIcon, null, tint = content, modifier = Modifier.size(16.dp))
        Text(label, style = Nf.type.label, color = content, maxLines = 1)
    }
}

/**
 * Segmented control with a squircle thumb that glides between options on a
 * spring. Options share the width evenly.
 */
@Composable
fun NfSegmented(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Nf.colors
    val haptics = LocalAppHaptics.current
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(NfShapes.medium)
            .background(colors.surfaceRaised)
            .padding(3.dp),
    ) {
        val segment = maxWidth / options.size.coerceAtLeast(1)
        val offset by animateDpAsState(segment * selectedIndex.coerceAtLeast(0), NfMotion.glide(), label = "segmentThumb")
        if (selectedIndex >= 0) Box(
            Modifier
                .offset { IntOffset(offset.roundToPx(), 0) }
                .width(segment)
                .fillMaxHeight()
                .clip(NfShapes.small)
                .background(colors.surfaceTop)
                .border(1.dp, if (colors.isDark) colors.surfaceTop else colors.line, NfShapes.small),
        )
        Row(Modifier.fillMaxWidth().fillMaxHeight().selectableGroup()) {
            options.forEachIndexed { index, option ->
                val selected = index == selectedIndex
                val textColor by animateColorAsState(if (selected) colors.textPrimary else colors.textSecondary, label = "segmentText")
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(NfShapes.small)
                        .selectable(
                            selected = selected,
                            role = Role.RadioButton,
                            onClick = {
                                if (!selected) haptics.perform(HapticKind.Tick)
                                onSelect(index)
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        option,
                        style = Nf.type.label,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

/**
 * Switches keep the stadium track: a squircle there would be harder to read
 * as on or off at a glance.
 */
@Composable
fun NfSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val colors = Nf.colors
    val haptics = LocalAppHaptics.current
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange?.let { callback ->
            { value: Boolean ->
                haptics.perform(if (value) HapticKind.Confirm else HapticKind.Tick)
                callback(value)
            }
        },
        enabled = enabled,
        modifier = modifier,
        colors = SwitchDefaults.colors(
            checkedThumbColor = colors.onAccent,
            checkedTrackColor = colors.accent,
            checkedBorderColor = Color.Transparent,
            uncheckedThumbColor = colors.textSecondary,
            uncheckedTrackColor = colors.surfaceRaised,
            uncheckedBorderColor = colors.lineStrong,
        ),
    )
}

/** Radio style choice drawn as a squircle tick box. */
@Composable
fun NfChoiceMark(selected: Boolean, modifier: Modifier = Modifier) {
    val colors = Nf.colors
    val fill by animateColorAsState(if (selected) colors.accent else Color.Transparent, label = "choiceFill")
    val stroke by animateColorAsState(if (selected) colors.accent else colors.lineStrong, label = "choiceStroke")
    Box(
        modifier
            .size(22.dp)
            .clip(NfShapes.small)
            .background(fill)
            .border(1.5.dp, stroke, NfShapes.small),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(NfShapes.badge)
                    .background(colors.onAccent),
            )
        }
    }
}

@Composable
fun NfChoiceRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    val haptics = LocalAppHaptics.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(NfShapes.medium)
            .clickable(role = Role.RadioButton) {
                haptics.perform(HapticKind.Tick)
                onClick()
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        NfChoiceMark(selected)
        androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
            Text(title, style = Nf.type.bodyStrong, color = Nf.colors.textPrimary)
            if (subtitle != null) Text(subtitle, style = Nf.type.bodySmall, color = Nf.colors.textSecondary)
        }
    }
}
