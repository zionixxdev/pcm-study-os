/** Small date helpers. All app dates are stored as ISO strings (YYYY-MM-DD for
 * date-only fields, full ISO for timestamps) so they sort lexicographically. */

export function nowIso(): string {
  return new Date().toISOString()
}

/** Local (not UTC) calendar date as YYYY-MM-DD. */
export function todayStr(d: Date = new Date()): string {
  const y = d.getFullYear()
  const m = `${d.getMonth() + 1}`.padStart(2, '0')
  const day = `${d.getDate()}`.padStart(2, '0')
  return `${y}-${m}-${day}`
}

export function addDays(dateStr: string, days: number): string {
  const [y, m, d] = dateStr.split('-').map(Number)
  const dt = new Date(y, (m ?? 1) - 1, d ?? 1)
  dt.setDate(dt.getDate() + days)
  return todayStr(dt)
}

export function daysBetween(from: string, to: string): number {
  const a = new Date(`${from}T00:00:00`)
  const b = new Date(`${to}T00:00:00`)
  return Math.round((b.getTime() - a.getTime()) / 86400000)
}

export function isBefore(a: string, b: string): boolean {
  return a < b
}

/** Monday-first weekday index 0..6 */
export function weekdayIndex(dateStr: string): number {
  const [y, m, d] = dateStr.split('-').map(Number)
  const wd = new Date(y, (m ?? 1) - 1, d ?? 1).getDay()
  return (wd + 6) % 7
}

export function formatDate(dateStr?: string): string {
  if (!dateStr) return '—'
  const [y, m, d] = dateStr.slice(0, 10).split('-').map(Number)
  if (!y || !m || !d) return dateStr
  return `${`${d}`.padStart(2, '0')}/${`${m}`.padStart(2, '0')}/${y}`
}

export function formatMinutes(min: number): string {
  if (!min || min <= 0) return '0m'
  const h = Math.floor(min / 60)
  const m = Math.round(min % 60)
  if (h === 0) return `${m}m`
  if (m === 0) return `${h}h`
  return `${h}h ${m}m`
}

export function startOfWeek(dateStr: string): string {
  return addDays(dateStr, -weekdayIndex(dateStr))
}

export function monthKey(dateStr: string): string {
  return dateStr.slice(0, 7)
}
