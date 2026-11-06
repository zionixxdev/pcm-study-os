package dev.zionixx.pcmstudyos.domain

import dev.zionixx.pcmstudyos.data.local.entity.StudyTask
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CatchupPlannerTest {

    private fun task(
        id: String,
        due: String? = null,
        priority: String = "medium",
        minutes: Int = 60,
        status: String = "pending",
    ) = StudyTask(
        id = id,
        yearId = "y1",
        title = id,
        priority = priority,
        status = status,
        dueDate = due,
        estimatedMinutes = minutes,
    )

    @Test
    fun `fills a day up to capacity then spills over`() {
        val tasks = listOf(
            task("a", minutes = 120, priority = "high"),
            task("b", minutes = 120, priority = "high"),
            task("c", minutes = 120),
        )
        val plan = CatchupPlanner.plan(LocalDate.of(2026, 1, 5), tasks, 240, emptySet())
        assertEquals(3, plan.suggestions.size)
        assertEquals(LocalDate.of(2026, 1, 5), plan.suggestions[0].date)
        assertEquals(LocalDate.of(2026, 1, 5), plan.suggestions[1].date)
        assertEquals(LocalDate.of(2026, 1, 6), plan.suggestions[2].date)
        assertFalse(plan.suggestions.any { it.atRisk })
    }

    @Test
    fun `never exceeds the daily capacity`() {
        val tasks = (0 until 6).map { task("t$it", minutes = 60) }
        val plan = CatchupPlanner.plan(LocalDate.of(2026, 1, 5), tasks, 120, emptySet())
        assertTrue(plan.loadByDate().values.all { it <= 120 })
    }

    @Test
    fun `skips days that are not study days`() {
        // 2026-01-05 is a Monday; study only on Saturday (ISO day 6).
        val plan = CatchupPlanner.plan(LocalDate.of(2026, 1, 5), listOf(task("a")), 240, setOf(6))
        assertEquals(LocalDate.of(2026, 1, 10), plan.suggestions.first().date)
    }

    @Test
    fun `sorts by deadline then priority`() {
        val tasks = listOf(
            task("later", due = "2026-02-01", priority = "high", minutes = 30),
            task("soon-low", due = "2026-01-06", priority = "low", minutes = 30),
            task("soon-high", due = "2026-01-06", priority = "high", minutes = 30),
        )
        val plan = CatchupPlanner.plan(LocalDate.of(2026, 1, 5), tasks, 240, emptySet())
        assertEquals(listOf("soon-high", "soon-low", "later"), plan.suggestions.map { it.taskId })
    }

    @Test
    fun `flags work that cannot fit before its deadline`() {
        val plan = CatchupPlanner.plan(
            LocalDate.of(2026, 1, 5),
            listOf(task("big", due = "2026-01-05", minutes = 300)),
            120,
            emptySet(),
        )
        assertTrue(plan.suggestions.single().atRisk)
    }

    @Test
    fun `ignores completed tasks and is deterministic`() {
        val tasks = listOf(task("done", status = "completed"), task("open"))
        val first = CatchupPlanner.plan(LocalDate.of(2026, 1, 5), tasks, 120, emptySet())
        val second = CatchupPlanner.plan(LocalDate.of(2026, 1, 5), tasks, 120, emptySet())
        assertEquals(listOf("open"), first.suggestions.map { it.taskId })
        assertEquals(first, second)
    }

    @Test
    fun `overdue finds unfinished past-due tasks`() {
        val tasks = listOf(
            task("late", due = "2026-01-01"),
            task("done", due = "2026-01-01", status = "completed"),
            task("future", due = "2026-03-01"),
        )
        assertEquals(listOf("late"), CatchupPlanner.overdue(tasks, LocalDate.of(2026, 1, 5)).map { it.id })
    }
}
