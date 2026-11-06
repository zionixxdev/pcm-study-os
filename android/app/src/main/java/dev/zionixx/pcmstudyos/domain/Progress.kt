package dev.zionixx.pcmstudyos.domain

import dev.zionixx.pcmstudyos.data.local.entity.StudyComponent
import dev.zionixx.pcmstudyos.data.local.entity.Subtopic
import dev.zionixx.pcmstudyos.data.local.entity.Topic

/**
 * Syllabus progress. Every percentage is derived from stored rows — nothing is
 * hardcoded. Rules (identical to the web app, so the two stay consistent):
 *
 *  1. An explicit `completed` topic status wins.
 *  2. Otherwise average the signals that exist: subtopic completion and study
 *     component completion.
 *  3. A topic with neither falls back to its status weight.
 */
object Progress {

    const val STATUS_NOT_STARTED = "not_started"
    const val STATUS_IN_PROGRESS = "in_progress"
    const val STATUS_COMPLETED = "completed"
    const val STATUS_NEEDS_REVISION = "needs_revision"

    fun topicPercent(topic: Topic, subtopics: List<Subtopic>, components: List<StudyComponent>): Int {
        if (topic.status == STATUS_COMPLETED) return 100
        val signals = mutableListOf<Double>()
        if (subtopics.isNotEmpty()) {
            signals += subtopics.count { it.completed }.toDouble() / subtopics.size
        }
        if (components.isNotEmpty()) {
            signals += components.count { it.done }.toDouble() / components.size
        }
        if (signals.isEmpty()) {
            return when (topic.status) {
                STATUS_IN_PROGRESS -> 50
                STATUS_NEEDS_REVISION -> 60
                else -> 0
            }
        }
        return clamp((signals.average() * 100).toInt())
    }

    fun chapterPercent(topics: List<Topic>, percentOf: (Topic) -> Int): Int {
        if (topics.isEmpty()) return 0
        return clamp(topics.sumOf { percentOf(it) } / topics.size)
    }

    fun subjectPercent(chapterPercents: List<Int>): Int {
        if (chapterPercents.isEmpty()) return 0
        return clamp(chapterPercents.sum() / chapterPercents.size)
    }

    fun overall(subjectPercents: List<Int>): Int = subjectPercent(subjectPercents)

    private fun clamp(value: Int): Int = value.coerceIn(0, 100)
}
