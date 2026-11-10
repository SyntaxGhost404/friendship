package app.nearfolk.core.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.nearfolk.core.data.local.toModel
import app.nearfolk.core.database.NearfolkDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Builds old databases from their exported schemas, as earlier releases
 * created them on devices, then opens them with the current code.
 */
@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {

    @Test
    fun version1UpgradesWithoutLosingMessages() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration_${System.nanoTime()}.db"
        val file = context.getDatabasePath(name).apply { parentFile?.mkdirs() }

        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            createSchema(db, 1)
            db.execSQL(
                "INSERT INTO conversations (id, peerId, state, createdAt, lastActivityAt, unreadCount, muted, peerHasReplied, " +
                    "lastMessageId, lastMessageText, lastMessageSenderId, lastMessageAt, lastMessageStatus) " +
                    "VALUES ('c1', 'm1', 'Active', 1, 2, 0, 0, 1, 'msg1', 'hello', 'self', 2, 'Read')",
            )
            db.execSQL("INSERT INTO messages (id, conversationId, senderId, text, sentAt, status) VALUES ('msg1', 'c1', 'self', 'hello', 2, 'Read')")
            db.execSQL("INSERT INTO comments (id, postId, authorId, text, createdAt) VALUES ('k1', 'p1', 'self', 'nice', 3)")
            db.version = 1
        }

        val room = Room.databaseBuilder(context, NearfolkDatabase::class.java, name).allowMainThreadQueries().build()
        try {
            val messages = room.messageDao().observe("c1").first().map { it.toModel() }
            assertEquals(1, messages.size)
            assertEquals("hello", messages.single().text)
            assertTrue(messages.single().attachments.isEmpty())
            val convo = room.conversationDao().get("c1")!!
            assertEquals("[]", convo.lastMessageAttachments)
            assertEquals("[]", room.commentDao().forPost("p1").single().attachments)
            assertEquals(3, room.openHelper.readableDatabase.version)
        } finally {
            room.close()
            context.deleteDatabase(name)
        }
    }

    /** Creates the tables exactly as the exported schema of [version] describes them. */
    private fun createSchema(db: SQLiteDatabase, version: Int) {
        val schema = JSONObject(File("../database/schemas/app.nearfolk.core.database.NearfolkDatabase/$version.json").readText())
            .getJSONObject("database")
        val entities = schema.getJSONArray("entities")
        for (i in 0 until entities.length()) {
            val entity = entities.getJSONObject(i)
            val table = entity.getString("tableName")
            db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
            entity.optJSONArray("indices")?.let { indices ->
                for (j in 0 until indices.length()) {
                    db.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
                }
            }
        }
        val setup = schema.getJSONArray("setupQueries")
        for (i in 0 until setup.length()) db.execSQL(setup.getString(i))
        db.version = version
    }

    @Test
    fun version2PostsGetEstimatedCounters() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration_${System.nanoTime()}.db"
        val file = context.getDatabasePath(name).apply { parentFile?.mkdirs() }
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            createSchema(db, 2)
            db.execSQL(
                "INSERT INTO posts (id, authorId, caption, media, createdAt, likeCount, commentCount, likedByMe) " +
                    "VALUES ('p1', 'm1', 'hello', NULL, 1, 18, 2, 1)",
            )
        }

        val room = Room.databaseBuilder(context, NearfolkDatabase::class.java, name).allowMainThreadQueries().build()
        try {
            val post = room.postDao().get("p1")!!
            assertEquals("hello", post.caption)
            assertEquals(18, post.likeCount)
            assertTrue(post.likedByMe)
            // 18 likes * 9 + 2 comments * 14 + 12, plus up to 89 more.
            assertTrue("views ${post.viewCount}", post.viewCount in 202..291)
            assertEquals(2, post.saveCount)
            assertEquals(1, post.shareCount)
            assertEquals(null, post.savedAt)
        } finally {
            room.close()
            context.deleteDatabase(name)
        }
    }
}
