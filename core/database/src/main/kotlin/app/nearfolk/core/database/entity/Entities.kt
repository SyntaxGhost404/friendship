package app.nearfolk.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "members")
data class MemberEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val gender: String,
    val birthEpochDay: Long,
    val countryCode: String,
    val city: String?,
    val catchphrase: String,
    val bio: String,
    val avatar: String?,
    val banner: String?,
    val streakStartEpochDay: Long,
    val online: Boolean,
    val lastSeenAt: Long,
    val joinedAt: Long,
    val isPlus: Boolean,
    val voicePath: String?,
    val voiceDurationMs: Long?,
    val voiceRecordedAt: Long?,
    /** False while the profile is deactivated. */
    val discoverable: Boolean,
)

@Entity(tableName = "highlights", indices = [Index("memberId")])
data class HighlightEntity(
    @PrimaryKey val id: String,
    val memberId: String,
    val media: String,
    val caption: String?,
    val createdAt: Long,
)

@Entity(tableName = "posts", indices = [Index("authorId"), Index("createdAt")])
data class PostEntity(
    @PrimaryKey val id: String,
    val authorId: String,
    val caption: String,
    val media: String?,
    val createdAt: Long,
    val likeCount: Int,
    val commentCount: Int,
    val likedByMe: Boolean,
)

@Entity(tableName = "comments", indices = [Index("postId")])
data class CommentEntity(
    @PrimaryKey val id: String,
    val postId: String,
    val authorId: String,
    val text: String,
    val createdAt: Long,
)

@Entity(tableName = "conversations", indices = [Index(value = ["peerId"], unique = true), Index("lastActivityAt")])
data class ConversationEntity(
    @PrimaryKey val id: String,
    val peerId: String,
    val state: String,
    val createdAt: Long,
    val lastActivityAt: Long,
    val unreadCount: Int,
    val muted: Boolean,
    /** Whether the other person has written at least once. Lifts the first contact cap. */
    val peerHasReplied: Boolean,
    val lastMessageId: String?,
    val lastMessageText: String?,
    val lastMessageSenderId: String?,
    val lastMessageAt: Long?,
    val lastMessageStatus: String?,
)

@Entity(tableName = "messages", indices = [Index(value = ["conversationId", "sentAt"])])
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val senderId: String,
    val text: String,
    val sentAt: Long,
    val status: String,
)

@Entity(tableName = "journal", indices = [Index("authorId")])
data class JournalEntity(
    @PrimaryKey val id: String,
    val authorId: String,
    val title: String,
    val body: String,
    val mood: String,
    val visibility: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "blocks")
data class BlockEntity(
    @PrimaryKey val memberId: String,
    val blockedAt: Long,
)

@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey val id: String,
    val memberId: String,
    val conversationId: String?,
    val reason: String,
    val details: String?,
    val createdAt: Long,
)

@Entity(tableName = "activity", indices = [Index("createdAt")])
data class ActivityEntity(
    @PrimaryKey val id: String,
    val kind: String,
    val actorId: String,
    val postId: String,
    val commentText: String?,
    val createdAt: Long,
    val seen: Boolean,
)

/** Query results that join an author onto content. */
data class PostRow(
    val id: String,
    val authorId: String,
    val caption: String,
    val media: String?,
    val createdAt: Long,
    val likeCount: Int,
    val commentCount: Int,
    val likedByMe: Boolean,
    val authorName: String,
    val authorAvatar: String?,
)

data class CommentRow(
    val id: String,
    val postId: String,
    val authorId: String,
    val text: String,
    val createdAt: Long,
    val authorName: String,
    val authorAvatar: String?,
)

data class ActivityRow(
    val id: String,
    val kind: String,
    val actorId: String,
    val postId: String,
    val commentText: String?,
    val createdAt: Long,
    val seen: Boolean,
    val actorName: String,
    val actorAvatar: String?,
    val postCaption: String,
)
