import { createRows } from '../db'
import { CBSE11_SUBJECTS, type SeedSubject } from './cbse11'

/** Installs the editable CBSE seed syllabus into an academic year.
 *
 * Everything created here is ordinary app data: rename, reorder or delete it.
 * Rows are generated in memory and bulk-written per table — the whole Class 11
 * syllabus lands in a handful of transactions instead of a thousand. */
export async function installSyllabus(yearId: string, subjects: SeedSubject[] = CBSE11_SUBJECTS): Promise<void> {
  const subjectRows: Array<Record<string, unknown>> = []
  const chapterRows: Array<Record<string, unknown>> = []
  const topicRows: Array<Record<string, unknown>> = []
  const subtopicRows: Array<Record<string, unknown>> = []
  const componentRows: Array<Record<string, unknown>> = []

  subjects.forEach((subject, subjectIndex) => {
    const subjectId = crypto.randomUUID()
    subjectRows.push({
      id: subjectId,
      yearId,
      name: subject.name,
      description: subject.description,
      color: subject.color,
      icon: subject.icon,
      priority: 'high',
      order: subjectIndex,
    })

    subject.chapters.forEach((chapter, chapterIndex) => {
      const chapterId = crypto.randomUUID()
      chapterRows.push({
        id: chapterId,
        subjectId,
        name: chapter.name,
        priority: 'medium',
        order: chapterIndex,
      })

      chapter.topics.forEach((topic, topicIndex) => {
        const topicId = crypto.randomUUID()
        topicRows.push({
          id: topicId,
          chapterId,
          name: topic.name,
          priority: 'medium',
          status: 'not_started',
          order: topicIndex,
        })

        topic.subtopics?.forEach((name, subIndex) => {
          subtopicRows.push({
            id: crypto.randomUUID(),
            topicId,
            name,
            difficulty: 'medium',
            completed: false,
            order: subIndex,
          })
        })

        subject.components.forEach((type) => {
          componentRows.push({ id: crypto.randomUUID(), topicId, type, done: false })
        })
      })
    })
  })

  // createRows stamps ownership and timestamps; ids above are preserved.
  await createRows('subjects', subjectRows as never)
  if (chapterRows.length) await createRows('chapters', chapterRows as never)
  if (topicRows.length) await createRows('topics', topicRows as never)
  if (subtopicRows.length) await createRows('subtopics', subtopicRows as never)
  if (componentRows.length) await createRows('components', componentRows as never)
}
