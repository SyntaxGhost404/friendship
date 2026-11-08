package app.nearfolk.core.data.local

import app.nearfolk.core.common.TimeSource
import app.nearfolk.core.data.community.CommunitySimulator
import app.nearfolk.core.data.repository.MemberRepository
import app.nearfolk.core.database.dao.HighlightDao
import app.nearfolk.core.database.dao.JournalDao
import app.nearfolk.core.database.dao.MemberDao
import app.nearfolk.core.database.dao.SafetyDao
import app.nearfolk.core.model.DiscoverFilter
import app.nearfolk.core.model.GenderFilter
import app.nearfolk.core.model.Highlight
import app.nearfolk.core.model.Member
import app.nearfolk.core.model.Presence
import app.nearfolk.core.model.SELF_ID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class LocalMemberRepository @Inject constructor(
    private val members: MemberDao,
    private val highlights: HighlightDao,
    private val journal: JournalDao,
    private val safety: SafetyDao,
    private val simulator: CommunitySimulator,
    private val time: TimeSource,
) : MemberRepository {

    override fun discover(filter: DiscoverFilter): Flow<List<Member>> =
        members.observeDiscoverable(SELF_ID).map { rows ->
            val today = time.today()
            rows.asSequence()
                .map { it.toModel() }
                .filter { matches(it, filter, today) }
                .sortedWith(
                    compareByDescending<Member> { it.presence is Presence.Online }
                        .thenByDescending { (it.presence as? Presence.Offline)?.lastSeenAt?.toEpochMilli() ?: Long.MAX_VALUE }
                        .thenBy { it.displayName },
                )
                .toList()
        }

    private fun matches(m: Member, f: DiscoverFilter, today: java.time.LocalDate): Boolean {
        val genderOk = when (f.gender) {
            GenderFilter.All -> true
            GenderFilter.Female -> m.gender == app.nearfolk.core.model.Gender.Female
            GenderFilter.Male -> m.gender == app.nearfolk.core.model.Gender.Male
            GenderFilter.Nonbinary -> m.gender == app.nearfolk.core.model.Gender.Nonbinary
        }
        val countryOk = f.countries.isEmpty() || m.countryCode in f.countries
        val onlineOk = !f.onlineOnly || m.presence is Presence.Online
        val ageOk = f.ageRange?.let { m.ageOn(today) in it } ?: true
        return genderOk && countryOk && onlineOk && ageOk
    }

    override fun member(id: String): Flow<Member?> = members.observe(id).map { it?.toModel() }

    override fun highlights(memberId: String): Flow<List<Highlight>> =
        highlights.observe(memberId).map { rows -> rows.map { it.toModel() } }

    override fun blockedMembers(): Flow<List<Member>> = members.observeBlocked().map { rows -> rows.map { it.toModel() } }

    override fun isBlocked(memberId: String): Flow<Boolean> = safety.observeBlocked(memberId)

    override fun publicJournalCount(memberId: String): Flow<Int> = journal.observePublicCount(memberId)

    override suspend fun refresh() = simulator.refreshPresence()
}
