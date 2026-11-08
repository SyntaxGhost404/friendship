package app.nearfolk.core.designsystem.shape

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.min

/** Corner smoothing that closely follows the familiar app icon squircle. */
const val DefaultSmoothing = 0.6f

/**
 * Rounded rectangle with continuous curvature corners. See [CornerGeometry].
 *
 * Corners use start and end so the shape mirrors correctly in right to left
 * layouts. Use [SquircleShape.relative] for shapes that scale with their size,
 * such as avatars.
 */
@Immutable
class SquircleShape private constructor(
    private val topStart: Dp,
    private val topEnd: Dp,
    private val bottomEnd: Dp,
    private val bottomStart: Dp,
    private val fraction: Float?,
    private val smoothing: Float,
) : Shape {

    constructor(radius: Dp, smoothing: Float = DefaultSmoothing) :
        this(radius, radius, radius, radius, null, smoothing)

    constructor(
        topStart: Dp = 0.dp,
        topEnd: Dp = 0.dp,
        bottomEnd: Dp = 0.dp,
        bottomStart: Dp = 0.dp,
        smoothing: Float = DefaultSmoothing,
    ) : this(topStart, topEnd, bottomEnd, bottomStart, null, smoothing)

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val ltr = layoutDirection == LayoutDirection.Ltr
        val radii: FloatArray = if (fraction != null) {
            val r = min(size.width, size.height) * fraction
            floatArrayOf(r, r, r, r)
        } else {
            with(density) {
                val ts = topStart.toPx()
                val te = topEnd.toPx()
                val be = bottomEnd.toPx()
                val bs = bottomStart.toPx()
                if (ltr) floatArrayOf(ts, te, be, bs) else floatArrayOf(te, ts, bs, be)
            }
        }
        val path = Path()
        emitSquircle(ComposePathSink(path), size.width, size.height, radii[0], radii[1], radii[2], radii[3], smoothing)
        return Outline.Generic(path)
    }

    fun copy(
        topStart: Dp = this.topStart,
        topEnd: Dp = this.topEnd,
        bottomEnd: Dp = this.bottomEnd,
        bottomStart: Dp = this.bottomStart,
    ): SquircleShape = SquircleShape(topStart, topEnd, bottomEnd, bottomStart, null, smoothing)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SquircleShape) return false
        return topStart == other.topStart && topEnd == other.topEnd && bottomEnd == other.bottomEnd &&
            bottomStart == other.bottomStart && fraction == other.fraction && smoothing == other.smoothing
    }

    override fun hashCode(): Int {
        var result = topStart.hashCode()
        result = 31 * result + topEnd.hashCode()
        result = 31 * result + bottomEnd.hashCode()
        result = 31 * result + bottomStart.hashCode()
        result = 31 * result + (fraction?.hashCode() ?: 0)
        result = 31 * result + smoothing.hashCode()
        return result
    }

    override fun toString(): String =
        "SquircleShape(topStart=$topStart, topEnd=$topEnd, bottomEnd=$bottomEnd, bottomStart=$bottomStart, fraction=$fraction, smoothing=$smoothing)"

    companion object {
        /** Radius as a share of the shorter side. 0.5 gives a fully rounded end. */
        fun relative(fraction: Float, smoothing: Float = DefaultSmoothing): SquircleShape =
            SquircleShape(0.dp, 0.dp, 0.dp, 0.dp, fraction.coerceIn(0f, 0.5f), smoothing)
    }
}

private class ComposePathSink(private val path: Path) : PathSink {
    override fun moveTo(x: Float, y: Float) = path.moveTo(x, y)
    override fun lineTo(x: Float, y: Float) = path.lineTo(x, y)
    override fun cubicTo(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) =
        path.cubicTo(x1, y1, x2, y2, x3, y3)

    override fun arc(cx: Float, cy: Float, r: Float, startDegrees: Float, sweepDegrees: Float) {
        path.arcTo(Rect(cx - r, cy - r, cx + r, cy + r), startDegrees, sweepDegrees, forceMoveTo = false)
    }

    override fun close() = path.close()
}
