const toSnake = (s: string) => s.replace(/[A-Z]/g, (c) => `_${c.toLowerCase()}`)
const toCamel = (s: string) => s.replace(/_([a-z0-9])/g, (_, c: string) => c.toUpperCase())

export function camelToSnakeKey(k: string): string {
  return toSnake(k)
}
export function snakeToCamelKey(k: string): string {
  return toCamel(k)
}

export function toSnakeKeys<T extends Record<string, unknown>>(obj: T): Record<string, unknown> {
  const out: Record<string, unknown> = {}
  for (const [k, v] of Object.entries(obj)) out[toSnake(k)] = v
  return out
}

export function toCamelKeys<T = Record<string, unknown>>(obj: Record<string, unknown>): T {
  const out: Record<string, unknown> = {}
  for (const [k, v] of Object.entries(obj)) out[toCamel(k)] = v
  return out as T
}

/**
 * Maps an app table name to its Postgres table name.
 *
 * Tables are prefixed with `pcm_` so this app can share a Supabase project with
 * other applications without name collisions (profiles, tasks, user_settings and
 * messages are common names). Keep in sync with supabase/migrations.
 */
export function tableToDb(table: string): string {
  return `pcm_${toSnake(table)}`
}
