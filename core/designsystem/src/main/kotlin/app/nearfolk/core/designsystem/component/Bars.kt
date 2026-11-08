package app.nearfolk.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.nearfolk.core.designsystem.theme.HapticKind
import app.nearfolk.core.designsystem.theme.LocalAppHaptics
import app.nearfolk.core.designsystem.theme.Nf
import app.nearfolk.core.designsystem.theme.NfMotion
import app.nearfolk.core.designsystem.theme.NfShapes

/** Compact bar for pushed screens: back, title, actions. */
@Composable
fun NfTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    backDescription: String? = null,
    subtitle: String? = null,
    titleContent: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Nf.colors.canvas)
            .windowInsetsPadding(WindowInsets.statusBars)
            .height(60.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            NfIconButton(Icons.AutoMirrored.Rounded.ArrowBack, backDescription, onBack)
            Spacer(Modifier.width(4.dp))
        } else {
            Spacer(Modifier.width(8.dp))
        }
        Box(Modifier.weight(1f)) {
            if (titleContent != null) {
                titleContent()
            } else {
                Column {
                    Text(title, style = Nf.type.titleSmall, color = Nf.colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (subtitle != null) {
                        Text(subtitle, style = Nf.type.labelSmall, color = Nf.colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

/** Large title header for the top level destinations. */
@Composable
fun NfLargeHeader(
    title: String,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = Nf.type.headline, color = Nf.colors.textPrimary, modifier = Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp), content = actions)
    }
}

data class NfNavItem(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val badgeCount: Int = 0,
)

/**
 * Bottom navigation. A squircle indicator travels behind the selected icon
 * on a spring, so tab changes feel physical without any glow or blur.
 */
@Composable
fun NfNavigationBar(
    items: List<NfNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Nf.colors
    val haptics = LocalAppHaptics.current
    Column(modifier.fillMaxWidth().background(colors.canvas)) {
        NfDivider()
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .height(66.dp),
        ) {
            val slot = maxWidth / items.size.coerceAtLeast(1)
            val indicatorWidth = 56.dp
            val indicatorX by animateDpAsState(
                targetValue = slot * selectedIndex.coerceAtLeast(0) + (slot - indicatorWidth) / 2,
                animationSpec = NfMotion.glide(),
                label = "navIndicator",
            )
            if (selectedIndex >= 0) {
                Box(
                    Modifier
                        .offset { IntOffset(indicatorX.roundToPx(), 9.dp.roundToPx()) }
                        .width(indicatorWidth)
                        .height(30.dp)
                        .clip(NfShapes.small)
                        .background(colors.surfaceRaised),
                )
            }
            Row(Modifier.fillMaxWidth().fillMaxHeight().selectableGroup()) {
                items.forEachIndexed { index, item ->
                    val selected = index == selectedIndex
                    val tint by animateColorAsState(if (selected) colors.textPrimary else colors.textTertiary, label = "navTint")
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .selectable(
                                selected = selected,
                                role = Role.Tab,
                                indication = null,
                                interactionSource = null,
                                onClick = {
                                    if (!selected) haptics.perform(HapticKind.Tick)
                                    onSelect(index)
                                },
                            )
                            .padding(top = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box {
                            Icon(
                                if (selected) item.selectedIcon else item.icon,
                                contentDescription = null,
                                tint = tint,
                                modifier = Modifier.size(24.dp),
                            )
                            if (item.badgeCount > 0) {
                                NfCountBadge(
                                    item.badgeCount,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 12.dp, y = (-6).dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(item.label, style = Nf.type.caption, color = tint, maxLines = 1)
                    }
                }
            }
        }
    }
}
