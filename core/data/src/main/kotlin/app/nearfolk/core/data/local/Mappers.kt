package app.nearfolk.core.data.local

import app.nearfolk.core.database.entity.ActivityRow
import app.nearfolk.core.database.entity.CommentRow
import app.nearfolk.core.database.entity.ConversationEntity
import app.nearfolk.core.database.entity.HighlightEntity
import app.nearfolk.core.database.entity.JournalEntity
import app.nearfolk.core.database.entity.MemberEntity
import app.nearfolk.core.database.entity.MessageEntity
import app.nearfolk.core.database.entity.PostRow
import app.nearfolk.core.model.ActivityItem
import app.nearfolk.core.model.ActivityKind
import app.nearfolk.core.model.Comment
import app.nearfolk.core.model.Conversation
import app.nearfolk.core.model.ConversationState
import app.nearfolk.core.model.DeliveryStatus
import app.nearfolk.core.model.Gender
import app.nearfolk.core.model.Highlight
import app.nearfolk.core.model.JournalEntry
import app.nearfolk.core.model.MediaRef
import app.nearfolk.core.model.Member
import app.nearfolk.core.model.Message
import app.nearfolk.core.model.Mood
import app.nearfolk.core.model.Post
import app.nearfolk.core.model.Presence
import app.nearfolk.core.model.Visibility
import app.nearfolk.core.model.VoiceIntro
import java.time.Instant
import java.time.LocalDate

internal fun MemberEntity.toModel() = Member(
    id = id,
    displayName = displayName,
    gender = enumOr(gender, Gender.Nonbinary),
    birthDate = LocalDate.ofEpochDay(birthEpochDay),
    countryCode = countryCode,
    city = city,
    catchphrase = catchphrase,
    bio = bio,
    avatar = MediaRef.decode(avatar),
    banner = MediaRef.decode(banner),
    streakStart = LocalDate.ofEpochDay(streakStartEpochDay),
    presence = if (online) Presence.Online else Presence.Offline(Instant.ofEpochMilli(lastSeenAt)),
    joinedAt = Instant.ofEpochMilli(joinedAt),
    isPlus = isPlus,
    voiceIntro = run {
        val path = voicePath
        val duration = voiceDurationMs
        if (path != null && duration != null) VoiceIntro(path, duration, Instant.ofEpochMilli(voiceRecordedAt ?: joinedAt)) else null
    },
)

internal fun HighlightEntity.toModel() = Highlight(
    id = id,
    memberId = memberId,
    media = MediaRef.decode(media) ?: MediaRef.Generated(id.hashCode().toLong()),
    caption = caption,
    createdAt = Instant.ofEpochMilli(createdAt),
)

internal fun PostRow.toModel() = Post(
    id = id,
    authorId = authorId,
    authorName = authorName,
    authorAvatar = MediaRef.decode(authorAvatar),
    caption = caption,
    media = MediaRef.decode(media),
    createdAt = Instant.ofEpochMilli(createdAt),
    likeCount = likeCount,
    commentCount = commentCount,
    likedByMe = likedByMe,
)

internal fun CommentRow.toModel() = Comment(
    id = id,
    postId = postId,
    authorId = authorId,
    authorName = authorName,
    authorAvatar = MediaRef.decode(authorAvatar),
    text = text,
    createdAt = Instant.ofEpochMilli(createdAt),
)

internal fun ActivityRow.toModel() = ActivityItem(
    id = id,
    kind = enumOr(kind, ActivityKind.Like),
    actorId = actorId,
    actorName = actorName,
    actorAvatar = MediaRef.decode(actorAvatar),
    postId = postId,
    postCaption = postCaption,
    commentText = commentText,
    createdAt = Instant.ofEpochMilli(createdAt),
    seen = seen,
)

internal fun MessageEntity.toModel() = Message(
    id = id,
    conversationId = conversationId,
    senderId = senderId,
    text = text,
    sentAt = Instant.ofEpochMilli(sentAt),
    status = enumOr(status, DeliveryStatus.Sent),
)

internal fun ConversationEntity.toModel(peer: Member, typing: Boolean): Conversation {
    val messageId = lastMessageId
    val text = lastMessageText
    val sender = lastMessageSenderId
    val at = lastMessageAt
    val last = if (messageId != null && text != null && sender != null && at != null) {
        Message(
            id = messageId,
            conversationId = id,
            senderId = sender,
            text = text,
            sentAt = Instant.ofEpochMilli(at),
            status = enumOr(lastMessageStatus, DeliveryStatus.Sent),
        )
    } else {
        null
    }
    return Conversation(
        id = id,
        peer = peer,
        state = enumOr(state, ConversationState.Active),
        lastMessage = last,
        unreadCount = unreadCount,
        muted = muted,
        createdAt = Instant.ofEpochMilli(createdAt),
        peerIsTyping = typing,
    )
}

internal fun JournalEntity.toModel() = JournalEntry(
    id = id,
    authorId = authorId,
    title = title,
    body = body,
    mood = enumOr(mood, Mood.Calm),
    visibility = enumOr(visibility, Visibility.Private),
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt),
)

internal inline fun <reified T : Enum<T>> enumOr(raw: String?, fallback: T): T =
    raw?.let { name -> enumValues<T>().firstOrNull { it.name == name } } ?: fallback
