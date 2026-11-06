package dev.zionixx.pcmstudyos.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.zionixx.pcmstudyos.data.local.dao.AcademicYearDao
import dev.zionixx.pcmstudyos.data.local.dao.ChapterDao
import dev.zionixx.pcmstudyos.data.local.dao.ComponentDao
import dev.zionixx.pcmstudyos.data.local.dao.ExamDao
import dev.zionixx.pcmstudyos.data.local.dao.FormulaDao
import dev.zionixx.pcmstudyos.data.local.dao.GoalDao
import dev.zionixx.pcmstudyos.data.local.dao.MistakeDao
import dev.zionixx.pcmstudyos.data.local.dao.NoteDao
import dev.zionixx.pcmstudyos.data.local.dao.PracticeDao
import dev.zionixx.pcmstudyos.data.local.dao.MaintenanceDao
import dev.zionixx.pcmstudyos.data.local.dao.ProgressDao
import dev.zionixx.pcmstudyos.data.local.dao.RevisionDao
import dev.zionixx.pcmstudyos.data.local.dao.SessionDao
import dev.zionixx.pcmstudyos.data.local.dao.StreakDao
import dev.zionixx.pcmstudyos.data.local.dao.SubjectDao
import dev.zionixx.pcmstudyos.data.local.dao.SubtopicDao
import dev.zionixx.pcmstudyos.data.local.dao.TaskDao
import dev.zionixx.pcmstudyos.data.local.dao.TopicDao
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

@Database(
    entities = [
        AcademicYear::class,
        Subject::class,
        Chapter::class,
        Topic::class,
        Subtopic::class,
        StudyComponent::class,
        StudyTask::class,
        StudySession::class,
        RevisionItem::class,
        RevisionHistory::class,
        FormulaEntry::class,
        PracticeRecord::class,
        Mistake::class,
        Exam::class,
        TestResult::class,
        Note::class,
        Goal::class,
        StudyStreak::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class PcmDatabase : RoomDatabase() {

    abstract fun academicYearDao(): AcademicYearDao
    abstract fun subjectDao(): SubjectDao
    abstract fun chapterDao(): ChapterDao
    abstract fun topicDao(): TopicDao
    abstract fun subtopicDao(): SubtopicDao
    abstract fun componentDao(): ComponentDao
    abstract fun taskDao(): TaskDao
    abstract fun sessionDao(): SessionDao
    abstract fun revisionDao(): RevisionDao
    abstract fun formulaDao(): FormulaDao
    abstract fun practiceDao(): PracticeDao
    abstract fun mistakeDao(): MistakeDao
    abstract fun examDao(): ExamDao
    abstract fun noteDao(): NoteDao
    abstract fun goalDao(): GoalDao
    abstract fun streakDao(): StreakDao
    abstract fun progressDao(): ProgressDao
    abstract fun maintenanceDao(): MaintenanceDao

    companion object {
        const val NAME = "pcm-study-os.db"

        /**
         * v1 -> v2
         *  - topics gained a `difficulty` column (used by the revision engine)
         *  - new `test_results` table for chapter-wise test breakdowns
         *
         * Existing rows are preserved: the column is added with a default and no
         * table is recreated or dropped.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `topics` ADD COLUMN `difficulty` TEXT NOT NULL DEFAULT 'medium'")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `test_results` (
                        `id` TEXT NOT NULL,
                        `examId` TEXT NOT NULL,
                        `chapterId` TEXT,
                        `chapterName` TEXT NOT NULL,
                        `maxMarks` REAL NOT NULL,
                        `obtainedMarks` REAL NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`examId`) REFERENCES `exams`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_test_results_examId` ON `test_results` (`examId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_test_results_chapterId` ON `test_results` (`chapterId`)")
            }
        }

        fun build(context: Context): PcmDatabase =
            Room.databaseBuilder(context.applicationContext, PcmDatabase::class.java, NAME)
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
