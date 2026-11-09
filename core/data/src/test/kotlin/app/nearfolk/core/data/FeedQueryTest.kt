package app.nearfolk.core.data

import android.content.Context
import androidx.paging.PagingSource
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.nearfolk.core.database.NearfolkDatabase
import app.nearfolk.core.database.entity.BlockEntity
import app.nearfolk.core.database.entity.MemberEntity
import app.nearfolk.core.database.entity.PostEntity
import app.nearfolk.core.database.entity.PostRow
import app.nearfolk.core.model.SELF_ID
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FeedQueryTest {

    private lateinit var db: NearfolkDatabase

    private fun member(id: String, name: String, discoverable: Boolean = true) = MemberEntity(
        id = id, displayName = name, gender = "Female", birthEpochDay = 9000, countryCode = "NO",
        city = null, catchphrase = "", bio = "", avatar = null, banner = null, streakStartEpochDay = 19000,
        online = false, lastSeenAt = 0, joinedAt = 0, isPlus = false, voicePath = null, voiceDurationMs = null,
        voiceRecordedAt = null, discoverable = discoverable,
    )

    private fun post(id: String, author: String, at: Long) =
        PostEntity(id = id, authorId = author, caption = id, media = null, createdAt = at, likeCount = 0, commentCount = 0, likedByMe = false)

    private suspend fun feed(): List<PostRow> {
        val result = db.postDao().pagingFeed(SELF_ID).load(PagingSource.LoadParams.Refresh(key = null, loadSize = 100, placeholdersEnabled = false))
        return (result as PagingSource.LoadResult.Page).data
    }

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, NearfolkDatabase::class.java).allowMainThreadQueries().build()
        db.memberDao().upsertAll(
            listOf(
                member(SELF_ID, "Me"),
                member("ana", "Ana"),
                member("ben", "Ben"),
                member("cleo", "Cleo", discoverable = false),
            ),
        )
        db.postDao().insertAll(
            listOf(
                post("ana-old", "ana", 1_000),
                post("ben-new", "ben", 5_000),
                post("me-mid", SELF_ID, 3_000),
                post("cleo-hidden", "cleo", 6_000),
                post("ana-new", "ana", 4_000),
            ),
        )
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun everyoneVisibleNewestFirst() = runBlocking {
        val rows = feed()
        assertEquals(listOf("ben-new", "ana-new", "me-mid", "ana-old"), rows.map { it.id })
        assertEquals("Ben", rows.first().authorName)
    }

    @Test
    fun blockedMembersDropOut() = runBlocking {
        db.safetyDao().block(BlockEntity("ana", 0))
        assertEquals(listOf("ben-new", "me-mid"), feed().map { it.id })
    }

    @Test
    fun ownPostsStayWhileDeactivated() = runBlocking {
        db.memberDao().setDiscoverable(SELF_ID, false)
        assertEquals(listOf("ben-new", "ana-new", "me-mid", "ana-old"), feed().map { it.id })
    }
}
