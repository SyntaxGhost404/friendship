package app.nearfolk.feature.you

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
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
import app.nearfolk.core.designsystem.component.NfSpinner
import app.nearfolk.core.designsystem.component.NfTopBar
import app.nearfolk.core.designsystem.theme.Nf
import app.nearfolk.core.model.Post
import app.nearfolk.core.ui.AttachmentProblemMessages
import app.nearfolk.core.ui.CommentsSheet
import app.nearfolk.core.ui.LocalSnackbarHost
import app.nearfolk.core.ui.PostActions
import app.nearfolk.core.ui.PostContentType
import app.nearfolk.core.ui.PostItem
import app.nearfolk.core.ui.PostStatsSheet
import app.nearfolk.core.ui.TrackPostViews
import java.time.Instant
import kotlinx.coroutines.launch

/** Test tags for the saved list. */
object SavedTags {
    const val List = "saved_list"
}

@Composable
fun SavedPostsRouteScreen(
    onBack: () -> Unit,
    onOpenMember: (String) -> Unit,
    onOpenFeed: () -> Unit,
    viewModel: SavedPostsViewModel = hiltViewModel(),
) {
    val posts = viewModel.saved.collectAsLazyPagingItems()
    val count by viewModel.count.collectAsStateWithLifecycle()
    val commentsPostId by viewModel.commentsPostId.collectAsStateWithLifecycle()
    val comments by viewModel.comments.collectAsStateWithLifecycle()
    val commentAttachments by viewModel.commentAttachments.collectAsStateWithLifecycle()
    AttachmentProblemMessages(viewModel.attachmentProblems)
    val context = LocalContext.current
    val snackbar = LocalSnackbarHost.current
    val resources = LocalResources.current
    var statsFor by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is SavedEvent.Removed -> launch {
                    // Only the latest removal can be undone; a new one replaces the note.
                    snackbar.currentSnackbarData?.dismiss()
                    val result = snackbar.showSnackbar(
                        message = resources.getString(R.string.saved_removed),
                        actionLabel = resources.getString(R.string.saved_undo),
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.restore(event.postId, event.savedAt)
                }
            }
        }
    }

    val actions = remember(viewModel, context, onOpenMember) {
        PostActions(
            onToggleLike = { viewModel.toggleLike(it.id) },
            onOpenComments = { viewModel.openComments(it.id) },
            onToggleSave = viewModel::toggleSave,
            onShare = { PostSharing.share(context, it) },
            onOpenStats = { statsFor = it.id },
            onOpenAuthor = { onOpenMember(it.authorId) },
        )
    }
    SavedPostsScreen(
        posts = posts,
        count = count,
        actions = actions,
        onViewed = viewModel::recordView,
        onBack = onBack,
        onOpenFeed = onOpenFeed,
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

/**
 * The posts you saved, the most recently saved first. Only you see this list.
 * Taking a post out of it can be undone from the note that follows.
 */
@Composable
fun SavedPostsScreen(
    posts: LazyPagingItems<Post>,
    count: Int?,
    actions: PostActions,
    onViewed: (String) -> Unit,
    onBack: () -> Unit,
    onOpenFeed: () -> Unit,
    now: Instant = Instant.now(),
) {
    val listState = rememberLazyListState()
    TrackPostViews(listState, onViewed)
    Column(Modifier.fillMaxSize().background(Nf.colors.canvas)) {
        NfTopBar(
            stringResource(R.string.saved_title),
            onBack = onBack,
            backDescription = stringResource(app.nearfolk.core.ui.R.string.ui_back),
            subtitle = count?.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.saved_subtitle, it, it) },
        )
        when {
            posts.itemCount == 0 && posts.loadState.refresh is LoadState.Loading -> NfSpinner()
            posts.itemCount == 0 -> NfEmptyState(
                Icons.Rounded.BookmarkBorder,
                stringResource(R.string.saved_empty_title),
                stringResource(R.string.saved_empty_body),
                action = {
                    NfButton(stringResource(R.string.saved_browse), onOpenFeed, style = NfButtonStyle.Secondary, size = NfButtonSize.Medium)
                },
            )
            else -> LazyColumn(
                state = listState,
                contentPadding = PaddingValues(bottom = 24.dp),
                modifier = Modifier.fillMaxSize().testTag(SavedTags.List),
            ) {
                items(count = posts.itemCount, key = posts.itemKey { it.id }, contentType = posts.itemContentType { PostContentType }) { index ->
                    val post = posts[index] ?: return@items
                    Column(Modifier.padding(horizontal = 20.dp).animateItem()) {
                        PostItem(post, actions, now = now)
                        NfDivider()
                    }
                }
                if (posts.loadState.append is LoadState.Loading) {
                    item(key = "appending") { NfSpinner() }
                }
            }
        }
    }
}
