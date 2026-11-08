package app.nearfolk.feature.messages

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.nearfolk.core.designsystem.component.NfButton
import app.nearfolk.core.designsystem.component.NfButtonSize
import app.nearfolk.core.designsystem.component.NfCountBadge
import app.nearfolk.core.designsystem.component.NfEmptyState
import app.nearfolk.core.designsystem.component.NfLargeHeader
import app.nearfolk.core.designsystem.component.NfListRow
import app.nearfolk.core.designsystem.component.NfSegmented
import app.nearfolk.core.designsystem.component.NfSpinner
import app.nearfolk.core.designsystem.component.NfTopBar
import app.nearfolk.core.designsystem.component.pressScale
import app.nearfolk.core.designsystem.icon.NfIcons
import app.nearfolk.core.designsystem.theme.HapticKind
import app.nearfolk.core.designsystem.theme.LocalAppHaptics
import app.nearfolk.core.designsystem.theme.Nf
import app.nearfolk.core.designsystem.theme.NfShapes
import app.nearfolk.core.model.Conversation
import app.nearfolk.core.model.Presence
import app.nearfolk.core.ui.MemberAvatar
import app.nearfolk.core.ui.rememberTimeFormat
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data object MessagesRoute

@Serializable
data object ArchivedRoute

@Composable
fun InboxRouteScreen(
    onOpenChat: (String) -> Unit,
    onOpenArchived: () -> Unit,
    onOpenDiscover: () -> Unit,
    viewModel: InboxViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    InboxScreen(state, onOpenChat, onOpenArchived, onOpenDiscover)
}

@Composable
fun InboxScreen(
    state: InboxUiState,
    onOpenChat: (String) -> Unit,
    onOpenArchived: () -> Unit,
    onOpenDiscover: () -> Unit,
    now: Instant = Instant.now(),
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().background(Nf.colors.canvas)) {
        NfLargeHeader(stringResource(R.string.msg_title))
        NfSegmented(
            options = listOf(
                stringResource(R.string.msg_tab_chats),
                if (state.requests.isEmpty()) stringResource(R.string.msg_tab_requests) else stringResource(R.string.msg_tab_requests_count, state.requests.size),
            ),
            selectedIndex = tab,
            onSelect = { tab = it },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        if (state.loading) {
            NfSpinner()
            return@Column
        }
        AnimatedContent(tab, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "inboxTab", modifier = Modifier.weight(1f)) { selected ->
            val list = if (selected == 0) state.chats else state.requests
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 4.dp, end = 4.dp, bottom = 24.dp)) {
                if (selected == 1 && list.isNotEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.msg_requests_hint),
                            style = Nf.type.bodySmall,
                            color = Nf.colors.textTertiary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }
                items(list, key = { it.id }) { convo ->
                    ConversationRow(convo, now, onClick = { onOpenChat(convo.id) }, modifier = Modifier.animateItem())
                }
                if (list.isEmpty()) {
                    item {
                        if (selected == 0) {
                            NfEmptyState(
                                NfIcons.Messages,
                                stringResource(R.string.msg_empty_title),
                                stringResource(R.string.msg_empty_body),
                                action = { NfButton(stringResource(R.string.msg_empty_cta), onOpenDiscover, size = NfButtonSize.Medium) },
                            )
                        } else {
                            NfEmptyState(Icons.Rounded.Inbox, stringResource(R.string.msg_requests_empty_title), stringResource(R.string.msg_requests_empty_body))
                        }
                    }
                }
                if (selected == 0 && state.archivedCount > 0) {
                    item(key = "archived") {
                        NfListRow(
                            stringResource(R.string.msg_archived),
                            subtitle = pluralStringResource(R.plurals.msg_archived_count, state.archivedCount, state.archivedCount),
                            icon = Icons.Rounded.Archive,
                            onClick = onOpenArchived,
                            modifier = Modifier.padding(top = 8.dp).clip(NfShapes.large),
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun ConversationRow(convo: Conversation, now: Instant, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val time = rememberTimeFormat()
    val interaction = remember { MutableInteractionSource() }
    val haptics = LocalAppHaptics.current
    val unread = convo.unreadCount > 0
    Row(
        modifier
            .fillMaxWidth()
            .pressScale(interaction, pressed = 0.985f)
            .clip(NfShapes.large)
            .clickable(interactionSource = interaction, indication = ripple(), role = Role.Button) {
                haptics.perform(HapticKind.Tap)
                onClick()
            }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        MemberAvatar(convo.peer.displayName, convo.peer.avatar, 52.dp, online = convo.peer.presence is Presence.Online)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        convo.peer.displayName,
                        style = Nf.type.titleSmall.copy(fontWeight = if (unread) FontWeight.Bold else FontWeight.SemiBold),
                        color = Nf.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (convo.muted) {
                        Spacer(Modifier.size(6.dp))
                        Icon(Icons.Rounded.NotificationsOff, stringResource(R.string.msg_muted), tint = Nf.colors.textTertiary, modifier = Modifier.size(14.dp))
                    }
                }
                Spacer(Modifier.size(8.dp))
                convo.lastMessage?.let {
                    Text(
                        time.inbox(it.sentAt, now),
                        style = Nf.type.labelSmall,
                        color = if (unread) Nf.colors.accent else Nf.colors.textTertiary,
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                val preview = when {
                    convo.peerIsTyping -> stringResource(app.nearfolk.core.ui.R.string.ui_typing)
                    convo.lastMessage == null -> ""
                    convo.lastMessage!!.isMine -> stringResource(R.string.msg_you_prefix, convo.lastMessage!!.text)
                    else -> convo.lastMessage!!.text
                }
                Text(
                    preview,
                    style = if (unread) Nf.type.bodyStrong else Nf.type.body,
                    color = when {
                        convo.peerIsTyping -> Nf.colors.accent
                        unread -> Nf.colors.textPrimary
                        else -> Nf.colors.textSecondary
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (unread) {
                    Spacer(Modifier.size(8.dp))
                    NfCountBadge(convo.unreadCount)
                }
            }
        }
    }
}

@Composable
fun ArchivedRouteScreen(onBack: () -> Unit, onOpenChat: (String) -> Unit, viewModel: ArchivedViewModel = hiltViewModel()) {
    val list by viewModel.conversations.collectAsStateWithLifecycle()
    val now = Instant.now()
    Column(Modifier.fillMaxSize().background(Nf.colors.canvas)) {
        NfTopBar(stringResource(R.string.msg_archived_title), onBack = onBack, backDescription = stringResource(app.nearfolk.core.ui.R.string.ui_back))
        val items = list
        when {
            items == null -> NfSpinner()
            items.isEmpty() -> NfEmptyState(Icons.Rounded.Archive, stringResource(R.string.msg_archived_title), stringResource(R.string.msg_archived_empty))
            else -> LazyColumn(contentPadding = PaddingValues(horizontal = 4.dp)) {
                items(items, key = { it.id }) { convo -> ConversationRow(convo, now, onClick = { onOpenChat(convo.id) }) }
            }
        }
    }
}

