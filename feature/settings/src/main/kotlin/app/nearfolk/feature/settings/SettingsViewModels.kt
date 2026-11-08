package app.nearfolk.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.nearfolk.core.data.repository.ChatRepository
import app.nearfolk.core.data.repository.MemberRepository
import app.nearfolk.core.data.repository.PasscodeRepository
import app.nearfolk.core.data.repository.PlusRepository
import app.nearfolk.core.data.repository.PreferencesRepository
import app.nearfolk.core.data.repository.PurchaseResult
import app.nearfolk.core.data.repository.SessionRepository
import app.nearfolk.core.model.DeletionReason
import app.nearfolk.core.model.Member
import app.nearfolk.core.model.PlusPlan
import app.nearfolk.core.model.PlusStatus
import app.nearfolk.core.model.ThemeMode
import app.nearfolk.core.model.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val preferences: UserPreferences = UserPreferences(),
    val plus: PlusStatus = PlusStatus(false, null, null),
    val passcodeSet: Boolean = false,
    val blockedCount: Int = 0,
    val loaded: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: PreferencesRepository,
    private val session: SessionRepository,
    plus: PlusRepository,
    passcode: PasscodeRepository,
    members: MemberRepository,
) : ViewModel() {
    val state: StateFlow<SettingsUiState> = combine(
        preferences.preferences,
        plus.status,
        passcode.isSet,
        members.blockedMembers(),
    ) { prefs, plusStatus, passcodeSet, blocked ->
        SettingsUiState(prefs, plusStatus, passcodeSet, blocked.size, loaded = true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setTheme(mode: ThemeMode) = viewModelScope.launch { preferences.setThemeMode(mode) }
    fun setPureBlack(value: Boolean) = viewModelScope.launch { preferences.setPureBlack(value) }
    fun setNotifyMessages(value: Boolean) = viewModelScope.launch { preferences.setNotifyMessages(value) }
    fun setNotifyActivity(value: Boolean) = viewModelScope.launch { preferences.setNotifyActivity(value) }
    fun setHaptics(value: Boolean) = viewModelScope.launch { preferences.setHaptics(value) }
    fun deactivate() = viewModelScope.launch { session.deactivate() }
    fun signOut() = viewModelScope.launch { session.signOut() }
}

@HiltViewModel
class ReactivateViewModel @Inject constructor(private val session: SessionRepository) : ViewModel() {
    fun reactivate() = viewModelScope.launch { session.reactivate() }
    fun signOut() = viewModelScope.launch { session.signOut() }
}

@HiltViewModel
class BlockedViewModel @Inject constructor(
    members: MemberRepository,
    private val chat: ChatRepository,
) : ViewModel() {
    val blocked: StateFlow<List<Member>?> = members.blockedMembers().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    fun unblock(id: String) = viewModelScope.launch { chat.unblock(id) }
}

@HiltViewModel
class PlusViewModel @Inject constructor(private val plus: PlusRepository) : ViewModel() {
    val status: StateFlow<PlusStatus?> = plus.status.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val storeConnected: Boolean = plus.isStoreConnected
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy
    private val _purchased = Channel<Unit>(Channel.CONFLATED)
    val purchased = _purchased.receiveAsFlow()

    fun purchase(plan: PlusPlan) {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            if (plus.purchase(plan) == PurchaseResult.Success) _purchased.send(Unit)
            _busy.value = false
        }
    }

    fun cancel() = viewModelScope.launch { plus.cancel() }
}

@HiltViewModel
class DeleteAccountViewModel @Inject constructor(private val session: SessionRepository) : ViewModel() {
    private val _deleting = MutableStateFlow(false)
    val deleting: StateFlow<Boolean> = _deleting

    fun deactivate() = viewModelScope.launch { session.deactivate() }

    fun delete(reason: DeletionReason, details: String?) {
        if (_deleting.value) return
        _deleting.value = true
        viewModelScope.launch { session.deleteAccount(reason, details) }
    }
}
