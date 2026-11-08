package app.nearfolk.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.nearfolk.core.designsystem.component.NfButton
import app.nearfolk.core.designsystem.component.NfButtonSize
import app.nearfolk.core.designsystem.component.NfButtonStyle
import app.nearfolk.core.designsystem.component.NfChip
import app.nearfolk.core.designsystem.component.NfCountBadge
import app.nearfolk.core.designsystem.component.NfGroup
import app.nearfolk.core.designsystem.component.NfListRow
import app.nearfolk.core.designsystem.component.NfNavItem
import app.nearfolk.core.designsystem.component.NfNavigationBar
import app.nearfolk.core.designsystem.component.NfNotice
import app.nearfolk.core.designsystem.component.NfNoticeTone
import app.nearfolk.core.designsystem.component.NfPresencePip
import app.nearfolk.core.designsystem.component.NfSearchField
import app.nearfolk.core.designsystem.component.NfSegmented
import app.nearfolk.core.designsystem.component.NfSwitchRow
import app.nearfolk.core.designsystem.component.NfTag
import app.nearfolk.core.designsystem.component.NfDivider
import app.nearfolk.core.designsystem.icon.NfIcons
import app.nearfolk.core.designsystem.theme.Nf
import app.nearfolk.core.designsystem.theme.NfShapes
import app.nearfolk.core.designsystem.theme.NearfolkTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h1100dp-xhdpi")
class ComponentGalleryScreenshotTest {

    @Test
    fun galleryDark() = captureRoboImage("build/screenshots/gallery_dark.png") { Gallery(dark = true) }

    @Test
    fun galleryLight() = captureRoboImage("build/screenshots/gallery_light.png") { Gallery(dark = false) }

    @Composable
    private fun Gallery(dark: Boolean) {
        NearfolkTheme(darkTheme = dark) {
            Column(
                Modifier
                    .background(Nf.colors.canvas)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("Discover", style = Nf.type.headline)
                NfSearchField("mar", {}, "Search by name", "Clear")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NfChip("All", true, {})
                    NfChip("Female", false, {})
                    NfChip("Male", false, {})
                    NfChip("Nonbinary", false, {})
                }
                NfSegmented(listOf("System", "Light", "Dark"), 2, {})
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NfButton("Message", {}, size = NfButtonSize.Medium)
                    NfButton("Journal", {}, style = NfButtonStyle.Secondary, size = NfButtonSize.Medium)
                    NfButton("Block", {}, style = NfButtonStyle.Danger, size = NfButtonSize.Medium)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box {
                        Box(Modifier.size(64.dp).clip(NfShapes.avatar).background(Nf.colors.accentSoft))
                        NfPresencePip(true, Modifier.align(androidx.compose.ui.Alignment.BottomEnd), size = 16.dp)
                    }
                    Box(Modifier.size(64.dp).clip(NfShapes.large).background(Nf.colors.surfaceRaised))
                    Box(Modifier.size(width = 120.dp, height = 64.dp).clip(NfShapes.extraLarge).background(Nf.colors.ink))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        NfCountBadge(3)
                        NfCountBadge(128)
                        NfTag("Leo")
                        NfTag("Plus", emphasized = true)
                    }
                }
                NfNotice(
                    title = "Stay safe",
                    text = "Never share passwords, codes or bank details, even with people you trust here.",
                    icon = Icons.Rounded.Shield,
                    tone = NfNoticeTone.Accent,
                    onDismiss = {},
                    dismissDescription = "Dismiss",
                )
                NfGroup {
                    NfListRow("Edit profile", subtitle = "Name, status, country", icon = NfIcons.You, onClick = {})
                    NfDivider(inset = true)
                    NfSwitchRow("Haptic feedback", true, {}, icon = NfIcons.Settings)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    listOf(NfIcons.Discover, NfIcons.DiscoverSelected, NfIcons.Messages, NfIcons.MessagesSelected, NfIcons.You, NfIcons.YouSelected, NfIcons.Settings, NfIcons.SettingsSelected, NfIcons.Journal, NfIcons.Mark).forEach {
                        Icon(it, null, tint = Nf.colors.textPrimary, modifier = Modifier.size(24.dp))
                    }
                }
                NfNavigationBar(
                    items = listOf(
                        NfNavItem("Discover", NfIcons.Discover, NfIcons.DiscoverSelected),
                        NfNavItem("Messages", NfIcons.Messages, NfIcons.MessagesSelected, badgeCount = 4),
                        NfNavItem("You", NfIcons.You, NfIcons.YouSelected),
                        NfNavItem("Settings", NfIcons.Settings, NfIcons.SettingsSelected),
                    ),
                    selectedIndex = 1,
                    onSelect = {},
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
