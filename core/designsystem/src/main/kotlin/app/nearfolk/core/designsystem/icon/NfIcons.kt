package app.nearfolk.core.designsystem.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Navigation icons drawn for this app. They share the squircle language of
 * the rest of the interface: soft square bodies, round joins, 1.8 strokes.
 * Outline versions mark idle tabs, solid versions mark the selected tab.
 */
object NfIcons {

    private const val STROKE = 1.8f

    private fun icon(name: String, block: ImageVector.Builder.() -> Unit): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply(block).build()

    private fun ImageVector.Builder.stroke(pathBuilder: PathBuilder.() -> Unit) = path(
        fill = null,
        stroke = SolidColor(Color.Black),
        strokeLineWidth = STROKE,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        pathBuilder = pathBuilder,
    )

    private fun ImageVector.Builder.solid(fillType: PathFillType = PathFillType.NonZero, pathBuilder: PathBuilder.() -> Unit) = path(
        fill = SolidColor(Color.Black),
        pathFillType = fillType,
        pathBuilder = pathBuilder,
    )

    /** Approximates a squircle with tighter arcs at the corners. */
    private fun PathBuilder.softRect(l: Float, t: Float, r: Float, b: Float, rad: Float) {
        val k = rad * 0.85f
        moveTo(l + rad, t)
        lineTo(r - rad, t)
        curveTo(r - rad + k, t, r, t + rad - k, r, t + rad)
        lineTo(r, b - rad)
        curveTo(r, b - rad + k, r - rad + k, b, r - rad, b)
        lineTo(l + rad, b)
        curveTo(l + rad - k, b, l, b - rad + k, l, b - rad)
        lineTo(l, t + rad)
        curveTo(l, t + rad - k, l + rad - k, t, l + rad, t)
        close()
    }

    private fun PathBuilder.diamond(cx: Float, cy: Float, rx: Float, ry: Float) {
        moveTo(cx, cy - ry)
        lineTo(cx + rx, cy)
        lineTo(cx, cy + ry)
        lineTo(cx - rx, cy)
        close()
    }

    val Discover: ImageVector by lazy {
        icon("Discover") {
            stroke { softRect(3.5f, 3.5f, 20.5f, 20.5f, 6.5f) }
            stroke { diamond(12f, 12f, 2.6f, 4.6f) }
        }
    }

    val DiscoverSelected: ImageVector by lazy {
        icon("DiscoverSelected") {
            solid(PathFillType.EvenOdd) {
                softRect(2.6f, 2.6f, 21.4f, 21.4f, 7.2f)
                diamond(12f, 12f, 2.6f, 4.6f)
            }
        }
    }

    private fun PathBuilder.bubble() {
        moveTo(8.5f, 4f)
        lineTo(15.5f, 4f)
        curveTo(18.6f, 4f, 20.5f, 5.9f, 20.5f, 9f)
        lineTo(20.5f, 12f)
        curveTo(20.5f, 15.1f, 18.6f, 17f, 15.5f, 17f)
        lineTo(11.5f, 17f)
        lineTo(7.4f, 20.2f)
        curveTo(7f, 20.5f, 6.5f, 20.2f, 6.5f, 19.7f)
        lineTo(6.5f, 16.8f)
        curveTo(4.6f, 16.2f, 3.5f, 14.5f, 3.5f, 12f)
        lineTo(3.5f, 9f)
        curveTo(3.5f, 5.9f, 5.4f, 4f, 8.5f, 4f)
        close()
    }

    val Messages: ImageVector by lazy {
        icon("Messages") {
            stroke { bubble() }
            stroke {
                moveTo(8.5f, 9.2f)
                lineTo(15.5f, 9.2f)
                moveTo(8.5f, 12.4f)
                lineTo(12.8f, 12.4f)
            }
        }
    }

    val MessagesSelected: ImageVector by lazy {
        icon("MessagesSelected") {
            solid(PathFillType.EvenOdd) {
                bubble()
                softRect(7.6f, 8.3f, 16.4f, 10.1f, 0.9f)
                softRect(7.6f, 11.5f, 13.7f, 13.3f, 0.9f)
            }
        }
    }

    private fun PathBuilder.shoulders(l: Float, t: Float, r: Float, b: Float, rad: Float) {
        val k = rad * 0.85f
        moveTo(l, b)
        lineTo(l, t + rad)
        curveTo(l, t + rad - k, l + rad - k, t, l + rad, t)
        lineTo(r - rad, t)
        curveTo(r - rad + k, t, r, t + rad - k, r, t + rad)
        lineTo(r, b)
    }

    val You: ImageVector by lazy {
        icon("You") {
            stroke { softRect(8.2f, 3.2f, 15.8f, 10.8f, 3.2f) }
            stroke { shoulders(4.5f, 13.6f, 19.5f, 20.6f, 5.2f) }
        }
    }

    val YouSelected: ImageVector by lazy {
        icon("YouSelected") {
            solid { softRect(7.6f, 2.6f, 16.4f, 11.4f, 3.6f) }
            solid {
                shoulders(3.8f, 13f, 20.2f, 21.2f, 5.6f)
                close()
            }
        }
    }

    val Settings: ImageVector by lazy {
        icon("Settings") {
            stroke {
                moveTo(4f, 8f)
                lineTo(6.2f, 8f)
                moveTo(12.2f, 8f)
                lineTo(20f, 8f)
                moveTo(4f, 16f)
                lineTo(11.8f, 16f)
                moveTo(17.8f, 16f)
                lineTo(20f, 16f)
            }
            stroke { softRect(6.2f, 5f, 12.2f, 11f, 2.4f) }
            stroke { softRect(11.8f, 13f, 17.8f, 19f, 2.4f) }
        }
    }

    val SettingsSelected: ImageVector by lazy {
        icon("SettingsSelected") {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(4f, 8f)
                lineTo(20f, 8f)
                moveTo(4f, 16f)
                lineTo(20f, 16f)
            }
            solid { softRect(5.6f, 4.4f, 12.8f, 11.6f, 2.8f) }
            solid { softRect(11.2f, 12.4f, 18.4f, 19.6f, 2.8f) }
        }
    }

    /** Bound notebook with a ribbon, used for the journal. */
    val Journal: ImageVector by lazy {
        icon("Journal") {
            stroke { softRect(5f, 3.5f, 19f, 20.5f, 4f) }
            stroke {
                moveTo(9f, 3.8f)
                lineTo(9f, 20.2f)
                moveTo(12.5f, 8.5f)
                lineTo(16f, 8.5f)
                moveTo(12.5f, 12f)
                lineTo(15f, 12f)
            }
        }
    }

    /** Wordmark glyph: two squircles that overlap, two people meeting. */
    val Mark: ImageVector by lazy {
        icon("Mark") {
            solid { softRect(2.5f, 5f, 14.5f, 17f, 5f) }
            path(fill = SolidColor(Color.Black), fillAlpha = 0.55f) { softRect(9.5f, 7f, 21.5f, 19f, 5f) }
        }
    }
}
