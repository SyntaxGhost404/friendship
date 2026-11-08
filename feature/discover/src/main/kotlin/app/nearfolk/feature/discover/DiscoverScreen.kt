package app.nearfolk.feature.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PersonSearch
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.nearfolk.core.designsystem.component.NfBottomSheet
import app.nearfolk.core.designsystem.component.NfButton
import app.nearfolk.core.designsystem.component.NfButtonSize
import app.nearfolk.core.designsystem.component.NfButtonStyle
import app.nearfolk.core.designsystem.component.NfChip
import app.nearfolk.core.designsystem.component.NfCountBadge
import app.nearfolk.core.designsystem.component.NfDivider
import app.nearfolk.core.designsystem.component.NfEmptyState
import app.nearfolk.core.designsystem.component.NfGroup
import app.nearfolk.core.designsystem.component.NfIconButton
import app.nearfolk.core.designsystem.component.NfLargeHeader
import app.nearfolk.core.designsystem.component.NfListRow
import app.nearfolk.core.designsystem.component.NfNotice
import app.nearfolk.core.designsystem.component.NfNoticeTone
import app.nearfolk.core.designsystem.component.NfSearchField
import app.nearfolk.core.designsystem.component.NfSectionHeader
import app.nearfolk.core.designsystem.component.NfSegmented
import app.nearfolk.core.designsystem.component.NfSpinner
import app.nearfolk.core.designsystem.component.NfSwitchRow
import app.nearfolk.core.designsystem.theme.Nf
import app.nearfolk.core.model.DiscoverFilter
import app.nearfolk.core.model.GenderFilter
import app.nearfolk.core.model.ProfileRules
import app.nearfolk.core.ui.CountryPickerSheet
import app.nearfolk.core.ui.MemberRow
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data object DiscoverRoute

@Composable
fun DiscoverRouteScreen(
    onOpenMember: (String) -> Unit,
    onOpenPlus: () -> Unit,
    viewModel: DiscoverViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val query by viewModel.queryText.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    DiscoverScreen(
        state = state,
        query = query,
        refreshing = refreshing,
        onQuery = viewModel::setQuery,
        onGender = viewModel::setGender,
        onApplyFilter = viewModel::applyFilter,
        onClearFilters = viewModel::clearFilters,
        onRefresh = viewModel::refresh,
        onOpenMember = onOpenMember,
        onOpenPlus = onOpenPlus,
    )
}

private fun GenderFilter.label(): Int = when (this) {
    GenderFilter.All -> R.string.discover_gender_all
    GenderFilter.Female -> app.nearfolk.core.ui.R.string.ui_gender_female
    GenderFilter.Male -> app.nearfolk.core.ui.R.string.ui_gender_male
    GenderFilter.Nonbinary -> app.nearfolk.core.ui.R.string.ui_gender_nonbinary
}

@Composable
fun DiscoverScreen(
    state: DiscoverUiState,
    query: String,
    refreshing: Boolean,
    onQuery: (String) -> Unit,
    onGender: (GenderFilter) -> Unit,
    onApplyFilter: (DiscoverFilter) -> Unit,
    onClearFilters: () -> Unit,
    onRefresh: () -> Unit,
    onOpenMember: (String) -> Unit,
    onOpenPlus: () -> Unit,
    now: Instant = Instant.now(),
) {
    var showFilters by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    LaunchedEffect(state.filter, state.query) { listState.scrollToItem(0) }

    Column(Modifier.fillMaxSize().background(Nf.colors.canvas)) {
        NfLargeHeader(stringResource(R.string.discover_title)) {
            Box {
                NfIconButton(Icons.Rounded.Tune, stringResource(R.string.discover_filters), { showFilters = true }, style = NfButtonStyle.Secondary)
                val extra = state.filter.copy(gender = GenderFilter.All).activeCount
                if (extra > 0) NfCountBadge(extra, Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-6).dp))
            }
        }
        NfSearchField(
            query = query,
            onQueryChange = onQuery,
            placeholder = stringResource(R.string.discover_search_hint),
            clearDescription = stringResource(R.string.discover_search_clear),
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(GenderFilter.entries) { g ->
                NfChip(stringResource(g.label()), selected = state.filter.gender == g, onClick = { onGender(g) })
            }
        }
        PullToRefreshBox(isRefreshing = refreshing, onRefresh = onRefresh, modifier = Modifier.weight(1f)) {
            when {
                state.loading -> NfSpinner()
                state.members.isEmpty() -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    if (query.isNotBlank()) {
                        NfEmptyState(
                            Icons.Rounded.PersonSearch,
                            stringResource(R.string.discover_empty_search_title, query.trim()),
                            stringResource(R.string.discover_empty_search_body),
                        )
                    } else {
                        NfEmptyState(
                            Icons.Rounded.PersonSearch,
                            stringResource(R.string.discover_empty_title),
                            stringResource(R.string.discover_empty_body),
                            action = {
                                NfButton(stringResource(R.string.discover_clear_filters), onClearFilters, style = NfButtonStyle.Secondary, size = NfButtonSize.Medium)
                            },
                        )
                    }
                }
                else -> LazyColumn(state = listState, contentPadding = PaddingValues(start = 4.dp, end = 4.dp, bottom = 24.dp)) {
                    item(key = "summary") {
                        Text(
                            if (query.isBlank()) {
                                pluralStringResource(R.plurals.discover_online_count, state.onlineCount, state.onlineCount)
                            } else {
                                pluralStringResource(R.plurals.discover_results, state.members.size, state.members.size)
                            },
                            style = Nf.type.labelSmall,
                            color = Nf.colors.textTertiary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                    }
                    items(state.members, key = { it.id }) { member ->
                        MemberRow(member, onClick = { onOpenMember(member.id) }, now = now, modifier = Modifier.animateItem())
                    }
                }
            }
        }
    }

    if (showFilters) {
        FilterSheet(
            filter = state.filter,
            isPlus = state.isPlus,
            countries = state.availableCountries,
            onDismiss = { showFilters = false },
            onApply = {
                onApplyFilter(it)
                showFilters = false
            },
            onOpenPlus = {
                showFilters = false
                onOpenPlus()
            },
        )
    }
}

@Composable
private fun FilterSheet(
    filter: DiscoverFilter,
    isPlus: Boolean,
    countries: List<String>,
    onDismiss: () -> Unit,
    onApply: (DiscoverFilter) -> Unit,
    onOpenPlus: () -> Unit,
) {
    var draft by remember { mutableStateOf(filter) }
    var pickCountries by remember { mutableStateOf(false) }
    NfBottomSheet(onDismiss = onDismiss, title = stringResource(R.string.discover_filter_title)) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            NfSectionHeader(stringResource(R.string.discover_filter_gender))
            NfSegmented(
                options = GenderFilter.entries.map { stringResource(it.label()) },
                selectedIndex = GenderFilter.entries.indexOf(draft.gender),
                onSelect = { draft = draft.copy(gender = GenderFilter.entries[it]) },
            )
            Spacer(Modifier.height(16.dp))
            NfGroup {
                NfSwitchRow(
                    stringResource(R.string.discover_filter_online),
                    draft.onlineOnly,
                    { draft = draft.copy(onlineOnly = it) },
                    subtitle = stringResource(R.string.discover_filter_online_body),
                )
                NfDivider()
                NfListRow(
                    stringResource(R.string.discover_filter_countries),
                    subtitle = if (draft.countries.isEmpty()) {
                        stringResource(R.string.discover_filter_countries_any)
                    } else {
                        pluralStringResource(R.plurals.discover_filter_countries_some, draft.countries.size, draft.countries.size)
                    },
                    onClick = { pickCountries = true },
                )
            }
            NfSectionHeader(stringResource(R.string.discover_filter_age))
            if (isPlus) {
                val range = draft.ageRange ?: ProfileRules.MIN_AGE..80
                Text(
                    stringResource(R.string.discover_filter_age_value, range.first, range.last),
                    style = Nf.type.bodyStrong,
                    color = Nf.colors.textPrimary,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
                RangeSlider(
                    value = range.first.toFloat()..range.last.toFloat(),
                    onValueChange = { v -> draft = draft.copy(ageRange = v.start.toInt()..v.endInclusive.toInt()) },
                    valueRange = ProfileRules.MIN_AGE.toFloat()..80f,
                    colors = SliderDefaults.colors(
                        thumbColor = Nf.colors.accent,
                        activeTrackColor = Nf.colors.accent,
                        inactiveTrackColor = Nf.colors.surfaceRaised,
                    ),
                )
            } else {
                NfNotice(
                    text = stringResource(R.string.discover_filter_age_plus),
                    tone = NfNoticeTone.Accent,
                    action = {
                        NfButton(stringResource(R.string.discover_filter_age_plus_cta), onOpenPlus, size = NfButtonSize.Small)
                    },
                )
            }
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NfButton(stringResource(R.string.discover_filter_reset), { draft = DiscoverFilter() }, style = NfButtonStyle.Secondary, modifier = Modifier.weight(1f))
                NfButton(stringResource(R.string.discover_filter_apply), { onApply(draft) }, modifier = Modifier.weight(1.4f))
            }
            Spacer(Modifier.height(8.dp))
        }
    }
    if (pickCountries) {
        CountryPickerSheet(
            selected = draft.countries,
            onDismiss = { pickCountries = false },
            onConfirm = {
                draft = draft.copy(countries = it)
                pickCountries = false
            },
            multiSelect = true,
            codes = countries,
        )
    }
}

