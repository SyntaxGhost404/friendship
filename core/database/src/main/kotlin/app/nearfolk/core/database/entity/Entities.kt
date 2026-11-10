package app.nearfolk.core.database.entity

import androidx.room.ColumnInfo
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
    @ColumnInfo(defaultValue = "0") val viewCount: Int = 0,
    @ColumnInfo(defaultValue = "0") val saveCount: Int = 0,
    @ColumnInfo(defaultValue = "0") val shareCount: Int = 0,
    /** When you saved the post, or null if you have not. */
    val savedAt: Long? = null,
)

@Entity(tableName = "comments", indices = [Index("postId"), Index("parentId")])
data class CommentEntity(
    @PrimaryKey val id: String,
    val postId: String,
    val authorId: String,
    val text: String,
    val createdAt: Long,
    /** JSON list of attachments, see AttachmentCodec in core:data. New comments are text only. */
    @ColumnInfo(defaultValue = "[]") val attachments: String = "[]",
    /** The top-level comment a reply sits under. Replies never nest deeper. */
    val parentId: String? = null,
    /** The member a reply answers by name. */
    val replyToId: String? = null,
    /** Likes minus dislikes, your own vote included. */
    @ColumnInfo(defaultValue = "0") val score: Int = 0,
    /** Your vote: 1, 0 or -1. */
    @ColumnInfo(defaultValue = "0") val myVote: Int = 0,
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
    @ColumnInfo(defaultValue = "[]") val lastMessageAttachments: String = "[]",
)

@Entity(tableName = "messages", indices = [Index(value = ["conversationId", "sentAt"])])
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val senderId: String,
    val text: String,
    val sentAt: Long,
    val status: String,
    @ColumnInfo(defaultValue = "[]") val attachments: String = "[]",
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
    val viewCount: Int,
    val saveCount: Int,
    val shareCount: Int,
    val savedAt: Long?,
    val authorName: String,
    val authorAvatar: String?,
)

data class CommentRow(
    val id: String,
    val postId: String,
    val authorId: String,
    val text: String,
    val createdAt: Long,
    val attachments: String,
    val parentId: String?,
    val replyToId: String?,
    val score: Int,
    val myVote: Int,
    val authorName: String,
    val authorAvatar: String?,
    val replyToName: String?,
    val replyCount: Int,
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
