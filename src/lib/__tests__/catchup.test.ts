import { describe, expect, it } from 'vitest'
import { planCatchup, summarisePlan, overdueTasks } from '../catchup'
import type { Task } from '../../types'

const base = { userId: 'u', createdAt: '2025-01-01T00:00:00Z', updatedAt: '2025-01-01T00:00:00Z', yearId: 'y1', recurring: 'none' as const, order: 0 }

function task(id: string, dueDate: string | undefined, priority: Task['priority'], minutes = 60, status: Task['status'] = 'pending'): Task {
  return { ...base, id, title: id, priority, status, dueDate, estimatedMinutes: minutes }
}

describe('planCatchup', () => {
  it('fills a day up to capacity and spills to the next day', () => {
    const tasks = [task('a', undefined, 'high', 120), task('b', undefined, 'high', 120), task('c', undefined, 'medium', 120)]
    const plan = planCatchup({ today: '2026-01-05', tasks, dailyCapacityMinutes: 240, studyDays: [] })
    expect(plan).toHaveLength(3)
    expect(plan[0].date).toBe('2026-01-05')
    expect(plan[1].date).toBe('2026-01-05')
    expect(plan[2].date).toBe('2026-01-06')
    expect(plan.every((p) => !p.atRisk)).toBe(true)
  })

  it('never exceeds the daily capacity', () => {
    const tasks = Array.from({ length: 6 }, (_, i) => task(`t${i}`, undefined, 'medium', 60))
    const plan = planCatchup({ today: '2026-01-05', tasks, dailyCapacityMinutes: 120, studyDays: [] })
    const summary = summarisePlan(plan, 120)
    expect(summary.every((d) => d.planned <= 120)).toBe(true)
  })

  it('skips non-study days', () => {
    // 2026-01-05 is a Monday (weekday index 0). Study only on Saturdays (5).
    const plan = planCatchup({ today: '2026-01-05', tasks: [task('a', undefined, 'high', 60)], dailyCapacityMinutes: 240, studyDays: [5] })
    expect(plan[0].date).toBe('2026-01-10')
  })

  it('sorts by deadline then priority', () => {
    const tasks = [task('later', '2026-02-01', 'high', 30), task('soon-low', '2026-01-06', 'low', 30), task('soon-high', '2026-01-06', 'high', 30)]
    const plan = planCatchup({ today: '2026-01-05', tasks, dailyCapacityMinutes: 240, studyDays: [] })
    expect(plan.map((p) => p.taskId)).toEqual(['soon-high', 'soon-low', 'later'])
  })

  it('flags work that cannot fit before its deadline', () => {
    const tasks = [task('big', '2026-01-05', 'high', 300)]
    const plan = planCatchup({ today: '2026-01-05', tasks, dailyCapacityMinutes: 120, studyDays: [] })
    expect(plan[0].atRisk).toBe(true)
  })

  it('ignores completed tasks and is deterministic', () => {
    const tasks = [task('done', undefined, 'high', 60, 'completed'), task('open', undefined, 'high', 60)]
    const a = planCatchup({ today: '2026-01-05', tasks, dailyCapacityMinutes: 120, studyDays: [] })
    const b = planCatchup({ today: '2026-01-05', tasks, dailyCapacityMinutes: 120, studyDays: [] })
    expect(a).toEqual(b)
    expect(a.map((p) => p.taskId)).toEqual(['open'])
  })
})

describe('overdueTasks', () => {
  it('finds unfinished tasks past their due date', () => {
    const tasks = [task('late', '2026-01-01', 'high'), task('done', '2026-01-01', 'high', 60, 'completed'), task('future', '2026-03-01', 'high')]
    expect(overdueTasks(tasks, '2026-01-05').map((t) => t.id)).toEqual(['late'])
  })
})
