package app.nearfolk.core.model

import java.time.Instant

enum class ConversationState {
    /** Both sides are talking, or you started it. */
    Active,

    /** Someone new wrote to you and you have not replied yet. */
    Request,

    /** Hidden from the inbox, for example after blocking. */
    Archived,
}

data class Conversation(
    val id: String,
    val peer: Member,
    val state: ConversationState,
    val lastMessage: Message?,
    val unreadCount: Int,
    val muted: Boolean,
    val createdAt: Instant,
    val peerIsTyping: Boolean = false,
)

enum class DeliveryStatus { Sending, Sent, Delivered, Read, Failed }

data class Message(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val text: String,
    val sentAt: Instant,
    val status: DeliveryStatus,
) {
    val isMine: Boolean get() = senderId == SELF_ID
}

enum class ReportReason { Harassment, Spam, SexualContent, Hate, Impersonation, Underage, SelfHarm, Other }

sealed interface SendResult {
    data object Sent : SendResult

    /** First contact is capped until the other person replies. */
    data class WaitForReply(val limit: Int) : SendResult
    data object Blocked : SendResult
    data object Empty : SendResult
}
