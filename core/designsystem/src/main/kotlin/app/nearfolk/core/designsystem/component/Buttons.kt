package app.nearfolk.core.designsystem.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.nearfolk.core.designsystem.theme.HapticKind
import app.nearfolk.core.designsystem.theme.LocalAppHaptics
import app.nearfolk.core.designsystem.theme.Nf
import app.nearfolk.core.designsystem.theme.NfShapes

enum class NfButtonStyle { Primary, Secondary, Outline, Quiet, Danger }

enum class NfButtonSize(val height: Dp, val horizontalPadding: Dp, val iconSize: Dp) {
    Large(52.dp, 22.dp, 20.dp),
    Medium(44.dp, 18.dp, 18.dp),
    Small(36.dp, 14.dp, 16.dp),
}

private data class ButtonColors(val container: Color, val content: Color, val border: Color?)

@Composable
private fun colorsFor(style: NfButtonStyle): ButtonColors {
    val c = Nf.colors
    return when (style) {
        NfButtonStyle.Primary -> ButtonColors(c.accent, c.onAccent, null)
        NfButtonStyle.Secondary -> ButtonColors(c.surfaceRaised, c.textPrimary, null)
        NfButtonStyle.Outline -> ButtonColors(Color.Transparent, c.textPrimary, c.lineStrong)
        NfButtonStyle.Quiet -> ButtonColors(Color.Transparent, c.textPrimary, null)
        NfButtonStyle.Danger -> ButtonColors(c.danger, c.onDanger, null)
    }
}

@Composable
fun NfButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: NfButtonStyle = NfButtonStyle.Primary,
    size: NfButtonSize = NfButtonSize.Large,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null,
) {
    val colors = colorsFor(style)
    val interaction = remember { MutableInteractionSource() }
    val haptics = LocalAppHaptics.current
    val shape = if (size == NfButtonSize.Small) NfShapes.small else NfShapes.medium
    Box(
        modifier = modifier
            .pressScale(interaction)
            .defaultMinSize(minHeight = size.height)
            .clip(shape)
            .background(colors.container)
            .then(if (colors.border != null) Modifier.border(1.dp, colors.border, shape) else Modifier)
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = colors.content),
                enabled = enabled && !loading,
                role = Role.Button,
                onClick = {
                    haptics.perform(HapticKind.Tap)
                    onClick()
                },
            )
            .alpha(if (enabled) 1f else 0.45f)
            .padding(PaddingValues(horizontal = size.horizontalPadding)),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = loading,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "buttonLoading",
        ) { isLoading ->
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(size.iconSize),
                    color = colors.content,
                    strokeWidth = 2.dp,
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (leadingIcon != null) {
                        Icon(leadingIcon, contentDescription = null, tint = colors.content, modifier = Modifier.size(size.iconSize))
                    }
                    Text(
                        text = text,
                        style = Nf.type.label,
                        color = colors.content,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** Square icon button with a squircle hit area. */
@Composable
fun NfIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: NfButtonStyle = NfButtonStyle.Quiet,
    size: Dp = 40.dp,
    iconSize: Dp = 22.dp,
    tint: Color? = null,
    enabled: Boolean = true,
) {
    val colors = colorsFor(style)
    val interaction = remember { MutableInteractionSource() }
    val haptics = LocalAppHaptics.current
    Box(
        modifier = modifier
            .pressScale(interaction, pressed = 0.9f)
            .size(size)
            .clip(NfShapes.medium)
            .background(colors.container)
            .then(if (colors.border != null) Modifier.border(1.dp, colors.border, NfShapes.medium) else Modifier)
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = colors.content),
                enabled = enabled,
                role = Role.Button,
                onClick = {
                    haptics.perform(HapticKind.Tap)
                    onClick()
                },
            )
            .alpha(if (enabled) 1f else 0.4f),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint ?: colors.content, modifier = Modifier.size(iconSize))
    }
}
