package app.nearfolk

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Drives the real app: dependency graph, database, preferences, navigation
 * and the local community. Creates a profile, finds a member with a misspelt
 * search, opens their profile, sends a message and waits for it to be read.
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

    @Test
    fun onboardingToFirstConversation() {
        waitForText("Create a profile")
        rule.onNodeWithText("Create a profile").performClick()

        rule.onNode(hasSetTextAction()).performTextInput("Rae")
        rule.onNodeWithText("Female").performClick()
        rule.onNodeWithText("Continue").performClick()

        waitForText("When were you born?")
        rule.onNodeWithText("Pick a date").performClick()
        rule.waitUntilAtLeastOneExists(hasContentDescription("Switch to text input mode", substring = true, ignoreCase = true))
        rule.onNodeWithContentDescription("Switch to text input mode", substring = true, ignoreCase = true).performClick()
        rule.onNode(hasSetTextAction()).performTextInput("05031996")
        rule.onNodeWithText("Done").performClick()
        rule.onNodeWithText("Continue").performClick()

        waitForText("A few ground rules")
        rule.onNodeWithText("I agree to follow the community guidelines").performClick()
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
        rule.onNode(hasSetTextAction()).performTextInput("Hi Amara, how is your week going?")
        rule.onNodeWithContentDescription("Send").performClick()
        waitForText("Hi Amara, how is your week going?")

        // The other side reads it within a few seconds.
        rule.waitUntilAtLeastOneExists(hasContentDescription("Read"), 20_000)
        rule.onRoot().captureRoboImage("build/screenshots/flow_3_chat.png")
    }
}
