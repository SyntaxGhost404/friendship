package app.nearfolk.feature.you

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.nearfolk.core.designsystem.component.NfButton
import app.nearfolk.core.designsystem.component.NfButtonSize
import app.nearfolk.core.designsystem.component.NfButtonStyle
import app.nearfolk.core.designsystem.component.NfCountBadge
import app.nearfolk.core.designsystem.component.NfDivider
import app.nearfolk.core.designsystem.component.NfGroup
import app.nearfolk.core.designsystem.component.NfIconButton
import app.nearfolk.core.designsystem.component.NfLargeHeader
import app.nearfolk.core.designsystem.component.NfListRow
import app.nearfolk.core.designsystem.component.NfPlusMark
import app.nearfolk.core.designsystem.component.NfSectionHeader
import app.nearfolk.core.designsystem.component.NfSpinner
import app.nearfolk.core.designsystem.icon.NfIcons
import app.nearfolk.core.designsystem.theme.Nf
import app.nearfolk.core.designsystem.theme.NfShapes
import app.nearfolk.core.model.PlusLimits
import app.nearfolk.core.ui.MemberAvatar
import app.nearfolk.core.ui.StreakCounter
import app.nearfolk.core.ui.formatDuration
import app.nearfolk.core.ui.metaLine
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable data object YouRoute
@Serializable data object EditProfileRoute
@Serializable data class CropRoute(val uri: String, val target: String)
@Serializable data object VoiceIntroRoute
@Serializable data object MyPostsRoute
@Serializable data object SavedPostsRoute
@Serializable data object ComposePostRoute
@Serializable data object HighlightsRoute
@Serializable data object ActivityRoute

@Composable
fun YouRouteScreen(
    onPreview: () -> Unit,
    onEdit: () -> Unit,
    onNewPost: () -> Unit,
    onMyPosts: () -> Unit,
    onSaved: () -> Unit,
    onHighlights: () -> Unit,
    onActivity: () -> Unit,
    onVoice: () -> Unit,
    onJournal: () -> Unit,
    onPlus: () -> Unit,
    viewModel: YouViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    YouScreen(state, onPreview, onEdit, onNewPost, onMyPosts, onSaved, onHighlights, onActivity, onVoice, onJournal, onPlus)
}

@Composable
fun YouScreen(
    state: YouUiState,
    onPreview: () -> Unit,
    onEdit: () -> Unit,
    onNewPost: () -> Unit,
    onMyPosts: () -> Unit,
    onSaved: () -> Unit,
    onHighlights: () -> Unit,
    onActivity: () -> Unit,
    onVoice: () -> Unit,
    onJournal: () -> Unit,
    onPlus: () -> Unit,
    today: LocalDate = LocalDate.now(),
) {
    val me = state.me
    Column(Modifier.fillMaxSize().background(Nf.colors.canvas)) {
        NfLargeHeader(stringResource(R.string.you_title)) {
            NfIconButton(Icons.Rounded.Visibility, stringResource(R.string.you_preview), onPreview, style = NfButtonStyle.Secondary)
        }
        if (me == null) {
            NfSpinner()
            return@Column
        }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(NfShapes.large)
                    .background(Nf.colors.surface)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                MemberAvatar(me.displayName, me.avatar, 72.dp, ringColor = Nf.colors.surface)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(me.displayName, style = Nf.type.title, color = Nf.colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                        if (state.isPlus) NfPlusMark()
                    }
                    Text(me.metaLine(today), style = Nf.type.bodySmall, color = Nf.colors.textSecondary)
                    Text(
                        me.catchphrase.ifBlank { stringResource(R.string.you_no_status) },
                        style = Nf.type.bodySmall,
                        color = if (me.catchphrase.isBlank()) Nf.colors.textTertiary else Nf.colors.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            NfButton(stringResource(R.string.you_edit), onEdit, style = NfButtonStyle.Secondary, size = NfButtonSize.Medium, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Stat(state.stats.posts, stringResource(R.string.you_stat_posts), Modifier.weight(1f))
                Stat(state.stats.likes, stringResource(R.string.you_stat_likes), Modifier.weight(1f))
                Stat(state.stats.highlights, stringResource(R.string.you_stat_highlights), Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            StreakCounter(me.streakDays(today), me.streakStart, Modifier.fillMaxWidth())

            NfSectionHeader(stringResource(R.string.you_section_share))
            NfGroup {
                NfListRow(stringResource(R.string.you_new_post), subtitle = stringResource(R.string.you_new_post_body), icon = Icons.Rounded.AddPhotoAlternate, onClick = onNewPost)
                NfDivider(inset = true)
                NfListRow(
                    stringResource(R.string.you_voice),
                    subtitle = me.voiceIntro?.let { stringResource(R.string.you_voice_length, formatDuration(it.durationMillis)) } ?: stringResource(R.string.you_voice_none),
                    icon = Icons.Rounded.Mic,
                    onClick = onVoice,
                )
                NfDivider(inset = true)
                NfListRow(
                    stringResource(R.string.you_journal),
                    subtitle = stringResource(if (state.passcodeSet) R.string.you_journal_locked else R.string.you_journal_open),
                    icon = NfIcons.Journal,
                    onClick = onJournal,
                )
            }

            NfSectionHeader(stringResource(R.string.you_section_manage))
            NfGroup {
                NfListRow(stringResource(R.string.you_posts), subtitle = stringResource(R.string.you_posts_body), icon = Icons.Rounded.ViewAgenda, onClick = onMyPosts)
                NfDivider(inset = true)
                NfListRow(
                    stringResource(R.string.you_saved),
                    subtitle = if (state.savedCount == 0) {
                        stringResource(R.string.you_saved_none)
                    } else {
                        pluralStringResource(R.plurals.you_saved_body, state.savedCount, state.savedCount)
                    },
                    icon = Icons.Rounded.Bookmark,
                    onClick = onSaved,
                )
                NfDivider(inset = true)
                NfListRow(
                    stringResource(R.string.you_highlights),
                    subtitle = stringResource(R.string.you_highlights_body, state.stats.highlights, PlusLimits.highlights(state.isPlus)),
                    icon = Icons.Rounded.GridView,
                    onClick = onHighlights,
                )
                NfDivider(inset = true)
                NfListRow(
                    stringResource(R.string.you_activity),
                    subtitle = stringResource(R.string.you_activity_body),
                    icon = Icons.Rounded.Favorite,
                    onClick = onActivity,
                    trailing = { NfCountBadge(state.unseenActivity) },
                )
            }
            Spacer(Modifier.height(16.dp))
            NfGroup {
                NfListRow(
                    stringResource(R.string.you_plus),
                    subtitle = stringResource(if (state.isPlus) R.string.you_plus_active else R.string.you_plus_body),
                    icon = Icons.Rounded.WorkspacePremium,
                    iconTint = Nf.colors.accent,
                    onClick = onPlus,
                )
            }
        }
    }
}

@Composable
private fun Stat(value: Int, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(NfShapes.large)
            .background(Nf.colors.surface)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value.toString(), style = Nf.type.title, color = Nf.colors.textPrimary)
        Text(label, style = Nf.type.labelSmall, color = Nf.colors.textSecondary)
    }
}
