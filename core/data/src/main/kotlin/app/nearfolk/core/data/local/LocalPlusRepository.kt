package app.nearfolk.core.data.local

import app.nearfolk.core.common.TimeSource
import app.nearfolk.core.data.repository.PlusRepository
import app.nearfolk.core.data.repository.PurchaseResult
import app.nearfolk.core.datastore.NearfolkPreferences
import app.nearfolk.core.model.PlusPlan
import app.nearfolk.core.model.PlusStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Plan state kept on device. No store is connected in this build, so a
 * purchase unlocks the plan locally for testing. A Play Billing backed
 * implementation replaces this class.
 */
@Singleton
internal class LocalPlusRepository @Inject constructor(
    private val prefs: NearfolkPreferences,
    private val time: TimeSource,
) : PlusRepository {
    override val status: Flow<PlusStatus> = prefs.plusStatus
    override val isStoreConnected: Boolean = false

    override suspend fun purchase(plan: PlusPlan): PurchaseResult {
        delay(600)
        prefs.setPlus(plan, time.today().plusMonths(plan.months.toLong()).toEpochDay())
        return PurchaseResult.Success
    }

    override suspend fun cancel() = prefs.setPlus(null, null)
}
