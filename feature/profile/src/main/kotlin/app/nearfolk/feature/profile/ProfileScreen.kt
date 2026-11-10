package app.nearfolk.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import app.nearfolk.core.data.share.PostSharing
import app.nearfolk.core.data.media.PlaybackState
import app.nearfolk.core.designsystem.component.NfButton
import app.nearfolk.core.designsystem.component.NfButtonSize
import app.nearfolk.core.designsystem.component.NfButtonStyle
import app.nearfolk.core.designsystem.component.NfDivider
import app.nearfolk.core.designsystem.component.NfEmptyState
import app.nearfolk.core.designsystem.component.NfGroup
import app.nearfolk.core.designsystem.component.NfIconButton
import app.nearfolk.core.designsystem.component.NfListRow
import app.nearfolk.core.designsystem.component.NfNotice
import app.nearfolk.core.designsystem.component.NfNoticeTone
import app.nearfolk.core.designsystem.component.NfPlusMark
import app.nearfolk.core.designsystem.component.NfSectionHeader
import app.nearfolk.core.designsystem.component.NfSpinner
import app.nearfolk.core.designsystem.icon.NfIcons
import app.nearfolk.core.designsystem.theme.Nf
import app.nearfolk.core.designsystem.theme.NfShapes
import app.nearfolk.core.model.Countries
import app.nearfolk.core.model.Highlight
import app.nearfolk.core.model.Member
import app.nearfolk.core.model.Post
import app.nearfolk.core.model.Presence
import app.nearfolk.core.ui.AttachmentProblemMessages
import app.nearfolk.core.ui.BlockDialog
import app.nearfolk.core.ui.CommentsSheet
import app.nearfolk.core.ui.LocalSnackbarHost
import app.nearfolk.core.ui.MediaImage
import app.nearfolk.core.ui.MemberAvatar
import app.nearfolk.core.ui.MenuAction
import app.nearfolk.core.ui.NfMenu
import app.nearfolk.core.ui.PostActions
import app.nearfolk.core.ui.PostContentType
import app.nearfolk.core.ui.PostItem
import app.nearfolk.core.ui.PostStatsSheet
import app.nearfolk.core.ui.TrackPostViews
import app.nearfolk.core.ui.rememberSavedNotice
import app.nearfolk.core.ui.ReportSheet
import app.nearfolk.core.ui.SquareTile
import app.nearfolk.core.ui.StreakCounter
import app.nearfolk.core.ui.VoiceIntroBar
import app.nearfolk.core.ui.metaLine
import app.nearfolk.core.ui.rememberTimeFormat
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Serializable
data class MemberProfileRoute(val memberId: String)

@Composable
fun ProfileRouteScreen(
    onBack: () -> Unit,
    onOpenChat: (String) -> Unit,
    onOpenJournal: (String) -> Unit,
    onOpenSaved: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val posts = viewModel.postStream.collectAsLazyPagingItems()
    val playback by viewModel.playback.collectAsStateWithLifecycle()
    val commentsPostId by viewModel.commentsPostId.collectAsStateWithLifecycle()
    val comments by viewModel.comments.collectAsStateWithLifecycle()
    val commentAttachments by viewModel.commentAttachments.collectAsStateWithLifecycle()
    AttachmentProblemMessages(viewModel.attachmentProblems)
    val snackbar = LocalSnackbarHost.current
    val context = LocalContext.current
    val resources = LocalResources.current
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ProfileEvent.OpenChat -> onOpenChat(event.conversationId)
                ProfileEvent.Reported -> snackbar.showSnackbar(resources.getString(app.nearfolk.core.ui.R.string.ui_report_sent))
                is ProfileEvent.Blocked -> snackbar.showSnackbar(resources.getString(app.nearfolk.core.ui.R.string.ui_blocked_done, event.name))
            }
        }
    }
    val savedNotice = rememberSavedNotice(onOpenSaved)
    var statsFor by rememberSaveable { mutableStateOf<String?>(null) }
    val actions = remember(viewModel, context, savedNotice) {
        PostActions(
            onToggleLike = { viewModel.toggleLike(it.id) },
            onOpenComments = { viewModel.openComments(it.id) },
            onToggleSave = {
                viewModel.toggleSave(it.id)
                savedNotice(it.savedByMe)
            },
            onShare = { PostSharing.share(context, it) },
            onOpenStats = { statsFor = it.id },
        )
    }
    ProfileScreen(
        state = state,
        posts = posts,
        playback = playback,
        onBack = onBack,
        onMessage = viewModel::message,
        onJournal = { onOpenJournal(viewModel.memberId) },
        postActions = actions,
        onViewed = viewModel::recordView,
        onToggleVoice = viewModel::toggleVoice,
        onReport = viewModel::report,
        onBlock = viewModel::block,
        onUnblock = viewModel::unblock,
    )
    if (commentsPostId != null) {
        CommentsSheet(
            comments = comments,
            text = viewModel.commentText,
            attachments = commentAttachments,
            onAddMedia = viewModel::addCommentMedia,
            onRemoveAttachment = viewModel::removeCommentAttachment,
            onDismiss = { viewModel.openComments(null) },
            onSend = viewModel::sendComment,
        )
    }
    statsFor?.let { id ->
        posts.itemSnapshotList.items.firstOrNull { it.id == id }?.let { PostStatsSheet(it, onDismiss = { statsFor = null }) }
    }
}

@Composable
fun ProfileScreen(
    state: ProfileUiState,
    posts: LazyPagingItems<Post>,
    playback: PlaybackState,
    onBack: () -> Unit,
    onMessage: () -> Unit,
    onJournal: () -> Unit,
    postActions: PostActions,
    onViewed: (String) -> Unit,
    onToggleVoice: () -> Unit,
    onReport: (app.nearfolk.core.model.ReportReason, String?) -> Unit,
    onBlock: () -> Unit,
    onUnblock: () -> Unit,
    now: Instant = Instant.now(),
) {
    var menu by remember { mutableStateOf(false) }
    var reporting by rememberSaveable { mutableStateOf(false) }
    var blocking by rememberSaveable { mutableStateOf(false) }
    var viewerIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    val member = state.member
    val listState = rememberLazyListState()
    TrackPostViews(listState, onViewed)

    Box(Modifier.fillMaxSize().background(Nf.colors.canvas)) {
        when {
            state.loading -> NfSpinner(Modifier.windowInsetsPadding(WindowInsets.statusBars).padding(top = 80.dp))
            member == null -> NfEmptyState(Icons.Rounded.Visibility, stringResource(R.string.profile_not_found), "", Modifier.padding(top = 80.dp))
            else -> LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(bottom = 32.dp)) {
                item(key = "header") { Header(member, state, now, onMessage, onJournal) }
                if (state.blocked) {
                    item(key = "blocked") {
                        NfNotice(
                            title = stringResource(R.string.profile_blocked_title, member.displayName),
                            text = stringResource(R.string.profile_blocked_body),
                            tone = NfNoticeTone.Danger,
                            action = { NfButton(stringResource(R.string.profile_unblock), onUnblock, size = NfButtonSize.Small, style = NfButtonStyle.Secondary) },
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                } else {
                    profileBody(member, state, playback, posts, now, onToggleVoice, postActions) { viewerIndex = it }
                }
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            NfIconButton(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.profile_back), onBack, style = NfButtonStyle.Secondary)
            Spacer(Modifier.weight(1f))
            if (member != null && !state.preview) {
                Box {
                    NfIconButton(Icons.Rounded.MoreHoriz, stringResource(R.string.profile_more), { menu = true }, style = NfButtonStyle.Secondary)
                    NfMenu(
                        expanded = menu,
                        onDismiss = { menu = false },
                        actions = listOf(
                            MenuAction(stringResource(R.string.profile_report), Icons.Rounded.Flag) { reporting = true },
                            if (state.blocked) {
                                MenuAction(stringResource(R.string.profile_unblock), Icons.Rounded.Block) { onUnblock() }
                            } else {
                                MenuAction(stringResource(R.string.profile_block), Icons.Rounded.Block, destructive = true) { blocking = true }
                            },
                        ),
                    )
                }
            }
        }
    }

    if (member != null && reporting) {
        ReportSheet(member.displayName, onDismiss = { reporting = false }, onSubmit = { r, d ->
            reporting = false
            onReport(r, d)
        })
    }
    if (member != null && blocking) {
        BlockDialog(member.displayName, onConfirm = {
            blocking = false
            onBlock()
        }, onDismiss = { blocking = false })
    }
    viewerIndex?.let { index ->
        HighlightViewer(state.highlights, index, onDismiss = { viewerIndex = null })
    }
}

@Composable
private fun Header(member: Member, state: ProfileUiState, now: Instant, onMessage: () -> Unit, onJournal: () -> Unit) {
    val time = rememberTimeFormat()
    val today = LocalDate.now(ZoneId.systemDefault())
    Column {
        Box {
            val banner = member.banner
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(196.dp)
                    .background(Nf.colors.surfaceRaised),
            ) {
                if (banner != null) MediaImage(banner, Modifier.fillMaxSize())
            }
            Box(
                Modifier
                    .padding(start = 16.dp, top = 140.dp)
                    .clip(NfShapes.avatar)
                    .background(Nf.colors.canvas)
                    .padding(4.dp),
            ) {
                MemberAvatar(member.displayName, member.avatar, 104.dp)
            }
        }
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(member.displayName, style = Nf.type.headline, color = Nf.colors.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (member.isPlus) NfPlusMark()
            }
            Text(
                time.presence(member.presence, now),
                style = Nf.type.label,
                color = if (member.presence is Presence.Online) Nf.colors.online else Nf.colors.textTertiary,
            )
            Text(member.metaLine(today), style = Nf.type.body, color = Nf.colors.textSecondary)
            if (member.catchphrase.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text("“${member.catchphrase}”", style = Nf.type.title.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Normal), color = Nf.colors.textPrimary)
            }
            Spacer(Modifier.height(14.dp))
            if (state.preview) {
                NfNotice(stringResource(R.string.profile_preview_notice), tone = NfNoticeTone.Accent, icon = Icons.Rounded.Visibility)
            } else if (!state.blocked) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NfButton(stringResource(R.string.profile_message), onMessage, leadingIcon = NfIcons.Messages, modifier = Modifier.weight(1f))
                    NfButton(
                        if (state.journalCount > 0) stringResource(R.string.profile_journal_count, state.journalCount) else stringResource(R.string.profile_journal),
                        onJournal,
                        style = NfButtonStyle.Secondary,
                        leadingIcon = NfIcons.Journal,
                        enabled = state.journalCount > 0,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

private fun LazyListScope.profileBody(
    member: Member,
    state: ProfileUiState,
    playback: PlaybackState,
    posts: LazyPagingItems<Post>,
    now: Instant,
    onToggleVoice: () -> Unit,
    postActions: PostActions,
    onOpenHighlight: (Int) -> Unit,
) {
    item(key = "streak") {
        val today = LocalDate.now(ZoneId.systemDefault())
        StreakCounter(member.streakDays(today), member.streakStart, Modifier.padding(horizontal = 16.dp, vertical = 16.dp).fillMaxWidth())
    }
    if (member.bio.isNotBlank()) {
        item(key = "about") {
            Column(Modifier.padding(horizontal = 16.dp)) {
                NfSectionHeader(stringResource(R.string.profile_about))
                Text(member.bio, style = Nf.type.body, color = Nf.colors.textPrimary, modifier = Modifier.padding(horizontal = 4.dp))
            }
        }
    }
    member.voiceIntro?.let { voice ->
        item(key = "voice") {
            Column(Modifier.padding(horizontal = 16.dp)) {
                NfSectionHeader(stringResource(R.string.profile_voice))
                val mine = playback.path == voice.path
                VoiceIntroBar(voice.durationMillis, mine && playback.playing, if (mine) playback.positionMillis else 0, onToggleVoice)
            }
        }
    }
    item(key = "details") {
        val time = rememberTimeFormat()
        Column(Modifier.padding(horizontal = 16.dp)) {
            NfSectionHeader(stringResource(R.string.profile_details))
            NfGroup {
                NfListRow(stringResource(R.string.profile_birthday), subtitle = time.shortDate(member.birthDate.withYear(LocalDate.now().year)))
                NfDivider()
                NfListRow(
                    stringResource(R.string.profile_lives_in),
                    subtitle = listOfNotNull(member.city, Countries.displayName(member.countryCode)).joinToString(", ") + "  " + Countries.flag(member.countryCode),
                )
                NfDivider()
                NfListRow(stringResource(R.string.profile_joined), subtitle = time.longDate(time.localDate(member.joinedAt)))
            }
        }
    }
    if (state.highlights.isNotEmpty()) {
        item(key = "highlights") {
            Column(Modifier.padding(horizontal = 16.dp)) {
                NfSectionHeader(stringResource(R.string.profile_highlights))
                state.highlights.chunked(3).forEachIndexed { rowIndex, row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 6.dp)) {
                        row.forEachIndexed { i, h ->
                            SquareTile(
                                content = { MediaImage(h.media, Modifier.fillMaxSize()) },
                                onClick = { onOpenHighlight(rowIndex * 3 + i) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
    item(key = "postsHeader") {
        NfSectionHeader(stringResource(R.string.profile_posts), Modifier.padding(horizontal = 16.dp))
    }
    items(count = posts.itemCount, key = posts.itemKey { it.id }, contentType = posts.itemContentType { PostContentType }) { index ->
        val post = posts[index] ?: return@items
        Column(Modifier.padding(horizontal = 20.dp)) {
            PostItem(post, postActions, now = now)
            NfDivider()
        }
    }
    if (posts.loadState.refresh is LoadState.NotLoading && posts.itemCount == 0) {
        item(key = "noPosts") {
            Text(stringResource(R.string.profile_no_posts), style = Nf.type.body, color = Nf.colors.textTertiary, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
        }
    }
    if (posts.loadState.append is LoadState.Loading) {
        item(key = "appending") { NfSpinner() }
    }
}

