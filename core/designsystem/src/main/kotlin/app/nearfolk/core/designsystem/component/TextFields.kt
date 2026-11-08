package app.nearfolk.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.nearfolk.core.designsystem.theme.Nf
import app.nearfolk.core.designsystem.theme.NfShapes

/**
 * Text input on a raised squircle well. The label sits above the field so
 * it never collides with the value, and the character count shows once the
 * value gets close to [maxLength].
 */
@Composable
fun NfTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    supportingText: String? = null,
    error: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLength: Int? = null,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    textStyle: TextStyle = Nf.type.body,
    minHeight: Dp = 52.dp,
) {
    val colors = Nf.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val borderColor by animateColorAsState(
        targetValue = when {
            error != null -> colors.danger
            focused -> colors.textSecondary
            else -> colors.surfaceRaised
        },
        label = "fieldBorder",
    )
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (label != null) {
            Text(label, style = Nf.type.labelSmall, color = colors.textSecondary)
        }
        BasicTextField(
            value = value,
            onValueChange = { next -> onValueChange(if (maxLength != null) next.take(maxLength) else next) },
            enabled = enabled,
            singleLine = singleLine,
            minLines = if (singleLine) 1 else minLines,
            textStyle = textStyle.copy(color = colors.textPrimary),
            cursorBrush = SolidColor(colors.accent),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            interactionSource = interaction,
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = minHeight)
                        .clip(NfShapes.medium)
                        .background(colors.surfaceRaised)
                        .border(1.dp, borderColor, NfShapes.medium)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
                ) {
                    if (leading != null) {
                        leading()
                        Spacer(Modifier.width(10.dp))
                    }
                    Box(Modifier.weight(1f)) {
                        if (value.isEmpty() && placeholder != null) {
                            Text(placeholder, style = textStyle, color = colors.textTertiary, maxLines = if (singleLine) 1 else Int.MAX_VALUE)
                        }
                        inner()
                    }
                    if (trailing != null) {
                        Spacer(Modifier.width(8.dp))
                        trailing()
                    }
                }
            },
        )
        val helper = error ?: supportingText
        val showCount = maxLength != null && value.length >= (maxLength * 0.8).toInt()
        if (helper != null || showCount) {
            Row(Modifier.fillMaxWidth()) {
                Text(
                    text = helper.orEmpty(),
                    style = Nf.type.labelSmall,
                    color = if (error != null) colors.danger else colors.textTertiary,
                    modifier = Modifier.weight(1f),
                )
                if (showCount) {
                    Text("${value.length}/$maxLength", style = Nf.type.labelSmall, color = colors.textTertiary)
                }
            }
        }
    }
}

@Composable
fun NfSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    clearDescription: String,
    modifier: Modifier = Modifier,
) {
    val colors = Nf.colors
    NfTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = placeholder,
        modifier = modifier,
        minHeight = 46.dp,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        leading = { Icon(Icons.Rounded.Search, contentDescription = null, tint = colors.textTertiary, modifier = Modifier.size(20.dp)) },
        trailing = if (query.isNotEmpty()) {
            {
                NfIconButton(
                    icon = Icons.Rounded.Close,
                    contentDescription = clearDescription,
                    onClick = { onQueryChange("") },
                    size = 28.dp,
                    iconSize = 18.dp,
                    tint = colors.textSecondary,
                    modifier = Modifier.heightIn(max = 28.dp),
                )
            }
        } else {
            null
        },
    )
}
