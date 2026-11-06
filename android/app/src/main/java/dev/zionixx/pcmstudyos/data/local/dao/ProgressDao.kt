package dev.zionixx.pcmstudyos.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import dev.zionixx.pcmstudyos.data.local.entity.Chapter
import dev.zionixx.pcmstudyos.data.local.entity.StudyComponent
import dev.zionixx.pcmstudyos.data.local.entity.Subtopic
import dev.zionixx.pcmstudyos.data.local.entity.Topic

/**
 * Read-only joins used for progress and analytics. One query per table for the
 * whole academic year, so the dashboard never runs N+1 queries on a low-end
 * device.
 */
@Dao
interface ProgressDao {

    @Query(
        """
        SELECT c.* FROM chapters c
        INNER JOIN subjects s ON c.subjectId = s.id
        WHERE s.yearId = :yearId AND c.archived = 0
        """,
    )
    suspend fun chaptersForYear(yearId: String): List<Chapter>

    @Query(
        """
        SELECT t.* FROM topics t
        INNER JOIN chapters c ON t.chapterId = c.id
        INNER JOIN subjects s ON c.subjectId = s.id
        WHERE s.yearId = :yearId AND t.archived = 0
        """,
    )
    suspend fun topicsForYear(yearId: String): List<Topic>

    @Query(
        """
        SELECT st.* FROM subtopics st
        INNER JOIN topics t ON st.topicId = t.id
        INNER JOIN chapters c ON t.chapterId = c.id
        INNER JOIN subjects s ON c.subjectId = s.id
        WHERE s.yearId = :yearId
        """,
    )
    suspend fun subtopicsForYear(yearId: String): List<Subtopic>

    @Query(
        """
        SELECT sc.* FROM study_components sc
        INNER JOIN topics t ON sc.topicId = t.id
        INNER JOIN chapters c ON t.chapterId = c.id
        INNER JOIN subjects s ON c.subjectId = s.id
        WHERE s.yearId = :yearId
        """,
    )
    suspend fun componentsForYear(yearId: String): List<StudyComponent>
}
