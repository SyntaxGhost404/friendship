package app.nearfolk.core.model

import java.time.LocalDate
import java.time.MonthDay

/** Western tropical zodiac, keyed by the first day of each sign. */
enum class ZodiacSign(private val start: MonthDay) {
    Capricorn(MonthDay.of(12, 22)),
    Aquarius(MonthDay.of(1, 20)),
    Pisces(MonthDay.of(2, 19)),
    Aries(MonthDay.of(3, 21)),
    Taurus(MonthDay.of(4, 20)),
    Gemini(MonthDay.of(5, 21)),
    Cancer(MonthDay.of(6, 21)),
    Leo(MonthDay.of(7, 23)),
    Virgo(MonthDay.of(8, 23)),
    Libra(MonthDay.of(9, 23)),
    Scorpio(MonthDay.of(10, 23)),
    Sagittarius(MonthDay.of(11, 22)),
    ;

    companion object {
        private val ordered = entries.filter { it != Capricorn }.sortedBy { it.start }

        fun of(date: LocalDate): ZodiacSign {
            val day = MonthDay.from(date)
            if (!day.isBefore(Capricorn.start)) return Capricorn
            return ordered.lastOrNull { !day.isBefore(it.start) } ?: Capricorn
        }
    }
}
