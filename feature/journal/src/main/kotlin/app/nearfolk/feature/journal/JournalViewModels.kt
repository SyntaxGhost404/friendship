package app.nearfolk.feature.journal

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import app.nearfolk.core.common.Passcode
import app.nearfolk.core.data.repository.JournalRepository
import app.nearfolk.core.data.repository.MemberRepository
import app.nearfolk.core.data.repository.PasscodeAttempt
import app.nearfolk.core.data.repository.PasscodeRepository
import app.nearfolk.core.model.JournalEntry
import app.nearfolk.core.model.Member
import app.nearfolk.core.model.Mood
import app.nearfolk.core.model.Visibility
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Messages shown under the keypad. */
sealed interface PasscodeMessage {
    data class Wrong(val triesLeft: Int) : PasscodeMessage
    data class Locked(val untilEpochMillis: Long) : PasscodeMessage
    data object Mismatch : PasscodeMessage
    data object TooSimple : PasscodeMessage
}

data class JournalUiState(
    val loading: Boolean = true,
    val passcodeSet: Boolean = false,
    val unlocked: Boolean = false,
    val entries: List<JournalEntry> = emptyList(),
)

@HiltViewModel
class JournalViewModel @Inject constructor(
    journal: JournalRepository,
    private val passcode: PasscodeRepository,
) : ViewModel() {
    val state: StateFlow<JournalUiState> = combine(
        passcode.isSet,
        passcode.unlocked,
        journal.myEntries(),
    ) { set, unlocked, entries -> JournalUiState(false, set, unlocked, entries) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), JournalUiState())

    private val _entry = MutableStateFlow("")
    val entry: StateFlow<String> = _entry
    private val _message = MutableStateFlow<PasscodeMessage?>(null)
    val message: StateFlow<PasscodeMessage?> = _message
    private val _shake = Channel<Unit>(Channel.CONFLATED)
    val shake = _shake.receiveAsFlow()

    fun digit(d: Int) {
        if (_entry.value.length >= Passcode.LENGTH) return
        _entry.update { it + d }
        if (_entry.value.length == Passcode.LENGTH) submit()
    }

    fun backspace() = _entry.update { it.dropLast(1) }

    private fun submit() {
        val code = _entry.value
        viewModelScope.launch {
            when (val result = passcode.attempt(code)) {
                PasscodeAttempt.Success -> _message.value = null
                is PasscodeAttempt.Wrong -> {
                    _message.value = PasscodeMessage.Wrong(result.attemptsBeforeWait)
                    _shake.send(Unit)
                }
                is PasscodeAttempt.Locked -> {
                    _message.value = PasscodeMessage.Locked(result.untilEpochMillis)
                    _shake.send(Unit)
                }
            }
            _entry.value = ""
        }
    }

    fun lock() = passcode.lock()
}

enum class PasscodeFlow { Create, Change, Remove }

@HiltViewModel
class PasscodeSetupViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val passcode: PasscodeRepository,
) : ViewModel() {
    val flow: PasscodeFlow = PasscodeFlow.valueOf(savedStateHandle.toRoute<PasscodeSetupRoute>().flow)

    enum class Stage { Current, Create, Confirm, Done }

    private val _stage = MutableStateFlow(if (flow == PasscodeFlow.Create) Stage.Create else Stage.Current)
    val stage: StateFlow<Stage> = _stage
    private val _entry = MutableStateFlow("")
    val entry: StateFlow<String> = _entry
    private val _message = MutableStateFlow<PasscodeMessage?>(null)
    val message: StateFlow<PasscodeMessage?> = _message
    private val _shake = Channel<Unit>(Channel.CONFLATED)
    val shake = _shake.receiveAsFlow()
    private var first: String? = null

    fun digit(d: Int) {
        if (_entry.value.length >= Passcode.LENGTH || _stage.value == Stage.Done) return
        _entry.update { it + d }
        if (_entry.value.length == Passcode.LENGTH) submit(_entry.value)
    }

    fun backspace() = _entry.update { it.dropLast(1) }

    private fun submit(code: String) {
        viewModelScope.launch {
            when (_stage.value) {
                Stage.Current -> when (val result = passcode.attempt(code)) {
                    PasscodeAttempt.Success -> {
                        _message.value = null
                        if (flow == PasscodeFlow.Remove) {
                            passcode.clear()
                            _stage.value = Stage.Done
                        } else {
                            _stage.value = Stage.Create
                        }
                    }
                    is PasscodeAttempt.Wrong -> fail(PasscodeMessage.Wrong(result.attemptsBeforeWait))
                    is PasscodeAttempt.Locked -> fail(PasscodeMessage.Locked(result.untilEpochMillis))
                }
                Stage.Create -> {
                    if (Passcode.validate(code) != null) {
                        fail(PasscodeMessage.TooSimple)
                    } else {
                        first = code
                        _message.value = null
                        _stage.value = Stage.Confirm
                    }
                }
                Stage.Confirm -> {
                    if (code == first) {
                        passcode.set(code)
                        _stage.value = Stage.Done
                    } else {
                        first = null
                        fail(PasscodeMessage.Mismatch)
                        _stage.value = Stage.Create
                    }
                }
                Stage.Done -> Unit
            }
            _entry.value = ""
        }
    }

    private suspend fun fail(message: PasscodeMessage) {
        _message.value = message
        _shake.send(Unit)
    }

    suspend fun isSet(): Boolean = passcode.isSet.first()
}

@HiltViewModel
class JournalEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val journal: JournalRepository,
) : ViewModel() {
    private val entryId: String? = savedStateHandle.toRoute<JournalEditorRoute>().entryId

    data class Draft(
        val title: String = "",
        val body: String = "",
        val mood: Mood = Mood.Calm,
        val visibility: Visibility = Visibility.Private,
        val loaded: Boolean = false,
        val isNew: Boolean = true,
    )

    private val _draft = MutableStateFlow(Draft(loaded = entryId == null))
    val draft: StateFlow<Draft> = _draft
    private val _done = Channel<Unit>(Channel.CONFLATED)
    val done = _done.receiveAsFlow()

    init {
        if (entryId != null) {
            viewModelScope.launch {
                journal.entry(entryId).first()?.let { e ->
                    _draft.value = Draft(e.title, e.body, e.mood, e.visibility, loaded = true, isNew = false)
                } ?: run { _draft.update { it.copy(loaded = true) } }
            }
        }
    }

    fun update(transform: (Draft) -> Draft) = _draft.update(transform)

    fun save() {
        val d = _draft.value
        if (d.body.isBlank()) return
        viewModelScope.launch {
            journal.save(entryId, d.title, d.body, d.mood, d.visibility)
            _done.send(Unit)
        }
    }

    fun delete() {
        val id = entryId ?: return
        viewModelScope.launch {
            journal.delete(id)
            _done.send(Unit)
        }
    }
}

@HiltViewModel
class PublicJournalViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    journal: JournalRepository,
    members: MemberRepository,
) : ViewModel() {
    private val memberId = savedStateHandle.toRoute<PublicJournalRoute>().memberId
    val member: StateFlow<Member?> = members.member(memberId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val entries: StateFlow<List<JournalEntry>?> = journal.publicEntries(memberId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
