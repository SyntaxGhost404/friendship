package app.nearfolk.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.nearfolk.core.common.TimeSource
import app.nearfolk.core.data.repository.PreferencesRepository
import app.nearfolk.core.data.repository.SessionRepository
import app.nearfolk.core.model.Gender
import app.nearfolk.core.model.ProfileDraft
import app.nearfolk.core.model.ProfileRules
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Period
import java.util.Locale
import javax.inject.Inject

data class OnboardingState(
    val step: Int = 0,
    val name: String = "",
    val gender: Gender? = null,
    val birthDate: LocalDate? = null,
    val countryCode: String? = Locale.getDefault().country.takeIf { it.length == 2 },
    val agreed: Boolean = false,
    val creating: Boolean = false,
) {
    val nameValid: Boolean get() = name.trim().length in ProfileRules.NAME_MIN..ProfileRules.NAME_MAX
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val session: SessionRepository,
    private val preferences: PreferencesRepository,
    private val time: TimeSource,
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    val storedProfileName: StateFlow<String?> =
        session.storedProfileName.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val today: LocalDate get() = time.today()

    fun isOldEnough(date: LocalDate): Boolean = Period.between(date, today).years >= ProfileRules.MIN_AGE

    fun setStep(step: Int) = _state.update { it.copy(step = step) }
    fun setName(value: String) = _state.update { it.copy(name = value.take(ProfileRules.NAME_MAX)) }
    fun setGender(value: Gender) = _state.update { it.copy(gender = value) }
    fun setBirthDate(value: LocalDate) = _state.update { it.copy(birthDate = value) }
    fun setCountry(code: String) = _state.update { it.copy(countryCode = code) }
    fun setAgreed(value: Boolean) = _state.update { it.copy(agreed = value) }

    fun continueStored() {
        viewModelScope.launch { session.continueStoredProfile() }
    }

    fun create() {
        val s = _state.value
        val gender = s.gender ?: return
        val birth = s.birthDate ?: return
        val country = s.countryCode ?: return
        if (!s.nameValid || !isOldEnough(birth) || !s.agreed || s.creating) return
        _state.update { it.copy(creating = true) }
        viewModelScope.launch {
            preferences.markNotificationPromptShown()
            session.createProfile(ProfileDraft(s.name, gender, birth, country))
        }
    }
}
