package app.nearfolk

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.nearfolk.core.designsystem.theme.Nf
import app.nearfolk.core.model.AccountState
import app.nearfolk.core.ui.AppInfo
import app.nearfolk.core.ui.LocalAppInfo
import app.nearfolk.core.ui.LocalSnackbarHost
import app.nearfolk.core.ui.NfSnackbarHost
import app.nearfolk.feature.onboarding.OnboardingRoute
import app.nearfolk.feature.settings.DeleteAccountRouteScreen
import app.nearfolk.feature.settings.ReactivateScreen
import app.nearfolk.navigation.MainScaffold

/** Chooses between onboarding, the reactivation screen and the main app. */
@Composable
fun NearfolkRoot(viewModel: MainViewModel) {
    val account by viewModel.accountState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val info = remember { AppInfo(BuildConfig.VERSION_NAME, BuildConfig.SUPPORT_EMAIL) }
    CompositionLocalProvider(LocalSnackbarHost provides snackbar, LocalAppInfo provides info) {
        Box(Modifier.fillMaxSize().background(Nf.colors.canvas)) {
            AnimatedContent(
                targetState = account,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                contentKey = { it?.name },
                label = "accountState",
            ) { state ->
                when (state) {
                    null -> Box(Modifier.fillMaxSize())
                    AccountState.SignedOut -> OnboardingRoute()
                    AccountState.Deactivated -> DeactivatedFlow()
                    AccountState.Active -> MainScaffold(viewModel)
                }
            }
            if (account != AccountState.Active) {
                NfSnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.navigationBars))
            }
        }
    }
}

@Composable
private fun DeactivatedFlow() {
    var deleting by rememberSaveable { mutableStateOf(false) }
    if (deleting) {
        DeleteAccountRouteScreen(onBack = { deleting = false })
    } else {
        ReactivateScreen(onDelete = { deleting = true })
    }
}
