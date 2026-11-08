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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
 * Confirmation dialog. Buttons stack full width so long labels such as
 * "Delete chat" never wrap, and the destructive action sits closest to the thumb.
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
        Column(
            Modifier
                .padding(horizontal = 28.dp)
                .fillMaxWidth()
                .clip(NfShapes.extraLarge)
                .background(Nf.colors.surface)
                .padding(24.dp),
        ) {
            Text(title, style = Nf.type.title, color = Nf.colors.textPrimary)
            Spacer(Modifier.height(10.dp))
            Text(message, style = Nf.type.body, color = Nf.colors.textSecondary)
            if (content != null) {
                Spacer(Modifier.height(16.dp))
                content()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NfBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        sheetState = sheetState,
        shape = NfShapes.sheet,
        containerColor = Nf.colors.surface,
        contentColor = Nf.colors.textPrimary,
        scrimColor = Nf.colors.scrim,
        dragHandle = {
            Box(Modifier.padding(top = 10.dp, bottom = 6.dp)) {
                Box(
                    Modifier
                        .width(36.dp)
                        .height(5.dp)
                        .clip(NfShapes.badge)
                        .background(Nf.colors.lineStrong),
                )
            }
        },
    ) {
        Column(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
            if (title != null) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = Nf.type.title, color = Nf.colors.textPrimary)
                }
            }
            content()
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
