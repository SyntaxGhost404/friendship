package app.nearfolk.core.designsystem.theme

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView

enum class HapticKind { Tap, Tick, Confirm, Reject, LongPress }

/** Haptic feedback that respects the in app toggle as well as the system setting. */
@Stable
interface AppHaptics {
    fun perform(kind: HapticKind)

    companion object {
        val None: AppHaptics = object : AppHaptics {
            override fun perform(kind: HapticKind) = Unit
        }
    }
}

@Stable
private class ViewHaptics(
    private val view: View,
    private val enabled: () -> Boolean,
) : AppHaptics {
    override fun perform(kind: HapticKind) {
        val target = view
        if (!enabled()) return
        val constant = when (kind) {
            HapticKind.Tap -> HapticFeedbackConstants.KEYBOARD_TAP
            HapticKind.Tick -> HapticFeedbackConstants.CLOCK_TICK
            HapticKind.LongPress -> HapticFeedbackConstants.LONG_PRESS
            HapticKind.Confirm ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM
                else HapticFeedbackConstants.VIRTUAL_KEY
            HapticKind.Reject ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.REJECT
                else HapticFeedbackConstants.LONG_PRESS
        }
        target.performHapticFeedback(constant)
    }
}

val LocalAppHaptics = staticCompositionLocalOf { AppHaptics.None }

@Composable
fun rememberAppHaptics(enabled: Boolean): AppHaptics {
    val view = LocalView.current
    val current = rememberUpdatedState(enabled)
    return remember(view) { ViewHaptics(view) { current.value } }
}
