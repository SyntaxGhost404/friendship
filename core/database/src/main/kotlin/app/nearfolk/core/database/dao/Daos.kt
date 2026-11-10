package app.nearfolk.core.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import app.nearfolk.core.database.entity.ActivityEntity
import app.nearfolk.core.database.entity.ActivityRow
import app.nearfolk.core.database.entity.BlockEntity
import app.nearfolk.core.database.entity.CommentEntity
import app.nearfolk.core.database.entity.CommentRow
import app.nearfolk.core.database.entity.ConversationEntity
import app.nearfolk.core.database.entity.HighlightEntity
import app.nearfolk.core.database.entity.JournalEntity
import app.nearfolk.core.database.entity.MemberEntity
import app.nearfolk.core.database.entity.MessageEntity
import app.nearfolk.core.database.entity.PostEntity
import app.nearfolk.core.database.entity.PostRow
import app.nearfolk.core.database.entity.ReportEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemberDao {
    @Query("SELECT * FROM members WHERE id != :selfId AND discoverable = 1 AND id NOT IN (SELECT memberId FROM blocks)")
    fun observeDiscoverable(selfId: String): Flow<List<MemberEntity>>

    @Query("SELECT * FROM members WHERE id = :id")
    fun observe(id: String): Flow<MemberEntity?>

    @Query("SELECT * FROM members WHERE id = :id")
    suspend fun get(id: String): MemberEntity?

    @Query("SELECT * FROM members WHERE id != :selfId")
    suspend fun others(selfId: String): List<MemberEntity>

    @Query("SELECT * FROM members WHERE id IN (SELECT memberId FROM blocks) ORDER BY displayName")
    fun observeBlocked(): Flow<List<MemberEntity>>

    @Query("SELECT COUNT(*) FROM members")
    suspend fun count(): Int

    @Upsert
    suspend fun upsert(member: MemberEntity)

    @Upsert
    suspend fun upsertAll(members: List<MemberEntity>)

    @Query("UPDATE members SET online = :online, lastSeenAt = :lastSeenAt WHERE id = :id")
    suspend fun setPresence(id: String, online: Boolean, lastSeenAt: Long)

    @Query("UPDATE members SET discoverable = :discoverable WHERE id = :id")
    suspend fun setDiscoverable(id: String, discoverable: Boolean)

    @Query("DELETE FROM members WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface HighlightDao {
    @Query("SELECT * FROM highlights WHERE memberId = :memberId ORDER BY createdAt DESC")
    fun observe(memberId: String): Flow<List<HighlightEntity>>

    @Query("SELECT COUNT(*) FROM highlights WHERE memberId = :memberId")
    suspend fun count(memberId: String): Int

    @Query("SELECT * FROM highlights WHERE id = :id")
    suspend fun get(id: String): HighlightEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<HighlightEntity>)

    @Query("DELETE FROM highlights WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM highlights WHERE memberId = :memberId")
    suspend fun deleteFor(memberId: String)
}

@Dao
interface PostDao {
    @Query(
        """
        SELECT p.*, m.displayName AS authorName, m.avatar AS authorAvatar
        FROM posts p JOIN members m ON m.id = p.authorId
        WHERE p.authorId = :authorId
        ORDER BY p.createdAt DESC
        """,
    )
    fun pagingFor(authorId: String): PagingSource<Int, PostRow>

    /**
     * Everyone's posts, newest first: your own, plus those of members who are
     * discoverable and not blocked. A blocked or deactivated member drops out
     * of the feed the moment that changes.
     */
    @Query(
        """
        SELECT p.*, m.displayName AS authorName, m.avatar AS authorAvatar
        FROM posts p JOIN members m ON m.id = p.authorId
        WHERE m.id = :selfId OR (m.discoverable = 1 AND m.id NOT IN (SELECT memberId FROM blocks))
        ORDER BY p.createdAt DESC, p.id DESC
        """,
    )
    fun pagingFeed(selfId: String): PagingSource<Int, PostRow>

    /** Posts you saved, the most recently saved first, from authors you can still see. */
    @Query(
        """
        SELECT p.*, m.displayName AS authorName, m.avatar AS authorAvatar
        FROM posts p JOIN members m ON m.id = p.authorId
        WHERE p.savedAt IS NOT NULL
            AND (m.id = :selfId OR (m.discoverable = 1 AND m.id NOT IN (SELECT memberId FROM blocks)))
        ORDER BY p.savedAt DESC, p.id DESC
        """,
    )
    fun pagingSaved(selfId: String): PagingSource<Int, PostRow>

    @Query(
        """
        SELECT COUNT(*) FROM posts p JOIN members m ON m.id = p.authorId
        WHERE p.savedAt IS NOT NULL
            AND (m.id = :selfId OR (m.discoverable = 1 AND m.id NOT IN (SELECT memberId FROM blocks)))
        """,
    )
    fun observeSavedCount(selfId: String): Flow<Int>

    @Query(
        """
        SELECT p.*, m.displayName AS authorName, m.avatar AS authorAvatar
        FROM posts p JOIN members m ON m.id = p.authorId
        WHERE p.authorId = :authorId
        ORDER BY p.createdAt DESC
        """,
    )
    fun observeFor(authorId: String): Flow<List<PostRow>>

    @Query(
        """
        SELECT p.*, m.displayName AS authorName, m.avatar AS authorAvatar
        FROM posts p JOIN members m ON m.id = p.authorId WHERE p.id = :id
        """,
    )
    fun observe(id: String): Flow<PostRow?>

    @Query("SELECT * FROM posts WHERE id = :id")
    suspend fun get(id: String): PostEntity?

    @Query("SELECT * FROM posts WHERE authorId = :authorId")
    suspend fun allFor(authorId: String): List<PostEntity>

    @Query("SELECT COUNT(*) FROM posts WHERE authorId = :authorId")
    fun observeCount(authorId: String): Flow<Int>

    @Query("SELECT COALESCE(SUM(likeCount), 0) FROM posts WHERE authorId = :authorId")
    fun observeLikeTotal(authorId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(posts: List<PostEntity>)

    @Query("UPDATE posts SET likedByMe = NOT likedByMe, likeCount = likeCount + CASE WHEN likedByMe THEN -1 ELSE 1 END WHERE id = :id")
    suspend fun toggleLike(id: String)

    @Query("UPDATE posts SET likeCount = likeCount + 1 WHERE id = :id")
    suspend fun incrementLikes(id: String)

    /** Saves the post at [now], or takes it out of your saved list if it is in there. */
    @Query(
        """
        UPDATE posts SET
            saveCount = MAX(0, saveCount + CASE WHEN savedAt IS NULL THEN 1 ELSE -1 END),
            savedAt = CASE WHEN savedAt IS NULL THEN :now ELSE NULL END
        WHERE id = :id
        """,
    )
    suspend fun toggleSave(id: String, now: Long)

    /** Puts the post in your saved list at [savedAt], or removes it when that is null. */
    @Query(
        """
        UPDATE posts SET
            saveCount = MAX(0, saveCount + CASE
                WHEN savedAt IS NULL AND :savedAt IS NOT NULL THEN 1
                WHEN savedAt IS NOT NULL AND :savedAt IS NULL THEN -1
                ELSE 0 END),
            savedAt = :savedAt
        WHERE id = :id
        """,
    )
    suspend fun setSaved(id: String, savedAt: Long?)

    @Query("UPDATE posts SET viewCount = viewCount + :amount WHERE id IN (:ids)")
    suspend fun addViews(ids: List<String>, amount: Int)

    @Query("UPDATE posts SET saveCount = saveCount + 1 WHERE id = :id")
    suspend fun incrementSaves(id: String)

    @Query("UPDATE posts SET shareCount = shareCount + 1 WHERE id = :id")
    suspend fun incrementShares(id: String)

    @Query("SELECT id FROM posts WHERE createdAt >= :since")
    suspend fun idsSince(since: Long): List<String>

    @Query("UPDATE posts SET commentCount = commentCount + 1 WHERE id = :id")
    suspend fun incrementComments(id: String)

    @Query("DELETE FROM posts WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM posts WHERE authorId = :authorId")
    suspend fun deleteFor(authorId: String)
}

@Dao
interface CommentDao {
    @Query(
        """
        SELECT c.*, m.displayName AS authorName, m.avatar AS authorAvatar
        FROM comments c JOIN members m ON m.id = c.authorId
        WHERE c.postId = :postId ORDER BY c.createdAt ASC
        """,
    )
    fun observe(postId: String): Flow<List<CommentRow>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(comments: List<CommentEntity>)

    @Query("SELECT * FROM comments WHERE postId = :postId")
    suspend fun forPost(postId: String): List<CommentEntity>

    @Query("DELETE FROM comments WHERE postId = :postId")
    suspend fun deleteForPost(postId: String)

    @Query("DELETE FROM comments WHERE authorId = :authorId")
    suspend fun deleteByAuthor(authorId: String)
}

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations WHERE state = :state ORDER BY lastActivityAt DESC")
    fun observeByState(state: String): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id")
    fun observe(id: String): Flow<ConversationEntity?>

    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun get(id: String): ConversationEntity?

    @Query("SELECT * FROM conversations WHERE peerId = :peerId")
    suspend fun forPeer(peerId: String): ConversationEntity?

    @Query("SELECT COALESCE(SUM(unreadCount), 0) FROM conversations WHERE state = 'Active' AND muted = 0")
    fun observeUnreadTotal(): Flow<Int>

    @Query("SELECT COUNT(*) FROM conversations WHERE state = 'Request'")
    fun observeRequestCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM conversations WHERE state = 'Request'")
    suspend fun requestCount(): Int

    @Upsert
    suspend fun upsert(conversation: ConversationEntity)

    @Query("UPDATE conversations SET unreadCount = 0 WHERE id = :id")
    suspend fun clearUnread(id: String)

    @Query("UPDATE conversations SET state = :state WHERE id = :id")
    suspend fun setState(id: String, state: String)

    @Query("UPDATE conversations SET muted = :muted WHERE id = :id")
    suspend fun setMuted(id: String, muted: Boolean)

    @Query("UPDATE conversations SET lastMessageStatus = :status WHERE id = :id AND lastMessageSenderId = :senderId")
    suspend fun setLastStatusFor(id: String, senderId: String, status: String)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY sentAt ASC")
    fun observe(conversationId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId")
    suspend fun allFor(conversationId: String): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY sentAt DESC LIMIT :limit")
    suspend fun latest(conversationId: String, limit: Int): List<MessageEntity>

    @Query("SELECT COUNT(*) FROM messages WHERE conversationId = :conversationId AND senderId = :senderId")
    suspend fun countFrom(conversationId: String, senderId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<MessageEntity>)

    @Query("UPDATE messages SET status = :status WHERE conversationId = :conversationId AND senderId = :senderId AND status IN (:from)")
    suspend fun promoteStatus(conversationId: String, senderId: String, from: List<String>, status: String)

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteFor(conversationId: String)
}

@Dao
interface JournalDao {
    @Query("SELECT * FROM journal WHERE authorId = :authorId ORDER BY createdAt DESC")
    fun observeFor(authorId: String): Flow<List<JournalEntity>>

    @Query("SELECT * FROM journal WHERE authorId = :authorId AND visibility = 'Public' ORDER BY createdAt DESC")
    fun observePublic(authorId: String): Flow<List<JournalEntity>>

    @Query("SELECT COUNT(*) FROM journal WHERE authorId = :authorId AND visibility = 'Public'")
    fun observePublicCount(authorId: String): Flow<Int>

    @Query("SELECT * FROM journal WHERE id = :id")
    fun observe(id: String): Flow<JournalEntity?>

    @Query("SELECT * FROM journal WHERE id = :id")
    suspend fun get(id: String): JournalEntity?

    @Upsert
    suspend fun upsert(entry: JournalEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<JournalEntity>)

    @Query("DELETE FROM journal WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM journal WHERE authorId = :authorId")
    suspend fun deleteFor(authorId: String)
}

@Dao
interface SafetyDao {
    @Query("SELECT EXISTS(SELECT 1 FROM blocks WHERE memberId = :memberId)")
    fun observeBlocked(memberId: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM blocks WHERE memberId = :memberId)")
    suspend fun isBlocked(memberId: String): Boolean

    @Upsert
    suspend fun block(entity: BlockEntity)

    @Query("DELETE FROM blocks WHERE memberId = :memberId")
    suspend fun unblock(memberId: String)

    @Insert
    suspend fun report(entity: ReportEntity)
}

@Dao
interface ActivityDao {
    @Query(
        """
        SELECT a.*, m.displayName AS actorName, m.avatar AS actorAvatar, p.caption AS postCaption
        FROM activity a
        JOIN members m ON m.id = a.actorId
        JOIN posts p ON p.id = a.postId
        ORDER BY a.createdAt DESC
        """,
    )
    fun observe(): Flow<List<ActivityRow>>

    @Query("SELECT COUNT(*) FROM activity WHERE seen = 0")
    fun observeUnseen(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ActivityEntity)

    @Query("UPDATE activity SET seen = 1")
    suspend fun markAllSeen()

    @Query("DELETE FROM activity WHERE postId = :postId")
    suspend fun deleteForPost(postId: String)
}

@Dao
interface MaintenanceDao {
    @Transaction
    @Query("DELETE FROM activity")
    suspend fun clearActivity()
}
