package app.nearfolk.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FuzzyMatcherTest {

    @Test
    fun exactAndPrefixRankAboveTypos() {
        val exact = FuzzyMatcher.score("amara", "Amara")
        val prefix = FuzzyMatcher.score("ama", "Amara")
        val typo = FuzzyMatcher.score("anara", "Amara")
        assertEquals(1.0, exact, 0.0)
        assertTrue(prefix > typo)
        assertTrue(typo > 0.0)
    }

    @Test
    fun accentsAndCaseAreIgnored() {
        assertEquals(1.0, FuzzyMatcher.score("JOSE", "José"), 0.0)
        assertTrue(FuzzyMatcher.score("soren", "Søren Holm") >= 0.9)
    }

    @Test
    fun adjacentSwapCountsAsOneEdit() {
        assertEquals(1, FuzzyMatcher.osaDistance("lucsa", "lucas"))
        assertTrue(FuzzyMatcher.score("lucsa", "Lucas") > 0.5)
    }

    @Test
    fun shortQueriesDoNotMatchLoosely() {
        assertEquals(0.0, FuzzyMatcher.score("zq", "Amara"), 0.0)
    }

    @Test
    fun multiWordQueryNeedsEveryWord() {
        assertTrue(FuzzyMatcher.score("noor ab", "Noor Abadi") > 0.8)
        assertEquals(0.0, FuzzyMatcher.score("noor xyzzy", "Noor Abadi"), 0.0)
    }

    @Test
    fun rankOrdersBestFirst() {
        val names = listOf("Marisol", "Mari", "Omar", "Tamara")
        val ranked = FuzzyMatcher.rank("mari", names) { it }
        assertEquals(listOf("Mari", "Marisol"), ranked.take(2))
    }
}
