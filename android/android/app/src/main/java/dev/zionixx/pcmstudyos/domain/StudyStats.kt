package dev.zionixx.pcmstudyos.domain

import dev.zionixx.pcmstudyos.data.local.entity.StudySession
import dev.zionixx.pcmstudyos.data.local.entity.StudyStreak
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Study time, consistency and streaks, all computed from stored sessions. */
object StudyStats {

    data class Summary(
        val todayMinutes: Int,
        val weekMinutes: Int,
        val monthMinutes: Int,
        val totalMinutes: Int,
        val averageDailyMinutes: Int,
        val activeDays: Int,
        val currentStreak: Int,
        val longestStreak: Int,
        val last7Days: List<Pair<LocalDate, Int>>,
        val minutesBySubject: Map<String, Int>,
    )

    fun dateOf(millis: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

    fun summarize(
        sessions: List<StudySession>,
        streaks: List<StudyStreak>,
        today: LocalDate = LocalDate.now(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): Summary {
        val byDate = mutableMapOf<LocalDate, Int>()
        val bySubject = mutableMapOf<String, Int>()
        for (session in sessions) {
            val date = dateOf(session.startedAt, zone)
            byDate[date] = (byDate[date] ?: 0) + session.minutes
            session.subjectId?.let { bySubject[it] = (bySubject[it] ?: 0) + session.minutes }
        }

        val total = sessions.sumOf { it.minutes }
        val weekStart = today.minusDays(6)
        val monthStart = today.withDayOfMonth(1)
        val week = byDate.filterKeys { !it.isBefore(weekStart) }.values.sum()
        val month = byDate.filterKeys { !it.isBefore(monthStart) }.values.sum()
        val activeDays = byDate.count { it.value > 0 }

        // Prefer the pre-aggregated streak table when it is available.
        val streakDates = (if (streaks.isNotEmpty()) streaks.map { LocalDate.parse(it.date) to it.minutes } else byDate.map { it.key to it.value })
            .filter { it.second > 0 }
            .sortedBy { it.first }

        var longest = 0
        var run = 0
        var previous: LocalDate? = null
        for ((date, _) in streakDates) {
            run = if (previous != null && previous.plusDays(1) == date) run + 1 else 1
            if (run > longest) longest = run
            previous = date
        }

        var cursor = if (byDate[today] ?: 0 > 0) today else today.minusDays(1)
        var current = 0
        while ((byDate[cursor] ?: 0) > 0) {
            current++
            cursor = cursor.minusDays(1)
        }

        val last7 = (6 downTo 0).map { offset ->
            val day = today.minusDays(offset.toLong())
            day to (byDate[day] ?: 0)
        }

        return Summary(
            todayMinutes = byDate[today] ?: 0,
            weekMinutes = week,
            monthMinutes = month,
            totalMinutes = total,
            averageDailyMinutes = if (activeDays == 0) 0 else total / activeDays,
            activeDays = activeDays,
            currentStreak = current,
            longestStreak = longest,
            last7Days = last7,
            minutesBySubject = bySubject,
        )
    }
}

/** Percentage helpers for tests and analytics. */
object Grading {
    fun percent(obtained: Double?, max: Double): Int {
        if (max <= 0.0 || obtained == null) return 0
        return ((obtained / max) * 100).toInt().coerceIn(0, 100)
    }

    fun accuracy(correct: Int, partial: Int, attempted: Int): Int {
        if (attempted <= 0) return 0
        return (((correct + partial * 0.5) / attempted) * 100).toInt().coerceIn(0, 100)
    }
}
