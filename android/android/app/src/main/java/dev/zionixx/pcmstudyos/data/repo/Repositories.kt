package dev.zionixx.pcmstudyos.data.repo

import androidx.room.withTransaction
import dev.zionixx.pcmstudyos.data.local.PcmDatabase
import dev.zionixx.pcmstudyos.data.local.entity.AcademicYear
import dev.zionixx.pcmstudyos.data.local.entity.Chapter
import dev.zionixx.pcmstudyos.data.local.entity.Goal
import dev.zionixx.pcmstudyos.data.local.entity.StudyComponent
import dev.zionixx.pcmstudyos.data.local.entity.StudySession
import dev.zionixx.pcmstudyos.data.local.entity.StudyStreak
import dev.zionixx.pcmstudyos.data.local.entity.StudyTask
import dev.zionixx.pcmstudyos.data.local.entity.Subject
import dev.zionixx.pcmstudyos.data.local.entity.Subtopic
import dev.zionixx.pcmstudyos.data.local.entity.Topic
import dev.zionixx.pcmstudyos.data.local.entity.newId
import dev.zionixx.pcmstudyos.data.local.entity.nowMillis
import dev.zionixx.pcmstudyos.data.local.seed.Seeder
import dev.zionixx.pcmstudyos.domain.CatchupPlanner
import dev.zionixx.pcmstudyos.domain.Progress
import dev.zionixx.pcmstudyos.domain.StudyStats
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyllabusRepository @Inject constructor(private val db: PcmDatabase) {

    /** Increments on every syllabus write so screens can recompute progress. */
    private val changeSignal = kotlinx.coroutines.flow.MutableStateFlow(0)
    val changes: kotlinx.coroutines.flow.StateFlow<Int> = changeSignal

    private fun bump() {
        changeSignal.value += 1
    }

    data class YearProgress(
        val overall: Int,
        val subjectPercents: Map<String, Int>,
        val chapterPercents: Map<String, Int>,
        val topicPercents: Map<String, Int>,
        val completedTopics: Int,
        val totalTopics: Int,
        val completedChapters: Int,
        val totalChapters: Int,
    )

    fun years(): Flow<List<AcademicYear>> = db.academicYearDao().observeAll()

    suspend fun currentYear(): AcademicYear? = db.academicYearDao().current() ?: db.academicYearDao().getAll().firstOrNull()

    suspend fun ensureSeedData() {
        if (db.academicYearDao().count() == 0) {
            Seeder(db).createYearWithSyllabus()
        }
    }

    suspend fun addSeedSyllabus(yearId: String) {
        Seeder(db).installSyllabus(yearId)
        bump()
    }

    suspend fun createYear(name: String, start: String, end: String, withSyllabus: Boolean = true): String {
        val year = AcademicYear(name = name, startDate = start, endDate = end, isCurrent = false)
        db.academicYearDao().upsert(year)
        if (withSyllabus) Seeder(db).installSyllabus(year.id)
        bump()
        return year.id
    }

    suspend fun updateYear(year: AcademicYear) {
        db.academicYearDao().upsert(year.copy(updatedAt = nowMillis()))
    }

    suspend fun setCurrentYear(yearId: String) {
        db.withTransaction {
            db.academicYearDao().clearCurrent()
            db.academicYearDao().byId(yearId)?.let {
                db.academicYearDao().upsert(it.copy(isCurrent = true, updatedAt = nowMillis()))
            }
        }
    }

    suspend fun archiveYear(yearId: String, archived: Boolean) {
        db.academicYearDao().byId(yearId)?.let {
            db.academicYearDao().upsert(it.copy(archived = archived, updatedAt = nowMillis()))
        }
    }

    /** Deleting a year cascades to subjects -> chapters -> topics -> everything. */
    suspend fun deleteYear(yearId: String) {
        db.academicYearDao().deleteById(yearId)
    }

    suspend fun copySyllabus(fromYearId: String, toYearId: String) {
        db.withTransaction {
            val subjects = db.subjectDao().forYear(fromYearId)
            var order = db.subjectDao().countForYear(toYearId)
            for (subject in subjects) {
                val newSubjectId = newId()
                db.subjectDao().upsert(subject.copy(id = newSubjectId, yearId = toYearId, sortOrder = order++, createdAt = nowMillis(), updatedAt = nowMillis()))
                for (chapter in db.chapterDao().forSubject(subject.id)) {
                    val newChapterId = newId()
                    db.chapterDao().upsert(chapter.copy(id = newChapterId, subjectId = newSubjectId, createdAt = nowMillis(), updatedAt = nowMillis()))
                    for (topic in db.topicDao().forChapter(chapter.id)) {
                        val newTopicId = newId()
                        db.topicDao().upsert(
                            topic.copy(
                                id = newTopicId,
                                chapterId = newChapterId,
                                status = Progress.STATUS_NOT_STARTED,
                                createdAt = nowMillis(),
                                updatedAt = nowMillis(),
                            ),
                        )
                        val subs = db.subtopicDao().forTopic(topic.id)
                        if (subs.isNotEmpty()) {
                            db.subtopicDao().upsertAll(subs.map { it.copy(id = newId(), topicId = newTopicId, completed = false, createdAt = nowMillis(), updatedAt = nowMillis()) })
                        }
                        val comps = db.componentDao().forTopic(topic.id)
                        if (comps.isNotEmpty()) {
                            db.componentDao().upsertAll(comps.map { it.copy(id = newId(), topicId = newTopicId, done = false) })
                        }
                    }
                }
            }
        }
    }

    suspend fun topicList(yearId: String): List<Topic> = db.progressDao().topicsForYear(yearId)

    fun subjects(yearId: String): Flow<List<Subject>> = db.subjectDao().observeForYear(yearId)

    fun chapters(subjectId: String): Flow<List<Chapter>> = db.chapterDao().observeForSubject(subjectId)

    fun topics(chapterId: String): Flow<List<Topic>> = db.topicDao().observeForChapter(chapterId)

    fun subtopics(topicId: String): Flow<List<Subtopic>> = db.subtopicDao().observeForTopic(topicId)

    fun components(topicId: String): Flow<List<StudyComponent>> = db.componentDao().observeForTopic(topicId)

    suspend fun saveSubject(subject: Subject) {
        db.subjectDao().upsert(subject.copy(updatedAt = nowMillis()))
        bump()
    }

    suspend fun deleteSubject(subjectId: String) {
        // Chapters -> topics -> subtopics/components go with it (FK CASCADE).
        db.subjectDao().deleteById(subjectId)
        bump()
    }

    suspend fun saveChapter(chapter: Chapter) {
        db.chapterDao().upsert(chapter.copy(updatedAt = nowMillis()))
        bump()
    }

    suspend fun deleteChapter(chapterId: String) {
        db.chapterDao().deleteById(chapterId)
        bump()
    }

    suspend fun saveTopic(topic: Topic) {
        db.topicDao().upsert(topic.copy(updatedAt = nowMillis()))
        bump()
    }

    suspend fun deleteTopic(topicId: String) {
        db.topicDao().deleteById(topicId)
        bump()
    }

    suspend fun setTopicStatus(topicId: String, status: String) {
        db.topicDao().setStatus(topicId, status, nowMillis())
        bump()
    }

    suspend fun addSubtopic(topicId: String, name: String) {
        val order = db.subtopicDao().forTopic(topicId).size
        db.subtopicDao().upsert(Subtopic(topicId = topicId, name = name, sortOrder = order))
        bump()
    }

    suspend fun toggleSubtopic(subtopicId: String, done: Boolean) {
        db.subtopicDao().setCompleted(subtopicId, done, nowMillis())
        bump()
    }

    suspend fun deleteSubtopic(subtopicId: String) {
        db.subtopicDao().deleteById(subtopicId)
        bump()
    }

    suspend fun toggleComponent(componentId: String, done: Boolean) {
        db.componentDao().setDone(componentId, done)
        bump()
    }

    suspend fun duplicateChapter(chapterId: String) {
        db.withTransaction {
            val chapter = db.chapterDao().byId(chapterId) ?: return@withTransaction
            val copy = chapter.copy(
                id = newId(),
                name = "${chapter.name} (copy)",
                sortOrder = chapter.sortOrder + 1,
                createdAt = nowMillis(),
                updatedAt = nowMillis(),
            )
            db.chapterDao().upsert(copy)
            for (topic in db.topicDao().forChapter(chapterId)) {
                val newTopicId = newId()
                db.topicDao().upsert(topic.copy(id = newTopicId, chapterId = copy.id, status = Progress.STATUS_NOT_STARTED, createdAt = nowMillis(), updatedAt = nowMillis()))
                val subs = db.subtopicDao().forTopic(topic.id)
                if (subs.isNotEmpty()) db.subtopicDao().upsertAll(subs.map { it.copy(id = newId(), topicId = newTopicId, completed = false) })
                val comps = db.componentDao().forTopic(topic.id)
                if (comps.isNotEmpty()) db.componentDao().upsertAll(comps.map { it.copy(id = newId(), topicId = newTopicId, done = false) })
            }
        }
        bump()
    }

    suspend fun moveSubject(subjectId: String, delta: Int) {
        val subject = db.subjectDao().byId(subjectId) ?: return
        val siblings = db.subjectDao().forYear(subject.yearId)
        val index = siblings.indexOfFirst { it.id == subjectId }
        val target = siblings.getOrNull(index + delta) ?: return
        db.subjectDao().upsert(subject.copy(sortOrder = target.sortOrder, updatedAt = nowMillis()))
        db.subjectDao().upsert(target.copy(sortOrder = subject.sortOrder, updatedAt = nowMillis()))
        bump()
    }

    suspend fun moveChapter(chapterId: String, delta: Int) {
        val chapter = db.chapterDao().byId(chapterId) ?: return
        val siblings = db.chapterDao().forSubject(chapter.subjectId)
        val index = siblings.indexOfFirst { it.id == chapterId }
        val target = siblings.getOrNull(index + delta) ?: return
        db.chapterDao().upsert(chapter.copy(sortOrder = target.sortOrder, updatedAt = nowMillis()))
        db.chapterDao().upsert(target.copy(sortOrder = chapter.sortOrder, updatedAt = nowMillis()))
        bump()
    }

    suspend fun moveTopic(topicId: String, delta: Int) {
        val topic = db.topicDao().byId(topicId) ?: return
        val siblings = db.topicDao().forChapter(topic.chapterId)
        val index = siblings.indexOfFirst { it.id == topicId }
        val target = siblings.getOrNull(index + delta) ?: return
        db.topicDao().upsert(topic.copy(sortOrder = target.sortOrder, updatedAt = nowMillis()))
        db.topicDao().upsert(target.copy(sortOrder = topic.sortOrder, updatedAt = nowMillis()))
        bump()
    }

    /** Creates chapters/topics/subtopics from a bulk-import preview. */
    suspend fun importParsed(
        subjectId: String,
        chapters: List<dev.zionixx.pcmstudyos.domain.ImportParser.ParsedChapter>,
        existingChapterId: String? = null,
        skipDuplicates: Boolean = true,
    ): Int {
        var imported = 0
        db.withTransaction {
            if (existingChapterId != null) {
                var order = db.topicDao().countForChapter(existingChapterId)
                val existingNames = db.topicDao().forChapter(existingChapterId).map { it.name.lowercase() }.toMutableSet()
                for (chapter in chapters) {
                    for (topic in chapter.topics) {
                        if (skipDuplicates && !existingNames.add(topic.name.lowercase())) continue
                        val topicId = newId()
                        db.topicDao().upsert(
                            Topic(id = topicId, chapterId = existingChapterId, name = topic.name, sortOrder = order++),
                        )
                        if (topic.subtopics.isNotEmpty()) {
                            db.subtopicDao().upsertAll(
                                topic.subtopics.mapIndexed { index, name ->
                                    Subtopic(topicId = topicId, name = name, sortOrder = index)
                                },
                            )
                        }
                        imported++
                    }
                }
            } else {
                var chapterOrder = db.chapterDao().countForSubject(subjectId)
                for (chapter in chapters) {
                    val chapterId = newId()
                    db.chapterDao().upsert(
                        Chapter(id = chapterId, subjectId = subjectId, name = chapter.name, sortOrder = chapterOrder++),
                    )
                    var order = 0
                    for (topic in chapter.topics) {
                        val topicId = newId()
                        db.topicDao().upsert(
                            Topic(id = topicId, chapterId = chapterId, name = topic.name, sortOrder = order++),
                        )
                        if (topic.subtopics.isNotEmpty()) {
                            db.subtopicDao().upsertAll(
                                topic.subtopics.mapIndexed { index, name ->
                                    Subtopic(topicId = topicId, name = name, sortOrder = index)
                                },
                            )
                        }
                        imported++
                    }
                }
            }
        }
        bump()
        return imported
    }

    /** Global search across the year's content. */
    suspend fun search(yearId: String, query: String): List<dev.zionixx.pcmstudyos.domain.SearchResult> {
        val results = mutableListOf<dev.zionixx.pcmstudyos.domain.SearchResult>()
        val subjects = db.subjectDao().forYear(yearId)
        val chapters = db.progressDao().chaptersForYear(yearId)
        val topics = db.progressDao().topicsForYear(yearId)

        subjects.filter { it.name.lowercase().contains(query) }.forEach {
            results += dev.zionixx.pcmstudyos.domain.SearchResult("subject", it.name, "Subject", "syllabus")
        }
        chapters.filter { it.name.lowercase().contains(query) }.forEach { chapter ->
            val subject = subjects.firstOrNull { it.id == chapter.subjectId }?.name
            results += dev.zionixx.pcmstudyos.domain.SearchResult("chapter", chapter.name, subject, "syllabus")
        }
        topics.filter { it.name.lowercase().contains(query) }.forEach { topic ->
            val chapter = chapters.firstOrNull { it.id == topic.chapterId }?.name
            results += dev.zionixx.pcmstudyos.domain.SearchResult("topic", topic.name, chapter, "syllabus")
        }
        db.noteDao().getAll().filter { it.yearId == yearId && (it.title + " " + it.body).lowercase().contains(query) }.forEach {
            results += dev.zionixx.pcmstudyos.domain.SearchResult("note", it.title, "Note", "notes")
        }
        db.formulaDao().getAll().filter { it.yearId == yearId && (it.title + " " + it.expression).lowercase().contains(query) }.forEach {
            results += dev.zionixx.pcmstudyos.domain.SearchResult("formula", it.title, it.category, "formulas")
        }
        db.taskDao().forYear(yearId).filter { it.title.lowercase().contains(query) }.forEach {
            results += dev.zionixx.pcmstudyos.domain.SearchResult("task", it.title, it.status, "planner")
        }
        db.mistakeDao().getAll().filter { it.yearId == yearId && it.question.lowercase().contains(query) }.forEach {
            results += dev.zionixx.pcmstudyos.domain.SearchResult("mistake", it.question, it.category, "mistakes")
        }
        db.examDao().getAll().filter { it.yearId == yearId && it.name.lowercase().contains(query) }.forEach {
            results += dev.zionixx.pcmstudyos.domain.SearchResult("test", it.name, it.date, "tests")
        }
        return results.take(80)
    }

    suspend fun progress(yearId: String): YearProgress {
        val subjects = db.subjectDao().forYear(yearId)
        val chapters = db.progressDao().chaptersForYear(yearId)
        val topics = db.progressDao().topicsForYear(yearId)
        val subtopics = db.progressDao().subtopicsForYear(yearId)
        val components = db.progressDao().componentsForYear(yearId)

        val subsByTopic = subtopics.groupBy { it.topicId }
        val compsByTopic = components.groupBy { it.topicId }
        val topicsByChapter = topics.groupBy { it.chapterId }
        val chaptersBySubject = chapters.groupBy { it.subjectId }

        val topicPercents = topics.associate { topic ->
            topic.id to Progress.topicPercent(topic, subsByTopic[topic.id].orEmpty(), compsByTopic[topic.id].orEmpty())
        }
        val chapterPercents = chapters.associate { chapter ->
            chapter.id to Progress.chapterPercent(topicsByChapter[chapter.id].orEmpty()) { topicPercents[it.id] ?: 0 }
        }
        val subjectPercents = subjects.associate { subject ->
            subject.id to Progress.subjectPercent(chaptersBySubject[subject.id].orEmpty().map { chapterPercents[it.id] ?: 0 })
        }

        return YearProgress(
            overall = Progress.overall(subjectPercents.values.toList()),
            subjectPercents = subjectPercents,
            chapterPercents = chapterPercents,
            topicPercents = topicPercents,
            completedTopics = topicPercents.count { it.value == 100 },
            totalTopics = topics.size,
            completedChapters = chapterPercents.count { it.value == 100 },
            totalChapters = chapters.size,
        )
    }

    suspend fun topicPercentOf(topicId: String): Int {
        val topic = db.topicDao().byId(topicId) ?: return 0
        return Progress.topicPercent(topic, db.subtopicDao().forTopic(topicId), db.componentDao().forTopic(topicId))
    }
}

@Singleton
class StudyRepository @Inject constructor(private val db: PcmDatabase) {

    fun tasks(yearId: String): Flow<List<StudyTask>> = db.taskDao().observeForYear(yearId)

    fun sessions(yearId: String): Flow<List<StudySession>> = db.sessionDao().observeForYear(yearId)

    fun pendingTaskCount(yearId: String): Flow<Int> = db.taskDao().observePendingCount(yearId)

    fun streaks(): Flow<List<StudyStreak>> = db.streakDao().observeAll()

    suspend fun saveTask(task: StudyTask) = db.taskDao().upsert(task.copy(updatedAt = nowMillis()))

    suspend fun deleteTask(id: String) = db.taskDao().deleteById(id)

    suspend fun setTaskStatus(id: String, status: String) {
        val task = db.taskDao().byId(id) ?: return
        val completedAt = if (status == "completed") nowMillis() else null
        db.taskDao().setStatus(id, status, completedAt, nowMillis())
        if (status == "completed" && task.recurring != "none") {
            val next = task.copy(
                id = newId(),
                status = "pending",
                completedAt = null,
                dueDate = task.dueDate?.let { date ->
                    val parsed = runCatching { LocalDate.parse(date) }.getOrNull()
                    parsed?.plusDays(if (task.recurring == "daily") 1 else 7)?.format(DateTimeFormatter.ISO_LOCAL_DATE)
                },
                createdAt = nowMillis(),
                updatedAt = nowMillis(),
            )
            db.taskDao().upsert(next)
        }
    }

    suspend fun reschedule(id: String, date: LocalDate) {
        db.taskDao().reschedule(id, date.format(DateTimeFormatter.ISO_LOCAL_DATE), nowMillis())
    }

    /** Saves a focus session and keeps the streak table in step. */
    suspend fun recordSession(session: StudySession) {
        db.withTransaction {
            db.sessionDao().insert(session)
            val date = StudyStats.dateOf(session.startedAt).format(DateTimeFormatter.ISO_LOCAL_DATE)
            val existing = db.streakDao().forDate(date)
            db.streakDao().upsert(
                StudyStreak(
                    date = date,
                    minutes = (existing?.minutes ?: 0) + session.minutes,
                    sessionCount = (existing?.sessionCount ?: 0) + 1,
                ),
            )
        }
    }

    suspend fun deleteSession(id: String) = db.sessionDao().deleteById(id)

    suspend fun stats(yearId: String, sessions: List<StudySession>): StudyStats.Summary =
        StudyStats.summarize(sessions, db.streakDao().getAll())

    suspend fun catchupPlan(yearId: String, capacityMinutes: Int, studyDays: Set<Int>): CatchupPlanner.Plan =
        CatchupPlanner.plan(
            today = LocalDate.now(),
            tasks = db.taskDao().forYear(yearId),
            dailyCapacityMinutes = capacityMinutes,
            studyDays = studyDays,
        )

    suspend fun subjectList(yearId: String): List<Subject> = db.subjectDao().forYear(yearId)

    suspend fun topicList(yearId: String): List<Topic> = db.progressDao().topicsForYear(yearId)

    fun goals(yearId: String): Flow<List<Goal>> = db.goalDao().observeForYear(yearId)

    suspend fun saveGoal(goal: Goal) = db.goalDao().upsert(goal.copy(updatedAt = nowMillis()))

    suspend fun deleteGoal(id: String) = db.goalDao().deleteById(id)
}
