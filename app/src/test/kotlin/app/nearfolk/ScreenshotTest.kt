package app.nearfolk

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.nearfolk.core.data.media.PlaybackState
import app.nearfolk.core.data.repository.PostStats
import app.nearfolk.core.designsystem.component.NfNavItem
import app.nearfolk.core.designsystem.component.NfNavigationBar
import app.nearfolk.core.designsystem.icon.NfIcons
import app.nearfolk.core.designsystem.theme.Nf
import app.nearfolk.core.designsystem.theme.NearfolkTheme
import app.nearfolk.core.ui.AppInfo
import app.nearfolk.core.ui.LocalAppInfo
import app.nearfolk.core.ui.LocalSnackbarHost
import app.nearfolk.feature.discover.DiscoverScreen
import app.nearfolk.feature.discover.DiscoverUiState
import app.nearfolk.feature.journal.PasscodeMessage
import app.nearfolk.feature.journal.PasscodePad
import app.nearfolk.feature.messages.ChatScreen
import app.nearfolk.feature.messages.ChatUiState
import app.nearfolk.feature.messages.InboxScreen
import app.nearfolk.feature.messages.InboxUiState
import app.nearfolk.feature.onboarding.OnboardingScreen
import app.nearfolk.feature.onboarding.OnboardingState
import app.nearfolk.feature.profile.ProfileScreen
import app.nearfolk.feature.profile.ProfileUiState
import app.nearfolk.feature.you.YouScreen
import app.nearfolk.feature.you.YouUiState
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/**
 * Renders the main screens with sample data in both themes. Images land in
 * app/build/screenshots when run with -Proborazzi.test.record=true.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h860dp-xhdpi")
class ScreenshotTest {

    private fun shot(name: String, dark: Boolean, withBar: Int = -1, content: @Composable () -> Unit) =
        captureRoboImage("build/screenshots/${name}_${if (dark) "dark" else "light"}.png") {
            NearfolkTheme(darkTheme = dark) {
                CompositionLocalProvider(
                    LocalSnackbarHost provides SnackbarHostState(),
                    LocalAppInfo provides AppInfo("0.1.0", "support@example.com"),
                ) {
                    Column(Modifier.fillMaxSize().background(Nf.colors.canvas)) {
                        Box(Modifier.weight(1f)) { content() }
                        if (withBar >= 0) {
                            NfNavigationBar(
                                items = listOf(
                                    NfNavItem("Discover", NfIcons.Discover, NfIcons.DiscoverSelected),
                                    NfNavItem("Messages", NfIcons.Messages, NfIcons.MessagesSelected, 3),
                                    NfNavItem("You", NfIcons.You, NfIcons.YouSelected),
                                    NfNavItem("Settings", NfIcons.Settings, NfIcons.SettingsSelected),
                                ),
                                selectedIndex = withBar,
                                onSelect = {},
                            )
                        }
                    }
                }
            }
        }

    @Test fun discoverDark() = discover(true)
    @Test fun discoverLight() = discover(false)

    private fun discover(dark: Boolean) = shot("discover", dark, withBar = 0) {
        DiscoverScreen(
            state = DiscoverUiState(loading = false, members = Fixtures.members, onlineCount = 2, filter = Fixtures.filter),
            query = "",
            refreshing = false,
            onQuery = {}, onGender = {}, onApplyFilter = {}, onClearFilters = {}, onRefresh = {}, onOpenMember = {}, onOpenPlus = {},
            now = Fixtures.now,
        )
    }

    @Test fun inboxDark() = inbox(true)
    @Test fun inboxLight() = inbox(false)

    private fun inbox(dark: Boolean) = shot("inbox", dark, withBar = 1) {
        InboxScreen(InboxUiState(false, Fixtures.conversations, emptyList(), 1), {}, {}, {}, now = Fixtures.now)
    }

    @Test fun chatDark() = chat(true)
    @Test fun chatLight() = chat(false)

    private fun chat(dark: Boolean) = shot("chat", dark) {
        ChatScreen(
            state = ChatUiState(false, Fixtures.conversations.first().copy(peerIsTyping = false), Fixtures.thread, showSafetyBanner = true, allowance = null, blocked = false),
            draft = "",
            onDraft = {}, onSend = {}, onBack = {}, onOpenProfile = {}, onDismissSafety = {}, onAccept = {}, onToggleMute = {},
            onReport = { _, _ -> }, onBlock = {}, onUnblock = {}, onDelete = {},
            now = Fixtures.now,
        )
    }

    @Test fun profileDark() = profile(true)
    @Test fun profileLight() = profile(false)

    private fun profile(dark: Boolean) = shot("profile", dark) {
        val posts = flowOf(PagingData.from(Fixtures.posts)).collectAsLazyPagingItems()
        ProfileScreen(
            state = ProfileUiState(false, Fixtures.amara, Fixtures.highlights, journalCount = 2, blocked = false, preview = false),
            posts = posts,
            playback = PlaybackState(),
            onBack = {}, onMessage = {}, onJournal = {}, onToggleLike = {}, onOpenComments = {}, onToggleVoice = {},
            onReport = { _, _ -> }, onBlock = {}, onUnblock = {},
            now = Fixtures.now,
        )
    }

    @Test fun youDark() = you(true)
    @Test fun youLight() = you(false)

    private fun you(dark: Boolean) = shot("you", dark, withBar = 2) {
        YouScreen(
            YouUiState(Fixtures.me, PostStats(4, 37, 6), unseenActivity = 2, isPlus = false, passcodeSet = true),
            {}, {}, {}, {}, {}, {}, {}, {}, {},
        )
    }

    @Test fun welcomeDark() = shot("welcome", true) {
        OnboardingScreen(OnboardingState(), "Rae", 4, { true }, LocalDate.now(), {}, {}, {}, {}, {}, {}, {}, {})
    }

    @Test fun onboardingRulesLight() = shot("rules", false) {
        OnboardingScreen(OnboardingState(step = 3, agreed = true), null, 4, { true }, LocalDate.now(), {}, {}, {}, {}, {}, {}, {}, {})
    }

    @Test fun passcodeDark() = shot("passcode", true) {
        PasscodePad("Enter your passcode", "Your journal is locked.", 3, PasscodeMessage.Wrong(2), emptyFlow(), {}, {})
    }
}
