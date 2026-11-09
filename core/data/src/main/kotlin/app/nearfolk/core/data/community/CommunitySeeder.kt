package app.nearfolk.core.data.community

import app.nearfolk.core.common.TimeSource
import app.nearfolk.core.database.dao.CommentDao
import app.nearfolk.core.database.dao.HighlightDao
import app.nearfolk.core.database.dao.JournalDao
import app.nearfolk.core.database.dao.MemberDao
import app.nearfolk.core.database.dao.PostDao
import app.nearfolk.core.database.entity.CommentEntity
import app.nearfolk.core.database.entity.HighlightEntity
import app.nearfolk.core.database.entity.JournalEntity
import app.nearfolk.core.database.entity.MemberEntity
import app.nearfolk.core.database.entity.PostEntity
import app.nearfolk.core.datastore.NearfolkPreferences
import app.nearfolk.core.model.MediaRef
import app.nearfolk.core.model.Visibility
import java.time.Duration
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Fills an empty database with the sample community. Identifiers are stable,
 * so running it again replaces rows instead of duplicating them.
 */
@Singleton
internal class CommunitySeeder @Inject constructor(
    private val members: MemberDao,
    private val highlights: HighlightDao,
    private val posts: PostDao,
    private val comments: CommentDao,
    private val journal: JournalDao,
    private val prefs: NearfolkPreferences,
    private val time: TimeSource,
) {
    suspend fun seedIfNeeded() {
        if (prefs.seededVersion() >= VERSION && members.count() > 1) return
        seed()
        prefs.setSeededVersion(VERSION)
    }

    private suspend fun seed() {
        val now = time.now().toEpochMilli()
        val today = time.today()
        val day = Duration.ofDays(1).toMillis()
        val memberRows = mutableListOf<MemberEntity>()
        val highlightRows = mutableListOf<HighlightEntity>()
        val postRows = mutableListOf<PostEntity>()
        val commentRows = mutableListOf<CommentEntity>()
        val journalRows = mutableListOf<JournalEntity>()

        SeedContent.people.forEachIndexed { index, person ->
            val rng = Random(index * 7919L + 17)
            val id = memberId(index)
            val online = rng.nextFloat() < 0.4f
            val joinedDaysAgo = 20 + rng.nextInt(700)
            memberRows += MemberEntity(
                id = id,
                displayName = person.name,
                gender = person.gender.name,
                birthEpochDay = LocalDate.parse(person.birth).toEpochDay(),
                countryCode = person.country,
                city = person.city,
                catchphrase = person.catchphrase,
                bio = person.bio,
                avatar = MediaRef.Generated(rng.nextLong()).encode(),
                banner = MediaRef.Generated(rng.nextLong()).encode(),
                streakStartEpochDay = today.minusDays(rng.nextLong(5, 900)).toEpochDay(),
                online = online,
                lastSeenAt = if (online) now else now - rng.nextLong(5 * 60_000L, 3 * day),
                joinedAt = now - joinedDaysAgo * day,
                isPlus = rng.nextFloat() < 0.2f,
                voicePath = null,
                voiceDurationMs = null,
                voiceRecordedAt = null,
                discoverable = true,
            )

            repeat(3 + rng.nextInt(7)) { h ->
                highlightRows += HighlightEntity(
                    id = "$id-h$h",
                    memberId = id,
                    media = MediaRef.Generated(rng.nextLong()).encode(),
                    caption = null,
                    createdAt = now - rng.nextLong(day, joinedDaysAgo * day),
                )
            }

            val postCount = 4 + rng.nextInt(11)
            repeat(postCount) { p ->
                val postId = "$id-p$p"
                val createdAt = now - rng.nextLong(30 * 60_000L, minOf(joinedDaysAgo, 160) * day)
                val commentCount = rng.nextInt(5)
                repeat(commentCount) { c ->
                    val author = rng.nextInt(SeedContent.people.size).let { if (it == index) (it + 1) % SeedContent.people.size else it }
                    commentRows += CommentEntity(
                        id = "$postId-c$c",
                        postId = postId,
                        authorId = memberId(author),
                        text = SeedContent.comments[rng.nextInt(SeedContent.comments.size)],
                        // Never in the future: a recent post only has the minutes since it went up.
                        createdAt = minOf(createdAt + rng.nextLong(60_000L, day), now - 60_000L),
                    )
                }
                postRows += PostEntity(
                    id = postId,
                    authorId = id,
                    caption = SeedContent.captions[rng.nextInt(SeedContent.captions.size)],
                    media = if (rng.nextFloat() < 0.65f) MediaRef.Generated(rng.nextLong()).encode() else null,
                    createdAt = createdAt,
                    likeCount = rng.nextInt(140),
                    commentCount = commentCount,
                    likedByMe = false,
                )
            }

            if (rng.nextFloat() < 0.6f) {
                repeat(1 + rng.nextInt(3)) { j ->
                    val seed = SeedContent.journals[rng.nextInt(SeedContent.journals.size)]
                    val created = now - rng.nextLong(day, 90 * day)
                    journalRows += JournalEntity(
                        id = "$id-j$j",
                        authorId = id,
                        title = seed.title,
                        body = seed.body,
                        mood = seed.mood.name,
                        visibility = Visibility.Public.name,
                        createdAt = created,
                        updatedAt = created,
                    )
                }
            }
        }

        members.upsertAll(memberRows)
        highlights.insertAll(highlightRows)
        posts.insertAll(postRows)
        comments.insertAll(commentRows)
        journal.insertAll(journalRows)
    }

    companion object {
        const val VERSION = 1
        fun memberId(index: Int) = "m%03d".format(index)
    }
}
