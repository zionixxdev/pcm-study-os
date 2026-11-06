import { beforeEach, describe, expect, it } from 'vitest'
import { db, createRow, deleteRow, updateRow, deleteChapterCascade, setCurrentUserId } from '../db'
import { todayStr, addDays, formatMinutes } from '../dates'
import { completeTask } from '../tasks'

beforeEach(async () => {
  await db.delete()
  await db.open()
  setCurrentUserId('test-user')
})

describe('local persistence', () => {
  it('creates a row with timestamps and queues a sync op', async () => {
    const id = await createRow('academicYears', { name: 'Class 11', startDate: '2026-04-01', endDate: '2027-03-31', isCurrent: true })
    const row = await db.academicYears.get(id)
    expect(row?.name).toBe('Class 11')
    expect(row?.userId).toBe('test-user')
    expect(row?.createdAt).toBeTruthy()
    expect(await db.outbox.count()).toBe(1)
  })

  it('updates a row and keeps a single queued op per row', async () => {
    const id = await createRow('subjects', { yearId: 'y', name: 'Physics', color: '#fff', priority: 'high', order: 0 })
    await updateRow('subjects', id, { name: 'Physics (updated)' })
    const row = await db.subjects.get(id)
    expect(row?.name).toBe('Physics (updated)')
    expect((row?.updatedAt ?? '') >= (row?.createdAt ?? '')).toBe(true)
    const ops = await db.outbox.where({ rowId: id }).toArray()
    expect(ops).toHaveLength(1)
  })

  it('deletes a row and queues a delete op', async () => {
    const id = await createRow('subjects', { yearId: 'y', name: 'Temp', color: '#fff', priority: 'low', order: 0 })
    await deleteRow('subjects', id)
    expect(await db.subjects.get(id)).toBeUndefined()
    const op = await db.outbox.where({ rowId: id }).first()
    expect(op?.op).toBe('delete')
  })

  it('cascades chapter deletion to topics, subtopics and components', async () => {
    const subjectId = await createRow('subjects', { yearId: 'y', name: 'Physics', color: '#fff', priority: 'high', order: 0 })
    const chapterId = await createRow('chapters', { subjectId, name: 'Kinematics', priority: 'medium', order: 0 })
    const topicId = await createRow('topics', { chapterId, name: 'Speed', priority: 'medium', status: 'not_started', order: 0 })
    await createRow('subtopics', { topicId, name: 'Average speed', difficulty: 'easy', completed: false, order: 0 })
    await createRow('components', { topicId, type: 'theory', done: false })

    await deleteChapterCascade(chapterId)

    expect(await db.chapters.count()).toBe(0)
    expect(await db.topics.count()).toBe(0)
    expect(await db.subtopics.count()).toBe(0)
    expect(await db.components.count()).toBe(0)
    expect(await db.subjects.count()).toBe(1)
  })

  it('persists data across a close/reopen of the database', async () => {
    await createRow('notes', { yearId: 'y', title: 'Persisted', body: 'hello' })
    db.close()
    await db.open()
    const rows = await db.notes.toArray()
    expect(rows.map((r) => r.title)).toEqual(['Persisted'])
  })
})

describe('task completion', () => {
  it('marks a task complete', async () => {
    const id = await createRow('tasks', {
      yearId: 'y',
      title: 'Read NCERT',
      priority: 'medium',
      status: 'pending',
      estimatedMinutes: 45,
      recurring: 'none',
      order: 0,
      dueDate: todayStr(),
    })
    const task = await db.tasks.get(id)
    await completeTask(task!)
    const after = await db.tasks.get(id)
    expect(after?.status).toBe('completed')
    expect(after?.completedAt).toBeTruthy()
  })

  it('rolls a recurring task to its next occurrence', async () => {
    const id = await createRow('tasks', {
      yearId: 'y',
      title: 'Daily revision',
      priority: 'medium',
      status: 'pending',
      estimatedMinutes: 15,
      recurring: 'daily',
      order: 0,
      dueDate: '2026-01-05',
    })
    const task = await db.tasks.get(id)
    await completeTask(task!)
    const all = await db.tasks.toArray()
    expect(all).toHaveLength(2)
    const next = all.find((t) => t.id !== id)
    expect(next?.dueDate).toBe('2026-01-06')
    expect(next?.status).toBe('pending')
  })
})

describe('date helpers', () => {
  it('adds days across month boundaries', () => {
    expect(addDays('2026-01-31', 1)).toBe('2026-02-01')
    expect(addDays('2026-03-01', -1)).toBe('2026-02-28')
  })

  it('formats minutes for humans', () => {
    expect(formatMinutes(0)).toBe('0m')
    expect(formatMinutes(45)).toBe('45m')
    expect(formatMinutes(60)).toBe('1h')
    expect(formatMinutes(135)).toBe('2h 15m')
  })
})
