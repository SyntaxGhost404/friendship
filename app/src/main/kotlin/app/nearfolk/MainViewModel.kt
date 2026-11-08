package app.nearfolk

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.nearfolk.core.data.repository.ChatRepository
import app.nearfolk.core.data.repository.PostRepository
import app.nearfolk.core.data.repository.PreferencesRepository
import app.nearfolk.core.data.repository.SessionRepository
import app.nearfolk.core.model.AccountState
import app.nearfolk.core.model.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class Badges(val messages: Int = 0, val you: Int = 0)

@HiltViewModel
class MainViewModel @Inject constructor(
    session: SessionRepository,
    preferences: PreferencesRepository,
    chat: ChatRepository,
    posts: PostRepository,
) : ViewModel() {
    /** Null until DataStore answers, which keeps the splash screen up. */
    val accountState: StateFlow<AccountState?> =
        session.accountState.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val preferences: StateFlow<UserPreferences?> =
        preferences.preferences.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val badges: StateFlow<Badges> = combine(chat.unreadTotal(), chat.requestCount(), posts.unseenActivityCount()) { unread, requests, activity ->
        Badges(unread + requests, activity)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Badges())

    private val _pendingChat = MutableStateFlow<String?>(null)
    val pendingChat: StateFlow<String?> = _pendingChat.asStateFlow()

    fun openChatFromNotification(id: String?) {
        if (id != null) _pendingChat.value = id
    }

    fun consumePendingChat() {
        _pendingChat.value = null
    }
}
