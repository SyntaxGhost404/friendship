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
import app.nearfolk.core.model.SELF_ID
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
    /**
     * Seeds in steps, so an existing install only gets what it is missing:
     * 1 is the community itself, 2 adds reply threads and comment scores.
     */
    suspend fun seedIfNeeded() {
        val fresh = prefs.seededVersion() < 1 || members.count() <= 1
        if (!fresh && prefs.seededVersion() >= VERSION) return
        if (fresh) seed()
        seedThreads()
        prefs.setSeededVersion(VERSION)
    }

    /**
     * Gives the sample comments a score and some of them a thread of replies.
     * Reply ids are derived from the comment, so running it again replaces
     * rows instead of adding more.
     */
    private suspend fun seedThreads() {
        val now = time.now().toEpochMilli()
        val roots = comments.topLevelByOthers(SELF_ID).filter { SEEDED_COMMENT.matches(it.id) }
        val replies = mutableListOf<CommentEntity>()
        val addedPerPost = mutableMapOf<String, Int>()
        roots.forEach { root ->
            val rng = Random(root.id.hashCode().toLong())
            if (rng.nextFloat() >= 0.4f) return@forEach
            val count = 1 + rng.nextInt(4)
            var at = root.createdAt
            var previous = root.authorId
            repeat(count) { k ->
                val pick = rng.nextInt(SeedContent.people.size)
                val author = memberId(pick).takeIf { it != previous } ?: memberId((pick + 1) % SeedContent.people.size)
                at = minOf(at + rng.nextLong(2 * 60_000L, 6 * 60 * 60_000L), now - 60_000L)
                // Some replies answer the reply before them and name its author.
                val mention = if (k > 0 && rng.nextBoolean() && previous != author) previous else null
                replies += CommentEntity(
                    id = "${root.id}-r$k",
                    postId = root.postId,
                    authorId = author,
                    text = SeedContent.replies[rng.nextInt(SeedContent.replies.size)],
                    createdAt = maxOf(at, root.createdAt + 1),
                    parentId = root.id,
                    replyToId = mention,
                    score = rng.nextInt(7),
                )
                previous = author
            }
            addedPerPost[root.postId] = (addedPerPost[root.postId] ?: 0) + count
        }
        comments.insertAll(replies)
        addedPerPost.forEach { (postId, count) -> posts.addComments(postId, count) }
        comments.seedScores(SELF_ID)
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
                        // Spread over the time since the post went up, at most a day, never in the future.
                        createdAt = createdAt + rng.nextLong(60_000L, maxOf(120_000L, minOf(day, now - createdAt - 60_000L))),
                    )
                }
                val likes = rng.nextInt(140)
                postRows += PostEntity(
                    id = postId,
                    authorId = id,
                    caption = SeedContent.captions[rng.nextInt(SeedContent.captions.size)],
                    media = if (rng.nextFloat() < 0.65f) MediaRef.Generated(rng.nextLong()).encode() else null,
                    createdAt = createdAt,
                    likeCount = likes,
                    commentCount = commentCount,
                    likedByMe = false,
                    viewCount = likes * (6 + rng.nextInt(10)) + commentCount * 14 + 10 + rng.nextInt(80),
                    saveCount = likes / (6 + rng.nextInt(8)),
                    shareCount = likes / (12 + rng.nextInt(16)),
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
        const val VERSION = 2
        private val SEEDED_COMMENT = Regex("m\\d{3}-p\\d+-c\\d+")
        fun memberId(index: Int) = "m%03d".format(index)
    }
}
