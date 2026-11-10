package app.nearfolk.feature.feed

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import app.nearfolk.core.data.share.PostSharing
import app.nearfolk.core.designsystem.component.NfButton
import app.nearfolk.core.designsystem.component.NfButtonSize
import app.nearfolk.core.designsystem.component.NfButtonStyle
import app.nearfolk.core.designsystem.component.NfDivider
import app.nearfolk.core.designsystem.component.NfEmptyState
import app.nearfolk.core.designsystem.component.NfIconButton
import app.nearfolk.core.designsystem.component.NfLargeHeader
import app.nearfolk.core.designsystem.component.NfSpinner
import app.nearfolk.core.designsystem.icon.NfIcons
import app.nearfolk.core.designsystem.theme.Nf
import app.nearfolk.core.designsystem.theme.NfMotion
import app.nearfolk.core.model.Post
import app.nearfolk.core.model.SELF_ID
import app.nearfolk.core.ui.CommentsSheet
import app.nearfolk.core.ui.PostActions
import app.nearfolk.core.ui.PostContentType
import app.nearfolk.core.ui.PostItem
import app.nearfolk.core.ui.PostStatsSheet
import app.nearfolk.core.ui.TrackPostViews
import app.nearfolk.core.ui.rememberSavedNotice
import java.time.Instant
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data object FeedRoute

/** Test tags for the feed. */
object FeedTags {
    const val List = "feed_list"
}

@Composable
fun FeedRouteScreen(
    onOpenMember: (String) -> Unit,
    onNewPost: () -> Unit,
    onOpenSaved: () -> Unit,
    viewModel: FeedViewModel = hiltViewModel(),
) {
    val posts = viewModel.feed.collectAsLazyPagingItems()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val commentsState by viewModel.commentsState.collectAsStateWithLifecycle()
    // Hidden while this screen hands over to a profile. Back here, the sheet returns as it was.
    var leavingForProfile by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val savedNotice = rememberSavedNotice(onOpenSaved)
    var statsFor by rememberSaveable { mutableStateOf<String?>(null) }
    val actions = remember(viewModel, context, savedNotice, onOpenMember) {
        PostActions(
            onToggleLike = { viewModel.toggleLike(it.id) },
            onOpenComments = { viewModel.openComments(it.id) },
            onToggleSave = {
                viewModel.toggleSave(it.id)
                savedNotice(it.savedByMe)
            },
            onShare = { PostSharing.share(context, it) },
            onOpenStats = { statsFor = it.id },
            onOpenAuthor = { onOpenMember(it.authorId) },
        )
    }
    FeedScreen(
        posts = posts,
        refreshing = refreshing,
        onRefresh = viewModel::refresh,
        actions = actions,
        onViewed = viewModel::recordView,
        onNewPost = onNewPost,
    )
    statsFor?.let { id ->
        posts.itemSnapshotList.items.firstOrNull { it.id == id }?.let { PostStatsSheet(it, onDismiss = { statsFor = null }) }
    }
    if (commentsState.postId != null && !leavingForProfile) {
        CommentsSheet(
            state = commentsState,
            host = viewModel,
            onShare = { PostSharing.shareComment(context, it) },
            onOpenProfile = { memberId ->
                leavingForProfile = true
                onOpenMember(memberId)
            },
        )
    }
}

/**
 * Everyone's posts in one timeline, newest first, drawn exactly like the
 * posts on a profile. Tapping an author opens their profile. Posts that
 * arrive while you are reading further down do not move the list; a pill
 * offers to jump back up to them.
 */
@Composable
fun FeedScreen(
    posts: LazyPagingItems<Post>,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    actions: PostActions,
    onViewed: (String) -> Unit,
    onNewPost: () -> Unit,
    now: Instant = Instant.now(),
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val fresh = rememberFreshPosts(posts, listState)
    TrackPostViews(listState, onViewed)

    Column(Modifier.fillMaxSize().background(Nf.colors.canvas)) {
        NfLargeHeader(stringResource(R.string.feed_title)) {
            NfIconButton(Icons.Rounded.Add, stringResource(R.string.feed_new_post), onNewPost, style = NfButtonStyle.Primary)
        }
        PullToRefreshBox(isRefreshing = refreshing, onRefresh = onRefresh, modifier = Modifier.weight(1f)) {
            val refresh = posts.loadState.refresh
            when {
                posts.itemCount == 0 && refresh is LoadState.Loading -> NfSpinner()
                posts.itemCount == 0 -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    NfEmptyState(
                        NfIcons.Feed,
                        stringResource(R.string.feed_empty_title),
                        stringResource(R.string.feed_empty_body),
                        action = {
                            NfButton(stringResource(R.string.feed_empty_action), onNewPost, style = NfButtonStyle.Secondary, size = NfButtonSize.Medium)
                        },
                    )
                }
                else -> LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(bottom = 24.dp),
                    modifier = Modifier.fillMaxSize().testTag(FeedTags.List),
                ) {
                    items(count = posts.itemCount, key = posts.itemKey { it.id }, contentType = posts.itemContentType { PostContentType }) { index ->
                        val post = posts[index] ?: return@items
                        Column(Modifier.padding(horizontal = 20.dp)) {
                            PostItem(post, actions, now = now)
                            NfDivider()
                        }
                    }
                    if (posts.loadState.append is LoadState.Loading) {
                        item(key = "appending") { NfSpinner() }
                    }
                }
            }
            // Qualified so the outer Column scope does not pick its own overload.
            androidx.compose.animation.AnimatedVisibility(
                visible = fresh.waiting,
                enter = slideInVertically(NfMotion.slide) { -it } + fadeIn(),
                exit = slideOutVertically(NfMotion.slide) { -it } + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp),
            ) {
                NfButton(
                    stringResource(R.string.feed_new_posts),
                    onClick = { scope.launch { listState.animateScrollToItem(0) } },
                    size = NfButtonSize.Small,
                    leadingIcon = Icons.Rounded.ArrowUpward,
                )
            }
        }
    }
}

/**
 * Tracks whether newer posts are waiting above what the reader is looking at.
 * The newest post counts as seen whenever the list is at the very top. Your
 * own new post is shown right away instead of waiting behind the pill.
 */
@Composable
private fun rememberFreshPosts(posts: LazyPagingItems<Post>, listState: LazyListState): FreshPosts {
    val newest = if (posts.itemCount > 0) posts.peek(0) else null
    var seen by rememberSaveable { mutableStateOf<String?>(null) }
    val atTop by remember(listState) {
        derivedStateOf { listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0 }
    }
    LaunchedEffect(newest?.id, atTop) {
        val id = newest?.id ?: return@LaunchedEffect
        when {
            atTop || seen == null -> seen = id
            id != seen && newest.authorId == SELF_ID -> {
                listState.scrollToItem(0)
                seen = id
            }
        }
    }
    return FreshPosts(waiting = newest != null && seen != null && newest.id != seen && !atTop)
}

private class FreshPosts(val waiting: Boolean)
