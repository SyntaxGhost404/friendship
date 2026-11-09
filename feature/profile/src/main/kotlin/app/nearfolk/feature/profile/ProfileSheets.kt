package app.nearfolk.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.nearfolk.core.designsystem.component.NfButtonStyle
import app.nearfolk.core.designsystem.component.NfFullScreenDialog
import app.nearfolk.core.designsystem.component.NfIconButton
import app.nearfolk.core.designsystem.theme.Nf
import app.nearfolk.core.designsystem.theme.NfShapes
import app.nearfolk.core.model.Highlight
import app.nearfolk.core.ui.MediaImage

/** Full screen pager over a member's highlights. */
@Composable
internal fun HighlightViewer(highlights: List<Highlight>, start: Int, onDismiss: () -> Unit) {
    if (highlights.isEmpty()) return
    NfFullScreenDialog(onDismiss) {
        val pager = rememberPagerState(initialPage = start.coerceIn(0, highlights.lastIndex)) { highlights.size }
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            HorizontalPager(pager, Modifier.fillMaxSize()) { page ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    MediaImage(
                        highlights[page].media,
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .padding(12.dp)
                            .clip(NfShapes.large),
                    )
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NfIconButton(Icons.Rounded.Close, stringResource(app.nearfolk.core.ui.R.string.ui_close), onDismiss, style = NfButtonStyle.Secondary)
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.profile_highlight_counter, pager.currentPage + 1, highlights.size),
                        style = Nf.type.label,
                        color = Color.White,
                        modifier = Modifier
                            .clip(NfShapes.small)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
                Box(Modifier.padding(20.dp))
            }
        }
    }
}
