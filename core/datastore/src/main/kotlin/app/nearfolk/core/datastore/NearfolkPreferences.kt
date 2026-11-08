package app.nearfolk.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.nearfolk.core.model.AccountState
import app.nearfolk.core.model.DiscoverFilter
import app.nearfolk.core.model.GenderFilter
import app.nearfolk.core.model.PlusPlan
import app.nearfolk.core.model.PlusStatus
import app.nearfolk.core.model.ThemeMode
import app.nearfolk.core.model.UserPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.store: DataStore<Preferences> by preferencesDataStore(name = "nearfolk_prefs")

/** Stored passcode material. Only the salted hash is kept. */
data class PasscodeRecord(val hash: String, val salt: String)

/**
 * Typed wrapper around Preferences DataStore. Every key the app persists
 * outside Room is declared here.
 */
@Singleton
class NearfolkPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private object Keys {
        val themeMode = stringPreferencesKey("theme_mode")
        val pureBlack = booleanPreferencesKey("pure_black")
        val notifyMessages = booleanPreferencesKey("notify_messages")
        val notifyActivity = booleanPreferencesKey("notify_activity")
        val haptics = booleanPreferencesKey("haptics")
        val filterGender = stringPreferencesKey("filter_gender")
        val filterCountries = stringSetPreferencesKey("filter_countries")
        val filterOnline = booleanPreferencesKey("filter_online")
        val filterAgeMin = intPreferencesKey("filter_age_min")
        val filterAgeMax = intPreferencesKey("filter_age_max")
        val dismissedBanners = stringSetPreferencesKey("dismissed_banners")
        val notificationPromptShown = booleanPreferencesKey("notification_prompt_shown")
        val accountState = stringPreferencesKey("account_state")
        val passcodeHash = stringPreferencesKey("passcode_hash")
        val passcodeSalt = stringPreferencesKey("passcode_salt")
        val passcodeFailures = intPreferencesKey("passcode_failures")
        val passcodeLockedUntil = longPreferencesKey("passcode_locked_until")
        val plusPlan = stringPreferencesKey("plus_plan")
        val plusRenews = longPreferencesKey("plus_renews_epoch_day")
        val seededVersion = intPreferencesKey("seeded_version")
        val introRequestsSent = intPreferencesKey("intro_requests_sent")
    }

    private val data: Flow<Preferences> = dataStore.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }

    val userPreferences: Flow<UserPreferences> = data.map { p ->
        val min = p[Keys.filterAgeMin]
        val max = p[Keys.filterAgeMax]
        UserPreferences(
            themeMode = p[Keys.themeMode]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.System,
            pureBlack = p[Keys.pureBlack] ?: false,
            notifyMessages = p[Keys.notifyMessages] ?: true,
            notifyActivity = p[Keys.notifyActivity] ?: true,
            haptics = p[Keys.haptics] ?: true,
            discoverFilter = DiscoverFilter(
                gender = p[Keys.filterGender]?.let { runCatching { GenderFilter.valueOf(it) }.getOrNull() } ?: GenderFilter.All,
                countries = p[Keys.filterCountries] ?: emptySet(),
                onlineOnly = p[Keys.filterOnline] ?: false,
                ageRange = if (min != null && max != null) min..max else null,
            ),
            dismissedSafetyBanners = p[Keys.dismissedBanners] ?: emptySet(),
            notificationPromptShown = p[Keys.notificationPromptShown] ?: false,
        )
    }.distinctUntilChanged()

    val accountState: Flow<AccountState> = data.map { p ->
        p[Keys.accountState]?.let { runCatching { AccountState.valueOf(it) }.getOrNull() } ?: AccountState.SignedOut
    }.distinctUntilChanged()

    val passcode: Flow<PasscodeRecord?> = data.map { p ->
        val hash = p[Keys.passcodeHash]
        val salt = p[Keys.passcodeSalt]
        if (hash != null && salt != null) PasscodeRecord(hash, salt) else null
    }.distinctUntilChanged()

    val passcodeFailures: Flow<Int> = data.map { it[Keys.passcodeFailures] ?: 0 }.distinctUntilChanged()
    val passcodeLockedUntil: Flow<Long> = data.map { it[Keys.passcodeLockedUntil] ?: 0L }.distinctUntilChanged()

    val plusStatus: Flow<PlusStatus> = data.map { p ->
        val plan = p[Keys.plusPlan]?.let { runCatching { PlusPlan.valueOf(it) }.getOrNull() }
        PlusStatus(active = plan != null, plan = plan, renewsOnEpochDay = p[Keys.plusRenews])
    }.distinctUntilChanged()

    suspend fun seededVersion(): Int = data.first()[Keys.seededVersion] ?: 0
    suspend fun introRequestsSent(): Int = data.first()[Keys.introRequestsSent] ?: 0

    suspend fun setThemeMode(mode: ThemeMode) = edit { it[Keys.themeMode] = mode.name }
    suspend fun setPureBlack(value: Boolean) = edit { it[Keys.pureBlack] = value }
    suspend fun setNotifyMessages(value: Boolean) = edit { it[Keys.notifyMessages] = value }
    suspend fun setNotifyActivity(value: Boolean) = edit { it[Keys.notifyActivity] = value }
    suspend fun setHaptics(value: Boolean) = edit { it[Keys.haptics] = value }
    suspend fun setNotificationPromptShown() = edit { it[Keys.notificationPromptShown] = true }
    suspend fun setAccountState(state: AccountState) = edit { it[Keys.accountState] = state.name }
    suspend fun setSeededVersion(version: Int) = edit { it[Keys.seededVersion] = version }
    suspend fun setIntroRequestsSent(count: Int) = edit { it[Keys.introRequestsSent] = count }

    suspend fun setDiscoverFilter(filter: DiscoverFilter) = edit { p ->
        p[Keys.filterGender] = filter.gender.name
        p[Keys.filterCountries] = filter.countries
        p[Keys.filterOnline] = filter.onlineOnly
        val range = filter.ageRange
        if (range != null) {
            p[Keys.filterAgeMin] = range.first
            p[Keys.filterAgeMax] = range.last
        } else {
            p.remove(Keys.filterAgeMin)
            p.remove(Keys.filterAgeMax)
        }
    }

    suspend fun dismissBanner(id: String) = edit { p ->
        p[Keys.dismissedBanners] = (p[Keys.dismissedBanners] ?: emptySet()) + id
    }

    suspend fun setPasscode(record: PasscodeRecord?) = edit { p ->
        if (record == null) {
            p.remove(Keys.passcodeHash)
            p.remove(Keys.passcodeSalt)
        } else {
            p[Keys.passcodeHash] = record.hash
            p[Keys.passcodeSalt] = record.salt
        }
        p[Keys.passcodeFailures] = 0
        p[Keys.passcodeLockedUntil] = 0L
    }

    suspend fun setPasscodeFailures(count: Int, lockedUntil: Long) = edit { p ->
        p[Keys.passcodeFailures] = count
        p[Keys.passcodeLockedUntil] = lockedUntil
    }

    suspend fun setPlus(plan: PlusPlan?, renewsOnEpochDay: Long?) = edit { p ->
        if (plan == null) {
            p.remove(Keys.plusPlan)
            p.remove(Keys.plusRenews)
        } else {
            p[Keys.plusPlan] = plan.name
            if (renewsOnEpochDay != null) p[Keys.plusRenews] = renewsOnEpochDay
        }
    }

    /** Wipes everything, used by account deletion. */
    suspend fun clearAll() = edit { it.clear() }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        dataStore.edit { block(it) }
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    @Provides
    @Singleton
    fun dataStore(@ApplicationContext context: Context): DataStore<Preferences> = context.store
}
