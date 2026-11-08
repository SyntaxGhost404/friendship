package app.nearfolk.feature.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import app.nearfolk.core.data.media.AttachmentDraftFactory
import app.nearfolk.core.data.media.PlaybackState
import app.nearfolk.core.data.media.VoicePlayer
import app.nearfolk.core.data.repository.ChatRepository
import app.nearfolk.core.data.repository.MemberRepository
import app.nearfolk.core.data.repository.PostRepository
import app.nearfolk.core.data.repository.ProfileRepository
import app.nearfolk.core.model.Comment
import app.nearfolk.core.model.DraftAttachment
import app.nearfolk.core.model.MediaSource
import app.nearfolk.core.model.Highlight
import app.nearfolk.core.model.Member
import app.nearfolk.core.model.Post
import app.nearfolk.core.model.ReportReason
import app.nearfolk.core.model.SELF_ID
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val loading: Boolean = true,
    val member: Member? = null,
    val highlights: List<Highlight> = emptyList(),
    val journalCount: Int = 0,
    val blocked: Boolean = false,
    val preview: Boolean = false,
)

sealed interface ProfileEvent {
    data class OpenChat(val conversationId: String) : ProfileEvent
    data object Reported : ProfileEvent
    data class Blocked(val name: String) : ProfileEvent
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    members: MemberRepository,
    profile: ProfileRepository,
    private val postRepository: PostRepository,
    private val chat: ChatRepository,
    private val player: VoicePlayer,
    drafts: AttachmentDraftFactory,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<MemberProfileRoute>()
    val memberId: String = route.memberId
    private val isSelf = memberId == SELF_ID

    private val memberFlow: Flow<Member?> = if (isSelf) profile.me else members.member(memberId)

    val state: StateFlow<ProfileUiState> = combine(
        memberFlow,
        members.highlights(memberId),
        members.publicJournalCount(memberId),
        if (isSelf) flowOf(false) else members.isBlocked(memberId),
    ) { member, highlights, journalCount, blocked ->
        ProfileUiState(false, member, highlights, journalCount, blocked, preview = isSelf)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState(preview = isSelf))

    val postStream: Flow<PagingData<Post>> = postRepository.posts(memberId).cachedIn(viewModelScope)

    val playback: StateFlow<PlaybackState> = player.state

    private val openCommentsFor = MutableStateFlow<String?>(null)
    val commentsPostId: StateFlow<String?> = openCommentsFor
    val comments: StateFlow<List<Comment>> = openCommentsFor
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else postRepository.comments(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _events = Channel<ProfileEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun message() {
        if (isSelf) return
        viewModelScope.launch { _events.send(ProfileEvent.OpenChat(chat.conversationWith(memberId))) }
    }

    fun toggleLike(postId: String) {
        viewModelScope.launch { postRepository.toggleLike(postId) }
    }

    /** Comment being written for the open post. */
    val commentText = TextFieldState()
    private val commentMedia = drafts.create(viewModelScope)
    val commentAttachments: StateFlow<List<DraftAttachment>> = commentMedia.items
    val attachmentProblems = commentMedia.problems

    fun openComments(postId: String?) {
        // A draft belongs to one post; switching posts starts fresh.
        if (postId != null && postId != lastCommentPost) {
            commentText.clearText()
            commentMedia.discard()
            lastCommentPost = postId
        }
        openCommentsFor.value = postId
    }

    private var lastCommentPost: String? = null

    fun addCommentMedia(sources: List<MediaSource>) = commentMedia.add(sources)

    fun removeCommentAttachment(id: String) = commentMedia.remove(id)

    fun sendComment() {
        val id = openCommentsFor.value ?: return
        val body = commentText.text.toString()
        val files = commentMedia.ready()
        if (body.isBlank() && files.isEmpty()) return
        if (commentAttachments.value.any { it is DraftAttachment.Importing }) return
        commentText.clearText()
        commentMedia.handOver()
        viewModelScope.launch { postRepository.addComment(id, body, files) }
    }

    fun toggleVoice() {
        state.value.member?.voiceIntro?.let { player.toggle(it.path) }
    }

    fun report(reason: ReportReason, details: String?) {
        viewModelScope.launch {
            chat.report(memberId, reason, details, null)
            _events.send(ProfileEvent.Reported)
        }
    }

    fun block() {
        viewModelScope.launch {
            chat.block(memberId)
            _events.send(ProfileEvent.Blocked(state.value.member?.displayName.orEmpty()))
        }
    }

    fun unblock() {
        viewModelScope.launch { chat.unblock(memberId) }
    }

    override fun onCleared() {
        commentMedia.discard()
        if (player.state.value.path == state.value.member?.voiceIntro?.path) player.stop()
    }
}
