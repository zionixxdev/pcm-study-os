import { useLiveQuery } from 'dexie-react-hooks'
import { db } from '../lib/db'
import type { Chapter, StudyComponent, Subject, Subtopic, Topic } from '../types'

export interface YearData {
  subjects: Subject[]
  chapters: Chapter[]
  topics: Topic[]
  subtopics: Subtopic[]
  components: StudyComponent[]
}

const EMPTY: YearData = { subjects: [], chapters: [], topics: [], subtopics: [], components: [] }

/** All syllabus structure for one academic year. Progress from other years is
 * never mixed in, because every query is scoped through subjects -> yearId. */
export function useYearData(yearId: string | null): YearData | undefined {
  return useLiveQuery(async () => {
    if (!yearId) return EMPTY
    const subjects = await db.subjects.where({ yearId }).toArray()
    subjects.sort((a, b) => a.order - b.order)
    const subjectIds = new Set(subjects.map((s) => s.id))
    const chapters = (await db.chapters.toArray()).filter((c) => subjectIds.has(c.subjectId))
    chapters.sort((a, b) => a.order - b.order)
    const chapterIds = new Set(chapters.map((c) => c.id))
    const topics = (await db.topics.toArray()).filter((t) => chapterIds.has(t.chapterId))
    topics.sort((a, b) => a.order - b.order)
    const topicIds = new Set(topics.map((t) => t.id))
    const [subtopics, components] = await Promise.all([
      db.subtopics.toArray().then((rows) => rows.filter((r) => topicIds.has(r.topicId))),
      db.components.toArray().then((rows) => rows.filter((r) => topicIds.has(r.topicId))),
    ])
    subtopics.sort((a, b) => a.order - b.order)
    return { subjects, chapters, topics, subtopics, components }
  }, [yearId])
}
