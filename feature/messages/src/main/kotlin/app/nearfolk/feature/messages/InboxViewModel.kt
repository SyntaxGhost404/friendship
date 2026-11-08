package app.nearfolk.feature.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.nearfolk.core.data.repository.ChatRepository
import app.nearfolk.core.model.Conversation
import app.nearfolk.core.model.ConversationState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class InboxUiState(
    val loading: Boolean = true,
    val chats: List<Conversation> = emptyList(),
    val requests: List<Conversation> = emptyList(),
    val archivedCount: Int = 0,
)

@HiltViewModel
class InboxViewModel @Inject constructor(chat: ChatRepository) : ViewModel() {
    val state: StateFlow<InboxUiState> = combine(
        chat.conversations(ConversationState.Active),
        chat.conversations(ConversationState.Request),
        chat.conversations(ConversationState.Archived),
    ) { active, requests, archived ->
        InboxUiState(false, active, requests, archived.size)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InboxUiState())
}

@HiltViewModel
class ArchivedViewModel @Inject constructor(chat: ChatRepository) : ViewModel() {
    val conversations: StateFlow<List<Conversation>?> = chat.conversations(ConversationState.Archived)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
