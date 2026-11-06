import { describe, expect, it } from 'vitest'
import { nextIntervalIndex, nextRevisionDate, revisionStatus } from '../revision'

describe('revision scheduling', () => {
  it('moves forward on good and easy, resets on again', () => {
    expect(nextIntervalIndex(0, 'good')).toBe(1)
    expect(nextIntervalIndex(1, 'good')).toBe(2)
    expect(nextIntervalIndex(2, 'easy')).toBe(4)
    expect(nextIntervalIndex(3, 'again')).toBe(0)
  })

  it('holds the interval on hard', () => {
    expect(nextIntervalIndex(2, 'hard')).toBe(2)
  })

  it('never exceeds the last interval', () => {
    expect(nextIntervalIndex(4, 'easy')).toBe(4)
    expect(nextIntervalIndex(4, 'good')).toBe(4)
  })

  it('computes the next date from the interval list', () => {
    expect(nextRevisionDate('2026-01-01', [1, 3, 7, 14, 30], 0)).toBe('2026-01-02')
    expect(nextRevisionDate('2026-01-01', [1, 3, 7, 14, 30], 2)).toBe('2026-01-08')
    expect(nextRevisionDate('2026-01-01', [1, 3, 7, 14, 30], 4)).toBe('2026-01-31')
  })

  it('falls back to defaults for an empty interval list', () => {
    expect(nextRevisionDate('2026-01-01', [], 1)).toBe('2026-01-04')
  })

  it('classifies due, overdue, upcoming and completed', () => {
    const item = { dueDate: '2026-01-05', status: 'upcoming' as const }
    expect(revisionStatus(item, '2026-01-05')).toBe('due')
    expect(revisionStatus(item, '2026-01-06')).toBe('overdue')
    expect(revisionStatus(item, '2026-01-04')).toBe('upcoming')
    expect(revisionStatus({ dueDate: '2026-01-05', status: 'completed' }, '2026-01-09')).toBe('completed')
  })
})
