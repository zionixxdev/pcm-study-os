package dev.zionixx.pcmstudyos.data.repo

import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Inject
import javax.inject.Singleton

/** Single source of truth for "which academic year is active". */
@Singleton
class ActiveYear @Inject constructor(settingsRepository: SettingsRepository) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val id: StateFlow<String?> = settingsRepository.settings
        .map { it.currentYearId }
        .stateIn(scope, SharingStarted.Eagerly, null)
}
