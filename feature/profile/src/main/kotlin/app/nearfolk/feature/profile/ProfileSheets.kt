package app.nearfolk.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.remember
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.nearfolk.core.designsystem.component.NfBottomSheet
import app.nearfolk.core.designsystem.component.NfButtonStyle
import app.nearfolk.core.designsystem.component.NfFullScreenDialog
import app.nearfolk.core.designsystem.component.NfIconButton
import app.nearfolk.core.designsystem.theme.Nf
import app.nearfolk.core.designsystem.theme.NfShapes
import app.nearfolk.core.model.Attachment
import app.nearfolk.core.model.Comment
import app.nearfolk.core.model.DraftAttachment
import app.nearfolk.core.model.MediaSource
import app.nearfolk.core.model.Highlight
import app.nearfolk.core.model.ProfileRules
import app.nearfolk.core.ui.AttachmentGrid
import app.nearfolk.core.ui.AttachmentViewer
import app.nearfolk.core.ui.MediaImage
import app.nearfolk.core.ui.MessageComposer
import app.nearfolk.core.ui.MemberAvatar
import app.nearfolk.core.ui.rememberTimeFormat

/** Full screen pager over a member's highlights. */
@Composable
internal fun HighlightViewer(highlights: List<Highlight>, start: Int, onDismiss: () -> Unit) {
    if (highlights.isEmpty()) return
    NfFullScreenDialog(onDismiss) {
        val pager = rememberPagerState(initialPage = start.coerceIn(0, highlights.lastIndex)) { highlights.size }
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            HorizontalPager(pager, Modifier.fillMaxSize()) { page ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    MediaImage(
                        highlights[page].media,
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .padding(12.dp)
                            .clip(NfShapes.large),
                    )
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NfIconButton(Icons.Rounded.Close, stringResource(app.nearfolk.core.ui.R.string.ui_close), onDismiss, style = NfButtonStyle.Secondary)
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.profile_highlight_counter, pager.currentPage + 1, highlights.size),
                        style = Nf.type.label,
                        color = Color.White,
                        modifier = Modifier
                            .clip(NfShapes.small)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
                Box(Modifier.padding(20.dp))
            }
        }
    }
}

@Composable
internal fun CommentsSheet(
    comments: List<Comment>,
    text: TextFieldState,
    attachments: List<DraftAttachment>,
    onAddMedia: (List<MediaSource>) -> Unit,
    onRemoveAttachment: (String) -> Unit,
    onDismiss: () -> Unit,
    onSend: () -> Unit,
) {
    val time = rememberTimeFormat()
    var viewing by remember { mutableStateOf<Pair<List<Attachment>, Int>?>(null) }
    NfBottomSheet(onDismiss = onDismiss, title = stringResource(R.string.profile_comments_title)) {
        Column(Modifier.fillMaxHeight(0.85f)) {
            if (comments.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.profile_comments_empty), style = Nf.type.body, color = Nf.colors.textTertiary)
                }
            } else {
                LazyColumn(Modifier.weight(1f).padding(horizontal = 20.dp)) {
                    items(comments, key = { it.id }) { c ->
                        Row(Modifier.padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            MemberAvatar(c.authorName, c.authorAvatar, 32.dp)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(c.authorName, style = Nf.type.label, color = Nf.colors.textPrimary)
                                    Text(time.relative(c.createdAt), style = Nf.type.labelSmall, color = Nf.colors.textTertiary)
                                }
                                if (c.text.isNotEmpty()) Text(c.text, style = Nf.type.body, color = Nf.colors.textPrimary)
                                if (c.attachments.isNotEmpty()) {
                                    AttachmentGrid(
                                        c.attachments,
                                        onOpen = { viewing = c.attachments to it },
                                        corner = 12.dp,
                                        modifier = Modifier.widthIn(max = 240.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
            MessageComposer(
                text = text,
                attachments = attachments,
                onAddMedia = onAddMedia,
                onRemoveAttachment = onRemoveAttachment,
                onSend = onSend,
                placeholder = stringResource(R.string.profile_comment_hint),
                maxLength = ProfileRules.COMMENT_MAX,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
    viewing?.let { (list, index) -> AttachmentViewer(list, index, onDismiss = { viewing = null }) }
}
