import { useMemo, useState } from 'react'
import { useLiveQuery } from 'dexie-react-hooks'
import { Eye, EyeOff, Plus, Search, Star, Trash2 } from 'lucide-react'
import {
  Badge,
  Button,
  Card,
  ConfirmDialog,
  EmptyState,
  Field,
  Input,
  Modal,
  PageHeader,
  Select,
  Textarea,
  cx,
  useToast,
} from '../components/ui'
import { useApp } from '../store/AppContext'
import { useYearData } from '../hooks/useYearData'
import { createRow, db, deleteRow, updateRow } from '../lib/db'
import { Markdown } from '../components/Markdown'
import { formatDate } from '../lib/dates'
import type { Note } from '../types'

export default function Notes() {
  const { currentYear } = useApp()
  const { push } = useToast()
  const yearId = currentYear?.id ?? null
  const data = useYearData(yearId)
  const rows = useLiveQuery(async () => (yearId ? db.notes.where({ yearId }).toArray() : []), [yearId])
  const [query, setQuery] = useState('')
  const [editing, setEditing] = useState<Note | null>(null)
  const [formOpen, setFormOpen] = useState(false)
  const [confirmId, setConfirmId] = useState<string | null>(null)
  const [openId, setOpenId] = useState<string | null>(null)

  const list = useMemo(() => {
    const q = query.trim().toLowerCase()
    return (rows ?? [])
      .filter((n) => !q || [n.title, n.body, n.category].join(' ').toLowerCase().includes(q))
      .sort((a, b) => b.updatedAt.localeCompare(a.updatedAt))
  }, [rows, query])

  const subjectName = (id?: string) => data?.subjects.find((s) => s.id === id)?.name ?? ''

  return (
    <div>
      <PageHeader
        title="Notes"
        subtitle={`${rows?.length ?? 0} notes`}
        action={
          <Button size="sm" onClick={() => { setEditing(null); setFormOpen(true) }}>
            <Plus size={14} /> Note
          </Button>
        }
      />

      <div className="relative mb-3">
        <Search size={14} className="absolute left-2.5 top-1/2 -translate-y-1/2 text-muted" />
        <Input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Search notes…" className="pl-8" />
      </div>

      {list.length === 0 ? (
        <EmptyState title="No notes yet" hint="Write notes with headings, lists, tables and $maths$." action={<Button onClick={() => setFormOpen(true)}>New note</Button>} />
      ) : (
        <div className="grid gap-2 sm:grid-cols-2">
          {list.map((n) => {
            const open = openId === n.id
            return (
              <Card key={n.id} className="p-3">
                <div className="flex items-start justify-between gap-2">
                  <button className="min-w-0 text-left" onClick={() => setOpenId(open ? null : n.id)}>
                    <p className="truncate text-sm font-medium">{n.title}</p>
                    <p className="text-[10px] text-muted">
                      {subjectName(n.subjectId) || 'General'} · {formatDate(n.updatedAt.slice(0, 10))}
                      {n.category ? ` · ${n.category}` : ''}
                    </p>
                  </button>
                  <div className="flex items-center gap-0.5">
                    <button onClick={() => void updateRow('notes', n.id, { favourite: !n.favourite })} aria-label="Favourite" className={cx('p-1', n.favourite ? 'text-amber-400' : 'text-muted')}>
                      <Star size={12} />
                    </button>
                    <button onClick={() => setOpenId(open ? null : n.id)} aria-label={open ? 'Collapse' : 'Expand'} className="p-1 text-muted">
                      {open ? <EyeOff size={12} /> : <Eye size={12} />}
                    </button>
                    <button onClick={() => { setEditing(n); setFormOpen(true) }} aria-label="Edit" className="p-1 text-muted">
                      <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M12 20h9" /><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4Z" /></svg>
                    </button>
                    <button onClick={() => setConfirmId(n.id)} aria-label="Delete" className="p-1 text-muted hover:text-red-400">
                      <Trash2 size={12} />
                    </button>
                  </div>
                </div>
                {open ? (
                  <div className="mt-2 border-t border-line pt-2">
                    <Markdown source={n.body} />
                  </div>
                ) : (
                  <p className="mt-1.5 line-clamp-2 text-[11px] text-muted">{n.body.replace(/[#*$`]/g, '').slice(0, 160)}</p>
                )}
                {n.favourite ? <Badge tone="warn">Favourite</Badge> : null}
              </Card>
            )
          })}
        </div>
      )}

      <NoteForm
        open={formOpen}
        entry={editing}
        yearId={yearId}
        subjects={data?.subjects ?? []}
        onClose={() => setFormOpen(false)}
      />

      <ConfirmDialog
        open={Boolean(confirmId)}
        title="Delete note?"
        message="This note will be removed permanently."
        onCancel={() => setConfirmId(null)}
        onConfirm={async () => {
          if (confirmId) await deleteRow('notes', confirmId)
          setConfirmId(null)
          push('Note deleted.', 'success')
        }}
      />
    </div>
  )
}

function NoteForm({
  open,
  entry,
  yearId,
  subjects,
  onClose,
}: {
  open: boolean
  entry: Note | null
  yearId: string | null
  subjects: Array<{ id: string; name: string }>
  onClose: () => void
}) {
  const { push } = useToast()
  const [title, setTitle] = useState(entry?.title ?? '')
  const [body, setBody] = useState(entry?.body ?? '')
  const [category, setCategory] = useState(entry?.category ?? '')
  const [subjectId, setSubjectId] = useState(entry?.subjectId ?? '')
  const [preview, setPreview] = useState(false)

  const key = `${open}-${entry?.id ?? 'new'}`
  const [lastKey, setLastKey] = useState(key)
  if (key !== lastKey) {
    setLastKey(key)
    setTitle(entry?.title ?? '')
    setBody(entry?.body ?? '')
    setCategory(entry?.category ?? '')
    setSubjectId(entry?.subjectId ?? '')
    setPreview(false)
  }

  const submit = async () => {
    if (!title.trim()) {
      push('A title is required.', 'error')
      return
    }
    const payload = { title: title.trim(), body, category, subjectId: subjectId || undefined }
    if (entry) {
      await updateRow('notes', entry.id, payload)
      push('Note saved.', 'success')
    } else {
      if (!yearId) return
      await createRow('notes', { ...payload, yearId })
      push('Note created.', 'success')
    }
    onClose()
  }

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={entry ? 'Edit note' : 'New note'}
      wide
      footer={
        <>
          <Button variant="ghost" onClick={() => setPreview((p) => !p)}>
            {preview ? 'Edit' : 'Preview'}
          </Button>
          <Button variant="ghost" onClick={onClose}>
            Cancel
          </Button>
          <Button onClick={() => void submit()}>Save</Button>
        </>
      }
    >
      <div className="space-y-3">
        <Field label="Title *">
          <Input value={title} onChange={(e) => setTitle(e.target.value)} />
        </Field>
        <div className="grid grid-cols-2 gap-3">
          <Field label="Subject">
            <Select value={subjectId} onChange={(e) => setSubjectId(e.target.value)}>
              <option value="">General</option>
              {subjects.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.name}
                </option>
              ))}
            </Select>
          </Field>
          <Field label="Category">
            <Input value={category} onChange={(e) => setCategory(e.target.value)} placeholder="revision / summary" />
          </Field>
        </div>
        {preview ? (
          <div className="min-h-[220px] rounded-lg border border-line p-3">
            <Markdown source={body} />
          </div>
        ) : (
          <Field label="Body" hint="Markdown: # heading, **bold**, *italic*, - list, | tables |, $maths$">
            <Textarea value={body} onChange={(e) => setBody(e.target.value)} className="min-h-[240px] font-mono text-xs" />
          </Field>
        )}
      </div>
    </Modal>
  )
}
