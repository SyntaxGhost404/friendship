package app.nearfolk.core.designsystem.shape

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

class SquircleGeometryTest {

    @Test
    fun zeroSmoothingIsAPlainRoundedCorner() {
        val g = CornerGeometry.of(radius = 20f, smoothing = 0f, budget = 100f)
        assertEquals(20f, g.extent, 1e-4f)
        assertEquals(0f, g.a, 1e-4f)
        assertEquals(0f, g.b, 1e-4f)
        assertEquals(90f, g.sweepDegrees, 1e-4f)
    }

    @Test
    fun smoothingConsumesMoreEdgeAndShortensArc() {
        val g = CornerGeometry.of(radius = 20f, smoothing = 0.6f, budget = 100f)
        assertEquals(32f, g.extent, 1e-3f)
        assertEquals(36f, g.sweepDegrees, 1e-3f)
        assertTrue(g.a > g.b && g.b > 0f)
    }

    @Test
    fun rampEndsExactlyOnTheArc() {
        // The first ramp must land on the circle of radius r around the corner centre.
        val r = 24f
        val g = CornerGeometry.of(radius = r, smoothing = 0.6f, budget = 200f)
        val su = -g.extent + g.a + g.b + g.c
        val sv = g.d
        val distance = hypot(su - (-r), sv - r)
        assertEquals(r, distance, 1e-3f)
        // and at the angle where the arc starts
        val angle = Math.toRadians((-90f + g.trimDegrees).toDouble())
        assertEquals(-r + r * cos(angle).toFloat(), su, 1e-3f)
        assertEquals(r + r * sin(angle).toFloat(), sv, 1e-3f)
    }

    @Test
    fun smallBoxesReduceSmoothingInsteadOfOverlapping() {
        val g = CornerGeometry.of(radius = 20f, smoothing = 0.6f, budget = 24f)
        assertTrue(g.extent <= 24f + 1e-4f)
        assertEquals(20f, g.radius, 1e-4f)
    }

    @Test
    fun hugeRadiusBecomesStadium() {
        val g = CornerGeometry.of(radius = 999f, smoothing = 0.6f, budget = 20f)
        assertEquals(20f, g.radius, 1e-4f)
        assertEquals(20f, g.extent, 1e-4f)
        assertEquals(90f, g.sweepDegrees, 1e-4f)
    }

    @Test
    fun outlineStaysInsideTheBox() {
        val points = mutableListOf<Pair<Float, Float>>()
        val sink = object : PathSink {
            override fun moveTo(x: Float, y: Float) { points += x to y }
            override fun lineTo(x: Float, y: Float) { points += x to y }
            override fun cubicTo(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) {
                points += x1 to y1; points += x2 to y2; points += x3 to y3
            }
            override fun arc(cx: Float, cy: Float, r: Float, startDegrees: Float, sweepDegrees: Float) {
                val end = Math.toRadians((startDegrees + sweepDegrees).toDouble())
                points += (cx + r * cos(end).toFloat()) to (cy + r * sin(end).toFloat())
            }
            override fun close() = Unit
        }
        emitSquircle(sink, 120f, 80f, 20f, 30f, 10f, 0f, 0.6f)
        assertTrue(points.isNotEmpty())
        points.forEach { (x, y) ->
            assertTrue("x=$x", x in -1e-3f..120.001f)
            assertTrue("y=$y", y in -1e-3f..80.001f)
        }
    }
}
