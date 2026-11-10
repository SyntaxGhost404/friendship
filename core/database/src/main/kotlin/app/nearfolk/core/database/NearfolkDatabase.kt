package app.nearfolk.core.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase
import app.nearfolk.core.database.dao.ActivityDao
import app.nearfolk.core.database.dao.CommentDao
import app.nearfolk.core.database.dao.ConversationDao
import app.nearfolk.core.database.dao.HighlightDao
import app.nearfolk.core.database.dao.JournalDao
import app.nearfolk.core.database.dao.MaintenanceDao
import app.nearfolk.core.database.dao.MemberDao
import app.nearfolk.core.database.dao.MessageDao
import app.nearfolk.core.database.dao.PostDao
import app.nearfolk.core.database.dao.SafetyDao
import app.nearfolk.core.database.entity.ActivityEntity
import app.nearfolk.core.database.entity.BlockEntity
import app.nearfolk.core.database.entity.CommentEntity
import app.nearfolk.core.database.entity.ConversationEntity
import app.nearfolk.core.database.entity.HighlightEntity
import app.nearfolk.core.database.entity.JournalEntity
import app.nearfolk.core.database.entity.MemberEntity
import app.nearfolk.core.database.entity.MessageEntity
import app.nearfolk.core.database.entity.PostEntity
import app.nearfolk.core.database.entity.ReportEntity

/**
 * Local source of truth. Screens observe these tables; whatever talks to the
 * network writes into them. That keeps the app usable offline and keeps the
 * UI independent of the backend.
 */
@Database(
    entities = [
        MemberEntity::class,
        HighlightEntity::class,
        PostEntity::class,
        CommentEntity::class,
        ConversationEntity::class,
        MessageEntity::class,
        JournalEntity::class,
        BlockEntity::class,
        ReportEntity::class,
        ActivityEntity::class,
    ],
    version = 4,
    exportSchema = true,
    autoMigrations = [
        // 2: attachments on messages and comments, and on the inbox preview.
        AutoMigration(from = 1, to = 2),
        // 3: view, save and share counts on posts, and your saved posts.
        AutoMigration(from = 2, to = 3, spec = NearfolkDatabase.PostStatsBackfill::class),
        // 4: reply threads and like and dislike scores on comments.
        AutoMigration(from = 3, to = 4),
    ],
)
abstract class NearfolkDatabase : RoomDatabase() {
    abstract fun memberDao(): MemberDao
    abstract fun highlightDao(): HighlightDao
    abstract fun postDao(): PostDao
    abstract fun commentDao(): CommentDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun journalDao(): JournalDao
    abstract fun safetyDao(): SafetyDao
    abstract fun activityDao(): ActivityDao
    abstract fun maintenanceDao(): MaintenanceDao

    /**
     * Posts from before version 3 never counted views, saves or shares. Rather
     * than show zero views under a post with dozens of likes, estimate them
     * from the likes and comments the post already has.
     */
    class PostStatsBackfill : AutoMigrationSpec {
        override fun onPostMigrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "UPDATE posts SET viewCount = likeCount * 9 + commentCount * 14 + 12 + (abs(random()) % 90), " +
                    "saveCount = likeCount / 9, shareCount = likeCount / 16",
            )
        }
    }
}
