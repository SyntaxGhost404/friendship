package app.nearfolk.feature.feed

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
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val posts: PostRepository,
    threads: CommentThreadFactory,
) : ViewModel() {

    val feed: Flow<PagingData<Post>> = posts.feed().cachedIn(viewModelScope)

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private val thread = threads.create(viewModelScope)
    val commentsPostId: StateFlow<String?> = thread.postId
    val comments: StateFlow<List<Comment>> = thread.comments

    /** Comment being written for the open post. */
    val commentText = TextFieldState()
    val commentAttachments: StateFlow<List<DraftAttachment>> = thread.attachments
    val attachmentProblems = thread.problems

    fun refresh() {
        if (_refreshing.value) return
        viewModelScope.launch {
            _refreshing.value = true
            posts.refreshFeed()
            delay(500)
            _refreshing.value = false
        }
    }

    fun toggleLike(postId: String) {
        viewModelScope.launch { posts.toggleLike(postId) }
    }

    fun toggleSave(postId: String) {
        viewModelScope.launch { posts.toggleSave(postId) }
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
