package app.nearfolk.core.data.local

import app.nearfolk.core.data.repository.PreferencesRepository
import app.nearfolk.core.datastore.NearfolkPreferences
import app.nearfolk.core.model.DiscoverFilter
import app.nearfolk.core.model.ThemeMode
import app.nearfolk.core.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class LocalPreferencesRepository @Inject constructor(
    private val prefs: NearfolkPreferences,
) : PreferencesRepository {
    override val preferences: Flow<UserPreferences> = prefs.userPreferences
    override suspend fun setThemeMode(mode: ThemeMode) = prefs.setThemeMode(mode)
    override suspend fun setPureBlack(enabled: Boolean) = prefs.setPureBlack(enabled)
    override suspend fun setNotifyMessages(enabled: Boolean) = prefs.setNotifyMessages(enabled)
    override suspend fun setNotifyActivity(enabled: Boolean) = prefs.setNotifyActivity(enabled)
    override suspend fun setHaptics(enabled: Boolean) = prefs.setHaptics(enabled)
    override suspend fun setDiscoverFilter(filter: DiscoverFilter) = prefs.setDiscoverFilter(filter)
    override suspend fun dismissSafetyBanner(conversationId: String) = prefs.dismissBanner(conversationId)
    override suspend fun markNotificationPromptShown() = prefs.setNotificationPromptShown()
}
