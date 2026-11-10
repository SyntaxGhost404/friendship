package app.nearfolk.core.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldDecorator
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.ThumbDown
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.nearfolk.core.designsystem.component.NfButtonStyle
import app.nearfolk.core.designsystem.component.NfIconButton
import app.nearfolk.core.designsystem.component.NfBottomSheet
import app.nearfolk.core.designsystem.component.NfSpinner
import app.nearfolk.core.designsystem.component.NfTag
import app.nearfolk.core.designsystem.component.shapeSurface
import app.nearfolk.core.designsystem.theme.HapticKind
import app.nearfolk.core.designsystem.theme.LocalAppHaptics
import app.nearfolk.core.designsystem.theme.Nf
import app.nearfolk.core.designsystem.theme.NfMotion
import app.nearfolk.core.designsystem.theme.NfShapes
import app.nearfolk.core.model.Attachment
import app.nearfolk.core.model.Comment
import app.nearfolk.core.model.CommentSort
import app.nearfolk.core.model.CommentsState
import app.nearfolk.core.model.ProfileRules
import app.nearfolk.core.model.Vote

/** What a screen's view model offers its comments sheet. */
interface CommentsHost {
    /** The top-level comment being written. */
    val commentText: TextFieldState

    /** The reply being written on the replies page. */
    val replyText: TextFieldState

    /** Opens the comments of a post, or closes them when [postId] is null. */
    fun openComments(postId: String?)
    fun sortComments(sort: CommentSort)
    fun openReplies(comment: Comment)
    fun closeReplies()

    /** Opens the comment's thread with the reply box pointed at it. */
    fun replyTo(comment: Comment)
    fun clearReplyTarget()
    fun voteComment(comment: Comment, vote: Vote)
    fun sendComment()
    fun sendReply()
}

/** Test tags for the comments sheet. */
object CommentTags {
    const val List = "comments_list"
    const val Replies = "comment_replies"
    const val Field = "comment_field"
}

/**
 * Comments of one post, modelled on YouTube's threads. The sheet lists the
 * top-level comments, by Top or Newest. Each one can have replies, one level
 * deep: "3 replies" opens a replies page inside the sheet with the comment at
 * the top and its replies oldest first. Answering a reply puts the answer in
 * the same thread and names the person answered. Back steps out of the
 * replies page before it closes the sheet.
 *
 * Like and dislike work as up and down votes on one score, like Reddit: a
 * dislike takes one off. Avatars, names and named people open profiles.
 * Comments are text only.
 */
@Composable
fun CommentsSheet(
    state: CommentsState,
    host: CommentsHost,
    onShare: (Comment) -> Unit,
    onOpenProfile: (String) -> Unit,
) {
    var viewing by remember { mutableStateOf<Pair<List<Attachment>, Int>?>(null) }
    var focusReply by remember { mutableIntStateOf(0) }
    val haptics = LocalAppHaptics.current
    val rows = CommentRowActions(
        onOpenProfile = onOpenProfile,
        onVote = { comment, vote ->
            haptics.perform(HapticKind.Tick)
            host.voteComment(comment, vote)
        },
        onReply = { comment ->
            host.replyTo(comment)
            focusReply++
        },
        onShare = onShare,
        onOpenReplies = host::openReplies,
        onOpenAttachment = { list, index -> viewing = list to index },
    )
    NfBottomSheet(onDismiss = { host.openComments(null) }, paneTitle = stringResource(R.string.ui_comments)) {
        BackHandler(enabled = state.openThread != null) { host.closeReplies() }
        Box(Modifier.fillMaxHeight(0.85f).fillMaxWidth().clipToBounds()) {
            val page = state.openThread?.let { ThreadPage(it, state.replies, state.replyingTo) }
            AnimatedContent(
                targetState = page,
                contentKey = { it?.root?.id },
                transitionSpec = {
                    // Pages slide like screens. No fade: popups never draw through a buffer.
                    val forward = targetState != null
                    slideInHorizontally(NfMotion.slide) { if (forward) it else -it } togetherWith
                        slideOutHorizontally(NfMotion.slide) { if (forward) -it else it } using
                        SizeTransform(clip = false)
                },
                label = "commentsPage",
            ) { thread ->
                if (thread == null) {
                    CommentListPage(state, host, rows)
                } else {
                    RepliesPage(thread, state.postAuthorId, host, rows, focusReply)
                }
            }
        }
    }
    viewing?.let { (list, index) -> AttachmentViewer(list, index, onDismiss = { viewing = null }) }
}

private data class ThreadPage(val root: Comment, val replies: List<Comment>, val replyingTo: Comment?)

private class CommentRowActions(
    val onOpenProfile: (String) -> Unit,
    val onVote: (Comment, Vote) -> Unit,
    val onReply: (Comment) -> Unit,
    val onShare: (Comment) -> Unit,
    val onOpenReplies: (Comment) -> Unit,
    val onOpenAttachment: (List<Attachment>, Int) -> Unit,
)

@Composable
private fun CommentListPage(state: CommentsState, host: CommentsHost, rows: CommentRowActions) {
    val counts = rememberCountFormat()
    val listState = rememberLazyListState()
    var scrollToNewest by remember { mutableStateOf(false) }
    // What you just wrote goes to the top of the list; bring it into view.
    LaunchedEffect(state.threads.firstOrNull()?.id) {
        if (scrollToNewest && state.threads.isNotEmpty()) {
            listState.animateScrollToItem(0)
            scrollToNewest = false
        }
    }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 2.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.ui_comments), style = Nf.type.title, color = Nf.colors.textPrimary, modifier = Modifier.semantics { heading() })
            if (state.total > 0) {
                Spacer(Modifier.width(8.dp))
                Text(counts.short(state.total), style = Nf.type.title, color = Nf.colors.textTertiary)
            }
        }
        Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SortChip(stringResource(R.string.ui_sort_top), state.sort == CommentSort.Top) { host.sortComments(CommentSort.Top) }
            SortChip(stringResource(R.string.ui_sort_newest), state.sort == CommentSort.Newest) { host.sortComments(CommentSort.Newest) }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.loading -> NfSpinner()
                state.threads.isEmpty() -> Text(
                    stringResource(R.string.ui_comments_empty),
                    style = Nf.type.body,
                    color = Nf.colors.textTertiary,
                    modifier = Modifier.align(Alignment.Center).padding(horizontal = 32.dp),
                )
                else -> LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(start = 16.dp, end = 12.dp, top = 4.dp, bottom = 8.dp),
                    modifier = Modifier.fillMaxSize().testTag(CommentTags.List),
                ) {
                    items(state.threads, key = { it.id }) { comment ->
                        CommentItem(
                            comment = comment,
                            byPostAuthor = comment.authorId == state.postAuthorId,
                            actions = rows,
                            showReplies = true,
                        )
                    }
                }
            }
        }
        CommentComposer(
            text = host.commentText,
            onSend = {
                scrollToNewest = true
                host.sendComment()
            },
            placeholder = stringResource(R.string.ui_comment_hint),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun RepliesPage(thread: ThreadPage, postAuthorId: String?, host: CommentsHost, rows: CommentRowActions, focusReply: Int) {
    val listState = rememberLazyListState()
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    var scrollToNewest by remember { mutableStateOf(false) }
    // Tapping reply puts the cursor in the reply box, as YouTube does.
    LaunchedEffect(focusReply) {
        if (focusReply > 0) {
            focus.requestFocus()
            keyboard?.show()
        }
    }
    // A new reply lands at the end of the thread; bring it into view.
    LaunchedEffect(thread.replies.lastOrNull()?.id) {
        if (scrollToNewest && thread.replies.isNotEmpty()) {
            listState.animateScrollToItem(thread.replies.size)
            scrollToNewest = false
        }
    }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 20.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            NfIconButton(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.ui_replies_back), host::closeReplies, tint = Nf.colors.textPrimary)
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.ui_replies_title), style = Nf.type.title, color = Nf.colors.textPrimary, modifier = Modifier.semantics { heading() })
        }
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 8.dp),
            modifier = Modifier.weight(1f).fillMaxWidth().testTag(CommentTags.Replies),
        ) {
            item(key = thread.root.id) {
                // The comment everyone is answering, set apart at the top.
                CommentItem(
                    comment = thread.root,
                    byPostAuthor = thread.root.authorId == postAuthorId,
                    actions = rows,
                    modifier = Modifier.background(Nf.colors.surfaceRaised, NfShapes.large).padding(horizontal = 8.dp),
                )
            }
            if (thread.replies.isEmpty()) {
                item(key = "noReplies") {
                    Text(
                        stringResource(R.string.ui_replies_empty),
                        style = Nf.type.bodySmall,
                        color = Nf.colors.textTertiary,
                        modifier = Modifier.padding(start = 54.dp, top = 12.dp),
                    )
                }
            }
            items(thread.replies, key = { it.id }) { reply ->
                CommentItem(
                    comment = reply,
                    byPostAuthor = reply.authorId == postAuthorId,
                    actions = rows,
                    avatarSize = 28.dp,
                    modifier = Modifier.padding(start = 44.dp),
                )
            }
        }
        thread.replyingTo?.let { target ->
            ReplyTarget(target.authorName, onClear = host::clearReplyTarget)
        }
        CommentComposer(
            text = host.replyText,
            onSend = {
                scrollToNewest = true
                host.sendReply()
            },
            placeholder = stringResource(R.string.ui_reply_hint),
            focusRequester = focus,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun CommentItem(
    comment: Comment,
    byPostAuthor: Boolean,
    actions: CommentRowActions,
    modifier: Modifier = Modifier,
    avatarSize: Dp = 36.dp,
    showReplies: Boolean = false,
) {
    val time = rememberTimeFormat()
    val openLabel = stringResource(R.string.ui_open_profile)
    Row(modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            Modifier
                .padding(top = 2.dp)
                .clip(NfShapes.avatar)
                .clickable(onClickLabel = openLabel, role = Role.Button) { actions.onOpenProfile(comment.authorId) },
        ) {
            MemberAvatar(comment.authorName, comment.authorAvatar, avatarSize, ringColor = Color.Transparent)
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    comment.authorName,
                    style = Nf.type.label,
                    color = Nf.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clip(NfShapes.badge)
                        .clickable(onClickLabel = openLabel, role = Role.Button) { actions.onOpenProfile(comment.authorId) }
                        .padding(vertical = 2.dp),
                )
                if (byPostAuthor) NfTag(stringResource(R.string.ui_comment_author_tag))
                Text(time.relative(comment.createdAt), style = Nf.type.labelSmall, color = Nf.colors.textTertiary, maxLines = 1)
            }
            CommentText(comment, onOpenProfile = actions.onOpenProfile)
            if (comment.attachments.isNotEmpty()) {
                AttachmentGrid(
                    comment.attachments,
                    onOpen = { actions.onOpenAttachment(comment.attachments, it) },
                    corner = 12.dp,
                    modifier = Modifier.padding(top = 6.dp).widthIn(max = 240.dp),
                )
            }
            CommentActionRow(comment, actions)
            if (showReplies && comment.replyCount > 0) {
                RepliesLink(comment.replyCount) { actions.onOpenReplies(comment) }
            }
        }
    }
}

/** The comment, with "@Name" in front when it answers another reply. The name opens that profile. */
@Composable
private fun CommentText(comment: Comment, onOpenProfile: (String) -> Unit) {
    val mentionId = comment.replyToId
    val mentionName = comment.replyToName
    val accent = Nf.colors.accent
    val text = if (mentionId == null || mentionName == null) {
        buildAnnotatedString { append(comment.text) }
    } else {
        buildAnnotatedString {
            withLink(
                LinkAnnotation.Clickable(
                    tag = mentionId,
                    styles = TextLinkStyles(style = SpanStyle(color = accent, fontWeight = FontWeight.SemiBold)),
                ) { onOpenProfile(mentionId) },
            ) { append("@$mentionName") }
            append(" ")
            append(comment.text)
        }
    }
    Text(text, style = Nf.type.body, color = Nf.colors.textPrimary, modifier = Modifier.padding(top = 2.dp))
}

/**
 * Like, the score, dislike, reply and share. The score is likes minus
 * dislikes; your own vote is filled in and coloured.
 */
@Composable
private fun CommentActionRow(comment: Comment, actions: CommentRowActions) {
    val counts = rememberCountFormat()
    val liked = comment.myVote == Vote.Up
    val disliked = comment.myVote == Vote.Down
    val scoreColor = when {
        liked -> Nf.colors.accent
        disliked -> Nf.colors.textPrimary
        else -> Nf.colors.textSecondary
    }
    val scoreLabel = stringResource(R.string.ui_comment_score, counts.full(comment.score))
    // Pulled left so the first icon lines up with the text above it.
    Row(Modifier.offset(x = (-10).dp), verticalAlignment = Alignment.CenterVertically) {
        CommentAction(
            icon = if (liked) Icons.Rounded.ThumbUp else Icons.Outlined.ThumbUp,
            label = stringResource(if (liked) R.string.ui_comment_unlike else R.string.ui_comment_like),
            tint = if (liked) Nf.colors.accent else Nf.colors.textSecondary,
            onClick = { actions.onVote(comment, Vote.Up) },
        )
        Text(
            counts.short(comment.score),
            style = Nf.type.labelSmall,
            color = scoreColor,
            modifier = Modifier.padding(end = 2.dp).semantics { contentDescription = scoreLabel },
        )
        CommentAction(
            icon = if (disliked) Icons.Rounded.ThumbDown else Icons.Outlined.ThumbDown,
            label = stringResource(if (disliked) R.string.ui_comment_undislike else R.string.ui_comment_dislike),
            tint = if (disliked) Nf.colors.textPrimary else Nf.colors.textSecondary,
            onClick = { actions.onVote(comment, Vote.Down) },
        )
        CommentAction(
            icon = Icons.AutoMirrored.Rounded.Reply,
            label = stringResource(R.string.ui_comment_reply),
            tint = Nf.colors.textSecondary,
            onClick = { actions.onReply(comment) },
        )
        CommentAction(
            icon = Icons.Rounded.Share,
            label = stringResource(R.string.ui_comment_share),
            tint = Nf.colors.textSecondary,
            onClick = { actions.onShare(comment) },
        )
    }
}

@Composable
private fun CommentAction(icon: ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .size(40.dp)
            .clip(NfShapes.small)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(18.dp))
    }
}

/** "3 replies", which opens the replies page. */
@Composable
private fun RepliesLink(count: Int, onClick: () -> Unit) {
    Row(
        Modifier
            .offset(x = (-8).dp)
            .clip(NfShapes.small)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 8.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(pluralStringResource(R.plurals.ui_comment_replies, count, count), style = Nf.type.label, color = Nf.colors.accent)
        Icon(Icons.Rounded.ChevronRight, null, tint = Nf.colors.accent, modifier = Modifier.size(18.dp))
    }
}

/** Shows whose reply is being answered, with a way back to answering the thread. */
@Composable
private fun ReplyTarget(name: String, onClear: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 12.dp, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.ui_replying_to, name),
            style = Nf.type.labelSmall,
            color = Nf.colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        NfIconButton(
            Icons.Rounded.Close,
            stringResource(R.string.ui_replying_to_clear, name),
            onClear,
            size = 32.dp,
            iconSize = 16.dp,
            tint = Nf.colors.textSecondary,
        )
    }
}

@Composable
private fun SortChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = Nf.colors
    Box(
        Modifier
            .height(32.dp)
            // A shape fill and no ripple: this sits in a popup window.
            .background(if (selected) colors.ink else colors.surfaceRaised, NfShapes.small)
            .selectable(selected = selected, interactionSource = null, indication = null, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = Nf.type.label, color = if (selected) colors.onInk else colors.textPrimary)
    }
}

/**
 * The text-only box for comments and replies: the text and a send button,
 * nothing else. It grows to a quarter of the space above the keyboard, then
 * scrolls inside. Drawn as a shape fill, not a clip, because it lives in a sheet.
 */
@Composable
fun CommentComposer(
    text: TextFieldState,
    onSend: () -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
    maxLength: Int = ProfileRules.COMMENT_MAX,
) {
    val colors = Nf.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val density = LocalDensity.current
    val windowHeight = LocalWindowInfo.current.containerSize.height
    val keyboard = WindowInsets.imeAnimationTarget.getBottom(density)
    val lineHeight = with(density) { Nf.type.body.lineHeight.toPx() }
    val maxLines = composerMaxLines(windowHeight - keyboard, lineHeight)
    val length = text.text.length
    Row(
        modifier
            .fillMaxWidth()
            .shapeSurface(colors.surfaceRaised, NfShapes.large, stroke = if (focused) colors.lineStrong else colors.line)
            .padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        BasicTextField(
            state = text,
            lineLimits = TextFieldLineLimits.MultiLine(minHeightInLines = 1, maxHeightInLines = maxLines),
            inputTransformation = InputTransformation.maxLength(maxLength),
            textStyle = Nf.type.body.copy(color = colors.textPrimary),
            cursorBrush = SolidColor(colors.accent),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            interactionSource = interaction,
            modifier = Modifier
                .weight(1f)
                .padding(top = 9.dp, bottom = 9.dp, end = 8.dp)
                .focusRequester(focusRequester)
                .testTag(CommentTags.Field),
            decorator = TextFieldDecorator { inner ->
                Box {
                    if (text.text.isEmpty()) {
                        Text(placeholder, style = Nf.type.body, color = colors.textTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    inner()
                }
            },
        )
        if (length >= maxLength * 0.9) {
            Text(
                "$length/$maxLength",
                style = Nf.type.labelSmall,
                color = if (length >= maxLength) colors.danger else colors.textTertiary,
                modifier = Modifier.align(Alignment.CenterVertically).padding(end = 8.dp),
            )
        }
        NfIconButton(
            Icons.AutoMirrored.Rounded.Send,
            stringResource(R.string.ui_composer_send),
            onClick = onSend,
            style = NfButtonStyle.Primary,
            size = 40.dp,
            iconSize = 20.dp,
            enabled = text.text.isNotBlank(),
        )
    }
}
