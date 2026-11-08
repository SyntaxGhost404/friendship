package app.nearfolk.core.data.media

import app.nearfolk.core.model.Attachment
import app.nearfolk.core.model.AttachmentKind
import org.json.JSONArray
import org.json.JSONObject

/** Stores attachment lists in a single text column. Unknown or broken rows decode to an empty list. */
object AttachmentCodec {
    fun encode(list: List<Attachment>): String {
        if (list.isEmpty()) return "[]"
        val array = JSONArray()
        list.forEach { a ->
            array.put(
                JSONObject()
                    .put("id", a.id)
                    .put("kind", a.kind.name)
                    .put("path", a.path)
                    .put("mime", a.mimeType)
                    .put("w", a.width)
                    .put("h", a.height)
                    .put("d", a.durationMillis)
                    .put("s", a.sizeBytes),
            )
        }
        return array.toString()
    }

    fun decode(raw: String?): List<Attachment> {
        if (raw.isNullOrBlank() || raw == "[]") return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { i ->
                val o = array.optJSONObject(i) ?: return@mapNotNull null
                val kind = AttachmentKind.entries.firstOrNull { it.name == o.optString("kind") } ?: return@mapNotNull null
                val path = o.optString("path").ifBlank { return@mapNotNull null }
                Attachment(
                    id = o.optString("id"),
                    kind = kind,
                    path = path,
                    mimeType = o.optString("mime"),
                    width = o.optInt("w"),
                    height = o.optInt("h"),
                    durationMillis = o.optLong("d"),
                    sizeBytes = o.optLong("s"),
                )
            }
        }.getOrDefault(emptyList())
    }
}
