package app.nearfolk.core.ui

import android.icu.text.CompactDecimalFormat
import android.icu.text.NumberFormat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.nearfolk.core.designsystem.component.NfBottomSheet
import app.nearfolk.core.designsystem.component.NfIconButton
import app.nearfolk.core.designsystem.theme.HapticKind
import app.nearfolk.core.designsystem.theme.LocalAppHaptics
import app.nearfolk.core.designsystem.theme.Nf
import app.nearfolk.core.designsystem.theme.NfShapes
import app.nearfolk.core.model.Post
import java.time.Instant
import java.util.Locale
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * What the buttons under a post do. A screen builds one and hands it to every
 * [PostItem], so posts behave the same in the feed, on profiles and in lists.
 */
@Immutable
class PostActions(
    val onToggleLike: (Post) -> Unit,
    val onOpenComments: (Post) -> Unit,
    val onToggleSave: (Post) -> Unit,
    val onShare: (Post) -> Unit,
    val onOpenStats: (Post) -> Unit,
    /** Null where the author is already on screen, such as their own profile. */
    val onOpenAuthor: ((Post) -> Unit)? = null,
) {
    companion object {
        val None = PostActions({}, {}, {}, {}, {})
    }
}

/** Content type of post rows in a lazy list. [TrackPostViews] counts these. */
const val PostContentType = "post"

/**
 * One post in a stream. Flat, separated by spacing, media in a squircle frame.
 * Under it sit five actions that share the width equally: like, comment,
 * views, save and share, each with its live count.
 */
@Composable
fun PostItem(
    post: Post,
    actions: PostActions,
    modifier: Modifier = Modifier,
    onOptions: (() -> Unit)? = null,
    now: Instant = Instant.now(),
) {
    val time = rememberTimeFormat()
    Column(modifier.fillMaxWidth().padding(top = 14.dp, bottom = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val openAuthor = actions.onOpenAuthor
            Row(
                Modifier
                    .weight(1f)
                    .then(
                        if (openAuthor == null) {
                            Modifier
                        } else {
                            Modifier
                                .clip(NfShapes.small)
                                .clickable(
                                    onClickLabel = stringResource(R.string.ui_open_profile),
                                    role = Role.Button,
                                    onClick = { openAuthor(post) },
                                )
                        },
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MemberAvatar(post.authorName, post.authorAvatar, 36.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(post.authorName, style = Nf.type.label, color = Nf.colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(time.relative(post.createdAt, now), style = Nf.type.labelSmall, color = Nf.colors.textTertiary)
                }
            }
            if (onOptions != null) {
                NfIconButton(Icons.Rounded.MoreHoriz, stringResource(R.string.ui_post_options), onOptions, size = 36.dp, tint = Nf.colors.textSecondary)
            }
        }
        if (post.caption.isNotBlank()) {
            Text(post.caption, style = Nf.type.body, color = Nf.colors.textPrimary)
        }
        post.media?.let { media ->
            MediaImage(
                media,
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(NfShapes.large),
            )
        }
        PostActionRow(post, actions)
    }
}

@Composable
private fun PostActionRow(post: Post, actions: PostActions) {
    val haptics = LocalAppHaptics.current
    val counts = rememberCountFormat()
    val likeBounce = rememberBounce(post.likedByMe)
    val saveBounce = rememberBounce(post.savedByMe)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        PostAction(
            icon = if (post.likedByMe) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            label = stringResource(if (post.likedByMe) R.string.ui_unlike else R.string.ui_like),
            count = post.likeCount,
            countLabel = pluralStringResource(R.plurals.ui_count_likes, post.likeCount, counts.full(post.likeCount)),
            counts = counts,
            active = if (post.likedByMe) Nf.colors.danger else null,
            iconScale = likeBounce,
            onClick = {
                haptics.perform(if (post.likedByMe) HapticKind.Tick else HapticKind.Confirm)
                actions.onToggleLike(post)
            },
        )
        PostAction(
            icon = Icons.Rounded.ChatBubbleOutline,
            label = stringResource(R.string.ui_comments),
            count = post.commentCount,
            countLabel = pluralStringResource(R.plurals.ui_count_comments, post.commentCount, counts.full(post.commentCount)),
            counts = counts,
            onClick = { actions.onOpenComments(post) },
        )
        PostAction(
            icon = Icons.Rounded.BarChart,
            label = stringResource(R.string.ui_views),
            count = post.viewCount,
            countLabel = pluralStringResource(R.plurals.ui_count_views, post.viewCount, counts.full(post.viewCount)),
            counts = counts,
            onClick = { actions.onOpenStats(post) },
        )
        PostAction(
            icon = if (post.savedByMe) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
            label = stringResource(if (post.savedByMe) R.string.ui_unsave else R.string.ui_save),
            count = post.saveCount,
            countLabel = pluralStringResource(R.plurals.ui_count_saves, post.saveCount, counts.full(post.saveCount)),
            counts = counts,
            active = if (post.savedByMe) Nf.colors.accent else null,
            iconScale = saveBounce,
            onClick = {
                haptics.perform(if (post.savedByMe) HapticKind.Tick else HapticKind.Confirm)
                actions.onToggleSave(post)
            },
        )
        PostAction(
            icon = Icons.Rounded.Share,
            label = stringResource(R.string.ui_share),
            count = post.shareCount,
            countLabel = pluralStringResource(R.plurals.ui_count_shares, post.shareCount, counts.full(post.shareCount)),
            counts = counts,
            onClick = {
                haptics.perform(HapticKind.Tap)
                actions.onShare(post)
            },
        )
    }
}

/**
 * One fifth of the action row. The whole cell is the touch target; the icon
 * and its count sit centred in it. [active] tints both when the action is on.
 */
@Composable
private fun RowScope.PostAction(
    icon: ImageVector,
    label: String,
    count: Int,
    countLabel: String,
    counts: CountFormat,
    onClick: () -> Unit,
    active: Color? = null,
    iconScale: Animatable<Float, *>? = null,
) {
    val tint = active ?: Nf.colors.textSecondary
    Row(
        Modifier
            .weight(1f)
            .heightIn(min = 44.dp)
            .clip(NfShapes.small)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier
                .size(19.dp)
                .then(if (iconScale == null) Modifier else Modifier.graphicsLayer { scaleX = iconScale.value; scaleY = iconScale.value }),
        )
        AnimatedContent(
            targetState = count,
            transitionSpec = {
                // Counts roll up when they grow and down when they shrink.
                val up = targetState > initialState
                (slideInVertically { if (up) it else -it } + fadeIn()) togetherWith (slideOutVertically { if (up) -it else it } + fadeOut())
            },
            label = "count",
        ) { value ->
            Text(
                counts.short(value),
                style = Nf.type.labelSmall,
                color = if (active != null) active else Nf.colors.textSecondary,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.semantics { contentDescription = countLabel },
            )
        }
    }
}

@Composable
private fun rememberBounce(on: Boolean): Animatable<Float, *> {
    val bounce = remember { Animatable(1f) }
    var first by remember { mutableStateOf(true) }
    LaunchedEffect(on) {
        // Bounce when the state changes, not when the row first appears.
        if (on && !first) {
            bounce.snapTo(0.7f)
            bounce.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 500f))
        }
        first = false
    }
    return bounce
}

/** Formats counts for the current locale: compact under a post, in full where there is room. */
class CountFormat internal constructor(locale: Locale) {
    private val compact = CompactDecimalFormat.getInstance(locale, CompactDecimalFormat.CompactStyle.SHORT)
    private val exact = NumberFormat.getIntegerInstance(locale)

    /** 999, 1.2K, 15K, 1.2M in English; the locale's own forms elsewhere. */
    fun short(value: Int): String = if (value < 1_000) exact.format(value.toLong()) else compact.format(value.toLong())

    /** 1,234 in English. */
    fun full(value: Int): String = exact.format(value.toLong())
}

@Composable
fun rememberCountFormat(): CountFormat {
    val locale = LocalConfiguration.current.locales[0]
    return remember(locale) { CountFormat(locale) }
}

/**
 * Counts a view for each post row that is at least half on screen. The
 * repository counts a post once per session, so scrolling back and forth
 * does not inflate the number.
 */
@Composable
fun TrackPostViews(state: LazyListState, onViewed: (String) -> Unit) {
    val current by rememberUpdatedState(onViewed)
    LaunchedEffect(state) {
        snapshotFlow {
            val info = state.layoutInfo
            info.visibleItemsInfo
                .filter { item ->
                    if (item.contentType != PostContentType || item.size <= 0) return@filter false
                    val shown = minOf(item.offset + item.size, info.viewportEndOffset) - maxOf(item.offset, info.viewportStartOffset)
                    val needed = minOf(item.size, info.viewportEndOffset - info.viewportStartOffset) / 2
                    shown >= needed
                }
                .mapNotNull { it.key as? String }
        }
            .distinctUntilChanged()
            .collect { ids -> ids.forEach { current(it) } }
    }
}

/**
 * Everything counted for one post. Opened from the views button. Counts update
 * while the sheet is open.
 */
@Composable
fun PostStatsSheet(post: Post, onDismiss: () -> Unit) {
    val counts = rememberCountFormat()
    NfBottomSheet(onDismiss = onDismiss, title = stringResource(R.string.ui_stats_title)) {
        Column(Modifier.padding(horizontal = 20.dp).padding(top = 4.dp, bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    Modifier.size(52.dp).background(Nf.colors.surfaceRaised, NfShapes.medium),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.BarChart, null, tint = Nf.colors.textPrimary, modifier = Modifier.size(26.dp))
                }
                Column {
                    Text(counts.full(post.viewCount), style = Nf.type.headline, color = Nf.colors.textPrimary)
                    Text(stringResource(R.string.ui_stats_views), style = Nf.type.label, color = Nf.colors.textSecondary)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.ui_stats_views_body), style = Nf.type.bodySmall, color = Nf.colors.textSecondary)
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile(counts.full(post.likeCount), stringResource(R.string.ui_stats_likes), Modifier.weight(1f))
                StatTile(counts.full(post.commentCount), stringResource(R.string.ui_stats_comments), Modifier.weight(1f))
                StatTile(counts.full(post.saveCount), stringResource(R.string.ui_stats_saves), Modifier.weight(1f))
                StatTile(counts.full(post.shareCount), stringResource(R.string.ui_stats_shares), Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.ui_stats_private), style = Nf.type.labelSmall, color = Nf.colors.textTertiary)
        }
    }
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        // A shape fill, not a clip: this sits in a popup window.
        modifier.background(Nf.colors.surfaceRaised, NfShapes.medium).padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = Nf.type.title, color = Nf.colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(label, style = Nf.type.labelSmall, color = Nf.colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * Returns what to call after the save button was pressed with the post's state
 * before the press. Saving shows a short note with a way to the saved list;
 * removing a post from the list says nothing.
 */
@Composable
fun rememberSavedNotice(onOpenSaved: () -> Unit): (wasSaved: Boolean) -> Unit {
    val snackbar = LocalSnackbarHost.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val open by rememberUpdatedState(onOpenSaved)
    return remember(snackbar, resources, scope) {
        { wasSaved ->
            if (!wasSaved) {
                scope.launch {
                    snackbar.currentSnackbarData?.dismiss()
                    val result = snackbar.showSnackbar(
                        message = resources.getString(R.string.ui_saved_notice),
                        actionLabel = resources.getString(R.string.ui_saved_view),
                        // With an action the default is to stay until dismissed.
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) open()
                }
            }
        }
    }
}
