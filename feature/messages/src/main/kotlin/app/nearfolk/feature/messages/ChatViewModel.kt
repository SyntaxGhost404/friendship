package app.nearfolk.feature.messages

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import app.nearfolk.core.data.media.AttachmentDraftFactory
import app.nearfolk.core.data.platform.AppVisibility
import app.nearfolk.core.data.repository.ChatRepository
import app.nearfolk.core.data.repository.MemberRepository
import app.nearfolk.core.data.repository.PreferencesRepository
import app.nearfolk.core.model.Conversation
import app.nearfolk.core.model.DraftAttachment
import app.nearfolk.core.model.MediaSource
import app.nearfolk.core.model.Message
import app.nearfolk.core.model.ReportReason
import app.nearfolk.core.model.SendResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val loading: Boolean = true,
    val conversation: Conversation? = null,
    val messages: List<Message> = emptyList(),
    val showSafetyBanner: Boolean = false,
    val allowance: Int? = null,
    val blocked: Boolean = false,
)

sealed interface ChatEvent {
    data object Deleted : ChatEvent
    data object Reported : ChatEvent
    data class Blocked(val name: String) : ChatEvent
    data class MuteChanged(val muted: Boolean) : ChatEvent
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val chat: ChatRepository,
    members: MemberRepository,
    private val preferences: PreferencesRepository,
    private val visibility: AppVisibility,
    drafts: AttachmentDraftFactory,
) : ViewModel() {
    val conversationId: String = savedStateHandle.toRoute<ChatRoute>().conversationId

    private val conversation = chat.conversation(conversationId)

    val state: StateFlow<ChatUiState> = combine(
        conversation,
        chat.messages(conversationId),
        preferences.preferences,
        chat.firstContactAllowance(conversationId),
        conversation.flatMapLatest { c -> if (c == null) flowOf(false) else members.isBlocked(c.peer.id) },
    ) { convo, messages, prefs, allowance, blocked ->
        ChatUiState(
            loading = false,
            conversation = convo,
            messages = messages,
            showSafetyBanner = conversationId !in prefs.dismissedSafetyBanners,
            allowance = allowance,
            blocked = blocked,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChatUiState())

    /** What is being written. Lives here so it survives configuration changes. */
    val text = TextFieldState()
    private val media = drafts.create(viewModelScope)
    val attachments: StateFlow<List<DraftAttachment>> = media.items
    val attachmentProblems = media.problems
    private val _sending = MutableStateFlow(false)

    private val _events = Channel<ChatEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onVisible(visible: Boolean) {
        visibility.setOpenConversation(if (visible) conversationId else null)
        if (visible) markRead()
    }

    fun markRead() {
        viewModelScope.launch { chat.markRead(conversationId) }
    }

    fun addMedia(sources: List<MediaSource>) = media.add(sources)

    fun removeAttachment(id: String) = media.remove(id)

    fun send() {
        val body = text.text.toString()
        val files = media.ready()
        if ((body.isBlank() && files.isEmpty()) || _sending.value) return
        if (attachments.value.any { it is DraftAttachment.Importing }) return
        _sending.value = true
        viewModelScope.launch {
            if (chat.send(conversationId, body, files) == SendResult.Sent) {
                text.clearText()
                media.handOver()
            }
            _sending.value = false
        }
    }

    fun dismissSafety() {
        viewModelScope.launch { preferences.dismissSafetyBanner(conversationId) }
    }

    fun accept() {
        viewModelScope.launch { chat.acceptRequest(conversationId) }
    }

    fun toggleMute() {
        val muted = state.value.conversation?.muted ?: return
        viewModelScope.launch {
            chat.setMuted(conversationId, !muted)
            _events.send(ChatEvent.MuteChanged(!muted))
        }
    }

    fun report(reason: ReportReason, details: String?) {
        val peer = state.value.conversation?.peer ?: return
        viewModelScope.launch {
            chat.report(peer.id, reason, details, conversationId)
            _events.send(ChatEvent.Reported)
        }
    }

    fun block() {
        val peer = state.value.conversation?.peer ?: return
        viewModelScope.launch {
            chat.block(peer.id)
            _events.send(ChatEvent.Blocked(peer.displayName))
        }
    }

    fun unblock() {
        val peer = state.value.conversation?.peer ?: return
        viewModelScope.launch { chat.unblock(peer.id) }
    }

    fun delete() {
        viewModelScope.launch {
            chat.delete(conversationId)
            _events.send(ChatEvent.Deleted)
        }
    }

    override fun onCleared() {
        if (visibility.openConversation.value == conversationId) visibility.setOpenConversation(null)
        media.discard()
    }
}
