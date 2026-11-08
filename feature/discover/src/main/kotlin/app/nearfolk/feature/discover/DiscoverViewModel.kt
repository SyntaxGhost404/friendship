package app.nearfolk.feature.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.nearfolk.core.common.FuzzyMatcher
import app.nearfolk.core.data.repository.MemberRepository
import app.nearfolk.core.data.repository.PlusRepository
import app.nearfolk.core.data.repository.PreferencesRepository
import app.nearfolk.core.model.DiscoverFilter
import app.nearfolk.core.model.GenderFilter
import app.nearfolk.core.model.Member
import app.nearfolk.core.model.Presence
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DiscoverUiState(
    val loading: Boolean = true,
    val members: List<Member> = emptyList(),
    val onlineCount: Int = 0,
    val filter: DiscoverFilter = DiscoverFilter(),
    val isPlus: Boolean = false,
    val availableCountries: List<String> = emptyList(),
    val query: String = "",
)

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val members: MemberRepository,
    private val preferences: PreferencesRepository,
    plus: PlusRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing

    private val isPlus = plus.status.map { it.active }.distinctUntilChanged()

    /** Age range only applies while Plus is active. */
    private val effectiveFilter = combine(preferences.preferences.map { it.discoverFilter }, isPlus) { filter, plusActive ->
        if (plusActive) filter else filter.copy(ageRange = null)
    }.distinctUntilChanged()

    private val everyone = members.discover(DiscoverFilter())

    val state: StateFlow<DiscoverUiState> = combine(
        effectiveFilter.flatMapLatest { members.discover(it) },
        effectiveFilter,
        isPlus,
        everyone.map { list -> list.map { it.countryCode }.distinct() },
        query.debounce(120).onStart { emit("") },
    ) { list, filter, plusActive, countries, q ->
        val ranked = if (q.isBlank()) list else FuzzyMatcher.rank(q, list) { it.displayName }
        DiscoverUiState(
            loading = false,
            members = ranked,
            onlineCount = list.count { it.presence is Presence.Online },
            filter = filter,
            isPlus = plusActive,
            availableCountries = countries,
            query = q,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DiscoverUiState())

    val queryText: StateFlow<String> = query

    fun setQuery(value: String) {
        query.value = value
    }

    fun setGender(gender: GenderFilter) = update { it.copy(gender = gender) }

    fun applyFilter(filter: DiscoverFilter) = update { filter }

    fun clearFilters() = update { DiscoverFilter() }

    private fun update(transform: (DiscoverFilter) -> DiscoverFilter) {
        viewModelScope.launch { preferences.setDiscoverFilter(transform(state.value.filter)) }
    }

    fun refresh() {
        if (_refreshing.value) return
        viewModelScope.launch {
            _refreshing.value = true
            members.refresh()
            delay(500)
            _refreshing.value = false
        }
    }
}
