package app.nearfolk.core.data.repository

import app.nearfolk.core.data.media.AttachmentDraftFactory
import app.nearfolk.core.model.AttachmentProblem
import app.nearfolk.core.model.Comment
import app.nearfolk.core.model.DraftAttachment
import app.nearfolk.core.model.MediaSource
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The comments of one post at a time and the attachments of the comment being
 * written for it. Used by every screen that opens a comments sheet. The text
 * itself stays with the screen, which owns its text field.
 */
class CommentThread internal constructor(
    private val scope: CoroutineScope,
    private val posts: PostRepository,
    drafts: AttachmentDraftFactory,
) {
    private val open = MutableStateFlow<String?>(null)
    private val media = drafts.create(scope)
    private var draftFor: String? = null

    /** The post whose comments are open, or null when the sheet is closed. */
    val postId: StateFlow<String?> = open.asStateFlow()

    val comments: StateFlow<List<Comment>> = open
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else posts.comments(id) }
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val attachments: StateFlow<List<DraftAttachment>> = media.items
    val problems: Flow<AttachmentProblem> = media.problems

    /**
     * Opens the comments of [postId], or closes them when it is null. A draft
     * belongs to one post: opening a different post discards the attachments
     * and returns true so the caller clears its text as well.
     */
    fun open(postId: String?): Boolean {
        val switched = postId != null && postId != draftFor
        if (switched) {
            media.discard()
            draftFor = postId
        }
        open.value = postId
        return switched
    }

    fun addMedia(sources: List<MediaSource>) = media.add(sources)

    fun removeAttachment(id: String) = media.remove(id)

    /**
     * Sends [text] with the attachments that are ready. Returns false, and
     * keeps the draft, when there is nothing to send or a file is still being
     * prepared; the caller clears its text only when this returns true.
     */
    fun send(text: String): Boolean {
        val id = open.value ?: return false
        val files = media.ready()
        if (text.isBlank() && files.isEmpty()) return false
        if (attachments.value.any { it is DraftAttachment.Importing }) return false
        media.handOver()
        scope.launch { posts.addComment(id, text, files) }
        return true
    }

    /** Deletes the copied files of an unsent draft. Call from onCleared. */
    fun discard() = media.discard()
}

class CommentThreadFactory @Inject constructor(
    private val posts: PostRepository,
    private val drafts: AttachmentDraftFactory,
) {
    fun create(scope: CoroutineScope) = CommentThread(scope, posts, drafts)
}
