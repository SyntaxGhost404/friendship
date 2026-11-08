package app.nearfolk.feature.messages

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.nearfolk.core.designsystem.component.NfButton
import app.nearfolk.core.designsystem.component.NfButtonSize
import app.nearfolk.core.designsystem.component.NfButtonStyle
import app.nearfolk.core.designsystem.component.NfConfirmDialog
import app.nearfolk.core.designsystem.component.NfIconButton
import app.nearfolk.core.designsystem.component.NfNotice
import app.nearfolk.core.designsystem.component.NfNoticeTone
import app.nearfolk.core.designsystem.component.NfSpinner
import app.nearfolk.core.designsystem.component.NfTextField
import app.nearfolk.core.designsystem.component.NfTopBar
import app.nearfolk.core.designsystem.shape.SquircleShape
import app.nearfolk.core.designsystem.theme.HapticKind
import app.nearfolk.core.designsystem.theme.LocalAppHaptics
import app.nearfolk.core.designsystem.theme.Nf
import app.nearfolk.core.designsystem.theme.NfShapes
import app.nearfolk.core.model.ConversationState
import app.nearfolk.core.model.DeliveryStatus
import app.nearfolk.core.model.Message
import app.nearfolk.core.model.Presence
import app.nearfolk.core.model.ProfileRules
import app.nearfolk.core.ui.BlockDialog
import app.nearfolk.core.ui.LocalSnackbarHost
import app.nearfolk.core.ui.MemberAvatar
import app.nearfolk.core.ui.MenuAction
import app.nearfolk.core.ui.NfMenu
import app.nearfolk.core.ui.ReportSheet
import app.nearfolk.core.ui.TimeFormat
import app.nearfolk.core.ui.rememberTimeFormat
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate

@Serializable
data class ChatRoute(val conversationId: String)

private sealed interface ChatRow {
    val key: String

    data class Day(val date: LocalDate) : ChatRow {
        override val key = "day-$date"
    }

    data class Bubble(val message: Message, val joinsAbove: Boolean, val joinsBelow: Boolean, val showStatus: Boolean) : ChatRow {
        override val key = message.id
    }
}

/** Groups consecutive messages from the same sender within five minutes and inserts day headers. */
private fun buildRows(messages: List<Message>, time: TimeFormat): List<ChatRow> {
    val rows = mutableListOf<ChatRow>()
    val lastMineIndex = messages.indexOfLast { it.isMine }
    messages.forEachIndexed { i, m ->
        val day = time.localDate(m.sentAt)
        val prev = messages.getOrNull(i - 1)
        val next = messages.getOrNull(i + 1)
        if (prev == null || time.localDate(prev.sentAt) != day) rows += ChatRow.Day(day)
        fun close(a: Message?, b: Message) = a != null && a.senderId == b.senderId &&
            kotlin.math.abs(a.sentAt.epochSecond - b.sentAt.epochSecond) < 300 && time.localDate(a.sentAt) == time.localDate(b.sentAt)
        rows += ChatRow.Bubble(m, joinsAbove = close(prev, m), joinsBelow = close(next, m), showStatus = i == lastMineIndex)
    }
    return rows
}

@Composable
fun ChatRouteScreen(
    onBack: () -> Unit,
    onOpenProfile: (String) -> Unit,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbarHost.current
    val context = LocalContext.current
    val resources = LocalResources.current
    LifecycleResumeEffect(viewModel) {
        viewModel.onVisible(true)
        onPauseOrDispose { viewModel.onVisible(false) }
    }
    LaunchedEffect(state.messages.size) { viewModel.markRead() }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                ChatEvent.Deleted -> onBack()
                ChatEvent.Reported -> snackbar.showSnackbar(resources.getString(app.nearfolk.core.ui.R.string.ui_report_sent))
                is ChatEvent.Blocked -> {
                    onBack()
                    snackbar.showSnackbar(resources.getString(app.nearfolk.core.ui.R.string.ui_blocked_done, event.name))
                }
                is ChatEvent.MuteChanged -> snackbar.showSnackbar(
                    resources.getString(if (event.muted) R.string.chat_muted_on else R.string.chat_muted_off),
                )
            }
        }
    }
    ChatScreen(
        state = state,
        draft = draft,
        onDraft = viewModel::setDraft,
        onSend = viewModel::send,
        onBack = onBack,
        onOpenProfile = { state.conversation?.peer?.id?.let(onOpenProfile) },
        onDismissSafety = viewModel::dismissSafety,
        onAccept = viewModel::accept,
        onToggleMute = viewModel::toggleMute,
        onReport = viewModel::report,
        onBlock = viewModel::block,
        onUnblock = viewModel::unblock,
        onDelete = viewModel::delete,
    )
}

@Composable
fun ChatScreen(
    state: ChatUiState,
    draft: String,
    onDraft: (String) -> Unit,
    onSend: () -> Unit,
    onBack: () -> Unit,
    onOpenProfile: () -> Unit,
    onDismissSafety: () -> Unit,
    onAccept: () -> Unit,
    onToggleMute: () -> Unit,
    onReport: (app.nearfolk.core.model.ReportReason, String?) -> Unit,
    onBlock: () -> Unit,
    onUnblock: () -> Unit,
    onDelete: () -> Unit,
    now: Instant = Instant.now(),
) {
    val time = rememberTimeFormat()
    val convo = state.conversation
    val peer = convo?.peer
    var menu by remember { mutableStateOf(false) }
    var reporting by rememberSaveable { mutableStateOf(false) }
    var blocking by rememberSaveable { mutableStateOf(false) }
    var deleting by rememberSaveable { mutableStateOf(false) }
    val haptics = LocalAppHaptics.current

    Column(Modifier.fillMaxSize().background(Nf.colors.canvas).imePadding()) {
        NfTopBar(
            title = peer?.displayName.orEmpty(),
            onBack = onBack,
            backDescription = stringResource(app.nearfolk.core.ui.R.string.ui_back),
            titleContent = {
                if (peer != null) {
                    Row(
                        Modifier.clip(NfShapes.medium).clickable(role = Role.Button, onClick = onOpenProfile).padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        MemberAvatar(peer.displayName, peer.avatar, 38.dp, online = peer.presence is Presence.Online)
                        Column {
                            Text(peer.displayName, style = Nf.type.titleSmall, color = Nf.colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            val typing = convo.peerIsTyping
                            Text(
                                if (typing) stringResource(app.nearfolk.core.ui.R.string.ui_typing) else time.presence(peer.presence, now),
                                style = Nf.type.labelSmall,
                                color = when {
                                    typing -> Nf.colors.accent
                                    peer.presence is Presence.Online -> Nf.colors.online
                                    else -> Nf.colors.textTertiary
                                },
                                maxLines = 1,
                            )
                        }
                    }
                }
            },
            actions = {
                if (convo != null) {
                    Box {
                        NfIconButton(Icons.Rounded.MoreHoriz, stringResource(app.nearfolk.core.ui.R.string.ui_more), { menu = true })
                        NfMenu(
                            expanded = menu,
                            onDismiss = { menu = false },
                            actions = buildList {
                                add(MenuAction(stringResource(R.string.chat_view_profile), Icons.Rounded.Person, onClick = onOpenProfile))
                                add(
                                    if (convo.muted) {
                                        MenuAction(stringResource(R.string.chat_unmute), Icons.Rounded.Notifications, onClick = onToggleMute)
                                    } else {
                                        MenuAction(stringResource(R.string.chat_mute), Icons.Rounded.NotificationsOff, onClick = onToggleMute)
                                    },
                                )
                                add(MenuAction(stringResource(R.string.chat_report), Icons.Rounded.Flag) { reporting = true })
                                add(
                                    if (state.blocked) {
                                        MenuAction(stringResource(R.string.chat_unblock), Icons.Rounded.Block, onClick = onUnblock)
                                    } else {
                                        MenuAction(stringResource(R.string.chat_block), Icons.Rounded.Block, destructive = true) { blocking = true }
                                    },
                                )
                                add(MenuAction(stringResource(R.string.chat_delete), Icons.Rounded.Delete, destructive = true) { deleting = true })
                            },
                        )
                    }
                }
            },
        )

        if (state.loading || convo == null || peer == null) {
            NfSpinner()
            return@Column
        }

        AnimatedVisibility(state.showSafetyBanner, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            NfNotice(
                title = stringResource(R.string.chat_safety_title),
                text = stringResource(R.string.chat_safety_body),
                icon = Icons.Rounded.Shield,
                tone = NfNoticeTone.Accent,
                dismissDescription = stringResource(app.nearfolk.core.ui.R.string.ui_dismiss),
                onDismiss = onDismissSafety,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }

        val rows = remember(state.messages, time) { buildRows(state.messages, time).asReversed() }
        val listState = rememberLazyListState()
        LaunchedEffect(state.messages.lastOrNull()?.id, convo.peerIsTyping) {
            if (listState.firstVisibleItemIndex <= 2) listState.animateScrollToItem(0)
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (state.messages.isEmpty()) {
                Text(
                    stringResource(R.string.chat_first_message, peer.displayName),
                    style = Nf.type.body,
                    color = Nf.colors.textTertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                )
            }
            LazyColumn(
                state = listState,
                reverseLayout = true,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
            ) {
                if (convo.peerIsTyping) {
                    item(key = "typing") { TypingBubble(Modifier.animateItem()) }
                }
                items(rows, key = { it.key }) { row ->
                    when (row) {
                        is ChatRow.Day -> DayHeader(time.dayHeader(row.date), Modifier.animateItem())
                        is ChatRow.Bubble -> MessageBubble(row, time, Modifier.animateItem())
                    }
                }
            }
        }

        val allowance = state.allowance
        when {
            convo.state == ConversationState.Request && !state.blocked -> RequestDecision(
                name = peer.displayName,
                onAccept = onAccept,
                onBlock = { blocking = true },
                onDelete = { deleting = true },
            )
            state.blocked -> Text(
                stringResource(R.string.chat_blocked, peer.displayName),
                style = Nf.type.bodySmall,
                color = Nf.colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).padding(20.dp),
            )
            else -> Column(Modifier.windowInsetsPadding(WindowInsets.navigationBars)) {
                if (allowance != null && state.messages.any { it.isMine }) {
                    Text(
                        when (allowance) {
                            0 -> stringResource(R.string.chat_wait, peer.displayName)
                            else -> pluralStringResource(R.plurals.chat_allowance, allowance, allowance, peer.displayName)
                        },
                        style = Nf.type.labelSmall,
                        color = Nf.colors.textTertiary,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    )
                }
                Row(
                    Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val canSend = allowance != 0
                    NfTextField(
                        value = draft,
                        onValueChange = onDraft,
                        placeholder = stringResource(R.string.chat_hint),
                        singleLine = false,
                        maxLength = ProfileRules.MESSAGE_MAX,
                        enabled = canSend,
                        minHeight = 48.dp,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.weight(1f),
                    )
                    NfIconButton(
                        Icons.AutoMirrored.Rounded.Send,
                        stringResource(R.string.chat_send),
                        onClick = {
                            haptics.perform(HapticKind.Confirm)
                            onSend()
                        },
                        style = NfButtonStyle.Primary,
                        size = 48.dp,
                        enabled = canSend && draft.isNotBlank(),
                    )
                }
            }
        }
    }

    if (peer != null && reporting) {
        ReportSheet(peer.displayName, onDismiss = { reporting = false }, onSubmit = { r, d ->
            reporting = false
            onReport(r, d)
        })
    }
    if (peer != null && blocking) {
        BlockDialog(peer.displayName, onConfirm = {
            blocking = false
            onBlock()
        }, onDismiss = { blocking = false })
    }
    if (deleting) {
        NfConfirmDialog(
            title = stringResource(R.string.chat_delete_title),
            message = stringResource(R.string.chat_delete_body),
            confirmLabel = stringResource(R.string.chat_delete_confirm),
            dismissLabel = stringResource(app.nearfolk.core.ui.R.string.ui_cancel),
            onConfirm = {
                deleting = false
                onDelete()
            },
            onDismiss = { deleting = false },
            destructive = true,
        )
    }
}

/**
 * Shown in place of the message bar until a request is accepted, so nobody
 * can reply by accident. Three equal width choices span the card.
 */
@Composable
private fun RequestDecision(name: String, onAccept: () -> Unit, onBlock: () -> Unit, onDelete: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(12.dp)
            .clip(NfShapes.large)
            .background(Nf.colors.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(stringResource(R.string.chat_request_title, name), style = Nf.type.titleSmall, color = Nf.colors.textPrimary)
        Text(stringResource(R.string.chat_request_body), style = Nf.type.bodySmall, color = Nf.colors.textSecondary)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NfButton(stringResource(R.string.chat_request_accept), onAccept, size = NfButtonSize.Medium, modifier = Modifier.weight(1f))
            NfButton(stringResource(R.string.chat_block), onBlock, size = NfButtonSize.Medium, style = NfButtonStyle.Secondary, modifier = Modifier.weight(1f))
            NfButton(stringResource(app.nearfolk.core.ui.R.string.ui_delete), onDelete, size = NfButtonSize.Medium, style = NfButtonStyle.Secondary, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun DayHeader(label: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
        Text(
            label,
            style = Nf.type.labelSmall,
            color = Nf.colors.textTertiary,
            modifier = Modifier
                .clip(NfShapes.small)
                .background(Nf.colors.surface)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun MessageBubble(row: ChatRow.Bubble, time: TimeFormat, modifier: Modifier = Modifier) {
    val m = row.message
    val mine = m.isMine
    val colors = Nf.colors
    val big = 20.dp
    val small = 6.dp
    val shape = if (mine) {
        SquircleShape(topStart = big, topEnd = if (row.joinsAbove) small else big, bottomEnd = if (row.joinsBelow) small else big, bottomStart = big)
    } else {
        SquircleShape(topStart = if (row.joinsAbove) small else big, topEnd = big, bottomEnd = big, bottomStart = if (row.joinsBelow) small else big)
    }
    val container = if (mine) colors.bubbleMine else colors.bubbleTheirs
    val content = if (mine) colors.onBubbleMine else colors.onBubbleTheirs
    val context = LocalContext.current
    val resources = LocalResources.current
    val haptics = LocalAppHaptics.current
    val copied = stringResource(R.string.chat_copied)
    val snackbar = LocalSnackbarHost.current
    var copyRequest by remember { mutableIntStateOf(0) }
    LaunchedEffect(copyRequest) { if (copyRequest > 0) snackbar.showSnackbar(copied) }
    Row(
        modifier.fillMaxWidth().padding(top = if (row.joinsAbove) 2.dp else 8.dp),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            Modifier
                .widthIn(max = 300.dp)
                .clip(shape)
                .background(container)
                .combinedClickable(onClick = {}, onLongClick = {
                    haptics.perform(HapticKind.LongPress)
                    copyToClipboard(context, m.text)
                    copyRequest++
                })
                .padding(start = 14.dp, end = 12.dp, top = 9.dp, bottom = 7.dp),
            horizontalAlignment = Alignment.End,
        ) {
            Text(m.text, style = Nf.type.body, color = content, modifier = Modifier.align(Alignment.Start))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(time.clock(m.sentAt), style = Nf.type.caption, color = content.copy(alpha = 0.6f))
                if (mine) StatusMark(m.status, content)
            }
        }
    }
}

@Composable
private fun StatusMark(status: DeliveryStatus, tint: androidx.compose.ui.graphics.Color) {
    val (icon, label, alpha) = when (status) {
        DeliveryStatus.Sending -> Triple(Icons.Rounded.Schedule, R.string.chat_status_sending, 0.6f)
        DeliveryStatus.Sent -> Triple(Icons.Rounded.Done, R.string.chat_status_sent, 0.6f)
        DeliveryStatus.Delivered -> Triple(Icons.Rounded.DoneAll, R.string.chat_status_delivered, 0.6f)
        DeliveryStatus.Read -> Triple(Icons.Rounded.DoneAll, R.string.chat_status_read, 1f)
        DeliveryStatus.Failed -> Triple(Icons.Rounded.ErrorOutline, R.string.chat_status_failed, 1f)
    }
    val color = if (status == DeliveryStatus.Read) Nf.colors.online else tint.copy(alpha = alpha)
    Icon(icon, stringResource(label), tint = color, modifier = Modifier.size(14.dp))
}

@Composable
private fun TypingBubble(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "typing")
    Row(modifier.fillMaxWidth().padding(top = 8.dp)) {
        Row(
            Modifier
                .clip(SquircleShape(20.dp))
                .background(Nf.colors.bubbleTheirs)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            repeat(3) { i ->
                val lift by transition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(tween(520, delayMillis = i * 140), RepeatMode.Reverse),
                    label = "dot$i",
                )
                Box(
                    Modifier
                        .offset { IntOffset(0, (-3 * lift).dp.roundToPx()) }
                        .size(7.dp)
                        .graphicsLayer { alpha = 0.45f + 0.55f * lift }
                        .clip(NfShapes.badge)
                        .background(Nf.colors.onBubbleTheirs),
                )
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("message", text))
}

