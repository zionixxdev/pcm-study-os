package dev.zionixx.pcmstudyos.ui.screens

import android.os.SystemClock
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zionixx.pcmstudyos.data.local.entity.Goal
import dev.zionixx.pcmstudyos.data.local.entity.RevisionItem
import dev.zionixx.pcmstudyos.domain.Grading
import dev.zionixx.pcmstudyos.domain.Progress
import dev.zionixx.pcmstudyos.domain.RevisionSchedule
import dev.zionixx.pcmstudyos.domain.StudyStats
import dev.zionixx.pcmstudyos.timer.FocusTimerService
import dev.zionixx.pcmstudyos.timer.TimerMode
import dev.zionixx.pcmstudyos.timer.TimerPhase
import dev.zionixx.pcmstudyos.ui.components.BarChart
import dev.zionixx.pcmstudyos.ui.components.DonutChart
import dev.zionixx.pcmstudyos.ui.components.EmptyState
import dev.zionixx.pcmstudyos.ui.components.LineChart
import dev.zionixx.pcmstudyos.ui.components.PcmCard
import dev.zionixx.pcmstudyos.ui.components.PcmTextField
import dev.zionixx.pcmstudyos.ui.components.Pill
import dev.zionixx.pcmstudyos.ui.components.RingProgress
import dev.zionixx.pcmstudyos.ui.components.ScreenTitle
import dev.zionixx.pcmstudyos.ui.components.SectionHeader
import dev.zionixx.pcmstudyos.ui.components.StatTile
import dev.zionixx.pcmstudyos.ui.components.StatusPill
import dev.zionixx.pcmstudyos.ui.components.ThinProgress
import dev.zionixx.pcmstudyos.ui.theme.Danger
import dev.zionixx.pcmstudyos.ui.theme.Success
import dev.zionixx.pcmstudyos.ui.theme.Warning
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun TimerScreen(vm: TimerViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    val todayMinutes by vm.todayMinutes.collectAsStateWithLifecycle()
    val subjects by vm.subjectsFlow.collectAsStateWithLifecycle()

    var now by remember { mutableStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(state.running) {
        while (state.running) {
            now = SystemClock.elapsedRealtime()
            delay(250)
        }
    }

    val context = LocalContext.current
    val elapsed = state.elapsedMs(now)
    val remaining = state.remainingMs(now)
    var notes by remember(state.sessionStartEpoch) { mutableStateOf(state.notes) }
    var showSaveDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { ScreenTitle("Focus timer", "Today: ${formatMinutes(todayMinutes)}") }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TimerMode.values().forEach { mode ->
                    OutlinedButton(
                        onClick = {
                            vm.configure(mode)
                            vm.controller.setNotes(notes)
                        },
                    ) {
                        Text(
                            when (mode) {
                                TimerMode.POMODORO -> "Pomodoro"
                                TimerMode.FOCUS -> "Custom"
                                TimerMode.STOPWATCH -> "Stopwatch"
                            },
                            color = if (state.mode == mode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item {
            PcmCard {
                Column(
                    Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Pill(
                        when (state.phase) {
                            TimerPhase.FOCUS -> if (state.mode == TimerMode.STOPWATCH) "Stopwatch" else "Focus · round ${state.round}"
                            TimerPhase.SHORT_BREAK -> "Short break"
                            TimerPhase.LONG_BREAK -> "Long break"
                        },
                        if (state.phase == TimerPhase.FOCUS) MaterialTheme.colorScheme.primary else Success,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = if (state.mode == TimerMode.STOPWATCH) clock(elapsed) else clock(remaining),
                        fontSize = 46.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                    )
                    Spacer(Modifier.height(6.dp))
                    if (state.mode != TimerMode.STOPWATCH) {
                        ThinProgress((state.progress(now) * 100).toInt())
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (state.running) {
                            OutlinedButton(onClick = {
                                vm.controller.pause()
                                FocusTimerService.stop(context)
                            }) { Text("Pause") }
                        } else {
                            Button(onClick = {
                                vm.controller.setNotes(notes)
                                vm.controller.start()
                                FocusTimerService.start(context)
                            }) { Icon(Icons.Filled.PlayArrow, contentDescription = null); Text("Start") }
                        }
                        OutlinedButton(
                            onClick = { showSaveDialog = true },
                            enabled = elapsed >= 30_000L,
                        ) { Text("Stop & save") }
                        IconButton(onClick = {
                            vm.controller.reset()
                            FocusTimerService.stop(context)
                        }) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Reset")
                        }
                    }
                    Text(
                        if (elapsed < 30_000L) "A session is saved once you have focused for 30 seconds." else "Only the time you actually focus is recorded.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            PcmCard {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeader("Session")
                    Text("Subject", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        subjects.forEach { subject ->
                            OutlinedButton(onClick = { vm.controller.select(state.mode, subject.id, null, null) }) {
                                Text(
                                    subject.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (state.subjectId == subject.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    PcmTextField(notes, { notes = it; vm.controller.setNotes(it) }, "Notes", singleLine = false, minLines = 2)
                    if (state.mode == TimerMode.FOCUS) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(15, 25, 45, 60).forEach { minutes ->
                                OutlinedButton(onClick = { vm.controller.setPlannedMinutes(minutes) }) {
                                    Text("${minutes}m", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("Today", formatMinutes(todayMinutes), Modifier.weight(1f))
                StatTile("Sessions", "${sessions.size}", Modifier.weight(1f))
                StatTile("Target", formatMinutes(settings.dailyTargetMinutes), Modifier.weight(1f))
            }
        }

        item { SectionHeader("Recent sessions") }
        if (sessions.isEmpty()) {
            item { PcmCard { EmptyState(icon = Icons.Filled.DateRange, title = "No sessions yet", hint = "Start a focus session to build your streak.") } }
        } else {
            items(sessions.take(8), key = { it.id }) { session ->
                PcmCard {
                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                StudyStats.dateOf(session.startedAt).format(DateTimeFormatter.ofPattern("d MMM")),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(session.mode, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(formatMinutes(session.minutes), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save session?") },
            text = {
                Text(
                    "You focused for ${formatMinutes((elapsed / 60_000L).toInt())}. Save it to your study log?",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.commit()
                    FocusTimerService.stop(context)
                    showSaveDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = {
                    vm.controller.reset()
                    FocusTimerService.stop(context)
                    showSaveDialog = false
                }) { Text("Discard") }
            },
        )
    }
}

private fun clock(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%02d:%02d".format(minutes, seconds)
}

@Composable
fun RevisionScreen(vm: RevisionViewModel = hiltViewModel()) {
    val revisions by vm.revisions.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val topics by vm.topicsFlow.collectAsStateWithLifecycle()

    var filter by remember { mutableStateOf("all") }
    var ratingTarget by remember { mutableStateOf<RevisionItem?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<RevisionItem?>(null) }

    val today = LocalDate.now()
    val withStatus = revisions.map { it to RevisionSchedule.statusOf(it, today) }
    val visible = withStatus
        .filter { (_, status) -> filter == "all" || status == filter }
        .sortedBy { it.first.dueDate }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            ScreenTitle(
                title = "Revision",
                subtitle = "Spaced repetition · ${settings.revisionIntervals.joinToString(" / ")} days",
                action = {
                    IconButton(onClick = { showAdd = true }) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Add revision", tint = MaterialTheme.colorScheme.primary)
                    }
                },
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("all", "due", "overdue", "upcoming", "completed").forEach { value ->
                    OutlinedButton(onClick = { filter = value }) {
                        Text(
                            value,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (filter == value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("Due", "${withStatus.count { it.second == "due" }}", Modifier.weight(1f), accent = Warning)
                StatTile("Overdue", "${withStatus.count { it.second == "overdue" }}", Modifier.weight(1f), accent = Danger)
                StatTile("Done", "${withStatus.count { it.second == "completed" }}", Modifier.weight(1f), accent = Success)
            }
        }

        if (visible.isEmpty()) {
            item {
                PcmCard {
                    EmptyState(
                        icon = Icons.Filled.Refresh,
                        title = "No revision items",
                        hint = "Add a topic to revise; the next date is scheduled automatically after each revision.",
                        actionLabel = "Add revision",
                        onAction = { showAdd = true },
                    )
                }
            }
        } else {
            items(visible, key = { it.first.id }) { (item, status) ->
                PcmCard(onClick = { if (status != "completed") ratingTarget = item }) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(item.label, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(
                                "${item.dueDate} · interval ${RevisionSchedule.intervalFor(settings.revisionIntervals, item.intervalIndex)}d" +
                                    (item.lastRating?.let { " · last: $it" } ?: ""),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        StatusPill(status)
                        IconButton(onClick = { deleteTarget = item }, modifier = Modifier.width(32.dp)) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    ratingTarget?.let { item ->
        RatingDialog(
            item = item,
            onDismiss = { ratingTarget = null },
            onRate = { rating, notes ->
                vm.complete(item, rating, notes)
                ratingTarget = null
            },
        )
    }

    if (showAdd) {
        AddRevisionDialog(
            topics = topics,
            onDismiss = { showAdd = false },
            onAdd = { topicId, label, date ->
                vm.add(topicId, label, date, null)
                showAdd = false
            },
        )
    }

    deleteTarget?.let { item ->
        dev.zionixx.pcmstudyos.ui.components.ConfirmDialog(
            title = "Delete revision?",
            message = "\"${item.label}\" will be removed from the schedule.",
            onDismiss = { deleteTarget = null },
            onConfirm = {
                vm.delete(item.id)
                deleteTarget = null
            },
        )
    }
}

@Composable
private fun RatingDialog(item: RevisionItem, onDismiss: () -> Unit, onRate: (String, String?) -> Unit) {
    var notes by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("How well did you remember?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(item.label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("again", "hard", "good", "easy").forEach { rating ->
                        OutlinedButton(onClick = { onRate(rating, notes.ifBlank { null }) }) {
                            Text(rating, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                PcmTextField(notes, { notes = it }, "Notes", singleLine = false, minLines = 2)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun AddRevisionDialog(
    topics: List<dev.zionixx.pcmstudyos.data.local.entity.Topic>,
    onDismiss: () -> Unit,
    onAdd: (String?, String, LocalDate) -> Unit,
) {
    var label by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var topicId by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add revision") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PcmTextField(label, { label = it }, "Topic or label *")
                PcmTextField(date, { date = it }, "Revision date", placeholder = "yyyy-mm-dd")
                Text("Link a topic (optional)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    topics.take(8).forEach { topic ->
                        OutlinedButton(onClick = { topicId = topic.id; if (label.isBlank()) label = topic.name }) {
                            Text(
                                topic.name,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                color = if (topicId == topic.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                error?.let { Text(it, color = Danger, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (label.isBlank()) {
                    error = "A label is required."
                    return@TextButton
                }
                val parsed = runCatching { LocalDate.parse(date) }.getOrNull()
                if (parsed == null) {
                    error = "Use a valid date (yyyy-mm-dd)."
                    return@TextButton
                }
                onAdd(topicId, label.trim(), parsed)
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun GoalsScreen(vm: ToolsViewModel = hiltViewModel()) {
    val goals by vm.goals.collectAsStateWithLifecycle()
    var editor by remember { mutableStateOf<Goal?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Goal?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            ScreenTitle(
                title = "Goals",
                subtitle = "${goals.count { it.status == "active" }} active",
                action = {
                    IconButton(onClick = { editor = null; showEditor = true }) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "New goal", tint = MaterialTheme.colorScheme.primary)
                    }
                },
            )
        }
        if (goals.isEmpty()) {
            item {
                PcmCard {
                    EmptyState(
                        icon = Icons.Filled.Star,
                        title = "No goals yet",
                        hint = "Set a weekly study-hour target or a chapter deadline.",
                        actionLabel = "Create goal",
                        onAction = { editor = null; showEditor = true },
                    )
                }
            }
        } else {
            items(goals, key = { it.id }) { goal ->
                val percent = if (goal.target > 0) ((goal.current / goal.target) * 100).toInt().coerceIn(0, 100) else 0
                PcmCard(onClick = { editor = goal; showEditor = true }) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(goal.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Pill(goal.kind, MaterialTheme.colorScheme.primary)
                        }
                        Text(
                            "${goal.current.toInt()} / ${goal.target.toInt()}" + (goal.deadline?.let { " · by $it" } ?: ""),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(8.dp))
                        ThinProgress(percent, color = if (percent >= 100) Success else null)
                        Row {
                            TextButton(onClick = { vm.saveGoal(goal.copy(status = "completed", current = goal.target)) }) { Text("Complete") }
                            TextButton(onClick = { deleteTarget = goal }) { Text("Delete", color = Danger) }
                        }
                    }
                }
            }
        }
    }

    if (showEditor) {
        var title by remember { mutableStateOf(editor?.title ?: "") }
        var target by remember { mutableStateOf(editor?.target?.toInt()?.toString() ?: "10") }
        var current by remember { mutableStateOf(editor?.current?.toInt()?.toString() ?: "0") }
        var deadline by remember { mutableStateOf(editor?.deadline ?: "") }
        var kind by remember { mutableStateOf(editor?.kind ?: "weekly") }
        AlertDialog(
            onDismissRequest = { showEditor = false },
            title = { Text(if (editor == null) "New goal" else "Edit goal") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PcmTextField(title, { title = it }, "Title *")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f)) { PcmTextField(target, { target = it.filter { c -> c.isDigit() } }, "Target", keyboardType = KeyboardType.Number) }
                        Column(Modifier.weight(1f)) { PcmTextField(current, { current = it.filter { c -> c.isDigit() } }, "Current", keyboardType = KeyboardType.Number) }
                    }
                    PcmTextField(deadline, { deadline = it }, "Deadline", placeholder = "yyyy-mm-dd")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("daily", "weekly", "monthly", "hours", "revision").forEach { value ->
                            OutlinedButton(onClick = { kind = value }) {
                                Text(value, style = MaterialTheme.typography.labelSmall, color = if (kind == value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val targetValue = target.toDoubleOrNull() ?: 0.0
                    if (title.isBlank() || targetValue <= 0) return@TextButton
                    val yearId = vm.yearId.value ?: return@TextButton
                    vm.saveGoal(
                        (editor ?: Goal(yearId = yearId, title = "", target = 1.0)).copy(
                            title = title.trim(),
                            target = targetValue,
                            current = current.toDoubleOrNull() ?: 0.0,
                            deadline = deadline.ifBlank { null },
                            kind = kind,
                        ),
                    )
                    showEditor = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showEditor = false }) { Text("Cancel") } },
        )
    }

    deleteTarget?.let { goal ->
        dev.zionixx.pcmstudyos.ui.components.ConfirmDialog(
            title = "Delete goal?",
            message = "\"${goal.title}\" will be removed.",
            onDismiss = { deleteTarget = null },
            onConfirm = {
                vm.deleteGoal(goal.id)
                deleteTarget = null
            },
        )
    }
}

@Composable
fun AnalyticsScreen(vm: AnalyticsViewModel = hiltViewModel()) {
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    val subjects by vm.subjects.collectAsStateWithLifecycle()
    val progress by vm.progress.collectAsStateWithLifecycle()
    val exams by vm.exams.collectAsStateWithLifecycle()
    val practice by vm.practice.collectAsStateWithLifecycle()
    val revisions by vm.revisions.collectAsStateWithLifecycle()
    val mistakes by vm.mistakes.collectAsStateWithLifecycle()
    val rangeDays by vm.rangeDays.collectAsStateWithLifecycle()
    val topicList by vm.topics.collectAsStateWithLifecycle()

    val today = LocalDate.now()
    val from = today.minusDays((rangeDays - 1).toLong())
    val filtered = sessions.filter { StudyStats.dateOf(it.startedAt) >= from }
    val summary = StudyStats.summarize(filtered, emptyList(), today)

    val daily = (rangeDays - 1 downTo 0).map { offset ->
        val day = today.minusDays(offset.toLong())
        filtered.filter { StudyStats.dateOf(it.startedAt) == day }.sumOf { it.minutes }.toFloat()
    }

    val bySubject = subjects.map { subject ->
        subject to filtered.filter { it.subjectId == subject.id }.sumOf { it.minutes }
    }.filter { it.second > 0 }

    val graded = exams.filter { it.obtainedMarks != null && it.maxMarks > 0 }.sortedBy { it.date }
    val scoreTrend = graded.map { Grading.percent(it.obtainedMarks, it.maxMarks).toFloat() }

    val attempted = practice.count { it.result != "skipped" }
    val correct = practice.count { it.result == "correct" }
    val partial = practice.count { it.result == "partial" }
    val accuracy = Grading.accuracy(correct, partial, attempted)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { ScreenTitle("Analytics", "Computed from your stored records only") }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(7, 30, 90).forEach { days ->
                    OutlinedButton(onClick = { vm.setRange(days) }) {
                        Text(
                            "${days}d",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (rangeDays == days) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("Syllabus", "${progress?.overall ?: 0}%", Modifier.weight(1f))
                StatTile("Study time", formatMinutes(summary.totalMinutes), Modifier.weight(1f))
                StatTile("Daily avg", formatMinutes(summary.averageDailyMinutes), Modifier.weight(1f))
            }
        }

        item {
            PcmCard {
                Column(Modifier.padding(12.dp)) {
                    SectionHeader("Daily study minutes")
                    BarChart(daily, height = 100.dp)
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${rangeDays}d ago", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("today", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        item {
            PcmCard {
                Column(Modifier.padding(12.dp)) {
                    SectionHeader("Study time by subject")
                    if (bySubject.isEmpty()) {
                        Text("No sessions in this range.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            DonutChart(bySubject.map { parseColor(it.first.color) to it.second.toFloat() }, size = 110.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                bySubject.take(5).forEach { (subject, minutes) ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        androidx.compose.foundation.layout.Box(
                                            Modifier.width(8.dp).height(8.dp).background(parseColor(subject.color), androidx.compose.foundation.shape.RoundedCornerShape(50)),
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            "${subject.name} · ${formatMinutes(minutes)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            PcmCard {
                Column(Modifier.padding(12.dp)) {
                    SectionHeader("Test performance")
                    if (scoreTrend.size < 2) {
                        Text("Log at least two graded tests to see a trend.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        LineChart(scoreTrend, maxOverride = 100f)
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("Accuracy", "$accuracy%", Modifier.weight(1f), hint = "$attempted questions")
                StatTile("Revisions", "${revisions.count { it.status == "completed" }}/${revisions.size}", Modifier.weight(1f))
                StatTile("Mistakes", "${mistakes.count { !it.resolved }}", Modifier.weight(1f), hint = "unresolved")
            }
        }

        item {
            PcmCard {
                Column(Modifier.padding(12.dp)) {
                    SectionHeader("Weakest topics")
                    val weak = progress?.topicPercents?.entries
                        ?.filter { it.value < 100 }
                        ?.sortedBy { it.value }
                        ?.take(5)
                        ?: emptyList()
                    if (weak.isEmpty()) {
                        Text("No partially-complete topics yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        weak.forEach { entry ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    topicList.firstOrNull { it.id == entry.key }?.name ?: "Topic",
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                Text("${entry.value}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}
