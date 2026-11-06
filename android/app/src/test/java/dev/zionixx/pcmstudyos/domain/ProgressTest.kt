package dev.zionixx.pcmstudyos.domain

import dev.zionixx.pcmstudyos.data.local.entity.StudyComponent
import dev.zionixx.pcmstudyos.data.local.entity.Subtopic
import dev.zionixx.pcmstudyos.data.local.entity.Topic
import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressTest {

    private fun topic(id: String, status: String = "not_started") = Topic(
        id = id,
        chapterId = "c1",
        name = id,
        status = status,
    )

    private fun subtopic(id: String, topicId: String, done: Boolean) = Subtopic(
        id = id,
        topicId = topicId,
        name = id,
        completed = done,
    )

    private fun component(id: String, topicId: String, done: Boolean) = StudyComponent(
        id = id,
        topicId = topicId,
        type = id,
        done = done,
    )

    @Test
    fun `topic percent uses subtopic completion`() {
        val subs = listOf(subtopic("a", "t1", true), subtopic("b", "t1", false))
        assertEquals(50, Progress.topicPercent(topic("t1"), subs, emptyList()))
    }

    @Test
    fun `topic percent averages subtopic and component signals`() {
        val subs = listOf(subtopic("a", "t1", true), subtopic("b", "t1", true))
        val comps = listOf(component("theory", "t1", true), component("numericals", "t1", false))
        assertEquals(75, Progress.topicPercent(topic("t1"), subs, comps))
    }

    @Test
    fun `topic percent falls back to status when there are no signals`() {
        assertEquals(0, Progress.topicPercent(topic("t1", "not_started"), emptyList(), emptyList()))
        assertEquals(50, Progress.topicPercent(topic("t1", "in_progress"), emptyList(), emptyList()))
        assertEquals(60, Progress.topicPercent(topic("t1", "needs_revision"), emptyList(), emptyList()))
    }

    @Test
    fun `explicit completed status wins`() {
        val subs = listOf(subtopic("a", "t1", false))
        assertEquals(100, Progress.topicPercent(topic("t1", "completed"), subs, emptyList()))
    }

    @Test
    fun `chapter and subject percentages roll up`() {
        val chapter = Progress.chapterPercent(listOf(topic("t1"), topic("t2"))) { if (it.id == "t1") 100 else 50 }
        assertEquals(75, chapter)
        assertEquals(62, Progress.subjectPercent(listOf(100, 50, 37)))
    }

    @Test
    fun `empty structures never divide by zero`() {
        assertEquals(0, Progress.chapterPercent(emptyList()) { 0 })
        assertEquals(0, Progress.subjectPercent(emptyList()))
        assertEquals(0, Progress.overall(emptyList()))
    }

    @Test
    fun `overall averages subject percentages`() {
        assertEquals(75, Progress.overall(listOf(100, 50)))
    }
}
