package app.nearfolk

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.nearfolk.core.designsystem.component.NfConfirmDialog
import app.nearfolk.core.designsystem.component.NfIconButton
import app.nearfolk.core.designsystem.theme.Nf
import app.nearfolk.core.designsystem.theme.NearfolkTheme
import app.nearfolk.core.ui.CountryPickerSheet
import app.nearfolk.core.ui.MenuAction
import app.nearfolk.core.ui.NfDatePickerDialog
import app.nearfolk.core.ui.NfMenu
import app.nearfolk.core.ui.ReportSheet
import com.github.takahirom.roborazzi.captureScreenRoboImage
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Menus, sheets and dialogs live in their own windows. These tests open each
 * one, check how it is placed and dismissed, and capture the whole screen.
 * Images land in app/build/screenshots when run with -Proborazzi.test.record=true.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h860dp-xhdpi")
class PopupTest {

    @get:Rule
    val rule = createComposeRule()

    private var menuOpen by mutableStateOf(false)
    private var reported = 0
    private var blocked = 0

    private fun show(dark: Boolean, content: @Composable () -> Unit) {
        rule.setContent {
            NearfolkTheme(darkTheme = dark) {
                Box(Modifier.fillMaxSize().background(Nf.colors.canvas)) { content() }
            }
        }
        rule.waitForIdle()
    }

    private fun capture(name: String) = captureScreenRoboImage("build/screenshots/popup_$name.png")

    private fun showMenu(dark: Boolean, alignment: Alignment) = show(dark) {
        Box(Modifier.fillMaxSize().padding(8.dp)) {
            Box(Modifier.align(alignment).testTag("anchor")) {
                NfIconButton(Icons.Rounded.MoreHoriz, "More", { menuOpen = true })
                NfMenu(
                    expanded = menuOpen,
                    onDismiss = { menuOpen = false },
                    actions = listOf(
                        MenuAction("Report", Icons.Rounded.Flag) { reported++ },
                        MenuAction("Block", Icons.Rounded.Block, destructive = true) { blocked++ },
                    ),
                )
            }
        }
    }

    @Test
    fun menuOpensAndRunsAnAction() {
        showMenu(dark = true, Alignment.TopEnd)
        rule.onNodeWithText("Report").assertDoesNotExist()

        rule.onNodeWithContentDescription("More").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Report").assertIsDisplayed()
        rule.onNodeWithText("Block").assertIsDisplayed()
        capture("menu_dark")

        rule.onNodeWithText("Block").performClick()
        rule.waitForIdle()
        assertEquals(1, blocked)
        assertEquals(0, reported)
        rule.onNodeWithText("Block").assertDoesNotExist()
    }

    @Test
    fun menuOpensFromAnAnchorAtTheBottom() {
        showMenu(dark = false, Alignment.BottomEnd)
        rule.onNodeWithContentDescription("More").performClick()
        rule.waitForIdle()
        capture("menu_above_light")

        rule.onNodeWithText("Block").assertIsDisplayed()
    }

    @Test
    fun menuClosesOnBack() {
        showMenu(dark = true, Alignment.TopEnd)
        rule.onNodeWithContentDescription("More").performClick()
        rule.waitForIdle()
        Espresso.pressBack()
        rule.waitForIdle()
        rule.onNodeWithText("Report").assertDoesNotExist()
        assertEquals(0, reported + blocked)
    }

    @Test
    fun countrySheetDark() = countrySheet(true)

    @Test
    fun countrySheetLight() = countrySheet(false)

    private fun countrySheet(dark: Boolean) {
        show(dark) {
            CountryPickerSheet(selected = setOf("KE", "JP"), onDismiss = {}, onConfirm = {}, multiSelect = true)
        }
        capture("country_sheet_${if (dark) "dark" else "light"}")
        // A hard fling through the list must leave the sheet where it is.
        val row = rule.onNodeWithText("Albania")
        row.assertIsDisplayed()
        row.performTouchInput { swipeUp(durationMillis = 40) }
        rule.waitForIdle()
        capture("country_sheet_scrolled_${if (dark) "dark" else "light"}")
    }

    @Test
    fun reportSheetDark() {
        show(true) { ReportSheet("Amara", onDismiss = {}, onSubmit = { _, _ -> }) }
        capture("report_sheet_dark")
    }

    @Test
    fun confirmDialogDark() = confirmDialog(true)

    @Test
    fun confirmDialogLight() = confirmDialog(false)

    private fun confirmDialog(dark: Boolean) {
        show(dark) {
            NfConfirmDialog(
                title = "Delete this chat?",
                message = "The conversation is removed from this device. The other person keeps their copy.",
                confirmLabel = "Delete chat",
                dismissLabel = "Cancel",
                onConfirm = {},
                onDismiss = {},
                destructive = true,
            )
        }
        capture("confirm_dialog_${if (dark) "dark" else "light"}")
    }

    @Test
    fun datePickerDark() {
        show(true) {
            NfDatePickerDialog(
                initial = LocalDate.of(1998, 5, 17),
                min = LocalDate.of(1930, 1, 1),
                max = LocalDate.of(2008, 11, 9),
                onConfirm = {},
                onDismiss = {},
            )
        }
        capture("date_picker_dark")
    }
}
