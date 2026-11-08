package app.nearfolk.core.data.repository

import androidx.paging.PagingData
import app.nearfolk.core.model.AccountState
import app.nearfolk.core.model.ActivityItem
import app.nearfolk.core.model.Comment
import app.nearfolk.core.model.Conversation
import app.nearfolk.core.model.ConversationState
import app.nearfolk.core.model.DeletionReason
import app.nearfolk.core.model.DiscoverFilter
import app.nearfolk.core.model.Highlight
import app.nearfolk.core.model.IdentityUpdate
import app.nearfolk.core.model.JournalEntry
import app.nearfolk.core.model.Member
import app.nearfolk.core.model.Message
import app.nearfolk.core.model.Mood
import app.nearfolk.core.model.PlusPlan
import app.nearfolk.core.model.PlusStatus
import app.nearfolk.core.model.Post
import app.nearfolk.core.model.ProfileDraft
import app.nearfolk.core.model.ReportReason
import app.nearfolk.core.model.SendResult
import app.nearfolk.core.model.ThemeMode
import app.nearfolk.core.model.UserPreferences
import app.nearfolk.core.model.Visibility
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/*
 * Repository contracts. Screens depend only on these interfaces. The current
 * implementations keep everything on device and simulate the rest of the
 * community; a networked implementation can replace them without touching
 * any screen.
 */

interface SessionRepository {
    val accountState: Flow<AccountState>

    /** Name of the profile stored on this device, if any, for the welcome screen. */
    val storedProfileName: Flow<String?>
    suspend fun createProfile(draft: ProfileDraft)
    suspend fun continueStoredProfile()
    suspend fun signOut()
    suspend fun deactivate()
    suspend fun reactivate()
    suspend fun deleteAccount(reason: DeletionReason, details: String?)
}

interface PreferencesRepository {
    val preferences: Flow<UserPreferences>
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setPureBlack(enabled: Boolean)
    suspend fun setNotifyMessages(enabled: Boolean)
    suspend fun setNotifyActivity(enabled: Boolean)
    suspend fun setHaptics(enabled: Boolean)
    suspend fun setDiscoverFilter(filter: DiscoverFilter)
    suspend fun dismissSafetyBanner(conversationId: String)
    suspend fun markNotificationPromptShown()
}

interface MemberRepository {
    /** Everyone visible on Discover, minus you and anyone you blocked. Sorted online first. */
    fun discover(filter: DiscoverFilter): Flow<List<Member>>
    fun member(id: String): Flow<Member?>
    fun highlights(memberId: String): Flow<List<Highlight>>
    fun blockedMembers(): Flow<List<Member>>
    fun isBlocked(memberId: String): Flow<Boolean>
    fun publicJournalCount(memberId: String): Flow<Int>
    suspend fun refresh()
}

sealed interface AddHighlightResult {
    data object Added : AddHighlightResult
    data class LimitReached(val limit: Int) : AddHighlightResult
}

interface ProfileRepository {
    val me: Flow<Member?>
    suspend fun updateIdentity(update: IdentityUpdate)
    suspend fun setAvatar(path: String?)
    suspend fun setBanner(path: String?)
    suspend fun addHighlight(path: String, caption: String?): AddHighlightResult
    suspend fun removeHighlight(id: String)
    suspend fun setVoiceIntro(path: String, durationMillis: Long)
    suspend fun deleteVoiceIntro()
}

data class PostStats(val posts: Int, val likes: Int, val highlights: Int)

interface PostRepository {
    fun posts(authorId: String): Flow<PagingData<Post>>
    fun allPosts(authorId: String): Flow<List<Post>>
    fun post(id: String): Flow<Post?>
    fun comments(postId: String): Flow<List<Comment>>
    fun stats(authorId: String): Flow<PostStats>
    suspend fun toggleLike(postId: String)
    suspend fun addComment(postId: String, text: String)
    suspend fun createPost(caption: String, mediaPath: String?)
    suspend fun deletePost(id: String)
    fun activity(): Flow<List<ActivityItem>>
    fun unseenActivityCount(): Flow<Int>
    suspend fun markActivitySeen()
}

interface ChatRepository {
    fun conversations(state: ConversationState): Flow<List<Conversation>>
    fun conversation(id: String): Flow<Conversation?>
    fun messages(conversationId: String): Flow<List<Message>>
    fun unreadTotal(): Flow<Int>
    fun requestCount(): Flow<Int>

    /** Returns the id of the thread with [memberId], creating an empty one when needed. */
    suspend fun conversationWith(memberId: String): String
    suspend fun send(conversationId: String, text: String): SendResult
    suspend fun markRead(conversationId: String)
    suspend fun acceptRequest(conversationId: String)
    suspend fun setMuted(conversationId: String, muted: Boolean)
    suspend fun delete(conversationId: String)

    /** Blocks the member and archives any thread with them. */
    suspend fun block(memberId: String)
    suspend fun unblock(memberId: String)
    suspend fun report(memberId: String, reason: ReportReason, details: String?, conversationId: String?)

    /** Remaining messages you can send before the other person replies, or null when uncapped. */
    fun firstContactAllowance(conversationId: String): Flow<Int?>
}

interface JournalRepository {
    fun myEntries(): Flow<List<JournalEntry>>
    fun publicEntries(memberId: String): Flow<List<JournalEntry>>
    fun entry(id: String): Flow<JournalEntry?>
    suspend fun save(id: String?, title: String, body: String, mood: Mood, visibility: Visibility): String
    suspend fun delete(id: String)
}

sealed interface PasscodeAttempt {
    data object Success : PasscodeAttempt
    data class Wrong(val attemptsBeforeWait: Int) : PasscodeAttempt
    data class Locked(val untilEpochMillis: Long) : PasscodeAttempt
}

interface PasscodeRepository {
    val isSet: Flow<Boolean>
    val unlocked: StateFlow<Boolean>
    val lockedUntil: Flow<Long>
    suspend fun set(code: String)
    suspend fun clear()
    suspend fun attempt(code: String): PasscodeAttempt
    fun lock()
}

sealed interface PurchaseResult {
    data object Success : PurchaseResult
    data object Cancelled : PurchaseResult
    data class Failed(val message: String?) : PurchaseResult
}

interface PlusRepository {
    val status: Flow<PlusStatus>

    /** False until a real store connection is configured. */
    val isStoreConnected: Boolean
    suspend fun purchase(plan: PlusPlan): PurchaseResult
    suspend fun cancel()
}
