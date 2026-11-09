package app.nearfolk.core.ui

import android.widget.VideoView
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.nearfolk.core.designsystem.component.NfButtonStyle
import app.nearfolk.core.designsystem.component.NfFullScreenDialog
import app.nearfolk.core.designsystem.component.NfIconButton
import app.nearfolk.core.designsystem.shape.SquircleShape
import app.nearfolk.core.designsystem.theme.Nf
import app.nearfolk.core.designsystem.theme.NfShapes
import app.nearfolk.core.model.Attachment
import app.nearfolk.core.model.AttachmentKind
import coil3.compose.AsyncImage
import java.io.File

/** Thumbnail for one attachment: photo or GIF frame, or a video frame with a play mark and its length. */
@Composable
fun AttachmentThumbnail(attachment: Attachment, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Crop) {
    Box(modifier.background(Nf.colors.surfaceTop)) {
        AsyncImage(
            model = File(attachment.path),
            contentDescription = null,
            contentScale = contentScale,
            modifier = Modifier.fillMaxSize(),
        )
        when (attachment.kind) {
            AttachmentKind.Video -> {
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .size(36.dp)
                        .clip(NfShapes.medium)
                        .background(Color.Black.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(22.dp)) }
                if (attachment.durationMillis > 0) MediaLabel(formatDuration(attachment.durationMillis), Modifier.align(Alignment.BottomStart))
            }
            AttachmentKind.Gif -> MediaLabel(stringResource(R.string.ui_gif), Modifier.align(Alignment.TopStart))
            AttachmentKind.Image -> Unit
        }
    }
}

@Composable
private fun MediaLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = Nf.type.caption,
        color = Color.White,
        modifier = modifier
            .padding(6.dp)
            .clip(NfShapes.badge)
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 5.dp, vertical = 2.dp),
    )
}

/**
 * Media inside a message or comment. One item keeps its own proportions;
 * more items share a square grid, and anything past six is summed up on the
 * last tile.
 */
@Composable
fun AttachmentGrid(
    attachments: List<Attachment>,
    onOpen: (Int) -> Unit,
    modifier: Modifier = Modifier,
    corner: Dp = 14.dp,
    gap: Dp = 3.dp,
) {
    if (attachments.isEmpty()) return
    val shape = SquircleShape(corner)
    val total = attachments.size
    @Composable
    fun Tile(index: Int, tileModifier: Modifier, overflow: Int = 0) {
        val description = stringResource(R.string.ui_open_attachment, index + 1, total)
        Box(
            tileModifier
                .clip(shape)
                .clickable(role = Role.Image) { onOpen(index) }
                .semantics { contentDescription = description },
        ) {
            AttachmentThumbnail(attachments[index], Modifier.fillMaxSize())
            if (overflow > 0) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                    Text("+$overflow", style = Nf.type.title, color = Color.White)
                }
            }
        }
    }
    if (total == 1) {
        Tile(0, modifier.fillMaxWidth().aspectRatio(attachments[0].aspectRatio.coerceIn(0.66f, 1.6f)))
        return
    }
    val columns = when (total) {
        2, 4 -> 2
        else -> 3
    }
    val shown = attachments.indices.take(if (columns == 2) 4 else 6)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(gap)) {
        shown.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                row.forEach { index ->
                    val overflow = if (index == shown.last()) total - shown.size else 0
                    Tile(index, Modifier.weight(1f).aspectRatio(1f), overflow)
                }
                repeat(columns - row.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
}

/** Full screen pager over attachments. Photos and GIFs fit the screen; videos play on tap. */
@Composable
fun AttachmentViewer(attachments: List<Attachment>, start: Int, onDismiss: () -> Unit) {
    if (attachments.isEmpty()) return
    NfFullScreenDialog(onDismiss) {
        val pager = rememberPagerState(initialPage = start.coerceIn(0, attachments.lastIndex)) { attachments.size }
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            HorizontalPager(pager, Modifier.fillMaxSize()) { page ->
                val item = attachments[page]
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (item.kind == AttachmentKind.Video) {
                        VideoPlayer(item.path, active = pager.currentPage == page, modifier = Modifier.fillMaxWidth().aspectRatio(item.aspectRatio))
                    } else {
                        AsyncImage(model = File(item.path), contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NfIconButton(Icons.Rounded.Close, stringResource(R.string.ui_close), onDismiss, style = NfButtonStyle.Secondary)
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    if (attachments.size > 1) {
                        Text(
                            stringResource(R.string.ui_viewer_counter, pager.currentPage + 1, attachments.size),
                            style = Nf.type.label,
                            color = Color.White,
                            modifier = Modifier.clip(NfShapes.small).background(Color.Black.copy(alpha = 0.5f)).padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
                Box(Modifier.size(40.dp))
            }
        }
    }
}

@Composable
private fun VideoPlayer(path: String, active: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var playing by remember { mutableStateOf(false) }
    val view = remember(path) {
        VideoView(context).apply {
            setVideoPath(path)
            setOnPreparedListener { seekTo(1) }
            setOnCompletionListener { playing = false }
        }
    }
    LaunchedEffect(active) {
        if (!active && view.isPlaying) {
            view.pause()
            playing = false
        }
    }
    DisposableEffect(view) { onDispose { view.stopPlayback() } }
    val label = stringResource(if (playing) R.string.ui_video_pause else R.string.ui_video_play)
    Box(
        modifier
            .clickable(role = Role.Button) {
                if (view.isPlaying) view.pause() else view.start()
                playing = view.isPlaying
            }
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        AndroidView({ view }, Modifier.fillMaxSize())
        if (!playing) {
            Box(
                Modifier.size(64.dp).clip(NfShapes.large).background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center,
            ) { Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(36.dp)) }
        }
    }
}

/** Short description of media for previews, such as "Photo", "3 videos" or "4 attachments". */
@Composable
fun attachmentSummary(attachments: List<Attachment>): String {
    val kinds = attachments.map { it.kind }.toSet()
    val n = attachments.size
    return when {
        n == 0 -> ""
        kinds == setOf(AttachmentKind.Image) -> pluralStringResource(R.plurals.ui_summary_photos, n, n)
        kinds == setOf(AttachmentKind.Video) -> pluralStringResource(R.plurals.ui_summary_videos, n, n)
        kinds == setOf(AttachmentKind.Gif) -> pluralStringResource(R.plurals.ui_summary_gifs, n, n)
        else -> pluralStringResource(R.plurals.ui_summary_mixed, n, n)
    }
}

@StringRes
fun app.nearfolk.core.model.AttachmentProblem.messageRes(): Int = when (this) {
    app.nearfolk.core.model.AttachmentProblem.TooLarge -> R.string.ui_attach_too_large
    app.nearfolk.core.model.AttachmentProblem.Unsupported -> R.string.ui_attach_unsupported
    app.nearfolk.core.model.AttachmentProblem.LimitReached -> 0
    app.nearfolk.core.model.AttachmentProblem.Failed -> R.string.ui_attach_failed
}
