package app.nearfolk.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import app.nearfolk.core.designsystem.component.NfSnackbar

/** One snackbar host for the whole app, provided by the root scaffold. */
val LocalSnackbarHost = staticCompositionLocalOf { SnackbarHostState() }

@Composable
fun NfSnackbarHost(state: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(state, modifier) { data ->
        Box(Modifier.fillMaxWidth()) {
            NfSnackbar(
                message = data.visuals.message,
                actionLabel = data.visuals.actionLabel,
                onAction = { data.performAction() },
            )
        }
    }
}
