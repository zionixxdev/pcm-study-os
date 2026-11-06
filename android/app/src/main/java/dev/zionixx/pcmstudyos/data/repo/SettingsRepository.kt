package dev.zionixx.pcmstudyos.data.repo

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pcm_settings")

data class AppSettings(
    val theme: String = "dark",           // dark | light | system
    val accent: String = "#4F8CFF",
    val fontScale: String = "base",       // sm | base | lg
    val compact: Boolean = false,
    val dailyTargetMinutes: Int = 240,
    val studyDays: Set<Int> = setOf(1, 2, 3, 4, 5, 6),
    val pomodoroFocus: Int = 25,
    val pomodoroShortBreak: Int = 5,
    val pomodoroLongBreak: Int = 15,
    val pomodoroRounds: Int = 4,
    val revisionIntervals: List<Int> = listOf(1, 3, 7, 14, 30),
    val defaultPriority: String = "medium",
    val notificationsEnabled: Boolean = true,
    val currentYearId: String? = null,
    val onboarded: Boolean = false,
)

/**
 * Preferences live in DataStore — never in the main database, and never as a
 * replacement for it. Only settings and the selected year are stored here.
 */
@Singleton
class SettingsRepository @Inject constructor(@ApplicationContext private val context: Context) {

    private object Keys {
        val theme = stringPreferencesKey("theme")
        val accent = stringPreferencesKey("accent")
        val fontScale = stringPreferencesKey("font_scale")
        val compact = booleanPreferencesKey("compact")
        val dailyTarget = intPreferencesKey("daily_target_minutes")
        val studyDays = stringSetPreferencesKey("study_days")
        val pomodoroFocus = intPreferencesKey("pomodoro_focus")
        val pomodoroShort = intPreferencesKey("pomodoro_short")
        val pomodoroLong = intPreferencesKey("pomodoro_long")
        val pomodoroRounds = intPreferencesKey("pomodoro_rounds")
        val revisionIntervals = stringPreferencesKey("revision_intervals")
        val defaultPriority = stringPreferencesKey("default_priority")
        val notifications = booleanPreferencesKey("notifications")
        val currentYearId = stringPreferencesKey("current_year_id")
        val onboarded = booleanPreferencesKey("onboarded")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            theme = prefs[Keys.theme] ?: "dark",
            accent = prefs[Keys.accent] ?: "#4F8CFF",
            fontScale = prefs[Keys.fontScale] ?: "base",
            compact = prefs[Keys.compact] ?: false,
            dailyTargetMinutes = prefs[Keys.dailyTarget] ?: 240,
            studyDays = prefs[Keys.studyDays]?.mapNotNull { it.toIntOrNull() }?.toSet() ?: setOf(1, 2, 3, 4, 5, 6),
            pomodoroFocus = prefs[Keys.pomodoroFocus] ?: 25,
            pomodoroShortBreak = prefs[Keys.pomodoroShort] ?: 5,
            pomodoroLongBreak = prefs[Keys.pomodoroLong] ?: 15,
            pomodoroRounds = prefs[Keys.pomodoroRounds] ?: 4,
            revisionIntervals = prefs[Keys.revisionIntervals]?.split(",")?.mapNotNull { it.trim().toIntOrNull() }?.ifEmpty { null }
                ?: listOf(1, 3, 7, 14, 30),
            defaultPriority = prefs[Keys.defaultPriority] ?: "medium",
            notificationsEnabled = prefs[Keys.notifications] ?: true,
            currentYearId = prefs[Keys.currentYearId],
            onboarded = prefs[Keys.onboarded] ?: false,
        )
    }

    suspend fun setTheme(value: String) = context.dataStore.edit { it[Keys.theme] = value }

    suspend fun setAccent(value: String) = context.dataStore.edit { it[Keys.accent] = value }

    suspend fun setFontScale(value: String) = context.dataStore.edit { it[Keys.fontScale] = value }

    suspend fun setCompact(value: Boolean) = context.dataStore.edit { it[Keys.compact] = value }

    suspend fun setDailyTarget(minutes: Int) =
        context.dataStore.edit { it[Keys.dailyTarget] = minutes.coerceIn(15, 900) }

    suspend fun setStudyDays(days: Set<Int>) =
        context.dataStore.edit { it[Keys.studyDays] = days.map { day -> day.toString() }.toSet() }

    suspend fun setPomodoro(focus: Int, shortBreak: Int, longBreak: Int, rounds: Int) = context.dataStore.edit {
        it[Keys.pomodoroFocus] = focus.coerceIn(1, 180)
        it[Keys.pomodoroShort] = shortBreak.coerceIn(1, 60)
        it[Keys.pomodoroLong] = longBreak.coerceIn(1, 120)
        it[Keys.pomodoroRounds] = rounds.coerceIn(1, 12)
    }

    suspend fun setRevisionIntervals(intervals: List<Int>) = context.dataStore.edit {
        val cleaned = intervals.filter { value -> value > 0 }.ifEmpty { listOf(1, 3, 7, 14, 30) }
        it[Keys.revisionIntervals] = cleaned.joinToString(",")
    }

    suspend fun setDefaultPriority(value: String) = context.dataStore.edit { it[Keys.defaultPriority] = value }

    suspend fun setNotificationsEnabled(value: Boolean) = context.dataStore.edit { it[Keys.notifications] = value }

    suspend fun setCurrentYear(yearId: String?) = context.dataStore.edit {
        if (yearId == null) it.remove(Keys.currentYearId) else it[Keys.currentYearId] = yearId
    }

    suspend fun setOnboarded(value: Boolean) = context.dataStore.edit { it[Keys.onboarded] = value }
}
