package app.nearfolk.core.data.local

import app.nearfolk.core.common.TimeSource
import app.nearfolk.core.data.community.CommunitySimulator
import app.nearfolk.core.data.media.MediaFiles
import app.nearfolk.core.data.media.VoicePlayer
import app.nearfolk.core.data.repository.PasscodeRepository
import app.nearfolk.core.data.repository.SessionRepository
import app.nearfolk.core.database.NearfolkDatabase
import app.nearfolk.core.database.entity.MemberEntity
import app.nearfolk.core.datastore.NearfolkPreferences
import app.nearfolk.core.model.AccountState
import app.nearfolk.core.model.DeletionReason
import app.nearfolk.core.model.ProfileDraft
import app.nearfolk.core.model.SELF_ID
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class LocalSessionRepository @Inject constructor(
    private val db: NearfolkDatabase,
    private val prefs: NearfolkPreferences,
    private val media: MediaFiles,
    private val player: VoicePlayer,
    private val passcode: PasscodeRepository,
    private val simulator: CommunitySimulator,
    private val time: TimeSource,
) : SessionRepository {

    override val accountState: Flow<AccountState> = prefs.accountState

    override val storedProfileName: Flow<String?> =
        db.memberDao().observe(SELF_ID).map { it?.displayName }

    override suspend fun createProfile(draft: ProfileDraft) {
        val now = time.now().toEpochMilli()
        db.memberDao().upsert(
            MemberEntity(
                id = SELF_ID,
                displayName = draft.displayName.trim(),
                gender = draft.gender.name,
                birthEpochDay = draft.birthDate.toEpochDay(),
                countryCode = draft.countryCode,
                city = null,
                catchphrase = "",
                bio = "",
                avatar = null,
                banner = null,
                streakStartEpochDay = time.today().toEpochDay(),
                online = true,
                lastSeenAt = now,
                joinedAt = now,
                isPlus = false,
                voicePath = null,
                voiceDurationMs = null,
                voiceRecordedAt = null,
                discoverable = true,
            ),
        )
        prefs.setAccountState(AccountState.Active)
    }

    override suspend fun continueStoredProfile() {
        val me = db.memberDao().get(SELF_ID) ?: return
        prefs.setAccountState(if (me.discoverable) AccountState.Active else AccountState.Deactivated)
    }

    override suspend fun signOut() {
        player.stop()
        passcode.lock()
        simulator.resetSession()
        prefs.setAccountState(AccountState.SignedOut)
    }

    override suspend fun deactivate() {
        db.memberDao().setDiscoverable(SELF_ID, false)
        prefs.setAccountState(AccountState.Deactivated)
    }

    override suspend fun reactivate() {
        db.memberDao().setDiscoverable(SELF_ID, true)
        prefs.setAccountState(AccountState.Active)
    }

    /**
     * Removes the profile and everything it created: posts, comments, likes,
     * journal, conversations, safety records, media files and settings.
     * The reason is not stored on device; a backend would receive it.
     */
    @Suppress("UNUSED_PARAMETER")
    override suspend fun deleteAccount(reason: DeletionReason, details: String?) {
        player.stop()
        passcode.lock()
        simulator.resetSession()
        db.withTransaction {
            db.openHelper.writableDatabase.apply {
                execSQL("DELETE FROM messages")
                execSQL("DELETE FROM conversations")
                execSQL("DELETE FROM activity")
                execSQL("DELETE FROM blocks")
                execSQL("DELETE FROM reports")
                execSQL("DELETE FROM comments WHERE postId IN (SELECT id FROM posts WHERE authorId = '$SELF_ID')")
                execSQL("UPDATE posts SET commentCount = commentCount - (SELECT COUNT(*) FROM comments c WHERE c.postId = posts.id AND c.authorId = '$SELF_ID') WHERE id IN (SELECT postId FROM comments WHERE authorId = '$SELF_ID')")
                execSQL("DELETE FROM comments WHERE authorId = '$SELF_ID'")
                execSQL("UPDATE posts SET likeCount = likeCount - 1, likedByMe = 0 WHERE likedByMe = 1")
                execSQL("DELETE FROM posts WHERE authorId = '$SELF_ID'")
                execSQL("DELETE FROM highlights WHERE memberId = '$SELF_ID'")
                execSQL("DELETE FROM journal WHERE authorId = '$SELF_ID'")
                execSQL("DELETE FROM members WHERE id = '$SELF_ID'")
            }
        }
        media.purgeAll()
        val seeded = prefs.seededVersion()
        prefs.clearAll()
        prefs.setSeededVersion(seeded)
        prefs.setAccountState(AccountState.SignedOut)
    }
}
