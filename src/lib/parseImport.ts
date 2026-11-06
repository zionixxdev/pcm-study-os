/** Parser for the bulk topic importer. Supports the documented paste format:
 *
 *   Chapter 1: Units and Measurements
 *   - Physical quantities
 *     - Fundamental and derived quantities
 *   - Dimensional analysis
 *
 * plus CSV (chapter,topic,subtopic) and JSON (array of chapter objects).
 * Malformed input is reported as warnings, never thrown. */

export interface ParsedTopic {
  name: string
  subtopics: string[]
}

export interface ParsedChapter {
  name: string
  topics: ParsedTopic[]
}

export interface ParseResult {
  chapters: ParsedChapter[]
  warnings: string[]
  topicCount: number
  subtopicCount: number
}

const CHAPTER_RE = /^(?:#{1,3}\s*)?(?:chapter|unit|ch\.?)\s*[-\d.:)]*\s*(.+)$/i
const BULLET_RE = /^(\s*)(?:[-*•–]|\d+[.)])\s+(.*)$/

function clean(s: string): string {
  return s.replace(/\s+/g, ' ').trim()
}

export function parseBulkText(input: string): ParseResult {
  const warnings: string[] = []
  const chapters: ParsedChapter[] = []
  let currentChapter: ParsedChapter | null = null
  let currentTopic: ParsedTopic | null = null

  const lines = input.split(/\r?\n/)

  for (let i = 0; i < lines.length; i++) {
    const rawLine = lines[i]
    const lineNo = i + 1
    if (!rawLine.trim()) continue

    const bullet = rawLine.match(BULLET_RE)
    const indent = bullet ? bullet[1].replace(/\t/g, '  ').length : 0
    const body = clean(bullet ? bullet[2] : rawLine)

    if (!body) continue

    const isHeading = !bullet && (CHAPTER_RE.test(body) || body.endsWith(':'))

    if (isHeading) {
      const m = body.match(CHAPTER_RE)
      const name = clean(m ? m[1] : body.replace(/:$/, ''))
      if (!name) {
        warnings.push(`Line ${lineNo}: empty chapter name — skipped.`)
        continue
      }
      currentChapter = { name, topics: [] }
      chapters.push(currentChapter)
      currentTopic = null
      continue
    }

    if (!currentChapter) {
      // Text before any chapter heading becomes an implicit first chapter.
      currentChapter = { name: 'Imported topics', topics: [] }
      chapters.push(currentChapter)
      warnings.push(`Line ${lineNo}: text before the first chapter heading was placed in "Imported topics".`)
    }

    if (bullet && indent >= 2 && currentTopic) {
      currentTopic.subtopics.push(body)
      continue
    }

    currentTopic = { name: body, subtopics: [] }
    currentChapter.topics.push(currentTopic)
  }

  const topicCount = chapters.reduce((n, c) => n + c.topics.length, 0)
  const subtopicCount = chapters.reduce((n, c) => n + c.topics.reduce((m, t) => m + t.subtopics.length, 0), 0)

  if (chapters.length === 0) warnings.push('No chapters or topics were detected in the input.')

  return { chapters, warnings, topicCount, subtopicCount }
}

export function parseCsv(input: string): ParseResult {
  const warnings: string[] = []
  const chapters: ParsedChapter[] = []
  const byName = new Map<string, ParsedChapter>()

  const lines = input.split(/\r?\n/).filter((l) => l.trim().length > 0)
  if (lines.length === 0) return { chapters, warnings: ['The CSV file is empty.'], topicCount: 0, subtopicCount: 0 }

  let start = 0
  const header = lines[0].toLowerCase()
  if (header.includes('chapter') || header.includes('topic')) start = 1

  for (let i = start; i < lines.length; i++) {
    const cells = splitCsvLine(lines[i]).map((c) => clean(c))
    if (cells.every((c) => !c)) continue
    const [chapterName, topicName, subtopicName] = cells
    if (!chapterName || !topicName) {
      warnings.push(`Line ${i + 1}: needs at least a chapter and a topic column — skipped.`)
      continue
    }
    let chapter = byName.get(chapterName)
    if (!chapter) {
      chapter = { name: chapterName, topics: [] }
      byName.set(chapterName, chapter)
      chapters.push(chapter)
    }
    let topic = chapter.topics.find((t) => t.name === topicName)
    if (!topic) {
      topic = { name: topicName, subtopics: [] }
      chapter.topics.push(topic)
    }
    if (subtopicName) topic.subtopics.push(subtopicName)
  }

  return {
    chapters,
    warnings,
    topicCount: chapters.reduce((n, c) => n + c.topics.length, 0),
    subtopicCount: chapters.reduce((n, c) => n + c.topics.reduce((m, t) => m + t.subtopics.length, 0), 0),
  }
}

function splitCsvLine(line: string): string[] {
  const out: string[] = []
  let cur = ''
  let quoted = false
  for (let i = 0; i < line.length; i++) {
    const ch = line[i]
    if (ch === '"') {
      if (quoted && line[i + 1] === '"') {
        cur += '"'
        i++
      } else quoted = !quoted
    } else if (ch === ',' && !quoted) {
      out.push(cur)
      cur = ''
    } else cur += ch
  }
  out.push(cur)
  return out
}

export function parseJsonImport(input: string): ParseResult {
  const warnings: string[] = []
  try {
    const data: unknown = JSON.parse(input)
    if (!Array.isArray(data)) {
      return { chapters: [], warnings: ['JSON must be an array of chapters.'], topicCount: 0, subtopicCount: 0 }
    }
    const chapters: ParsedChapter[] = []
    for (const raw of data) {
      const obj = raw as { name?: unknown; topics?: unknown }
      if (typeof obj?.name !== 'string') {
        warnings.push('Skipped an entry without a chapter name.')
        continue
      }
      const topics: ParsedTopic[] = []
      if (Array.isArray(obj.topics)) {
        for (const t of obj.topics) {
          const to = t as { name?: unknown; subtopics?: unknown }
          if (typeof to?.name !== 'string') continue
          const subtopics = Array.isArray(to.subtopics)
            ? to.subtopics.filter((s): s is string => typeof s === 'string')
            : []
          topics.push({ name: to.name, subtopics })
        }
      }
      chapters.push({ name: obj.name, topics })
    }
    return {
      chapters,
      warnings,
      topicCount: chapters.reduce((n, c) => n + c.topics.length, 0),
      subtopicCount: chapters.reduce((n, c) => n + c.topics.reduce((m, t) => m + t.subtopics.length, 0), 0),
    }
  } catch {
    return { chapters: [], warnings: ['The JSON could not be parsed.'], topicCount: 0, subtopicCount: 0 }
  }
}

export function parseAuto(input: string): ParseResult {
  const trimmed = input.trim()
  if (trimmed.startsWith('[') || trimmed.startsWith('{')) return parseJsonImport(trimmed)
  if (/^[^,\n]+,[^,\n]+/.test(trimmed.split(/\r?\n/)[0] ?? '')) return parseCsv(trimmed)
  return parseBulkText(trimmed)
}

/** Marks parsed topics whose names already exist in the destination. */
export function markDuplicates(chapters: ParsedChapter[], existingTopics: string[]): ParsedChapter[] {
  const seen = new Set(existingTopics.map((t) => t.toLowerCase()))
  return chapters.map((c) => ({
    ...c,
    topics: c.topics.map((t) => {
      const dup = seen.has(t.name.toLowerCase())
      seen.add(t.name.toLowerCase())
      return { ...t, duplicate: dup } as ParsedTopic & { duplicate: boolean }
    }),
  }))
}
