package app.nearfolk.core.data.media

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import app.nearfolk.core.data.di.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Records short AAC clips for the voice intro. One recording at a time. */
@Singleton
class VoiceRecorder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val files: MediaFiles,
) {
    private var recorder: MediaRecorder? = null
    private var output: File? = null
    private var startedAt = 0L

    val isRecording: Boolean get() = recorder != null

    fun start(): Result<File> = runCatching {
        cancel()
        val file = files.newVoiceFile()
        val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
        r.setAudioSource(MediaRecorder.AudioSource.MIC)
        r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        r.setAudioEncodingBitRate(96_000)
        r.setAudioSamplingRate(44_100)
        r.setOutputFile(file.absolutePath)
        r.prepare()
        r.start()
        recorder = r
        output = file
        startedAt = SystemClock.elapsedRealtime()
        file
    }

    fun elapsedMillis(): Long = if (recorder != null) SystemClock.elapsedRealtime() - startedAt else 0L

    /** 0 to 1 loudness since the last call. */
    fun level(): Float = runCatching { (recorder?.maxAmplitude ?: 0) / 32_767f }.getOrDefault(0f).coerceIn(0f, 1f)

    /** Stops and returns the clip with its length, or a failure when it was too short to keep. */
    fun stop(): Result<Pair<File, Long>> {
        val r = recorder ?: return Result.failure(IllegalStateException("Not recording"))
        val file = output
        val duration = elapsedMillis()
        recorder = null
        output = null
        return runCatching {
            r.stop()
            r.release()
            requireNotNull(file) to duration
        }.onFailure {
            r.release()
            file?.delete()
        }
    }

    fun cancel() {
        recorder?.let { r -> runCatching { r.stop() }; r.release() }
        recorder = null
        output?.delete()
        output = null
    }
}

data class PlaybackState(
    val path: String? = null,
    val playing: Boolean = false,
    val positionMillis: Long = 0,
    val durationMillis: Long = 0,
)

/** Plays one clip at a time and publishes progress. */
@Singleton
class VoicePlayer @Inject constructor(
    @ApplicationScope private val scope: CoroutineScope,
) {
    private var player: MediaPlayer? = null
    private var ticker: Job? = null
    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    fun toggle(path: String) {
        val current = player
        if (current != null && _state.value.path == path) {
            if (current.isPlaying) {
                current.pause()
                _state.value = _state.value.copy(playing = false)
            } else {
                current.start()
                _state.value = _state.value.copy(playing = true)
                tick()
            }
            return
        }
        stop()
        val p = MediaPlayer()
        runCatching {
            p.setDataSource(path)
            p.prepare()
        }.onFailure {
            p.release()
            return
        }
        p.setOnCompletionListener { stop() }
        p.start()
        player = p
        _state.value = PlaybackState(path, true, 0, p.duration.toLong())
        tick()
    }

    private fun tick() {
        ticker?.cancel()
        ticker = scope.launch {
            while (isActive) {
                val p = player ?: break
                _state.value = _state.value.copy(positionMillis = runCatching { p.currentPosition.toLong() }.getOrDefault(0))
                delay(50)
            }
        }
    }

    fun stop() {
        ticker?.cancel()
        player?.let { runCatching { it.stop() }; it.release() }
        player = null
        _state.value = PlaybackState()
    }
}
