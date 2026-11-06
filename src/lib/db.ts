import Dexie, { type Table } from 'dexie'
import { nowIso } from './dates'
import type {
  AcademicYear,
  Base,
  Chapter,
  DashboardPrefs,
  Exam,
  FormulaEntry,
  Goal,
  ImportHistoryEntry,
  Mistake,
  Note,
  PracticeQuestion,
  Resource,
  RevisionHistoryEntry,
  RevisionItem,
  Settings,
  StudyComponent,
  StudySession,
  Subject,
  Subtopic,
  Task,
  Topic,
} from '../types'

export interface RowMap {
  academicYears: AcademicYear
  subjects: Subject
  chapters: Chapter
  topics: Topic
  subtopics: Subtopic
  components: StudyComponent
  tasks: Task
  sessions: StudySession
  revisions: RevisionItem
  revisionHistory: RevisionHistoryEntry
  formulas: FormulaEntry
  practice: PracticeQuestion
  mistakes: Mistake
  exams: Exam
  notes: Note
  resources: Resource
  goals: Goal
  importHistory: ImportHistoryEntry
}

export type DataTableName = keyof RowMap
export type Row<K extends DataTableName> = RowMap[K]

export interface OutboxEntry {
  seq?: number
  table: string
  rowId: string
  op: 'upsert' | 'delete'
  at: string
}

export interface MetaEntry {
  key: string
  value: string
}

export const DATA_TABLES: DataTableName[] = [
  'academicYears',
  'subjects',
  'chapters',
  'topics',
  'subtopics',
  'components',
  'tasks',
  'sessions',
  'revisions',
  'revisionHistory',
  'formulas',
  'practice',
  'mistakes',
  'exams',
  'notes',
  'resources',
  'goals',
  'importHistory',
]

export class AppDB extends Dexie {
  academicYears!: Table<AcademicYear, string>
  subjects!: Table<Subject, string>
  chapters!: Table<Chapter, string>
  topics!: Table<Topic, string>
  subtopics!: Table<Subtopic, string>
  components!: Table<StudyComponent, string>
  tasks!: Table<Task, string>
  sessions!: Table<StudySession, string>
  revisions!: Table<RevisionItem, string>
  revisionHistory!: Table<RevisionHistoryEntry, string>
  formulas!: Table<FormulaEntry, string>
  practice!: Table<PracticeQuestion, string>
  mistakes!: Table<Mistake, string>
  exams!: Table<Exam, string>
  notes!: Table<Note, string>
  resources!: Table<Resource, string>
  goals!: Table<Goal, string>
  importHistory!: Table<ImportHistoryEntry, string>
  settings!: Table<Settings, string>
  dashboardPrefs!: Table<DashboardPrefs, string>
  outbox!: Table<OutboxEntry, number>
  meta!: Table<MetaEntry, string>

  constructor() {
    super('pcm-study-os')
    this.version(1).stores({
      academicYears: 'id, name, isCurrent, archived',
      subjects: 'id, yearId, name, order, archived',
      chapters: 'id, subjectId, name, order, archived',
      topics: 'id, chapterId, name, order, status, archived',
      subtopics: 'id, topicId, order',
      components: 'id, topicId, type',
      tasks: 'id, yearId, status, dueDate, priority, subjectId, chapterId, topicId',
      sessions: 'id, yearId, startedAt, subjectId, topicId',
      revisions: 'id, yearId, dueDate, status, topicId',
      revisionHistory: 'id, revisionId, at',
      formulas: 'id, yearId, subjectId, chapterId, title, category',
      practice: 'id, yearId, subjectId, chapterId, topicId, result, attemptDate',
      mistakes: 'id, yearId, subjectId, category, resolved, date',
      exams: 'id, yearId, date, type, subjectId',
      notes: 'id, yearId, subjectId, chapterId, topicId, title, category',
      resources: 'id, yearId, subjectId, chapterId, topicId',
      goals: 'id, yearId, kind, status, deadline',
      importHistory: 'id, at, entity',
      settings: 'id',
      dashboardPrefs: 'id',
      outbox: '++seq, table, rowId, at, [table+rowId]',
      meta: 'key',
    })
  }
}

export const db = new AppDB()

/* ------------------------------------------------------------------ */
/* Identity: the owner of locally created rows.                        */
/* ------------------------------------------------------------------ */

export const LOCAL_USER = 'local-user'
let currentUserId = LOCAL_USER

export function setCurrentUserId(id: string | null | undefined): void {
  currentUserId = id || LOCAL_USER
}
export function getCurrentUserId(): string {
  return currentUserId
}

/* ------------------------------------------------------------------ */
/* Writes. Every mutation stamps timestamps and queues a sync op.      */
/* ------------------------------------------------------------------ */

function dataTable(name: DataTableName) {
  return db.table(name) as unknown as Table<Base, string>
}

export async function createRow<K extends DataTableName>(
  name: K,
  data: Omit<Row<K>, keyof Base> & Partial<Pick<Base, 'id' | 'archived'>>,
): Promise<string> {
  const id = (data.id as string | undefined) ?? crypto.randomUUID()
  const ts = nowIso()
  const row = {
    ...data,
    id,
    userId: currentUserId,
    createdAt: ts,
    updatedAt: ts,
  } as unknown as Base
  await db.transaction('rw', dataTable(name), db.outbox, async () => {
    await dataTable(name).put(row)
    await enqueue(name, id, 'upsert')
  })
  return id
}

export async function createRows<K extends DataTableName>(
  name: K,
  rows: Array<Omit<Row<K>, keyof Base> & Partial<Pick<Base, 'id' | 'archived'>>>,
): Promise<string[]> {
  const ts = nowIso()
  const prepared = rows.map((data) => ({
    ...data,
    id: (data.id as string | undefined) ?? crypto.randomUUID(),
    userId: currentUserId,
    createdAt: ts,
    updatedAt: ts,
  })) as unknown as Base[]
  await db.transaction('rw', dataTable(name), db.outbox, async () => {
    await dataTable(name).bulkPut(prepared)
    await db.outbox.bulkAdd(prepared.map((r) => ({ table: name, rowId: r.id, op: 'upsert' as const, at: ts })))
  })
  return prepared.map((r) => r.id)
}

export async function updateRow<K extends DataTableName>(
  name: K,
  id: string,
  patch: Partial<Row<K>>,
): Promise<void> {
  const table = dataTable(name)
  const ts = nowIso()
  await db.transaction('rw', table, db.outbox, async () => {
    await table.update(id, { ...(patch as object), updatedAt: ts })
    await enqueue(name, id, 'upsert')
  })
}

export async function deleteRow(name: DataTableName, id: string): Promise<void> {
  const table = dataTable(name)
  await db.transaction('rw', table, db.outbox, async () => {
    await table.delete(id)
    await enqueue(name, id, 'delete')
  })
}

async function enqueue(table: string, rowId: string, op: 'upsert' | 'delete'): Promise<void> {
  const existing = await db.outbox.where({ table, rowId }).first()
  const at = nowIso()
  if (existing?.seq != null) {
    // A later local change supersedes the queued op for the same row.
    await db.outbox.update(existing.seq, { op, at })
  } else {
    await db.outbox.add({ table, rowId, op, at })
  }
}

/* ------------------------------------------------------------------ */
/* Cascading deletes (kept explicit so nothing is silently orphaned).  */
/* ------------------------------------------------------------------ */

export async function deleteSubjectCascade(subjectId: string): Promise<void> {
  const chapters = await db.chapters.where({ subjectId }).toArray()
  for (const ch of chapters) await deleteChapterCascade(ch.id)
  await deleteRow('subjects', subjectId)
}

export async function deleteChapterCascade(chapterId: string): Promise<void> {
  const topics = await db.topics.where({ chapterId }).toArray()
  for (const t of topics) await deleteTopicCascade(t.id)
  await deleteRow('chapters', chapterId)
}

export async function deleteTopicCascade(topicId: string): Promise<void> {
  await db.subtopics.where({ topicId }).delete()
  await db.components.where({ topicId }).delete()
  const subs = await db.subtopics.where({ topicId }).toArray()
  await db.outbox.bulkAdd(subs.map((s) => ({ table: 'subtopics', rowId: s.id, op: 'delete' as const, at: nowIso() })))
  const comps = await db.components.where({ topicId }).toArray()
  await db.outbox.bulkAdd(comps.map((c) => ({ table: 'components', rowId: c.id, op: 'delete' as const, at: nowIso() })))
  await deleteRow('topics', topicId)
}

/* ------------------------------------------------------------------ */
/* Meta helpers                                                        */
/* ------------------------------------------------------------------ */

export async function getMeta(key: string): Promise<string | null> {
  const row = await db.meta.get(key)
  return row?.value ?? null
}

export async function setMeta(key: string, value: string): Promise<void> {
  await db.meta.put({ key, value })
}

export async function clearAllData(): Promise<void> {
  await db.transaction('rw', [...DATA_TABLES.map((t) => db.table(t)), db.outbox, db.meta], async () => {
    for (const t of DATA_TABLES) await db.table(t).clear()
    await db.outbox.clear()
    await db.meta.clear()
  })
}
