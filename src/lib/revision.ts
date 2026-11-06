import { addDays } from './dates'
import type { RevisionItem, RevisionRating, RevisionStatus } from '../types'

export const DEFAULT_INTERVALS = [1, 3, 7, 14, 30]

/** Maps a self-reported recall rating onto the next interval slot. */
export function nextIntervalIndex(current: number, rating: RevisionRating, intervalsLength = DEFAULT_INTERVALS.length): number {
  const last = Math.max(0, intervalsLength - 1)
  switch (rating) {
    case 'again':
      return 0
    case 'hard':
      return Math.min(Math.max(0, current), last)
    case 'good':
      return Math.min(current + 1, last)
    case 'easy':
      return Math.min(current + 2, last)
    default:
      return Math.min(Math.max(0, current), last)
  }
}

export function nextRevisionDate(from: string, intervals: number[], intervalIndex: number): string {
  const list = intervals.length ? intervals : DEFAULT_INTERVALS
  const idx = Math.min(Math.max(0, intervalIndex), list.length - 1)
  return addDays(from, list[idx])
}

export function revisionStatus(item: Pick<RevisionItem, 'dueDate' | 'status' | 'completedAt'>, today: string): RevisionStatus {
  if (item.status === 'completed' || item.completedAt) return 'completed'
  if (item.dueDate < today) return 'overdue'
  if (item.dueDate === today) return 'due'
  return 'upcoming'
}

export function effectiveStatus(item: RevisionItem, today: string): RevisionStatus {
  return revisionStatus(item, today)
}
