package app.nearfolk

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.nearfolk.core.designsystem.theme.NearfolkTheme
import app.nearfolk.core.model.Post
import app.nearfolk.core.model.SELF_ID
import app.nearfolk.feature.feed.FeedScreen
import app.nearfolk.feature.feed.FeedTags
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h860dp-xhdpi")
class FeedTest {

    @get:Rule
    val rule = createComposeRule()

    private val start = Instant.parse("2026-11-09T12:00:00Z")
    private var timeline = (1..30).map { post("p$it", "m$it", minutesAgo = it * 10L) }
    private val pages = MutableStateFlow(PagingData.from(timeline))
    private val opened = mutableListOf<String>()

    private fun post(id: String, author: String, minutesAgo: Long) =
        Post(id, author, "Member $author", null, "Post $id", null, start.minusSeconds(minutesAgo * 60), 0, 0, false)

    private fun publish(post: Post) {
        timeline = listOf(post) + timeline
        pages.value = PagingData.from(timeline)
        rule.waitForIdle()
    }

    private fun show() {
        rule.setContent {
            NearfolkTheme(darkTheme = false) {
                Box(Modifier.fillMaxSize()) {
                    FeedScreen(
                        posts = pages.collectAsLazyPagingItems(),
                        refreshing = false,
                        onRefresh = {},
                        onToggleLike = {},
                        onOpenComments = {},
                        onOpenMember = { opened += it },
                        onNewPost = {},
                        now = start,
                    )
                }
            }
        }
        rule.waitForIdle()
    }

    private fun list() = rule.onNodeWithTag(FeedTags.List)

    @Test
    fun newPostsWaitBehindAPillWhileReadingFurtherDown() {
        show()
        list().performScrollToIndex(12)
        rule.onNodeWithText("New posts").assertDoesNotExist()

        publish(post("fresh", "m99", minutesAgo = 0))
        rule.onNodeWithText("Post p13").assertIsDisplayed()
        rule.onNodeWithText("New posts").assertIsDisplayed().performClick()
        rule.waitForIdle()

        rule.onNodeWithText("Post fresh").assertIsDisplayed()
        rule.onNodeWithText("New posts").assertDoesNotExist()
    }

    @Test
    fun yourOwnPostIsShownRightAway() {
        show()
        list().performScrollToIndex(12)
        publish(post("mine", SELF_ID, minutesAgo = 0))
        rule.onNodeWithText("Post mine").assertIsDisplayed()
        rule.onNodeWithText("New posts").assertDoesNotExist()
    }

    @Test
    fun tappingTheAuthorOpensTheirProfile() {
        show()
        rule.onNodeWithText("Member m1").performTouchInput { click() }
        assertEquals(listOf("m1"), opened)
    }
}
