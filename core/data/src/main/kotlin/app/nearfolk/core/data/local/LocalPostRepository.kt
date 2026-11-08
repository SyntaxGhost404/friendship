package app.nearfolk.core.data.local

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import app.nearfolk.core.common.TimeSource
import app.nearfolk.core.data.community.CommunitySimulator
import app.nearfolk.core.data.media.MediaFiles
import app.nearfolk.core.data.repository.PostRepository
import app.nearfolk.core.data.repository.PostStats
import app.nearfolk.core.database.NearfolkDatabase
import app.nearfolk.core.database.entity.CommentEntity
import app.nearfolk.core.database.entity.PostEntity
import app.nearfolk.core.model.ActivityItem
import app.nearfolk.core.model.Comment
import app.nearfolk.core.model.MediaRef
import app.nearfolk.core.model.Post
import app.nearfolk.core.model.ProfileRules
import app.nearfolk.core.model.SELF_ID
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class LocalPostRepository @Inject constructor(
    private val db: NearfolkDatabase,
    private val media: MediaFiles,
    private val simulator: CommunitySimulator,
    private val time: TimeSource,
) : PostRepository {
    private val posts = db.postDao()
    private val comments = db.commentDao()
    private val activity = db.activityDao()

    override fun posts(authorId: String): Flow<PagingData<Post>> =
        Pager(PagingConfig(pageSize = 12, prefetchDistance = 6, enablePlaceholders = false)) { posts.pagingFor(authorId) }
            .flow
            .map { page -> page.map { it.toModel() } }

    override fun allPosts(authorId: String): Flow<List<Post>> = posts.observeFor(authorId).map { rows -> rows.map { it.toModel() } }

    override fun post(id: String): Flow<Post?> = posts.observe(id).map { it?.toModel() }

    override fun comments(postId: String): Flow<List<Comment>> = comments.observe(postId).map { rows -> rows.map { it.toModel() } }

    override fun stats(authorId: String): Flow<PostStats> = combine(
        posts.observeCount(authorId),
        posts.observeLikeTotal(authorId),
        db.highlightDao().observe(authorId),
    ) { count, likes, highlights -> PostStats(count, likes, highlights.size) }

    override suspend fun toggleLike(postId: String) = posts.toggleLike(postId)

    override suspend fun addComment(postId: String, text: String) {
        val clean = text.trim().take(ProfileRules.COMMENT_MAX)
        if (clean.isEmpty()) return
        db.withTransaction {
            comments.insertAll(listOf(CommentEntity(UUID.randomUUID().toString(), postId, SELF_ID, clean, time.now().toEpochMilli())))
            posts.incrementComments(postId)
        }
    }

    override suspend fun createPost(caption: String, mediaPath: String?) {
        val id = UUID.randomUUID().toString()
        posts.insertAll(
            listOf(
                PostEntity(
                    id = id,
                    authorId = SELF_ID,
                    caption = caption.trim().take(ProfileRules.CAPTION_MAX),
                    media = mediaPath?.let { MediaRef.Local(it).encode() },
                    createdAt = time.now().toEpochMilli(),
                    likeCount = 0,
                    commentCount = 0,
                    likedByMe = false,
                ),
            ),
        )
        simulator.onPostCreated(id)
    }

    override suspend fun deletePost(id: String) {
        val row = posts.get(id) ?: return
        if (row.authorId != SELF_ID) return
        db.withTransaction {
            comments.deleteForPost(id)
            activity.deleteForPost(id)
            posts.delete(id)
        }
        (MediaRef.decode(row.media) as? MediaRef.Local)?.let { media.delete(it.path) }
    }

    override fun activity(): Flow<List<ActivityItem>> = activity.observe().map { rows -> rows.map { it.toModel() } }

    override fun unseenActivityCount(): Flow<Int> = activity.observeUnseen()

    override suspend fun markActivitySeen() = activity.markAllSeen()
}
