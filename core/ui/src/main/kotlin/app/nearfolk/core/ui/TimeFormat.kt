package app.nearfolk.core.ui

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import app.nearfolk.core.model.Presence
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Date
import java.util.Locale

/**
 * Formats times the way a messaging app should: exact clock times that
 * follow the device 24 hour setting, and short relative labels where space
 * is tight.
 */
@Stable
class TimeFormat internal constructor(private val context: Context, private val locale: Locale) {
    private val zone: ZoneId get() = ZoneId.systemDefault()
    private val clockFormat = DateFormat.getTimeFormat(context)
    private val monthDay = DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "MMMd"), locale)
    private val monthDayYear = DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "MMMdyyyy"), locale)
    private val weekdayMonthDay = DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "EEEEMMMMd"), locale)
    private val longDate = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale)

    fun clock(instant: Instant): String = clockFormat.format(Date.from(instant))

    private fun date(instant: Instant): LocalDate = instant.atZone(zone).toLocalDate()

    fun shortDate(date: LocalDate, today: LocalDate = LocalDate.now(zone)): String =
        if (date.year == today.year) monthDay.format(date) else monthDayYear.format(date)

    fun longDate(date: LocalDate): String = longDate.format(date)

    /** "Online" or "Last seen today at 14:32". */
    fun presence(presence: Presence, now: Instant = Instant.now()): String = when (presence) {
        Presence.Online -> context.getString(R.string.ui_online)
        is Presence.Offline -> {
            val seen = presence.lastSeenAt
            val today = date(now)
            val day = date(seen)
            when {
                Duration.between(seen, now).toMinutes() < 1 -> context.getString(R.string.ui_last_seen_just_now)
                day == today -> context.getString(R.string.ui_last_seen_today, clock(seen))
                day == today.minusDays(1) -> context.getString(R.string.ui_last_seen_yesterday, clock(seen))
                else -> context.getString(R.string.ui_last_seen_date, shortDate(day, today), clock(seen))
            }
        }
    }

    /** Compact presence for list rows: "Seen 14:32", "Seen yesterday", "Seen Mar 3". */
    fun presenceShort(presence: Presence, now: Instant = Instant.now()): String = when (presence) {
        Presence.Online -> context.getString(R.string.ui_online)
        is Presence.Offline -> {
            val today = date(now)
            val day = date(presence.lastSeenAt)
            when (day) {
                today -> context.getString(R.string.ui_seen_short_today, clock(presence.lastSeenAt))
                today.minusDays(1) -> context.getString(R.string.ui_seen_short_yesterday)
                else -> context.getString(R.string.ui_seen_short_date, shortDate(day, today))
            }
        }
    }

    /** Inbox timestamp: clock time today, "Yesterday", weekday this week, date beyond. */
    fun inbox(instant: Instant, now: Instant = Instant.now()): String {
        val today = date(now)
        val day = date(instant)
        return when {
            day == today -> clock(instant)
            day == today.minusDays(1) -> context.getString(R.string.ui_yesterday)
            day.isAfter(today.minusDays(7)) -> day.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, locale)
            else -> shortDate(day, today)
        }
    }

    /** Relative age for posts and comments: "now", "5m", "3h", "2d", then a date. */
    fun relative(instant: Instant, now: Instant = Instant.now()): String {
        val d = Duration.between(instant, now)
        return when {
            d.toMinutes() < 1 -> context.getString(R.string.ui_now)
            d.toHours() < 1 -> context.getString(R.string.ui_minutes_short, d.toMinutes().toInt())
            d.toDays() < 1 -> context.getString(R.string.ui_hours_short, d.toHours().toInt())
            d.toDays() < 7 -> context.getString(R.string.ui_days_short, d.toDays().toInt())
            else -> shortDate(date(instant), date(now))
        }
    }

    /** Day separator in a chat thread. */
    fun dayHeader(day: LocalDate, today: LocalDate = LocalDate.now(zone)): String = when (day) {
        today -> context.getString(R.string.ui_today)
        today.minusDays(1) -> context.getString(R.string.ui_yesterday)
        else -> if (day.year == today.year) weekdayMonthDay.format(day) else longDate.format(day)
    }

    fun localDate(instant: Instant): LocalDate = date(instant)
}

@Composable
fun rememberTimeFormat(): TimeFormat {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val locale = configuration.locales[0]
    return remember(context, locale) { TimeFormat(context, locale) }
}
