package dev.zionixx.pcmstudyos.data.repo

import androidx.room.withTransaction
import dev.zionixx.pcmstudyos.data.local.PcmDatabase
import dev.zionixx.pcmstudyos.data.local.entity.Exam
import dev.zionixx.pcmstudyos.data.local.entity.FormulaEntry
import dev.zionixx.pcmstudyos.data.local.entity.Mistake
import dev.zionixx.pcmstudyos.data.local.entity.Note
import dev.zionixx.pcmstudyos.data.local.entity.PracticeRecord
import dev.zionixx.pcmstudyos.data.local.entity.RevisionHistory
import dev.zionixx.pcmstudyos.data.local.entity.RevisionItem
import dev.zionixx.pcmstudyos.data.local.entity.TestResult
import dev.zionixx.pcmstudyos.data.local.entity.newId
import dev.zionixx.pcmstudyos.data.local.entity.nowMillis
import dev.zionixx.pcmstudyos.domain.RevisionSchedule
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToolsRepository @Inject constructor(private val db: PcmDatabase) {

    // ------------------------------------------------------------- revisions

    fun revisions(yearId: String): Flow<List<RevisionItem>> = db.revisionDao().observeForYear(yearId)

    fun revisionHistory(): Flow<List<RevisionHistory>> = db.revisionDao().observeHistory()

    suspend fun addRevision(yearId: String, topicId: String?, label: String, dueDate: LocalDate, notes: String? = null) {
        val existing = db.revisionDao().forYear(yearId)
        val duplicate = existing.any {
            it.dueDate == dueDate.toString() && (it.topicId == topicId || it.label == label)
        }
        if (duplicate) return
        db.revisionDao().upsert(
            RevisionItem(
                yearId = yearId,
                topicId = topicId,
                label = label,
                dueDate = dueDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                status = "upcoming",
                notes = notes,
            ),
        )
    }

    /**
     * Marks a revision complete, records the rating, then schedules the next
     * occurrence from the user's interval ladder.
     */
    suspend fun completeRevision(item: RevisionItem, rating: String, intervals: List<Int>, notes: String? = null): LocalDate {
        val today = LocalDate.now()
        val nextIndex = RevisionSchedule.nextIntervalIndex(item.intervalIndex, rating, intervals.size)
        val nextDate = RevisionSchedule.nextDate(today, intervals, nextIndex)
        db.withTransaction {
            db.revisionDao().upsert(
                item.copy(
                    status = "completed",
                    completedAt = nowMillis(),
                    lastRating = rating,
                    notes = notes ?: item.notes,
                    updatedAt = nowMillis(),
                ),
            )
            db.revisionDao().insertHistory(
                RevisionHistory(
                    revisionId = item.id,
                    rating = rating,
                    notes = notes,
                ),
            )
            db.revisionDao().upsert(
                RevisionItem(
                    id = newId(),
                    yearId = item.yearId,
                    topicId = item.topicId,
                    subtopicId = item.subtopicId,
                    label = item.label,
                    dueDate = nextDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    intervalIndex = nextIndex,
                    status = "upcoming",
                ),
            )
        }
        return nextDate
    }

    suspend fun deleteRevision(id: String) = db.revisionDao().deleteById(id)

    // -------------------------------------------------------------- formulas

    fun formulas(yearId: String): Flow<List<FormulaEntry>> = db.formulaDao().observeForYear(yearId)

    suspend fun saveFormula(entry: FormulaEntry) = db.formulaDao().upsert(entry.copy(updatedAt = nowMillis()))

    suspend fun deleteFormula(id: String) = db.formulaDao().deleteById(id)

    suspend fun toggleFormulaFavourite(entry: FormulaEntry) =
        db.formulaDao().upsert(entry.copy(favourite = !entry.favourite, updatedAt = nowMillis()))

    // -------------------------------------------------------------- practice

    fun practice(yearId: String): Flow<List<PracticeRecord>> = db.practiceDao().observeForYear(yearId)

    suspend fun savePractice(record: PracticeRecord) = db.practiceDao().upsert(record.copy(updatedAt = nowMillis()))

    suspend fun deletePractice(id: String) = db.practiceDao().deleteById(id)

    // -------------------------------------------------------------- mistakes

    fun mistakes(yearId: String): Flow<List<Mistake>> = db.mistakeDao().observeForYear(yearId)

    suspend fun saveMistake(mistake: Mistake) = db.mistakeDao().upsert(mistake.copy(updatedAt = nowMillis()))

    suspend fun setMistakeResolved(id: String, resolved: Boolean) =
        db.mistakeDao().setResolved(id, resolved, nowMillis())

    suspend fun deleteMistake(id: String) = db.mistakeDao().deleteById(id)

    // ----------------------------------------------------------------- exams

    fun exams(yearId: String): Flow<List<Exam>> = db.examDao().observeForYear(yearId)

    fun testResults(): Flow<List<TestResult>> = db.examDao().observeAllResults()

    suspend fun saveExam(exam: Exam, results: List<TestResult> = emptyList()) {
        db.withTransaction {
            db.examDao().upsert(exam.copy(updatedAt = nowMillis()))
            if (results.isNotEmpty()) {
                db.examDao().deleteResultsFor(exam.id)
                db.examDao().upsertResults(results.map { it.copy(examId = exam.id) })
            }
        }
    }

    suspend fun deleteExam(id: String) = db.examDao().deleteById(id)

    // ----------------------------------------------------------------- notes

    fun notes(yearId: String): Flow<List<Note>> = db.noteDao().observeForYear(yearId)

    suspend fun saveNote(note: Note) = db.noteDao().upsert(note.copy(updatedAt = nowMillis()))

    suspend fun deleteNote(id: String) = db.noteDao().deleteById(id)

    suspend fun toggleNoteFavourite(note: Note) =
        db.noteDao().upsert(note.copy(favourite = !note.favourite, updatedAt = nowMillis()))
}
