package app.nearfolk.core.designsystem

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.nearfolk.core.designsystem.component.NfButton
import app.nearfolk.core.designsystem.component.NfButtonStyle
import app.nearfolk.core.designsystem.component.NfIconButton
import app.nearfolk.core.designsystem.theme.NearfolkTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Buttons must stay on screen through every enabled change, look dimmed while
 * disabled and fully lit once enabled.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h640dp-xhdpi")
class ButtonStateTest {

    @get:Rule
    val rule = createComposeRule()

    private var enabled by mutableStateOf(false)

    /** Relative luminance of one pixel, sampled inside the button padding so text is ignored. */
    private fun Bitmap.lum(c: Int) = (0.2126 * android.graphics.Color.red(c) + 0.7152 * android.graphics.Color.green(c) + 0.0722 * android.graphics.Color.blue(c)) / 255.0

    private fun sample(tag: String): Bitmap = rule.onNodeWithTag(tag).captureToImage().asAndroidBitmap()

    private fun setUp() {
        rule.setContent {
            NearfolkTheme(darkTheme = true) {
                Column(Modifier.background(Color.Black).padding(16.dp)) {
                    NfButton("Continue", {}, enabled = enabled, modifier = Modifier.testTag("button"))
                    Box(Modifier.size(16.dp))
                    NfIconButton(
                        Icons.AutoMirrored.Rounded.Send,
                        "Send",
                        {},
                        style = NfButtonStyle.Primary,
                        size = 48.dp,
                        enabled = enabled,
                        modifier = Modifier.testTag("send"),
                    )
                }
            }
        }
    }

    @Test
    fun buttonsStayVisibleAndBrightenWhenEnabled() {
        setUp()
        val disabledButton = sample("button").let { it.getPixel(8, it.height / 2) }
        val disabledSend = sample("send").let { it.getPixel(6, it.height / 2) }

        enabled = true
        rule.mainClock.advanceTimeBy(1_000)
        rule.waitForIdle()
        val enabledButton = sample("button").let { it.getPixel(8, it.height / 2) }
        val enabledSend = sample("send").let { it.getPixel(6, it.height / 2) }

        val black = 0.02
        assertTrue("enabled button vanished", sample("button").let { it.lum(it.getPixel(8, it.height / 2)) } > black)
        assertTrue("enabled send vanished", sample("send").let { it.lum(it.getPixel(6, it.height / 2)) } > black)
        assertTrue("disabled button must be dimmer than enabled", sample("button").lum(disabledButton) < sample("button").lum(enabledButton))
        assertTrue("disabled send must be dimmer than enabled", sample("send").lum(disabledSend) < sample("send").lum(enabledSend))

        // And back again.
        enabled = false
        rule.mainClock.advanceTimeBy(1_000)
        rule.waitForIdle()
        assertTrue("disabled button vanished", sample("button").let { it.lum(it.getPixel(8, it.height / 2)) } > black)
    }
}
