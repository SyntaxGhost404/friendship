package app.nearfolk

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.nearfolk.core.designsystem.theme.NearfolkTheme
import app.nearfolk.core.model.Post
import app.nearfolk.core.ui.PostActions
import app.nearfolk.core.ui.PostItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant
import kotlin.math.abs

/**
 * The five actions under a post share the width equally, show their counts
 * compactly and keep them in step with the post.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h640dp-xhdpi")
class PostActionsTest {

    @get:Rule
    val rule = createComposeRule()

    private val now = Instant.parse("2026-11-10T12:00:00Z")
    private var post by mutableStateOf(
        Post(
            "p1", "m1", "Amara Okafor", null, "Found this spot by accident.", null, now.minusSeconds(600),
            likeCount = 42, commentCount = 3, likedByMe = false, viewCount = 15_320, saveCount = 1_240, shareCount = 7,
        ),
    )
    private val calls = mutableListOf<String>()

    private val actions = PostActions(
        onToggleLike = { calls += "like:${it.id}" },
        onOpenComments = { calls += "comments:${it.id}" },
        onToggleSave = { calls += "save:${it.id}" },
        onShare = { calls += "share:${it.id}" },
        onOpenStats = { calls += "stats:${it.id}" },
    )

    private fun show() {
        rule.setContent {
            NearfolkTheme(darkTheme = true) {
                Box(Modifier.padding(horizontal = 20.dp).testTag("post")) {
                    PostItem(post, actions, now = now)
                }
            }
        }
        rule.waitForIdle()
    }

    private fun action(description: String): SemanticsNodeInteraction =
        rule.onNode(hasContentDescription(description) and hasClickAction())

    private val labels = listOf("Like", "Comments", "Views", "Save", "Share")

    @Test
    fun fiveActionsShareTheWidthEqually() {
        show()
        val post = rule.onNodeWithTag("post").getBoundsInRoot()
        val cells = labels.map { action(it).assertIsDisplayed().getBoundsInRoot() }
        val widths = cells.map { (it.right - it.left).value }
        widths.forEach { assertEquals("equal widths $widths", widths.first(), it, 1f) }
        assertEquals("starts at the post's left edge", post.left.value, cells.first().left.value, 1f)
        assertEquals("ends at the post's right edge", post.right.value, cells.last().right.value, 1f)
        cells.zipWithNext { a, b -> assertTrue("cells touch", abs((b.left - a.right).value) < 1f) }
        cells.forEach { assertEquals("one row", cells.first().top.value, it.top.value, 1f) }
    }

    @Test
    fun countsAreCompactAndReadInFull() {
        show()
        rule.onNodeWithText("42").assertIsDisplayed()
        rule.onNodeWithText("3").assertIsDisplayed()
        rule.onNodeWithText("15K").assertIsDisplayed()
        rule.onNodeWithText("1.2K").assertIsDisplayed()
        rule.onNodeWithText("7").assertIsDisplayed()
        // Screen readers hear the whole number.
        rule.onNode(hasContentDescription("15,320 views", substring = false)).assertExists()
        rule.onNode(hasContentDescription("1,240 saves")).assertExists()
        rule.onNode(hasContentDescription("7 shares")).assertExists()
    }

    @Test
    fun everyActionCallsBackWithThePost() {
        show()
        labels.forEach { action(it).performClick() }
        assertEquals(listOf("like:p1", "comments:p1", "stats:p1", "save:p1", "share:p1"), calls)
    }

    @Test
    fun countsAndStatesFollowThePost() {
        show()
        post = post.copy(likedByMe = true, likeCount = 43, savedAt = now, saveCount = 1_241, shareCount = 8, viewCount = 15_321)
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(1_000)
        action("Remove like").assertIsDisplayed()
        action("Remove from saved").assertIsDisplayed()
        rule.onNodeWithText("43").assertIsDisplayed()
        rule.onNodeWithText("8").assertIsDisplayed()
        rule.onNode(hasContentDescription("1,241 saves")).assertExists()
    }
}
