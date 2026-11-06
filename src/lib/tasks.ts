import { addDays, nowIso, todayStr } from './dates'
import { createRow, updateRow } from './db'
import type { Task } from '../types'

/** Marks a task complete and rolls recurring tasks forward instead of
 * duplicating history. */
export async function completeTask(task: Task): Promise<void> {
  await updateRow('tasks', task.id, { status: 'completed', completedAt: nowIso() })
  if (task.recurring === 'none') return
  const base = task.dueDate ?? todayStr()
  const nextDue = addDays(base, task.recurring === 'daily' ? 1 : 7)
  await createRow('tasks', {
    yearId: task.yearId,
    title: task.title,
    description: task.description,
    subjectId: task.subjectId,
    chapterId: task.chapterId,
    topicId: task.topicId,
    priority: task.priority,
    status: 'pending',
    startDate: task.startDate,
    dueDate: nextDue,
    estimatedMinutes: task.estimatedMinutes,
    recurring: task.recurring,
    order: task.order,
  })
}

export async function reopenTask(task: Task): Promise<void> {
  await updateRow('tasks', task.id, { status: 'pending', completedAt: undefined })
}

export function isOverdue(task: Task, today: string = todayStr()): boolean {
  return task.status !== 'completed' && Boolean(task.dueDate) && (task.dueDate as string) < today
}

export function isToday(task: Task, today: string = todayStr()): boolean {
  return task.dueDate === today
}
