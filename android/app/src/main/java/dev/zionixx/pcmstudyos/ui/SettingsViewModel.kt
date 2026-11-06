package dev.zionixx.pcmstudyos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.zionixx.pcmstudyos.data.local.entity.AcademicYear
import dev.zionixx.pcmstudyos.data.repo.ActiveYear
import dev.zionixx.pcmstudyos.data.repo.AppSettings
import dev.zionixx.pcmstudyos.data.repo.BackupRepository
import dev.zionixx.pcmstudyos.data.repo.SettingsRepository
import dev.zionixx.pcmstudyos.data.local.PcmDatabase
import java.io.InputStream
import java.io.OutputStream
import dev.zionixx.pcmstudyos.data.repo.SyllabusRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val syllabusRepository: SyllabusRepository,
    private val backupRepository: BackupRepository,
    private val database: PcmDatabase,
    activeYear: ActiveYear,
) : ViewModel() {

    val settings = settingsRepository.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())
    val years: StateFlow<List<AcademicYear>> = syllabusRepository.years().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val activeYearId: StateFlow<String?> = activeYear.id

    fun setTheme(value: String) = viewModelScope.launch { settingsRepository.setTheme(value) }
    fun setAccent(value: String) = viewModelScope.launch { settingsRepository.setAccent(value) }
    fun setFontScale(value: String) = viewModelScope.launch { settingsRepository.setFontScale(value) }
    fun setCompact(value: Boolean) = viewModelScope.launch { settingsRepository.setCompact(value) }
    fun setDailyTarget(minutes: Int) = viewModelScope.launch { settingsRepository.setDailyTarget(minutes) }
    fun setStudyDays(days: Set<Int>) = viewModelScope.launch { settingsRepository.setStudyDays(days) }
    fun setPomodoro(focus: Int, short: Int, long: Int, rounds: Int) =
        viewModelScope.launch { settingsRepository.setPomodoro(focus, short, long, rounds) }
    fun setIntervals(list: List<Int>) = viewModelScope.launch { settingsRepository.setRevisionIntervals(list) }
    fun setPriority(value: String) = viewModelScope.launch { settingsRepository.setDefaultPriority(value) }
    fun setNotifications(value: Boolean) = viewModelScope.launch { settingsRepository.setNotificationsEnabled(value) }

    fun switchYear(yearId: String) = viewModelScope.launch {
        syllabusRepository.setCurrentYear(yearId)
        settingsRepository.setCurrentYear(yearId)
    }

    fun createYear(name: String, start: String, end: String) = viewModelScope.launch {
        syllabusRepository.createYear(name, start, end)
    }

    fun renameYear(year: AcademicYear) = viewModelScope.launch { syllabusRepository.updateYear(year) }
    fun archiveYear(yearId: String, archived: Boolean) = viewModelScope.launch { syllabusRepository.archiveYear(yearId, archived) }
    fun deleteYear(yearId: String) = viewModelScope.launch { syllabusRepository.deleteYear(yearId) }

    fun addSeedSyllabus(yearId: String) = viewModelScope.launch { syllabusRepository.addSeedSyllabus(yearId) }

    suspend fun writeBackup(out: OutputStream): Int = backupRepository.writeBackup(out)

    suspend fun restoreBackup(input: InputStream, replace: Boolean) = backupRepository.restore(input, replace)

    suspend fun writeCsv(table: BackupRepository.CsvTable, yearId: String, out: OutputStream) =
        backupRepository.writeCsv(table, yearId, out)

    suspend fun writePdf(yearId: String, out: OutputStream) = backupRepository.writePdfReport(yearId, out)

    /** Raw PRAGMA — Room cannot model it as a @Query, so it runs on the open helper. */
    suspend fun integrityCheck(): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        runCatching {
            database.openHelper.readableDatabase.query("PRAGMA integrity_check").use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else "unknown"
            }
        }.getOrDefault("check failed")
    }

    fun resetDatabase() = viewModelScope.launch {
        val maintenance = database.maintenanceDao()
        maintenance.clearTestResults()
        maintenance.clearRevisionHistory()
        maintenance.clearRevisions()
        maintenance.clearSessions()
        maintenance.clearStreaks()
        maintenance.clearTasks()
        maintenance.clearFormulas()
        maintenance.clearPractice()
        maintenance.clearMistakes()
        maintenance.clearExams()
        maintenance.clearNotes()
        maintenance.clearGoals()
        maintenance.clearComponents()
        maintenance.clearSubtopics()
        maintenance.clearTopics()
        maintenance.clearChapters()
        maintenance.clearSubjects()
        maintenance.clearYears()
        syllabusRepository.ensureSeedData()
    }
}
