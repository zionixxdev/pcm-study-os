import { addDays, daysBetween, todayStr, weekdayIndex } from './dates'
import type { Task } from '../types'

/** Deterministic catch-up planner — no AI, no randomness, same input => same
 * output. It distributes unfinished work across the user's study days without
 * exceeding their daily capacity and flags work that will miss its deadline. */

export interface CatchupInput {
  today: string
  tasks: Task[]
  dailyCapacityMinutes: number
  studyDays: number[]
  horizonDays?: number
}

export interface CatchupSuggestion {
  taskId: string
  title: string
  date: string
  minutes: number
  atRisk: boolean
  reason: string
}

const PRIORITY_WEIGHT: Record<Task['priority'], number> = { high: 0, medium: 1, low: 2 }

export function planCatchup(input: CatchupInput): CatchupSuggestion[] {
  const { today, tasks, dailyCapacityMinutes, studyDays } = input
  const horizon = input.horizonDays ?? 14
  const capacity = Math.max(15, dailyCapacityMinutes || 0)

  const candidates = tasks
    .filter((t) => t.status !== 'completed' && !t.archived)
    .sort((a, b) => {
      const dueA = a.dueDate ?? '9999-12-31'
      const dueB = b.dueDate ?? '9999-12-31'
      if (dueA !== dueB) return dueA < dueB ? -1 : 1
      const pw = PRIORITY_WEIGHT[a.priority] - PRIORITY_WEIGHT[b.priority]
      if (pw !== 0) return pw
      if (a.order !== b.order) return a.order - b.order
      return a.createdAt < b.createdAt ? -1 : 1
    })

  const days: string[] = []
  for (let i = 0; i < horizon; i++) {
    const day = addDays(today, i)
    if (studyDays.length === 0 || studyDays.includes(weekdayIndex(day))) days.push(day)
  }
  if (days.length === 0) days.push(today)

  const load = new Map<string, number>(days.map((d) => [d, 0]))
  const suggestions: CatchupSuggestion[] = []

  for (const task of candidates) {
    const minutes = Math.max(5, task.estimatedMinutes || 30)
    const deadline = task.dueDate
    let chosen: string | null = null

    for (const day of days) {
      const used = load.get(day) ?? 0
      if (used + minutes > capacity) continue
      if (deadline && daysBetween(day, deadline) < 0) continue
      chosen = day
      break
    }

    let reason = 'Fits inside your daily capacity.'
    let atRisk = false

    if (!chosen) {
      // Nothing fits without overflow: take the earliest day that still meets
      // the deadline, else the first day, and mark the task as at risk.
      const beforeDeadline = deadline ? days.filter((d) => daysBetween(d, deadline) >= 0) : days
      chosen = beforeDeadline[0] ?? days[0]
      reason = deadline ? 'Daily capacity is full — this may miss its deadline.' : 'Daily capacity is full — scheduled as overflow.'
      atRisk = true
    } else if (deadline) {
      const slack = daysBetween(chosen, deadline)
      if (slack <= 1) reason = 'Scheduled close to its deadline.'
    }

    load.set(chosen, (load.get(chosen) ?? 0) + minutes)
    suggestions.push({ taskId: task.id, title: task.title, date: chosen, minutes, atRisk, reason })
  }

  return suggestions
}

export interface DayLoad {
  date: string
  planned: number
  capacity: number
}

export function summarisePlan(suggestions: CatchupSuggestion[], capacity: number): DayLoad[] {
  const map = new Map<string, number>()
  for (const s of suggestions) map.set(s.date, (map.get(s.date) ?? 0) + s.minutes)
  return [...map.entries()]
    .sort((a, b) => (a[0] < b[0] ? -1 : 1))
    .map(([date, planned]) => ({ date, planned, capacity }))
}

export function overdueTasks(tasks: Task[], today: string = todayStr()): Task[] {
  return tasks.filter((t) => t.status !== 'completed' && t.dueDate && t.dueDate < today)
}
