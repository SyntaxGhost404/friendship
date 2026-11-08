package app.nearfolk.core.data.platform

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * What the person is looking at right now. Used to decide whether an incoming
 * message counts as read and whether it deserves a system notification.
 */
@Singleton
class AppVisibility @Inject constructor() {
    private val _foreground = MutableStateFlow(false)
    val foreground: StateFlow<Boolean> = _foreground.asStateFlow()

    private val _openConversation = MutableStateFlow<String?>(null)
    val openConversation: StateFlow<String?> = _openConversation.asStateFlow()

    fun setForeground(value: Boolean) {
        _foreground.value = value
    }

    fun setOpenConversation(id: String?) {
        _openConversation.value = id
    }

    fun isViewing(conversationId: String): Boolean =
        _foreground.value && _openConversation.value == conversationId
}

/** Implemented by the app module, which owns notification channels and intents. */
interface ChatNotifier {
    fun showMessage(conversationId: String, senderName: String, text: String, isRequest: Boolean)
    /** [commentText] is null for a like. */
    fun showActivity(actorName: String, commentText: String?)
    fun cancel(conversationId: String)
}
