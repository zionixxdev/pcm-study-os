package dev.zionixx.pcmstudyos.data.repo

import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.room.withTransaction
import dev.zionixx.pcmstudyos.data.local.PcmDatabase
import dev.zionixx.pcmstudyos.data.local.entity.AcademicYear
import dev.zionixx.pcmstudyos.data.local.entity.Chapter
import dev.zionixx.pcmstudyos.data.local.entity.Exam
import dev.zionixx.pcmstudyos.data.local.entity.FormulaEntry
import dev.zionixx.pcmstudyos.data.local.entity.Goal
import dev.zionixx.pcmstudyos.data.local.entity.Mistake
import dev.zionixx.pcmstudyos.data.local.entity.Note
import dev.zionixx.pcmstudyos.data.local.entity.PracticeRecord
import dev.zionixx.pcmstudyos.data.local.entity.RevisionHistory
import dev.zionixx.pcmstudyos.data.local.entity.RevisionItem
import dev.zionixx.pcmstudyos.data.local.entity.StudyComponent
import dev.zionixx.pcmstudyos.data.local.entity.StudySession
import dev.zionixx.pcmstudyos.data.local.entity.StudyStreak
import dev.zionixx.pcmstudyos.data.local.entity.StudyTask
import dev.zionixx.pcmstudyos.data.local.entity.Subject
import dev.zionixx.pcmstudyos.data.local.entity.Subtopic
import dev.zionixx.pcmstudyos.data.local.entity.TestResult
import dev.zionixx.pcmstudyos.data.local.entity.Topic
import dev.zionixx.pcmstudyos.domain.Grading
import dev.zionixx.pcmstudyos.domain.StudyStats
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class BackupFile(
    val app: String = "pcm-study-os-android",
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val academicYears: List<AcademicYear> = emptyList(),
    val subjects: List<Subject> = emptyList(),
    val chapters: List<Chapter> = emptyList(),
    val topics: List<Topic> = emptyList(),
    val subtopics: List<Subtopic> = emptyList(),
    val components: List<StudyComponent> = emptyList(),
    val tasks: List<StudyTask> = emptyList(),
    val sessions: List<StudySession> = emptyList(),
    val revisions: List<RevisionItem> = emptyList(),
    val revisionHistory: List<RevisionHistory> = emptyList(),
    val formulas: List<FormulaEntry> = emptyList(),
    val practice: List<PracticeRecord> = emptyList(),
    val mistakes: List<Mistake> = emptyList(),
    val exams: List<Exam> = emptyList(),
    val testResults: List<TestResult> = emptyList(),
    val notes: List<Note> = emptyList(),
    val goals: List<Goal> = emptyList(),
    val streaks: List<StudyStreak> = emptyList(),
) {
    val recordCount: Int
        get() = academicYears.size + subjects.size + chapters.size + topics.size + subtopics.size +
            components.size + tasks.size + sessions.size + revisions.size + revisionHistory.size +
            formulas.size + practice.size + mistakes.size + exams.size + testResults.size +
            notes.size + goals.size + streaks.size
}

data class RestoreResult(val ok: Boolean, val message: String, val records: Int = 0)

/**
 * Local-only backup, restore, CSV and PDF export. Everything goes through the
 * Storage Access Framework: the app never writes outside a location the user
 * picks, and never uploads anything.
 */
@Singleton
class BackupRepository @Inject constructor(private val db: PcmDatabase) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // ------------------------------------------------------------------ JSON

    suspend fun snapshot(): BackupFile = BackupFile(
        academicYears = db.academicYearDao().getAll(),
        subjects = db.subjectDao().getAll(),
        chapters = db.chapterDao().getAll(),
        topics = db.topicDao().getAll(),
        subtopics = db.subtopicDao().getAll(),
        components = db.componentDao().getAll(),
        tasks = db.taskDao().getAll(),
        sessions = db.sessionDao().getAll(),
        revisions = db.revisionDao().getAll(),
        revisionHistory = db.revisionDao().getAllHistory(),
        formulas = db.formulaDao().getAll(),
        practice = db.practiceDao().getAll(),
        mistakes = db.mistakeDao().getAll(),
        exams = db.examDao().getAll(),
        testResults = db.examDao().getAllResults(),
        notes = db.noteDao().getAll(),
        goals = db.goalDao().getAll(),
        streaks = db.streakDao().getAll(),
    )

    suspend fun writeBackup(out: OutputStream): Int {
        val backup = snapshot()
        out.bufferedWriter().use { it.write(json.encodeToString(backup)) }
        return backup.recordCount
    }

    /** Validates before touching anything; a bad file never destroys data. */
    suspend fun restore(input: InputStream, replace: Boolean): RestoreResult {
        val text = input.bufferedReader().use { it.readText() }
        val parsed = runCatching { json.decodeFromString<BackupFile>(text) }.getOrNull()
            ?: return RestoreResult(false, "That file is not a valid PCM Study OS backup.")
        if (parsed.app != "pcm-study-os-android") {
            return RestoreResult(false, "This backup was not produced by PCM Study OS.")
        }
        if (parsed.recordCount == 0) {
            return RestoreResult(false, "The backup contains no records.")
        }

        db.withTransaction {
            if (replace) {
                // Child tables first, so foreign keys are never violated.
                val maintenance = db.maintenanceDao()
                maintenance.clearTestResults()
                maintenance.clearRevisionHistory()
                maintenance.clearRevisions()
                maintenance.clearSessions()
                maintenance.clearStreaks()
                maintenance.clearTasks()
                maintenance.clearFormulas()
                maintenance.clearPractice()
                maintenance.clearMistakes()
                maintenance.clearExams()
                maintenance.clearNotes()
                maintenance.clearGoals()
                maintenance.clearComponents()
                maintenance.clearSubtopics()
                maintenance.clearTopics()
                maintenance.clearChapters()
                maintenance.clearSubjects()
                maintenance.clearYears()
            }
            db.academicYearDao().upsertAll(parsed.academicYears)
            db.subjectDao().upsertAll(parsed.subjects)
            db.chapterDao().upsertAll(parsed.chapters)
            db.topicDao().upsertAll(parsed.topics)
            db.subtopicDao().upsertAll(parsed.subtopics)
            db.componentDao().upsertAll(parsed.components)
            db.taskDao().upsertAll(parsed.tasks)
            db.sessionDao().insertAll(parsed.sessions)
            db.revisionDao().upsertAll(parsed.revisions)
            db.revisionDao().insertHistoryAll(parsed.revisionHistory)
            db.formulaDao().upsertAll(parsed.formulas)
            db.practiceDao().upsertAll(parsed.practice)
            db.mistakeDao().upsertAll(parsed.mistakes)
            db.examDao().upsertAll(parsed.exams)
            db.examDao().upsertResults(parsed.testResults)
            db.noteDao().upsertAll(parsed.notes)
            db.goalDao().upsertAll(parsed.goals)
            db.streakDao().upsertAll(parsed.streaks)
        }
        return RestoreResult(true, "Restored ${parsed.recordCount} records.", parsed.recordCount)
    }

    // ------------------------------------------------------------------- CSV

    enum class CsvTable { SYLLABUS, TASKS, SESSIONS, REVISIONS, TESTS, PRACTICE, MISTAKES }

    suspend fun writeCsv(table: CsvTable, yearId: String, out: OutputStream) {
        val builder = StringBuilder()
        when (table) {
            CsvTable.SYLLABUS -> {
                builder.appendLine("subject,chapter,topic,status,priority")
                val subjects = db.subjectDao().forYear(yearId)
                for (subject in subjects) {
                    for (chapter in db.chapterDao().forSubject(subject.id)) {
                        for (topic in db.topicDao().forChapter(chapter.id)) {
                            builder.appendLine(listOf(subject.name, chapter.name, topic.name, topic.status, topic.priority).joinToCsv())
                        }
                    }
                }
            }
            CsvTable.TASKS -> {
                builder.appendLine("title,status,priority,due_date,estimated_minutes")
                db.taskDao().forYear(yearId).forEach {
                    builder.appendLine(listOf(it.title, it.status, it.priority, it.dueDate ?: "", it.estimatedMinutes.toString()).joinToCsv())
                }
            }
            CsvTable.SESSIONS -> {
                builder.appendLine("started_at,minutes,mode,notes")
                db.sessionDao().getAll().filter { it.yearId == yearId }.forEach {
                    builder.appendLine(listOf(StudyStats.dateOf(it.startedAt).toString(), it.minutes.toString(), it.mode, it.notes ?: "").joinToCsv())
                }
            }
            CsvTable.REVISIONS -> {
                builder.appendLine("item,due_date,status,last_rating,interval_index")
                db.revisionDao().forYear(yearId).forEach {
                    builder.appendLine(listOf(it.label, it.dueDate, it.status, it.lastRating ?: "", it.intervalIndex.toString()).joinToCsv())
                }
            }
            CsvTable.TESTS -> {
                builder.appendLine("name,type,date,max_marks,obtained,percentage")
                db.examDao().getAll().filter { it.yearId == yearId }.forEach {
                    builder.appendLine(
                        listOf(
                            it.name, it.type, it.date, it.maxMarks.toInt().toString(),
                            it.obtainedMarks?.toInt()?.toString() ?: "",
                            Grading.percent(it.obtainedMarks, it.maxMarks).toString(),
                        ).joinToCsv(),
                    )
                }
            }
            CsvTable.PRACTICE -> {
                builder.appendLine("question,result,difficulty,attempt_date,retry")
                db.practiceDao().getAll().filter { it.yearId == yearId }.forEach {
                    builder.appendLine(listOf(it.title, it.result, it.difficulty, it.attemptDate, it.retry.toString()).joinToCsv())
                }
            }
            CsvTable.MISTAKES -> {
                builder.appendLine("question,category,date,resolved")
                db.mistakeDao().getAll().filter { it.yearId == yearId }.forEach {
                    builder.appendLine(listOf(it.question, it.category, it.date, it.resolved.toString()).joinToCsv())
                }
            }
        }
        out.bufferedWriter().use { it.write(builder.toString()) }
    }

    // ------------------------------------------------------------------- PDF

    /** Progress report built with the platform PdfDocument — no third-party PDF library. */
    suspend fun writePdfReport(yearId: String, out: OutputStream) {
        val year = db.academicYearDao().byId(yearId)
        val subjects = db.subjectDao().forYear(yearId)
        val chapters = db.progressDao().chaptersForYear(yearId)
        val topics = db.progressDao().topicsForYear(yearId)
        val subtopics = db.progressDao().subtopicsForYear(yearId)
        val components = db.progressDao().componentsForYear(yearId)
        val sessions = db.sessionDao().getAll().filter { it.yearId == yearId }

        val subsByTopic = subtopics.groupBy { it.topicId }
        val compsByTopic = components.groupBy { it.topicId }
        val topicsByChapter = topics.groupBy { it.chapterId }
        val chaptersBySubject = chapters.groupBy { it.subjectId }

        val topicPct = topics.associate { it.id to dev.zionixx.pcmstudyos.domain.Progress.topicPercent(it, subsByTopic[it.id].orEmpty(), compsByTopic[it.id].orEmpty()) }
        val chapterPct = chapters.associate { chapter ->
            chapter.id to dev.zionixx.pcmstudyos.domain.Progress.chapterPercent(topicsByChapter[chapter.id].orEmpty()) { topicPct[it.id] ?: 0 }
        }
        val subjectPct = subjects.associate { subject ->
            subject.id to dev.zionixx.pcmstudyos.domain.Progress.subjectPercent(chaptersBySubject[subject.id].orEmpty().map { chapterPct[it.id] ?: 0 })
        }
        val overall = dev.zionixx.pcmstudyos.domain.Progress.overall(subjectPct.values.toList())
        val summary = StudyStats.summarize(sessions, db.streakDao().getAll())

        val document = PdfDocument()
        val paint = Paint().apply { textSize = 11f; color = Color.rgb(30, 41, 59) }
        val titlePaint = Paint().apply { textSize = 18f; color = Color.rgb(15, 23, 42); isFakeBoldText = true }
        val headingPaint = Paint().apply { textSize = 13f; color = Color.rgb(30, 64, 175); isFakeBoldText = true }

        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
        var canvas = page.canvas
        var y = 50f

        fun newPageIfNeeded(needed: Float) {
            if (y + needed < 800f) return
            document.finishPage(page)
            pageNumber++
            page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
            canvas = page.canvas
            y = 50f
        }

        canvas.drawText("PCM Study OS — Progress report", 40f, y, titlePaint)
        y += 18f
        canvas.drawText("${year?.name ?: "Academic year"} · generated ${LocalDate.now()}", 40f, y, paint)
        y += 26f

        canvas.drawText("Overall", 40f, y, headingPaint); y += 16f
        listOf(
            "Syllabus completion" to "$overall%",
            "Chapters completed" to "${chapterPct.count { it.value == 100 }}/${chapters.size}",
            "Topics completed" to "${topicPct.count { it.value == 100 }}/${topics.size}",
            "Study time logged" to "${summary.totalMinutes / 60}h ${summary.totalMinutes % 60}m",
            "Current streak" to "${summary.currentStreak} days",
        ).forEach { (label, value) ->
            newPageIfNeeded(18f)
            canvas.drawText("$label: $value", 52f, y, paint)
            y += 15f
        }

        y += 10f
        canvas.drawText("Subject progress", 40f, y, headingPaint); y += 16f
        subjects.forEach { subject ->
            newPageIfNeeded(18f)
            canvas.drawText("${subject.name} — ${subjectPct[subject.id] ?: 0}%", 52f, y, paint)
            y += 15f
        }

        y += 10f
        canvas.drawText("Chapter completion", 40f, y, headingPaint); y += 16f
        chapters.take(120).forEach { chapter ->
            newPageIfNeeded(18f)
            val subjectName = subjects.firstOrNull { it.id == chapter.subjectId }?.name ?: ""
            canvas.drawText("${chapter.name} ($subjectName) — ${chapterPct[chapter.id] ?: 0}%", 52f, y, paint)
            y += 14f
        }

        document.finishPage(page)
        document.writeTo(out)
        document.close()
    }
}

private fun List<String>.joinToCsv(): String = joinToString(",") { value ->
    if (value.contains(',') || value.contains('"') || value.contains('\n')) {
        "\"" + value.replace("\"", "\"\"") + "\""
    } else {
        value
    }
}
