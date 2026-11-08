package app.nearfolk.core.designsystem.shape

import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Geometry for one continuous curvature corner.
 *
 * A plain rounded corner joins a straight edge to a quarter circle, so the
 * curvature jumps from zero to 1/r at the seam. Here part of the circular arc
 * is replaced at both ends by a cubic Bezier ramp whose first three control
 * points sit on the edge (zero curvature where it leaves the edge) and whose
 * last segment is tangent to the arc. The arc keeps its centre, so radius
 * values mean the same thing as in a regular rounded rectangle.
 *
 * All lengths are measured along the edge from the corner apex.
 *
 * @property radius effective radius after clamping
 * @property extent length consumed on each edge, (1 + smoothing) * radius
 * @property a distance from the ramp start to the first handle
 * @property b distance between the two handles on the edge
 * @property c edge parallel offset from the last handle to the arc
 * @property d edge normal offset from the last handle to the arc
 * @property trimDegrees arc angle removed from each end of the quarter circle
 * @property sweepDegrees remaining circular arc, 90 * (1 - smoothing)
 */
internal data class CornerGeometry(
    val radius: Float,
    val extent: Float,
    val a: Float,
    val b: Float,
    val c: Float,
    val d: Float,
    val trimDegrees: Float,
    val sweepDegrees: Float,
) {
    val isSharp: Boolean get() = radius <= 0f

    companion object {
        val Sharp = CornerGeometry(0f, 0f, 0f, 0f, 0f, 0f, 0f, 90f)

        /**
         * @param budget the most this corner may consume on either edge,
         * normally half of the shorter side.
         */
        fun of(radius: Float, smoothing: Float, budget: Float): CornerGeometry {
            val r = min(radius, budget)
            if (r <= 0f) return Sharp
            var s = smoothing.coerceIn(0f, 1f)
            var extent = (1f + s) * r
            if (extent > budget) {
                // Not enough edge for the full ramp: smooth less instead of shrinking the radius.
                s = (budget / r - 1f).coerceIn(0f, s)
                extent = (1f + s) * r
            }
            val trim = 45f * s
            val sweep = 90f - 2f * trim
            val trimRad = Math.toRadians(trim.toDouble())
            val handleToArc = r * tan(trimRad / 2.0)
            val c = (handleToArc * cos(trimRad)).toFloat()
            val d = (handleToArc * sin(trimRad)).toFloat()
            val arcSpan = (sqrt(2.0) * r * sin(Math.toRadians(sweep / 2.0))).toFloat()
            val b = ((extent - arcSpan - c - d) / 3f).coerceAtLeast(0f)
            val a = 2f * b
            return CornerGeometry(r, extent, a, b, c, d, trim, sweep)
        }
    }
}

/** Minimal path sink so the geometry can be tested without Android graphics. */
internal interface PathSink {
    fun moveTo(x: Float, y: Float)
    fun lineTo(x: Float, y: Float)
    fun cubicTo(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float)

    /** Clockwise arc on the circle at ([cx], [cy]) with radius [r]. Angles in degrees, 0 is +x, screen coordinates. */
    fun arc(cx: Float, cy: Float, r: Float, startDegrees: Float, sweepDegrees: Float)
    fun close()
}

/**
 * Emits a closed squircle outline for a [width] by [height] box, clockwise from
 * the top edge. Radii are given in the order top left, top right, bottom right,
 * bottom left.
 */
internal fun emitSquircle(
    sink: PathSink,
    width: Float,
    height: Float,
    topLeft: Float,
    topRight: Float,
    bottomRight: Float,
    bottomLeft: Float,
    smoothing: Float,
) {
    if (width <= 0f || height <= 0f) return
    val budget = min(width, height) / 2f
    val tl = CornerGeometry.of(topLeft, smoothing, budget)
    val tr = CornerGeometry.of(topRight, smoothing, budget)
    val br = CornerGeometry.of(bottomRight, smoothing, budget)
    val bl = CornerGeometry.of(bottomLeft, smoothing, budget)

    sink.moveTo(tl.extent, 0f)
    corner(sink, tr, apexX = width, apexY = 0f, inX = 1f, inY = 0f, outX = 0f, outY = 1f, baseAngle = -90f)
    corner(sink, br, apexX = width, apexY = height, inX = 0f, inY = 1f, outX = -1f, outY = 0f, baseAngle = 0f)
    corner(sink, bl, apexX = 0f, apexY = height, inX = -1f, inY = 0f, outX = 0f, outY = -1f, baseAngle = 90f)
    corner(sink, tl, apexX = 0f, apexY = 0f, inX = 0f, inY = -1f, outX = 1f, outY = 0f, baseAngle = 180f)
    sink.close()
}

/**
 * Draws one corner. Local coordinates: u runs along the incoming edge toward
 * the apex (negative before it) and v along the outgoing edge. The second ramp
 * mirrors the first across the corner diagonal, mapping (u, v) to (-v, -u).
 */
private fun corner(
    sink: PathSink,
    g: CornerGeometry,
    apexX: Float,
    apexY: Float,
    inX: Float,
    inY: Float,
    outX: Float,
    outY: Float,
    baseAngle: Float,
) {
    fun x(u: Float, v: Float) = apexX + u * inX + v * outX
    fun y(u: Float, v: Float) = apexY + u * inY + v * outY

    if (g.isSharp) {
        sink.lineTo(apexX, apexY)
        return
    }
    val p = g.extent
    val r = g.radius
    // First ramp, edge to arc.
    val u1 = -p
    val u2 = -p + g.a
    val u3 = -p + g.a + g.b
    val su = u3 + g.c
    val sv = g.d
    sink.lineTo(x(u1, 0f), y(u1, 0f))
    sink.cubicTo(x(u2, 0f), y(u2, 0f), x(u3, 0f), y(u3, 0f), x(su, sv), y(su, sv))
    // Circular section around the usual corner centre.
    if (g.sweepDegrees > 0.01f) {
        sink.arc(x(-r, r), y(-r, r), r, baseAngle + g.trimDegrees, g.sweepDegrees)
    }
    // Second ramp, arc back to the outgoing edge.
    sink.cubicTo(x(0f, -u3), y(0f, -u3), x(0f, -u2), y(0f, -u2), x(0f, -u1), y(0f, -u1))
}
