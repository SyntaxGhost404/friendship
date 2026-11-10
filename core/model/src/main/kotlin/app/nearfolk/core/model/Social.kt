package app.nearfolk.core.model

import java.time.Instant

data class Post(
    val id: String,
    val authorId: String,
    val authorName: String,
    val authorAvatar: MediaRef?,
    val caption: String,
    val media: MediaRef?,
    val createdAt: Instant,
    val likeCount: Int,
    val commentCount: Int,
    val likedByMe: Boolean,
    /** Times the post has been seen, by anyone. */
    val viewCount: Int = 0,
    /** How many people keep the post in their saved list. */
    val saveCount: Int = 0,
    val shareCount: Int = 0,
    /** When you saved the post, or null if it is not in your saved list. */
    val savedAt: Instant? = null,
) {
    val savedByMe: Boolean get() = savedAt != null
}

data class Comment(
    val id: String,
    val postId: String,
    val authorId: String,
    val authorName: String,
    val authorAvatar: MediaRef?,
    val text: String,
    val createdAt: Instant,
    val attachments: List<Attachment> = emptyList(),
)

data class Highlight(
    val id: String,
    val memberId: String,
    val media: MediaRef,
    val caption: String?,
    val createdAt: Instant,
)

enum class ActivityKind { Like, Comment }

/** Something another member did to your content. */
data class ActivityItem(
    val id: String,
    val kind: ActivityKind,
    val actorId: String,
    val actorName: String,
    val actorAvatar: MediaRef?,
    val postId: String,
    val postCaption: String,
    val commentText: String?,
    val createdAt: Instant,
    val seen: Boolean,
)
