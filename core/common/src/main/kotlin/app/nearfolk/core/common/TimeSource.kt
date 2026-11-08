package app.nearfolk.core.common

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Injectable clock so time based logic stays testable. */
interface TimeSource {
    fun now(): Instant
    fun zone(): ZoneId
    fun today(): LocalDate = now().atZone(zone()).toLocalDate()

    companion object {
        val System: TimeSource = object : TimeSource {
            override fun now(): Instant = Clock.systemUTC().instant()
            override fun zone(): ZoneId = ZoneId.systemDefault()
        }
    }
}
