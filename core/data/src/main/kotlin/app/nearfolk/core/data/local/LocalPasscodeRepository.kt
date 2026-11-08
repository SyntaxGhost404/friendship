package app.nearfolk.core.data.local

import app.nearfolk.core.common.Passcode
import app.nearfolk.core.common.TimeSource
import app.nearfolk.core.data.di.ApplicationScope
import app.nearfolk.core.data.platform.AppVisibility
import app.nearfolk.core.data.repository.PasscodeAttempt
import app.nearfolk.core.data.repository.PasscodeRepository
import app.nearfolk.core.datastore.NearfolkPreferences
import app.nearfolk.core.datastore.PasscodeRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Journal gate. Unlocking lasts until the app goes to the background, then
 * the journal locks again.
 */
@Singleton
internal class LocalPasscodeRepository @Inject constructor(
    private val prefs: NearfolkPreferences,
    private val time: TimeSource,
    visibility: AppVisibility,
    @ApplicationScope scope: CoroutineScope,
) : PasscodeRepository {

    private val _unlocked = MutableStateFlow(false)
    override val unlocked: StateFlow<Boolean> = _unlocked.asStateFlow()

    override val isSet: Flow<Boolean> = prefs.passcode.map { it != null }
    override val lockedUntil: Flow<Long> = prefs.passcodeLockedUntil

    init {
        scope.launch {
            visibility.foreground.collect { fg -> if (!fg) _unlocked.value = false }
        }
    }

    override suspend fun set(code: String) {
        require(Passcode.validate(code) == null) { "Invalid passcode" }
        val hashed = withContext(Dispatchers.Default) { Passcode.hash(code) }
        prefs.setPasscode(PasscodeRecord(hashed.hash, hashed.salt))
        _unlocked.value = true
    }

    override suspend fun clear() {
        prefs.setPasscode(null)
        _unlocked.value = false
    }

    override suspend fun attempt(code: String): PasscodeAttempt {
        val now = time.now().toEpochMilli()
        val lockedUntil = prefs.passcodeLockedUntil.first()
        if (lockedUntil > now) return PasscodeAttempt.Locked(lockedUntil)
        val record = prefs.passcode.first() ?: return PasscodeAttempt.Success.also { _unlocked.value = true }
        val ok = withContext(Dispatchers.Default) { Passcode.verify(code, Passcode.Hashed(record.hash, record.salt)) }
        if (ok) {
            prefs.setPasscodeFailures(0, 0L)
            _unlocked.value = true
            return PasscodeAttempt.Success
        }
        val failures = prefs.passcodeFailures.first() + 1
        val wait = Passcode.lockoutMillis(failures)
        prefs.setPasscodeFailures(failures, if (wait > 0) now + wait else 0L)
        return if (wait > 0) PasscodeAttempt.Locked(now + wait) else PasscodeAttempt.Wrong(Passcode.FREE_ATTEMPTS - failures)
    }

    override fun lock() {
        _unlocked.value = false
    }
}
