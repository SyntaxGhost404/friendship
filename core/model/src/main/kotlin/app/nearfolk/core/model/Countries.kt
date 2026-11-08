package app.nearfolk.core.model

import java.util.Locale

/** Country helpers backed by the JDK region list, so names follow the device language. */
object Countries {
    val codes: List<String> = Locale.getISOCountries().toList()

    fun displayName(code: String, locale: Locale = Locale.getDefault()): String =
        runCatching { Locale.Builder().setRegion(code).build().getDisplayCountry(locale) }
            .getOrNull()
            ?.ifBlank { null }
            ?: code

    /** Regional indicator pair, which renders as a flag on Android. */
    fun flag(code: String): String {
        if (code.length != 2 || !code.all { it in 'A'..'Z' }) return ""
        val base = 0x1F1E6 - 'A'.code
        return String(Character.toChars(base + code[0].code)) + String(Character.toChars(base + code[1].code))
    }
}
