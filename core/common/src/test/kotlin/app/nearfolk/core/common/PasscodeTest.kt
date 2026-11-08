package app.nearfolk.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PasscodeTest {

    @Test
    fun validationRules() {
        assertEquals(Passcode.Problem.WrongLength, Passcode.validate("1234"))
        assertEquals(Passcode.Problem.NotDigits, Passcode.validate("12a456"))
        assertEquals(Passcode.Problem.TooSimple, Passcode.validate("111111"))
        assertEquals(Passcode.Problem.TooSimple, Passcode.validate("345678"))
        assertEquals(Passcode.Problem.TooSimple, Passcode.validate("987654"))
        assertNull(Passcode.validate("402817"))
    }

    @Test
    fun hashRoundTrip() {
        val stored = Passcode.hash("402817")
        assertTrue(Passcode.verify("402817", stored))
        assertFalse(Passcode.verify("402818", stored))
    }

    @Test
    fun saltMakesHashesDiffer() {
        assertFalse(Passcode.hash("402817").hash == Passcode.hash("402817").hash)
    }

    @Test
    fun lockoutGrowsAndCaps() {
        assertEquals(0, Passcode.lockoutMillis(4))
        assertEquals(30_000, Passcode.lockoutMillis(5))
        assertEquals(60_000, Passcode.lockoutMillis(6))
        assertEquals(15 * 60_000L, Passcode.lockoutMillis(40))
    }
}
