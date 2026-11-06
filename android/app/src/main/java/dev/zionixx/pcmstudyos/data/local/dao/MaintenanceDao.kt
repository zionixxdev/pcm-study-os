package dev.zionixx.pcmstudyos.data.local.dao

import androidx.room.Dao
import androidx.room.Query

/**
 * Bulk deletes used by "replace" restore and by the danger-zone reset.
 * Ordered so child rows go before parents (foreign keys are enforced).
 */
@Dao
interface MaintenanceDao {
    @Query("DELETE FROM test_results") suspend fun clearTestResults()
    @Query("DELETE FROM revision_history") suspend fun clearRevisionHistory()
    @Query("DELETE FROM revisions") suspend fun clearRevisions()
    @Query("DELETE FROM sessions") suspend fun clearSessions()
    @Query("DELETE FROM study_streaks") suspend fun clearStreaks()
    @Query("DELETE FROM tasks") suspend fun clearTasks()
    @Query("DELETE FROM formulas") suspend fun clearFormulas()
    @Query("DELETE FROM practice") suspend fun clearPractice()
    @Query("DELETE FROM mistakes") suspend fun clearMistakes()
    @Query("DELETE FROM exams") suspend fun clearExams()
    @Query("DELETE FROM notes") suspend fun clearNotes()
    @Query("DELETE FROM goals") suspend fun clearGoals()
    @Query("DELETE FROM study_components") suspend fun clearComponents()
    @Query("DELETE FROM subtopics") suspend fun clearSubtopics()
    @Query("DELETE FROM topics") suspend fun clearTopics()
    @Query("DELETE FROM chapters") suspend fun clearChapters()
    @Query("DELETE FROM subjects") suspend fun clearSubjects()
    @Query("DELETE FROM academic_years") suspend fun clearYears()
}
