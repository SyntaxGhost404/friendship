package app.nearfolk.core.ui

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import app.nearfolk.core.designsystem.component.NfPopupContent
import app.nearfolk.core.designsystem.component.shapeSurface
import app.nearfolk.core.designsystem.theme.HapticKind
import app.nearfolk.core.designsystem.theme.LocalAppHaptics
import app.nearfolk.core.designsystem.theme.Nf
import app.nearfolk.core.designsystem.theme.NfMotion
import app.nearfolk.core.designsystem.theme.NfShapes

data class MenuAction(val label: String, val icon: ImageVector, val destructive: Boolean = false, val onClick: () -> Unit)

/**
 * Overflow menu on a squircle surface, anchored to the layout it is declared in.
 *
 * It follows the popup rules in [NfPopupContent]: the menu fades and slides a
 * few pixels, it is never scaled, and the surface is a shape fill with no clip
 * around the scrolling rows.
 */
@Composable
fun NfMenu(expanded: Boolean, onDismiss: () -> Unit, actions: List<MenuAction>) {
    val visible = remember { MutableTransitionState(false) }
    visible.targetState = expanded
    if (!visible.currentState && !visible.targetState) return

    val density = LocalDensity.current
    var above by remember { mutableStateOf(false) }
    val position = remember(density) {
        with(density) {
            MenuPosition(
                inset = MenuInset.roundToPx(),
                gap = 4.dp.roundToPx(),
                sideMargin = 8.dp.roundToPx(),
                edgeMargin = 48.dp.roundToPx(),
                onPlaced = { above = it },
            )
        }
    }
    val transition = rememberTransition(visible, label = "menu")
    val progress by transition.animateFloat({ NfMotion.settle() }, label = "menuProgress") { if (it) 1f else 0f }

    Popup(popupPositionProvider = position, onDismissRequest = onDismiss, properties = PopupProperties(focusable = true)) {
        NfPopupContent {
            Column(
                Modifier
                    // Transparent room above and below, so the slide never leaves the popup window.
                    .padding(vertical = MenuInset)
                    .graphicsLayer {
                        alpha = progress
                        translationY = (if (above) MenuTravel else -MenuTravel).toPx() * (1f - progress)
                        // Fades each draw call instead of rendering the menu to a buffer first.
                        compositingStrategy = CompositingStrategy.ModulateAlpha
                    }
                    .widthIn(min = 184.dp, max = 280.dp)
                    .width(IntrinsicSize.Max)
                    .shapeSurface(Nf.colors.surfaceRaised, NfShapes.large, stroke = Nf.colors.lineStrong)
                    .padding(6.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                actions.forEach { action ->
                    MenuRow(action, enabled = expanded, onDismiss = onDismiss)
                }
            }
        }
    }
}

@Composable
private fun MenuRow(action: MenuAction, enabled: Boolean, onDismiss: () -> Unit) {
    val haptics = LocalAppHaptics.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val focused by interaction.collectIsFocusedAsState()
    val hovered by interaction.collectIsHoveredAsState()
    val color = if (action.destructive) Nf.colors.danger else Nf.colors.textPrimary
    val highlight = when {
        pressed -> color.copy(alpha = 0.14f)
        focused || hovered -> color.copy(alpha = 0.08f)
        else -> Color.Transparent
    }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            // The highlight is a shape fill. A ripple would need a clip to stay inside the row.
            .background(highlight, NfShapes.small)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button) {
                haptics.perform(HapticKind.Tap)
                onDismiss()
                action.onClick()
            }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(action.icon, null, tint = color, modifier = Modifier.size(20.dp))
        Text(action.label, style = Nf.type.bodyStrong, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private val MenuInset = 8.dp
private val MenuTravel = 6.dp

/** Where the popup window goes, and whether the menu ended up above its anchor. */
internal data class MenuPlacement(val offset: IntOffset, val above: Boolean)

/**
 * Places the menu under its anchor, aligned to the anchor's end edge. If there
 * is no room below it goes above, and it always stays inside the window.
 *
 * [content] is the size of the popup, which includes [inset] pixels of
 * transparent padding above and below the visible surface. [gap] separates the
 * surface from the anchor, [sideMargin] and [edgeMargin] keep it off the
 * window edges and the system bars.
 */
internal fun placeMenu(
    anchor: IntRect,
    window: IntSize,
    content: IntSize,
    rtl: Boolean,
    inset: Int,
    gap: Int,
    sideMargin: Int,
    edgeMargin: Int,
): MenuPlacement {
    val aligned = if (rtl) anchor.left else anchor.right - content.width
    val x = aligned.coerceIn(sideMargin, (window.width - content.width - sideMargin).coerceAtLeast(sideMargin))

    val surface = content.height - inset * 2
    val below = anchor.bottom + gap
    val fitsBelow = below + surface <= window.height - edgeMargin
    val fitsAbove = anchor.top - gap - surface >= edgeMargin
    val above = !fitsBelow && fitsAbove
    val top = if (above) anchor.top - gap - surface else below
    val y = top.coerceIn(edgeMargin, (window.height - surface - edgeMargin).coerceAtLeast(edgeMargin)) - inset
    return MenuPlacement(IntOffset(x, y), above)
}

private class MenuPosition(
    private val inset: Int,
    private val gap: Int,
    private val sideMargin: Int,
    private val edgeMargin: Int,
    private val onPlaced: (above: Boolean) -> Unit,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val placement = placeMenu(
            anchor = anchorBounds,
            window = windowSize,
            content = popupContentSize,
            rtl = layoutDirection == LayoutDirection.Rtl,
            inset = inset,
            gap = gap,
            sideMargin = sideMargin,
            edgeMargin = edgeMargin,
        )
        onPlaced(placement.above)
        return placement.offset
    }
}
