package dev.zionixx.pcmstudyos.ui.screens

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zionixx.pcmstudyos.data.local.entity.Exam
import dev.zionixx.pcmstudyos.data.local.entity.FormulaEntry
import dev.zionixx.pcmstudyos.data.local.entity.Mistake
import dev.zionixx.pcmstudyos.data.local.entity.Note
import dev.zionixx.pcmstudyos.data.local.entity.PracticeRecord
import dev.zionixx.pcmstudyos.domain.Grading
import dev.zionixx.pcmstudyos.ui.components.ConfirmDialog
import dev.zionixx.pcmstudyos.ui.components.EmptyState
import dev.zionixx.pcmstudyos.ui.components.LineChart
import dev.zionixx.pcmstudyos.ui.components.MarkdownText
import dev.zionixx.pcmstudyos.ui.components.MathText
import dev.zionixx.pcmstudyos.ui.components.PcmCard
import dev.zionixx.pcmstudyos.ui.components.PcmTextField
import dev.zionixx.pcmstudyos.ui.components.Pill
import dev.zionixx.pcmstudyos.ui.components.ScreenTitle
import dev.zionixx.pcmstudyos.ui.components.SectionHeader
import dev.zionixx.pcmstudyos.ui.components.StatTile
import dev.zionixx.pcmstudyos.ui.theme.Danger
import dev.zionixx.pcmstudyos.ui.theme.Success
import dev.zionixx.pcmstudyos.ui.theme.Warning
import java.time.LocalDate

private val LIST_PADDING = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp)

@Composable
fun FormulasScreen(vm: ToolsViewModel = hiltViewModel()) {
    val formulas by vm.formulas.collectAsStateWithLifecycle()
    val subjects by vm.subjects.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var editor by remember { mutableStateOf<FormulaEntry?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<FormulaEntry?>(null) }

    val visible = formulas
        .filter { query.isBlank() || (it.title + " " + it.expression + " " + (it.symbols ?: "")).contains(query, ignoreCase = true) }
        .sortedBy { it.title }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = LIST_PADDING, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            ScreenTitle(
                title = "Formula library",
                subtitle = "${formulas.size} entries",
                action = {
                    IconButton(onClick = { editor = null; showEditor = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add entry", tint = MaterialTheme.colorScheme.primary)
                    }
                },
            )
        }
        item {
            PcmTextField(query, { query = it }, "Search formulas, laws, derivations", placeholder = "e.g. velocity")
        }
        if (visible.isEmpty()) {
            item {
                PcmCard {
                    EmptyState(
                        icon = Icons.Filled.Search,
                        title = "No entries",
                        hint = "Store a formula once with its symbols and units, then search it during revision.",
                        actionLabel = "Add formula",
                        onAction = { editor = null; showEditor = true },
                    )
                }
            }
        } else {
            items(visible, key = { it.id }) { entry ->
                PcmCard(onClick = { editor = entry; showEditor = true }) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(entry.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            IconButton(onClick = { vm.favouriteFormula(entry) }, modifier = Modifier.width(32.dp)) {
                                Icon(
                                    Icons.Filled.Star,
                                    contentDescription = "Favourite",
                                    tint = if (entry.favourite) Warning else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        MathText(entry.expression)
                        entry.symbols?.let {
                            Text("Where: $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                        }
                        entry.units?.let {
                            Text("Units: $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Pill(entry.category, MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = { deleteTarget = entry }) { Text("Delete", color = Danger) }
                        }
                    }
                }
            }
        }
    }

    if (showEditor) {
        var title by remember { mutableStateOf(editor?.title ?: "") }
        var expression by remember { mutableStateOf(editor?.expression ?: "") }
        var symbols by remember { mutableStateOf(editor?.symbols ?: "") }
        var units by remember { mutableStateOf(editor?.units ?: "") }
        var explanation by remember { mutableStateOf(editor?.explanation ?: "") }
        var category by remember { mutableStateOf(editor?.category ?: "formula") }
        var subjectId by remember { mutableStateOf(editor?.subjectId ?: subjects.firstOrNull()?.id ?: "") }
        var error by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showEditor = false },
            title = { Text(if (editor == null) "New entry" else "Edit entry") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PcmTextField(title, { title = it }, "Title *")
                    PcmTextField(expression, { expression = it }, "Expression *", placeholder = "v = u + at  or  x^2 + y^2 = r^2")
                    PcmTextField(symbols, { symbols = it }, "Symbols")
                    PcmTextField(units, { units = it }, "SI units")
                    PcmTextField(explanation, { explanation = it }, "Explanation", singleLine = false, minLines = 2)
                    Text("Type", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("formula", "derivation", "law", "definition", "unit", "equation", "identity", "theorem").forEach { value ->
                            OutlinedButton(onClick = { category = value }) {
                                Text(value, style = MaterialTheme.typography.labelSmall, color = if (category == value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Text("Subject", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        subjects.forEach { subject ->
                            OutlinedButton(onClick = { subjectId = subject.id }) {
                                Text(subject.name, style = MaterialTheme.typography.labelSmall, color = if (subjectId == subject.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    error?.let { Text(it, color = Danger, style = MaterialTheme.typography.bodySmall) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val yearId = vm.yearId.value
                    if (title.isBlank() || expression.isBlank()) {
                        error = "Title and expression are required."
                        return@TextButton
                    }
                    if (yearId == null || subjectId.isBlank()) {
                        error = "Pick a subject first."
                        return@TextButton
                    }
                    vm.saveFormula(
                        (editor ?: FormulaEntry(yearId = yearId, subjectId = subjectId, title = "", expression = "")).copy(
                            title = title.trim(),
                            expression = expression.trim(),
                            symbols = symbols.ifBlank { null },
                            units = units.ifBlank { null },
                            explanation = explanation.ifBlank { null },
                            category = category,
                            subjectId = subjectId,
                        ),
                    )
                    showEditor = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showEditor = false }) { Text("Cancel") } },
        )
    }

    deleteTarget?.let { entry ->
        ConfirmDialog(
            title = "Delete entry?",
            message = "\"${entry.title}\" will be removed from the library.",
            onDismiss = { deleteTarget = null },
            onConfirm = {
                vm.deleteFormula(entry.id)
                deleteTarget = null
            },
        )
    }
}

@Composable
fun PracticeScreen(vm: ToolsViewModel = hiltViewModel()) {
    val records by vm.practice.collectAsStateWithLifecycle()
    val subjects by vm.subjects.collectAsStateWithLifecycle()
    var showEditor by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<PracticeRecord?>(null) }

    val attempted = records.count { it.result != "skipped" }
    val correct = records.count { it.result == "correct" }
    val partial = records.count { it.result == "partial" }
    val accuracy = Grading.accuracy(correct, partial, attempted)

    LazyColumn(Modifier.fillMaxSize(), contentPadding = LIST_PADDING, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            ScreenTitle(
                title = "Practice tracker",
                subtitle = "${records.size} questions logged",
                action = {
                    IconButton(onClick = { showEditor = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Log question", tint = MaterialTheme.colorScheme.primary)
                    }
                },
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("Attempted", "$attempted", Modifier.weight(1f))
                StatTile("Correct", "$correct", Modifier.weight(1f), accent = Success)
                StatTile("Wrong", "${records.count { it.result == "incorrect" }}", Modifier.weight(1f), accent = Danger)
                StatTile("Accuracy", "$accuracy%", Modifier.weight(1f))
            }
        }
        if (records.isEmpty()) {
            item {
                PcmCard {
                    EmptyState(
                        icon = Icons.Filled.Check,
                        title = "No questions logged",
                        hint = "Log a numerical after solving it — accuracy and weak chapters build up automatically.",
                        actionLabel = "Log question",
                        onAction = { showEditor = true },
                    )
                }
            }
        } else {
            items(records, key = { it.id }) { record ->
                PcmCard {
                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(record.title, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(
                                "${record.attemptDate} · ${record.difficulty}" + (record.minutes?.let { " · ${it}m" } ?: ""),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Pill(
                            record.result,
                            when (record.result) {
                                "correct" -> Success
                                "partial" -> Warning
                                "incorrect" -> Danger
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                        IconButton(onClick = { deleteTarget = record }, modifier = Modifier.width(32.dp)) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    if (showEditor) {
        var title by remember { mutableStateOf("") }
        var result by remember { mutableStateOf("correct") }
        var difficulty by remember { mutableStateOf("medium") }
        var minutes by remember { mutableStateOf("") }
        var subjectId by remember { mutableStateOf(subjects.firstOrNull()?.id ?: "") }
        var error by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { showEditor = false },
            title = { Text("Log question") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PcmTextField(title, { title = it }, "Question / identifier *", placeholder = "NCERT Ex 3.14 Q12")
                    PcmTextField(minutes, { minutes = it.filter { c -> c.isDigit() } }, "Time taken (min)", keyboardType = KeyboardType.Number)
                    Text("Result", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("correct", "partial", "incorrect", "skipped").forEach { value ->
                            OutlinedButton(onClick = { result = value }) {
                                Text(value, style = MaterialTheme.typography.labelSmall, color = if (result == value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Text("Difficulty", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("easy", "medium", "hard").forEach { value ->
                            OutlinedButton(onClick = { difficulty = value }) {
                                Text(value, style = MaterialTheme.typography.labelSmall, color = if (difficulty == value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Text("Subject", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        subjects.forEach { subject ->
                            OutlinedButton(onClick = { subjectId = subject.id }) {
                                Text(subject.name, style = MaterialTheme.typography.labelSmall, color = if (subjectId == subject.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    error?.let { Text(it, color = Danger, style = MaterialTheme.typography.bodySmall) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val yearId = vm.yearId.value
                    if (title.isBlank()) {
                        error = "A title is required."
                        return@TextButton
                    }
                    if (yearId == null || subjectId.isBlank()) {
                        error = "Pick a subject first."
                        return@TextButton
                    }
                    vm.savePractice(
                        PracticeRecord(
                            yearId = yearId,
                            subjectId = subjectId,
                            title = title.trim(),
                            attemptDate = LocalDate.now().toString(),
                            minutes = minutes.toIntOrNull(),
                            result = result,
                            difficulty = difficulty,
                        ),
                    )
                    showEditor = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showEditor = false }) { Text("Cancel") } },
        )
    }

    deleteTarget?.let { record ->
        ConfirmDialog(
            title = "Delete record?",
            message = "\"${record.title}\" will be removed.",
            onDismiss = { deleteTarget = null },
            onConfirm = {
                vm.deletePractice(record.id)
                deleteTarget = null
            },
        )
    }
}

@Composable
fun MistakesScreen(vm: ToolsViewModel = hiltViewModel()) {
    val mistakes by vm.mistakes.collectAsStateWithLifecycle()
    val subjects by vm.subjects.collectAsStateWithLifecycle()
    var filter by remember { mutableStateOf("open") }
    var showEditor by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Mistake?>(null) }

    val visible = mistakes.filter {
        when (filter) {
            "open" -> !it.resolved
            "resolved" -> it.resolved
            else -> true
        }
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = LIST_PADDING, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            ScreenTitle(
                title = "Mistake notebook",
                subtitle = "${mistakes.count { !it.resolved }} unresolved",
                action = {
                    IconButton(onClick = { showEditor = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Log mistake", tint = MaterialTheme.colorScheme.primary)
                    }
                },
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("open", "resolved", "all").forEach { value ->
                    OutlinedButton(onClick = { filter = value }) {
                        Text(value, style = MaterialTheme.typography.labelSmall, color = if (filter == value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        if (visible.isEmpty()) {
            item {
                PcmCard {
                    EmptyState(
                        icon = Icons.Filled.Star,
                        title = "No mistakes logged",
                        hint = "Recording why an answer went wrong is the fastest way to stop repeating it.",
                        actionLabel = "Log mistake",
                        onAction = { showEditor = true },
                    )
                }
            }
        } else {
            items(visible, key = { it.id }) { mistake ->
                PcmCard {
                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
                        IconButton(onClick = { vm.resolveMistake(mistake.id, !mistake.resolved) }, modifier = Modifier.width(30.dp)) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = "Toggle resolved",
                                tint = if (mistake.resolved) Success else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                mistake.question,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Pill(mistake.category, Warning)
                                Text(mistake.date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            mistake.wrongApproach?.let {
                                Text("✗ $it", style = MaterialTheme.typography.labelSmall, color = Danger, maxLines = 2)
                            }
                            mistake.correctApproach?.let {
                                Text("✓ $it", style = MaterialTheme.typography.labelSmall, color = Success, maxLines = 2)
                            }
                        }
                        IconButton(onClick = { deleteTarget = mistake }, modifier = Modifier.width(30.dp)) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    if (showEditor) {
        var question by remember { mutableStateOf("") }
        var wrong by remember { mutableStateOf("") }
        var correct by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("conceptual") }
        var subjectId by remember { mutableStateOf(subjects.firstOrNull()?.id ?: "") }
        var error by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { showEditor = false },
            title = { Text("Log mistake") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PcmTextField(question, { question = it }, "Question / problem *", singleLine = false, minLines = 2)
                    PcmTextField(wrong, { wrong = it }, "My incorrect approach", singleLine = false, minLines = 2)
                    PcmTextField(correct, { correct = it }, "Correct approach", singleLine = false, minLines = 2)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("conceptual", "formula", "calculation", "sign", "unit", "silly", "incomplete", "time").forEach { value ->
                            OutlinedButton(onClick = { category = value }) {
                                Text(value, style = MaterialTheme.typography.labelSmall, color = if (category == value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        subjects.forEach { subject ->
                            OutlinedButton(onClick = { subjectId = subject.id }) {
                                Text(subject.name, style = MaterialTheme.typography.labelSmall, color = if (subjectId == subject.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    error?.let { Text(it, color = Danger, style = MaterialTheme.typography.bodySmall) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val yearId = vm.yearId.value
                    if (question.isBlank()) {
                        error = "Describe the question."
                        return@TextButton
                    }
                    if (yearId == null || subjectId.isBlank()) {
                        error = "Pick a subject first."
                        return@TextButton
                    }
                    vm.saveMistake(
                        Mistake(
                            yearId = yearId,
                            subjectId = subjectId,
                            question = question.trim(),
                            wrongApproach = wrong.ifBlank { null },
                            correctApproach = correct.ifBlank { null },
                            category = category,
                            date = LocalDate.now().toString(),
                        ),
                    )
                    showEditor = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showEditor = false }) { Text("Cancel") } },
        )
    }

    deleteTarget?.let { mistake ->
        ConfirmDialog(
            title = "Delete mistake?",
            message = "This entry will be removed from the notebook.",
            onDismiss = { deleteTarget = null },
            onConfirm = {
                vm.deleteMistake(mistake.id)
                deleteTarget = null
            },
        )
    }
}

@Composable
fun TestsScreen(vm: ToolsViewModel = hiltViewModel()) {
    val exams by vm.exams.collectAsStateWithLifecycle()
    val subjects by vm.subjects.collectAsStateWithLifecycle()
    var showEditor by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Exam?>(null) }

    val graded = exams.filter { it.obtainedMarks != null && it.maxMarks > 0 }.sortedBy { it.date }
    val trend = graded.map { Grading.percent(it.obtainedMarks, it.maxMarks).toFloat() }
    val average = if (graded.isEmpty()) 0 else graded.sumOf { Grading.percent(it.obtainedMarks, it.maxMarks) } / graded.size

    LazyColumn(Modifier.fillMaxSize(), contentPadding = LIST_PADDING, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            ScreenTitle(
                title = "Tests",
                subtitle = "${graded.size} graded · average $average%",
                action = {
                    IconButton(onClick = { showEditor = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add test", tint = MaterialTheme.colorScheme.primary)
                    }
                },
            )
        }
        if (trend.size > 1) {
            item {
                PcmCard {
                    Column(Modifier.padding(12.dp)) {
                        SectionHeader("Score trend")
                        LineChart(trend, maxOverride = 100f)
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("Tests", "${exams.size}", Modifier.weight(1f))
                StatTile("Average", "$average%", Modifier.weight(1f))
                StatTile("Best", "${graded.maxOfOrNull { Grading.percent(it.obtainedMarks, it.maxMarks) } ?: 0}%", Modifier.weight(1f), accent = Success)
            }
        }
        if (exams.isEmpty()) {
            item {
                PcmCard {
                    EmptyState(
                        icon = Icons.Filled.Check,
                        title = "No tests recorded",
                        hint = "Log marks after each test to see trends and weak chapters.",
                        actionLabel = "Add test",
                        onAction = { showEditor = true },
                    )
                }
            }
        } else {
            items(exams, key = { it.id }) { exam ->
                val percent = Grading.percent(exam.obtainedMarks, exam.maxMarks)
                PcmCard {
                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(exam.name, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(
                                "${exam.date} · ${exam.obtainedMarks?.toInt() ?: "—"}/${exam.maxMarks.toInt()}" + (exam.durationMinutes?.let { " · ${it}m" } ?: ""),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (exam.obtainedMarks != null) {
                            Pill("$percent%", when {
                                percent >= 75 -> Success
                                percent >= 50 -> Warning
                                else -> Danger
                            })
                        } else {
                            Pill("upcoming", MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { deleteTarget = exam }, modifier = Modifier.width(30.dp)) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    if (showEditor) {
        var name by remember { mutableStateOf("") }
        var type by remember { mutableStateOf("unit") }
        var date by remember { mutableStateOf(LocalDate.now().toString()) }
        var maxMarks by remember { mutableStateOf("100") }
        var obtained by remember { mutableStateOf("") }
        var duration by remember { mutableStateOf("") }
        var error by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showEditor = false },
            title = { Text("Add test") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PcmTextField(name, { name = it }, "Name *")
                    PcmTextField(date, { date = it }, "Date", placeholder = "yyyy-mm-dd")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f)) {
                            PcmTextField(maxMarks, { maxMarks = it }, "Max marks", keyboardType = KeyboardType.Number)
                        }
                        Column(Modifier.weight(1f)) {
                            PcmTextField(obtained, { obtained = it }, "Obtained", keyboardType = KeyboardType.Number)
                        }
                        Column(Modifier.weight(1f)) {
                            PcmTextField(duration, { duration = it }, "Minutes", keyboardType = KeyboardType.Number)
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("unit", "monthly", "half_yearly", "annual", "mock", "custom").forEach { value ->
                            OutlinedButton(onClick = { type = value }) {
                                Text(
                                    value.replace('_', ' '),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (type == value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    error?.let { Text(it, color = Danger, style = MaterialTheme.typography.bodySmall) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val yearId = vm.yearId.value
                    val max = maxMarks.toDoubleOrNull() ?: 0.0
                    val got = obtained.toDoubleOrNull()
                    if (name.isBlank()) {
                        error = "A name is required."
                        return@TextButton
                    }
                    if (yearId == null) {
                        error = "No academic year selected."
                        return@TextButton
                    }
                    if (max <= 0) {
                        error = "Maximum marks must be greater than zero."
                        return@TextButton
                    }
                    if (got != null && (got < 0 || got > max)) {
                        error = "Obtained marks must be between 0 and the maximum."
                        return@TextButton
                    }
                    if (runCatching { LocalDate.parse(date) }.isFailure) {
                        error = "Use a valid date (yyyy-mm-dd)."
                        return@TextButton
                    }
                    vm.saveExam(
                        Exam(
                            yearId = yearId,
                            name = name.trim(),
                            type = type,
                            date = date,
                            maxMarks = max,
                            obtainedMarks = got,
                            durationMinutes = duration.toIntOrNull(),
                        ),
                    )
                    showEditor = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showEditor = false }) { Text("Cancel") } },
        )
    }

    deleteTarget?.let { exam ->
        ConfirmDialog(
            title = "Delete test?",
            message = "\"${exam.name}\" will be removed.",
            onDismiss = { deleteTarget = null },
            onConfirm = {
                vm.deleteExam(exam.id)
                deleteTarget = null
            },
        )
    }
}

@Composable
fun NotesScreen(vm: ToolsViewModel = hiltViewModel()) {
    val notes by vm.notes.collectAsStateWithLifecycle()
    val subjects by vm.subjects.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var editor by remember { mutableStateOf<Note?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf<String?>(null) }
    var deleteTarget by remember { mutableStateOf<Note?>(null) }

    val visible = notes.filter { query.isBlank() || (it.title + " " + it.body).contains(query, ignoreCase = true) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = LIST_PADDING, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            ScreenTitle(
                title = "Notes",
                subtitle = "${notes.size} notes",
                action = {
                    IconButton(onClick = { editor = null; showEditor = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "New note", tint = MaterialTheme.colorScheme.primary)
                    }
                },
            )
        }
        item { PcmTextField(query, { query = it }, "Search notes") }
        if (visible.isEmpty()) {
            item {
                PcmCard {
                    EmptyState(
                        icon = Icons.Filled.Search,
                        title = "No notes",
                        hint = "Write notes with # headings, **bold**, - lists and \$maths\$.",
                        actionLabel = "New note",
                        onAction = { editor = null; showEditor = true },
                    )
                }
            }
        } else {
            items(visible, key = { it.id }) { note ->
                PcmCard(onClick = { expanded = if (expanded == note.id) null else note.id }) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(note.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            IconButton(onClick = { vm.favouriteNote(note) }, modifier = Modifier.width(32.dp)) {
                                Icon(
                                    Icons.Filled.Star,
                                    contentDescription = "Favourite",
                                    tint = if (note.favourite) Warning else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        if (expanded == note.id) {
                            MarkdownText(note.body)
                            Row {
                                TextButton(onClick = { editor = note; showEditor = true }) { Text("Edit") }
                                TextButton(onClick = { deleteTarget = note }) { Text("Delete", color = Danger) }
                            }
                        } else {
                            Text(
                                note.body.take(140),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showEditor) {
        var title by remember { mutableStateOf(editor?.title ?: "") }
        var body by remember { mutableStateOf(editor?.body ?: "") }
        var subjectId by remember { mutableStateOf(editor?.subjectId ?: "") }
        var preview by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showEditor = false },
            title = { Text(if (editor == null) "New note" else "Edit note") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PcmTextField(title, { title = it }, "Title *")
                    if (preview) {
                        PcmCard { Column(Modifier.padding(10.dp)) { MarkdownText(body) } }
                    } else {
                        PcmTextField(body, { body = it }, "Body (Markdown)", singleLine = false, minLines = 8)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { preview = !preview }) { Text(if (preview) "Edit" else "Preview") }
                        subjects.take(3).forEach { subject ->
                            OutlinedButton(onClick = { subjectId = subject.id }) {
                                Text(
                                    subject.name.take(8),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (subjectId == subject.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    error?.let { Text(it, color = Danger, style = MaterialTheme.typography.bodySmall) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val yearId = vm.yearId.value
                    if (title.isBlank()) {
                        error = "A title is required."
                        return@TextButton
                    }
                    if (yearId == null) {
                        error = "No academic year selected."
                        return@TextButton
                    }
                    vm.saveNote(
                        (editor ?: Note(yearId = yearId, title = "")).copy(
                            title = title.trim(),
                            body = body,
                            subjectId = subjectId.ifBlank { null },
                        ),
                    )
                    showEditor = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showEditor = false }) { Text("Cancel") } },
        )
    }

    deleteTarget?.let { note ->
        ConfirmDialog(
            title = "Delete note?",
            message = "\"${note.title}\" will be removed permanently.",
            onDismiss = { deleteTarget = null },
            onConfirm = {
                vm.deleteNote(note.id)
                deleteTarget = null
            },
        )
    }
}
