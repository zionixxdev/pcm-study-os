import { useLiveQuery } from 'dexie-react-hooks'
import { db } from '../lib/db'
import { formatMinutes, todayStr } from '../lib/dates'
import type { Goal, RevisionItem, StudySession, Task } from '../types'

export function useTasks(yearId: string | null): Task[] | undefined {
  return useLiveQuery(async () => {
    if (!yearId) return []
    const rows = await db.tasks.where({ yearId }).toArray()
    return rows.sort((a, b) => (a.dueDate ?? '9999').localeCompare(b.dueDate ?? '9999'))
  }, [yearId])
}

export function useSessions(yearId: string | null): StudySession[] | undefined {
  return useLiveQuery(async () => {
    if (!yearId) return []
    return db.sessions.where({ yearId }).toArray()
  }, [yearId])
}

export function useRevisions(yearId: string | null): RevisionItem[] | undefined {
  return useLiveQuery(async () => {
    if (!yearId) return []
    return db.revisions.where({ yearId }).toArray()
  }, [yearId])
}

export function useGoals(yearId: string | null): Goal[] | undefined {
  return useLiveQuery(async () => {
    if (!yearId) return []
    return db.goals.where({ yearId }).toArray()
  }, [yearId])
}

export interface StudyStats {
  todayMinutes: number
  weekMinutes: number
  monthMinutes: number
  totalMinutes: number
  avgDailyMinutes: number
  activeDays: number
  currentStreak: number
  longestStreak: number
  last7: Array<{ date: string; minutes: number }>
  bySubject: Record<string, number>
}

export function computeStats(sessions: StudySession[]): StudyStats {
  const today = todayStr()
  const byDate = new Map<string, number>()
  const bySubject: Record<string, number> = {}
  for (const s of sessions) {
    const date = s.startedAt.slice(0, 10)
    byDate.set(date, (byDate.get(date) ?? 0) + s.minutes)
    if (s.subjectId) bySubject[s.subjectId] = (bySubject[s.subjectId] ?? 0) + s.minutes
  }

  const totalMinutes = sessions.reduce((n, s) => n + s.minutes, 0)
  const todayMinutes = byDate.get(today) ?? 0
  const weekStart = shiftDate(today, -6)
  const monthStart = today.slice(0, 7) + '-01'
  let weekMinutes = 0
  let monthMinutes = 0
  for (const [date, minutes] of byDate) {
    if (date >= weekStart) weekMinutes += minutes
    if (date >= monthStart) monthMinutes += minutes
  }

  const activeDays = [...byDate.keys()].filter((d) => (byDate.get(d) ?? 0) > 0).length
  const avgDailyMinutes = activeDays ? Math.round(totalMinutes / activeDays) : 0

  // Streak counts back from today (or yesterday if today has no session yet).
  let cursor = today
  if (!byDate.get(cursor)) cursor = shiftDate(today, -1)
  let currentStreak = 0
  while ((byDate.get(cursor) ?? 0) > 0) {
    currentStreak++
    cursor = shiftDate(cursor, -1)
  }

  const sortedDays = [...byDate.keys()].sort()
  let longestStreak = 0
  let run = 0
  let prev: string | null = null
  for (const day of sortedDays) {
    if ((byDate.get(day) ?? 0) <= 0) continue
    run = prev && shiftDate(prev, 1) === day ? run + 1 : 1
    longestStreak = Math.max(longestStreak, run)
    prev = day
  }

  const last7: Array<{ date: string; minutes: number }> = []
  for (let i = 6; i >= 0; i--) {
    const d = shiftDate(today, -i)
    last7.push({ date: d, minutes: byDate.get(d) ?? 0 })
  }

  return { todayMinutes, weekMinutes, monthMinutes, totalMinutes, avgDailyMinutes, activeDays, currentStreak, longestStreak, last7, bySubject }
}

function shiftDate(date: string, days: number): string {
  const [y, m, d] = date.split('-').map(Number)
  const dt = new Date(y, (m ?? 1) - 1, d ?? 1)
  dt.setDate(dt.getDate() + days)
  return todayStr(dt)
}

export { formatMinutes }
