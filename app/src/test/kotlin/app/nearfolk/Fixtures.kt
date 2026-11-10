package app.nearfolk

import app.nearfolk.core.model.Attachment
import app.nearfolk.core.model.AttachmentKind
import app.nearfolk.core.model.Conversation
import app.nearfolk.core.model.DraftAttachment
import app.nearfolk.core.model.ConversationState
import app.nearfolk.core.model.DeliveryStatus
import app.nearfolk.core.model.DiscoverFilter
import app.nearfolk.core.model.Gender
import app.nearfolk.core.model.Highlight
import app.nearfolk.core.model.MediaRef
import app.nearfolk.core.model.Member
import app.nearfolk.core.model.Message
import app.nearfolk.core.model.Post
import app.nearfolk.core.model.Presence
import app.nearfolk.core.model.SELF_ID
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

/** Sample data for screenshot tests. Times are relative to [now] so labels stay meaningful. */
object Fixtures {
    val now: Instant = Instant.now()
    private fun ago(minutes: Long): Instant = now.minus(Duration.ofMinutes(minutes))

    fun member(
        id: String,
        name: String,
        gender: Gender,
        birth: String,
        country: String,
        city: String,
        online: Boolean,
        seenMinutesAgo: Long = 0,
        bio: String,
        status: String = "",
        plus: Boolean = false,
        seed: Long = id.hashCode().toLong(),
    ) = Member(
        id = id,
        displayName = name,
        gender = gender,
        birthDate = LocalDate.parse(birth),
        countryCode = country,
        city = city,
        catchphrase = status,
        bio = bio,
        avatar = MediaRef.Generated(seed),
        banner = MediaRef.Generated(seed * 31 + 7),
        streakStart = LocalDate.now().minusDays(214),
        presence = if (online) Presence.Online else Presence.Offline(ago(seenMinutesAgo)),
        joinedAt = ago(60L * 24 * 300),
        isPlus = plus,
        voiceIntro = null,
    )

    val amara = member("m1", "Amara Okafor", Gender.Female, "1997-08-14", "NG", "Lagos", true, bio = "Product designer by day. Slow cooking, old highlife records and long phone calls by night.", status = "Learning to enjoy my own company", plus = true, seed = 11)
    val soren = member("m2", "Søren Holm", Gender.Male, "1991-01-17", "DK", "Aarhus", false, 42, bio = "Carpenter. I build furniture and sometimes friendships. Winter swimming keeps me honest.", seed = 4)
    val kai = member("m3", "Kai Nakamura", Gender.Nonbinary, "2000-02-27", "JP", "Osaka", true, bio = "I shoot on film and wait weeks to see what I got. Patience is my favourite hobby.", seed = 25)
    val ines = member("m4", "Inês Duarte", Gender.Female, "1998-04-12", "PT", "Lisbon", false, 60L * 26, bio = "Translator working between four languages. Ask me for a word and I will give you three.", seed = 9)
    val theo = member("m5", "Théo Laurent", Gender.Male, "1995-02-14", "FR", "Lyon", false, 60L * 24 * 4, bio = "Pastry cook. Very good at croissants and very bad at texting back.", seed = 2)
    val me = member(SELF_ID, "Rae", Gender.Female, "1996-05-03", "CA", "Toronto", true, bio = "New here.", status = "Trying to say yes more", seed = 77)

    val members = listOf(amara, kai, soren, ines, theo)
    val filter = DiscoverFilter()

    val highlights = (0 until 6).map { Highlight("h$it", amara.id, MediaRef.Generated(100L + it * 13), null, ago(60L * 24 * it)) }

    val posts = listOf(
        Post("p1", amara.id, amara.displayName, amara.avatar, "Found this spot by accident. Coming back next week.", MediaRef.Generated(512), ago(95), 42, 3, true, viewCount = 618, saveCount = 5, shareCount = 2),
        Post("p2", amara.id, amara.displayName, amara.avatar, "Learning to enjoy eating alone at restaurants. It is getting easier.", null, ago(60L * 30), 18, 1, false, viewCount = 240, saveCount = 1, shareCount = 0),
    )

    /** A mixed timeline, newest first, as the feed shows it. */
    val feed = listOf(
        Post("f1", kai.id, kai.displayName, kai.avatar, "Two rolls back from the lab today. Half of them are of the same pigeon and I regret nothing.", MediaRef.Generated(733), ago(12), 9, 2, false, viewCount = 87, saveCount = 1, shareCount = 0),
        Post("f2", ines.id, ines.displayName, ines.avatar, "There is a Portuguese word for missing a place you have never been. I think about it every time it rains.", null, ago(48), 31, 6, true, viewCount = 1_240, saveCount = 12, shareCount = 4, savedAt = ago(20)),
        Post("f3", me.id, me.displayName, me.avatar, "First week in a new city. Found a bakery that opens at six, which is now my whole personality.", MediaRef.Generated(281), ago(60L * 3), 14, 4, false, viewCount = 356, saveCount = 2, shareCount = 1),
        Post("f4", theo.id, theo.displayName, theo.avatar, "Laminated dough at 5am with nobody else around is the closest thing I have to meditation.", null, ago(60L * 9), 57, 8, false, viewCount = 15_320, saveCount = 41, shareCount = 9),
    ) + posts

    /** Your saved list, most recently saved first. */
    val saved = listOf(feed[1].copy(savedAt = ago(20)), feed[3].copy(savedAt = ago(60L * 2), saveCount = 42), posts[0].copy(savedAt = ago(60L * 26), saveCount = 6))

    private fun msg(id: String, mine: Boolean, text: String, minutesAgo: Long, status: DeliveryStatus = DeliveryStatus.Read) =
        Message(id, "c1", if (mine) SELF_ID else amara.id, text, ago(minutesAgo), status)

    val thread = listOf(
        msg("1", false, "Hi! Your profile made me smile. How is your week going?", 60L * 25),
        msg("2", true, "Hey Amara! Slowly getting better. I finally unpacked the last box.", 60L * 25 - 2),
        msg("3", true, "Your highlife records though. Any recommendations?", 60L * 25 - 3),
        msg("4", false, "Oh you asked the right person.", 14),
        msg("5", false, "Start with anything from the seventies. Put it on while you cook.", 13),
        msg("6", true, "Doing that tonight. Thank you!", 2, DeliveryStatus.Delivered),
    )

    val draftAttachments = listOf(
        DraftAttachment.Ready("d1", Attachment("d1", AttachmentKind.Image, "/missing/1.webp", "image/webp", 800, 600)),
        DraftAttachment.Ready("d2", Attachment("d2", AttachmentKind.Gif, "/missing/2.gif", "image/gif", 400, 400)),
        DraftAttachment.Ready("d3", Attachment("d3", AttachmentKind.Video, "/missing/3.mp4", "video/mp4", 1080, 1920, durationMillis = 14_000)),
    )

    val conversations = listOf(
        Conversation("c1", amara, ConversationState.Active, thread.last(), 0, false, ago(60L * 25), peerIsTyping = true),
        Conversation("c2", soren, ConversationState.Active, Message("x", "c2", soren.id, "The lake was 4 degrees this morning. Worth it.", ago(50), DeliveryStatus.Delivered), 2, false, ago(500)),
        Conversation("c3", ines, ConversationState.Active, Message("y", "c3", SELF_ID, "Obrigada! I’ll try that word today.", ago(60L * 28), DeliveryStatus.Read), 0, true, ago(900)),
        Conversation("c4", theo, ConversationState.Active, Message("z", "c4", theo.id, "Sorry for the slow reply, bakery week.", ago(60L * 24 * 3), DeliveryStatus.Delivered), 0, false, ago(9000)),
    )
}
