import { describe, expect, it } from 'vitest'
import { buildProgress, topicPercent } from '../progress'
import type { Chapter, StudyComponent, Subject, Subtopic, Topic } from '../../types'

const base = { userId: 'u', createdAt: '2025-01-01T00:00:00Z', updatedAt: '2025-01-01T00:00:00Z' }

function topic(id: string, chapterId: string, status: Topic['status'] = 'not_started'): Topic {
  return { ...base, id, chapterId, name: id, priority: 'medium', status, order: 0 }
}
function subject(id: string): Subject {
  return { ...base, id, yearId: 'y1', name: id, color: '#fff', priority: 'medium', order: 0 }
}
function chapter(id: string, subjectId: string): Chapter {
  return { ...base, id, subjectId, name: id, priority: 'medium', order: 0 }
}

describe('topicPercent', () => {
  it('uses subtopic completion when subtopics exist', () => {
    const subs: Subtopic[] = [
      { ...base, id: 's1', topicId: 't1', name: 'a', difficulty: 'easy', completed: true, order: 0 },
      { ...base, id: 's2', topicId: 't1', name: 'b', difficulty: 'easy', completed: false, order: 1 },
    ]
    expect(topicPercent(topic('t1', 'c1'), subs, [])).toBe(50)
  })

  it('averages subtopic and component signals', () => {
    const subs: Subtopic[] = [
      { ...base, id: 's1', topicId: 't1', name: 'a', difficulty: 'easy', completed: true, order: 0 },
      { ...base, id: 's2', topicId: 't1', name: 'b', difficulty: 'easy', completed: true, order: 1 },
    ]
    const comps: StudyComponent[] = [
      { ...base, id: 'k1', topicId: 't1', type: 'theory', done: true },
      { ...base, id: 'k2', topicId: 't1', type: 'numericals', done: false },
    ]
    expect(topicPercent(topic('t1', 'c1'), subs, comps)).toBe(75)
  })

  it('falls back to status when there are no signals', () => {
    expect(topicPercent(topic('t1', 'c1', 'not_started'))).toBe(0)
    expect(topicPercent(topic('t1', 'c1', 'in_progress'))).toBe(50)
    expect(topicPercent(topic('t1', 'c1', 'needs_revision'))).toBe(60)
  })

  it('lets an explicit completed status win', () => {
    const subs: Subtopic[] = [{ ...base, id: 's1', topicId: 't1', name: 'a', difficulty: 'easy', completed: false, order: 0 }]
    expect(topicPercent(topic('t1', 'c1', 'completed'), subs, [])).toBe(100)
  })
})

describe('buildProgress', () => {
  it('rolls topic -> chapter -> subject -> overall', () => {
    const bundle = {
      subjects: [subject('sub1'), subject('sub2')],
      chapters: [chapter('c1', 'sub1'), chapter('c2', 'sub2')],
      topics: [topic('t1', 'c1', 'completed'), topic('t2', 'c2', 'in_progress')],
      subtopics: [] as Subtopic[],
      components: [] as StudyComponent[],
    }
    const result = buildProgress(bundle)
    expect(result.topicPct.get('t1')).toBe(100)
    expect(result.topicPct.get('t2')).toBe(50)
    expect(result.chapterPct.get('c1')).toBe(100)
    expect(result.subjectPct.get('sub1')).toBe(100)
    expect(result.subjectPct.get('sub2')).toBe(50)
    expect(result.overall).toBe(75)
    expect(result.completedTopics).toBe(1)
    expect(result.remainingTopics).toBe(1)
  })

  it('handles empty structures without dividing by zero', () => {
    const result = buildProgress({ subjects: [subject('sub1')], chapters: [], topics: [], subtopics: [], components: [] })
    expect(result.overall).toBe(0)
    expect(result.totalTopics).toBe(0)
  })

  it('ignores archived topics', () => {
    const archived: Topic = { ...topic('t3', 'c1', 'completed'), archived: true }
    const result = buildProgress({
      subjects: [subject('sub1')],
      chapters: [chapter('c1', 'sub1')],
      topics: [topic('t1', 'c1', 'in_progress'), archived],
      subtopics: [],
      components: [],
    })
    expect(result.totalTopics).toBe(1)
    expect(result.chapterPct.get('c1')).toBe(50)
  })
})
