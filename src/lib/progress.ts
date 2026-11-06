import type { Chapter, StudyComponent, Subject, Subtopic, Topic } from '../types'

/** Progress is always derived from stored records — never stored as a
 * percentage. The rules below are the single source of truth used by the
 * dashboard, syllabus tree and analytics. */

const clamp = (n: number) => Math.max(0, Math.min(100, Math.round(n)))

export function ratio(done: number, total: number): number | null {
  if (total <= 0) return null
  return done / total
}

/** 0..100 for a single topic.
 * 1. An explicit "completed" status wins.
 * 2. Otherwise the average of the signals that exist: subtopic completion and
 *    study-component completion. A topic with neither falls back to its status.
 */
export function topicPercent(topic: Topic, subtopics: Subtopic[] = [], components: StudyComponent[] = []): number {
  if (topic.status === 'completed') return 100
  const signals: number[] = []
  const subRatio = ratio(subtopics.filter((s) => s.completed).length, subtopics.length)
  if (subRatio !== null) signals.push(subRatio)
  const compRatio = ratio(components.filter((c) => c.done).length, components.length)
  if (compRatio !== null) signals.push(compRatio)
  if (signals.length === 0) {
    switch (topic.status) {
      case 'in_progress':
        return 50
      case 'needs_revision':
        return 60
      default:
        return 0
    }
  }
  return clamp((signals.reduce((a, b) => a + b, 0) / signals.length) * 100)
}

export interface ProgressBundle {
  subjects: Subject[]
  chapters: Chapter[]
  topics: Topic[]
  subtopics: Subtopic[]
  components: StudyComponent[]
}

function groupBy<T>(rows: T[], key: (row: T) => string | undefined): Map<string, T[]> {
  const map = new Map<string, T[]>()
  for (const row of rows) {
    const k = key(row)
    if (!k) continue
    const list = map.get(k)
    if (list) list.push(row)
    else map.set(k, [row])
  }
  return map
}

export function buildProgress(bundle: ProgressBundle) {
  const subsByTopic = groupBy(bundle.subtopics, (s) => s.topicId)
  const compsByTopic = groupBy(bundle.components, (c) => c.topicId)
  const topicsByChapter = groupBy(
    bundle.topics.filter((t) => !t.archived),
    (t) => t.chapterId,
  )
  const chaptersBySubject = groupBy(
    bundle.chapters.filter((c) => !c.archived),
    (c) => c.subjectId,
  )

  const topicPct = new Map<string, number>()
  for (const topic of bundle.topics) {
    topicPct.set(topic.id, topicPercent(topic, subsByTopic.get(topic.id) ?? [], compsByTopic.get(topic.id) ?? []))
  }

  const chapterPct = new Map<string, number>()
  for (const chapter of bundle.chapters) {
    const topics = topicsByChapter.get(chapter.id) ?? []
    const pcts = topics.map((t) => topicPct.get(t.id) ?? 0)
    chapterPct.set(chapter.id, pcts.length ? clamp(pcts.reduce((a, b) => a + b, 0) / pcts.length) : 0)
  }

  const subjectPct = new Map<string, number>()
  for (const subject of bundle.subjects.filter((s) => !s.archived)) {
    const chapters = chaptersBySubject.get(subject.id) ?? []
    const pcts = chapters.map((c) => chapterPct.get(c.id) ?? 0)
    subjectPct.set(subject.id, pcts.length ? clamp(pcts.reduce((a, b) => a + b, 0) / pcts.length) : 0)
  }

  const subjectPcts = [...subjectPct.values()]
  const overall = subjectPcts.length ? clamp(subjectPcts.reduce((a, b) => a + b, 0) / subjectPcts.length) : 0

  const activeTopics = bundle.topics.filter((t) => !t.archived)
  const completedTopics = activeTopics.filter((t) => topicPct.get(t.id) === 100).length
  const activeChapters = bundle.chapters.filter((c) => !c.archived)
  const completedChapters = activeChapters.filter((c) => chapterPct.get(c.id) === 100).length

  return {
    overall,
    topicPct,
    chapterPct,
    subjectPct,
    completedTopics,
    remainingTopics: activeTopics.length - completedTopics,
    totalTopics: activeTopics.length,
    completedChapters,
    totalChapters: activeChapters.length,
  }
}

/** Chapter completion for one subject, for subject cards. */
export function subjectSummary(bundle: ProgressBundle, subjectId: string) {
  const progress = buildProgress(bundle)
  const chapters = bundle.chapters.filter((c) => c.subjectId === subjectId && !c.archived)
  const topics = bundle.topics.filter((t) => chapters.some((c) => c.id === t.chapterId) && !t.archived)
  return {
    percent: progress.subjectPct.get(subjectId) ?? 0,
    chaptersDone: chapters.filter((c) => progress.chapterPct.get(c.id) === 100).length,
    chaptersTotal: chapters.length,
    topicsDone: topics.filter((t) => progress.topicPct.get(t.id) === 100).length,
    topicsTotal: topics.length,
  }
}
