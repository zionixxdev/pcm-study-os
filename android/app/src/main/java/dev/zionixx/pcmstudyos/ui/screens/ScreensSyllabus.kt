package dev.zionixx.pcmstudyos.ui.screens

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zionixx.pcmstudyos.data.local.entity.Chapter
import dev.zionixx.pcmstudyos.data.local.entity.StudyComponent
import dev.zionixx.pcmstudyos.data.local.entity.Subject
import dev.zionixx.pcmstudyos.data.local.entity.Topic
import dev.zionixx.pcmstudyos.domain.ImportParser
import dev.zionixx.pcmstudyos.domain.Progress
import dev.zionixx.pcmstudyos.ui.components.ConfirmDialog
import dev.zionixx.pcmstudyos.ui.components.EmptyState
import dev.zionixx.pcmstudyos.ui.components.PcmCard
import dev.zionixx.pcmstudyos.ui.components.PcmTextField
import dev.zionixx.pcmstudyos.ui.components.Pill
import dev.zionixx.pcmstudyos.ui.components.ScreenTitle
import dev.zionixx.pcmstudyos.ui.components.SectionHeader
import dev.zionixx.pcmstudyos.ui.components.StatusPill
import dev.zionixx.pcmstudyos.ui.components.ThinProgress
import dev.zionixx.pcmstudyos.ui.theme.Danger
import dev.zionixx.pcmstudyos.ui.theme.Success
import kotlinx.coroutines.launch

@Composable
fun SyllabusScreen(
    vm: SyllabusViewModel = hiltViewModel(),
    onOpenImport: () -> Unit,
) {
    val subjects by vm.subjects.collectAsStateWithLifecycle()
    val chapters by vm.chapters.collectAsStateWithLifecycle()
    val topics by vm.topics.collectAsStateWithLifecycle()
    val subtopics by vm.subtopics.collectAsStateWithLifecycle()
    val components by vm.components.collectAsStateWithLifecycle()
    val progress by vm.progress.collectAsStateWithLifecycle()
    val yearId by vm.yearId.collectAsStateWithLifecycle()
    val selectedSubjectId by vm.selectedSubjectId.collectAsStateWithLifecycle()
    val selectedChapterId by vm.selectedChapterId.collectAsStateWithLifecycle()
    val expandedTopicId by vm.expandedTopicId.collectAsStateWithLifecycle()

    var subjectEditor by remember { mutableStateOf<Subject?>(null) }
    var showSubjectEditor by remember { mutableStateOf(false) }
    var chapterEditor by remember { mutableStateOf<Chapter?>(null) }
    var showChapterEditor by remember { mutableStateOf(false) }
    var topicEditor by remember { mutableStateOf<Topic?>(null) }
    var showTopicEditor by remember { mutableStateOf(false) }
    var deleteSubject by remember { mutableStateOf<Subject?>(null) }
    var deleteChapter by remember { mutableStateOf<Chapter?>(null) }
    var deleteTopic by remember { mutableStateOf<Topic?>(null) }
    var newSubtopicFor by remember { mutableStateOf<Topic?>(null) }

    val selectedSubject = subjects.firstOrNull { it.id == selectedSubjectId }
    val selectedChapter = chapters.firstOrNull { it.id == selectedChapterId }

    Scaffold(
        floatingActionButton = {
            if (selectedChapter == null) {
                FloatingActionButton(
                    onClick = {
                        if (selectedSubject == null) {
                            subjectEditor = null; showSubjectEditor = true
                        } else {
                            chapterEditor = null; showChapterEditor = true
                        }
                    },
                ) {
                    Icon(Icons.Filled.Add, contentDescription = if (selectedSubject == null) "Add subject" else "Add chapter")
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 110.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                ScreenTitle(
                    title = when {
                        selectedChapter != null -> selectedChapter.name
                        selectedSubject != null -> selectedSubject.name
                        else -> "Syllabus"
                    },
                    subtitle = when {
                        selectedChapter != null -> selectedSubject?.name
                        selectedSubject != null -> "${chapters.size} chapters · ${progress?.subjectPercents?.get(selectedSubject.id) ?: 0}% complete"
                        else -> "${subjects.size} subjects · ${progress?.overall ?: 0}% overall"
                    },
                    action = {
                        Row {
                            if (selectedChapter != null || selectedSubject != null) {
                                IconButton(onClick = {
                                    if (selectedChapter != null) vm.selectChapter(null) else vm.selectSubject(null)
                                }) {
                                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                            IconButton(onClick = onOpenImport) {
                                Icon(Icons.Filled.Add, contentDescription = "Bulk import", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    },
                )
            }

            when {
                selectedSubject == null -> {
                    if (subjects.isEmpty()) {
                        item {
                            PcmCard {
                                EmptyState(
                                    icon = Icons.Filled.List,
                                    title = "No subjects",
                                    hint = "Add your first subject, or import a chapter list in bulk.",
                                    actionLabel = "Add subject",
                                    onAction = { subjectEditor = null; showSubjectEditor = true },
                                )
                            }
                        }
                    }
                    items(subjects, key = { it.id }) { subject ->
                        val percent = progress?.subjectPercents?.get(subject.id) ?: 0
                        PcmCard(onClick = { vm.selectSubject(subject.id) }) {
                            Column(Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    androidx.compose.foundation.layout.Box(
                                        Modifier.size(9.dp).background(parseColor(subject.color), RoundedCornerShape(50)),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(subject.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("$percent%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Spacer(Modifier.height(8.dp))
                                ThinProgress(percent, color = parseColor(subject.color))
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    TextButton(onClick = { subjectEditor = subject; showSubjectEditor = true }) { Text("Edit") }
                                    TextButton(onClick = { deleteSubject = subject }) { Text("Delete", color = Danger) }
                                }
                            }
                        }
                    }
                }

                selectedChapter == null -> {
                    if (chapters.isEmpty()) {
                        item {
                            PcmCard {
                                EmptyState(
                                    icon = Icons.Filled.List,
                                    title = "No chapters yet",
                                    hint = "Add a chapter, or paste a whole chapter list with Import.",
                                    actionLabel = "Add chapter",
                                    onAction = { chapterEditor = null; showChapterEditor = true },
                                )
                            }
                        }
                    }
                    items(chapters, key = { it.id }) { chapter ->
                        val percent = progress?.chapterPercents?.get(chapter.id) ?: 0
                        PcmCard(onClick = { vm.selectChapter(chapter.id) }) {
                            Column(Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(chapter.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Text("$percent%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Spacer(Modifier.height(8.dp))
                                ThinProgress(percent)
                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    TextButton(onClick = { chapterEditor = chapter; showChapterEditor = true }) { Text("Edit") }
                                    TextButton(onClick = { vm.moveChapter(chapter.id, -1) }) { Text("↑") }
                                    TextButton(onClick = { vm.moveChapter(chapter.id, 1) }) { Text("↓") }
                                    TextButton(onClick = { deleteChapter = chapter }) { Text("Delete", color = Danger) }
                                }
                            }
                        }
                    }
                }

                else -> {
                    item {
                        PcmCard {
                            Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(onClick = { topicEditor = null; showTopicEditor = true }) { Text("Add topic") }
                                OutlinedButton(onClick = { vm.duplicateChapter(selectedChapter.id) }) { Text("Duplicate chapter") }
                            }
                        }
                    }
                    if (topics.isEmpty()) {
                        item { PcmCard { EmptyState(icon = Icons.Filled.List, title = "No topics yet", hint = "Add a topic to start tracking progress.") } }
                    }
                    items(topics, key = { it.id }) { topic ->
                        val percent = progress?.topicPercents?.get(topic.id) ?: 0
                        val expanded = expandedTopicId == topic.id
                        PcmCard {
                            Column(Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { vm.toggleTopic(topic.id) }, modifier = Modifier.size(26.dp)) {
                                        Icon(
                                            if (expanded) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowRight,
                                            contentDescription = "Expand",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            topic.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                            StatusPill(topic.status)
                                            Text("$percent%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    IconButton(onClick = { vm.setStatus(topic.id, if (topic.status == "completed") Progress.STATUS_NOT_STARTED else Progress.STATUS_COMPLETED) }) {
                                        Icon(
                                            Icons.Filled.Check,
                                            contentDescription = "Mark complete",
                                            tint = if (topic.status == "completed") Success else MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                ThinProgress(percent)

                                if (expanded) {
                                    Spacer(Modifier.height(8.dp))
                                    if (components.isNotEmpty()) {
                                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            components.forEach { component ->
                                                ComponentChip(component) { vm.toggleComponent(component.id, !component.done) }
                                            }
                                        }
                                        Spacer(Modifier.height(6.dp))
                                    }
                                    subtopics.forEach { subtopic ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = { vm.toggleSubtopic(subtopic.id, !subtopic.completed) },
                                                modifier = Modifier.size(26.dp),
                                            ) {
                                                Icon(
                                                    Icons.Filled.Check,
                                                    contentDescription = "Toggle subtopic",
                                                    tint = if (subtopic.completed) Success else MaterialTheme.colorScheme.outline,
                                                    modifier = Modifier.size(15.dp),
                                                )
                                            }
                                            Text(
                                                subtopic.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                modifier = Modifier.weight(1f),
                                                color = if (subtopic.completed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                                maxLines = 2,
                                            )
                                            IconButton(onClick = { vm.deleteSubtopic(subtopic.id) }, modifier = Modifier.size(24.dp)) {
                                                Icon(Icons.Filled.Delete, contentDescription = "Delete subtopic", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        TextButton(onClick = { newSubtopicFor = topic }) { Text("Add subtopic") }
                                        TextButton(onClick = { topicEditor = topic; showTopicEditor = true }) { Text("Edit") }
                                        TextButton(onClick = { vm.moveTopic(topic.id, -1) }) { Text("↑") }
                                        TextButton(onClick = { vm.moveTopic(topic.id, 1) }) { Text("↓") }
                                        TextButton(onClick = { deleteTopic = topic }) { Text("Delete", color = Danger) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSubjectEditor) {
        SubjectEditorDialog(
            subject = subjectEditor,
            yearId = yearId,
            onDismiss = { showSubjectEditor = false; subjectEditor = null },
            onSave = { vm.saveSubject(it); showSubjectEditor = false; subjectEditor = null },
        )
    }

    if (showChapterEditor) {
        ChapterEditorDialog(
            chapter = chapterEditor,
            subjectId = selectedSubjectId,
            onDismiss = { showChapterEditor = false; chapterEditor = null },
            onSave = { vm.saveChapter(it); showChapterEditor = false; chapterEditor = null },
        )
    }

    if (showTopicEditor) {
        TopicEditorDialog(
            topic = topicEditor,
            chapterId = selectedChapterId,
            onDismiss = { showTopicEditor = false; topicEditor = null },
            onSave = { vm.saveTopic(it); showTopicEditor = false; topicEditor = null },
        )
    }

    newSubtopicFor?.let { topic ->
        var value by remember(topic.id) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { newSubtopicFor = null },
            title = { Text("Add subtopic") },
            text = { PcmTextField(value, { value = it }, "Name") },
            confirmButton = {
                TextButton(onClick = {
                    if (value.isNotBlank()) vm.addSubtopic(topic.id, value.trim())
                    newSubtopicFor = null
                }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { newSubtopicFor = null }) { Text("Cancel") } },
        )
    }

    deleteSubject?.let { subject ->
        ConfirmDialog(
            title = "Delete subject?",
            message = "\"${subject.name}\" and all of its chapters, topics and subtopics will be removed. This cannot be undone.",
            onDismiss = { deleteSubject = null },
            onConfirm = {
                vm.deleteSubject(subject.id)
                vm.selectSubject(null)
                deleteSubject = null
            },
        )
    }

    deleteChapter?.let { chapter ->
        ConfirmDialog(
            title = "Delete chapter?",
            message = "\"${chapter.name}\" and its topics will be removed.",
            onDismiss = { deleteChapter = null },
            onConfirm = {
                vm.deleteChapter(chapter.id)
                deleteChapter = null
            },
        )
    }

    deleteTopic?.let { topic ->
        ConfirmDialog(
            title = "Delete topic?",
            message = "\"${topic.name}\" and its subtopics will be removed.",
            onDismiss = { deleteTopic = null },
            onConfirm = {
                vm.deleteTopic(topic.id)
                deleteTopic = null
            },
        )
    }
}

@Composable
private fun ComponentChip(component: StudyComponent, onClick: () -> Unit) {
    val label = component.type.replaceFirstChar { it.uppercase() }
    OutlinedButton(onClick = onClick) {
        Text(
            if (component.done) "✓ $label" else label,
            style = MaterialTheme.typography.labelSmall,
            color = if (component.done) Success else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SubjectEditorDialog(
    subject: Subject?,
    yearId: String?,
    onDismiss: () -> Unit,
    onSave: (Subject) -> Unit,
) {
    var name by remember { mutableStateOf(subject?.name ?: "") }
    var description by remember { mutableStateOf(subject?.description ?: "") }
    var goal by remember { mutableStateOf(subject?.goal ?: "") }
    var color by remember { mutableStateOf(subject?.color ?: "#4F8CFF") }
    var priority by remember { mutableStateOf(subject?.priority ?: "medium") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (subject == null) "New subject" else "Edit subject") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PcmTextField(name, { name = it }, "Name *")
                PcmTextField(description, { description = it }, "Description", singleLine = false, minLines = 2)
                PcmTextField(goal, { goal = it }, "Goal")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("#4F8CFF", "#39C6A4", "#F2A03D", "#C084FC", "#F87171").forEach { hex ->
                        androidx.compose.foundation.layout.Box(
                            Modifier
                                .size(26.dp)
                                .background(parseColor(hex), RoundedCornerShape(50))
                                .padding(2.dp),
                        ) {
                            if (color == hex) {
                                Text("✓", color = androidx.compose.ui.graphics.Color.White, modifier = Modifier.padding(start = 6.dp))
                            }
                            androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().padding(0.dp))
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("high", "medium", "low").forEach { value ->
                        OutlinedButton(onClick = { priority = value }) {
                            Text(value, color = if (priority == value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
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
                if (yearId == null) {
                    error = "No academic year selected."
                    return@TextButton
                }
                onSave(
                    (subject ?: Subject(yearId = yearId, name = "")).copy(
                        name = name.trim(),
                        description = description.ifBlank { null },
                        goal = goal.ifBlank { null },
                        color = color,
                        priority = priority,
                    ),
                )
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ChapterEditorDialog(
    chapter: Chapter?,
    subjectId: String?,
    onDismiss: () -> Unit,
    onSave: (Chapter) -> Unit,
) {
    var name by remember { mutableStateOf(chapter?.name ?: "") }
    var description by remember { mutableStateOf(chapter?.description ?: "") }
    var deadline by remember { mutableStateOf(chapter?.deadline ?: "") }
    var hours by remember { mutableStateOf(chapter?.estimatedHours?.toString() ?: "") }
    var priority by remember { mutableStateOf(chapter?.priority ?: "medium") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (chapter == null) "New chapter" else "Edit chapter") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PcmTextField(name, { name = it }, "Name *")
                PcmTextField(description, { description = it }, "Description", singleLine = false, minLines = 2)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        PcmTextField(deadline, { deadline = it }, "Deadline", placeholder = "yyyy-mm-dd")
                    }
                    Column(Modifier.weight(1f)) {
                        PcmTextField(hours, { hours = it }, "Est. hours", keyboardType = KeyboardType.Decimal)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("high", "medium", "low").forEach { value ->
                        OutlinedButton(onClick = { priority = value }) {
                            Text(value, color = if (priority == value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
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
                val targetSubject = subjectId ?: chapter?.subjectId
                if (targetSubject == null) {
                    error = "No subject selected."
                    return@TextButton
                }
                onSave(
                    (chapter ?: Chapter(subjectId = targetSubject, name = "")).copy(
                        name = name.trim(),
                        description = description.ifBlank { null },
                        deadline = deadline.ifBlank { null },
                        estimatedHours = hours.toDoubleOrNull(),
                        priority = priority,
                    ),
                )
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun TopicEditorDialog(
    topic: Topic?,
    chapterId: String?,
    onDismiss: () -> Unit,
    onSave: (Topic) -> Unit,
) {
    var name by remember { mutableStateOf(topic?.name ?: "") }
    var notes by remember { mutableStateOf(topic?.notes ?: "") }
    var status by remember { mutableStateOf(topic?.status ?: "not_started") }
    var difficulty by remember { mutableStateOf(topic?.difficulty ?: "medium") }
    var minutes by remember { mutableStateOf(topic?.estimatedMinutes?.toString() ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (topic == null) "New topic" else "Edit topic") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PcmTextField(name, { name = it }, "Name *")
                PcmTextField(notes, { notes = it }, "Notes", singleLine = false, minLines = 2)
                PcmTextField(minutes, { minutes = it.filter { ch -> ch.isDigit() } }, "Estimated minutes", keyboardType = KeyboardType.Number)
                Text("Status", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("not_started", "in_progress", "completed", "needs_revision").forEach { value ->
                        OutlinedButton(onClick = { status = value }) {
                            Text(
                                value.replace('_', ' '),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (status == value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Text("Difficulty", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("easy", "medium", "hard").forEach { value ->
                        OutlinedButton(onClick = { difficulty = value }) {
                            Text(value, style = MaterialTheme.typography.labelSmall, color = if (difficulty == value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
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
                val target = chapterId ?: topic?.chapterId
                if (target == null) {
                    error = "No chapter selected."
                    return@TextButton
                }
                onSave(
                    (topic ?: Topic(chapterId = target, name = "")).copy(
                        name = name.trim(),
                        notes = notes.ifBlank { null },
                        status = status,
                        difficulty = difficulty,
                        estimatedMinutes = minutes.toIntOrNull(),
                    ),
                )
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Bulk import: paste → preview → confirm. Never writes without a preview. */
@Composable
fun ImportScreen(
    vm: SyllabusViewModel = hiltViewModel(),
    onDone: () -> Unit,
) {
    val subjects by vm.subjects.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    var parsed by remember { mutableStateOf<ImportParser.Result?>(null) }
    var subjectId by remember { mutableStateOf(subjects.firstOrNull()?.id ?: "") }
    var skipDuplicates by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ScreenTitle("Bulk import", "Paste topics, CSV or JSON — preview before anything is saved")
        Text("Subject", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            subjects.forEach { subject ->
                OutlinedButton(onClick = { subjectId = subject.id }) {
                    Text(subject.name, color = if (subjectId == subject.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        PcmTextField(
            text,
            { text = it; parsed = null },
            "Paste here",
            singleLine = false,
            minLines = 8,
            placeholder = "Chapter 1: Units and Measurements\n- Physical quantities\n  - Fundamental units",
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { parsed = ImportParser.parseAuto(text) }) { Text("Preview") }
            OutlinedButton(onClick = { skipDuplicates = !skipDuplicates }) {
                Text(if (skipDuplicates) "Skip duplicates: on" else "Skip duplicates: off")
            }
        }

        parsed?.let { result ->
            PcmCard {
                Column(Modifier.padding(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Pill("${result.chapters.size} chapters", MaterialTheme.colorScheme.primary)
                        Pill("${result.topicCount} topics", MaterialTheme.colorScheme.primary)
                        Pill("${result.subtopicCount} subtopics", MaterialTheme.colorScheme.primary)
                    }
                    result.warnings.take(4).forEach {
                        Text("• $it", style = MaterialTheme.typography.labelSmall, color = Danger)
                    }
                    Spacer(Modifier.height(6.dp))
                    LazyColumn(Modifier.height(180.dp)) {
                        result.chapters.forEach { chapter ->
                            item {
                                Text(chapter.name, style = MaterialTheme.typography.titleSmall)
                            }
                            items(chapter.topics) { topic ->
                                Text(
                                    "• ${topic.name}${if (topic.subtopics.isNotEmpty()) " (${topic.subtopics.size})" else ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
            OutlinedButton(
                onClick = {
                    if (subjectId.isBlank()) {
                        message = "Choose a subject first."
                        return@OutlinedButton
                    }
                    busy = true
                    scope.launch {
                        val count = vm.importTopics(subjectId, result.chapters, null)
                        busy = false
                        message = "$count topics imported."
                        text = ""
                        parsed = null
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (busy) "Importing…" else "Import ${result.topicCount} topics") }
        }
        message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Success) }
        TextButton(onClick = onDone) { Text("Done") }
    }
}
