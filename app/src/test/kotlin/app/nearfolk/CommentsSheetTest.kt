package app.nearfolk

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.geometry.Offset
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.nearfolk.core.designsystem.theme.NearfolkTheme
import app.nearfolk.core.model.Comment
import app.nearfolk.core.model.CommentSort
import app.nearfolk.core.model.CommentsState
import app.nearfolk.core.model.Vote
import app.nearfolk.core.ui.CommentTags
import app.nearfolk.core.ui.CommentsHost
import app.nearfolk.core.ui.CommentsSheet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Records what the comments sheet asks its screen to do. */
class FakeCommentsHost : CommentsHost {
    val calls = mutableListOf<String>()
    override val commentText = TextFieldState()
    override val replyText = TextFieldState()
    override fun openComments(postId: String?) { calls += "open:$postId" }
    override fun sortComments(sort: CommentSort) { calls += "sort:$sort" }
    override fun openReplies(comment: Comment) { calls += "replies:${comment.id}" }
    override fun closeReplies() { calls += "close" }
    override fun replyTo(comment: Comment) { calls += "replyTo:${comment.id}" }
    override fun clearReplyTarget() { calls += "clearTarget" }
    override fun voteComment(comment: Comment, vote: Vote) { calls += "vote:${comment.id}:$vote" }
    override fun sendComment() { calls += "send:${commentText.text}" }
    override fun sendReply() { calls += "sendReply:${replyText.text}" }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h860dp-xhdpi")
class CommentsSheetTest {

    @get:Rule
    val rule = createComposeRule()

    private val host = FakeCommentsHost()
    private val profiles = mutableListOf<String>()
    private val shared = mutableListOf<String>()

    private val list = CommentsState(postId = "p1", postAuthorId = Fixtures.amara.id, total = 8, loading = false, threads = Fixtures.comments)
    private var state by mutableStateOf(list)

    private fun show(initial: CommentsState = list) {
        state = initial
        rule.setContent {
            NearfolkTheme(darkTheme = true) {
                Box(Modifier.fillMaxSize()) {
                    CommentsSheet(state, host, onShare = { shared += it.id }, onOpenProfile = { profiles += it })
                }
            }
        }
        rule.waitForIdle()
    }

    private fun thread(replyingTo: Comment? = null) =
        list.copy(openThread = Fixtures.comments.first(), replies = Fixtures.replies, replyingTo = replyingTo)

    @Test
    fun listShowsScoresRepliesAndSort() {
        show()
        rule.onNodeWithText(Fixtures.comments[0].text).assertIsDisplayed()
        rule.onNodeWithText("24").assertIsDisplayed()
        rule.onNodeWithText("-2").assertIsDisplayed()
        rule.onNodeWithText("3 replies").assertIsDisplayed()
        rule.onNodeWithText("1 reply").assertIsDisplayed()
        rule.onNodeWithText("Author").assertIsDisplayed()
        rule.onNode(hasContentDescription("Score 24")).assertExists()
        rule.onNodeWithText("Newest").performClick()
        assertEquals("sort:Newest", host.calls.last())
    }

    @Test
    fun commentsAreTextOnly() {
        show()
        rule.onNodeWithContentDescription("Add photos, videos or GIFs").assertDoesNotExist()
        rule.onNodeWithTag(CommentTags.Field).performTextInput("Lovely light")
        rule.onNodeWithContentDescription("Send").performClick()
        assertEquals("send:Lovely light", host.calls.last())
    }

    @Test
    fun avatarAndNameOpenTheProfile() {
        show()
        val ines = Fixtures.ines
        rule.onNode(hasContentDescription("Profile photo of ${ines.displayName}") and hasClickAction()).performClick()
        rule.onNode(hasText(ines.displayName) and hasClickAction()).performClick()
        assertEquals(listOf(ines.id, ines.id), profiles)
    }

    @Test
    fun likeDislikeReplyAndShare() {
        show()
        rule.onAllNodesWithContentDescription("Like comment").onFirst().performClick()
        rule.onAllNodesWithContentDescription("Dislike comment").onFirst().performClick()
        // Your own like shows filled, and pressing it again takes it back.
        rule.onNodeWithContentDescription("Remove your like").performClick()
        rule.onAllNodesWithContentDescription("Reply").onFirst().performClick()
        rule.onAllNodesWithContentDescription("Share comment").onFirst().performClick()
        rule.onNodeWithText("3 replies").performClick()
        assertEquals(listOf("vote:c1:Up", "vote:c1:Down", "vote:c2:Up", "replyTo:c1", "replies:c1"), host.calls)
        assertEquals(listOf("c1"), shared)
    }

    @Test
    fun repliesPageShowsTheThreadOldestFirst() {
        show(thread())
        rule.onNodeWithText("Replies").assertIsDisplayed()
        rule.onNodeWithText(Fixtures.comments[0].text).assertIsDisplayed()
        Fixtures.replies.forEach { rule.onNodeWithText(it.text, substring = true).assertIsDisplayed() }
        // The answer to Kai names him in front of the text.
        rule.onNodeWithText("@${Fixtures.kai.displayName} ${Fixtures.replies[2].text}").assertIsDisplayed()
        rule.onNodeWithTag(CommentTags.Field).performTextInput("Thanks")
        rule.onNodeWithContentDescription("Send").performClick()
        assertEquals("sendReply:Thanks", host.calls.last())
    }

    @Test
    fun theNamedPersonOpensTheirProfile() {
        show(thread())
        // "@Kai Nakamura" starts the line; tap it.
        rule.onNodeWithText("@${Fixtures.kai.displayName} ${Fixtures.replies[2].text}")
            .performTouchInput { click(Offset(20f, centerY)) }
        assertEquals(listOf(Fixtures.kai.id), profiles)
    }

    @Test
    fun answeringAReplyShowsWhoAndCanBeCleared() {
        show(thread(replyingTo = Fixtures.replies[0]))
        rule.onNodeWithText("Replying to ${Fixtures.kai.displayName}").assertIsDisplayed()
        rule.onNodeWithContentDescription("Stop replying to ${Fixtures.kai.displayName}").performClick()
        assertEquals("clearTarget", host.calls.last())
    }

    @Test
    fun backLeavesTheRepliesPageBeforeTheSheet() {
        show(thread())
        Espresso.pressBack()
        rule.waitForIdle()
        assertEquals(listOf("close"), host.calls)

        state = list
        rule.waitForIdle()
        rule.onNodeWithText("3 replies").assertIsDisplayed()
        Espresso.pressBack()
        rule.waitForIdle()
        assertTrue(host.calls.contains("open:null"))
        assertFalse(host.calls.count { it == "close" } > 1)
    }
}
