package app.nearfolk.core.data.media

import app.nearfolk.core.model.Attachment
import app.nearfolk.core.model.AttachmentLimits
import app.nearfolk.core.model.AttachmentProblem
import app.nearfolk.core.model.DraftAttachment
import app.nearfolk.core.model.MediaSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import javax.inject.Inject

/**
 * Media waiting in a composer. Items are copied into app storage as soon as
 * they are added, because content shared by a keyboard is only readable for
 * a short time. Sending hands the files over to the message; discarding
 * deletes them.
 */
class AttachmentDraft internal constructor(
    private val scope: CoroutineScope,
    private val media: MediaFiles,
    val limit: Int,
) {
    private val _items = MutableStateFlow<List<DraftAttachment>>(emptyList())
    val items: StateFlow<List<DraftAttachment>> = _items.asStateFlow()

    private val _problems = MutableSharedFlow<AttachmentProblem>(extraBufferCapacity = 8)
    val problems: SharedFlow<AttachmentProblem> = _problems.asSharedFlow()

    private val jobs = mutableMapOf<String, Job>()

    val remaining: Int get() = (limit - _items.value.size).coerceAtLeast(0)

    fun add(sources: List<MediaSource>) {
        val accepted = sources.take(remaining)
        if (accepted.size < sources.size) _problems.tryEmit(AttachmentProblem.LimitReached)
        accepted.forEach { source ->
            val id = UUID.randomUUID().toString()
            _items.update { it + DraftAttachment.Importing(id) }
            jobs[id] = scope.launch {
                val result = runCatching { media.importAttachment(source) }
                jobs.remove(id)
                result
                    .onSuccess { attachment ->
                        val stillWanted = _items.value.any { it.id == id }
                        if (stillWanted) {
                            _items.update { list -> list.map { if (it.id == id) DraftAttachment.Ready(id, attachment) else it } }
                        } else {
                            File(attachment.path).delete()
                        }
                    }
                    .onFailure { error ->
                        _items.update { list -> list.filterNot { it.id == id } }
                        _problems.tryEmit((error as? AttachmentRejected)?.problem ?: AttachmentProblem.Failed)
                    }
            }
        }
    }

    fun remove(id: String) {
        jobs.remove(id)?.cancel()
        val item = _items.value.firstOrNull { it.id == id }
        _items.update { list -> list.filterNot { it.id == id } }
        (item as? DraftAttachment.Ready)?.let { File(it.attachment.path).delete() }
    }

    /** Ready attachments, in the order they were added. */
    fun ready(): List<Attachment> = _items.value.filterIsInstance<DraftAttachment.Ready>().map { it.attachment }

    /** Clears the draft after a successful send; the files now belong to the message. */
    fun handOver() {
        _items.value = emptyList()
    }

    /** Clears the draft and deletes every copied file. Safe to call from onCleared. */
    fun discard() {
        jobs.values.forEach { it.cancel() }
        jobs.clear()
        _items.value.filterIsInstance<DraftAttachment.Ready>().forEach { File(it.attachment.path).delete() }
        _items.value = emptyList()
    }
}

class AttachmentDraftFactory @Inject constructor(private val media: MediaFiles) {
    fun create(scope: CoroutineScope, limit: Int = AttachmentLimits.PER_MESSAGE) = AttachmentDraft(scope, media, limit)
}
