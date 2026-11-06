import { createRow, createRows } from '../db'
import { CBSE11_SUBJECTS, type SeedSubject } from './cbse11'

/** Installs the editable CBSE seed syllabus into an academic year.
 * Everything created here is ordinary app data: rename, reorder or delete it. */
export async function installSyllabus(yearId: string, subjects: SeedSubject[] = CBSE11_SUBJECTS): Promise<void> {
  let subjectOrder = 0
  for (const subject of subjects) {
    const subjectId = await createRow('subjects', {
      yearId,
      name: subject.name,
      description: subject.description,
      color: subject.color,
      icon: subject.icon,
      priority: 'high',
      order: subjectOrder++,
    })

    let chapterOrder = 0
    for (const chapter of subject.chapters) {
      const chapterId = await createRow('chapters', {
        subjectId,
        name: chapter.name,
        priority: 'medium',
        order: chapterOrder++,
      })

      const topicRows: Array<{ id: string; name: string; order: number }> = []
      let topicOrder = 0
      for (const topic of chapter.topics) {
        const id = crypto.randomUUID()
        topicRows.push({ id, name: topic.name, order: topicOrder++ })
        await createRow('topics', {
          id,
          chapterId,
          name: topic.name,
          priority: 'medium',
          status: 'not_started',
          order: topicOrder,
        })

        if (topic.subtopics?.length) {
          await createRows(
            'subtopics',
            topic.subtopics.map((name, i) => ({
              topicId: id,
              name,
              difficulty: 'medium' as const,
              completed: false,
              order: i,
            })),
          )
        }

        await createRows(
          'components',
          subject.components.map((type) => ({ topicId: id, type, done: false })),
        )
      }
      void topicRows
    }
  }
}
