package app.nearfolk.feature.you

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.core.graphics.scale
import androidx.core.net.toUri
import androidx.navigation.toRoute
import app.nearfolk.core.common.TimeSource
import app.nearfolk.core.data.media.MediaFiles
import app.nearfolk.core.data.media.MediaKind
import app.nearfolk.core.data.media.PlaybackState
import app.nearfolk.core.data.media.VoicePlayer
import app.nearfolk.core.data.media.VoiceRecorder
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import app.nearfolk.core.data.repository.AddHighlightResult
import app.nearfolk.core.data.repository.CommentThreadFactory
import app.nearfolk.core.data.repository.MemberRepository
import app.nearfolk.core.data.repository.PasscodeRepository
import app.nearfolk.core.data.repository.PlusRepository
import app.nearfolk.core.data.repository.PostRepository
import app.nearfolk.core.data.repository.PostStats
import app.nearfolk.core.data.repository.ProfileRepository
import app.nearfolk.core.model.ActivityItem
import app.nearfolk.core.model.Comment
import app.nearfolk.core.model.Gender
import app.nearfolk.core.model.Highlight
import app.nearfolk.core.model.IdentityUpdate
import app.nearfolk.core.model.Member
import app.nearfolk.core.model.PlusLimits
import app.nearfolk.core.model.Post
import app.nearfolk.core.model.ProfileRules
import app.nearfolk.core.model.SELF_ID
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.Period
import javax.inject.Inject
import kotlin.math.roundToInt
import app.nearfolk.core.model.CommentSort
import app.nearfolk.core.model.CommentsState
import app.nearfolk.core.model.Vote
import app.nearfolk.core.ui.CommentsHost

data class YouUiState(
    val me: Member? = null,
    val stats: PostStats = PostStats(0, 0, 0),
    val unseenActivity: Int = 0,
    val isPlus: Boolean = false,
    val passcodeSet: Boolean = false,
    val savedCount: Int = 0,
)

@HiltViewModel
class YouViewModel @Inject constructor(
    profile: ProfileRepository,
    posts: PostRepository,
    plus: PlusRepository,
    passcode: PasscodeRepository,
) : ViewModel() {
    val state: StateFlow<YouUiState> = combine(
        combine(profile.me, posts.stats(SELF_ID), posts.unseenActivityCount(), ::Triple),
        plus.status,
        passcode.isSet,
        posts.savedCount(),
    ) { (me, stats, unseen), plusStatus, passcodeSet, saved ->
        YouUiState(me, stats, unseen, plusStatus.active, passcodeSet, saved)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), YouUiState())
}

data class EditDraft(
    val loaded: Boolean = false,
    val name: String = "",
    val status: String = "",
    val bio: String = "",
    val country: String = "",
    val city: String = "",
    val gender: Gender = Gender.Nonbinary,
    val birth: LocalDate = LocalDate.of(2000, 1, 1),
    val streakStart: LocalDate = LocalDate.now(),
) {
    val nameValid get() = name.trim().length in ProfileRules.NAME_MIN..ProfileRules.NAME_MAX
}

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val profile: ProfileRepository,
    private val time: TimeSource,
) : ViewModel() {
    val me: StateFlow<Member?> = profile.me.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val _draft = MutableStateFlow(EditDraft())
    val draft: StateFlow<EditDraft> = _draft
    private var original: EditDraft? = null
    private val _saved = Channel<Unit>(Channel.CONFLATED)
    val saved = _saved.receiveAsFlow()

    val today: LocalDate get() = time.today()

    init {
        viewModelScope.launch {
            val m = profile.me.first { it != null } ?: return@launch
            val d = EditDraft(true, m.displayName, m.catchphrase, m.bio, m.countryCode, m.city.orEmpty(), m.gender, m.birthDate, m.streakStart)
            original = d
            _draft.value = d
        }
    }

    fun update(transform: (EditDraft) -> EditDraft) = _draft.update(transform)

    val hasChanges: Boolean get() = original != null && original != _draft.value

    fun isOldEnough(date: LocalDate) = Period.between(date, today).years >= ProfileRules.MIN_AGE

    fun canSave(d: EditDraft) = d.loaded && d.nameValid && isOldEnough(d.birth) && !d.streakStart.isAfter(today)

    fun save() {
        val d = _draft.value
        if (!canSave(d)) return
        viewModelScope.launch {
            profile.updateIdentity(IdentityUpdate(d.name, d.status, d.bio, d.country, d.city, d.gender, d.birth, d.streakStart))
            original = d
            _saved.send(Unit)
        }
    }

    fun removeAvatar() = viewModelScope.launch { profile.setAvatar(null) }
    fun removeBanner() = viewModelScope.launch { profile.setBanner(null) }
}

enum class CropTarget(val aspect: Float, val outWidth: Int, val outHeight: Int) {
    Avatar(1f, 720, 720),
    Banner(3f, 1500, 500),
}

@HiltViewModel
class CropViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val media: MediaFiles,
    private val profile: ProfileRepository,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<CropRoute>()
    val target: CropTarget = CropTarget.valueOf(route.target)

    sealed interface Load {
        data object Loading : Load
        data class Ready(val bitmap: Bitmap) : Load
        data object Failed : Load
    }

    private val _load = MutableStateFlow<Load>(Load.Loading)
    val load: StateFlow<Load> = _load
    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving
    private val _done = Channel<Unit>(Channel.CONFLATED)
    val done = _done.receiveAsFlow()

    init {
        viewModelScope.launch {
            _load.value = runCatching { media.decode(route.uri.toUri(), 2400) }
                .fold(onSuccess = { Load.Ready(it) }, onFailure = { Load.Failed })
        }
    }

    /** [left], [top], [size] describe the crop window in source bitmap pixels. */
    fun save(left: Float, top: Float, width: Float, height: Float) {
        val ready = _load.value as? Load.Ready ?: return
        if (_saving.value) return
        _saving.value = true
        viewModelScope.launch {
            val path = withContext(Dispatchers.Default) {
                val src = ready.bitmap
                val l = left.roundToInt().coerceIn(0, src.width - 1)
                val t = top.roundToInt().coerceIn(0, src.height - 1)
                val w = width.roundToInt().coerceIn(1, src.width - l)
                val h = height.roundToInt().coerceIn(1, src.height - t)
                val cropped = Bitmap.createBitmap(src, l, t, w, h)
                val scaled = cropped.scale(target.outWidth, target.outHeight)
                media.save(scaled, if (target == CropTarget.Avatar) MediaKind.Avatar else MediaKind.Banner)
            }
            if (target == CropTarget.Avatar) profile.setAvatar(path) else profile.setBanner(path)
            _done.send(Unit)
        }
    }
}

sealed interface VoiceStage {
    data object Idle : VoiceStage
    data class Recording(val elapsedMillis: Long, val levels: List<Float>) : VoiceStage
    data class Review(val path: String, val durationMillis: Long, val levels: List<Float>) : VoiceStage
}

@HiltViewModel
class VoiceIntroViewModel @Inject constructor(
    private val recorder: VoiceRecorder,
    private val player: VoicePlayer,
    private val profile: ProfileRepository,
    private val media: MediaFiles,
    plus: PlusRepository,
) : ViewModel() {
    val me: StateFlow<Member?> = profile.me.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val isPlus: StateFlow<Boolean> = plus.status.map { it.active }.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val playback: StateFlow<PlaybackState> = player.state
    private val _stage = MutableStateFlow<VoiceStage>(VoiceStage.Idle)
    val stage: StateFlow<VoiceStage> = _stage

    enum class Problem { TooShort, Failed }

    private val _problem = MutableStateFlow<Problem?>(null)
    val problem: StateFlow<Problem?> = _problem
    private var ticker: Job? = null

    val limitMillis: Long get() = PlusLimits.voiceSeconds(isPlus.value) * 1000L

    fun start() {
        player.stop()
        discardReview()
        _problem.value = null
        recorder.start().onFailure {
            _problem.value = Problem.Failed
            return
        }
        val levels = mutableListOf<Float>()
        ticker = viewModelScope.launch {
            while (isActive) {
                val elapsed = recorder.elapsedMillis()
                levels += recorder.level()
                _stage.value = VoiceStage.Recording(elapsed, levels.takeLast(48))
                if (elapsed >= limitMillis) {
                    stop()
                    break
                }
                delay(80)
            }
        }
    }

    fun stop() {
        ticker?.cancel()
        val levels = (_stage.value as? VoiceStage.Recording)?.levels.orEmpty()
        recorder.stop()
            .onSuccess { (file, duration) ->
                if (duration < 2_000) {
                    file.delete()
                    _problem.value = Problem.TooShort
                    _stage.value = VoiceStage.Idle
                } else {
                    _stage.value = VoiceStage.Review(file.absolutePath, duration, levels)
                }
            }
            .onFailure {
                _problem.value = Problem.TooShort
                _stage.value = VoiceStage.Idle
            }
    }

    fun toggle(path: String) = player.toggle(path)

    fun save() {
        val review = _stage.value as? VoiceStage.Review ?: return
        player.stop()
        viewModelScope.launch {
            profile.setVoiceIntro(review.path, review.durationMillis)
            _stage.value = VoiceStage.Idle
        }
    }

    fun discardReview() {
        val review = _stage.value as? VoiceStage.Review ?: return
        player.stop()
        viewModelScope.launch { media.delete(review.path) }
        _stage.value = VoiceStage.Idle
    }

    fun delete() {
        player.stop()
        viewModelScope.launch { profile.deleteVoiceIntro() }
    }

    override fun onCleared() {
        ticker?.cancel()
        if (recorder.isRecording) recorder.cancel()
        player.stop()
        (_stage.value as? VoiceStage.Review)?.let { java.io.File(it.path).delete() }
    }
}

@HiltViewModel
class MyPostsViewModel @Inject constructor(
    private val repository: PostRepository,
    threads: CommentThreadFactory,
) : ViewModel(), CommentsHost {
    val posts: StateFlow<List<Post>?> = repository.allPosts(SELF_ID).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    fun delete(id: String) = viewModelScope.launch { repository.deletePost(id) }
    fun toggleLike(id: String) = viewModelScope.launch { repository.toggleLike(id) }
    fun toggleSave(id: String) = viewModelScope.launch { repository.toggleSave(id) }
    fun recordView(id: String) = repository.recordView(id)

    private val thread = threads.create(viewModelScope)
    val commentsState: StateFlow<CommentsState> = thread.state
    override val commentText = TextFieldState()
    override val replyText = TextFieldState()

    override fun openComments(postId: String?) {
        if (thread.open(postId)) {
            commentText.clearText()
            replyText.clearText()
        }
    }

    override fun sortComments(sort: CommentSort) = thread.setSort(sort)

    override fun openReplies(comment: Comment) {
        if (thread.openReplies(comment)) replyText.clearText()
    }

    override fun closeReplies() = thread.closeReplies()

    override fun replyTo(comment: Comment) {
        if (thread.replyTo(comment)) replyText.clearText()
    }

    override fun clearReplyTarget() = thread.clearReplyTarget()

    override fun voteComment(comment: Comment, vote: Vote) = thread.vote(comment, vote)

    override fun sendComment() {
        if (thread.send(commentText.text.toString())) commentText.clearText()
    }

    override fun sendReply() {
        if (thread.sendReply(replyText.text.toString())) replyText.clearText()
    }
}

@HiltViewModel
class ComposePostViewModel @Inject constructor(
    private val posts: PostRepository,
    private val media: MediaFiles,
) : ViewModel() {
    private val _caption = MutableStateFlow("")
    val caption: StateFlow<String> = _caption
    private val _photo = MutableStateFlow<Uri?>(null)
    val photo: StateFlow<Uri?> = _photo
    private val _posting = MutableStateFlow(false)
    val posting: StateFlow<Boolean> = _posting
    private val _done = Channel<Unit>(Channel.CONFLATED)
    val done = _done.receiveAsFlow()

    fun setCaption(value: String) {
        _caption.value = value.take(ProfileRules.CAPTION_MAX)
    }

    fun setPhoto(uri: Uri?) {
        _photo.value = uri
    }

    fun post() {
        if (_posting.value || (_caption.value.isBlank() && _photo.value == null)) return
        _posting.value = true
        viewModelScope.launch {
            val path = _photo.value?.let { runCatching { media.importImage(it, MediaKind.Post) }.getOrNull() }
            posts.createPost(_caption.value, path)
            _done.send(Unit)
        }
    }
}

@HiltViewModel
class HighlightsViewModel @Inject constructor(
    members: MemberRepository,
    private val profile: ProfileRepository,
    private val media: MediaFiles,
    plus: PlusRepository,
) : ViewModel() {
    val highlights: StateFlow<List<Highlight>?> = members.highlights(SELF_ID).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val limit: StateFlow<Int> = plus.status.map { PlusLimits.highlights(it.active) }.stateIn(viewModelScope, SharingStarted.Eagerly, PlusLimits.FREE_HIGHLIGHTS)
    private val _adding = MutableStateFlow(false)
    val adding: StateFlow<Boolean> = _adding
    private val _limitHit = Channel<Int>(Channel.CONFLATED)
    val limitHit = _limitHit.receiveAsFlow()

    fun add(uris: List<Uri>) {
        if (uris.isEmpty()) return
        _adding.value = true
        viewModelScope.launch {
            for (uri in uris) {
                val path = runCatching { media.importImage(uri, MediaKind.Highlight) }.getOrNull() ?: continue
                val result = profile.addHighlight(path, null)
                if (result is AddHighlightResult.LimitReached) {
                    _limitHit.send(result.limit)
                    break
                }
            }
            _adding.value = false
        }
    }

    fun remove(id: String) = viewModelScope.launch { profile.removeHighlight(id) }
}

@HiltViewModel
class ActivityViewModel @Inject constructor(private val posts: PostRepository) : ViewModel() {
    val items: StateFlow<List<ActivityItem>?> = posts.activity().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    fun markSeen() = viewModelScope.launch { posts.markActivitySeen() }
}
