package app.nearfolk.core.data

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.paging.PagingSource
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.nearfolk.core.common.TimeSource
import app.nearfolk.core.data.community.CommunitySeeder
import app.nearfolk.core.data.community.CommunitySimulator
import app.nearfolk.core.data.local.LocalPostRepository
import app.nearfolk.core.data.local.toModel
import app.nearfolk.core.data.media.MediaFiles
import app.nearfolk.core.data.platform.AppVisibility
import app.nearfolk.core.data.platform.ChatNotifier
import app.nearfolk.core.data.share.PostShareReceiver
import app.nearfolk.core.data.share.PostSharing
import app.nearfolk.core.database.NearfolkDatabase
import app.nearfolk.core.database.entity.BlockEntity
import app.nearfolk.core.database.entity.MemberEntity
import app.nearfolk.core.database.entity.PostEntity
import app.nearfolk.core.datastore.NearfolkPreferences
import app.nearfolk.core.model.Post
import app.nearfolk.core.model.SELF_ID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.Instant
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class PostEngagementTest {

    private lateinit var db: NearfolkDatabase
    private lateinit var repo: LocalPostRepository
    private var clock = Instant.parse("2026-11-10T10:00:00Z")

    private val time = object : TimeSource {
        override fun now(): Instant = clock
        override fun zone(): ZoneId = ZoneId.of("UTC")
    }

    private val notifier = object : ChatNotifier {
        override fun showMessage(conversationId: String, senderName: String, text: String, isRequest: Boolean) = Unit
        override fun showActivity(actorName: String, commentText: String?) = Unit
        override fun cancel(conversationId: String) = Unit
    }

    private fun member(id: String) = MemberEntity(
        id = id, displayName = "Member $id", gender = "Female", birthEpochDay = 9000, countryCode = "NO",
        city = null, catchphrase = "", bio = "", avatar = null, banner = null, streakStartEpochDay = 19000,
        online = false, lastSeenAt = 0, joinedAt = 0, isPlus = false, voicePath = null, voiceDurationMs = null,
        voiceRecordedAt = null, discoverable = true,
    )

    private fun post(id: String, author: String) = PostEntity(
        id = id, authorId = author, caption = "Caption $id", media = null, createdAt = 1_000,
        likeCount = 10, commentCount = 2, likedByMe = false, viewCount = 100, saveCount = 4, shareCount = 1,
    )

    private suspend fun stored(id: String) = db.postDao().get(id)!!

    private suspend fun savedIds(): List<String> {
        val page = db.postDao().pagingSaved(SELF_ID).load(PagingSource.LoadParams.Refresh(null, 50, false))
        return (page as PagingSource.LoadResult.Page).data.map { it.id }
    }

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, NearfolkDatabase::class.java).allowMainThreadQueries().build()
        // Nothing runs in this scope: the simulated community and the timed view batch stay out of these tests.
        val idle = CoroutineScope(Job().apply { cancel() })
        val store = PreferenceDataStoreFactory.create(scope = CoroutineScope(Job())) {
            File(context.filesDir, "test_${System.nanoTime()}.preferences_pb")
        }
        val prefs = NearfolkPreferences(store)
        val seeder = CommunitySeeder(db.memberDao(), db.highlightDao(), db.postDao(), db.commentDao(), db.journalDao(), prefs, time)
        val simulator = CommunitySimulator(
            idle, seeder, db.memberDao(), db.conversationDao(), db.messageDao(), db.postDao(), db.commentDao(),
            db.activityDao(), db.safetyDao(), prefs, AppVisibility(), notifier, time,
        )
        repo = LocalPostRepository(db, MediaFiles(context, kotlinx.coroutines.Dispatchers.IO), simulator, time, idle)
        db.memberDao().upsertAll(listOf(member(SELF_ID), member("ana"), member("ben")))
        db.postDao().insertAll(listOf(post("a1", "ana"), post("a2", "ana"), post("b1", "ben"), post("me1", SELF_ID)))
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun savingCountsOnceAndRemembersWhen() = runBlocking {
        repo.toggleSave("a1")
        val saved = stored("a1")
        assertEquals(5, saved.saveCount)
        assertEquals(clock.toEpochMilli(), saved.savedAt)
        assertTrue(db.postDao().observe("a1").first()!!.toModel().savedByMe)

        repo.toggleSave("a1")
        assertEquals(4, stored("a1").saveCount)
        assertNull(stored("a1").savedAt)
    }

    @Test
    fun savedListIsNewestSavedFirstAndHidesBlockedAuthors() = runBlocking {
        repo.toggleSave("b1")
        clock = clock.plusSeconds(60)
        repo.toggleSave("a2")
        clock = clock.plusSeconds(60)
        repo.toggleSave("me1")
        assertEquals(listOf("me1", "a2", "b1"), savedIds())
        assertEquals(3, repo.savedCount().first())

        db.safetyDao().block(BlockEntity("ana", 0))
        assertEquals(listOf("me1", "b1"), savedIds())
        assertEquals(2, repo.savedCount().first())
    }

    @Test
    fun undoPutsAPostBackWhereItWas() = runBlocking {
        repo.toggleSave("b1")
        val first = stored("b1").savedAt!!
        clock = clock.plusSeconds(60)
        repo.toggleSave("a1")

        repo.setSaved("b1", null)
        assertEquals(listOf("a1"), savedIds())
        assertEquals(4, stored("b1").saveCount)

        repo.setSaved("b1", Instant.ofEpochMilli(first))
        assertEquals(listOf("a1", "b1"), savedIds())
        assertEquals(5, stored("b1").saveCount)

        // Setting the same state twice does not count twice.
        repo.setSaved("b1", Instant.ofEpochMilli(first))
        assertEquals(5, stored("b1").saveCount)
    }

    @Test
    fun aPostCountsOneViewPerSession() = runBlocking {
        repo.recordView("a1")
        repo.recordView("a1")
        repo.recordView("b1")
        repo.flushViews()
        assertEquals(101, stored("a1").viewCount)
        assertEquals(101, stored("b1").viewCount)
        assertEquals(100, stored("a2").viewCount)

        repo.recordView("a1")
        repo.flushViews()
        assertEquals(101, stored("a1").viewCount)
    }

    @Test
    fun sharingCounts() = runBlocking {
        repo.recordShare("a1")
        assertEquals(2, stored("a1").shareCount)
    }

    @Test
    fun shareSheetCarriesThePostAndReportsTheChoice() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val post = Post("a1", "ana", "Ana", null, "Sunrise over the harbour.", null, clock, 1, 0, false)
        val chooser = PostSharing.chooser(context, post)
        assertEquals(Intent.ACTION_CHOOSER, chooser.action)
        @Suppress("DEPRECATION")
        val send = chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
        assertEquals(Intent.ACTION_SEND, send.action)
        val text = send.getStringExtra(Intent.EXTRA_TEXT)!!
        assertTrue(text, text.startsWith("Ana on "))
        assertTrue(text, text.endsWith(": Sunrise over the harbour."))
        // Android 15 and later name the callback differently; either one reports the choice.
        assertTrue(
            chooser.hasExtra(Intent.EXTRA_CHOSEN_COMPONENT_INTENT_SENDER) || chooser.hasExtra(Intent.EXTRA_CHOOSER_RESULT_INTENT_SENDER),
        )
        // The callback for this post exists and goes to the share receiver.
        val callback = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, PostShareReceiver::class.java).setIdentifier("a1"),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_MUTABLE,
        )
        assertNotNull(callback)

        val photo = PostSharing.chooser(context, post.copy(caption = " "))
        @Suppress("DEPRECATION")
        val photoText = photo.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!.getStringExtra(Intent.EXTRA_TEXT)!!
        assertFalse(photoText.contains(":"))
        assertTrue(photoText.startsWith("A photo from Ana on "))
    }
}
