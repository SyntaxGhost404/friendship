package app.nearfolk.core.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import app.nearfolk.core.data.di.IoDispatcher
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
}

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

    fun newVoiceFile(): File = File(dir(MediaKind.Voice), "${UUID.randomUUID()}.m4a")

    /** Deletes a file only when it lives inside app media storage. */
    suspend fun delete(path: String?) = withContext(io) {
        if (path == null) return@withContext
        val file = File(path)
        if (file.canonicalPath.startsWith(root.canonicalPath)) file.delete()
    }

    suspend fun purgeAll() = withContext(io) { root.deleteRecursively() }
}
