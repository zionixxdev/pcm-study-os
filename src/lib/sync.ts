import { db, DATA_TABLES, getMeta, setMeta } from './db'
import { nowIso } from './dates'
import { supabase, supabaseConfigured } from './supabase'
import { tableToDb, toSnakeKeys, toCamelKeys } from './keys'

export type SyncState = 'synced' | 'pending' | 'syncing' | 'failed' | 'offline' | 'disabled'

export interface SyncStatus {
  state: SyncState
  pending: number
  lastSyncAt: string | null
  message?: string
}

let status: SyncStatus = { state: 'disabled', pending: 0, lastSyncAt: null }
const listeners = new Set<(s: SyncStatus) => void>()

export function getSyncStatus(): SyncStatus {
  return status
}

export function subscribeSync(fn: (s: SyncStatus) => void): () => void {
  listeners.add(fn)
  fn(status)
  return () => listeners.delete(fn)
}

function setStatus(patch: Partial<SyncStatus>): void {
  status = { ...status, ...patch }
  for (const fn of listeners) fn(status)
}

export async function refreshPendingCount(): Promise<void> {
  const pending = await db.outbox.count()
  setStatus({ pending })
  if (status.state === 'disabled' || status.state === 'offline' || status.state === 'syncing') return
  setStatus({ state: pending > 0 ? 'pending' : 'synced' })
}

export async function initSync(): Promise<void> {
  if (!supabaseConfigured) {
    setStatus({ state: 'disabled', lastSyncAt: await getMeta('lastSyncAt') })
    return
  }
  const lastSyncAt = await getMeta('lastSyncAt')
  const pending = await db.outbox.count()
  setStatus({
    state: navigator.onLine ? (pending ? 'pending' : 'synced') : 'offline',
    pending,
    lastSyncAt,
  })
  window.addEventListener('online', () => void fullSync())
  window.addEventListener('offline', () => setStatus({ state: 'offline' }))
}

async function currentUser() {
  if (!supabase) return null
  const { data } = await supabase.auth.getUser()
  return data.user ?? null
}

/** Pushes queued local changes, then pulls remote rows.
 * Conflict rule: a row with a queued local change is never overwritten by the
 * remote copy; otherwise the newer updatedAt wins. Nothing is deleted remotely
 * without a matching local delete op. */
export async function fullSync(): Promise<{ ok: boolean; error?: string }> {
  if (!supabase || !supabaseConfigured) return { ok: false, error: 'Supabase is not configured.' }
  if (!navigator.onLine) {
    setStatus({ state: 'offline' })
    return { ok: false, error: 'You are offline.' }
  }
  const user = await currentUser()
  if (!user) {
    setStatus({ state: 'disabled', message: 'Sign in to enable cloud sync.' })
    return { ok: false, error: 'Not signed in.' }
  }

  setStatus({ state: 'syncing' })
  try {
    const pending = await db.outbox.orderBy('seq').toArray()
    const pendingKeys = new Set(pending.map((p) => `${p.table}:${p.rowId}`))

    for (const op of pending) {
      const table = tableToDb(op.table)
      if (op.op === 'delete') {
        const { error } = await supabase.from(table).delete().eq('id', op.rowId).eq('user_id', user.id)
        if (error) throw new Error(`${table}: ${error.message}`)
      } else {
        const row = await db.table(op.table).get(op.rowId)
        if (!row) {
          if (op.seq != null) await db.outbox.delete(op.seq)
          continue
        }
        const payload = { ...toSnakeKeys(row as Record<string, unknown>), user_id: user.id }
        const { error } = await supabase.from(table).upsert(payload, { onConflict: 'id' })
        if (error) throw new Error(`${table}: ${error.message}`)
      }
      if (op.seq != null) await db.outbox.delete(op.seq)
    }

    for (const name of DATA_TABLES) {
      const table = tableToDb(name)
      const { data, error } = await supabase.from(table).select('*').eq('user_id', user.id)
      if (error) throw new Error(`${table}: ${error.message}`)
      if (!data) continue
      const remote = data.map((r) => toCamelKeys<Record<string, unknown>>(r as Record<string, unknown>))
      const local = await db.table(name).toArray()
      const localById = new Map(local.map((r) => [(r as { id: string }).id, r as Record<string, unknown>]))

      const toWrite: Record<string, unknown>[] = []
      for (const row of remote) {
        const id = row.id as string
        if (!id) continue
        if (pendingKeys.has(`${name}:${id}`)) continue
        const mine = localById.get(id)
        if (!mine) {
          toWrite.push(row)
          continue
        }
        const remoteTs = String(row.updatedAt ?? '')
        const localTs = String(mine.updatedAt ?? '')
        if (remoteTs && remoteTs > localTs) toWrite.push(row)
      }
      if (toWrite.length) {
        await db.table(name).bulkPut(toWrite)
      }

      // Local rows that never reached the cloud get queued for the next push.
      const remoteIds = new Set(remote.map((r) => r.id as string))
      for (const [id] of localById) {
        if (!remoteIds.has(id) && !pendingKeys.has(`${name}:${id}`)) {
          await db.outbox.add({ table: name, rowId: id, op: 'upsert', at: nowIso() })
        }
      }
    }

    const ts = nowIso()
    await setMeta('lastSyncAt', ts)
    const remaining = await db.outbox.count()
    setStatus({ state: remaining ? 'pending' : 'synced', pending: remaining, lastSyncAt: ts, message: undefined })
    return { ok: true }
  } catch (err) {
    const message = err instanceof Error ? err.message : 'Sync failed.'
    const pendingCount = await db.outbox.count()
    setStatus({ state: 'failed', pending: pendingCount, message })
    return { ok: false, error: message }
  }
}
