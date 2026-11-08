package app.nearfolk.core.model

/**
 * Pointer to an image. Generated artwork is drawn on device from a seed, local
 * files live in app storage, and remote images come from a future backend.
 */
sealed interface MediaRef {
    data class Generated(val seed: Long) : MediaRef
    data class Local(val path: String) : MediaRef
    data class Remote(val url: String) : MediaRef

    fun encode(): String = when (this) {
        is Generated -> "gen:$seed"
        is Local -> "file:$path"
        is Remote -> url
    }

    companion object {
        fun decode(raw: String?): MediaRef? = when {
            raw.isNullOrBlank() -> null
            raw.startsWith("gen:") -> raw.removePrefix("gen:").toLongOrNull()?.let(::Generated)
            raw.startsWith("file:") -> Local(raw.removePrefix("file:"))
            else -> Remote(raw)
        }
    }
}
