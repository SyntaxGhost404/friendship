package app.nearfolk.feature.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import app.nearfolk.core.data.media.PlaybackState
import app.nearfolk.core.data.media.VoicePlayer
import app.nearfolk.core.data.repository.ChatRepository
import app.nearfolk.core.data.repository.CommentThreadFactory
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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
    threads: CommentThreadFactory,
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

    private val thread = threads.create(viewModelScope)
    val commentsPostId: StateFlow<String?> = thread.postId
    val comments: StateFlow<List<Comment>> = thread.comments

    private val _events = Channel<ProfileEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun message() {
        if (isSelf) return
        viewModelScope.launch { _events.send(ProfileEvent.OpenChat(chat.conversationWith(memberId))) }
    }

    fun toggleLike(postId: String) {
        viewModelScope.launch { postRepository.toggleLike(postId) }
    }

    fun toggleSave(postId: String) {
        viewModelScope.launch { postRepository.toggleSave(postId) }
    }

    fun recordView(postId: String) = postRepository.recordView(postId)

    /** Comment being written for the open post. */
    val commentText = TextFieldState()
    val commentAttachments: StateFlow<List<DraftAttachment>> = thread.attachments
    val attachmentProblems = thread.problems

    fun openComments(postId: String?) {
        if (thread.open(postId)) commentText.clearText()
    }

    fun addCommentMedia(sources: List<MediaSource>) = thread.addMedia(sources)

    fun removeCommentAttachment(id: String) = thread.removeAttachment(id)

    fun sendComment() {
        if (thread.send(commentText.text.toString())) commentText.clearText()
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
        thread.discard()
        if (player.state.value.path == state.value.member?.voiceIntro?.path) player.stop()
    }
}
