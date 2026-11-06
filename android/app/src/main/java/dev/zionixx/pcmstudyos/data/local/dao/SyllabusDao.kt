package dev.zionixx.pcmstudyos.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.zionixx.pcmstudyos.data.local.entity.AcademicYear
import dev.zionixx.pcmstudyos.data.local.entity.Chapter
import dev.zionixx.pcmstudyos.data.local.entity.Goal
import dev.zionixx.pcmstudyos.data.local.entity.StudyComponent
import dev.zionixx.pcmstudyos.data.local.entity.Subject
import dev.zionixx.pcmstudyos.data.local.entity.Subtopic
import dev.zionixx.pcmstudyos.data.local.entity.Topic
import kotlinx.coroutines.flow.Flow

@Dao
interface AcademicYearDao {
    @Query("SELECT * FROM academic_years ORDER BY startDate DESC")
    fun observeAll(): Flow<List<AcademicYear>>

    @Query("SELECT * FROM academic_years ORDER BY startDate DESC")
    suspend fun getAll(): List<AcademicYear>

    @Query("SELECT * FROM academic_years WHERE id = :id")
    suspend fun byId(id: String): AcademicYear?

    @Query("SELECT * FROM academic_years WHERE isCurrent = 1 LIMIT 1")
    suspend fun current(): AcademicYear?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(year: AcademicYear)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(years: List<AcademicYear>)

    @Query("UPDATE academic_years SET isCurrent = 0")
    suspend fun clearCurrent()

    @Delete
    suspend fun delete(year: AcademicYear)

    @Query("DELETE FROM academic_years WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM academic_years")
    suspend fun count(): Int
}

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects WHERE yearId = :yearId AND archived = 0 ORDER BY sortOrder, name")
    fun observeForYear(yearId: String): Flow<List<Subject>>

    @Query("SELECT * FROM subjects WHERE yearId = :yearId AND archived = 0 ORDER BY sortOrder, name")
    suspend fun forYear(yearId: String): List<Subject>

    @Query("SELECT * FROM subjects WHERE id = :id")
    suspend fun byId(id: String): Subject?

    @Query("SELECT * FROM subjects")
    suspend fun getAll(): List<Subject>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(subject: Subject)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(subjects: List<Subject>)

    @Query("DELETE FROM subjects WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM subjects WHERE yearId = :yearId")
    suspend fun countForYear(yearId: String): Int
}

@Dao
interface ChapterDao {
    @Query("SELECT * FROM chapters WHERE subjectId = :subjectId AND archived = 0 ORDER BY sortOrder, name")
    fun observeForSubject(subjectId: String): Flow<List<Chapter>>

    @Query("SELECT * FROM chapters WHERE subjectId = :subjectId AND archived = 0 ORDER BY sortOrder, name")
    suspend fun forSubject(subjectId: String): List<Chapter>

    @Query("SELECT * FROM chapters")
    suspend fun getAll(): List<Chapter>

    @Query("SELECT * FROM chapters WHERE id = :id")
    suspend fun byId(id: String): Chapter?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(chapter: Chapter)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(chapters: List<Chapter>)

    @Query("DELETE FROM chapters WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM chapters WHERE subjectId = :subjectId")
    suspend fun countForSubject(subjectId: String): Int
}

@Dao
interface TopicDao {
    @Query("SELECT * FROM topics WHERE chapterId = :chapterId AND archived = 0 ORDER BY sortOrder, name")
    fun observeForChapter(chapterId: String): Flow<List<Topic>>

    @Query("SELECT * FROM topics WHERE chapterId = :chapterId AND archived = 0 ORDER BY sortOrder, name")
    suspend fun forChapter(chapterId: String): List<Topic>

    @Query("SELECT * FROM topics")
    suspend fun getAll(): List<Topic>

    @Query("SELECT * FROM topics WHERE id = :id")
    suspend fun byId(id: String): Topic?

    @Query("SELECT * FROM topics WHERE favourite = 1 AND archived = 0 ORDER BY name")
    fun observeFavourites(): Flow<List<Topic>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(topic: Topic)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(topics: List<Topic>)

    @Query("UPDATE topics SET status = :status, updatedAt = :at WHERE id = :id")
    suspend fun setStatus(id: String, status: String, at: Long)

    @Query("DELETE FROM topics WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM topics WHERE chapterId = :chapterId")
    suspend fun countForChapter(chapterId: String): Int

    @Query("SELECT COUNT(*) FROM topics WHERE status = 'completed' AND archived = 0")
    fun observeCompletedCount(): Flow<Int>
}

@Dao
interface SubtopicDao {
    @Query("SELECT * FROM subtopics WHERE topicId = :topicId ORDER BY sortOrder, name")
    fun observeForTopic(topicId: String): Flow<List<Subtopic>>

    @Query("SELECT * FROM subtopics WHERE topicId = :topicId ORDER BY sortOrder, name")
    suspend fun forTopic(topicId: String): List<Subtopic>

    @Query("SELECT * FROM subtopics")
    suspend fun getAll(): List<Subtopic>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(subtopic: Subtopic)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(subtopics: List<Subtopic>)

    @Query("UPDATE subtopics SET completed = :done, updatedAt = :at WHERE id = :id")
    suspend fun setCompleted(id: String, done: Boolean, at: Long)

    @Query("DELETE FROM subtopics WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM subtopics WHERE topicId = :topicId AND completed = 1")
    suspend fun completedCount(topicId: String): Int

    @Query("SELECT COUNT(*) FROM subtopics WHERE topicId = :topicId")
    suspend fun totalCount(topicId: String): Int
}

@Dao
interface ComponentDao {
    @Query("SELECT * FROM study_components WHERE topicId = :topicId")
    fun observeForTopic(topicId: String): Flow<List<StudyComponent>>

    @Query("SELECT * FROM study_components WHERE topicId = :topicId")
    suspend fun forTopic(topicId: String): List<StudyComponent>

    @Query("SELECT * FROM study_components")
    suspend fun getAll(): List<StudyComponent>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(components: List<StudyComponent>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(component: StudyComponent)

    @Query("UPDATE study_components SET done = :done WHERE id = :id")
    suspend fun setDone(id: String, done: Boolean)
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals WHERE yearId = :yearId AND status != 'archived' ORDER BY deadline IS NULL, deadline")
    fun observeForYear(yearId: String): Flow<List<Goal>>

    @Query("SELECT * FROM goals")
    suspend fun getAll(): List<Goal>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(goal: Goal)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(goals: List<Goal>)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteById(id: String)
}
