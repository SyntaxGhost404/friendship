package app.nearfolk.core.model

enum class AttachmentKind { Image, Gif, Video }

/** A media file stored in app storage and attached to a message or comment. */
data class Attachment(
    val id: String,
    val kind: AttachmentKind,
    val path: String,
    val mimeType: String,
    val width: Int,
    val height: Int,
    val durationMillis: Long = 0,
    val sizeBytes: Long = 0,
) {
    /** Width over height, clamped so very tall or wide media still lays out sensibly. */
    val aspectRatio: Float
        get() = if (width > 0 && height > 0) (width.toFloat() / height).coerceIn(0.5f, 2f) else 1f
}

/** Where a picked or pasted item comes from, before it is copied into app storage. */
data class MediaSource(val uri: String, val mimeType: String?)

/** One slot in a composer: still being copied, or ready to send. */
sealed interface DraftAttachment {
    val id: String

    data class Importing(override val id: String) : DraftAttachment
    data class Ready(override val id: String, val attachment: Attachment) : DraftAttachment
}

object AttachmentLimits {
    const val PER_MESSAGE = 10
    const val MAX_IMAGE_BYTES = 20L * 1024 * 1024
    const val MAX_VIDEO_BYTES = 100L * 1024 * 1024
}

enum class AttachmentProblem { TooLarge, Unsupported, LimitReached, Failed }
