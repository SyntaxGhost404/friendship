package app.nearfolk.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.nearfolk.core.designsystem.theme.Nf
import app.nearfolk.core.designsystem.theme.NfShapes

/**
 * Full screen dialog for media viewers. The content draws its own background
 * and follows the popup rules in [NfPopupContent].
 */
@Composable
fun NfFullScreenDialog(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        NfPopupContent(content)
    }
}

/**
 * Confirmation dialog. Buttons stack full width so long labels such as
 * "Delete chat" never wrap, and the destructive action sits closest to the thumb.
 * The text scrolls when it does not fit; the buttons always stay on screen.
 */
@Composable
fun NfConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
    loading: Boolean = false,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        NfPopupContent {
            Column(
                Modifier
                    .padding(horizontal = 28.dp, vertical = 24.dp)
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
                    .shapeSurface(Nf.colors.surface, NfShapes.extraLarge)
                    .padding(24.dp),
            ) {
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                    Text(title, style = Nf.type.title, color = Nf.colors.textPrimary)
                    Spacer(Modifier.height(10.dp))
                    Text(message, style = Nf.type.body, color = Nf.colors.textSecondary)
                    if (content != null) {
                        Spacer(Modifier.height(16.dp))
                        content()
                    }
                }
                Spacer(Modifier.height(24.dp))
                NfButton(
                    text = confirmLabel,
                    onClick = onConfirm,
                    style = if (destructive) NfButtonStyle.Danger else NfButtonStyle.Primary,
                    loading = loading,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                NfButton(
                    text = dismissLabel,
                    onClick = onDismiss,
                    style = NfButtonStyle.Secondary,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
fun NfSpinner(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        androidx.compose.material3.CircularProgressIndicator(
            color = Nf.colors.textSecondary,
            strokeWidth = 2.dp,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
fun NfSnackbar(message: String, modifier: Modifier = Modifier, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(NfShapes.medium)
            .background(Nf.colors.ink)
            .padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp)
            .height(40.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(message, style = Nf.type.label, color = Nf.colors.onInk, modifier = Modifier.weight(1f), maxLines = 2)
        if (actionLabel != null && onAction != null) {
            Text(
                actionLabel,
                style = Nf.type.label,
                color = Nf.colors.onInk,
                modifier = Modifier
                    .clip(NfShapes.small)
                    .background(Nf.colors.onInk.copy(alpha = 0.12f))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}
