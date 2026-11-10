package app.nearfolk.core.data.repository

import app.nearfolk.core.model.Comment
import app.nearfolk.core.model.CommentSort
import app.nearfolk.core.model.CommentsState
import app.nearfolk.core.model.Vote
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The comments sheet of one post at a time, modelled on YouTube: a list of
 * top-level comments sorted by Top or Newest, and for one of them at a time a
 * replies page with its replies oldest first. Every screen that shows posts
 * keeps one of these. The text being written stays with the screen, which owns
 * its text fields; [open] says when to clear them.
 */
class CommentThread internal constructor(
    private val scope: CoroutineScope,
    private val comments: CommentRepository,
    private val posts: PostRepository,
) {
    private val postId = MutableStateFlow<String?>(null)
    private val sort = MutableStateFlow(CommentSort.Top)
    private val openThreadId = MutableStateFlow<String?>(null)
    private val replyingTo = MutableStateFlow<Comment?>(null)
    private var draftFor: String? = null

    val state: StateFlow<CommentsState> = postId
        .flatMapLatest { id -> if (id == null) flowOf(CommentsState()) else stateFor(id) }
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), CommentsState())

    private fun stateFor(postId: String): Flow<CommentsState> {
        val threads = sort.flatMapLatest { order -> comments.topLevel(postId).inStableOrder(order).map { order to it } }
        val thread = openThreadId.flatMapLatest { root ->
            if (root == null) flowOf(null to emptyList()) else combine(comments.comment(root), comments.replies(root), ::Pair)
        }
        return combine(posts.post(postId), threads, thread, replyingTo) { post, (order, top), (root, replies), target ->
            CommentsState(
                postId = postId,
                postAuthorId = post?.authorId,
                total = post?.commentCount ?: top.size,
                sort = order,
                loading = false,
                threads = top,
                openThread = root,
                replies = replies,
                // The live copy of the reply being answered; gone if it disappeared.
                replyingTo = target?.let { t -> replies.firstOrNull { it.id == t.id } },
            )
        }
    }

    /**
     * Opens the comments of [postId], or closes them when it is null. A newly
     * opened sheet starts on the comment list. Returns true when the comments
     * belong to a different post than last time, so the caller clears its drafts.
     */
    fun open(postId: String?): Boolean {
        if (postId != this.postId.value) {
            openThreadId.value = null
            replyingTo.value = null
        }
        this.postId.value = postId
        val switched = postId != null && postId != draftFor
        if (switched) draftFor = postId
        return switched
    }

    fun setSort(order: CommentSort) {
        sort.value = order
    }

    /** Opens the replies page of [comment]'s thread. Returns true when that is a different thread. */
    fun openReplies(comment: Comment): Boolean {
        val changed = openThreadId.value != comment.threadId
        openThreadId.value = comment.threadId
        replyingTo.value = null
        return changed
    }

    fun closeReplies() {
        openThreadId.value = null
        replyingTo.value = null
    }

    /**
     * Points the reply box at [comment] and opens its thread. Answering a
     * reply names its author; answering the top-level comment does not.
     * Returns true when the thread changed.
     */
    fun replyTo(comment: Comment): Boolean {
        val changed = openReplies(comment)
        replyingTo.value = comment.takeIf { it.isReply }
        return changed
    }

    /** Answers the thread itself again instead of one reply in it. */
    fun clearReplyTarget() {
        replyingTo.value = null
    }

    fun vote(comment: Comment, vote: Vote) {
        scope.launch { comments.vote(comment.id, vote) }
    }

    /** Posts a top-level comment. Returns false, keeping the draft, when there is nothing to post. */
    fun send(text: String): Boolean {
        val id = postId.value ?: return false
        if (text.isBlank()) return false
        scope.launch { comments.add(id, text) }
        return true
    }

    /** Posts a reply in the open thread, to the reply being answered or to the thread itself. */
    fun sendReply(text: String): Boolean {
        val id = postId.value ?: return false
        val current = state.value
        val root = current.openThread ?: return false
        if (text.isBlank()) return false
        val target = current.replyingTo ?: root
        scope.launch { comments.add(id, text, replyTo = target) }
        replyingTo.value = null
        return true
    }
}

class CommentThreadFactory @Inject constructor(
    private val comments: CommentRepository,
    private val posts: PostRepository,
) {
    fun create(scope: CoroutineScope) = CommentThread(scope, comments, posts)
}

/** How top-level comments are ranked when the list is first shown. */
internal val CommentSort.comparator: Comparator<Comment>
    get() = when (this) {
        CommentSort.Top -> compareByDescending<Comment> { it.score }.thenByDescending { it.replyCount }.thenByDescending { it.createdAt }
        CommentSort.Newest -> compareByDescending { it.createdAt }
    }

/**
 * Sorts the list once, then keeps that order while the sheet is open, so a
 * comment does not jump away under your finger when you vote on it. Comments
 * that arrive later, such as the one you just wrote, go to the top; removed
 * ones drop out. Choosing another sort starts over.
 */
internal fun Flow<List<Comment>>.inStableOrder(sort: CommentSort): Flow<List<Comment>> = flow {
    var order: List<String>? = null
    collect { list ->
        val byId = list.associateBy { it.id }
        val known = order
        val next = if (known == null) {
            list.sortedWith(sort.comparator).map { it.id }
        } else {
            val seen = known.toHashSet()
            list.filter { it.id !in seen }.sortedByDescending { it.createdAt }.map { it.id } + known.filter { it in byId }
        }
        order = next
        emit(next.map { byId.getValue(it) })
    }
}
