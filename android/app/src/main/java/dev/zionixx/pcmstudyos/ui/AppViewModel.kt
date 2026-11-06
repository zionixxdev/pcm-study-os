package dev.zionixx.pcmstudyos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.zionixx.pcmstudyos.data.local.entity.AcademicYear
import dev.zionixx.pcmstudyos.data.repo.ActiveYear
import dev.zionixx.pcmstudyos.data.repo.AppSettings
import dev.zionixx.pcmstudyos.data.repo.SettingsRepository
import dev.zionixx.pcmstudyos.data.repo.SyllabusRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val syllabusRepository: SyllabusRepository,
    private val settingsRepository: SettingsRepository,
    private val activeYear: ActiveYear,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val years: StateFlow<List<AcademicYear>> = syllabusRepository.years()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val currentYear: StateFlow<AcademicYear?> = combine(settings, years) { s, list ->
        list.firstOrNull { it.id == s.currentYearId } ?: list.firstOrNull { it.isCurrent } ?: list.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val ready = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            syllabusRepository.ensureSeedData()
            val stored = settingsRepository.settings.first().currentYearId
            val year = syllabusRepository.currentYear()
            if (year != null && (stored == null || stored != year.id)) {
                settingsRepository.setCurrentYear(year.id)
            }
            ready.value = true
        }
    }

    fun switchYear(yearId: String) {
        viewModelScope.launch {
            syllabusRepository.setCurrentYear(yearId)
            settingsRepository.setCurrentYear(yearId)
        }
    }

    fun createYear(name: String, start: String, end: String) {
        viewModelScope.launch { syllabusRepository.createYear(name, start, end) }
    }
}
