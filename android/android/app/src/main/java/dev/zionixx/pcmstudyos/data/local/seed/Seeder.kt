package dev.zionixx.pcmstudyos.data.local.seed

import androidx.room.withTransaction
import dev.zionixx.pcmstudyos.data.local.PcmDatabase
import dev.zionixx.pcmstudyos.data.local.entity.AcademicYear
import dev.zionixx.pcmstudyos.data.local.entity.Chapter
import dev.zionixx.pcmstudyos.data.local.entity.StudyComponent
import dev.zionixx.pcmstudyos.data.local.entity.Subject
import dev.zionixx.pcmstudyos.data.local.entity.Subtopic
import dev.zionixx.pcmstudyos.data.local.entity.Topic
import java.time.LocalDate

/**
 * Installs the editable CBSE seed syllabus. Written in a single transaction so a
 * first launch either gets the whole syllabus or none of it.
 */
class Seeder(private val db: PcmDatabase) {

    suspend fun createYearWithSyllabus(name: String = defaultYearName()): String {
        val yearId = seedYearId()
        db.withTransaction {
            db.academicYearDao().clearCurrent()
            db.academicYearDao().upsert(
                AcademicYear(
                    id = yearId,
                    name = name,
                    startDate = defaultStart(),
                    endDate = defaultEnd(),
                    isCurrent = true,
                ),
            )
            installSyllabus(yearId)
        }
        return yearId
    }

    /** Adds the seed subjects to an existing year (used by "Add CBSE syllabus"). */
    suspend fun installSyllabus(yearId: String) {
        db.withTransaction {
            var subjectOrder = db.subjectDao().countForYear(yearId)
            for (seed in SeedSyllabus.subjects) {
                val subjectId = dev.zionixx.pcmstudyos.data.local.entity.newId()
                db.subjectDao().upsert(
                    Subject(
                        id = subjectId,
                        yearId = yearId,
                        name = seed.name,
                        description = seed.description,
                        color = seed.color,
                        priority = "high",
                        sortOrder = subjectOrder++,
                    ),
                )
                seed.chapters.forEachIndexed { chapterIndex, chapterSeed ->
                    val chapterId = dev.zionixx.pcmstudyos.data.local.entity.newId()
                    db.chapterDao().upsert(
                        Chapter(
                            id = chapterId,
                            subjectId = subjectId,
                            name = chapterSeed.name,
                            priority = "medium",
                            sortOrder = chapterIndex,
                        ),
                    )
                    val topics = mutableListOf<Topic>()
                    val subtopics = mutableListOf<Subtopic>()
                    val components = mutableListOf<StudyComponent>()
                    chapterSeed.topics.forEachIndexed { topicIndex, topicSeed ->
                        val topicId = dev.zionixx.pcmstudyos.data.local.entity.newId()
                        topics += Topic(
                            id = topicId,
                            chapterId = chapterId,
                            name = topicSeed.name,
                            priority = "medium",
                            status = "not_started",
                            sortOrder = topicIndex,
                        )
                        topicSeed.subtopics.forEachIndexed { subIndex, sub ->
                            subtopics += Subtopic(
                                id = dev.zionixx.pcmstudyos.data.local.entity.newId(),
                                topicId = topicId,
                                name = sub,
                                sortOrder = subIndex,
                            )
                        }
                        seed.components.forEach { type ->
                            components += StudyComponent(
                                id = dev.zionixx.pcmstudyos.data.local.entity.newId(),
                                topicId = topicId,
                                type = type,
                                done = false,
                            )
                        }
                    }
                    db.topicDao().upsertAll(topics)
                    if (subtopics.isNotEmpty()) db.subtopicDao().upsertAll(subtopics)
                    if (components.isNotEmpty()) db.componentDao().upsertAll(components)
                }
            }
        }
    }

    companion object {
        fun seedYearId(): String = dev.zionixx.pcmstudyos.data.local.entity.newId()

        fun defaultYearName(today: LocalDate = LocalDate.now()): String {
            // Indian academic year runs April -> March.
            val startYear = if (today.monthValue >= 4) today.year else today.year - 1
            val endShort = (startYear + 1) % 100
            return "Class 11 — $startYear–${endShort.toString().padStart(2, '0')}"
        }

        fun defaultStart(today: LocalDate = LocalDate.now()): String {
            val startYear = if (today.monthValue >= 4) today.year else today.year - 1
            return "$startYear-04-01"
        }

        fun defaultEnd(today: LocalDate = LocalDate.now()): String {
            val startYear = if (today.monthValue >= 4) today.year else today.year - 1
            return "${startYear + 1}-03-31"
        }
    }
}
