package dev.zionixx.pcmstudyos.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.zionixx.pcmstudyos.data.local.PcmDatabase
import dev.zionixx.pcmstudyos.data.local.entity.AcademicYear
import dev.zionixx.pcmstudyos.data.local.entity.Chapter
import dev.zionixx.pcmstudyos.data.local.entity.StudyComponent
import dev.zionixx.pcmstudyos.data.local.entity.StudyTask
import dev.zionixx.pcmstudyos.data.local.entity.Subject
import dev.zionixx.pcmstudyos.data.local.entity.Subtopic
import dev.zionixx.pcmstudyos.data.local.entity.Topic
import dev.zionixx.pcmstudyos.data.repo.BackupRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DatabaseTest {

    private lateinit var db: PcmDatabase
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, PcmDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun seedTree(): Triple<String, String, String> {
        val year = AcademicYear(name = "Class 11 — 2026–27", startDate = "2026-04-01", endDate = "2027-03-31", isCurrent = true)
        db.academicYearDao().upsert(year)
        val subject = Subject(yearId = year.id, name = "Physics")
        db.subjectDao().upsert(subject)
        val chapter = Chapter(subjectId = subject.id, name = "Kinematics")
        db.chapterDao().upsert(chapter)
        val topic = Topic(chapterId = chapter.id, name = "Speed")
        db.topicDao().upsert(topic)
        return Triple(year.id, subject.id, chapter.id)
    }

    @Test
    fun `creates and retrieves a subject`() = runBlocking {
        val (yearId, _, _) = seedTree()
        val subjects = db.subjectDao().forYear(yearId)
        assertEquals(1, subjects.size)
        assertEquals("Physics", subjects.first().name)
    }

    @Test
    fun `creates chapters topics and subtopics`() = runBlocking {
        val (_, subjectId, chapterId) = seedTree()
        assertEquals(1, db.chapterDao().forSubject(subjectId).size)
        val topic = db.topicDao().forChapter(chapterId).first()
        db.subtopicDao().upsert(Subtopic(topicId = topic.id, name = "Average speed"))
        db.componentDao().upsert(StudyComponent(topicId = topic.id, type = "theory"))
        assertEquals(1, db.subtopicDao().forTopic(topic.id).size)
        assertEquals(1, db.componentDao().forTopic(topic.id).size)
    }

    @Test
    fun `updates task status and records completion time`() = runBlocking {
        val (yearId, _, _) = seedTree()
        val task = StudyTask(yearId = yearId, title = "Read NCERT", dueDate = "2026-06-01")
        db.taskDao().upsert(task)
        db.taskDao().setStatus(task.id, "completed", 1234L, 5678L)
        val updated = db.taskDao().byId(task.id)
        assertEquals("completed", updated?.status)
        assertEquals(1234L, updated?.completedAt)
    }

    @Test
    fun `deleting a subject cascades to children`() = runBlocking {
        val (_, subjectId, chapterId) = seedTree()
        val topic = db.topicDao().forChapter(chapterId).first()
        db.subtopicDao().upsert(Subtopic(topicId = topic.id, name = "Instantaneous speed"))
        db.componentDao().upsert(StudyComponent(topicId = topic.id, type = "numericals"))

        db.subjectDao().deleteById(subjectId)

        assertEquals(0, db.chapterDao().forSubject(subjectId).size)
        assertNull(db.chapterDao().byId(chapterId))
        assertTrue(db.topicDao().forChapter(chapterId).isEmpty())
        assertTrue(db.subtopicDao().forTopic(topic.id).isEmpty())
        assertTrue(db.componentDao().forTopic(topic.id).isEmpty())
    }

    @Test
    fun `data persists after the database is reopened`(): Unit = runBlocking {
        val file = java.io.File(context.cacheDir, "persist-test.db")
        file.delete()
        val first = Room.databaseBuilder(context, PcmDatabase::class.java, file.absolutePath)
            .allowMainThreadQueries()
            .build()
        first.academicYearDao().upsert(AcademicYear(name = "Class 12 — 2027–28", startDate = "2027-04-01", endDate = "2028-03-31"))
        first.close()

        val second = Room.databaseBuilder(context, PcmDatabase::class.java, file.absolutePath)
            .allowMainThreadQueries()
            .build()
        val years = second.academicYearDao().getAll()
        assertEquals(1, years.size)
        assertEquals("Class 12 — 2027–28", years.first().name)
        second.close()
        file.delete()
        Unit
    }

    @Test
    fun `backup and restore round-trips every record`() = runBlocking {
        val (yearId, subjectId, chapterId) = seedTree()
        val topic = db.topicDao().forChapter(chapterId).first()
        db.taskDao().upsert(StudyTask(yearId = yearId, title = "Task A"))
        db.noteDao().upsert(dev.zionixx.pcmstudyos.data.local.entity.Note(yearId = yearId, title = "Note A", body = "body"))
        db.sessionDao().insert(
            dev.zionixx.pcmstudyos.data.local.entity.StudySession(
                yearId = yearId,
                subjectId = subjectId,
                startedAt = 1_000L,
                endedAt = 2_000L,
                minutes = 25,
            ),
        )

        val repository = BackupRepository(db)
        val out = ByteArrayOutputStream()
        val count = repository.writeBackup(out)
        assertTrue(count >= 6)

        // Wipe everything, then restore from the same bytes.
        val maintenance = db.maintenanceDao()
        maintenance.clearTasks()
        maintenance.clearNotes()
        maintenance.clearSessions()
        maintenance.clearComponents()
        maintenance.clearSubtopics()
        maintenance.clearTopics()
        maintenance.clearChapters()
        maintenance.clearSubjects()
        maintenance.clearYears()
        assertEquals(0, db.subjectDao().getAll().size)

        val result = repository.restore(ByteArrayInputStream(out.toByteArray()), replace = false)
        assertTrue(result.ok)
        assertEquals(1, db.subjectDao().getAll().size)
        assertEquals(1, db.taskDao().getAll().size)
        assertEquals(1, db.noteDao().getAll().size)
        assertEquals(1, db.sessionDao().getAll().size)
        assertNotNull(db.topicDao().byId(topic.id))
    }

    @Test
    fun `restore rejects a foreign file and keeps existing data`() = runBlocking {
        seedTree()
        val repository = BackupRepository(db)
        val result = repository.restore(ByteArrayInputStream("""{"app":"something-else"}""".toByteArray()), replace = true)
        assertTrue(!result.ok)
        assertEquals(1, db.subjectDao().getAll().size)
    }

    @Test
    fun `progress query returns only the requested year`() = runBlocking {
        val (yearId, _, _) = seedTree()
        val other = AcademicYear(name = "Other", startDate = "2028-04-01", endDate = "2029-03-31")
        db.academicYearDao().upsert(other)
        val otherSubject = Subject(yearId = other.id, name = "Chemistry")
        db.subjectDao().upsert(otherSubject)

        val topics = db.progressDao().topicsForYear(yearId)
        assertEquals(1, topics.size)
        assertEquals(0, db.progressDao().topicsForYear(other.id).size)
    }
}
