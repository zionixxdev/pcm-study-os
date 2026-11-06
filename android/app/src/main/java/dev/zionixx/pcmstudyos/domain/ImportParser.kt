package dev.zionixx.pcmstudyos.domain

/**
 * Bulk topic import. Accepts the paste format, CSV and JSON; always returns a
 * preview plus warnings, and never throws on malformed input.
 *
 *   Chapter 1: Units and Measurements
 *   - Physical quantities
 *     - Fundamental units
 *   - Dimensional analysis
 */
object ImportParser {

    data class ParsedTopic(val name: String, val subtopics: List<String>, val duplicate: Boolean = false)
    data class ParsedChapter(val name: String, val topics: List<ParsedTopic>)
    data class Result(
        val chapters: List<ParsedChapter>,
        val warnings: List<String>,
    ) {
        val topicCount: Int get() = chapters.sumOf { it.topics.size }
        val subtopicCount: Int get() = chapters.sumOf { chapter -> chapter.topics.sumOf { it.subtopics.size } }
    }

    private val chapterRegex = Regex("""^(?:#{1,3}\s*)?(?:chapter|unit|ch\.?)\s*[-\d.:)]*\s*(.+)$""", RegexOption.IGNORE_CASE)
    private val bulletRegex = Regex("""^(\s*)(?:[-*•–]|\d+[.)])\s+(.*)$""")

    fun parseText(input: String): Result {
        val warnings = mutableListOf<String>()
        val chapters = mutableListOf<MutableChapter>()
        var currentChapter: MutableChapter? = null
        var currentTopic: MutableTopic? = null

        input.lines().forEachIndexed { index, raw ->
            if (raw.isBlank()) return@forEachIndexed
            val lineNo = index + 1
            val bullet = bulletRegex.find(raw)
            val indent = (bullet?.groupValues?.get(1) ?: "").replace("\t", "  ").length
            val body = (bullet?.groupValues?.get(2) ?: raw).replace(Regex("\\s+"), " ").trim()
            if (body.isEmpty()) return@forEachIndexed

            val isHeading = bullet == null && (chapterRegex.matches(body) || body.endsWith(":"))
            if (isHeading) {
                val name = chapterRegex.find(body)?.groupValues?.get(1)?.trim()?.ifEmpty { null }
                    ?: body.removeSuffix(":").trim()
                if (name.isEmpty()) {
                    warnings += "Line $lineNo: empty chapter name — skipped."
                    return@forEachIndexed
                }
                currentChapter = MutableChapter(name)
                chapters += currentChapter!!
                currentTopic = null
                return@forEachIndexed
            }

            if (currentChapter == null) {
                currentChapter = MutableChapter("Imported topics")
                chapters += currentChapter!!
                warnings += "Line $lineNo: text before the first chapter heading was placed in \"Imported topics\"."
            }

            if (bullet != null && indent >= 2 && currentTopic != null) {
                currentTopic!!.subtopics += body
                return@forEachIndexed
            }

            currentTopic = MutableTopic(body)
            currentChapter!!.topics += currentTopic!!
        }

        if (chapters.isEmpty()) warnings += "No chapters or topics were detected in the input."
        return Result(chapters.map { it.freeze() }, warnings)
    }

    fun parseCsv(input: String): Result {
        val warnings = mutableListOf<String>()
        val lines = input.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return Result(emptyList(), listOf("The CSV is empty."))

        var start = 0
        val header = lines.first().lowercase()
        if (header.contains("chapter") || header.contains("topic")) start = 1

        val chapters = linkedMapOf<String, MutableChapter>()
        for (i in start until lines.size) {
            val cells = splitCsvLine(lines[i]).map { it.trim() }
            if (cells.all { it.isEmpty() }) continue
            val chapterName = cells.getOrNull(0).orEmpty()
            val topicName = cells.getOrNull(1).orEmpty()
            if (chapterName.isEmpty() || topicName.isEmpty()) {
                warnings += "Line ${i + 1}: needs at least a chapter and a topic column — skipped."
                continue
            }
            val chapter = chapters.getOrPut(chapterName) { MutableChapter(chapterName) }
            val topic = chapter.topics.firstOrNull { it.name == topicName } ?: MutableTopic(topicName).also { chapter.topics += it }
            cells.getOrNull(2)?.takeIf { it.isNotEmpty() }?.let { topic.subtopics += it }
        }
        return Result(chapters.values.map { it.freeze() }, warnings)
    }

    fun parseJson(input: String): Result {
        return try {
            val array = org.json.JSONArray(input.trim())
            val chapters = mutableListOf<ParsedChapter>()
            val warnings = mutableListOf<String>()
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val name = obj.optString("name")
                if (name.isBlank()) {
                    warnings += "Skipped an entry without a chapter name."
                    continue
                }
                val topics = mutableListOf<ParsedTopic>()
                val topicArray = obj.optJSONArray("topics")
                if (topicArray != null) {
                    for (j in 0 until topicArray.length()) {
                        val t = topicArray.optJSONObject(j) ?: continue
                        val topicName = t.optString("name")
                        if (topicName.isBlank()) continue
                        val subs = mutableListOf<String>()
                        t.optJSONArray("subtopics")?.let { subArray ->
                            for (k in 0 until subArray.length()) {
                                subArray.optString(k).takeIf { it.isNotBlank() }?.let { subs += it }
                            }
                        }
                        topics += ParsedTopic(topicName, subs)
                    }
                }
                chapters += ParsedChapter(name, topics)
            }
            Result(chapters, warnings)
        } catch (e: Exception) {
            Result(emptyList(), listOf("The JSON could not be parsed: ${e.message ?: "invalid format"}"))
        }
    }

    fun parseAuto(input: String): Result {
        val trimmed = input.trim()
        return when {
            trimmed.startsWith("[") || trimmed.startsWith("{") -> parseJson(trimmed)
            trimmed.lines().firstOrNull()?.let { it.count { ch -> ch == ',' } >= 1 } == true -> parseCsv(trimmed)
            else -> parseText(trimmed)
        }
    }

    fun markDuplicates(chapters: List<ParsedChapter>, existing: List<String>): List<ParsedChapter> {
        val seen = existing.map { it.lowercase() }.toMutableSet()
        return chapters.map { chapter ->
            chapter.copy(
                topics = chapter.topics.map { topic ->
                    val duplicate = !seen.add(topic.name.lowercase())
                    topic.copy(duplicate = duplicate)
                },
            )
        }
    }

    private fun splitCsvLine(line: String): List<String> {
        val out = mutableListOf<String>()
        val current = StringBuilder()
        var quoted = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            when {
                ch == '"' && quoted && i + 1 < line.length && line[i + 1] == '"' -> {
                    current.append('"'); i++
                }
                ch == '"' -> quoted = !quoted
                ch == ',' && !quoted -> {
                    out += current.toString(); current.clear()
                }
                else -> current.append(ch)
            }
            i++
        }
        out += current.toString()
        return out
    }

    private class MutableTopic(val name: String) {
        val subtopics = mutableListOf<String>()
    }

    private class MutableChapter(val name: String) {
        val topics = mutableListOf<MutableTopic>()
        fun freeze() = ParsedChapter(name, topics.map { ParsedTopic(it.name, it.subtopics.toList()) })
    }
}
