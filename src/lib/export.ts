import jsPDF from 'jspdf'
import autoTable from 'jspdf-autotable'
import { db, DATA_TABLES, createRow, deleteRow, updateRow, type DataTableName } from './db'
import { nowIso } from './dates'

export interface Column<T> {
  key: keyof T | string
  label: string
  get?: (row: T) => unknown
}

function cell(value: unknown): string {
  if (value === null || value === undefined) return ''
  if (Array.isArray(value)) return value.join('; ')
  if (typeof value === 'boolean') return value ? 'yes' : 'no'
  return String(value)
}

export function rowsToCsv<T extends Record<string, unknown>>(rows: T[], columns: Column<T>[]): string {
  const escape = (v: string) => (/[",\n]/.test(v) ? `"${v.replace(/"/g, '""')}"` : v)
  const head = columns.map((c) => escape(c.label)).join(',')
  const body = rows.map((row) =>
    columns
      .map((c) => escape(cell(c.get ? c.get(row) : row[c.key as keyof T])))
      .join(','),
  )
  return [head, ...body].join('\n')
}

export function downloadBlob(filename: string, blob: Blob): void {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  a.remove()
  setTimeout(() => URL.revokeObjectURL(url), 2000)
}

export function exportCsv<T extends Record<string, unknown>>(filename: string, rows: T[], columns: Column<T>[]): void {
  const csv = rowsToCsv(rows, columns)
  downloadBlob(filename, new Blob([`\ufeff${csv}`], { type: 'text/csv;charset=utf-8;' }))
}

export interface PdfSection {
  heading: string
  columns: string[]
  rows: (string | number)[][]
  note?: string
}

export function exportPdf(title: string, subtitle: string, sections: PdfSection[]): void {
  const doc = new jsPDF({ unit: 'pt', format: 'a4' })
  const pageWidth = doc.internal.pageSize.getWidth()

  doc.setFontSize(18)
  doc.text(title, 40, 50)
  doc.setFontSize(10)
  doc.setTextColor(110)
  doc.text(subtitle, 40, 68)
  doc.setTextColor(20)

  let cursor = 90
  for (const section of sections) {
    if (cursor > doc.internal.pageSize.getHeight() - 90) {
      doc.addPage()
      cursor = 60
    }
    doc.setFontSize(12)
    doc.text(section.heading, 40, cursor)
    cursor += 8
    autoTable(doc, {
      head: [section.columns],
      body: section.rows.length ? section.rows.map((r) => r.map((c) => String(c))) : [['No records yet', ...section.columns.slice(1).map(() => '')]],
      startY: cursor,
      margin: { left: 40, right: 40 },
      styles: { fontSize: 9, cellPadding: 4 },
      headStyles: { fillColor: [30, 41, 59], textColor: 255 },
      alternateRowStyles: { fillColor: [245, 247, 250] },
    })
    const finalY = (doc as unknown as { lastAutoTable?: { finalY: number } }).lastAutoTable?.finalY ?? cursor
    cursor = finalY + 26
    if (section.note) {
      doc.setFontSize(9)
      doc.setTextColor(110)
      doc.text(section.note, 40, cursor)
      doc.setTextColor(20)
      cursor += 16
    }
  }

  const pages = doc.getNumberOfPages()
  for (let i = 1; i <= pages; i++) {
    doc.setPage(i)
    doc.setFontSize(8)
    doc.setTextColor(140)
    doc.text(`PCM Study OS · page ${i} of ${pages} · generated ${new Date().toLocaleString()}`, 40, doc.internal.pageSize.getHeight() - 20)
    doc.text(`width ${Math.round(pageWidth)}`, 0, 0)
  }

  doc.save(`${title.replace(/[^a-z0-9]+/gi, '-').toLowerCase()}.pdf`)
}

/* ------------------------------------------------------------------ */
/* JSON backup                                                         */
/* ------------------------------------------------------------------ */

export interface BackupFile {
  app: 'pcm-study-os'
  version: 1
  exportedAt: string
  tables: Record<string, unknown[]>
}

export async function buildBackup(): Promise<BackupFile> {
  const tables: Record<string, unknown[]> = {}
  for (const name of DATA_TABLES) {
    tables[name] = await db.table(name).toArray()
  }
  return { app: 'pcm-study-os', version: 1, exportedAt: nowIso(), tables }
}

export async function downloadBackup(): Promise<void> {
  const backup = await buildBackup()
  downloadBlob(`pcm-study-os-backup-${backup.exportedAt.slice(0, 10)}.json`, new Blob([JSON.stringify(backup, null, 2)], { type: 'application/json' }))
}

export interface BackupValidation {
  ok: boolean
  error?: string
  counts: Record<string, number>
  total: number
}

export function validateBackup(raw: unknown): BackupValidation {
  const counts: Record<string, number> = {}
  const obj = raw as Partial<BackupFile> | null
  if (!obj || typeof obj !== 'object') return { ok: false, error: 'The file is not valid JSON object.', counts, total: 0 }
  if (obj.app !== 'pcm-study-os') return { ok: false, error: 'This file was not produced by PCM Study OS.', counts, total: 0 }
  if (!obj.tables || typeof obj.tables !== 'object') return { ok: false, error: 'The backup has no data tables.', counts, total: 0 }
  let total = 0
  for (const name of DATA_TABLES) {
    const rows = (obj.tables as Record<string, unknown[]>)[name]
    if (Array.isArray(rows)) {
      counts[name] = rows.length
      total += rows.length
    }
  }
  if (total === 0) return { ok: false, error: 'The backup contains no records.', counts, total }
  return { ok: true, counts, total }
}

/** merge: adds records with ids that do not exist yet, keeps existing rows.
 *  replace: clears the local tables first (confirmation is the caller's job). */
export async function importBackup(backup: BackupFile, mode: 'merge' | 'replace'): Promise<{ inserted: number; skipped: number }> {
  let inserted = 0
  let skipped = 0
  for (const name of DATA_TABLES) {
    const rows = backup.tables[name]
    if (!Array.isArray(rows)) continue
    const table = db.table(name as DataTableName)
    if (mode === 'replace') {
      await table.clear()
    }
    for (const row of rows as Array<Record<string, unknown>>) {
      const id = row.id as string | undefined
      if (!id) {
        skipped++
        continue
      }
      const existing = await table.get(id)
      if (existing) {
        if (mode === 'merge') {
          skipped++
          continue
        }
        await updateRow(name as DataTableName, id, row as never)
      } else {
        await createRow(name as DataTableName, row as never)
      }
      inserted++
    }
  }
  return { inserted, skipped }
}

export async function exportTableCsv(name: DataTableName, filename: string, columns: Column<Record<string, unknown>>[]): Promise<void> {
  const rows = (await db.table(name).toArray()) as Record<string, unknown>[]
  exportCsv(filename, rows, columns)
}

export { deleteRow }
