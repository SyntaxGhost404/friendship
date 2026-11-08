package app.nearfolk.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.nearfolk.core.designsystem.theme.Nf
import app.nearfolk.core.designsystem.theme.NfShapes

/** Unread count on a small squircle. Shows 99+ past ninety nine. */
@Composable
fun NfCountBadge(count: Int, modifier: Modifier = Modifier) {
    if (count <= 0) return
    val colors = Nf.colors
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
            .clip(NfShapes.badge)
            .background(colors.accent)
            .padding(horizontal = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(if (count > 99) "99+" else count.toString(), style = Nf.type.caption, color = colors.onAccent)
    }
}

/** Plain unread marker for places where a number would be noise. */
@Composable
fun NfUnreadMark(modifier: Modifier = Modifier, size: Dp = 9.dp) {
    Box(
        modifier
            .size(size)
            .clip(NfShapes.badge)
            .background(Nf.colors.accent),
    )
}

/**
 * Online marker for avatars: a small squircle with a ring in [ringColor] so
 * it reads cleanly over any photo.
 */
@Composable
fun NfPresencePip(online: Boolean, modifier: Modifier = Modifier, size: Dp = 12.dp, ringColor: Color = Nf.colors.canvas) {
    val fill by animateColorAsState(if (online) Nf.colors.online else Color.Transparent, label = "presence")
    if (!online && fill == Color.Transparent) return
    Box(
        modifier
            .size(size)
            .clip(NfShapes.badge)
            .background(ringColor)
            .padding(2.dp)
            .clip(NfShapes.badge)
            .background(fill),
    )
}

/** Compact label such as a zodiac sign or a plan name. */
@Composable
fun NfTag(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    emphasized: Boolean = false,
) {
    val colors = Nf.colors
    val container = if (emphasized) colors.accentSoft else colors.surfaceRaised
    val content = if (emphasized) colors.onAccentSoft else colors.textSecondary
    Row(
        modifier = modifier
            .clip(NfShapes.badge)
            .background(container)
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (icon != null) Icon(icon, null, tint = content, modifier = Modifier.size(12.dp))
        Text(text, style = Nf.type.caption, color = content, maxLines = 1)
    }
}

/** Small mark next to names of Plus members. */
@Composable
fun NfPlusMark(modifier: Modifier = Modifier) {
    val colors = Nf.colors
    Box(
        modifier
            .size(16.dp)
            .clip(NfShapes.badge)
            .border(1.5.dp, colors.accent, NfShapes.badge),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(6.dp)
                .clip(NfShapes.badge)
                .background(colors.accent),
        )
    }
}
