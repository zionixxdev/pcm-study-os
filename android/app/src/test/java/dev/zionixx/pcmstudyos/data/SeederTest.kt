package dev.zionixx.pcmstudyos.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.zionixx.pcmstudyos.data.local.PcmDatabase
import dev.zionixx.pcmstudyos.data.local.seed.SeedSyllabus
import dev.zionixx.pcmstudyos.data.local.seed.Seeder
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * First launch must produce a usable syllabus. This exercises the same Seeder the
 * app runs when the database is empty.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SeederTest {

    private lateinit var db: PcmDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), PcmDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `seed data has the expected shape`() {
        assertEquals(5, SeedSyllabus.subjects.size)
        assertEquals(
            listOf("Physics", "Chemistry", "Mathematics", "English Core", "Computer Science"),
            SeedSyllabus.subjects.map { it.name },
        )
        assertTrue(SeedSyllabus.subjects.all { it.chapters.isNotEmpty() })
        assertTrue(SeedSyllabus.subjects.flatMap { it.chapters }.all { it.topics.isNotEmpty() })
    }

    @Test
    fun `first launch installs a full syllabus`(): Unit = runBlocking {
        val yearId = Seeder(db).createYearWithSyllabus()

        val year = db.academicYearDao().byId(yearId)
        assertEquals("first launch creates the academic year", true, year?.isCurrent)

        val subjects = db.subjectDao().forYear(yearId)
        assertEquals(5, subjects.size)

        val chapters = db.progressDao().chaptersForYear(yearId)
        assertEquals(47, chapters.size)

        val topics = db.progressDao().topicsForYear(yearId)
        assertEquals(172, topics.size)
        assertTrue("every topic starts untouched", topics.all { it.status == "not_started" })

        val subtopics = db.progressDao().subtopicsForYear(yearId)
        assertEquals(488, subtopics.size)
        assertTrue(subtopics.none { it.completed })

        val components = db.progressDao().componentsForYear(yearId)
        assertTrue("study components are created per topic", components.size > topics.size)
        assertTrue(components.none { it.done })
    }

    @Test
    fun `installing twice appends rather than corrupting`(): Unit = runBlocking {
        val seeder = Seeder(db)
        val yearId = seeder.createYearWithSyllabus()
        seeder.installSyllabus(yearId)
        val subjects = db.subjectDao().forYear(yearId)
        assertEquals(10, subjects.size)
        // The second install appends to the same year rather than creating another.
        assertEquals(1, db.academicYearDao().getAll().size)
        Unit
    }
}
