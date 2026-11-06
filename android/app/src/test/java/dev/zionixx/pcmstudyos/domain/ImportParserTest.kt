package dev.zionixx.pcmstudyos.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportParserTest {

    @Test
    fun `parses chapters topics and indented subtopics`() {
        val input = """
            Chapter 1: Units and Measurements
            - Physical quantities
              - Fundamental units
              - Derived units
            - Dimensional analysis

            Chapter 2: Motion in a Straight Line
            - Speed and velocity
            - Acceleration
        """.trimIndent()

        val result = ImportParser.parseText(input)
        assertEquals(2, result.chapters.size)
        assertEquals("Units and Measurements", result.chapters[0].name)
        assertEquals(2, result.chapters[0].topics.size)
        assertEquals(listOf("Fundamental units", "Derived units"), result.chapters[0].topics[0].subtopics)
        assertEquals(4, result.topicCount)
        assertEquals(2, result.subtopicCount)
        assertTrue(result.warnings.isEmpty())
    }

    @Test
    fun `treats a trailing colon line as a chapter heading`() {
        val result = ImportParser.parseText("Kinematics:\n- Speed\n- Velocity")
        assertEquals("Kinematics", result.chapters[0].name)
        assertEquals(2, result.chapters[0].topics.size)
    }

    @Test
    fun `handles orphan topics with a warning`() {
        val result = ImportParser.parseText("- orphan topic")
        assertEquals(1, result.chapters.size)
        assertTrue(result.warnings.isNotEmpty())
    }

    @Test
    fun `warns on empty input`() {
        assertTrue(ImportParser.parseText("   ").warnings.isNotEmpty())
    }

    @Test
    fun `parses csv with a header row`() {
        val csv = "chapter,topic,subtopic\nOptics,Reflection,Laws\nOptics,Reflection,Plane mirrors\nOptics,Refraction,Snell"
        val result = ImportParser.parseCsv(csv)
        assertEquals(1, result.chapters.size)
        assertEquals(2, result.chapters[0].topics.size)
        assertEquals(2, result.chapters[0].topics[0].subtopics.size)
        assertEquals(2, result.topicCount)
    }

    @Test
    fun `csv keeps quoted commas`() {
        val result = ImportParser.parseCsv("\"Optics, Light\",Reflection,Mirrors")
        assertEquals("Optics, Light", result.chapters[0].name)
    }

    @Test
    fun `warns when a csv row lacks a topic`() {
        assertTrue(ImportParser.parseCsv("Optics\nReflection").warnings.isNotEmpty())
    }

    @Test
    fun `parses json chapters`() {
        val json = """[{"name":"Waves","topics":[{"name":"Beats","subtopics":["Frequency"]}]}]"""
        val result = ImportParser.parseJson(json)
        assertEquals("Beats", result.chapters[0].topics[0].name)
        assertEquals(listOf("Frequency"), result.chapters[0].topics[0].subtopics)
    }

    @Test
    fun `invalid json is reported not thrown`() {
        assertTrue(ImportParser.parseJson("{not json").warnings.isNotEmpty())
    }

    @Test
    fun `auto detects json csv and text`() {
        assertEquals("A", ImportParser.parseAuto("""[{"name":"A","topics":[]}]""").chapters[0].name)
        assertEquals("A", ImportParser.parseAuto("Chapter,Topic\nA,B").chapters[0].name)
        assertEquals("X", ImportParser.parseAuto("Chapter 1: X\n- y").chapters[0].name)
    }

    @Test
    fun `marks duplicates against existing topics`() {
        val parsed = listOf(
            ImportParser.ParsedChapter(
                "Ch",
                listOf(
                    ImportParser.ParsedTopic("Speed", emptyList()),
                    ImportParser.ParsedTopic("Velocity", emptyList()),
                ),
            ),
        )
        val marked = ImportParser.markDuplicates(parsed, listOf("speed"))
        assertTrue(marked[0].topics[0].duplicate)
        assertTrue(!marked[0].topics[1].duplicate)
    }
}
