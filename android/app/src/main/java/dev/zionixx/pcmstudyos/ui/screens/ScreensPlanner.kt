package dev.zionixx.pcmstudyos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zionixx.pcmstudyos.data.local.entity.StudyTask
import dev.zionixx.pcmstudyos.data.local.entity.newId
import dev.zionixx.pcmstudyos.ui.components.ConfirmDialog
import dev.zionixx.pcmstudyos.ui.components.EmptyState
import dev.zionixx.pcmstudyos.ui.components.PcmCard
import dev.zionixx.pcmstudyos.ui.components.PcmTextField
import dev.zionixx.pcmstudyos.ui.components.Pill
import dev.zionixx.pcmstudyos.ui.components.ScreenTitle
import dev.zionixx.pcmstudyos.ui.components.SectionHeader
import dev.zionixx.pcmstudyos.ui.components.ThinProgress
import dev.zionixx.pcmstudyos.ui.theme.Danger
import dev.zionixx.pcmstudyos.ui.theme.Success
import dev.zionixx.pcmstudyos.ui.theme.Warning
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun PlannerScreen(
    vm: PlannerViewModel = hiltViewModel(),
    openNewOnStart: Boolean = false,
) {
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val plan by vm.plan.collectAsStateWithLifecycle()
    val yearId by vm.yearId.collectAsStateWithLifecycle()

    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var editor by remember { mutableStateOf<StudyTask?>(null) }
    var showEditor by remember(openNewOnStart) { mutableStateOf(openNewOnStart) }
    var showCatchup by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<StudyTask?>(null) }

    val week = remember(selectedDate) {
        val monday = selectedDate.minusDays((selectedDate.dayOfWeek.value - 1).toLong())
        (0..6).map { monday.plusDays(it.toLong()) }
    }
    val dayTasks = tasks.filter { it.dueDate == selectedDate.toString() }
    val overdue = tasks.filter { it.status != "completed" && it.dueDate != null && it.dueDate < LocalDate.now().toString() }
    val openCount = tasks.count { it.status != "completed" }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            ScreenTitle(
                title = "Planner",
                subtitle = "$openCount open tasks",
                action = {
                    Row {
                        IconButton(onClick = { showCatchup = true }) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Catch-up planner", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { editor = null; showEditor = true }) {
                            Icon(Icons.Filled.Add, contentDescription = "New task", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(week) { day ->
                    val isSelected = day == selectedDate
                    val isToday = day == LocalDate.now()
                    val count = tasks.count { it.dueDate == day.toString() && it.status != "completed" }
                    Column(
                        modifier = Modifier
                            .width(46.dp)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                RoundedCornerShape(12.dp),
                            )
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            day.format(DateTimeFormatter.ofPattern("EEE")).take(2),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            day.dayOfMonth.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        )
                        if (count > 0) {
                            Spacer(Modifier.height(2.dp))
                            Pill(
                                "$count",
                                if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                            )
                        } else if (isToday) {
                            Spacer(Modifier.height(2.dp))
                            Text("•", color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        if (overdue.isNotEmpty()) {
            item { SectionHeader("Overdue (${overdue.size})") }
            items(overdue.take(4), key = { "od-${it.id}" }) { task ->
                TaskRow(
                    task = task,
                    onToggle = { vm.toggleTask(task) },
                    onClick = { editor = task; showEditor = true },
                )
            }
        }

        item {
            SectionHeader(
                selectedDate.format(DateTimeFormatter.ofPattern("EEEE, d MMM")),
                action = { TextButton(onClick = { showCatchup = true }) { Text("Catch up") } },
            )
        }

        if (dayTasks.isEmpty()) {
            item {
                PcmCard {
                    EmptyState(
                        icon = Icons.Filled.DateRange,
                        title = "No tasks for this day",
                        hint = "Add a task or pick another date.",
                        actionLabel = "Add task",
                        onAction = { editor = null; showEditor = true },
                    )
                }
            }
        } else {
            items(dayTasks, key = { it.id }) { task ->
                PcmCard(onClick = { editor = task; showEditor = true }) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { vm.toggleTask(task) }, modifier = Modifier.size(28.dp)) {
                            Icon(
                                if (task.status == "completed") Icons.Filled.Check else Icons.Filled.DateRange,
                                contentDescription = "Toggle complete",
                                tint = if (task.status == "completed") Success else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
                            Text(
                                task.title,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                "${task.estimatedMinutes} min · ${task.priority}${if (task.recurring != "none") " · ${task.recurring}" else ""}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { deleteTarget = task }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete task", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }

    if (showEditor) {
        TaskEditorDialog(
            task = editor,
            yearId = yearId,
            defaultPriority = settings.defaultPriority,
            defaultDate = selectedDate,
            onDismiss = { showEditor = false; editor = null },
            onSave = { task ->
                vm.saveTask(task)
                showEditor = false
                editor = null
            },
        )
    }

    if (showCatchup) {
        CatchupSheet(
            capacityDefault = settings.dailyTargetMinutes,
            plan = plan,
            onGenerate = { vm.buildPlan(it) },
            onApply = { selected -> vm.applyPlan(selected); showCatchup = false },
            onDismiss = { vm.clearPlan(); showCatchup = false },
        )
    }

    deleteTarget?.let { task ->
        ConfirmDialog(
            title = "Delete task?",
            message = "\"${task.title}\" will be removed.",
            onDismiss = { deleteTarget = null },
            onConfirm = {
                vm.deleteTask(task.id)
                deleteTarget = null
            },
        )
    }
}

@Composable
private fun TaskEditorDialog(
    task: StudyTask?,
    yearId: String?,
    defaultPriority: String,
    defaultDate: LocalDate,
    onDismiss: () -> Unit,
    onSave: (StudyTask) -> Unit,
) {
    var title by remember { mutableStateOf(task?.title ?: "") }
    var description by remember { mutableStateOf(task?.description ?: "") }
    var priority by remember { mutableStateOf(task?.priority ?: defaultPriority) }
    var dueDate by remember { mutableStateOf(task?.dueDate ?: defaultDate.toString()) }
    var minutes by remember { mutableStateOf((task?.estimatedMinutes ?: 30).toString()) }
    var recurring by remember { mutableStateOf(task?.recurring ?: "none") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (task == null) "New task" else "Edit task") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PcmTextField(title, { title = it }, "Title *")
                PcmTextField(description, { description = it }, "Description", singleLine = false, minLines = 2)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        PcmTextField(dueDate, { dueDate = it }, "Due date", placeholder = "yyyy-mm-dd")
                    }
                    Column(Modifier.weight(1f)) {
                        PcmTextField(
                            minutes,
                            { minutes = it.filter { ch -> ch.isDigit() } },
                            "Minutes",
                            keyboardType = KeyboardType.Number,
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("high", "medium", "low").forEach { value ->
                        OutlinedButton(onClick = { priority = value }) {
                            Text(
                                value,
                                color = if (priority == value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("none", "daily", "weekly").forEach { value ->
                        OutlinedButton(onClick = { recurring = value }) {
                            Text(
                                if (value == "none") "once" else value,
                                color = if (recurring == value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                error?.let { Text(it, color = Danger, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isBlank()) {
                    error = "A title is required."
                    return@TextButton
                }
                if (yearId == null) {
                    error = "No academic year selected."
                    return@TextButton
                }
                if (runCatching { LocalDate.parse(dueDate) }.isFailure) {
                    error = "Use a valid date (yyyy-mm-dd)."
                    return@TextButton
                }
                onSave(
                    (task ?: StudyTask(yearId = yearId, title = "", id = newId())).copy(
                        title = title.trim(),
                        description = description.ifBlank { null },
                        priority = priority,
                        dueDate = dueDate,
                        estimatedMinutes = minutes.toIntOrNull()?.coerceIn(5, 600) ?: 30,
                        recurring = recurring,
                        status = task?.status ?: "pending",
                    ),
                )
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun CatchupSheet(
    capacityDefault: Int,
    plan: dev.zionixx.pcmstudyos.domain.CatchupPlanner.Plan?,
    onGenerate: (Int) -> Unit,
    onApply: (List<dev.zionixx.pcmstudyos.domain.CatchupPlanner.Suggestion>) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var capacity by remember { mutableStateOf(capacityDefault.toString()) }
    var accepted by remember(plan) { mutableStateOf(plan?.suggestions?.map { it.taskId }?.toSet() ?: emptySet()) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Smart catch-up", style = MaterialTheme.typography.titleLarge)
            Text(
                "Deterministic scheduling: unfinished tasks are spread across your study days without exceeding your daily capacity. Nothing changes until you apply it.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PcmTextField(
                capacity,
                { capacity = it.filter { ch -> ch.isDigit() } },
                "Daily capacity (minutes)",
                keyboardType = KeyboardType.Number,
            )
            OutlinedButton(onClick = { onGenerate(capacity.toIntOrNull() ?: capacityDefault) }, modifier = Modifier.fillMaxWidth()) {
                Text(if (plan == null) "Generate plan" else "Recalculate")
            }

            if (plan != null) {
                if (plan.suggestions.isEmpty()) {
                    Text("Nothing to reschedule — everything fits.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    val load = plan.loadByDate()
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        load.entries.take(3).forEach { (date, minutes) ->
                            Pill("${date.dayOfMonth}/${date.monthValue}: ${minutes}m", MaterialTheme.colorScheme.primary)
                        }
                    }
                    LazyColumn(Modifier.height(260.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(plan.suggestions, key = { it.taskId }) { suggestion ->
                            val checked = accepted.contains(suggestion.taskId)
                            PcmCard(onClick = {
                                accepted = if (checked) accepted - suggestion.taskId else accepted + suggestion.taskId
                            }) {
                                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (checked) Icons.Filled.Check else Icons.Filled.DateRange,
                                        contentDescription = null,
                                        tint = if (checked) Success else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(suggestion.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(
                                            "${suggestion.date} · ${suggestion.minutes}m · ${suggestion.reason}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                        )
                                    }
                                    if (suggestion.atRisk) Pill("at risk", Danger)
                                }
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = { onApply(plan.suggestions.filter { accepted.contains(it.taskId) }) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Apply ${accepted.size} changes") }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
