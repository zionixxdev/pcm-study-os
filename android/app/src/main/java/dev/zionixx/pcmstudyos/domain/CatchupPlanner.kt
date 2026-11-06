package dev.zionixx.pcmstudyos.domain

import dev.zionixx.pcmstudyos.data.local.entity.StudyTask
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Deterministic catch-up planner — no AI, no randomness. The same input always
 * produces the same plan.
 *
 * It walks forward from today, only over the user's study days, and fills each
 * day up to the configured capacity. A task that cannot fit before its deadline
 * is still scheduled (earliest possible day) but flagged `atRisk`, so the UI can
 * warn instead of silently producing an impossible plan.
 */
object CatchupPlanner {

    data class Suggestion(
        val taskId: String,
        val title: String,
        val date: LocalDate,
        val minutes: Int,
        val atRisk: Boolean,
        val reason: String,
    )

    data class Plan(val suggestions: List<Suggestion>, val capacity: Int) {
        val totalMinutes: Int get() = suggestions.sumOf { it.minutes }
        fun loadByDate(): Map<LocalDate, Int> =
            suggestions.groupBy { it.date }.mapValues { entry -> entry.value.sumOf { it.minutes } }
    }

    private val priorityWeight = mapOf("high" to 0, "medium" to 1, "low" to 2)

    fun plan(
        today: LocalDate,
        tasks: List<StudyTask>,
        dailyCapacityMinutes: Int,
        studyDays: Set<Int>, // ISO day-of-week 1=Mon .. 7=Sun
        horizonDays: Int = 14,
    ): Plan {
        val capacity = dailyCapacityMinutes.coerceAtLeast(15)

        val candidates = tasks
            .filter { it.status != "completed" }
            .sortedWith(
                compareBy(
                    { it.dueDate ?: "9999-12-31" },
                    { priorityWeight[it.priority] ?: 1 },
                    { it.sortOrder },
                    { it.createdAt },
                ),
            )

        val days = mutableListOf<LocalDate>()
        for (i in 0 until horizonDays) {
            val day = today.plusDays(i.toLong())
            if (studyDays.isEmpty() || studyDays.contains(day.dayOfWeek.value)) days += day
        }
        if (days.isEmpty()) days += today

        val load = days.associateWith { 0 }.toMutableMap()
        val suggestions = mutableListOf<Suggestion>()

        for (task in candidates) {
            val minutes = (task.estimatedMinutes.takeIf { it > 0 } ?: 30).coerceAtLeast(5)
            val deadline = task.dueDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

            var chosen: LocalDate? = null
            for (day in days) {
                val used = load[day] ?: 0
                if (used + minutes > capacity) continue
                if (deadline != null && day.isAfter(deadline)) continue
                chosen = day
                break
            }

            var reason = "Fits inside your daily capacity."
            var atRisk = false

            if (chosen == null) {
                val beforeDeadline = if (deadline != null) days.filter { !it.isAfter(deadline) } else days
                chosen = beforeDeadline.firstOrNull() ?: days.first()
                reason = if (deadline != null) {
                    "Daily capacity is full — this may miss its deadline."
                } else {
                    "Daily capacity is full — scheduled as overflow."
                }
                atRisk = true
            } else if (deadline != null) {
                val slack = ChronoUnit.DAYS.between(chosen, deadline)
                if (slack <= 1) reason = "Scheduled close to its deadline."
            }

            load[chosen] = (load[chosen] ?: 0) + minutes
            suggestions += Suggestion(task.id, task.title, chosen, minutes, atRisk, reason)
        }

        return Plan(suggestions, capacity)
    }

    fun overdue(tasks: List<StudyTask>, today: LocalDate): List<StudyTask> =
        tasks.filter { it.status != "completed" && it.dueDate != null && it.dueDate < today.toString() }

    val isoDate: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
}
