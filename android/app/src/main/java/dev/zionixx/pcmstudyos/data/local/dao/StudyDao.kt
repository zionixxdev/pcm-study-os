package dev.zionixx.pcmstudyos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.zionixx.pcmstudyos.data.local.entity.Exam
import dev.zionixx.pcmstudyos.data.local.entity.FormulaEntry
import dev.zionixx.pcmstudyos.data.local.entity.Mistake
import dev.zionixx.pcmstudyos.data.local.entity.Note
import dev.zionixx.pcmstudyos.data.local.entity.PracticeRecord
import dev.zionixx.pcmstudyos.data.local.entity.RevisionHistory
import dev.zionixx.pcmstudyos.data.local.entity.RevisionItem
import dev.zionixx.pcmstudyos.data.local.entity.StudySession
import dev.zionixx.pcmstudyos.data.local.entity.StudyStreak
import dev.zionixx.pcmstudyos.data.local.entity.StudyTask
import dev.zionixx.pcmstudyos.data.local.entity.TestResult
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE yearId = :yearId ORDER BY dueDate IS NULL, dueDate, sortOrder")
    fun observeForYear(yearId: String): Flow<List<StudyTask>>

    @Query("SELECT * FROM tasks WHERE yearId = :yearId ORDER BY dueDate IS NULL, dueDate, sortOrder")
    suspend fun forYear(yearId: String): List<StudyTask>

    @Query("SELECT * FROM tasks")
    suspend fun getAll(): List<StudyTask>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun byId(id: String): StudyTask?

    @Query("SELECT * FROM tasks WHERE status != 'completed' AND dueDate IS NOT NULL AND dueDate < :today")
    fun observeOverdue(today: String): Flow<List<StudyTask>>

    @Query("SELECT * FROM tasks WHERE dueDate = :date ORDER BY priority, sortOrder")
    fun observeForDate(date: String): Flow<List<StudyTask>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: StudyTask)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(tasks: List<StudyTask>)

    @Query("UPDATE tasks SET status = :status, completedAt = :completedAt, updatedAt = :at WHERE id = :id")
    suspend fun setStatus(id: String, status: String, completedAt: Long?, at: Long)

    @Query("UPDATE tasks SET dueDate = :dueDate, startDate = :dueDate, updatedAt = :at WHERE id = :id")
    suspend fun reschedule(id: String, dueDate: String, at: Long)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM tasks WHERE status != 'completed' AND yearId = :yearId")
    fun observePendingCount(yearId: String): Flow<Int>
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions WHERE yearId = :yearId ORDER BY startedAt DESC")
    fun observeForYear(yearId: String): Flow<List<StudySession>>

    @Query("SELECT * FROM sessions ORDER BY startedAt DESC")
    suspend fun getAll(): List<StudySession>

    @Query("SELECT * FROM sessions WHERE startedAt >= :from AND startedAt <= :to ORDER BY startedAt DESC")
    suspend fun between(from: Long, to: Long): List<StudySession>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: StudySession)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sessions: List<StudySession>)

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COALESCE(SUM(minutes), 0) FROM sessions WHERE startedAt >= :from AND startedAt <= :to")
    suspend fun minutesBetween(from: Long, to: Long): Int
}

@Dao
interface RevisionDao {
    @Query("SELECT * FROM revisions WHERE yearId = :yearId ORDER BY dueDate")
    fun observeForYear(yearId: String): Flow<List<RevisionItem>>

    @Query("SELECT * FROM revisions WHERE yearId = :yearId ORDER BY dueDate")
    suspend fun forYear(yearId: String): List<RevisionItem>

    @Query("SELECT * FROM revisions")
    suspend fun getAll(): List<RevisionItem>

    @Query("SELECT * FROM revisions WHERE id = :id")
    suspend fun byId(id: String): RevisionItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: RevisionItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<RevisionItem>)

    @Query("DELETE FROM revisions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM revision_history ORDER BY at DESC")
    fun observeHistory(): Flow<List<RevisionHistory>>

    @Query("SELECT * FROM revision_history")
    suspend fun getAllHistory(): List<RevisionHistory>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: RevisionHistory)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoryAll(history: List<RevisionHistory>)
}

@Dao
interface FormulaDao {
    @Query("SELECT * FROM formulas WHERE yearId = :yearId ORDER BY title")
    fun observeForYear(yearId: String): Flow<List<FormulaEntry>>

    @Query("SELECT * FROM formulas")
    suspend fun getAll(): List<FormulaEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: FormulaEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entries: List<FormulaEntry>)

    @Query("DELETE FROM formulas WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface PracticeDao {
    @Query("SELECT * FROM practice WHERE yearId = :yearId ORDER BY attemptDate DESC")
    fun observeForYear(yearId: String): Flow<List<PracticeRecord>>

    @Query("SELECT * FROM practice")
    suspend fun getAll(): List<PracticeRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: PracticeRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(records: List<PracticeRecord>)

    @Query("DELETE FROM practice WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface MistakeDao {
    @Query("SELECT * FROM mistakes WHERE yearId = :yearId ORDER BY date DESC")
    fun observeForYear(yearId: String): Flow<List<Mistake>>

    @Query("SELECT * FROM mistakes")
    suspend fun getAll(): List<Mistake>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(mistake: Mistake)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(mistakes: List<Mistake>)

    @Query("UPDATE mistakes SET resolved = :resolved, updatedAt = :at WHERE id = :id")
    suspend fun setResolved(id: String, resolved: Boolean, at: Long)

    @Query("DELETE FROM mistakes WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface ExamDao {
    @Query("SELECT * FROM exams WHERE yearId = :yearId ORDER BY date DESC")
    fun observeForYear(yearId: String): Flow<List<Exam>>

    @Query("SELECT * FROM exams")
    suspend fun getAll(): List<Exam>

    @Query("SELECT * FROM exams WHERE id = :id")
    suspend fun byId(id: String): Exam?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(exam: Exam)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(exams: List<Exam>)

    @Query("DELETE FROM exams WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM test_results")
    fun observeAllResults(): Flow<List<TestResult>>

    @Query("SELECT * FROM test_results")
    suspend fun getAllResults(): List<TestResult>

    @Query("SELECT * FROM test_results WHERE examId = :examId")
    suspend fun resultsFor(examId: String): List<TestResult>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertResults(results: List<TestResult>)

    @Query("DELETE FROM test_results WHERE examId = :examId")
    suspend fun deleteResultsFor(examId: String)
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE yearId = :yearId ORDER BY updatedAt DESC")
    fun observeForYear(yearId: String): Flow<List<Note>>

    @Query("SELECT * FROM notes")
    suspend fun getAll(): List<Note>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun byId(id: String): Note?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: Note)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(notes: List<Note>)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface StreakDao {
    @Query("SELECT * FROM study_streaks ORDER BY date DESC")
    fun observeAll(): Flow<List<StudyStreak>>

    @Query("SELECT * FROM study_streaks ORDER BY date DESC")
    suspend fun getAll(): List<StudyStreak>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(streak: StudyStreak)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(streaks: List<StudyStreak>)

    @Query("SELECT * FROM study_streaks WHERE date = :date")
    suspend fun forDate(date: String): StudyStreak?
}
