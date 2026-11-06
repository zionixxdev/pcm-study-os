package dev.zionixx.pcmstudyos.domain

import dev.zionixx.pcmstudyos.data.local.entity.StudySession
import dev.zionixx.pcmstudyos.data.local.entity.StudyStreak
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class StudyStatsTest {

    private val zone: ZoneId = ZoneOffset.UTC

    private fun session(daysAgo: Long, minutes: Int, subject: String? = "s1", today: LocalDate): StudySession {
        val date = today.minusDays(daysAgo)
        val millis = date.atStartOfDay(zone).toInstant().toEpochMilli() + 3_600_000L
        return StudySession(
            yearId = "y1",
            subjectId = subject,
            startedAt = millis,
            endedAt = millis + minutes * 60_000L,
            minutes = minutes,
        )
    }

    @Test
    fun `summarises today week month and total`() {
        val today = LocalDate.of(2026, 3, 15)
        val sessions = listOf(
            session(0, 60, today = today),
            session(1, 30, today = today),
            session(10, 45, today = today),
            session(40, 120, today = today),
        )
        val summary = StudyStats.summarize(sessions, emptyList(), today, zone)
        assertEquals(60, summary.todayMinutes)
        assertEquals(90, summary.weekMinutes)
        assertEquals(135, summary.monthMinutes)
        assertEquals(255, summary.totalMinutes)
        assertEquals(4, summary.activeDays)
        assertEquals(63, summary.averageDailyMinutes)
    }

    @Test
    fun `computes current and longest streaks`() {
        val today = LocalDate.of(2026, 3, 15)
        val sessions = listOf(
            session(0, 10, today = today),
            session(1, 10, today = today),
            session(2, 10, today = today),
            session(5, 10, today = today),
            session(6, 10, today = today),
        )
        val summary = StudyStats.summarize(sessions, emptyList(), today, zone)
        assertEquals(3, summary.currentStreak)
        assertEquals(3, summary.longestStreak)
    }

    @Test
    fun `prefers the streak table when present`() {
        val today = LocalDate.of(2026, 3, 15)
        val streaks = listOf(
            StudyStreak(today.minusDays(1).toString(), 10, 1),
            StudyStreak(today.minusDays(2).toString(), 10, 1),
            StudyStreak(today.minusDays(3).toString(), 10, 1),
            StudyStreak(today.minusDays(4).toString(), 10, 1),
        )
        val summary = StudyStats.summarize(emptyList(), streaks, today, zone)
        assertEquals(4, summary.longestStreak)
    }

    @Test
    fun `groups minutes by subject`() {
        val today = LocalDate.of(2026, 3, 15)
        val sessions = listOf(
            session(0, 60, "physics", today),
            session(0, 30, "chemistry", today),
            session(1, 20, "physics", today),
        )
        val summary = StudyStats.summarize(sessions, emptyList(), today, zone)
        assertEquals(80, summary.minutesBySubject["physics"])
        assertEquals(30, summary.minutesBySubject["chemistry"])
    }

    @Test
    fun `empty input is safe`() {
        val summary = StudyStats.summarize(emptyList(), emptyList(), LocalDate.of(2026, 3, 15), zone)
        assertEquals(0, summary.totalMinutes)
        assertEquals(0, summary.currentStreak)
        assertEquals(0, summary.longestStreak)
        assertEquals(7, summary.last7Days.size)
    }
}

class GradingTest {

    @Test
    fun `percentage of max marks`() {
        assertEquals(75, Grading.percent(75.0, 100.0))
        assertEquals(0, Grading.percent(null, 100.0))
        assertEquals(0, Grading.percent(50.0, 0.0))
        assertEquals(100, Grading.percent(50.0, 50.0))
    }

    @Test
    fun `accuracy counts partials as half`() {
        assertEquals(60, Grading.accuracy(correct = 5, partial = 2, attempted = 10))
        assertEquals(0, Grading.accuracy(0, 0, 0))
    }
}

class MathFormatTest {

    @Test
    fun `converts powers and subscripts`() {
        assertEquals("x² + y²", MathFormat.pretty("x^2 + y^2"))
        assertEquals("a₁ + a₂", MathFormat.pretty("a_1 + a_2"))
        assertEquals("2ⁿ", MathFormat.pretty("2^n"))
    }

    @Test
    fun `replaces common operators and symbols`() {
        assertEquals("a → b", MathFormat.pretty("a -> b"))
        assertEquals("x ≤ 5", MathFormat.pretty("x <= 5"))
        assertEquals("√(x)", MathFormat.pretty("\\sqrt{x}"))
        assertEquals("3 × 4", MathFormat.pretty("3 * 4"))
        assertEquals("π r²", MathFormat.pretty("\\pi r^2"))
    }

    @Test
    fun `strips tex delimiters`() {
        assertEquals("v = u + at", MathFormat.stripDelimiters("\$\$v = u + at\$\$"))
    }
}
