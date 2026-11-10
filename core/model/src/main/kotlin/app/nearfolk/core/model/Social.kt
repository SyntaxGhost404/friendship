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

/**
 * A comment on a post. Threads are two levels deep, as on YouTube: a top-level
 * comment and the replies under it. A reply to a reply joins the same thread
 * and names the person it answers.
 */
data class Comment(
    val id: String,
    val postId: String,
    val authorId: String,
    val authorName: String,
    val authorAvatar: MediaRef?,
    val text: String,
    val createdAt: Instant,
    /** Only comments written before comments became text only can have these. */
    val attachments: List<Attachment> = emptyList(),
    /** The top-level comment this reply sits under; null for a top-level comment. */
    val parentId: String? = null,
    /** The member a reply answers by name, when it answers another reply. */
    val replyToId: String? = null,
    val replyToName: String? = null,
    /** Likes minus dislikes from everyone, your own vote included. */
    val score: Int = 0,
    val myVote: Vote = Vote.None,
    /** Replies under a top-level comment. Always 0 for replies. */
    val replyCount: Int = 0,
) {
    val isReply: Boolean get() = parentId != null

    /** The top-level comment of the thread this comment belongs to. */
    val threadId: String get() = parentId ?: id
}

/** Your vote on a comment. A dislike takes one off the score, like a downvote. */
enum class Vote(val value: Int) {
    Down(-1),
    None(0),
    Up(1),
    ;

    companion object {
        fun of(value: Int): Vote = when {
            value > 0 -> Up
            value < 0 -> Down
            else -> None
        }
    }
}

/** Order of top-level comments. Replies are always oldest first. */
enum class CommentSort { Top, Newest }

/** Everything a comments sheet shows. */
data class CommentsState(
    val postId: String? = null,
    val postAuthorId: String? = null,
    /** All comments on the post, replies included. */
    val total: Int = 0,
    val sort: CommentSort = CommentSort.Top,
    val loading: Boolean = true,
    val threads: List<Comment> = emptyList(),
    /** The top-level comment whose replies are open, or null on the comment list. */
    val openThread: Comment? = null,
    val replies: List<Comment> = emptyList(),
    /** The reply being answered by name, or null when answering the thread itself. */
    val replyingTo: Comment? = null,
)

data class Highlight(
    val id: String,
    val memberId: String,
    val media: MediaRef,
    val caption: String?,
    val createdAt: Instant,
)

enum class ActivityKind { Like, Comment, Reply }

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
