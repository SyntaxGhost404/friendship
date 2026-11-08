package app.nearfolk

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.nearfolk.core.designsystem.theme.NearfolkTheme
import app.nearfolk.core.model.DraftAttachment
import app.nearfolk.core.ui.ComposerTags
import app.nearfolk.core.ui.MessageComposer
import app.nearfolk.core.ui.composerMaxLines
import androidx.compose.runtime.mutableStateOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h640dp-xhdpi")
class ComposerTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun maxLinesFollowTheVisibleHeight() {
        assertEquals(10, composerMaxLines(visibleHeightPx = 2400, lineHeightPx = 60f))
        assertEquals(5, composerMaxLines(visibleHeightPx = 1400, lineHeightPx = 60f))
        assertEquals(3, composerMaxLines(visibleHeightPx = 300, lineHeightPx = 60f))
        assertEquals(12, composerMaxLines(visibleHeightPx = 10_000, lineHeightPx = 60f))
    }

    @Test
    fun growsWithTextThenScrollsWhileActionsStayPut() {
        val text = TextFieldState()
        val attachments = mutableStateOf<List<DraftAttachment>>(emptyList())
        rule.setContent {
            NearfolkTheme(darkTheme = false) {
                Box(Modifier.fillMaxSize()) {
                    MessageComposer(
                        text = text,
                        attachments = attachments.value,
                        onAddMedia = {},
                        onRemoveAttachment = {},
                        onSend = {},
                        placeholder = "Message",
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }
        }
        fun height() = rule.onNodeWithTag(ComposerTags.Container).getBoundsInRoot().height
        val send = rule.onNodeWithContentDescription("Send")

        val empty = height()
        send.assertIsDisplayed().assertIsNotEnabled()

        rule.runOnIdle { text.setTextAndPlaceCursorAtEnd("H") }
        send.assertIsDisplayed().assertIsEnabled()

        rule.runOnIdle { text.setTextAndPlaceCursorAtEnd("one\ntwo\nthree") }
        val three = height()
        assertTrue("composer should grow with text", three > empty)

        rule.runOnIdle { text.setTextAndPlaceCursorAtEnd((1..60).joinToString("\n") { "line $it" }) }
        val capped = height()
        rule.runOnIdle { text.setTextAndPlaceCursorAtEnd((1..200).joinToString("\n") { "line $it" }) }
        assertEquals("height stops growing at the line limit", capped, height())
        assertTrue("composer must leave most of the screen to the conversation", capped < 640.dp * 0.45f)

        // The action row is fully inside the composer and on screen.
        val container = rule.onNodeWithTag(ComposerTags.Container).getBoundsInRoot()
        val actions = rule.onNodeWithTag(ComposerTags.Actions).getBoundsInRoot()
        assertTrue(actions.bottom <= container.bottom && actions.top >= container.top)
        send.assertIsDisplayed().assertIsEnabled()

        // Media still being copied keeps send dimmed even with text.
        rule.runOnIdle { attachments.value = listOf(DraftAttachment.Importing("x")) }
        send.assertIsDisplayed().assertIsNotEnabled()
    }
}
