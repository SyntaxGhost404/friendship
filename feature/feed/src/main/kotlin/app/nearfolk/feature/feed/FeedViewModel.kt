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
import app.nearfolk.core.model.Post
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import app.nearfolk.core.model.CommentSort
import app.nearfolk.core.model.CommentsState
import app.nearfolk.core.model.Vote
import app.nearfolk.core.ui.CommentsHost

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val posts: PostRepository,
    threads: CommentThreadFactory,
) : ViewModel(), CommentsHost {

    val feed: Flow<PagingData<Post>> = posts.feed().cachedIn(viewModelScope)

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

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
}
