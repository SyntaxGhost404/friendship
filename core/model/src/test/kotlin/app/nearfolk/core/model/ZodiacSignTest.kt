package app.nearfolk.core.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ZodiacSignTest {

    @Test
    fun boundaries() {
        assertEquals(ZodiacSign.Capricorn, ZodiacSign.of(LocalDate.of(1998, 1, 19)))
        assertEquals(ZodiacSign.Aquarius, ZodiacSign.of(LocalDate.of(1998, 1, 20)))
        assertEquals(ZodiacSign.Pisces, ZodiacSign.of(LocalDate.of(2000, 3, 20)))
        assertEquals(ZodiacSign.Aries, ZodiacSign.of(LocalDate.of(2000, 3, 21)))
        assertEquals(ZodiacSign.Leo, ZodiacSign.of(LocalDate.of(1995, 8, 1)))
        assertEquals(ZodiacSign.Sagittarius, ZodiacSign.of(LocalDate.of(1995, 12, 21)))
        assertEquals(ZodiacSign.Capricorn, ZodiacSign.of(LocalDate.of(1995, 12, 22)))
    }

    @Test
    fun mediaRefRoundTrip() {
        val refs = listOf(MediaRef.Generated(42), MediaRef.Local("/data/a.webp"), MediaRef.Remote("https://x.test/a.jpg"))
        refs.forEach { assertEquals(it, MediaRef.decode(it.encode())) }
    }

    @Test
    fun flagFromCode() {
        assertEquals("🇳🇴", Countries.flag("NO"))
        assertEquals("", Countries.flag("x"))
    }
}
