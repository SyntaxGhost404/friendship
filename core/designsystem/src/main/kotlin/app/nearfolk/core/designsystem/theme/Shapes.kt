package app.nearfolk.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.dp
import app.nearfolk.core.designsystem.shape.SquircleShape

/** Shape scale. Every curved container in the app draws from this set. */
@Immutable
object NfShapes {
    val badge = SquircleShape(6.dp)
    val small = SquircleShape(10.dp)
    val medium = SquircleShape(14.dp)
    val large = SquircleShape(20.dp)
    val extraLarge = SquircleShape(28.dp)
    val sheet = SquircleShape(topStart = 28.dp, topEnd = 28.dp)
    val avatar = SquircleShape.relative(0.32f)
    val pill = SquircleShape.relative(0.5f)
}
