package app.nearfolk.core.data.local

import app.nearfolk.core.common.TimeSource
import app.nearfolk.core.data.community.CommunitySimulator
import app.nearfolk.core.data.platform.ChatNotifier
import app.nearfolk.core.data.repository.ChatRepository
import app.nearfolk.core.database.NearfolkDatabase
import app.nearfolk.core.database.entity.BlockEntity
import app.nearfolk.core.database.entity.ConversationEntity
import app.nearfolk.core.database.entity.MessageEntity
import app.nearfolk.core.database.entity.ReportEntity
import app.nearfolk.core.model.Conversation
import app.nearfolk.core.model.ConversationState
import app.nearfolk.core.model.DeliveryStatus
import app.nearfolk.core.model.Message
import app.nearfolk.core.model.ProfileRules
import app.nearfolk.core.model.ReportReason
import app.nearfolk.core.model.SELF_ID
import app.nearfolk.core.model.SendResult
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class LocalChatRepository @Inject constructor(
    private val db: NearfolkDatabase,
    private val simulator: CommunitySimulator,
    private val notifier: ChatNotifier,
    private val time: TimeSource,
) : ChatRepository {
    private val conversations = db.conversationDao()
    private val messages = db.messageDao()
    private val members = db.memberDao()
    private val safety = db.safetyDao()

    override fun conversations(state: ConversationState): Flow<List<Conversation>> =
        combine(
            conversations.observeByState(state.name),
            members.observeDiscoverable(SELF_ID).map { rows -> rows.associateBy { it.id } },
            simulator.typing,
        ) { rows, _, typing -> rows to typing }
            .map { (rows, typing) ->
                rows.mapNotNull { row ->
                    val peer = members.get(row.peerId)?.toModel() ?: return@mapNotNull null
                    // Threads that never got a message stay out of the inbox.
                    if (row.lastMessageId == null) return@mapNotNull null
                    row.toModel(peer, row.id in typing)
                }
            }

    override fun conversation(id: String): Flow<Conversation?> =
        combine(conversations.observe(id), simulator.typing) { row, typing -> row to typing }
            .flatMapLatest { (row, typing) ->
                if (row == null) {
                    flowOf(null)
                } else {
                    members.observe(row.peerId).map { peer -> peer?.let { row.toModel(it.toModel(), row.id in typing) } }
                }
            }

    override fun messages(conversationId: String): Flow<List<Message>> =
        messages.observe(conversationId).map { rows -> rows.map { it.toModel() } }

    override fun unreadTotal(): Flow<Int> = conversations.observeUnreadTotal()

    override fun requestCount(): Flow<Int> = conversations.observeRequestCount()

    override suspend fun conversationWith(memberId: String): String {
        conversations.forPeer(memberId)?.let { return it.id }
        val now = time.now().toEpochMilli()
        val id = UUID.randomUUID().toString()
        conversations.upsert(
            ConversationEntity(
                id = id,
                peerId = memberId,
                state = ConversationState.Active.name,
                createdAt = now,
                lastActivityAt = now,
                unreadCount = 0,
                muted = false,
                peerHasReplied = false,
                lastMessageId = null,
                lastMessageText = null,
                lastMessageSenderId = null,
                lastMessageAt = null,
                lastMessageStatus = null,
            ),
        )
        return id
    }

    override suspend fun send(conversationId: String, text: String): SendResult {
        val clean = text.trim().take(ProfileRules.MESSAGE_MAX)
        if (clean.isEmpty()) return SendResult.Empty
        val convo = conversations.get(conversationId) ?: return SendResult.Empty
        if (safety.isBlocked(convo.peerId)) return SendResult.Blocked
        if (convo.state == ConversationState.Request.name) return SendResult.RequestPending
        if (!convo.peerHasReplied && messages.countFrom(conversationId, SELF_ID) >= ProfileRules.FIRST_CONTACT_LIMIT) {
            return SendResult.WaitForReply(ProfileRules.FIRST_CONTACT_LIMIT)
        }
        val now = time.now().toEpochMilli()
        val message = MessageEntity(UUID.randomUUID().toString(), conversationId, SELF_ID, clean, now, DeliveryStatus.Sent.name)
        db.withTransaction {
            messages.insert(message)
            conversations.upsert(
                convo.copy(
                    lastActivityAt = now,
                    lastMessageId = message.id,
                    lastMessageText = clean,
                    lastMessageSenderId = SELF_ID,
                    lastMessageAt = now,
                    lastMessageStatus = message.status,
                ),
            )
        }
        simulator.onOutgoingMessage(conversationId)
        return SendResult.Sent
    }

    override suspend fun markRead(conversationId: String) {
        conversations.clearUnread(conversationId)
        notifier.cancel(conversationId)
    }

    override suspend fun acceptRequest(conversationId: String) =
        conversations.setState(conversationId, ConversationState.Active.name)

    override suspend fun setMuted(conversationId: String, muted: Boolean) = conversations.setMuted(conversationId, muted)

    override suspend fun delete(conversationId: String) {
        db.withTransaction {
            messages.deleteFor(conversationId)
            conversations.delete(conversationId)
        }
        notifier.cancel(conversationId)
    }

    override suspend fun block(memberId: String) {
        db.withTransaction {
            safety.block(BlockEntity(memberId, time.now().toEpochMilli()))
            conversations.forPeer(memberId)?.let { conversations.setState(it.id, ConversationState.Archived.name) }
        }
    }

    override suspend fun unblock(memberId: String) = safety.unblock(memberId)

    override suspend fun report(memberId: String, reason: ReportReason, details: String?, conversationId: String?) {
        safety.report(
            ReportEntity(
                id = UUID.randomUUID().toString(),
                memberId = memberId,
                conversationId = conversationId,
                reason = reason.name,
                details = details?.trim()?.ifBlank { null },
                createdAt = time.now().toEpochMilli(),
            ),
        )
    }

    override fun firstContactAllowance(conversationId: String): Flow<Int?> =
        combine(conversations.observe(conversationId), messages.observe(conversationId)) { convo, list ->
            if (convo == null || convo.peerHasReplied) {
                null
            } else {
                (ProfileRules.FIRST_CONTACT_LIMIT - list.count { it.senderId == SELF_ID }).coerceAtLeast(0)
            }
        }
}
