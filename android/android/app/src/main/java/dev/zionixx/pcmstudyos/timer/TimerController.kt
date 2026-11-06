package dev.zionixx.pcmstudyos.timer

import android.os.SystemClock
import dev.zionixx.pcmstudyos.data.repo.SettingsRepository
import dev.zionixx.pcmstudyos.data.repo.StudyRepository
import dev.zionixx.pcmstudyos.data.local.entity.StudySession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

enum class TimerMode { POMODORO, FOCUS, STOPWATCH }
enum class TimerPhase { FOCUS, SHORT_BREAK, LONG_BREAK }

data class TimerState(
    val mode: TimerMode = TimerMode.POMODORO,
    val phase: TimerPhase = TimerPhase.FOCUS,
    val running: Boolean = false,
    val accumulatedMs: Long = 0L,
    val plannedMs: Long = 25 * 60_000L,
    val round: Int = 1,
    val subjectId: String? = null,
    val chapterId: String? = null,
    val topicId: String? = null,
    val notes: String = "",
    val startedAtMillis: Long? = null,
    val sessionStartEpoch: Long = System.currentTimeMillis(),
) {
    /** Real focused time, derived from the monotonic clock so it stays correct
     * while the app is in the background or the screen is off. */
    fun elapsedMs(nowElapsed: Long = SystemClock.elapsedRealtime()): Long {
        val live = if (running && startedAtMillis != null) (nowElapsed - startedAtMillis).coerceAtLeast(0) else 0L
        return accumulatedMs + live
    }

    fun remainingMs(nowElapsed: Long = SystemClock.elapsedRealtime()): Long =
        if (mode == TimerMode.STOPWATCH) 0L else (plannedMs - elapsedMs(nowElapsed)).coerceAtLeast(0L)

    fun minutesFocused(nowElapsed: Long = SystemClock.elapsedRealtime()): Int =
        (elapsedMs(nowElapsed) / 60_000L).toInt()

    fun progress(nowElapsed: Long = SystemClock.elapsedRealtime()): Float =
        if (mode == TimerMode.STOPWATCH || plannedMs <= 0L) 0f
        else (elapsedMs(nowElapsed).toFloat() / plannedMs).coerceIn(0f, 1f)
}

/**
 * Holds the single running timer. The foreground service drives it, so the
 * countdown survives the app going to the background; the UI only observes.
 */
@Singleton
class TimerController @Inject constructor() {

    private val _state = MutableStateFlow(TimerState())
    val state: StateFlow<TimerState> = _state

    fun configure(mode: TimerMode, focusMinutes: Int, shortBreak: Int, longBreak: Int) {
        val planned = when (mode) {
            TimerMode.POMODORO -> focusMinutes * 60_000L
            TimerMode.FOCUS -> focusMinutes * 60_000L
            TimerMode.STOPWATCH -> 0L
        }
        _state.value = _state.value.copy(
            mode = mode,
            phase = TimerPhase.FOCUS,
            running = false,
            accumulatedMs = 0L,
            plannedMs = planned,
            startedAtMillis = null,
            sessionStartEpoch = System.currentTimeMillis(),
        )
    }

    fun select(mode: TimerMode, subjectId: String?, chapterId: String?, topicId: String?) {
        _state.value = _state.value.copy(
            mode = mode,
            subjectId = subjectId,
            chapterId = chapterId,
            topicId = topicId,
        )
    }

    fun setNotes(notes: String) {
        _state.value = _state.value.copy(notes = notes)
    }

    fun setPlannedMinutes(minutes: Int) {
        _state.value = _state.value.copy(plannedMs = minutes.coerceIn(1, 240) * 60_000L)
    }

    fun start() {
        val current = _state.value
        if (current.running) return
        _state.value = current.copy(
            running = true,
            startedAtMillis = SystemClock.elapsedRealtime(),
            sessionStartEpoch = if (current.accumulatedMs == 0L) System.currentTimeMillis() else current.sessionStartEpoch,
        )
    }

    fun pause() {
        val current = _state.value
        if (!current.running) return
        val elapsed = current.elapsedMs()
        _state.value = current.copy(running = false, accumulatedMs = elapsed, startedAtMillis = null)
    }

    fun reset() {
        _state.value = _state.value.copy(
            running = false,
            accumulatedMs = 0L,
            startedAtMillis = null,
            sessionStartEpoch = System.currentTimeMillis(),
            phase = TimerPhase.FOCUS,
        )
    }

    /** Advances the phase after a completed focus/break block. */
    fun advancePhase(rounds: Int) {
        val current = _state.value
        val next = when (current.phase) {
            TimerPhase.FOCUS -> if ((current.round + 1) % rounds.coerceAtLeast(1) == 1) TimerPhase.LONG_BREAK else TimerPhase.SHORT_BREAK
            else -> TimerPhase.FOCUS
        }
        val nextRound = if (current.phase == TimerPhase.FOCUS) current.round + 1 else current.round
        _state.value = current.copy(
            phase = next,
            round = nextRound,
            running = false,
            accumulatedMs = 0L,
            startedAtMillis = null,
            sessionStartEpoch = System.currentTimeMillis(),
        )
    }

    fun setPhaseDuration(minutes: Int) {
        _state.value = _state.value.copy(plannedMs = minutes.coerceIn(1, 180) * 60_000L)
    }

    /** Builds the session row for the time actually focused. */
    fun buildSession(yearId: String): StudySession? {
        val current = _state.value
        val minutes = current.minutesFocused()
        if (minutes < 1) return null
        val ended = System.currentTimeMillis()
        return StudySession(
            yearId = yearId,
            subjectId = current.subjectId,
            chapterId = current.chapterId,
            topicId = current.topicId,
            startedAt = current.sessionStartEpoch,
            endedAt = ended,
            minutes = minutes,
            mode = when (current.mode) {
                TimerMode.POMODORO -> "pomodoro"
                TimerMode.FOCUS -> "focus"
                TimerMode.STOPWATCH -> "stopwatch"
            },
            notes = current.notes.ifBlank { null },
        )
    }
}

/** Persists whatever the timer accumulated and resets it. */
suspend fun TimerController.commit(
    studyRepository: StudyRepository,
    settingsRepository: SettingsRepository,
): Int {
    val settings = settingsRepository.settings.first()
    val yearId = settings.currentYearId ?: return 0
    val session = buildSession(yearId) ?: return 0
    studyRepository.recordSession(session)
    reset()
    return session.minutes
}
