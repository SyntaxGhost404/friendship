package app.nearfolk.core.model

enum class ThemeMode { System, Light, Dark }

enum class GenderFilter { All, Female, Male, Nonbinary }

data class DiscoverFilter(
    val gender: GenderFilter = GenderFilter.All,
    /** ISO 3166 alpha 2 codes. Empty means every country. */
    val countries: Set<String> = emptySet(),
    val onlineOnly: Boolean = false,
    /** Plus only. Null means no limit. */
    val ageRange: IntRange? = null,
) {
    val activeCount: Int
        get() = listOf(gender != GenderFilter.All, countries.isNotEmpty(), onlineOnly, ageRange != null).count { it }
}

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.System,
    val pureBlack: Boolean = false,
    val notifyMessages: Boolean = true,
    val notifyActivity: Boolean = true,
    val haptics: Boolean = true,
    val discoverFilter: DiscoverFilter = DiscoverFilter(),
    val dismissedSafetyBanners: Set<String> = emptySet(),
    val notificationPromptShown: Boolean = false,
)

enum class PlusPlan(val months: Int) { Monthly(1), Yearly(12) }

data class PlusStatus(
    val active: Boolean,
    val plan: PlusPlan?,
    val renewsOnEpochDay: Long?,
)

object PlusLimits {
    const val FREE_HIGHLIGHTS = 9
    const val PLUS_HIGHLIGHTS = 30
    const val FREE_VOICE_SECONDS = 30
    const val PLUS_VOICE_SECONDS = 60

    fun highlights(plus: Boolean) = if (plus) PLUS_HIGHLIGHTS else FREE_HIGHLIGHTS
    fun voiceSeconds(plus: Boolean) = if (plus) PLUS_VOICE_SECONDS else FREE_VOICE_SECONDS
}
