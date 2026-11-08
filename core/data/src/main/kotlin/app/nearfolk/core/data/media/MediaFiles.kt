package app.nearfolk.core.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.webkit.MimeTypeMap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.core.net.toUri
import app.nearfolk.core.data.di.IoDispatcher
import app.nearfolk.core.model.Attachment
import app.nearfolk.core.model.AttachmentKind
import app.nearfolk.core.model.AttachmentLimits
import app.nearfolk.core.model.AttachmentProblem
import app.nearfolk.core.model.MediaSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

enum class MediaKind(val folder: String, val maxEdge: Int) {
    Avatar("avatars", 720),
    Banner("banners", 1600),
    Post("posts", 2048),
    Highlight("highlights", 2048),
    Voice("voice", 0),
    Attachment("attachments", 2048),
}

/** Thrown by [MediaFiles.importAttachment] with the reason the item was refused. */
class AttachmentRejected(val problem: AttachmentProblem) : Exception(problem.name)

/**
 * App private media storage. Picked images are decoded, downscaled and
 * re-encoded, which also strips location and camera metadata.
 */
@Singleton
class MediaFiles @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    private val root: File get() = File(context.filesDir, "media")

    private fun dir(kind: MediaKind) = File(root, kind.folder).apply { mkdirs() }

    /** Decodes [uri] for editing, already rotated upright and no larger than [maxEdge]. */
    suspend fun decode(uri: Uri, maxEdge: Int = 2048): Bitmap = withContext(io) {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val longest = max(info.size.width, info.size.height)
            if (longest > maxEdge) {
                val scale = maxEdge.toFloat() / longest
                decoder.setTargetSize((info.size.width * scale).toInt(), (info.size.height * scale).toInt())
            }
        }
    }

    suspend fun importImage(uri: Uri, kind: MediaKind): String = save(decode(uri, kind.maxEdge), kind)

    suspend fun save(bitmap: Bitmap, kind: MediaKind): String = withContext(io) {
        val file = File(dir(kind), "${UUID.randomUUID()}.webp")
        file.outputStream().use { out ->
            @Suppress("DEPRECATION")
            val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP
            bitmap.compress(format, 88, out)
        }
        file.absolutePath
    }

    /**
     * Copies a picked or pasted item into app storage.
     *
     * Still photos are decoded, downscaled and re-encoded, which drops location
     * and camera metadata. GIFs and WebP (which may be animated stickers) are
     * kept byte for byte so they keep moving. Videos are copied as they are.
     */
    suspend fun importAttachment(source: MediaSource): Attachment = withContext(io) {
        val uri = source.uri.toUri()
        val resolver = context.contentResolver
        val mime = (resolver.getType(uri) ?: source.mimeType ?: "").lowercase()
        val size = sizeOf(uri)
        val id = UUID.randomUUID().toString()
        when {
            mime == "image/gif" || mime == "image/webp" -> {
                if (size > AttachmentLimits.MAX_IMAGE_BYTES) throw AttachmentRejected(AttachmentProblem.TooLarge)
                val file = File(dir(MediaKind.Attachment), "$id.${if (mime == "image/gif") "gif" else "webp"}")
                copy(uri, file)
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, bounds)
                if (bounds.outWidth <= 0) {
                    file.delete()
                    throw AttachmentRejected(AttachmentProblem.Unsupported)
                }
                Attachment(
                    id = id,
                    kind = if (mime == "image/gif") AttachmentKind.Gif else AttachmentKind.Image,
                    path = file.absolutePath,
                    mimeType = mime,
                    width = bounds.outWidth,
                    height = bounds.outHeight,
                    sizeBytes = file.length(),
                )
            }
            mime.startsWith("image/") -> {
                if (size > AttachmentLimits.MAX_IMAGE_BYTES) throw AttachmentRejected(AttachmentProblem.TooLarge)
                val bitmap = runCatching { decode(uri, MediaKind.Attachment.maxEdge) }
                    .getOrElse { throw AttachmentRejected(AttachmentProblem.Unsupported) }
                val path = save(bitmap, MediaKind.Attachment)
                Attachment(id, AttachmentKind.Image, path, "image/webp", bitmap.width, bitmap.height, sizeBytes = File(path).length())
            }
            mime.startsWith("video/") -> {
                if (size > AttachmentLimits.MAX_VIDEO_BYTES) throw AttachmentRejected(AttachmentProblem.TooLarge)
                val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "mp4"
                val file = File(dir(MediaKind.Attachment), "$id.$extension")
                copy(uri, file)
                val meta = runCatching { videoMetadata(file) }.getOrNull()
                if (meta == null) {
                    file.delete()
                    throw AttachmentRejected(AttachmentProblem.Unsupported)
                }
                Attachment(id, AttachmentKind.Video, file.absolutePath, mime, meta.width, meta.height, meta.durationMillis, file.length())
            }
            else -> throw AttachmentRejected(AttachmentProblem.Unsupported)
        }
    }

    private data class VideoMeta(val width: Int, val height: Int, val durationMillis: Long)

    private fun videoMetadata(file: File): VideoMeta {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(file.absolutePath)
            fun int(key: Int) = retriever.extractMetadata(key)?.toIntOrNull() ?: 0
            val rotation = int(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
            val w = int(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val h = int(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            require(w > 0 && h > 0)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0
            return if (rotation == 90 || rotation == 270) VideoMeta(h, w, duration) else VideoMeta(w, h, duration)
        } finally {
            retriever.release()
        }
    }

    private fun sizeOf(uri: Uri): Long =
        runCatching { context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } }.getOrNull() ?: -1L

    private fun copy(uri: Uri, target: File) {
        val input = context.contentResolver.openInputStream(uri) ?: throw AttachmentRejected(AttachmentProblem.Failed)
        input.use { source -> target.outputStream().use { source.copyTo(it) } }
    }

    /** Deletes the files behind attachments that are no longer referenced. */
    suspend fun deleteAttachments(list: List<Attachment>) = list.forEach { delete(it.path) }

    fun newVoiceFile(): File = File(dir(MediaKind.Voice), "${UUID.randomUUID()}.m4a")

    /** Deletes a file only when it lives inside app media storage. */
    suspend fun delete(path: String?) = withContext(io) {
        if (path == null) return@withContext
        val file = File(path)
        if (file.canonicalPath.startsWith(root.canonicalPath)) file.delete()
    }

    suspend fun purgeAll() = withContext(io) { root.deleteRecursively() }
}
