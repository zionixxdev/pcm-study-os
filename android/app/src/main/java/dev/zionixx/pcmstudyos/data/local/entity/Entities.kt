package dev.zionixx.pcmstudyos.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.util.UUID

/** All ids are app-generated UUIDs so records stay stable across backup/restore. */
fun newId(): String = UUID.randomUUID().toString()

fun nowMillis(): Long = System.currentTimeMillis()

// ---------------------------------------------------------------- academic year

@Entity(tableName = "academic_years", indices = [Index("isCurrent"), Index("archived")])
@Serializable
data class AcademicYear(
    @PrimaryKey val id: String = newId(),
    val name: String,
    val startDate: String, // ISO yyyy-MM-dd
    val endDate: String,
    val isCurrent: Boolean = false,
    val archived: Boolean = false,
    val notes: String? = null,
    val createdAt: Long = nowMillis(),
    val updatedAt: Long = nowMillis(),
)

// -------------------------------------------------------------------- subject

@Entity(
    tableName = "subjects",
    foreignKeys = [
        ForeignKey(
            entity = AcademicYear::class,
            parentColumns = ["id"],
            childColumns = ["yearId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("yearId"), Index(value = ["yearId", "sortOrder"])],
)
@Serializable
data class Subject(
    @PrimaryKey val id: String = newId(),
    val yearId: String,
    val name: String,
    val description: String? = null,
    val color: String = "#4F8CFF",
    val priority: String = "medium",
    val goal: String? = null,
    val targetDate: String? = null,
    val sortOrder: Int = 0,
    val archived: Boolean = false,
    val createdAt: Long = nowMillis(),
    val updatedAt: Long = nowMillis(),
)

// -------------------------------------------------------------------- chapter

@Entity(
    tableName = "chapters",
    foreignKeys = [
        ForeignKey(
            entity = Subject::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("subjectId"), Index(value = ["subjectId", "sortOrder"])],
)
@Serializable
data class Chapter(
    @PrimaryKey val id: String = newId(),
    val subjectId: String,
    val name: String,
    val description: String? = null,
    val priority: String = "medium",
    val estimatedHours: Double? = null,
    val deadline: String? = null,
    val notes: String? = null,
    val sortOrder: Int = 0,
    val archived: Boolean = false,
    val createdAt: Long = nowMillis(),
    val updatedAt: Long = nowMillis(),
)

// ---------------------------------------------------------------------- topic

@Entity(
    tableName = "topics",
    foreignKeys = [
        ForeignKey(
            entity = Chapter::class,
            parentColumns = ["id"],
            childColumns = ["chapterId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("chapterId"), Index("status"), Index(value = ["chapterId", "sortOrder"])],
)
@Serializable
data class Topic(
    @PrimaryKey val id: String = newId(),
    val chapterId: String,
    val name: String,
    val description: String? = null,
    val notes: String? = null,
    val priority: String = "medium",
    @ColumnInfo(defaultValue = "medium") val difficulty: String = "medium",
    val estimatedMinutes: Int? = null,
    val targetDate: String? = null,
    /** not_started | in_progress | completed | needs_revision */
    val status: String = "not_started",
    val favourite: Boolean = false,
    val sortOrder: Int = 0,
    val archived: Boolean = false,
    val createdAt: Long = nowMillis(),
    val updatedAt: Long = nowMillis(),
)

// ------------------------------------------------------------------- subtopic

@Entity(
    tableName = "subtopics",
    foreignKeys = [
        ForeignKey(
            entity = Topic::class,
            parentColumns = ["id"],
            childColumns = ["topicId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("topicId"), Index(value = ["topicId", "sortOrder"])],
)
@Serializable
data class Subtopic(
    @PrimaryKey val id: String = newId(),
    val topicId: String,
    val name: String,
    val notes: String? = null,
    val difficulty: String = "medium",
    val completed: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Long = nowMillis(),
    val updatedAt: Long = nowMillis(),
)

// ------------------------------------------------------------ study component
// A topic can track theory / formulas / derivations / NCERT / numericals /
// questions / revision / test practice independently.

@Entity(
    tableName = "study_components",
    foreignKeys = [
        ForeignKey(
            entity = Topic::class,
            parentColumns = ["id"],
            childColumns = ["topicId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["topicId", "type"], unique = true)],
)
@Serializable
data class StudyComponent(
    @PrimaryKey val id: String = newId(),
    val topicId: String,
    val type: String,
    val done: Boolean = false,
)

// ----------------------------------------------------------------------- task

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(entity = AcademicYear::class, parentColumns = ["id"], childColumns = ["yearId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("yearId"), Index("dueDate"), Index("status"), Index("priority")],
)
@Serializable
data class StudyTask(
    @PrimaryKey val id: String = newId(),
    val yearId: String,
    val title: String,
    val description: String? = null,
    val subjectId: String? = null,
    val chapterId: String? = null,
    val topicId: String? = null,
    val priority: String = "medium",
    /** pending | in_progress | completed */
    val status: String = "pending",
    val startDate: String? = null,
    val dueDate: String? = null,
    val estimatedMinutes: Int = 30,
    /** none | daily | weekly */
    val recurring: String = "none",
    val sortOrder: Int = 0,
    val completedAt: Long? = null,
    val createdAt: Long = nowMillis(),
    val updatedAt: Long = nowMillis(),
)

// -------------------------------------------------------------- study session

@Entity(
    tableName = "sessions",
    foreignKeys = [
        ForeignKey(entity = AcademicYear::class, parentColumns = ["id"], childColumns = ["yearId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("yearId"), Index("startedAt"), Index("subjectId")],
)
@Serializable
data class StudySession(
    @PrimaryKey val id: String = newId(),
    val yearId: String,
    val subjectId: String? = null,
    val chapterId: String? = null,
    val topicId: String? = null,
    val startedAt: Long,
    val endedAt: Long,
    val minutes: Int,
    /** pomodoro | focus | stopwatch */
    val mode: String = "pomodoro",
    val notes: String? = null,
)

// ------------------------------------------------------------------- revision

@Entity(
    tableName = "revisions",
    foreignKeys = [
        ForeignKey(entity = AcademicYear::class, parentColumns = ["id"], childColumns = ["yearId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("yearId"), Index("dueDate"), Index("status")],
)
@Serializable
data class RevisionItem(
    @PrimaryKey val id: String = newId(),
    val yearId: String,
    val topicId: String? = null,
    val subtopicId: String? = null,
    val label: String,
    val dueDate: String,
    val intervalIndex: Int = 0,
    /** due | upcoming | overdue | completed */
    val status: String = "upcoming",
    val lastRating: String? = null,
    val notes: String? = null,
    val completedAt: Long? = null,
    val createdAt: Long = nowMillis(),
    val updatedAt: Long = nowMillis(),
)

@Entity(
    tableName = "revision_history",
    foreignKeys = [
        ForeignKey(entity = RevisionItem::class, parentColumns = ["id"], childColumns = ["revisionId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("revisionId"), Index("at")],
)
@Serializable
data class RevisionHistory(
    @PrimaryKey val id: String = newId(),
    val revisionId: String,
    val rating: String,
    val notes: String? = null,
    val at: Long = nowMillis(),
)

// -------------------------------------------------------------------- formula

@Entity(
    tableName = "formulas",
    foreignKeys = [
        ForeignKey(entity = AcademicYear::class, parentColumns = ["id"], childColumns = ["yearId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("yearId"), Index("subjectId"), Index("category")],
)
@Serializable
data class FormulaEntry(
    @PrimaryKey val id: String = newId(),
    val yearId: String,
    val subjectId: String,
    val chapterId: String? = null,
    val topicId: String? = null,
    val title: String,
    val category: String = "formula",
    val expression: String,
    val explanation: String? = null,
    val symbols: String? = null,
    val units: String? = null,
    val notes: String? = null,
    val favourite: Boolean = false,
    val createdAt: Long = nowMillis(),
    val updatedAt: Long = nowMillis(),
)

// ------------------------------------------------------------------- practice

@Entity(
    tableName = "practice",
    foreignKeys = [
        ForeignKey(entity = AcademicYear::class, parentColumns = ["id"], childColumns = ["yearId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("yearId"), Index("subjectId"), Index("result"), Index("attemptDate")],
)
@Serializable
data class PracticeRecord(
    @PrimaryKey val id: String = newId(),
    val yearId: String,
    val subjectId: String,
    val chapterId: String? = null,
    val topicId: String? = null,
    val title: String,
    val question: String? = null,
    val source: String? = null,
    val difficulty: String = "medium",
    val attemptDate: String,
    val minutes: Int? = null,
    /** correct | incorrect | partial | skipped */
    val result: String = "correct",
    val notes: String? = null,
    val retry: Boolean = false,
    val createdAt: Long = nowMillis(),
    val updatedAt: Long = nowMillis(),
)

// ------------------------------------------------------------------- mistake

@Entity(
    tableName = "mistakes",
    foreignKeys = [
        ForeignKey(entity = AcademicYear::class, parentColumns = ["id"], childColumns = ["yearId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("yearId"), Index("subjectId"), Index("category"), Index("resolved")],
)
@Serializable
data class Mistake(
    @PrimaryKey val id: String = newId(),
    val yearId: String,
    val subjectId: String,
    val chapterId: String? = null,
    val topicId: String? = null,
    val question: String,
    val wrongApproach: String? = null,
    val correctApproach: String? = null,
    val explanation: String? = null,
    val category: String = "conceptual",
    val date: String,
    val retryDate: String? = null,
    val resolved: Boolean = false,
    val createdAt: Long = nowMillis(),
    val updatedAt: Long = nowMillis(),
)

// ---------------------------------------------------------------------- test

@Entity(
    tableName = "exams",
    foreignKeys = [
        ForeignKey(entity = AcademicYear::class, parentColumns = ["id"], childColumns = ["yearId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("yearId"), Index("date"), Index("type")],
)
@Serializable
data class Exam(
    @PrimaryKey val id: String = newId(),
    val yearId: String,
    val subjectId: String? = null,
    val name: String,
    /** unit | monthly | half_yearly | annual | mock | custom */
    val type: String = "unit",
    val date: String,
    val maxMarks: Double,
    val obtainedMarks: Double? = null,
    val durationMinutes: Int? = null,
    val remarks: String? = null,
    val weakTopics: String? = null,
    val createdAt: Long = nowMillis(),
    val updatedAt: Long = nowMillis(),
)

/** Per-chapter breakdown for a test — the "TestResult" record. */
@Entity(
    tableName = "test_results",
    foreignKeys = [
        ForeignKey(entity = Exam::class, parentColumns = ["id"], childColumns = ["examId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("examId"), Index("chapterId")],
)
@Serializable
data class TestResult(
    @PrimaryKey val id: String = newId(),
    val examId: String,
    val chapterId: String? = null,
    val chapterName: String,
    val maxMarks: Double,
    val obtainedMarks: Double,
)

// ---------------------------------------------------------------------- note

@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(entity = AcademicYear::class, parentColumns = ["id"], childColumns = ["yearId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("yearId"), Index("subjectId"), Index("updatedAt")],
)
@Serializable
data class Note(
    @PrimaryKey val id: String = newId(),
    val yearId: String,
    val subjectId: String? = null,
    val chapterId: String? = null,
    val topicId: String? = null,
    val title: String,
    val body: String = "",
    val category: String? = null,
    val favourite: Boolean = false,
    val createdAt: Long = nowMillis(),
    val updatedAt: Long = nowMillis(),
)

// ---------------------------------------------------------------------- goal

@Entity(tableName = "goals", indices = [Index("yearId"), Index("status")])
@Serializable
data class Goal(
    @PrimaryKey val id: String = newId(),
    val yearId: String,
    val title: String,
    val description: String? = null,
    val kind: String = "custom",
    val target: Double,
    val current: Double = 0.0,
    val deadline: String? = null,
    val status: String = "active",
    val createdAt: Long = nowMillis(),
    val updatedAt: Long = nowMillis(),
)

// --------------------------------------------------------------- study streak
// Written once per day when a session is saved, so streak maths never scans the
// whole session table on a low-end device.

@Entity(tableName = "study_streaks")
@Serializable
data class StudyStreak(
    @PrimaryKey val date: String, // yyyy-MM-dd
    val minutes: Int,
    val sessionCount: Int,
)
