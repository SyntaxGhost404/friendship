package app.nearfolk.core.data.community

import app.nearfolk.core.common.TimeSource
import app.nearfolk.core.data.di.ApplicationScope
import app.nearfolk.core.data.platform.AppVisibility
import app.nearfolk.core.data.platform.ChatNotifier
import app.nearfolk.core.database.dao.ActivityDao
import app.nearfolk.core.database.dao.CommentDao
import app.nearfolk.core.database.dao.ConversationDao
import app.nearfolk.core.database.dao.MemberDao
import app.nearfolk.core.database.dao.MessageDao
import app.nearfolk.core.database.dao.PostDao
import app.nearfolk.core.database.dao.SafetyDao
import app.nearfolk.core.database.entity.ActivityEntity
import app.nearfolk.core.database.entity.CommentEntity
import app.nearfolk.core.database.entity.ConversationEntity
import app.nearfolk.core.database.entity.MessageEntity
import app.nearfolk.core.database.entity.PostEntity
import app.nearfolk.core.datastore.NearfolkPreferences
import app.nearfolk.core.model.AccountState
import app.nearfolk.core.model.ActivityKind
import app.nearfolk.core.model.ConversationState
import app.nearfolk.core.model.DeliveryStatus
import app.nearfolk.core.model.MediaRef
import app.nearfolk.core.model.SELF_ID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Duration
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Stands in for the network while the app runs without a backend. It moves
 * members online and offline, answers your messages, sends a few first
 * messages to new profiles, posts now and then, reads, saves and shares
 * recent posts and reacts to your posts. Everything it does goes
 * through the same tables a real sync layer would write to.
 */
@Singleton
class CommunitySimulator @Inject internal constructor(
    @ApplicationScope private val scope: CoroutineScope,
    private val seeder: CommunitySeeder,
    private val members: MemberDao,
    private val conversations: ConversationDao,
    private val messages: MessageDao,
    private val posts: PostDao,
    private val comments: CommentDao,
    private val activity: ActivityDao,
    private val safety: SafetyDao,
    private val prefs: NearfolkPreferences,
    private val visibility: AppVisibility,
    private val notifier: ChatNotifier,
    private val time: TimeSource,
) {
    private val random = Random(System.nanoTime())
    private val _typing = MutableStateFlow<Set<String>>(emptySet())

    /** Conversation ids where the other person is typing right now. */
    val typing: StateFlow<Set<String>> = _typing.asStateFlow()

    private val replyLocks = mutableMapOf<String, Mutex>()
    private var started = false

    fun start() {
        if (started) return
        started = true
        scope.launch {
            seeder.seedIfNeeded()
            launch { presenceLoop() }
            launch { introLoop() }
            launch { postLoop() }
            launch { engagementLoop() }
        }
    }

    suspend fun refreshPresence() {
        val others = members.others(SELF_ID)
        val now = time.now().toEpochMilli()
        others.shuffled(random).take(4).forEach { m ->
            val online = random.nextFloat() < 0.45f
            members.setPresence(m.id, online, if (online || m.online) now else m.lastSeenAt)
        }
    }

    private suspend fun presenceLoop() {
        visibility.foreground.collectLatest { foreground ->
            while (foreground && scope.isActive) {
                delay(random.nextLong(15_000, 35_000))
                val others = members.others(SELF_ID)
                if (others.isEmpty()) continue
                val now = time.now().toEpochMilli()
                repeat(1 + random.nextInt(2)) {
                    val m = others[random.nextInt(others.size)]
                    val goOnline = !m.online && random.nextFloat() < 0.6f
                    members.setPresence(m.id, goOnline, now)
                }
            }
        }
    }

    /**
     * A few people say hello after you create a profile, spaced out so the
     * inbox fills gradually. Capped so it never turns into noise.
     */
    private suspend fun introLoop() {
        combine(prefs.accountState, visibility.foreground) { state, fg -> state == AccountState.Active && fg }
            .distinctUntilChanged()
            .collectLatest { active ->
                while (active && scope.isActive) {
                    val sent = prefs.introRequestsSent()
                    if (sent >= MAX_INTROS) return@collectLatest
                    delay(if (sent == 0) 20_000L else random.nextLong(90_000, 240_000))
                    sendIntro()
                    prefs.setIntroRequestsSent(sent + 1)
                }
            }
    }

    private suspend fun sendIntro() {
        val candidates = members.others(SELF_ID).filter { conversations.forPeer(it.id) == null && !safety.isBlocked(it.id) }
        val peer = candidates.filter { it.online }.ifEmpty { candidates }.randomOrNull(random) ?: return
        val template = SeedContent.introOpeners[random.nextInt(SeedContent.introOpeners.size)]
        val text = if (template.contains("%s")) template.format(peer.city ?: "here") else template
        val now = time.now().toEpochMilli()
        val conversationId = UUID.randomUUID().toString()
        val message = MessageEntity(UUID.randomUUID().toString(), conversationId, peer.id, text, now, DeliveryStatus.Delivered.name)
        conversations.upsert(
            ConversationEntity(
                id = conversationId,
                peerId = peer.id,
                state = ConversationState.Request.name,
                createdAt = now,
                lastActivityAt = now,
                unreadCount = 1,
                muted = false,
                peerHasReplied = true,
                lastMessageId = message.id,
                lastMessageText = text,
                lastMessageSenderId = peer.id,
                lastMessageAt = now,
                lastMessageStatus = message.status,
            ),
        )
        messages.insert(message)
        members.setPresence(peer.id, true, now)
        maybeNotify(conversationId, peer.displayName, text, isRequest = true, muted = false)
    }

    /**
     * While the app is open, someone posts every few minutes, so the feed
     * moves the way it would with people on the other end.
     */
    private suspend fun postLoop() {
        combine(prefs.accountState, visibility.foreground) { state, fg -> state == AccountState.Active && fg }
            .distinctUntilChanged()
            .collectLatest { active ->
                while (active && scope.isActive) {
                    delay(random.nextLong(120_000, 360_000))
                    publishPost()
                }
            }
    }

    /**
     * People read the feed while you do: recent posts, yours included, gather
     * views, and now and then a save or a share, so the counters stay live.
     */
    private suspend fun engagementLoop() {
        combine(prefs.accountState, visibility.foreground) { state, fg -> state == AccountState.Active && fg }
            .distinctUntilChanged()
            .collectLatest { active ->
                while (active && scope.isActive) {
                    delay(random.nextLong(12_000, 30_000))
                    val recent = posts.idsSince(time.now().minus(Duration.ofDays(7)).toEpochMilli())
                    if (recent.isEmpty()) continue
                    val seen = recent.shuffled(random).take(2 + random.nextInt(5))
                    posts.addViews(seen, 1 + random.nextInt(9))
                    if (random.nextFloat() < 0.25f) posts.incrementSaves(seen.first())
                    if (random.nextFloat() < 0.12f) posts.incrementShares(seen.last())
                }
            }
    }

    /** A pull to refresh sometimes finds a post that was just published. */
    suspend fun refreshFeed() {
        if (random.nextFloat() < 0.5f) publishPost()
    }

    private suspend fun publishPost() {
        val author = members.others(SELF_ID).filter { it.discoverable && !safety.isBlocked(it.id) }.randomOrNull(random) ?: return
        posts.insertAll(
            listOf(
                PostEntity(
                    id = "${author.id}-${UUID.randomUUID()}",
                    authorId = author.id,
                    caption = SeedContent.captions[random.nextInt(SeedContent.captions.size)],
                    media = if (random.nextFloat() < 0.6f) MediaRef.Generated(random.nextLong()).encode() else null,
                    createdAt = time.now().toEpochMilli(),
                    likeCount = 0,
                    commentCount = 0,
                    likedByMe = false,
                    // Its author saw it go up.
                    viewCount = 1,
                ),
            ),
        )
    }

    /** Called after you send a message. The other person reads it and usually answers. */
    fun onOutgoingMessage(conversationId: String) {
        scope.launch {
            val lock = synchronized(replyLocks) { replyLocks.getOrPut(conversationId) { Mutex() } }
            if (lock.isLocked) return@launch
            lock.withLock { respond(conversationId) }
        }
    }

    private suspend fun respond(conversationId: String) {
        val convo = conversations.get(conversationId) ?: return
        if (safety.isBlocked(convo.peerId)) return
        val peer = members.get(convo.peerId) ?: return

        delay(random.nextLong(500, 900))
        messages.promoteStatus(conversationId, SELF_ID, listOf(DeliveryStatus.Sent.name), DeliveryStatus.Delivered.name)
        conversations.setLastStatusFor(conversationId, SELF_ID, DeliveryStatus.Delivered.name)

        // Offline members come back a little later.
        if (!peer.online) delay(random.nextLong(3_000, 8_000))
        members.setPresence(peer.id, true, time.now().toEpochMilli())

        delay(random.nextLong(800, 2_000))
        messages.promoteStatus(
            conversationId,
            SELF_ID,
            listOf(DeliveryStatus.Sent.name, DeliveryStatus.Delivered.name),
            DeliveryStatus.Read.name,
        )
        conversations.setLastStatusFor(conversationId, SELF_ID, DeliveryStatus.Read.name)

        if (random.nextFloat() < 0.08f) return // sometimes people just read

        val recent = messages.latest(conversationId, 3).filter { it.senderId == SELF_ID }
        val last = recent.firstOrNull()
        val lastText = last?.text.orEmpty()
        val reply = if (last != null && lastText.isBlank() && last.attachments != "[]") {
            SeedContent.mediaReplies[random.nextInt(SeedContent.mediaReplies.size)]
        } else {
            pickReply(lastText, isFirstExchange = !convo.peerHasReplied)
        }

        _typing.update { it + conversationId }
        delay((1_200L + reply.length * 35L).coerceAtMost(5_000L))
        _typing.update { it - conversationId }

        if (conversations.get(conversationId) == null || safety.isBlocked(peer.id)) return
        val now = time.now().toEpochMilli()
        val viewing = visibility.isViewing(conversationId)
        val message = MessageEntity(UUID.randomUUID().toString(), conversationId, peer.id, reply, now, DeliveryStatus.Delivered.name)
        messages.insert(message)
        val current = conversations.get(conversationId) ?: return
        conversations.upsert(
            current.copy(
                lastActivityAt = now,
                unreadCount = if (viewing) 0 else current.unreadCount + 1,
                peerHasReplied = true,
                lastMessageId = message.id,
                lastMessageText = reply,
                lastMessageSenderId = peer.id,
                lastMessageAt = now,
                lastMessageStatus = message.status,
                lastMessageAttachments = "[]",
            ),
        )
        if (!viewing) maybeNotify(conversationId, peer.displayName, reply, isRequest = false, muted = current.muted)
    }

    private fun pickReply(lastText: String, isFirstExchange: Boolean): String {
        val words = lastText.lowercase().split(Regex("[^\\p{L}]+")).filter { it.isNotBlank() }
        val pool = when {
            isFirstExchange && words.any { it in SeedContent.greetingWords } -> SeedContent.greetingReplies
            lastText.trim().endsWith("?") -> SeedContent.questionReplies
            words.firstOrNull() in SeedContent.greetingWords -> SeedContent.greetingReplies
            else -> SeedContent.generalReplies
        }
        return pool[random.nextInt(pool.size)]
    }

    /** Members notice a new post over the next minutes. */
    fun onPostCreated(postId: String) {
        scope.launch {
            val people = members.others(SELF_ID).shuffled(random).take(2 + random.nextInt(4))
            people.forEachIndexed { i, person ->
                delay(random.nextLong(6_000, 20_000) * (i + 1))
                if (posts.get(postId) == null) return@launch
                // More people see a post than react to it.
                posts.addViews(listOf(postId), 3 + random.nextInt(12))
                if (random.nextFloat() < 0.2f) posts.incrementSaves(postId)
                val now = time.now().toEpochMilli()
                val comment = i % 2 == 1
                if (comment) {
                    val text = SeedContent.comments[random.nextInt(SeedContent.comments.size)]
                    comments.insertAll(listOf(CommentEntity(UUID.randomUUID().toString(), postId, person.id, text, now)))
                    posts.incrementComments(postId)
                    activity.insert(ActivityEntity(UUID.randomUUID().toString(), ActivityKind.Comment.name, person.id, postId, text, now, false))
                    notifyActivity(person.displayName, text, comment = true)
                } else {
                    posts.incrementLikes(postId)
                    activity.insert(ActivityEntity(UUID.randomUUID().toString(), ActivityKind.Like.name, person.id, postId, null, now, false))
                    notifyActivity(person.displayName, null, comment = false)
                }
            }
        }
    }

    private suspend fun maybeNotify(conversationId: String, name: String, text: String, isRequest: Boolean, muted: Boolean) {
        val p = prefs.userPreferences.first()
        if (!p.notifyMessages || muted || visibility.foreground.value) return
        notifier.showMessage(conversationId, name, text, isRequest)
    }

    private suspend fun notifyActivity(name: String, text: String?, comment: Boolean) {
        val p = prefs.userPreferences.first()
        if (!p.notifyActivity || visibility.foreground.value) return
        notifier.showActivity(name, if (comment) text.orEmpty() else null)
    }

    fun resetSession() {
        _typing.value = emptySet()
    }

    private companion object {
        const val MAX_INTROS = 4
    }

}
