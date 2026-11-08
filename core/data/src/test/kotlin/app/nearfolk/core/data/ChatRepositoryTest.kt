package app.nearfolk.core.data

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.nearfolk.core.common.TimeSource
import app.nearfolk.core.data.community.CommunitySeeder
import app.nearfolk.core.data.community.CommunitySimulator
import app.nearfolk.core.data.local.LocalChatRepository
import app.nearfolk.core.data.platform.AppVisibility
import app.nearfolk.core.data.platform.ChatNotifier
import app.nearfolk.core.database.NearfolkDatabase
import app.nearfolk.core.database.entity.MemberEntity
import app.nearfolk.core.datastore.NearfolkPreferences
import app.nearfolk.core.model.ConversationState
import app.nearfolk.core.model.ProfileRules
import app.nearfolk.core.model.SendResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.Instant
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class ChatRepositoryTest {

    private lateinit var db: NearfolkDatabase
    private lateinit var repo: LocalChatRepository
    private lateinit var scope: CoroutineScope
    private val peer = "m-test"

    private val time = object : TimeSource {
        override fun now(): Instant = Instant.parse("2026-11-08T10:00:00Z")
        override fun zone(): ZoneId = ZoneId.of("UTC")
    }

    private val notifier = object : ChatNotifier {
        override fun showMessage(conversationId: String, senderName: String, text: String, isRequest: Boolean) = Unit
        override fun showActivity(actorName: String, commentText: String?) = Unit
        override fun cancel(conversationId: String) = Unit
    }

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, NearfolkDatabase::class.java).allowMainThreadQueries().build()
        // A scope that never runs anything: the simulated replies stay out of these tests.
        scope = CoroutineScope(Job().apply { cancel() })
        val store = PreferenceDataStoreFactory.create(scope = CoroutineScope(Job())) {
            File(context.filesDir, "test_${System.nanoTime()}.preferences_pb")
        }
        val prefs = NearfolkPreferences(store)
        val seeder = CommunitySeeder(db.memberDao(), db.highlightDao(), db.postDao(), db.commentDao(), db.journalDao(), prefs, time)
        val simulator = CommunitySimulator(
            scope, seeder, db.memberDao(), db.conversationDao(), db.messageDao(), db.postDao(), db.commentDao(),
            db.activityDao(), db.safetyDao(), prefs, AppVisibility(), notifier, time,
        )
        repo = LocalChatRepository(db, simulator, notifier, time)
        db.memberDao().upsert(
            MemberEntity(
                id = peer, displayName = "Test Peer", gender = "Female", birthEpochDay = 9000, countryCode = "NO",
                city = null, catchphrase = "", bio = "", avatar = null, banner = null, streakStartEpochDay = 19000,
                online = false, lastSeenAt = 0, joinedAt = 0, isPlus = false, voicePath = null, voiceDurationMs = null,
                voiceRecordedAt = null, discoverable = true,
            ),
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun firstContactIsCappedUntilTheOtherPersonReplies() = runBlocking {
        val id = repo.conversationWith(peer)
        repeat(ProfileRules.FIRST_CONTACT_LIMIT) { assertEquals(SendResult.Sent, repo.send(id, "hello $it")) }
        assertEquals(SendResult.WaitForReply(ProfileRules.FIRST_CONTACT_LIMIT), repo.send(id, "one more"))
        assertEquals(0, repo.firstContactAllowance(id).first())
    }

    @Test
    fun blankMessagesAreIgnored() = runBlocking {
        val id = repo.conversationWith(peer)
        assertEquals(SendResult.Empty, repo.send(id, "   "))
        assertTrue(repo.messages(id).first().isEmpty())
    }

    @Test
    fun blockingArchivesTheThreadAndStopsSending() = runBlocking {
        val id = repo.conversationWith(peer)
        repo.send(id, "hi")
        repo.block(peer)
        assertEquals(ConversationState.Archived, repo.conversation(id).first()?.state)
        assertEquals(SendResult.Blocked, repo.send(id, "still there?"))
        repo.unblock(peer)
        assertEquals(SendResult.Sent, repo.send(id, "sorry"))
    }

    @Test
    fun deletingRemovesTheThread() = runBlocking {
        val id = repo.conversationWith(peer)
        repo.send(id, "hi")
        repo.delete(id)
        assertEquals(null, repo.conversation(id).first())
        assertTrue(repo.messages(id).first().isEmpty())
    }

    @Test
    fun conversationWithIsStable() = runBlocking {
        assertEquals(repo.conversationWith(peer), repo.conversationWith(peer))
    }
}
