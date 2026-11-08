package app.nearfolk.core.ui

import android.content.ClipDescription
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.content.MediaType
import androidx.compose.foundation.content.ReceiveContentListener
import androidx.compose.foundation.content.TransferableContent
import androidx.compose.foundation.content.consume
import androidx.compose.foundation.content.contentReceiver
import androidx.compose.foundation.content.hasMediaType
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldDecorator
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import app.nearfolk.core.designsystem.component.NfButtonStyle
import app.nearfolk.core.designsystem.component.NfIconButton
import app.nearfolk.core.designsystem.theme.Nf
import app.nearfolk.core.designsystem.theme.NfShapes
import app.nearfolk.core.model.AttachmentLimits
import app.nearfolk.core.model.AttachmentProblem
import app.nearfolk.core.model.DraftAttachment
import app.nearfolk.core.model.MediaSource
import kotlinx.coroutines.flow.Flow
import kotlin.math.floor

/** Test tags so the composer can be measured and driven in tests. */
object ComposerTags {
    const val Container = "composer"
    const val Field = "composer_field"
    const val Actions = "composer_actions"
}

/**
 * How many lines the text area may grow to before it scrolls inside the box.
 * A quarter of the height that is visible above the keyboard, between 3 and
 * 12 lines, so the conversation always keeps most of the screen.
 */
fun composerMaxLines(visibleHeightPx: Int, lineHeightPx: Float): Int {
    if (visibleHeightPx <= 0 || lineHeightPx <= 0f) return 4
    return floor(visibleHeightPx * 0.25f / lineHeightPx).toInt().coerceIn(3, 12)
}

/**
 * One container for writing a message or a comment: attachments waiting to
 * be sent, the text, and a fixed action row with attach on the left and
 * send on the right.
 *
 * The text grows with its content up to [composerMaxLines] and then scrolls
 * inside the box, so the action row never moves. Photos, videos and GIFs
 * come from the system picker or straight from the keyboard (GIF and
 * sticker panels), up to [maxAttachments]. Send is always on screen: dimmed
 * while there is nothing to send or media is still being added.
 */
@Composable
fun MessageComposer(
    text: TextFieldState,
    attachments: List<DraftAttachment>,
    onAddMedia: (List<MediaSource>) -> Unit,
    onRemoveAttachment: (String) -> Unit,
    onSend: () -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    maxLength: Int = 1000,
    maxAttachments: Int = AttachmentLimits.PER_MESSAGE,
) {
    val colors = Nf.colors
    val remaining = (maxAttachments - attachments.size).coerceAtLeast(0)
    val importing = attachments.any { it is DraftAttachment.Importing }
    val hasContent = text.text.isNotBlank() || attachments.any { it is DraftAttachment.Ready }
    val canSend = enabled && hasContent && !importing

    val currentRemaining by rememberUpdatedState(remaining)
    val currentEnabled by rememberUpdatedState(enabled)
    val currentOnAdd by rememberUpdatedState(onAddMedia)

    val pickMany = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(maxAttachments)) { uris ->
        if (uris.isNotEmpty()) currentOnAdd(uris.map { MediaSource(it.toString(), null) })
    }
    val pickOne = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) currentOnAdd(listOf(MediaSource(uri.toString(), null)))
    }
    val request = remember { PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo) }

    // GIFs, stickers and images from the keyboard or clipboard.
    val receiver = remember {
        object : ReceiveContentListener {
            override fun onReceive(transferableContent: TransferableContent): TransferableContent? {
                if (!currentEnabled || currentRemaining == 0) return transferableContent
                val isMedia = transferableContent.hasMediaType(MediaType.Image) || transferableContent.hasVideo()
                if (!isMedia) return transferableContent
                val mime = transferableContent.mediaMimeType()
                val sources = mutableListOf<MediaSource>()
                val rest = transferableContent.consume { item ->
                    val uri = item.uri
                    if (uri != null && sources.size < currentRemaining) {
                        sources += MediaSource(uri.toString(), mime)
                        true
                    } else {
                        false
                    }
                }
                if (sources.isNotEmpty()) currentOnAdd(sources)
                return rest
            }
        }
    }

    val density = LocalDensity.current
    val windowHeight = LocalWindowInfo.current.containerSize.height
    val keyboard = WindowInsets.imeAnimationTarget.getBottom(density)
    val lineHeight = with(density) { Nf.type.body.lineHeight.toPx() }
    val maxLines = composerMaxLines(windowHeight - keyboard, lineHeight)

    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val border by animateColorAsState(if (focused) colors.lineStrong else colors.line, label = "composerBorder")

    Column(
        modifier
            .testTag(ComposerTags.Container)
            .fillMaxWidth()
            .clip(NfShapes.large)
            .background(colors.surfaceRaised)
            .border(1.dp, border, NfShapes.large),
    ) {
        AnimatedVisibility(attachments.isNotEmpty(), enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            LazyRow(
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(attachments, key = { it.id }) { item -> DraftTile(item, onRemove = { onRemoveAttachment(item.id) }, modifier = Modifier.animateItem()) }
            }
        }
        BasicTextField(
            state = text,
            enabled = enabled,
            lineLimits = TextFieldLineLimits.MultiLine(minHeightInLines = 1, maxHeightInLines = maxLines),
            inputTransformation = InputTransformation.maxLength(maxLength),
            textStyle = Nf.type.body.copy(color = colors.textPrimary),
            cursorBrush = SolidColor(colors.accent),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            interactionSource = interaction,
            modifier = Modifier
                .testTag(ComposerTags.Field)
                .fillMaxWidth()
                .contentReceiver(receiver)
                .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 4.dp),
            decorator = TextFieldDecorator { inner ->
                Box {
                    if (text.text.isEmpty()) {
                        Text(placeholder, style = Nf.type.body, color = colors.textTertiary, maxLines = 1)
                    }
                    inner()
                }
            },
        )
        Row(
            Modifier
                .testTag(ComposerTags.Actions)
                .fillMaxWidth()
                .padding(start = 6.dp, end = 8.dp, bottom = 8.dp, top = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NfIconButton(
                Icons.Rounded.AddPhotoAlternate,
                stringResource(R.string.ui_composer_attach),
                onClick = { if (remaining > 1) pickMany.launch(request) else pickOne.launch(request) },
                enabled = enabled && remaining > 0,
                tint = colors.textSecondary,
            )
            if (attachments.isNotEmpty()) {
                Spacer(Modifier.width(2.dp))
                Text(
                    stringResource(R.string.ui_composer_count, attachments.size, maxAttachments),
                    style = Nf.type.labelSmall,
                    color = colors.textTertiary,
                )
            }
            Spacer(Modifier.weight(1f))
            val length = text.text.length
            if (length >= maxLength * 0.9) {
                Text("$length/$maxLength", style = Nf.type.labelSmall, color = if (length >= maxLength) colors.danger else colors.textTertiary)
                Spacer(Modifier.width(10.dp))
            }
            NfIconButton(
                Icons.AutoMirrored.Rounded.Send,
                stringResource(R.string.ui_composer_send),
                onClick = onSend,
                style = NfButtonStyle.Primary,
                size = 40.dp,
                iconSize = 20.dp,
                enabled = canSend,
            )
        }
    }
}

private fun TransferableContent.hasVideo(): Boolean {
    val description = clipMetadata.clipDescription
    return (0 until description.mimeTypeCount).any { description.getMimeType(it).startsWith("video/") }
}

private fun TransferableContent.mediaMimeType(): String? {
    val description: ClipDescription = clipMetadata.clipDescription
    return (0 until description.mimeTypeCount)
        .map { description.getMimeType(it) }
        .firstOrNull { it.startsWith("image/") || it.startsWith("video/") }
}

@Composable
private fun DraftTile(item: DraftAttachment, onRemove: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.size(68.dp)) {
        Box(
            Modifier
                .padding(top = 4.dp, end = 4.dp)
                .size(64.dp)
                .clip(NfShapes.medium)
                .background(Nf.colors.surfaceTop),
            contentAlignment = Alignment.Center,
        ) {
            when (item) {
                is DraftAttachment.Ready -> AttachmentThumbnail(item.attachment, Modifier.fillMaxSize())
                is DraftAttachment.Importing -> {
                    val label = stringResource(R.string.ui_composer_adding)
                    CircularProgressIndicator(
                        color = Nf.colors.textSecondary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp).semantics { contentDescription = label },
                    )
                }
            }
        }
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .size(22.dp)
                .clip(NfShapes.small)
                .background(Nf.colors.ink),
            contentAlignment = Alignment.Center,
        ) {
            NfIconButton(
                Icons.Rounded.Close,
                stringResource(R.string.ui_composer_remove),
                onClick = onRemove,
                size = 22.dp,
                iconSize = 14.dp,
                tint = Nf.colors.onInk,
            )
        }
    }
}

/** Shows a snackbar for media that could not be added. */
@Composable
fun AttachmentProblemMessages(problems: Flow<AttachmentProblem>, limit: Int = AttachmentLimits.PER_MESSAGE) {
    val snackbar = LocalSnackbarHost.current
    val resources = LocalResources.current
    LaunchedEffect(problems) {
        problems.collect { problem ->
            val message = if (problem == AttachmentProblem.LimitReached) {
                resources.getQuantityString(R.plurals.ui_attach_limit, limit, limit)
            } else {
                resources.getString(problem.messageRes())
            }
            snackbar.showSnackbar(message)
        }
    }
}

