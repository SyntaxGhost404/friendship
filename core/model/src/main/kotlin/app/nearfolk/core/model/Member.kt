package app.nearfolk.core.model

import java.time.Instant
import java.time.LocalDate
import java.time.Period

/** Identifier used for the signed in person across every table and flow. */
const val SELF_ID: String = "self"

enum class Gender { Female, Male, Nonbinary }

/** A person on the network, as seen from the outside. */
data class Member(
    val id: String,
    val displayName: String,
    val gender: Gender,
    val birthDate: LocalDate,
    val countryCode: String,
    val city: String?,
    val catchphrase: String,
    val bio: String,
    val avatar: MediaRef?,
    val banner: MediaRef?,
    val streakStart: LocalDate,
    val presence: Presence,
    val joinedAt: Instant,
    val isPlus: Boolean,
    val voiceIntro: VoiceIntro?,
) {
    val isSelf: Boolean get() = id == SELF_ID

    fun ageOn(today: LocalDate): Int = Period.between(birthDate, today).years.coerceAtLeast(0)

    val zodiac: ZodiacSign get() = ZodiacSign.of(birthDate)

    /** Whole days since [streakStart], counting the start day as day one. */
    fun streakDays(today: LocalDate): Long =
        (today.toEpochDay() - streakStart.toEpochDay() + 1).coerceAtLeast(0)
}

sealed interface Presence {
    data object Online : Presence
    data class Offline(val lastSeenAt: Instant) : Presence
}

data class VoiceIntro(
    val path: String,
    val durationMillis: Long,
    val recordedAt: Instant,
)
