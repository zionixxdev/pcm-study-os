package dev.zionixx.pcmstudyos.domain

import dev.zionixx.pcmstudyos.data.local.entity.RevisionItem
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class RevisionScheduleTest {

    @Test
    fun `rating moves along the ladder`() {
        assertEquals(1, RevisionSchedule.nextIntervalIndex(0, "good"))
        assertEquals(2, RevisionSchedule.nextIntervalIndex(1, "good"))
        assertEquals(4, RevisionSchedule.nextIntervalIndex(2, "easy"))
        assertEquals(0, RevisionSchedule.nextIntervalIndex(3, "again"))
        assertEquals(2, RevisionSchedule.nextIntervalIndex(2, "hard"))
    }

    @Test
    fun `never exceeds the last interval`() {
        assertEquals(4, RevisionSchedule.nextIntervalIndex(4, "easy"))
        assertEquals(4, RevisionSchedule.nextIntervalIndex(4, "good"))
    }

    @Test
    fun `next date uses the configured intervals`() {
        val intervals = listOf(1, 3, 7, 14, 30)
        val from = LocalDate.of(2026, 1, 1)
        assertEquals(LocalDate.of(2026, 1, 2), RevisionSchedule.nextDate(from, intervals, 0))
        assertEquals(LocalDate.of(2026, 1, 8), RevisionSchedule.nextDate(from, intervals, 2))
        assertEquals(LocalDate.of(2026, 1, 31), RevisionSchedule.nextDate(from, intervals, 4))
    }

    @Test
    fun `falls back to default intervals when empty`() {
        assertEquals(LocalDate.of(2026, 1, 4), RevisionSchedule.nextDate(LocalDate.of(2026, 1, 1), emptyList(), 1))
    }

    @Test
    fun `status reflects due overdue upcoming and completed`() {
        val item = RevisionItem(yearId = "y", label = "x", dueDate = "2026-01-05")
        val today = LocalDate.of(2026, 1, 5)
        assertEquals("due", RevisionSchedule.statusOf(item, today))
        assertEquals("overdue", RevisionSchedule.statusOf(item, today.plusDays(1)))
        assertEquals("upcoming", RevisionSchedule.statusOf(item, today.minusDays(1)))
        assertEquals("completed", RevisionSchedule.statusOf(item.copy(status = "completed"), today.plusDays(9)))
    }

    @Test
    fun `interval lookup clamps out-of-range indices`() {
        assertEquals(1, RevisionSchedule.intervalFor(listOf(1, 3, 7), 0))
        assertEquals(7, RevisionSchedule.intervalFor(listOf(1, 3, 7), 99))
    }
}
