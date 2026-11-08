package app.nearfolk.core.common

import java.text.Normalizer

/**
 * Typo tolerant name matching for member search.
 *
 * Scores fall in 0.0 to 1.0. Exact and prefix matches rank highest, then
 * substring matches, then matches within a small edit distance, then loose
 * subsequence matches such as initials. Multi word queries require every word
 * to match some part of the candidate.
 */
object FuzzyMatcher {

    private val combiningMarks = Regex("\\p{Mn}+")
    private val separators = Regex("[^\\p{L}\\p{N}]+")
    private val foldedLetters = mapOf(
        'ø' to "o", 'ß' to "ss", 'đ' to "d", 'ł' to "l", 'æ' to "ae", 'œ' to "oe", 'ı' to "i", 'þ' to "th",
    )

    fun normalize(input: String): String {
        val decomposed = Normalizer.normalize(input.lowercase(), Normalizer.Form.NFD)
        val stripped = combiningMarks.replace(decomposed, "")
        val folded = buildString(stripped.length) {
            stripped.forEach { ch -> append(foldedLetters[ch] ?: ch) }
        }
        return separators.replace(folded, " ").trim()
    }

    fun score(query: String, candidate: String): Double {
        val q = normalize(query)
        if (q.isEmpty()) return 1.0
        val c = normalize(candidate)
        if (c.isEmpty()) return 0.0
        val queryTokens = q.split(' ')
        val candidateTokens = c.split(' ')
        if (queryTokens.size == 1) return scoreToken(q, c, candidateTokens)
        var total = 0.0
        for (token in queryTokens) {
            val s = scoreToken(token, c, candidateTokens)
            if (s == 0.0) return 0.0
            total += s
        }
        return total / queryTokens.size
    }

    fun <T> rank(query: String, items: List<T>, minScore: Double = 0.3, key: (T) -> String): List<T> {
        if (normalize(query).isEmpty()) return items
        return items
            .map { it to score(query, key(it)) }
            .filter { it.second >= minScore }
            .sortedByDescending { it.second }
            .map { it.first }
    }

    private fun scoreToken(q: String, full: String, tokens: List<String>): Double {
        if (full == q) return 1.0
        if (full.startsWith(q)) return 0.95
        if (tokens.any { it.startsWith(q) }) return 0.9
        if (full.contains(q)) return 0.8

        val allowed = allowedEdits(q.length)
        if (allowed > 0) {
            var best = Int.MAX_VALUE
            for (token in tokens) {
                best = minOf(best, osaDistance(q, token))
                if (token.length > q.length) best = minOf(best, osaDistance(q, token.take(q.length)))
            }
            if (best <= allowed) return 0.75 - 0.1 * best
        }

        if (q.length >= 2 && isSubsequence(q, full)) {
            return 0.3 + 0.2 * (q.length.toDouble() / full.replace(" ", "").length).coerceAtMost(1.0)
        }
        return 0.0
    }

    private fun allowedEdits(length: Int): Int = when {
        length <= 2 -> 0
        length <= 5 -> 1
        else -> 2
    }

    private fun isSubsequence(q: String, text: String): Boolean {
        var i = 0
        for (ch in text) {
            if (i < q.length && ch == q[i]) i++
        }
        return i == q.length
    }

    /** Optimal string alignment distance: insertions, deletions, substitutions and adjacent swaps. */
    internal fun osaDistance(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        val rows = a.length + 1
        val cols = b.length + 1
        val d = Array(rows) { IntArray(cols) }
        for (i in 0 until rows) d[i][0] = i
        for (j in 0 until cols) d[0][j] = j
        for (i in 1 until rows) {
            for (j in 1 until cols) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                var v = minOf(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + cost)
                if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) {
                    v = minOf(v, d[i - 2][j - 2] + 1)
                }
                d[i][j] = v
            }
        }
        return d[a.length][b.length]
    }
}
