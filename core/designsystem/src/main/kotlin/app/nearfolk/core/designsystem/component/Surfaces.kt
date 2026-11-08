package app.nearfolk.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.nearfolk.core.designsystem.theme.HapticKind
import app.nearfolk.core.designsystem.theme.LocalAppHaptics
import app.nearfolk.core.designsystem.theme.Nf
import app.nearfolk.core.designsystem.theme.NfShapes

@Composable
fun NfDivider(modifier: Modifier = Modifier, inset: Boolean = false) {
    Box(
        modifier
            .fillMaxWidth()
            .padding(start = if (inset) 56.dp else 0.dp)
            .height(1.dp)
            .background(Nf.colors.line),
    )
}

/** Overline heading for a group of rows. */
@Composable
fun NfSectionHeader(text: String, modifier: Modifier = Modifier, action: (@Composable RowScope.() -> Unit)? = null) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = Nf.type.labelSmall, color = Nf.colors.textTertiary, modifier = Modifier.weight(1f))
        action?.invoke(this)
    }
}

/** Flat grouped container. Rows inside are separated by hairlines, not gaps. */
@Composable
fun NfGroup(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(NfShapes.large)
            .background(Nf.colors.surface),
        content = content,
    )
}

@Composable
fun NfListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconTint: Color? = null,
    titleColor: Color? = null,
    onClick: (() -> Unit)? = null,
    showChevron: Boolean = onClick != null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = Nf.colors
    val haptics = LocalAppHaptics.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .then(
                if (onClick != null) {
                    Modifier.clickable(role = Role.Button) {
                        haptics.perform(HapticKind.Tap)
                        onClick()
                    }
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (icon != null) {
            Box(
                Modifier
                    .size(28.dp)
                    .clip(NfShapes.small)
                    .background(colors.surfaceRaised),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = iconTint ?: colors.textSecondary, modifier = Modifier.size(18.dp))
            }
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = Nf.type.bodyStrong, color = titleColor ?: colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(subtitle, style = Nf.type.bodySmall, color = colors.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        trailing?.invoke()
        if (showChevron) {
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = colors.textTertiary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun NfSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    NfListRow(
        title = title,
        subtitle = subtitle,
        icon = icon,
        modifier = modifier,
        onClick = if (enabled) ({ onCheckedChange(!checked) }) else null,
        showChevron = false,
        trailing = { NfSwitch(checked = checked, onCheckedChange = null, enabled = enabled) },
    )
}

enum class NfNoticeTone { Neutral, Accent, Danger }

/** Inline notice for safety tips, limits and errors. Optional dismiss. */
@Composable
fun NfNotice(
    text: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    icon: ImageVector? = null,
    tone: NfNoticeTone = NfNoticeTone.Neutral,
    dismissDescription: String? = null,
    onDismiss: (() -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
) {
    val colors = Nf.colors
    val (container, content) = when (tone) {
        NfNoticeTone.Neutral -> colors.surfaceRaised to colors.textPrimary
        NfNoticeTone.Accent -> colors.accentSoft to colors.onAccentSoft
        NfNoticeTone.Danger -> colors.dangerSoft to colors.onDangerSoft
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(NfShapes.medium)
            .background(container)
            .padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = if (onDismiss != null) 6.dp else 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (icon != null) Icon(icon, null, tint = content, modifier = Modifier.padding(top = 1.dp).size(20.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (title != null) Text(title, style = Nf.type.label, color = content)
            Text(text, style = Nf.type.bodySmall, color = content)
            if (action != null) {
                Spacer(Modifier.height(6.dp))
                action()
            }
        }
        if (onDismiss != null) {
            NfIconButton(Icons.Rounded.Close, dismissDescription, onDismiss, size = 32.dp, iconSize = 18.dp, tint = content)
        }
    }
}

@Composable
fun NfEmptyState(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    val colors = Nf.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .size(64.dp)
                .clip(NfShapes.large)
                .background(colors.surfaceRaised),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = colors.textSecondary, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(title, style = Nf.type.titleSmall, color = colors.textPrimary, textAlign = TextAlign.Center)
        Text(body, style = Nf.type.bodySmall, color = colors.textSecondary, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 300.dp))
        if (action != null) {
            Spacer(Modifier.height(6.dp))
            action()
        }
    }
}
