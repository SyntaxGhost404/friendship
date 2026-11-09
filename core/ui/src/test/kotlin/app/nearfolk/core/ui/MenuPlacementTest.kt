package app.nearfolk.core.ui

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MenuPlacementTest {

    private val window = IntSize(800, 1720)
    private val menu = IntSize(368, 248)

    private fun place(anchor: IntRect, content: IntSize = menu, rtl: Boolean = false) =
        placeMenu(anchor, window, content, rtl, inset = 16, gap = 8, sideMargin = 16, edgeMargin = 96)

    @Test
    fun opensBelowTheAnchorAlignedToItsEndEdge() {
        val anchor = IntRect(704, 96, 784, 176)
        val placement = place(anchor)
        assertFalse(placement.above)
        assertEquals("End edges line up", anchor.right, placement.offset.x + menu.width)
        assertEquals("The surface starts one gap under the anchor", anchor.bottom + 8, placement.offset.y + 16)
    }

    @Test
    fun alignsToTheLeftEdgeInRightToLeftLayouts() {
        val anchor = IntRect(16, 96, 96, 176)
        assertEquals(anchor.left, place(anchor, rtl = true).offset.x)
    }

    @Test
    fun opensAboveWhenThereIsNoRoomBelow() {
        val anchor = IntRect(704, 1560, 784, 1640)
        val placement = place(anchor)
        assertTrue(placement.above)
        assertEquals("The surface ends one gap over the anchor", anchor.top - 8, placement.offset.y + menu.height - 16)
    }

    @Test
    fun staysInsideTheWindowSideways() {
        val nearLeft = place(IntRect(40, 96, 120, 176))
        assertEquals(16, nearLeft.offset.x)
        val pastRight = place(IntRect(760, 96, 840, 176))
        assertEquals(window.width - menu.width - 16, pastRight.offset.x)
    }

    @Test
    fun tallMenuIsPinnedBelowTheTopMargin() {
        val tall = IntSize(368, 2000)
        val placement = place(IntRect(704, 800, 784, 880), content = tall)
        assertFalse(placement.above)
        assertEquals(IntOffset(784 - 368, 96 - 16), placement.offset)
    }

    @Test
    fun anchorUnderTheStatusBarStillClearsTheTopMargin() {
        val placement = place(IntRect(704, 0, 784, 40))
        assertEquals(96, placement.offset.y + 16)
    }
}
