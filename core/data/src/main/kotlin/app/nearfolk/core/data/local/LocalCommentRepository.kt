package app.nearfolk.core.data.local

import androidx.room.withTransaction
import app.nearfolk.core.common.TimeSource
import app.nearfolk.core.data.community.CommunitySimulator
import app.nearfolk.core.data.repository.CommentRepository
import app.nearfolk.core.database.NearfolkDatabase
import app.nearfolk.core.database.entity.CommentEntity
import app.nearfolk.core.model.Comment
import app.nearfolk.core.model.ProfileRules
import app.nearfolk.core.model.SELF_ID
import app.nearfolk.core.model.Vote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class LocalCommentRepository @Inject constructor(
    private val db: NearfolkDatabase,
    private val simulator: CommunitySimulator,
    private val time: TimeSource,
) : CommentRepository {
    private val comments = db.commentDao()
    private val posts = db.postDao()

    override fun topLevel(postId: String): Flow<List<Comment>> =
        comments.observeTopLevel(postId).map { rows -> rows.map { it.toModel() } }

    override fun replies(commentId: String): Flow<List<Comment>> =
        comments.observeReplies(commentId).map { rows -> rows.map { it.toModel() } }

    override fun comment(id: String): Flow<Comment?> = comments.observeOne(id).map { it?.toModel() }

    override suspend fun add(postId: String, text: String, replyTo: Comment?) {
        val clean = text.trim().take(ProfileRules.COMMENT_MAX)
        if (clean.isEmpty()) return
        val entity = CommentEntity(
            id = UUID.randomUUID().toString(),
            postId = postId,
            authorId = SELF_ID,
            text = clean,
            createdAt = time.now().toEpochMilli(),
            parentId = replyTo?.threadId,
            replyToId = mentionFor(replyTo),
        )
        db.withTransaction {
            comments.insertAll(listOf(entity))
            posts.incrementComments(postId)
        }
        simulator.onCommentCreated(entity.id)
    }

    override suspend fun vote(commentId: String, vote: Vote) {
        if (vote == Vote.None) return
        comments.vote(commentId, vote.value)
    }

    internal companion object {
        /**
         * Who a reply names. Answering the top-level comment needs no name, it
         * sits right under it. Answering a reply names that reply's author,
         * unless that is you.
         */
        fun mentionFor(replyTo: Comment?): String? =
            replyTo?.takeIf { it.isReply && it.authorId != SELF_ID }?.authorId
    }
}
