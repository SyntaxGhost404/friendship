package app.nearfolk.feature.you

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import app.nearfolk.core.data.repository.CommentThreadFactory
import app.nearfolk.core.data.repository.PostRepository
import app.nearfolk.core.model.Comment
import app.nearfolk.core.model.DraftAttachment
import app.nearfolk.core.model.MediaSource
import app.nearfolk.core.model.Post
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SavedEvent {
    /** A post left the list. [savedAt] puts it back in the same place. */
    data class Removed(val postId: String, val savedAt: Instant) : SavedEvent
}

@HiltViewModel
class SavedPostsViewModel @Inject constructor(
    private val posts: PostRepository,
    threads: CommentThreadFactory,
) : ViewModel() {

    val saved: Flow<PagingData<Post>> = posts.saved().cachedIn(viewModelScope)
    val count: StateFlow<Int?> = posts.savedCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _events = Channel<SavedEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val thread = threads.create(viewModelScope)
    val commentsPostId: StateFlow<String?> = thread.postId
    val comments: StateFlow<List<Comment>> = thread.comments
    val commentText = TextFieldState()
    val commentAttachments: StateFlow<List<DraftAttachment>> = thread.attachments
    val attachmentProblems = thread.problems

    fun toggleLike(postId: String) {
        viewModelScope.launch { posts.toggleLike(postId) }
    }

    /** Everything here is saved, so the bookmark takes the post out, with a way back. */
    fun toggleSave(post: Post) {
        val savedAt = post.savedAt
        viewModelScope.launch {
            if (savedAt == null) {
                posts.toggleSave(post.id)
            } else {
                posts.setSaved(post.id, null)
                _events.send(SavedEvent.Removed(post.id, savedAt))
            }
        }
    }

    fun restore(postId: String, savedAt: Instant) {
        viewModelScope.launch { posts.setSaved(postId, savedAt) }
    }

    fun recordView(postId: String) = posts.recordView(postId)

    fun openComments(postId: String?) {
        if (thread.open(postId)) commentText.clearText()
    }

    fun addCommentMedia(sources: List<MediaSource>) = thread.addMedia(sources)

    fun removeCommentAttachment(id: String) = thread.removeAttachment(id)

    fun sendComment() {
        if (thread.send(commentText.text.toString())) commentText.clearText()
    }

    override fun onCleared() {
        thread.discard()
    }
}
