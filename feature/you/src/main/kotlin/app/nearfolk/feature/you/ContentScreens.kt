package app.nearfolk.feature.you

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.nearfolk.core.designsystem.component.NfButton
import app.nearfolk.core.designsystem.component.NfButtonSize
import app.nearfolk.core.designsystem.component.NfButtonStyle
import app.nearfolk.core.designsystem.component.NfConfirmDialog
import app.nearfolk.core.designsystem.component.NfDivider
import app.nearfolk.core.designsystem.component.NfEmptyState
import app.nearfolk.core.designsystem.component.NfIconButton
import app.nearfolk.core.designsystem.component.NfSpinner
import app.nearfolk.core.designsystem.component.NfTextField
import app.nearfolk.core.designsystem.component.NfTopBar
import app.nearfolk.core.designsystem.component.NfUnreadMark
import app.nearfolk.core.designsystem.theme.Nf
import app.nearfolk.core.designsystem.theme.NfShapes
import app.nearfolk.core.model.ActivityKind
import app.nearfolk.core.model.ProfileRules
import app.nearfolk.core.ui.LocalSnackbarHost
import app.nearfolk.core.ui.MediaImage
import app.nearfolk.core.ui.MemberAvatar
import app.nearfolk.core.ui.CommentsSheet
import app.nearfolk.core.ui.PostActions
import app.nearfolk.core.ui.PostContentType
import app.nearfolk.core.ui.PostItem
import app.nearfolk.core.ui.PostStatsSheet
import app.nearfolk.core.ui.TrackPostViews
import app.nearfolk.core.ui.rememberSavedNotice
import app.nearfolk.core.data.share.PostSharing
import app.nearfolk.core.ui.rememberTimeFormat
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch

@Composable
fun MyPostsRouteScreen(
    onBack: () -> Unit,
    onNewPost: () -> Unit,
    onOpenSaved: () -> Unit,
    onOpenMember: (String) -> Unit,
    viewModel: MyPostsViewModel = hiltViewModel(),
) {
    val posts by viewModel.posts.collectAsStateWithLifecycle()
    val commentsState by viewModel.commentsState.collectAsStateWithLifecycle()
    // Hidden while this screen hands over to a profile. Back here, the sheet returns as it was.
    var leavingForProfile by remember { mutableStateOf(false) }
    var deleting by rememberSaveable { mutableStateOf<String?>(null) }
    var statsFor by rememberSaveable { mutableStateOf<String?>(null) }
    val snackbar = LocalSnackbarHost.current
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val savedNotice = rememberSavedNotice(onOpenSaved)
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
    val listState = rememberLazyListState()
    TrackPostViews(listState, viewModel::recordView)
    Column(Modifier.fillMaxSize().background(Nf.colors.canvas)) {
        NfTopBar(
            stringResource(R.string.posts_title),
            onBack = onBack,
            backDescription = stringResource(app.nearfolk.core.ui.R.string.ui_back),
            actions = { NfIconButton(Icons.Rounded.Add, stringResource(R.string.compose_title), onNewPost, style = NfButtonStyle.Primary) },
        )
        val list = posts
        when {
            list == null -> NfSpinner()
            list.isEmpty() -> NfEmptyState(
                Icons.Rounded.ViewAgenda,
                stringResource(R.string.posts_empty_title),
                stringResource(R.string.posts_empty_body),
                action = { NfButton(stringResource(R.string.compose_title), onNewPost, size = NfButtonSize.Medium) },
            )
            else -> LazyColumn(state = listState, contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp)) {
                items(list, key = { it.id }, contentType = { PostContentType }) { post ->
                    Column(Modifier.animateItem()) {
                        PostItem(post, actions, onOptions = { deleting = post.id })
                        NfDivider()
                    }
                }
            }
        }
    }
    deleting?.let { id ->
        NfConfirmDialog(
            title = stringResource(R.string.posts_delete_title),
            message = stringResource(R.string.posts_delete_body),
            confirmLabel = stringResource(R.string.posts_delete),
            dismissLabel = stringResource(app.nearfolk.core.ui.R.string.ui_cancel),
            onConfirm = {
                deleting = null
                viewModel.delete(id)
                scope.launch { snackbar.showSnackbar(resources.getString(R.string.posts_deleted)) }
            },
            onDismiss = { deleting = null },
            destructive = true,
        )
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
    statsFor?.let { id -> posts?.firstOrNull { it.id == id }?.let { PostStatsSheet(it, onDismiss = { statsFor = null }) } }
}

@Composable
fun ComposePostRouteScreen(onDone: () -> Unit, viewModel: ComposePostViewModel = hiltViewModel()) {
    val caption by viewModel.caption.collectAsStateWithLifecycle()
    val photo by viewModel.photo.collectAsStateWithLifecycle()
    val posting by viewModel.posting.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbarHost.current
    val context = LocalContext.current
    val resources = LocalResources.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? -> uri?.let(viewModel::setPhoto) }
    LaunchedEffect(viewModel) {
        viewModel.done.collect {
            onDone()
            snackbar.showSnackbar(resources.getString(R.string.compose_posted))
        }
    }
    Column(Modifier.fillMaxSize().background(Nf.colors.canvas).imePadding()) {
        NfTopBar(
            stringResource(R.string.compose_title),
            onBack = onDone,
            backDescription = stringResource(app.nearfolk.core.ui.R.string.ui_back),
            actions = {
                NfButton(
                    stringResource(R.string.compose_post),
                    viewModel::post,
                    enabled = caption.isNotBlank() || photo != null,
                    loading = posting,
                    size = NfButtonSize.Small,
                )
            },
        )
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            NfTextField(
                value = caption,
                onValueChange = viewModel::setCaption,
                placeholder = stringResource(R.string.compose_hint),
                singleLine = false,
                minLines = 4,
                maxLength = ProfileRules.CAPTION_MAX,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
            val p = photo
            if (p != null) {
                Box {
                    AsyncImage(
                        model = p,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f).clip(NfShapes.large).background(Nf.colors.surfaceRaised),
                    )
                    NfIconButton(
                        Icons.Rounded.Close,
                        stringResource(R.string.compose_remove_photo),
                        { viewModel.setPhoto(null) },
                        style = NfButtonStyle.Secondary,
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                    )
                }
            } else {
                NfButton(
                    stringResource(R.string.compose_add_photo),
                    { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    style = NfButtonStyle.Secondary,
                    leadingIcon = Icons.Rounded.AddPhotoAlternate,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
fun HighlightsRouteScreen(onBack: () -> Unit, viewModel: HighlightsViewModel = hiltViewModel()) {
    val highlights by viewModel.highlights.collectAsStateWithLifecycle()
    val limit by viewModel.limit.collectAsStateWithLifecycle()
    val adding by viewModel.adding.collectAsStateWithLifecycle()
    var removing by rememberSaveable { mutableStateOf<String?>(null) }
    val snackbar = LocalSnackbarHost.current
    val context = LocalContext.current
    val resources = LocalResources.current
    val count = highlights?.size ?: 0
    val remaining = (limit - count).coerceAtLeast(0)
    val pickMany = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(maxItems = 30)) { uris ->
        viewModel.add(uris.take(remaining))
    }
    val pickOne = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let { viewModel.add(listOf(it)) } }
    LaunchedEffect(viewModel) {
        viewModel.limitHit.collect { snackbar.showSnackbar(resources.getQuantityString(R.plurals.highlights_limit, it, it)) }
    }
    Column(Modifier.fillMaxSize().background(Nf.colors.canvas)) {
        NfTopBar(
            stringResource(R.string.highlights_title),
            subtitle = stringResource(R.string.highlights_count, count, limit),
            onBack = onBack,
            backDescription = stringResource(app.nearfolk.core.ui.R.string.ui_back),
        )
        val list = highlights
        if (list == null) {
            NfSpinner()
            return@Column
        }
        Text(stringResource(R.string.highlights_body), style = Nf.type.bodySmall, color = Nf.colors.textSecondary, modifier = Modifier.padding(horizontal = 16.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (remaining > 0) {
                item(key = "add") {
                    Box(
                        Modifier
                            .aspectRatio(1f)
                            .clip(NfShapes.medium)
                            .background(Nf.colors.surfaceRaised)
                            .clickable(role = Role.Button, enabled = !adding) {
                                val request = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                if (remaining > 1) pickMany.launch(request) else pickOne.launch(request)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (adding) {
                            NfSpinner()
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Rounded.AddPhotoAlternate, null, tint = Nf.colors.textSecondary)
                                Spacer(Modifier.height(4.dp))
                                Text(stringResource(R.string.highlights_add), style = Nf.type.labelSmall, color = Nf.colors.textSecondary)
                            }
                        }
                    }
                }
            }
            items(list, key = { it.id }) { h ->
                Box(
                    Modifier
                        .animateItem()
                        .aspectRatio(1f)
                        .clip(NfShapes.medium)
                        .clickable(role = Role.Button) { removing = h.id },
                ) {
                    MediaImage(h.media, Modifier.fillMaxSize())
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(24.dp)
                            .clip(NfShapes.small)
                            .background(Nf.colors.scrim),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Delete, stringResource(R.string.highlights_remove), tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
    removing?.let { id ->
        NfConfirmDialog(
            title = stringResource(R.string.highlights_remove_title),
            message = stringResource(R.string.highlights_remove_body),
            confirmLabel = stringResource(R.string.highlights_remove),
            dismissLabel = stringResource(app.nearfolk.core.ui.R.string.ui_cancel),
            onConfirm = {
                removing = null
                viewModel.remove(id)
            },
            onDismiss = { removing = null },
            destructive = true,
        )
    }
}

@Composable
fun ActivityRouteScreen(onBack: () -> Unit, onOpenMember: (String) -> Unit, viewModel: ActivityViewModel = hiltViewModel()) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val time = rememberTimeFormat()
    androidx.compose.runtime.DisposableEffect(viewModel) { onDispose { viewModel.markSeen() } }
    Column(Modifier.fillMaxSize().background(Nf.colors.canvas)) {
        NfTopBar(stringResource(R.string.activity_title), onBack = onBack, backDescription = stringResource(app.nearfolk.core.ui.R.string.ui_back))
        val list = items
        when {
            list == null -> NfSpinner()
            list.isEmpty() -> NfEmptyState(Icons.Rounded.Favorite, stringResource(R.string.activity_empty_title), stringResource(R.string.activity_empty_body))
            else -> LazyColumn(contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)) {
                items(list, key = { it.id }) { item ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(NfShapes.large)
                            .clickable(role = Role.Button) { onOpenMember(item.actorId) }
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box {
                            MemberAvatar(item.actorName, item.actorAvatar, 44.dp)
                            Box(
                                Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(18.dp)
                                    .clip(NfShapes.badge)
                                    .background(Nf.colors.canvas)
                                    .padding(2.dp)
                                    .clip(NfShapes.badge)
                                    .background(if (item.kind == ActivityKind.Like) Nf.colors.danger else Nf.colors.ink),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    when (item.kind) {
                                        ActivityKind.Like -> Icons.Rounded.Favorite
                                        ActivityKind.Comment -> Icons.Rounded.ChatBubble
                                        ActivityKind.Reply -> Icons.AutoMirrored.Rounded.Reply
                                    },
                                    null,
                                    tint = if (item.kind == ActivityKind.Like) Nf.colors.onDanger else Nf.colors.onInk,
                                    modifier = Modifier.size(10.dp),
                                )
                            }
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                when (item.kind) {
                                    ActivityKind.Like -> stringResource(R.string.activity_liked, item.actorName)
                                    ActivityKind.Comment -> stringResource(R.string.activity_commented, item.actorName, item.commentText.orEmpty())
                                    ActivityKind.Reply -> stringResource(R.string.activity_replied, item.actorName, item.commentText.orEmpty())
                                },
                                style = if (item.seen) Nf.type.body else Nf.type.bodyStrong,
                                color = Nf.colors.textPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                "${time.relative(item.createdAt)}  ·  ${item.postCaption}",
                                style = Nf.type.labelSmall,
                                color = Nf.colors.textTertiary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (!item.seen) NfUnreadMark()
                    }
                }
            }
        }
    }
}
