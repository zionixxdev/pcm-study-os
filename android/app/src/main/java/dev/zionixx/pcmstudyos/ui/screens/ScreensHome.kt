package dev.zionixx.pcmstudyos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zionixx.pcmstudyos.data.local.entity.StudyTask
import dev.zionixx.pcmstudyos.domain.RevisionSchedule
import dev.zionixx.pcmstudyos.domain.StudyStats
import dev.zionixx.pcmstudyos.ui.components.BarChart
import dev.zionixx.pcmstudyos.ui.components.EmptyState
import dev.zionixx.pcmstudyos.ui.components.PcmCard
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun DashboardScreen(
    onNavigate: (String) -> Unit,
    vm: DashboardViewModel = hiltViewModel(),
) {
    val progress by vm.progress.collectAsStateWithLifecycle()
    val subjects by vm.subjects.collectAsStateWithLifecycle()
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val revisions by vm.revisions.collectAsStateWithLifecycle()
    val exams by vm.exams.collectAsStateWithLifecycle()
    val stats by vm.stats.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()

    val today = LocalDate.now()
    val todaysTasks = tasks.filter { it.dueDate == today.toString() || (it.dueDate != null && it.dueDate < today.toString() && it.status != "completed") }
    val dueRevisions = revisions.count { RevisionSchedule.statusOf(it, today) == "due" || RevisionSchedule.statusOf(it, today) == "overdue" }
    val upcomingExams = exams.filter { it.date >= today.toString() }.sortedBy { it.date }.take(3)
    val chapterDeadlines = subjects.size

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenTitle(
                title = "Dashboard",
                subtitle = today.format(DateTimeFormatter.ofPattern("EEEE, d MMM yyyy")),
            )
        }

        item {
            PcmCard {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RingProgress(percent = progress?.overall ?: 0, size = 74.dp, stroke = 8.dp)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Overall progress", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${progress?.overall ?: 0}% of the syllabus",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "${progress?.completedTopics ?: 0} of ${progress?.totalTopics ?: 0} topics done",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatTile("Chapters", "${progress?.completedChapters ?: 0}/${progress?.totalChapters ?: 0}", Modifier.weight(1f))
                        StatTile("Remaining", "${(progress?.totalTopics ?: 0) - (progress?.completedTopics ?: 0)}", Modifier.weight(1f))
                        StatTile("Subjects", "${subjects.size}", Modifier.weight(1f))
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("Today", formatMinutes(stats?.todayMinutes ?: 0), Modifier.weight(1f), hint = "target ${formatMinutes(settings.dailyTargetMinutes)}")
                StatTile("Streak", "${stats?.currentStreak ?: 0} d", Modifier.weight(1f), hint = "best ${stats?.longestStreak ?: 0} d")
            }
        }

        item {
            PcmCard {
                Column(Modifier.padding(14.dp)) {
                    SectionHeader("Last 7 days")
                    BarChart(
                        values = (stats?.last7Days ?: emptyList()).map { it.second.toFloat() },
                        height = 78.dp,
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        (stats?.last7Days ?: emptyList()).forEach { (day, _) ->
                            Text(
                                day.dayOfMonth.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        item {
            SectionHeader("Subjects", action = { TextButton(onClick = { onNavigate("syllabus") }) { Text("Manage") } })
        }

        if (subjects.isEmpty()) {
            item {
                PcmCard {
                    EmptyState(
                        icon = Icons.Filled.List,
                        title = "No subjects yet",
                        hint = "Add a subject to start tracking your syllabus.",
                        actionLabel = "Open syllabus",
                        onAction = { onNavigate("syllabus") },
                    )
                }
            }
        } else {
            items(subjects, key = { it.id }) { subject ->
                val percent = progress?.subjectPercents?.get(subject.id) ?: 0
                PcmCard(onClick = { onNavigate("syllabus") }) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(9.dp)
                                    .background(parseColor(subject.color), RoundedCornerShape(50)),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(subject.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            Text("$percent%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(8.dp))
                        ThinProgress(percent, color = parseColor(subject.color))
                    }
                }
            }
        }

        item {
            SectionHeader("Today's tasks", action = { TextButton(onClick = { onNavigate("planner") }) { Text("Planner") } })
        }

        if (todaysTasks.isEmpty()) {
            item {
                PcmCard { EmptyState(icon = Icons.Filled.DateRange, title = "Nothing due today", hint = "Add a task in the planner to plan your day.") }
            }
        } else {
            items(todaysTasks.take(6), key = { it.id }) { task ->
                TaskRow(task = task, onToggle = { vm.completeTask(task.id) })
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("Revision due", "$dueRevisions", Modifier.weight(1f), accent = if (dueRevisions > 0) Warning else null)
                StatTile("Open tasks", "${tasks.count { it.status != "completed" }}", Modifier.weight(1f))
                StatTile("Chapter due", "$chapterDeadlines", Modifier.weight(1f), hint = "subjects")
            }
        }

        if (upcomingExams.isNotEmpty()) {
            item { SectionHeader("Upcoming tests", action = { TextButton(onClick = { onNavigate("tests") }) { Text("All") } }) }
            items(upcomingExams, key = { it.id }) { exam ->
                PcmCard(onClick = { onNavigate("tests") }) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(exam.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(exam.date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Pill(exam.type.replace('_', ' '), Warning)
                    }
                }
            }
        }

        item {
            SectionHeader("Quick actions")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickAction("Add task", Icons.Filled.Add, Modifier.weight(1f)) { onNavigate("planner?new=1") }
                QuickAction("Timer", Icons.Filled.PlayArrow, Modifier.weight(1f)) { onNavigate("timer") }
                QuickAction("Revise", Icons.Filled.Refresh, Modifier.weight(1f)) { onNavigate("revision") }
                QuickAction("Search", Icons.Filled.Search, Modifier.weight(1f)) { onNavigate("search") }
            }
        }
    }
}

@Composable
private fun QuickAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    PcmCard(modifier = modifier, onClick = onClick) {
        Column(
            Modifier.padding(vertical = 12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}

@Composable
fun TaskRow(task: StudyTask, onToggle: () -> Unit, onClick: (() -> Unit)? = null) {
    PcmCard(onClick = onClick ?: onToggle) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(
                        if (task.status == "completed") Success else MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(6.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (task.status == "completed") {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = if (task.status == "completed") MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${task.estimatedMinutes}m · ${task.priority}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (task.dueDate != null) {
                        val overdue = task.status != "completed" && task.dueDate < LocalDate.now().toString()
                        Pill(if (overdue) "overdue" else task.dueDate.substring(5), if (overdue) Danger else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            StatusPill(task.status)
        }
    }
}

@Composable
fun MoreScreen(onNavigate: (String) -> Unit) {
    val items = listOf(
        Triple("Formula library", Icons.Filled.List, "formulas"),
        Triple("Practice tracker", Icons.Filled.Star, "practice"),
        Triple("Mistake notebook", Icons.Filled.Warning, "mistakes"),
        Triple("Tests", Icons.Filled.Check, "tests"),
        Triple("Notes", Icons.Filled.Create, "notes"),
        Triple("Analytics", Icons.Filled.DateRange, "analytics"),
        Triple("Goals", Icons.Filled.Star, "goals"),
        Triple("Focus timer", Icons.Filled.PlayArrow, "timer"),
        Triple("Search", Icons.Filled.Search, "search"),
        Triple("Settings", Icons.Filled.Settings, "settings"),
    )
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { ScreenTitle("More", "Everything else in PCM Study OS") }
        items(items, key = { it.third }) { (label, icon, route) ->
            PcmCard(onClick = { onNavigate(route) }) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun SearchScreen(
    onNavigate: (String) -> Unit,
    vm: SearchViewModel = hiltViewModel(),
) {
    val query by vm.query.collectAsStateWithLifecycle()
    val hits by vm.hits.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { ScreenTitle("Search", "Subjects, chapters, topics, notes, formulas, tasks, mistakes and tests") }
        item {
            dev.zionixx.pcmstudyos.ui.components.PcmTextField(
                value = query,
                onValueChange = vm::setQuery,
                label = "Search this academic year",
                placeholder = "e.g. thermodynamics",
            )
        }
        if (query.trim().length < 2) {
            item { EmptyState(icon = Icons.Filled.Search, title = "Start typing", hint = "Results appear as you type.") }
        } else if (hits.isEmpty()) {
            item { EmptyState(icon = Icons.Filled.Search, title = "No matches", hint = "Try a shorter word.") }
        } else {
            items(hits, key = { "${it.kind}-${it.title}-${it.subtitle}" }) { hit ->
                PcmCard(onClick = { onNavigate(hit.route) }) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(hit.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            hit.subtitle?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }
                        }
                        Pill(hit.kind, MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

fun formatMinutes(minutes: Int): String {
    if (minutes <= 0) return "0m"
    val hours = minutes / 60
    val rest = minutes % 60
    return when {
        hours == 0 -> "${rest}m"
        rest == 0 -> "${hours}h"
        else -> "${hours}h ${rest}m"
    }
}

fun parseColor(hex: String): Color = runCatching {
    val cleaned = hex.removePrefix("#")
    Color(0xFF000000 or cleaned.toLong(16))
}.getOrDefault(Color(0xFF4F8CFF))
