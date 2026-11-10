package app.nearfolk.core.data

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.nearfolk.core.common.TimeSource
import app.nearfolk.core.data.community.CommunitySeeder
import app.nearfolk.core.data.community.CommunitySimulator
import app.nearfolk.core.data.local.LocalCommentRepository
import app.nearfolk.core.data.local.LocalPostRepository
import app.nearfolk.core.data.media.MediaFiles
import app.nearfolk.core.data.platform.AppVisibility
import app.nearfolk.core.data.platform.ChatNotifier
import app.nearfolk.core.data.repository.CommentThread
import app.nearfolk.core.data.repository.CommentThreadFactory
import app.nearfolk.core.data.repository.inStableOrder
import app.nearfolk.core.database.NearfolkDatabase
import app.nearfolk.core.database.entity.BlockEntity
import app.nearfolk.core.database.entity.CommentEntity
import app.nearfolk.core.database.entity.MemberEntity
import app.nearfolk.core.database.entity.PostEntity
import app.nearfolk.core.datastore.NearfolkPreferences
import app.nearfolk.core.model.Comment
import app.nearfolk.core.model.CommentSort
import app.nearfolk.core.model.CommentsState
import app.nearfolk.core.model.SELF_ID
import app.nearfolk.core.model.Vote
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.Instant
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class CommentsTest {

    private lateinit var db: NearfolkDatabase
    private lateinit var comments: LocalCommentRepository
    private lateinit var posts: LocalPostRepository
    private lateinit var seeder: CommunitySeeder
    private lateinit var prefs: NearfolkPreferences
    private var clock = Instant.parse("2026-11-10T10:00:00Z")
    private val live = CoroutineScope(Dispatchers.Default + Job())

    private val time = object : TimeSource {
        override fun now(): Instant = clock
        override fun zone(): ZoneId = ZoneId.of("UTC")
    }

    private val notifier = object : ChatNotifier {
        override fun showMessage(conversationId: String, senderName: String, text: String, isRequest: Boolean) = Unit
        override fun showActivity(actorName: String, commentText: String?) = Unit
        override fun cancel(conversationId: String) = Unit
    }

    private fun member(id: String, name: String) = MemberEntity(
        id = id, displayName = name, gender = "Female", birthEpochDay = 9000, countryCode = "NO",
        city = null, catchphrase = "", bio = "", avatar = null, banner = null, streakStartEpochDay = 19000,
        online = false, lastSeenAt = 0, joinedAt = 0, isPlus = false, voicePath = null, voiceDurationMs = null,
        voiceRecordedAt = null, discoverable = true,
    )

    private fun comment(id: String, author: String, at: Long, score: Int = 0, parent: String? = null, replyTo: String? = null) =
        CommentEntity(id = id, postId = "p1", authorId = author, text = "Text $id", createdAt = at, parentId = parent, replyToId = replyTo, score = score)

    private suspend fun top(): List<Comment> = comments.topLevel("p1").first()
    private suspend fun replies(id: String): List<Comment> = comments.replies(id).first()
    private suspend fun one(id: String): Comment = comments.comment(id).first()!!

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, NearfolkDatabase::class.java).allowMainThreadQueries().build()
        // The simulated community stays out of these tests: its scope never runs anything.
        val idle = CoroutineScope(Job().apply { cancel() })
        val store = PreferenceDataStoreFactory.create(scope = CoroutineScope(Job())) {
            File(context.filesDir, "test_${System.nanoTime()}.preferences_pb")
        }
        prefs = NearfolkPreferences(store)
        seeder = CommunitySeeder(db.memberDao(), db.highlightDao(), db.postDao(), db.commentDao(), db.journalDao(), prefs, time)
        val simulator = CommunitySimulator(
            idle, seeder, db.memberDao(), db.conversationDao(), db.messageDao(), db.postDao(), db.commentDao(),
            db.activityDao(), db.safetyDao(), prefs, AppVisibility(), notifier, time,
        )
        comments = LocalCommentRepository(db, simulator, time)
        posts = LocalPostRepository(db, MediaFiles(context, Dispatchers.IO), simulator, time, idle)
        db.memberDao().upsertAll(listOf(member(SELF_ID, "Rae"), member("ana", "Ana"), member("ben", "Ben"), member("kai", "Kai")))
        db.postDao().insertAll(
            listOf(PostEntity(id = "p1", authorId = "ana", caption = "Harbour", media = null, createdAt = 0, likeCount = 0, commentCount = 0, likedByMe = false)),
        )
    }

    @After
    fun tearDown() {
        live.cancel()
        db.close()
    }

    @Test
    fun likeAndDislikeMoveOneScore() = runBlocking {
        db.commentDao().insertAll(listOf(comment("c1", "ana", 1, score = 10)))

        comments.vote("c1", Vote.Up)
        assertEquals(11, one("c1").score)
        assertEquals(Vote.Up, one("c1").myVote)

        // Switching to a dislike takes the like back and one more off.
        comments.vote("c1", Vote.Down)
        assertEquals(9, one("c1").score)
        assertEquals(Vote.Down, one("c1").myVote)

        // The same vote again takes it back.
        comments.vote("c1", Vote.Down)
        assertEquals(10, one("c1").score)
        assertEquals(Vote.None, one("c1").myVote)

        // A score can go below zero.
        db.commentDao().insertAll(listOf(comment("c2", "ben", 2, score = 0)))
        comments.vote("c2", Vote.Down)
        assertEquals(-1, one("c2").score)
    }

    @Test
    fun repliesStayOneLevelDeepAndNameWhoTheyAnswer() = runBlocking {
        db.commentDao().insertAll(listOf(comment("root", "ana", 1)))

        // Answering the top-level comment: under it, no name needed.
        comments.add("p1", "First reply", replyTo = one("root"))
        val first = replies("root").single()
        assertEquals("root", first.parentId)
        assertNull(first.replyToId)

        // Answering someone's reply: same thread, and it names them.
        clock = clock.plusSeconds(60)
        db.commentDao().insertAll(listOf(comment("byKai", "kai", clock.toEpochMilli(), parent = "root")))
        clock = clock.plusSeconds(60)
        comments.add("p1", "Agreed", replyTo = one("byKai"))
        val answer = replies("root").last()
        assertEquals("root", answer.parentId)
        assertEquals("kai", answer.replyToId)
        assertEquals("Kai", answer.replyToName)

        // Answering your own reply names nobody.
        clock = clock.plusSeconds(60)
        comments.add("p1", "One more thing", replyTo = answer)
        val own = replies("root").last()
        assertEquals("root", own.parentId)
        assertNull(own.replyToId)

        // Replies are oldest first and counted on the thread and on the post.
        assertEquals(listOf("First reply", "Text byKai", "Agreed", "One more thing"), replies("root").map { it.text })
        assertEquals(4, one("root").replyCount)
        assertEquals(listOf("root"), top().map { it.id })
        assertEquals(3, db.postDao().get("p1")!!.commentCount)
    }

    @Test
    fun commentsAreTextOnlyAndTrimmed() = runBlocking {
        comments.add("p1", "   ")
        assertTrue(top().isEmpty())
        comments.add("p1", "  Hello there  ")
        val mine = top().single()
        assertEquals("Hello there", mine.text)
        assertEquals(SELF_ID, mine.authorId)
        assertTrue(mine.attachments.isEmpty())
        assertEquals(1, db.postDao().get("p1")!!.commentCount)
    }

    @Test
    fun blockedPeopleDisappearFromThreads() = runBlocking {
        db.commentDao().insertAll(
            listOf(
                comment("a", "ana", 1),
                comment("b", "ben", 2),
                comment("a1", "ben", 3, parent = "a"),
                comment("a2", "kai", 4, parent = "a"),
            ),
        )
        assertEquals(2, one("a").replyCount)
        db.safetyDao().block(BlockEntity("ben", 0))
        assertEquals(listOf("a"), top().map { it.id })
        assertEquals(listOf("a2"), replies("a").map { it.id })
        assertEquals(1, one("a").replyCount)
    }

    @Test
    fun theListKeepsItsOrderWhileOpen() = runBlocking {
        fun c(id: String, score: Int, at: Long) = Comment(id, "p1", "ana", "Ana", null, id, Instant.ofEpochMilli(at), score = score)
        val first = listOf(c("low", 1, 3), c("high", 9, 1), c("mid", 5, 2))
        // A vote lifts "low" above everything, and a new comment arrives.
        val later = listOf(c("low", 20, 3), c("high", 9, 1), c("mid", 5, 2), c("new", 0, 4))
        val emitted = flowOf(first, later).inStableOrder(CommentSort.Top).toList()
        assertEquals(listOf("high", "mid", "low"), emitted[0].map { it.id })
        assertEquals(listOf("new", "high", "mid", "low"), emitted[1].map { it.id })
        assertEquals(20, emitted[1].last().score)

        val newest = flowOf(first).inStableOrder(CommentSort.Newest).toList().single()
        assertEquals(listOf("low", "mid", "high"), newest.map { it.id })
    }

    @Test
    fun threadStateFollowsTheSheet() = runBlocking {
        db.commentDao().insertAll(
            listOf(
                comment("quiet", "ben", 1, score = 1),
                comment("popular", "ana", 2, score = 30),
                comment("r1", "kai", 3, parent = "popular"),
            ),
        )
        val thread: CommentThread = CommentThreadFactory(comments, posts).create(live)
        suspend fun await(check: (CommentsState) -> Boolean) = withTimeout(5_000) { thread.state.first(check) }

        assertTrue(thread.open("p1"))
        var state = await { !it.loading && it.threads.size == 2 }
        assertEquals(listOf("popular", "quiet"), state.threads.map { it.id })
        assertEquals("ana", state.postAuthorId)

        // Opening the replies page of a thread.
        thread.openReplies(state.threads.first())
        state = await { it.openThread?.id == "popular" && it.replies.size == 1 }
        assertEquals("Text r1", state.replies.single().text)

        // Pointing the reply box at Kai's reply, then answering.
        thread.replyTo(state.replies.single())
        state = await { it.replyingTo?.id == "r1" }
        assertTrue(thread.sendReply("Thanks Kai"))
        state = await { it.replies.size == 2 }
        assertEquals("kai", state.replies.last().replyToId)
        assertNull(state.replyingTo)

        // Back on the list, votes do not move a comment, even when it overtakes the top one.
        thread.closeReplies()
        state = await { it.openThread == null }
        thread.vote(state.threads.last(), Vote.Up)
        db.commentDao().addScore("quiet", 100)
        state = await { s -> s.threads.last().score == 102 && s.threads.last().myVote == Vote.Up }
        assertEquals(listOf("popular", "quiet"), state.threads.map { it.id })

        // A new comment goes to the top.
        clock = clock.plusSeconds(60)
        assertTrue(thread.send("Lovely"))
        state = await { it.threads.size == 3 }
        assertEquals("Lovely", state.threads.first().text)

        // Closing and reopening starts on the list again; another post starts fresh drafts.
        thread.openReplies(state.threads[1])
        assertFalse(thread.open("p1"))
        thread.open(null)
        thread.open("p1")
        state = await { !it.loading && it.postId == "p1" }
        assertNull(state.openThread)
    }

    @Test
    fun sampleThreadsAreConsistent() = runBlocking {
        db.clearAllTables()
        seeder.seedIfNeeded()
        val all = db.query("SELECT id, postId, parentId, replyToId FROM comments", null).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(listOf(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3)))
            }
        }
        val byId = all.associateBy { it[0] }
        val replies = all.filter { it[2] != null }
        assertTrue("some threads have replies", replies.isNotEmpty())
        replies.forEach { r ->
            val parent = byId.getValue(r[2]!!)
            assertNull("replies sit under top-level comments only", parent[2])
            assertEquals("same post", parent[1], r[1])
        }
        // Every post counts its comments and replies.
        all.groupBy { it[1] }.forEach { (postId, list) ->
            assertEquals(postId, list.size, db.postDao().get(postId)!!.commentCount)
        }
        // Running it again changes nothing.
        seeder.seedIfNeeded()
        val again = db.query("SELECT COUNT(*) FROM comments", null).use { it.moveToFirst(); it.getInt(0) }
        assertEquals(all.size, again)
    }
}
