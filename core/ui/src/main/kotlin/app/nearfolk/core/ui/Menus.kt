package app.nearfolk.core.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.nearfolk.core.designsystem.theme.Nf
import app.nearfolk.core.designsystem.theme.NfShapes

data class MenuAction(val label: String, val icon: ImageVector, val destructive: Boolean = false, val onClick: () -> Unit)

/** Overflow menu on a squircle surface. */
@Composable
fun NfMenu(expanded: Boolean, onDismiss: () -> Unit, actions: List<MenuAction>) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        shape = NfShapes.large,
        containerColor = Nf.colors.surfaceRaised,
        tonalElevation = 0.dp,
        shadowElevation = 6.dp,
    ) {
        actions.forEach { action ->
            val color = if (action.destructive) Nf.colors.danger else Nf.colors.textPrimary
            DropdownMenuItem(
                text = { Text(action.label, style = Nf.type.bodyStrong, color = color) },
                leadingIcon = { Icon(action.icon, null, tint = color, modifier = Modifier.size(20.dp)) },
                onClick = {
                    onDismiss()
                    action.onClick()
                },
            )
        }
    }
}
