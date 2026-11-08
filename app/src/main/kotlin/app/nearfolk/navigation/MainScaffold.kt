package app.nearfolk.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import app.nearfolk.MainViewModel
import app.nearfolk.R
import app.nearfolk.core.designsystem.component.NfNavItem
import app.nearfolk.core.designsystem.component.NfNavigationBar
import app.nearfolk.core.designsystem.icon.NfIcons
import app.nearfolk.core.designsystem.theme.Nf
import app.nearfolk.core.designsystem.theme.NfMotion
import app.nearfolk.core.model.SELF_ID
import app.nearfolk.core.ui.AppDocument
import app.nearfolk.core.ui.LocalSnackbarHost
import app.nearfolk.core.ui.NfSnackbarHost
import app.nearfolk.feature.discover.DiscoverRoute
import app.nearfolk.feature.discover.DiscoverRouteScreen
import app.nearfolk.feature.journal.JournalEditorRoute
import app.nearfolk.feature.journal.JournalEditorRouteScreen
import app.nearfolk.feature.journal.JournalRoute
import app.nearfolk.feature.journal.JournalRouteScreen
import app.nearfolk.feature.journal.PasscodeFlow
import app.nearfolk.feature.journal.PasscodeSetupRoute
import app.nearfolk.feature.journal.PasscodeSetupRouteScreen
import app.nearfolk.feature.journal.PublicJournalRoute
import app.nearfolk.feature.journal.PublicJournalRouteScreen
import app.nearfolk.feature.messages.ArchivedRoute
import app.nearfolk.feature.messages.ArchivedRouteScreen
import app.nearfolk.feature.messages.ChatRoute
import app.nearfolk.feature.messages.ChatRouteScreen
import app.nearfolk.feature.messages.InboxRouteScreen
import app.nearfolk.feature.messages.MessagesRoute
import app.nearfolk.feature.profile.MemberProfileRoute
import app.nearfolk.feature.profile.ProfileRouteScreen
import app.nearfolk.feature.settings.BlockedRoute
import app.nearfolk.feature.settings.BlockedRouteScreen
import app.nearfolk.feature.settings.BugReportRoute
import app.nearfolk.feature.settings.BugReportRouteScreen
import app.nearfolk.feature.settings.DeleteAccountRoute
import app.nearfolk.feature.settings.DeleteAccountRouteScreen
import app.nearfolk.feature.settings.DocumentRoute
import app.nearfolk.feature.settings.DocumentRouteScreen
import app.nearfolk.feature.settings.PasscodeAction
import app.nearfolk.feature.settings.PlusRoute
import app.nearfolk.feature.settings.PlusRouteScreen
import app.nearfolk.feature.settings.SettingsRoute
import app.nearfolk.feature.settings.SettingsRouteScreen
import app.nearfolk.feature.you.ActivityRoute
import app.nearfolk.feature.you.ActivityRouteScreen
import app.nearfolk.feature.you.ComposePostRoute
import app.nearfolk.feature.you.ComposePostRouteScreen
import app.nearfolk.feature.you.CropRoute
import app.nearfolk.feature.you.CropRouteScreen
import app.nearfolk.feature.you.EditProfileRoute
import app.nearfolk.feature.you.EditProfileRouteScreen
import app.nearfolk.feature.you.HighlightsRoute
import app.nearfolk.feature.you.HighlightsRouteScreen
import app.nearfolk.feature.you.MyPostsRoute
import app.nearfolk.feature.you.MyPostsRouteScreen
import app.nearfolk.feature.you.VoiceIntroRoute
import app.nearfolk.feature.you.VoiceIntroRouteScreen
import app.nearfolk.feature.you.YouRoute
import app.nearfolk.feature.you.YouRouteScreen

private enum class Tab(val route: Any, val label: Int) {
    Discover(DiscoverRoute, R.string.nav_discover),
    Messages(MessagesRoute, R.string.nav_messages),
    You(YouRoute, R.string.nav_you),
    Settings(SettingsRoute, R.string.nav_settings),
}

private fun NavDestination?.tab(): Tab? = when {
    this == null -> null
    hasRoute(DiscoverRoute::class) -> Tab.Discover
    hasRoute(MessagesRoute::class) -> Tab.Messages
    hasRoute(YouRoute::class) -> Tab.You
    hasRoute(SettingsRoute::class) -> Tab.Settings
    else -> null
}

private fun NavHostController.openTab(tab: Tab) = navigate(tab.route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

/** Pops only when there is something underneath, so double taps never leave a blank screen. */
private fun NavHostController.back() {
    if (previousBackStackEntry != null) popBackStack()
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.betweenTabs() =
    initialState.destination.tab() != null && targetState.destination.tab() != null

@Composable
fun MainScaffold(viewModel: MainViewModel) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val currentTab = entry?.destination.tab()
    val badges by viewModel.badges.collectAsStateWithLifecycle()
    val pendingChat by viewModel.pendingChat.collectAsStateWithLifecycle()

    LaunchedEffect(pendingChat) {
        pendingChat?.let {
            nav.navigate(ChatRoute(it)) { launchSingleTop = true }
            viewModel.consumePendingChat()
        }
    }

    Column(Modifier.fillMaxSize().background(Nf.colors.canvas)) {
        Box(Modifier.weight(1f)) {
            NavHost(
                navController = nav,
                startDestination = DiscoverRoute,
                enterTransition = {
                    if (betweenTabs()) fadeIn(NfMotion.settle()) else slideInHorizontally(NfMotion.slide) { it / 5 } + fadeIn(NfMotion.settle())
                },
                exitTransition = {
                    if (betweenTabs()) fadeOut(NfMotion.settle()) else slideOutHorizontally(NfMotion.slide) { -it / 12 } + fadeOut(NfMotion.settle())
                },
                popEnterTransition = {
                    if (betweenTabs()) fadeIn(NfMotion.settle()) else slideInHorizontally(NfMotion.slide) { -it / 12 } + fadeIn(NfMotion.settle())
                },
                popExitTransition = {
                    if (betweenTabs()) fadeOut(NfMotion.settle()) else slideOutHorizontally(NfMotion.slide) { it / 5 } + fadeOut(NfMotion.settle())
                },
            ) {
                composable<DiscoverRoute> {
                    DiscoverRouteScreen(
                        onOpenMember = { nav.navigate(MemberProfileRoute(it)) },
                        onOpenPlus = { nav.navigate(PlusRoute) },
                    )
                }
                composable<MessagesRoute> {
                    InboxRouteScreen(
                        onOpenChat = { nav.navigate(ChatRoute(it)) },
                        onOpenArchived = { nav.navigate(ArchivedRoute) },
                        onOpenDiscover = { nav.openTab(Tab.Discover) },
                    )
                }
                composable<YouRoute> {
                    YouRouteScreen(
                        onPreview = { nav.navigate(MemberProfileRoute(SELF_ID)) },
                        onEdit = { nav.navigate(EditProfileRoute) },
                        onNewPost = { nav.navigate(ComposePostRoute) },
                        onMyPosts = { nav.navigate(MyPostsRoute) },
                        onHighlights = { nav.navigate(HighlightsRoute) },
                        onActivity = { nav.navigate(ActivityRoute) },
                        onVoice = { nav.navigate(VoiceIntroRoute) },
                        onJournal = { nav.navigate(JournalRoute) },
                        onPlus = { nav.navigate(PlusRoute) },
                    )
                }
                composable<SettingsRoute> {
                    SettingsRouteScreen(
                        onOpenPlus = { nav.navigate(PlusRoute) },
                        onOpenBlocked = { nav.navigate(BlockedRoute) },
                        onOpenDelete = { nav.navigate(DeleteAccountRoute) },
                        onOpenBugReport = { nav.navigate(BugReportRoute) },
                        onOpenDocument = { nav.navigate(DocumentRoute(it.name)) },
                        onPasscode = { action ->
                            val flow = when (action) {
                                PasscodeAction.Create -> PasscodeFlow.Create
                                PasscodeAction.Change -> PasscodeFlow.Change
                                PasscodeAction.Remove -> PasscodeFlow.Remove
                            }
                            nav.navigate(PasscodeSetupRoute(flow.name))
                        },
                    )
                }
                composable<MemberProfileRoute> {
                    ProfileRouteScreen(
                        onBack = nav::back,
                        onOpenChat = { nav.navigate(ChatRoute(it)) { launchSingleTop = true } },
                        onOpenJournal = { nav.navigate(PublicJournalRoute(it)) },
                    )
                }
                composable<ChatRoute> {
                    ChatRouteScreen(
                        onBack = nav::back,
                        onOpenProfile = { nav.navigate(MemberProfileRoute(it)) { launchSingleTop = true } },
                    )
                }
                composable<ArchivedRoute> { ArchivedRouteScreen(onBack = nav::back, onOpenChat = { nav.navigate(ChatRoute(it)) }) }
                composable<JournalRoute> {
                    JournalRouteScreen(
                        onBack = nav::back,
                        onNewEntry = { nav.navigate(JournalEditorRoute()) },
                        onOpenEntry = { nav.navigate(JournalEditorRoute(it)) },
                        onSetPasscode = { nav.navigate(PasscodeSetupRoute(PasscodeFlow.Create.name)) },
                    )
                }
                composable<JournalEditorRoute> { JournalEditorRouteScreen(onBack = nav::back) }
                composable<PasscodeSetupRoute> { PasscodeSetupRouteScreen(onDone = nav::back) }
                composable<PublicJournalRoute> { PublicJournalRouteScreen(onBack = nav::back) }
                composable<EditProfileRoute> {
                    EditProfileRouteScreen(onBack = nav::back, onCrop = { uri, target -> nav.navigate(CropRoute(uri, target.name)) })
                }
                composable<CropRoute> { CropRouteScreen(onDone = nav::back) }
                composable<VoiceIntroRoute> { VoiceIntroRouteScreen(onBack = nav::back) }
                composable<MyPostsRoute> {
                    MyPostsRouteScreen(
                        onBack = nav::back,
                        onNewPost = { nav.navigate(ComposePostRoute) },
                        onOpenComments = { nav.navigate(MemberProfileRoute(SELF_ID)) },
                    )
                }
                composable<ComposePostRoute> { ComposePostRouteScreen(onDone = nav::back) }
                composable<HighlightsRoute> { HighlightsRouteScreen(onBack = nav::back) }
                composable<ActivityRoute> { ActivityRouteScreen(onBack = nav::back, onOpenMember = { nav.navigate(MemberProfileRoute(it)) }) }
                composable<PlusRoute> { PlusRouteScreen(onBack = nav::back) }
                composable<BlockedRoute> { BlockedRouteScreen(onBack = nav::back) }
                composable<DeleteAccountRoute> { DeleteAccountRouteScreen(onBack = nav::back) }
                composable<BugReportRoute> { BugReportRouteScreen(onBack = nav::back) }
                composable<DocumentRoute> { backStack ->
                    val document = AppDocument.valueOf(backStack.toRoute<DocumentRoute>().document)
                    DocumentRouteScreen(document, onBack = nav::back)
                }
            }
            NfSnackbarHost(
                LocalSnackbarHost.current,
                Modifier
                    .align(Alignment.BottomCenter)
                    .then(if (currentTab == null) Modifier.windowInsetsPadding(WindowInsets.navigationBars) else Modifier),
            )
        }
        AnimatedVisibility(
            visible = currentTab != null,
            enter = slideInVertically(NfMotion.slide) { it } + fadeIn(),
            exit = slideOutVertically(NfMotion.slide) { it } + fadeOut(),
        ) {
            NfNavigationBar(
                items = listOf(
                    NfNavItem(stringResource(Tab.Discover.label), NfIcons.Discover, NfIcons.DiscoverSelected),
                    NfNavItem(stringResource(Tab.Messages.label), NfIcons.Messages, NfIcons.MessagesSelected, badges.messages),
                    NfNavItem(stringResource(Tab.You.label), NfIcons.You, NfIcons.YouSelected, badges.you),
                    NfNavItem(stringResource(Tab.Settings.label), NfIcons.Settings, NfIcons.SettingsSelected),
                ),
                selectedIndex = currentTab?.ordinal ?: -1,
                onSelect = { nav.openTab(Tab.entries[it]) },
            )
        }
    }
}

