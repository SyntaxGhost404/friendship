package app.nearfolk.core.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.click
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.nearfolk.core.designsystem.component.NfBottomSheet
import app.nearfolk.core.designsystem.theme.NearfolkTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The sheet may only ever sit at its resting place or below it. Flinging its
 * content against either edge, or dragging the sheet itself upward, must not
 * lift, stretch or resize it.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h640dp-xhdpi")
class BottomSheetTest {

    @get:Rule
    val rule = createComposeRule()

    private var open by mutableStateOf(true)
    private var closeOnDismiss = true
    private var dismissals = 0
    private val list = LazyListState()

    private fun show() {
        rule.setContent {
            NearfolkTheme(darkTheme = true) {
                Box(Modifier.fillMaxSize())
                if (open) {
                    NfBottomSheet(
                        onDismiss = {
                            dismissals++
                            if (closeOnDismiss) open = false
                        },
                        modifier = Modifier.testTag("sheet"),
                        title = "Countries",
                    ) {
                        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp).testTag("list"), state = list) {
                            items(Rows) { Text("Row $it", Modifier.fillMaxWidth().padding(16.dp)) }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
    }

    private fun sheet() = rule.onNodeWithTag("sheet")

    private fun bounds(): Rect = sheet().fetchSemanticsNode().boundsInRoot

    private fun windowHeight(): Int =
        sheet().fetchSemanticsNode().layoutInfo.coordinates.findRootCoordinates().size.height

    /** Steps the clock frame by frame and checks the sheet against [rest] on every one. */
    private fun assertStaysAt(rest: Rect, frames: Int = 90) {
        repeat(frames) {
            rule.mainClock.advanceTimeByFrame()
            assertEquals(rest, bounds())
        }
    }

    @Test
    fun restsAtTheBottomOfTheWindow() {
        show()
        val rest = bounds()
        assertEquals(windowHeight().toFloat(), rest.bottom, 0.5f)
        assertTrue("The sheet leaves room above itself", rest.top > 0f)
        assertEquals(0, dismissals)
    }

    @Test
    fun flingAgainstTheEndOfTheListDoesNotMoveTheSheet() {
        show()
        val rest = bounds()
        rule.onNodeWithTag("list").performScrollToIndex(Rows - 1)
        rule.waitForIdle()
        rule.mainClock.autoAdvance = false
        repeat(3) {
            rule.onNodeWithTag("list").performTouchInput { swipeUp(durationMillis = 40) }
            assertStaysAt(rest)
        }
        assertEquals(0, dismissals)
    }

    @Test
    fun flingAgainstTheTopOfTheListDoesNotMoveTheSheet() {
        show()
        val rest = bounds()
        rule.onNodeWithTag("list").performScrollToIndex(6)
        rule.waitForIdle()
        rule.mainClock.autoAdvance = false
        // A short, fast swipe: the finger lifts before the list reaches its top, the fling gets there.
        rule.onNodeWithTag("list").performTouchInput {
            swipeDown(startY = centerY, endY = centerY + 160f, durationMillis = 30)
        }
        assertStaysAt(rest, frames = 180)
        assertEquals("The fling reached the top", 0, list.firstVisibleItemIndex)
        assertEquals(0, list.firstVisibleItemScrollOffset)
        assertEquals(0, dismissals)
    }

    @Test
    fun longFastScrollThroughTheListDoesNotMoveTheSheet() {
        show()
        val rest = bounds()
        rule.mainClock.autoAdvance = false
        repeat(4) {
            rule.onNodeWithTag("list").performTouchInput { swipeUp(durationMillis = 30) }
            assertStaysAt(rest, frames = 12)
        }
        assertStaysAt(rest, frames = 240)
        assertEquals(0, dismissals)
    }

    @Test
    fun draggingTheSheetUpwardDoesNothing() {
        show()
        val rest = bounds()
        rule.mainClock.autoAdvance = false
        sheet().performTouchInput {
            down(Offset(centerX, 30f))
            repeat(10) { moveBy(Offset(0f, -40f)) }
        }
        assertEquals(rest, bounds())
        sheet().performTouchInput { up() }
        assertStaysAt(rest)
        assertEquals(0, dismissals)
    }

    @Test
    fun smallPullDownFollowsTheFingerThenReturns() {
        show()
        val rest = bounds()
        rule.mainClock.autoAdvance = false
        sheet().performTouchInput {
            down(Offset(centerX, 30f))
            repeat(10) { moveBy(Offset(0f, 12f), delayMillis = 40) }
        }
        val pulled = bounds()
        assertTrue("The sheet follows the finger down", pulled.top > rest.top + 40f)
        assertEquals("The sheet moves, it does not resize", rest.height, pulled.height, 0.5f)
        sheet().performTouchInput {
            // Hold still so the release carries no speed.
            repeat(5) { moveBy(Offset.Zero, delayMillis = 40) }
            up()
        }
        var lowest = pulled.top
        repeat(120) {
            rule.mainClock.advanceTimeByFrame()
            val top = bounds().top
            assertTrue("The sheet never rises above rest", top >= rest.top)
            assertTrue("The sheet only moves back up", top <= lowest + 0.5f)
            lowest = top
        }
        assertEquals(rest, bounds())
        assertEquals(0, dismissals)
    }

    @Test
    fun pullingDownThroughAListAtItsTopMovesTheSheet() {
        show()
        val rest = bounds()
        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag("list").performTouchInput {
            down(center)
            repeat(10) { moveBy(Offset(0f, 12f), delayMillis = 40) }
        }
        assertTrue(bounds().top > rest.top + 40f)
        // Moving the finger back up returns the sheet before the list scrolls.
        rule.onNodeWithTag("list").performTouchInput {
            repeat(20) { moveBy(Offset(0f, -12f), delayMillis = 40) }
        }
        assertEquals(rest, bounds())
        rule.onNodeWithTag("list").performTouchInput {
            repeat(5) { moveBy(Offset.Zero, delayMillis = 40) }
            up()
        }
        assertStaysAt(rest)
        assertEquals(0, dismissals)
    }

    @Test
    fun swipeDownDismisses() {
        show()
        sheet().performTouchInput { swipeDown(startY = 30f, durationMillis = 120) }
        rule.waitForIdle()
        assertEquals(1, dismissals)
        sheet().assertDoesNotExist()
    }

    @Test
    fun tapOutsideDismisses() {
        show()
        rule.onNode(isDialog()).performTouchInput { click(Offset(centerX, 20f)) }
        rule.waitForIdle()
        assertEquals(1, dismissals)
        sheet().assertDoesNotExist()
    }

    @Test
    fun backDismisses() {
        show()
        Espresso.pressBack()
        rule.waitForIdle()
        assertEquals(1, dismissals)
        sheet().assertDoesNotExist()
    }

    @Test
    fun accessibilityDismissActionDismisses() {
        show()
        sheet().performSemanticsAction(SemanticsActions.Dismiss)
        rule.waitForIdle()
        assertEquals(1, dismissals)
        sheet().assertDoesNotExist()
    }

    @Test
    fun returnsToRestWhenTheCallerKeepsItOpen() {
        closeOnDismiss = false
        show()
        val rest = bounds()
        sheet().performTouchInput { swipeDown(startY = 30f, durationMillis = 120) }
        rule.waitForIdle()
        assertEquals(1, dismissals)
        assertEquals(rest, bounds())
    }

    private companion object {
        const val Rows = 60
    }
}
