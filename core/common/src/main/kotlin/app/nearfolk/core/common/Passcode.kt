package app.nearfolk.core.common

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Six digit passcode rules, hashing and lockout timing for the private journal. */
object Passcode {
    const val LENGTH = 6
    private const val ITERATIONS = 120_000
    private const val KEY_BITS = 256
    private const val SALT_BYTES = 16

    enum class Problem { WrongLength, NotDigits, TooSimple }

    fun validate(code: String): Problem? = when {
        code.length != LENGTH -> Problem.WrongLength
        !code.all { it in '0'..'9' } -> Problem.NotDigits
        isTooSimple(code) -> Problem.TooSimple
        else -> null
    }

    /** Rejects one repeated digit and straight runs such as 123456 or 987654. */
    internal fun isTooSimple(code: String): Boolean {
        if (code.toSet().size == 1) return true
        val steps = code.zipWithNext { a, b -> b - a }.toSet()
        return steps == setOf(1) || steps == setOf(-1)
    }

    data class Hashed(val hash: String, val salt: String)

    fun hash(code: String, salt: ByteArray = newSalt()): Hashed {
        val key = derive(code, salt)
        val encoder = Base64.getEncoder()
        return Hashed(encoder.encodeToString(key), encoder.encodeToString(salt))
    }

    fun verify(code: String, stored: Hashed): Boolean {
        val decoder = Base64.getDecoder()
        val salt = runCatching { decoder.decode(stored.salt) }.getOrNull() ?: return false
        val expected = runCatching { decoder.decode(stored.hash) }.getOrNull() ?: return false
        return MessageDigest.isEqual(derive(code, salt), expected)
    }

    private fun derive(code: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(code.toCharArray(), salt, ITERATIONS, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun newSalt(): ByteArray = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }

    /**
     * Free attempts before the gate starts waiting. After that every failure
     * doubles the wait, starting at 30 seconds and capped at 15 minutes.
     */
    const val FREE_ATTEMPTS = 5

    fun lockoutMillis(failedAttempts: Int): Long {
        if (failedAttempts < FREE_ATTEMPTS) return 0
        val exponent = (failedAttempts - FREE_ATTEMPTS).coerceAtMost(5)
        return (30_000L shl exponent).coerceAtMost(15 * 60_000L)
    }
}
