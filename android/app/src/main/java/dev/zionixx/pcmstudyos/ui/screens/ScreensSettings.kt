package dev.zionixx.pcmstudyos.ui.screens

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zionixx.pcmstudyos.data.local.entity.AcademicYear
import dev.zionixx.pcmstudyos.data.repo.BackupRepository
import dev.zionixx.pcmstudyos.ui.SettingsViewModel
import dev.zionixx.pcmstudyos.ui.components.ConfirmDialog
import dev.zionixx.pcmstudyos.ui.components.PcmCard
import dev.zionixx.pcmstudyos.ui.components.PcmTextField
import dev.zionixx.pcmstudyos.ui.components.Pill
import dev.zionixx.pcmstudyos.ui.components.ScreenTitle
import dev.zionixx.pcmstudyos.ui.components.SectionHeader
import dev.zionixx.pcmstudyos.ui.theme.Danger
import dev.zionixx.pcmstudyos.ui.theme.Success
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun SettingsScreen(vm: SettingsViewModel = hiltViewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val years by vm.years.collectAsStateWithLifecycle()
    val activeYearId by vm.activeYearId.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var message by remember { mutableStateOf<String?>(null) }
    var showYearDialog by remember { mutableStateOf(false) }
    var editYear by remember { mutableStateOf<AcademicYear?>(null) }
    var deleteYear by remember { mutableStateOf<AcademicYear?>(null) }
    var confirmReset by remember { mutableStateOf(false) }
    var pendingRestoreReplace by remember { mutableStateOf(false) }
    var intervalsText by remember(settings.revisionIntervals) { mutableStateOf(settings.revisionIntervals.joinToString(", ")) }

    val createBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        val count = vm.writeBackup(out)
                        message = "Backup written with $count records."
                    }
                }.onFailure { message = "Could not write the backup: ${it.message}" }
            }
        }
    }

    val openBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        val result = vm.restoreBackup(input, pendingRestoreReplace)
                        message = result.message
                    }
                }.onFailure { message = "Could not read the backup: ${it.message}" }
                pendingRestoreReplace = false
            }
        }
    }

    val createCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            scope.launch {
                val yearId = activeYearId ?: return@launch
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        vm.writeCsv(BackupRepository.CsvTable.SYLLABUS, yearId, out)
                        message = "CSV exported."
                    }
                }.onFailure { message = "Export failed: ${it.message}" }
            }
        }
    }

    val createPdf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) {
            scope.launch {
                val yearId = activeYearId ?: return@launch
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        vm.writePdf(yearId, out)
                        message = "PDF report exported."
                    }
                }.onFailure { message = "Export failed: ${it.message}" }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { ScreenTitle("Settings", "Offline, local-only — nothing leaves this device") }

        message?.let { text ->
            item {
                PcmCard {
                    Text(text, Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall, color = Success)
                }
            }
        }

        item {
            PcmCard {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader("Appearance")
                    Text("Theme", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("dark", "light", "system").forEach { value ->
                            OutlinedButton(onClick = { vm.setTheme(value) }) {
                                Text(value, color = if (settings.theme == value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Text("Accent", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("#4F8CFF", "#39C6A4", "#F2A03D", "#C084FC", "#F87171").forEach { hex ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                androidx.compose.foundation.layout.Box(
                                    Modifier
                                        .size(28.dp)
                                        .background(parseColor(hex), RoundedCornerShape(50)),
                                )
                                if (settings.accent == hex) Text("•", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("sm" to "Small", "base" to "Default", "lg" to "Large").forEach { (value, label) ->
                            OutlinedButton(onClick = { vm.setFontScale(value) }) {
                                Text(label, style = MaterialTheme.typography.labelSmall, color = if (settings.fontScale == value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        item {
            PcmCard {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader("Study preferences")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f)) {
                            PcmTextField(
                                settings.dailyTargetMinutes.toString(),
                                { vm.setDailyTarget(it.toIntOrNull() ?: 240) },
                                "Daily target (min)",
                                keyboardType = KeyboardType.Number,
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            PcmTextField(
                                intervalsText,
                                {
                                    intervalsText = it
                                    val list = it.split(",").mapNotNull { part -> part.trim().toIntOrNull() }.filter { n -> n > 0 }
                                    if (list.isNotEmpty()) vm.setIntervals(list)
                                },
                                "Revision intervals (days)",
                            )
                        }
                    }
                    Text("Study days", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEachIndexed { index, label ->
                            val day = index + 1
                            val active = settings.studyDays.contains(day)
                            OutlinedButton(onClick = {
                                vm.setStudyDays(if (active) settings.studyDays - day else settings.studyDays + day)
                            }) {
                                Text(label, style = MaterialTheme.typography.labelSmall, color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f)) {
                            PcmTextField(
                                settings.pomodoroFocus.toString(),
                                { vm.setPomodoro(it.toIntOrNull() ?: 25, settings.pomodoroShortBreak, settings.pomodoroLongBreak, settings.pomodoroRounds) },
                                "Focus",
                                keyboardType = KeyboardType.Number,
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            PcmTextField(
                                settings.pomodoroShortBreak.toString(),
                                { vm.setPomodoro(settings.pomodoroFocus, it.toIntOrNull() ?: 5, settings.pomodoroLongBreak, settings.pomodoroRounds) },
                                "Short",
                                keyboardType = KeyboardType.Number,
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            PcmTextField(
                                settings.pomodoroLongBreak.toString(),
                                { vm.setPomodoro(settings.pomodoroFocus, settings.pomodoroShortBreak, it.toIntOrNull() ?: 15, settings.pomodoroRounds) },
                                "Long",
                                keyboardType = KeyboardType.Number,
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("high", "medium", "low").forEach { value ->
                            OutlinedButton(onClick = { vm.setPriority(value) }) {
                                Text(value, style = MaterialTheme.typography.labelSmall, color = if (settings.defaultPriority == value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    OutlinedButton(onClick = { vm.setNotifications(!settings.notificationsEnabled) }) {
                        Text(if (settings.notificationsEnabled) "Reminders: on" else "Reminders: off")
                    }
                }
            }
        }

        item {
            PcmCard {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeader(
                        "Academic years",
                        action = {
                            TextButton(onClick = { editYear = null; showYearDialog = true }) { Text("Add") }
                        },
                    )
                    years.forEach { year ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(year.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "${year.startDate} → ${year.endDate}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (year.id == activeYearId) {
                                Pill("current", MaterialTheme.colorScheme.primary)
                            } else {
                                TextButton(onClick = { vm.switchYear(year.id) }) { Text("Use") }
                            }
                            TextButton(onClick = { editYear = year; showYearDialog = true }) { Text("Edit") }
                            TextButton(onClick = { deleteYear = year }) { Text("Delete", color = Danger) }
                        }
                    }
                    Text(
                        "Progress, tasks and analytics always follow the selected year. Archiving keeps every record.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = { activeYearId?.let { vm.addSeedSyllabus(it) } }) {
                        Text("Add CBSE seed syllabus to this year")
                    }
                }
            }
        }

        item {
            PcmCard {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeader("Data")
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { createBackup.launch("pcm-study-os-backup-${LocalDate.now()}.json") }) {
                            Text("JSON backup")
                        }
                        OutlinedButton(onClick = {
                            pendingRestoreReplace = false
                            openBackup.launch(arrayOf("application/json"))
                        }) { Text("Restore") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { createCsv.launch("pcm-syllabus-${LocalDate.now()}.csv") }) { Text("CSV") }
                        OutlinedButton(onClick = { createPdf.launch("pcm-progress-${LocalDate.now()}.pdf") }) { Text("PDF report") }
                    }
                    OutlinedButton(onClick = {
                        scope.launch { message = "Database integrity: ${vm.integrityCheck()}" }
                    }) { Text("Check database integrity") }
                    Text(
                        "Exports go wherever you choose in the Android file picker. No upload, no account.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            PcmCard {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeader("About")
                    Text("PCM Study OS 1.0.0 (native Android)", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Local-first: Room database on this device, no network permission, no accounts, no analytics.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = { confirmReset = true }) { Text("Reset all data", color = Danger) }
                }
            }
        }
    }

    if (showYearDialog) {
        var name by remember { mutableStateOf(editYear?.name ?: "") }
        var start by remember { mutableStateOf(editYear?.startDate ?: "${LocalDate.now().year}-04-01") }
        var end by remember { mutableStateOf(editYear?.endDate ?: "${LocalDate.now().year + 1}-03-31") }
        var withSyllabus by remember { mutableStateOf(true) }
        var error by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showYearDialog = false },
            title = { Text(if (editYear == null) "New academic year" else "Edit academic year") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PcmTextField(name, { name = it }, "Name *", placeholder = "Class 12 — 2027–28")
                    PcmTextField(start, { start = it }, "Start date", placeholder = "yyyy-mm-dd")
                    PcmTextField(end, { end = it }, "End date", placeholder = "yyyy-mm-dd")
                    if (editYear == null) {
                        OutlinedButton(onClick = { withSyllabus = !withSyllabus }) {
                            Text(if (withSyllabus) "Copy CBSE syllabus: yes" else "Copy CBSE syllabus: no")
                        }
                    }
                    error?.let { Text(it, color = Danger, style = MaterialTheme.typography.bodySmall) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isBlank()) {
                        error = "A name is required."
                        return@TextButton
                    }
                    if (runCatching { LocalDate.parse(start) }.isFailure || runCatching { LocalDate.parse(end) }.isFailure) {
                        error = "Use valid dates (yyyy-mm-dd)."
                        return@TextButton
                    }
                    if (start >= end) {
                        error = "The end date must be after the start date."
                        return@TextButton
                    }
                    val existing = editYear
                    if (existing != null) {
                        vm.renameYear(existing.copy(name = name.trim(), startDate = start, endDate = end))
                    } else {
                        vm.createYear(name.trim(), start, end)
                    }
                    showYearDialog = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showYearDialog = false }) { Text("Cancel") } },
        )
    }

    deleteYear?.let { year ->
        ConfirmDialog(
            title = "Delete academic year?",
            message = "Deleting \"${year.name}\" removes its subjects, chapters, topics and every record linked to it. Export a backup first if you might want it back.",
            onDismiss = { deleteYear = null },
            onConfirm = {
                vm.deleteYear(year.id)
                deleteYear = null
            },
        )
    }

    if (confirmReset) {
        ConfirmDialog(
            title = "Reset all data?",
            message = "Everything on this device will be deleted and the CBSE seed syllabus will be installed again. This cannot be undone.",
            confirmLabel = "Reset",
            onDismiss = { confirmReset = false },
            onConfirm = {
                vm.resetDatabase()
                confirmReset = false
                message = "All data reset."
            },
        )
    }
}
