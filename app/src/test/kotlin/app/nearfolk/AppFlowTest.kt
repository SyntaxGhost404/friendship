package app.nearfolk

import android.content.Intent
import android.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.nearfolk.feature.feed.FeedTags
import app.nearfolk.feature.you.SavedTags
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

/**
 * Drives the real app: dependency graph, database, preferences, navigation
 * and the local community. Creates a profile, finds a member with a misspelt
 * search, opens their profile, sends a message and waits for it to be read,
 * then comments on a post in the feed, likes and replies to a comment, opens a
 * commenter's profile, saves and shares a post and opens its author's profile.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h860dp-xhdpi")
class AppFlowTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun waitForText(text: String, timeout: Long = 10_000) =
        rule.waitUntilAtLeastOneExists(hasText(text), timeout)

    private fun luminance(c: Int) =
        (0.2126 * Color.red(c) + 0.7152 * Color.green(c) + 0.0722 * Color.blue(c)) / 255.0

    /**
     * Samples the button surface (inside its padding, away from the label) and
     * compares it with the screen background. A vanished button matches the
     * background exactly.
     */
    private fun buttonContrast(node: SemanticsNodeInteraction): Double {
        rule.mainClock.advanceTimeBy(600)
        rule.waitForIdle()
        val button = node.captureToImage().asAndroidBitmap()
        val screen = rule.onRoot().captureToImage().asAndroidBitmap()
        val surface = button.getPixel(5, button.height / 2)
        val background = screen.getPixel(2, screen.height / 3)
        return abs(luminance(surface) - luminance(background))
    }

    private fun continueButton() = rule.onNode(hasText("Continue") and hasClickAction())

    /** Visible either way, and clearly brighter once the step is complete. */
    private fun assertContinue(enabled: Boolean) {
        val contrast = buttonContrast(continueButton())
        assertTrue("Continue button is not visible (contrast $contrast)", contrast > 0.08)
        if (enabled) continueButton().assertIsEnabled() else continueButton().assertIsNotEnabled()
    }

    @Test
    fun onboardingToFirstConversation() {
        waitForText("Create a profile")
        rule.onNodeWithText("Create a profile").performClick()

        assertContinue(enabled = false)
        rule.onNode(hasSetTextAction()).performTextInput("R")
        assertContinue(enabled = false)
        rule.onNode(hasSetTextAction()).performTextInput("ae")
        assertContinue(enabled = false)
        rule.onNodeWithText("Female").performClick()
        assertContinue(enabled = true)
        rule.onNodeWithText("Continue").performClick()

        waitForText("When were you born?")
        assertContinue(enabled = false)
        rule.onNodeWithText("Pick a date").performClick()
        rule.waitUntilAtLeastOneExists(hasContentDescription("Switch to text input mode", substring = true, ignoreCase = true))
        rule.onNodeWithContentDescription("Switch to text input mode", substring = true, ignoreCase = true).performClick()
        rule.onNode(hasSetTextAction()).performTextInput("05031996")
        rule.onNodeWithText("Done").performClick()
        assertContinue(enabled = true)
        rule.onNodeWithText("Continue").performClick()

        waitForText("A few ground rules")
        assertContinue(enabled = false)
        rule.onNodeWithText("I agree to follow the community guidelines").performClick()
        assertContinue(enabled = true)
        rule.onRoot().captureRoboImage("build/screenshots/flow_0_rules.png")
        rule.onNodeWithText("Continue").performClick()

        waitForText("Not now")
        rule.onNodeWithText("Not now").performClick()

        // Main app with the seeded community.
        waitForText("Search by name", timeout = 15_000)
        rule.waitUntilAtLeastOneExists(hasText("online now", substring = true), 15_000)
        rule.onRoot().captureRoboImage("build/screenshots/flow_1_discover.png")

        // Typo tolerant search: "amra" still finds Amara.
        rule.onNode(hasSetTextAction()).performTextInput("amra")
        waitForText("Amara Okafor")
        rule.onAllNodesWithText("Amara Okafor").onFirst().performClick()

        waitForText("Message")
        rule.onRoot().captureRoboImage("build/screenshots/flow_2_profile.png")
        rule.onNodeWithText("Message").performClick()

        waitForText("Stay safe")
        val send = rule.onNodeWithContentDescription("Send")
        send.assertIsNotEnabled()
        assertTrue("Send button hidden while empty", buttonContrast(send) > 0.08)
        rule.onNode(hasSetTextAction()).performTextInput("H")
        send.assertIsEnabled()
        assertTrue("Send button vanished while typing", buttonContrast(send) > 0.08)
        rule.onNode(hasSetTextAction()).performTextInput("i Amara, how is your week going?")
        rule.onRoot().captureRoboImage("build/screenshots/flow_3a_typing.png")
        send.performClick()
        waitForText("Hi Amara, how is your week going?")

        // The other side reads it within a few seconds.
        rule.waitUntilAtLeastOneExists(hasContentDescription("Read"), 20_000)
        rule.onRoot().captureRoboImage("build/screenshots/flow_3_chat.png")

        // Back to the tabs, then the feed: everyone's posts in one timeline.
        Espresso.pressBack()
        waitForText("Message")
        Espresso.pressBack()
        val feedTab = hasText("Feed") and hasClickAction()
        rule.waitUntilAtLeastOneExists(feedTab, 10_000)
        rule.onNode(feedTab).performClick()
        rule.waitUntilAtLeastOneExists(hasTestTag(FeedTags.List), 10_000)
        rule.onRoot().captureRoboImage("build/screenshots/flow_4_feed.png")

        // Comments open in a sheet. They are text only: no attachment button.
        rule.onAllNodes(hasContentDescription("Comments") and hasClickAction()).onFirst().performClick()
        waitForText("Add a comment")
        rule.onNodeWithContentDescription("Add photos, videos or GIFs").assertDoesNotExist()
        rule.onNode(hasSetTextAction()).performTextInput("Lovely light in this one.")
        rule.onNodeWithContentDescription("Send").performClick()
        waitForText("Lovely light in this one.")
        captureScreenRoboImage("build/screenshots/flow_5_feed_comments.png")

        // Liking a comment fills the thumb; pressing again takes it back.
        rule.onAllNodes(hasContentDescription("Like comment") and hasClickAction()).onFirst().performClick()
        rule.waitUntilAtLeastOneExists(hasContentDescription("Remove your like"), 5_000)

        // Reply opens the thread with the reply box ready; the reply lands in it.
        rule.onAllNodes(hasContentDescription("Reply") and hasClickAction()).onFirst().performClick()
        waitForText("Add a reply")
        rule.onNode(hasSetTextAction()).performTextInput("Thanks for reading.")
        rule.onNodeWithContentDescription("Send").performClick()
        waitForText("Thanks for reading.")
        captureScreenRoboImage("build/screenshots/flow_5b_replies.png")

        // Back steps out of the thread first, which now shows its reply count.
        Espresso.pressBack()
        waitForText("1 reply")
        waitForText("Add a comment")

        // A commenter's name opens their profile; back returns to the comments as they were.
        rule.onNode(hasText("Rae") and hasClickAction()).performClick()
        waitForText("This is how other people see your profile.")
        Espresso.pressBack()
        waitForText("Add a comment")
        Espresso.pressBack()
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Add a comment").fetchSemanticsNodes().isEmpty() }

        // Saving a post offers the way to the saved list under You.
        rule.onAllNodes(hasContentDescription("Save") and hasClickAction()).onFirst().performClick()
        waitForText("Saved. Find it under You.")
        rule.onNodeWithText("View").performClick()
        rule.waitUntilAtLeastOneExists(hasTestTag(SavedTags.List), 10_000)
        rule.onRoot().captureRoboImage("build/screenshots/flow_6_saved.png")

        // Taking it out of the list can be undone.
        rule.onNode(hasContentDescription("Remove from saved") and hasClickAction()).performClick()
        waitForText("Nothing saved yet")
        rule.onNodeWithText("Undo").performClick()
        rule.waitUntilAtLeastOneExists(hasTestTag(SavedTags.List), 10_000)
        Espresso.pressBack()
        rule.waitUntilAtLeastOneExists(hasTestTag(FeedTags.List), 10_000)

        // Share hands the post to the system share sheet.
        rule.onAllNodes(hasContentDescription("Share") and hasClickAction()).onFirst().performClick()
        val shared = Shadows.shadowOf(rule.activity).nextStartedActivity
        assertEquals(Intent.ACTION_CHOOSER, shared.action)

        // The author of a post opens their profile.
        rule.onAllNodes(opensProfile).onFirst().performClick()
        waitForText("Message")
    }

    private val opensProfile = SemanticsMatcher("opens a profile") {
        it.config.getOrNull(SemanticsActions.OnClick)?.label == "Open profile"
    }
}
