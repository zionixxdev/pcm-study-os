package dev.zionixx.pcmstudyos.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.zionixx.pcmstudyos.data.local.entity.Exam
import dev.zionixx.pcmstudyos.data.local.entity.FormulaEntry
import dev.zionixx.pcmstudyos.data.local.entity.Goal
import dev.zionixx.pcmstudyos.data.local.entity.Mistake
import dev.zionixx.pcmstudyos.data.local.entity.Note
import dev.zionixx.pcmstudyos.data.local.entity.PracticeRecord
import dev.zionixx.pcmstudyos.data.local.entity.RevisionItem
import dev.zionixx.pcmstudyos.data.local.entity.StudySession
import dev.zionixx.pcmstudyos.data.local.entity.StudyTask
import dev.zionixx.pcmstudyos.data.local.entity.Subject
import dev.zionixx.pcmstudyos.data.local.entity.Topic
import dev.zionixx.pcmstudyos.data.repo.ActiveYear
import dev.zionixx.pcmstudyos.data.repo.AppSettings
import dev.zionixx.pcmstudyos.data.repo.SettingsRepository
import dev.zionixx.pcmstudyos.data.repo.StudyRepository
import dev.zionixx.pcmstudyos.data.repo.SyllabusRepository
import dev.zionixx.pcmstudyos.data.repo.ToolsRepository
import dev.zionixx.pcmstudyos.domain.CatchupPlanner
import dev.zionixx.pcmstudyos.domain.StudyStats
import dev.zionixx.pcmstudyos.timer.TimerController
import dev.zionixx.pcmstudyos.timer.commit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

private val STOPPED = SharingStarted.WhileSubscribed(5_000)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val syllabusRepository: SyllabusRepository,
    private val studyRepository: StudyRepository,
    private val toolsRepository: ToolsRepository,
    private val settingsRepository: SettingsRepository,
    activeYear: ActiveYear,
) : ViewModel() {

    val yearId: StateFlow<String?> = activeYear.id

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val progress = combine(activeYear.id, syllabusRepository.changes) { id, _ -> id }
        .mapLatest { id -> id?.let { syllabusRepository.progress(it) } }
        .stateIn(viewModelScope, STOPPED, null)

    val subjects = activeYear.id
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else syllabusRepository.subjects(id) }
        .stateIn(viewModelScope, STOPPED, emptyList())

    val tasks = activeYear.id
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else studyRepository.tasks(id) }
        .stateIn(viewModelScope, STOPPED, emptyList())

    val sessions = activeYear.id
        .flatMapLatest { id -> if (id == null) flowOf(emptyList<StudySession>()) else studyRepository.sessions(id) }
        .stateIn(viewModelScope, STOPPED, emptyList())

    val revisions = activeYear.id
        .flatMapLatest { id -> if (id == null) flowOf(emptyList<RevisionItem>()) else toolsRepository.revisions(id) }
        .stateIn(viewModelScope, STOPPED, emptyList())

    val exams = activeYear.id
        .flatMapLatest { id -> if (id == null) flowOf(emptyList<Exam>()) else toolsRepository.exams(id) }
        .stateIn(viewModelScope, STOPPED, emptyList())

    val stats: StateFlow<StudyStats.Summary?> = combine(sessions, studyRepository.streaks()) { list, streaks ->
        StudyStats.summarize(list, streaks)
    }.stateIn(viewModelScope, STOPPED, null)

    fun completeTask(taskId: String) = viewModelScope.launch {
        studyRepository.setTaskStatus(taskId, "completed")
    }

    fun reopenTask(taskId: String) = viewModelScope.launch {
        studyRepository.setTaskStatus(taskId, "pending")
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlannerViewModel @Inject constructor(
    private val studyRepository: StudyRepository,
    private val settingsRepository: SettingsRepository,
    activeYear: ActiveYear,
) : ViewModel() {

    val yearId: StateFlow<String?> = activeYear.id

    val tasks = activeYear.id
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else studyRepository.tasks(id) }
        .stateIn(viewModelScope, STOPPED, emptyList())

    val settings = settingsRepository.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val plan = MutableStateFlow<CatchupPlanner.Plan?>(null)
    val planError = MutableStateFlow<String?>(null)

    fun saveTask(task: StudyTask) = viewModelScope.launch { studyRepository.saveTask(task) }

    fun deleteTask(id: String) = viewModelScope.launch { studyRepository.deleteTask(id) }

    fun toggleTask(task: StudyTask) = viewModelScope.launch {
        studyRepository.setTaskStatus(task.id, if (task.status == "completed") "pending" else "completed")
    }

    fun buildPlan(capacityMinutes: Int) = viewModelScope.launch {
        val id = yearId.value
        if (id == null) {
            planError.value = "No academic year selected."
            return@launch
        }
        val current = settings.value
        plan.value = studyRepository.catchupPlan(id, capacityMinutes, current.studyDays)
        planError.value = null
    }

    fun applyPlan(selected: List<CatchupPlanner.Suggestion>) = viewModelScope.launch {
        selected.forEach { suggestion -> studyRepository.reschedule(suggestion.taskId, suggestion.date) }
        plan.value = null
    }

    fun clearPlan() {
        plan.value = null
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SyllabusViewModel @Inject constructor(
    private val syllabusRepository: SyllabusRepository,
    private val settingsRepository: SettingsRepository,
    activeYear: ActiveYear,
) : ViewModel() {

    val yearId: StateFlow<String?> = activeYear.id

    val subjects = activeYear.id
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else syllabusRepository.subjects(id) }
        .stateIn(viewModelScope, STOPPED, emptyList())

    val progress = combine(activeYear.id, syllabusRepository.changes) { id, _ -> id }
        .mapLatest { id -> id?.let { syllabusRepository.progress(it) } }
        .stateIn(viewModelScope, STOPPED, null)

    val settings = settingsRepository.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val selectedSubjectId = MutableStateFlow<String?>(null)
    val selectedChapterId = MutableStateFlow<String?>(null)
    val expandedTopicId = MutableStateFlow<String?>(null)

    val chapters = combine(selectedSubjectId, syllabusRepository.changes) { id, _ -> id }
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else syllabusRepository.chapters(id) }
        .stateIn(viewModelScope, STOPPED, emptyList())

    val topics = combine(selectedChapterId, syllabusRepository.changes) { id, _ -> id }
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else syllabusRepository.topics(id) }
        .stateIn(viewModelScope, STOPPED, emptyList())

    val subtopics = combine(expandedTopicId, syllabusRepository.changes) { id, _ -> id }
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else syllabusRepository.subtopics(id) }
        .stateIn(viewModelScope, STOPPED, emptyList())

    val components = combine(expandedTopicId, syllabusRepository.changes) { id, _ -> id }
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else syllabusRepository.components(id) }
        .stateIn(viewModelScope, STOPPED, emptyList())

    fun selectSubject(id: String?) {
        selectedSubjectId.value = id
        selectedChapterId.value = null
        expandedTopicId.value = null
    }

    fun selectChapter(id: String?) {
        selectedChapterId.value = id
        expandedTopicId.value = null
    }

    fun toggleTopic(id: String) {
        expandedTopicId.value = if (expandedTopicId.value == id) null else id
    }

    fun saveSubject(subject: Subject) = viewModelScope.launch {
        val id = yearId.value ?: return@launch
        syllabusRepository.saveSubject(if (subject.yearId.isEmpty()) subject.copy(yearId = id) else subject)
    }

    fun deleteSubject(id: String) = viewModelScope.launch { syllabusRepository.deleteSubject(id) }

    fun saveChapter(chapter: dev.zionixx.pcmstudyos.data.local.entity.Chapter) = viewModelScope.launch {
        syllabusRepository.saveChapter(chapter)
    }

    fun deleteChapter(id: String) = viewModelScope.launch { syllabusRepository.deleteChapter(id) }

    fun saveTopic(topic: Topic) = viewModelScope.launch { syllabusRepository.saveTopic(topic) }

    fun deleteTopic(id: String) = viewModelScope.launch { syllabusRepository.deleteTopic(id) }

    fun setStatus(topicId: String, status: String) = viewModelScope.launch { syllabusRepository.setTopicStatus(topicId, status) }

    fun addSubtopic(topicId: String, name: String) = viewModelScope.launch { syllabusRepository.addSubtopic(topicId, name) }

    fun toggleSubtopic(id: String, done: Boolean) = viewModelScope.launch { syllabusRepository.toggleSubtopic(id, done) }

    fun deleteSubtopic(id: String) = viewModelScope.launch { syllabusRepository.deleteSubtopic(id) }

    fun toggleComponent(id: String, done: Boolean) = viewModelScope.launch { syllabusRepository.toggleComponent(id, done) }

    fun duplicateChapter(id: String) = viewModelScope.launch { syllabusRepository.duplicateChapter(id) }

    fun moveSubject(id: String, delta: Int) = viewModelScope.launch { syllabusRepository.moveSubject(id, delta) }

    fun moveChapter(id: String, delta: Int) = viewModelScope.launch { syllabusRepository.moveChapter(id, delta) }

    fun moveTopic(id: String, delta: Int) = viewModelScope.launch { syllabusRepository.moveTopic(id, delta) }

    suspend fun importTopics(
        subjectId: String,
        chapters: List<dev.zionixx.pcmstudyos.domain.ImportParser.ParsedChapter>,
        existingChapterId: String?,
    ): Int = syllabusRepository.importParsed(subjectId, chapters, existingChapterId)
}

@HiltViewModel
class TimerViewModel @Inject constructor(
    val controller: TimerController,
    private val studyRepository: StudyRepository,
    private val syllabusRepository: SyllabusRepository,
    private val settingsRepository: SettingsRepository,
    activeYear: ActiveYear,
) : ViewModel() {

    val yearId: StateFlow<String?> = activeYear.id

    val subjectsFlow = activeYear.id
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else syllabusRepository.subjects(id) }
        .stateIn(viewModelScope, STOPPED, emptyList<Subject>())
    val settings = settingsRepository.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())
    val state = controller.state

    val sessions = activeYear.id
        .flatMapLatest { id -> if (id == null) flowOf(emptyList<StudySession>()) else studyRepository.sessions(id) }
        .stateIn(viewModelScope, STOPPED, emptyList())

    val todayMinutes: StateFlow<Int> = sessions.map { list ->
        val today = LocalDate.now()
        list.filter { StudyStats.dateOf(it.startedAt) == today }.sumOf { it.minutes }
    }.stateIn(viewModelScope, STOPPED, 0)

    fun commit() = viewModelScope.launch { controller.commit(studyRepository, settingsRepository) }

    fun configure(mode: dev.zionixx.pcmstudyos.timer.TimerMode) {
        val current = settings.value
        controller.configure(mode, current.pomodoroFocus, current.pomodoroShortBreak, current.pomodoroLongBreak)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RevisionViewModel @Inject constructor(
    private val toolsRepository: ToolsRepository,
    private val syllabusRepository: SyllabusRepository,
    private val settingsRepository: SettingsRepository,
    activeYear: ActiveYear,
) : ViewModel() {

    val yearId: StateFlow<String?> = activeYear.id

    val topicsFlow = combine(activeYear.id, syllabusRepository.changes) { id, _ -> id }
        .mapLatest { id -> if (id == null) emptyList() else syllabusRepository.topicList(id) }
        .stateIn(viewModelScope, STOPPED, emptyList<Topic>())

    val revisions = activeYear.id
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else toolsRepository.revisions(id) }
        .stateIn(viewModelScope, STOPPED, emptyList())

    val history = toolsRepository.revisionHistory().stateIn(viewModelScope, STOPPED, emptyList())

    val settings = settingsRepository.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    fun add(topicId: String?, label: String, date: LocalDate, notes: String?) = viewModelScope.launch {
        val id = yearId.value ?: return@launch
        toolsRepository.addRevision(id, topicId, label, date, notes)
    }

    fun complete(item: RevisionItem, rating: String, notes: String?) = viewModelScope.launch {
        toolsRepository.completeRevision(item, rating, settings.value.revisionIntervals, notes)
    }

    fun delete(id: String) = viewModelScope.launch { toolsRepository.deleteRevision(id) }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ToolsViewModel @Inject constructor(
    private val toolsRepository: ToolsRepository,
    private val studyRepository: StudyRepository,
    activeYear: ActiveYear,
) : ViewModel() {

    val yearId: StateFlow<String?> = activeYear.id

    val formulas = activeYear.id.flatMapLatest { id -> if (id == null) flowOf(emptyList()) else toolsRepository.formulas(id) }
        .stateIn(viewModelScope, STOPPED, emptyList<FormulaEntry>())

    val practice = activeYear.id.flatMapLatest { id -> if (id == null) flowOf(emptyList()) else toolsRepository.practice(id) }
        .stateIn(viewModelScope, STOPPED, emptyList<PracticeRecord>())

    val mistakes = activeYear.id.flatMapLatest { id -> if (id == null) flowOf(emptyList()) else toolsRepository.mistakes(id) }
        .stateIn(viewModelScope, STOPPED, emptyList<Mistake>())

    val exams = activeYear.id.flatMapLatest { id -> if (id == null) flowOf(emptyList()) else toolsRepository.exams(id) }
        .stateIn(viewModelScope, STOPPED, emptyList<Exam>())

    val notes = activeYear.id.flatMapLatest { id -> if (id == null) flowOf(emptyList()) else toolsRepository.notes(id) }
        .stateIn(viewModelScope, STOPPED, emptyList<Note>())

    val goals = activeYear.id.flatMapLatest { id -> if (id == null) flowOf(emptyList()) else studyRepository.goals(id) }
        .stateIn(viewModelScope, STOPPED, emptyList<Goal>())

    val subjects = activeYear.id.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else kotlinx.coroutines.flow.flowOf(studyRepository.subjectList(id))
    }.stateIn(viewModelScope, STOPPED, emptyList<Subject>())

    val topics = activeYear.id.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else kotlinx.coroutines.flow.flowOf(studyRepository.topicList(id))
    }.stateIn(viewModelScope, STOPPED, emptyList<Topic>())

    fun saveFormula(entry: FormulaEntry) = viewModelScope.launch { toolsRepository.saveFormula(entry) }
    fun deleteFormula(id: String) = viewModelScope.launch { toolsRepository.deleteFormula(id) }
    fun favouriteFormula(entry: FormulaEntry) = viewModelScope.launch { toolsRepository.toggleFormulaFavourite(entry) }

    fun savePractice(record: PracticeRecord) = viewModelScope.launch { toolsRepository.savePractice(record) }
    fun deletePractice(id: String) = viewModelScope.launch { toolsRepository.deletePractice(id) }

    fun saveMistake(mistake: Mistake) = viewModelScope.launch { toolsRepository.saveMistake(mistake) }
    fun resolveMistake(id: String, resolved: Boolean) = viewModelScope.launch { toolsRepository.setMistakeResolved(id, resolved) }
    fun deleteMistake(id: String) = viewModelScope.launch { toolsRepository.deleteMistake(id) }

    fun saveExam(exam: Exam, results: List<dev.zionixx.pcmstudyos.data.local.entity.TestResult> = emptyList()) =
        viewModelScope.launch { toolsRepository.saveExam(exam, results) }
    fun deleteExam(id: String) = viewModelScope.launch { toolsRepository.deleteExam(id) }

    fun saveNote(note: Note) = viewModelScope.launch { toolsRepository.saveNote(note) }
    fun deleteNote(id: String) = viewModelScope.launch { toolsRepository.deleteNote(id) }
    fun favouriteNote(note: Note) = viewModelScope.launch { toolsRepository.toggleNoteFavourite(note) }

    fun saveGoal(goal: Goal) = viewModelScope.launch { studyRepository.saveGoal(goal) }
    fun deleteGoal(id: String) = viewModelScope.launch { studyRepository.deleteGoal(id) }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val syllabusRepository: SyllabusRepository,
    private val studyRepository: StudyRepository,
    private val toolsRepository: ToolsRepository,
    activeYear: ActiveYear,
) : ViewModel() {

    val yearId: StateFlow<String?> = activeYear.id
    val rangeDays = MutableStateFlow(30)

    val sessions = activeYear.id
        .flatMapLatest { id -> if (id == null) flowOf(emptyList<StudySession>()) else studyRepository.sessions(id) }
        .stateIn(viewModelScope, STOPPED, emptyList())

    val subjects = activeYear.id
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else syllabusRepository.subjects(id) }
        .stateIn(viewModelScope, STOPPED, emptyList())

    val progress = combine(activeYear.id, syllabusRepository.changes) { id, _ -> id }
        .mapLatest { id -> id?.let { syllabusRepository.progress(it) } }
        .stateIn(viewModelScope, STOPPED, null)

    val exams = activeYear.id.flatMapLatest { id -> if (id == null) flowOf(emptyList()) else toolsRepository.exams(id) }
        .stateIn(viewModelScope, STOPPED, emptyList<Exam>())

    val practice = activeYear.id.flatMapLatest { id -> if (id == null) flowOf(emptyList()) else toolsRepository.practice(id) }
        .stateIn(viewModelScope, STOPPED, emptyList<PracticeRecord>())

    val revisions = activeYear.id.flatMapLatest { id -> if (id == null) flowOf(emptyList()) else toolsRepository.revisions(id) }
        .stateIn(viewModelScope, STOPPED, emptyList<RevisionItem>())

    val mistakes = activeYear.id.flatMapLatest { id -> if (id == null) flowOf(emptyList()) else toolsRepository.mistakes(id) }
        .stateIn(viewModelScope, STOPPED, emptyList<Mistake>())

    val topics = combine(activeYear.id, syllabusRepository.changes) { id, _ -> id }
        .mapLatest { id -> if (id == null) emptyList() else syllabusRepository.topicList(id) }
        .stateIn(viewModelScope, STOPPED, emptyList<Topic>())

    fun setRange(days: Int) {
        rangeDays.value = days
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val syllabusRepository: SyllabusRepository,
    private val studyRepository: StudyRepository,
    private val toolsRepository: ToolsRepository,
    activeYear: ActiveYear,
) : ViewModel() {

    val query = MutableStateFlow("")
    val yearId: StateFlow<String?> = activeYear.id

    val hits: StateFlow<List<dev.zionixx.pcmstudyos.domain.SearchResult>> = combine(query, activeYear.id) { q, id -> q.trim().lowercase() to id }
        .mapLatest { (q, id) ->
            if (q.length < 2 || id == null) return@mapLatest emptyList()
            syllabusRepository.search(id, q)
        }
        .stateIn(viewModelScope, STOPPED, emptyList())

    fun setQuery(value: String) {
        query.value = value
    }
}
