package dev.zionixx.pcmstudyos.domain

import dev.zionixx.pcmstudyos.data.local.entity.RevisionItem
import java.time.LocalDate

/**
 * Spaced repetition. The default ladder is 1 / 3 / 7 / 14 / 30 days; the user can
 * change it in Settings. A recall rating moves the item along the ladder:
 * again → back to the first step, hard → stay, good → +1, easy → +2.
 */
object RevisionSchedule {

    val DEFAULT_INTERVALS = listOf(1, 3, 7, 14, 30)

    const val RATING_AGAIN = "again"
    const val RATING_HARD = "hard"
    const val RATING_GOOD = "good"
    const val RATING_EASY = "easy"

    fun nextIntervalIndex(current: Int, rating: String, intervalCount: Int = DEFAULT_INTERVALS.size): Int {
        val last = (intervalCount - 1).coerceAtLeast(0)
        return when (rating) {
            RATING_AGAIN -> 0
            RATING_HARD -> current.coerceIn(0, last)
            RATING_GOOD -> (current + 1).coerceAtMost(last)
            RATING_EASY -> (current + 2).coerceAtMost(last)
            else -> current.coerceIn(0, last)
        }
    }

    fun nextDate(from: LocalDate, intervals: List<Int>, intervalIndex: Int): LocalDate {
        val list = intervals.ifEmpty { DEFAULT_INTERVALS }
        val idx = intervalIndex.coerceIn(0, list.size - 1)
        return from.plusDays(list[idx].toLong())
    }

    fun statusOf(item: RevisionItem, today: LocalDate): String {
        if (item.status == "completed" || item.completedAt != null) return "completed"
        val due = runCatching { LocalDate.parse(item.dueDate) }.getOrNull() ?: return item.status
        return when {
            due.isBefore(today) -> "overdue"
            due.isEqual(today) -> "due"
            else -> "upcoming"
        }
    }

    fun intervalFor(intervals: List<Int>, intervalIndex: Int): Int {
        val list = intervals.ifEmpty { DEFAULT_INTERVALS }
        return list[intervalIndex.coerceIn(0, list.size - 1)]
    }
}
