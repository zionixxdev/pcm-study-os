import { useMemo, useState } from 'react'
import { useLiveQuery } from 'dexie-react-hooks'
import { Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { Pencil, Plus, Trash2 } from 'lucide-react'
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
  Stat,
  Textarea,
  useToast,
} from '../components/ui'
import { useApp } from '../store/AppContext'
import { useYearData } from '../hooks/useYearData'
import { createRow, db, deleteRow, updateRow } from '../lib/db'
import { formatDate, todayStr } from '../lib/dates'
import type { Exam } from '../types'

const TYPE_LABEL: Record<Exam['type'], string> = {
  unit: 'Unit test',
  monthly: 'Monthly test',
  half_yearly: 'Half-yearly',
  annual: 'Annual',
  mock: 'Mock test',
  custom: 'Custom',
}

export default function Tests() {
  const { currentYear } = useApp()
  const { push } = useToast()
  const yearId = currentYear?.id ?? null
  const data = useYearData(yearId)
  const rows = useLiveQuery(async () => (yearId ? db.exams.where({ yearId }).toArray() : []), [yearId])
  const [form, setForm] = useState<{ open: boolean; entry?: Exam }>({ open: false })
  const [confirmId, setConfirmId] = useState<string | null>(null)
  const today = todayStr()

  const list = (rows ?? []).sort((a, b) => b.date.localeCompare(a.date))
  const graded = list.filter((e) => typeof e.obtainedMarks === 'number' && e.maxMarks > 0)
  const pct = (e: Exam) => (e.maxMarks > 0 && typeof e.obtainedMarks === 'number' ? Math.round((e.obtainedMarks / e.maxMarks) * 100) : null)
  const average = graded.length ? Math.round(graded.reduce((n, e) => n + (pct(e) ?? 0), 0) / graded.length) : 0
  const upcoming = list.filter((e) => e.date >= today).sort((a, b) => a.date.localeCompare(b.date))

  const trend = useMemo(
    () =>
      [...graded]
        .sort((a, b) => a.date.localeCompare(b.date))
        .map((e) => ({ date: e.date.slice(5), score: pct(e) ?? 0, name: e.name })),
    [graded],
  )

  const subjectName = (id?: string) => data?.subjects.find((s) => s.id === id)?.name ?? 'All subjects'

  return (
    <div>
      <PageHeader
        title="Tests & exams"
        subtitle={`${graded.length} graded · average ${average}%`}
        action={
          <Button size="sm" onClick={() => setForm({ open: true })}>
            <Plus size={14} /> Test
          </Button>
        }
      />

      <div className="mb-3 grid grid-cols-2 gap-2 sm:grid-cols-4">
        <Stat label="Tests logged" value={list.length} />
        <Stat label="Average" value={`${average}%`} />
        <Stat label="Best" value={`${graded.length ? Math.max(...graded.map((e) => pct(e) ?? 0)) : 0}%`} />
        <Stat label="Upcoming" value={upcoming.length} />
      </div>

      {trend.length > 1 ? (
        <Card className="mb-3 p-3">
          <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-muted">Score trend</p>
          <div className="h-40 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={trend} margin={{ top: 4, right: 8, left: -22, bottom: 0 }}>
                <XAxis dataKey="date" tick={{ fontSize: 10, fill: 'rgb(var(--c-muted))' }} />
                <YAxis domain={[0, 100]} tick={{ fontSize: 10, fill: 'rgb(var(--c-muted))' }} />
                <Tooltip
                  contentStyle={{ background: 'rgb(var(--c-surface))', border: '1px solid rgb(var(--c-line))', borderRadius: 8, fontSize: 11 }}
                  labelStyle={{ color: 'rgb(var(--c-muted))' }}
                />
                <Line type="monotone" dataKey="score" stroke="rgb(var(--c-accent))" strokeWidth={2} dot={{ r: 2 }} />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </Card>
      ) : null}

      {upcoming.length > 0 ? (
        <Card className="mb-3 p-3">
          <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-muted">Upcoming</p>
          <ul className="space-y-1">
            {upcoming.map((e) => (
              <li key={e.id} className="flex items-center justify-between gap-2 text-xs">
                <span className="truncate">
                  {e.name} <span className="text-muted">· {subjectName(e.subjectId)}</span>
                </span>
                <span className="shrink-0 text-muted">{formatDate(e.date)}</span>
              </li>
            ))}
          </ul>
        </Card>
      ) : null}

      {list.length === 0 ? (
        <EmptyState title="No tests recorded" hint="Log marks after each test to see trends and weak chapters." action={<Button onClick={() => setForm({ open: true })}>Add test</Button>} />
      ) : (
        <ul className="space-y-1.5">
          {list.map((e) => {
            const p = pct(e)
            return (
              <li key={e.id}>
                <Card className="flex items-start gap-2 p-2.5">
                  <div className="min-w-0 flex-1">
                    <p className="flex flex-wrap items-center gap-1.5 text-sm">
                      <span className="truncate">{e.name}</span>
                      <Badge tone="neutral">{TYPE_LABEL[e.type]}</Badge>
                      {p !== null ? <Badge tone={p >= 75 ? 'success' : p >= 50 ? 'warn' : 'danger'}>{p}%</Badge> : <Badge tone="accent">upcoming</Badge>}
                    </p>
                    <p className="text-[11px] text-muted">
                      {subjectName(e.subjectId)} · {formatDate(e.date)}
                      {typeof e.obtainedMarks === 'number' ? ` · ${e.obtainedMarks}/${e.maxMarks}` : ` · max ${e.maxMarks}`}
                      {e.durationMinutes ? ` · ${e.durationMinutes} min` : ''}
                    </p>
                    {e.chaptersCovered.length > 0 ? (
                      <p className="mt-1 line-clamp-1 text-[10px] text-muted">
                        Chapters: {e.chaptersCovered.map((id) => data?.chapters.find((c) => c.id === id)?.name ?? id).join(', ')}
                      </p>
                    ) : null}
                    {e.weakTopics ? <p className="text-[11px] text-amber-400/90">Weak: {e.weakTopics}</p> : null}
                    {e.remarks ? <p className="text-[11px] text-muted">{e.remarks}</p> : null}
                  </div>
                  <div className="flex gap-0.5">
                    <button onClick={() => setForm({ open: true, entry: e })} aria-label="Edit test" className="p-1 text-muted">
                      <Pencil size={12} />
                    </button>
                    <button onClick={() => setConfirmId(e.id)} aria-label="Delete test" className="p-1 text-muted hover:text-red-400">
                      <Trash2 size={12} />
                    </button>
                  </div>
                </Card>
              </li>
            )
          })}
        </ul>
      )}

      <ExamForm open={form.open} entry={form.entry} yearId={yearId} onClose={() => setForm({ open: false })} />

      <ConfirmDialog
        open={Boolean(confirmId)}
        title="Delete test?"
        message="This test record will be removed."
        onCancel={() => setConfirmId(null)}
        onConfirm={async () => {
          if (confirmId) await deleteRow('exams', confirmId)
          setConfirmId(null)
          push('Deleted.', 'success')
        }}
      />
    </div>
  )
}

function ExamForm({ open, entry, yearId, onClose }: { open: boolean; entry?: Exam; yearId: string | null; onClose: () => void }) {
  const { push } = useToast()
  const { currentYear } = useApp()
  const data = useYearData(currentYear?.id ?? null)
  const [name, setName] = useState(entry?.name ?? '')
  const [type, setType] = useState<Exam['type']>(entry?.type ?? 'unit')
  const [date, setDate] = useState(entry?.date ?? todayStr())
  const [subjectId, setSubjectId] = useState(entry?.subjectId ?? '')
  const [maxMarks, setMaxMarks] = useState(String(entry?.maxMarks ?? 100))
  const [obtained, setObtained] = useState(String(entry?.obtainedMarks ?? ''))
  const [duration, setDuration] = useState(String(entry?.durationMinutes ?? ''))
  const [remarks, setRemarks] = useState(entry?.remarks ?? '')
  const [weak, setWeak] = useState(entry?.weakTopics ?? '')
  const [chapters, setChapters] = useState<string[]>(entry?.chaptersCovered ?? [])

  const key = `${open}-${entry?.id ?? 'new'}`
  const [lastKey, setLastKey] = useState(key)
  if (key !== lastKey) {
    setLastKey(key)
    setName(entry?.name ?? '')
    setType(entry?.type ?? 'unit')
    setDate(entry?.date ?? todayStr())
    setSubjectId(entry?.subjectId ?? '')
    setMaxMarks(String(entry?.maxMarks ?? 100))
    setObtained(String(entry?.obtainedMarks ?? ''))
    setDuration(String(entry?.durationMinutes ?? ''))
    setRemarks(entry?.remarks ?? '')
    setWeak(entry?.weakTopics ?? '')
    setChapters(entry?.chaptersCovered ?? [])
  }

  const chapterOptions = (data?.chapters ?? []).filter((c) => !subjectId || c.subjectId === subjectId)

  const submit = async () => {
    if (!name.trim()) {
      push('A test name is required.', 'error')
      return
    }
    const max = Number(maxMarks) || 0
    if (max <= 0) {
      push('Maximum marks must be greater than zero.', 'error')
      return
    }
    const got = obtained === '' ? undefined : Number(obtained)
    if (got !== undefined && (got < 0 || got > max)) {
      push('Marks obtained must be between 0 and the maximum.', 'error')
      return
    }
    const payload = {
      name: name.trim(),
      type,
      date,
      subjectId: subjectId || undefined,
      maxMarks: max,
      obtainedMarks: got,
      durationMinutes: duration ? Number(duration) : undefined,
      remarks,
      weakTopics: weak,
      chaptersCovered: chapters,
    }
    if (entry) {
      await updateRow('exams', entry.id, payload)
      push('Test updated.', 'success')
    } else {
      if (!yearId) return
      await createRow('exams', { ...payload, yearId })
      push('Test added.', 'success')
    }
    onClose()
  }

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={entry ? 'Edit test' : 'Add test'}
      wide
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            Cancel
          </Button>
          <Button onClick={() => void submit()}>{entry ? 'Save' : 'Add'}</Button>
        </>
      }
    >
      <div className="space-y-3">
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          <Field label="Name *">
            <Input value={name} onChange={(e) => setName(e.target.value)} placeholder="Unit Test 2 — Kinematics" />
          </Field>
          <Field label="Type">
            <Select value={type} onChange={(e) => setType(e.target.value as Exam['type'])}>
              {Object.entries(TYPE_LABEL).map(([v, l]) => (
                <option key={v} value={v}>
                  {l}
                </option>
              ))}
            </Select>
          </Field>
        </div>
        <div className="grid grid-cols-2 gap-3">
          <Field label="Date">
            <Input type="date" value={date} onChange={(e) => setDate(e.target.value)} />
          </Field>
          <Field label="Subject">
            <Select value={subjectId} onChange={(e) => { setSubjectId(e.target.value); setChapters([]) }}>
              <option value="">All subjects</option>
              {(data?.subjects ?? []).map((s) => (
                <option key={s.id} value={s.id}>
                  {s.name}
                </option>
              ))}
            </Select>
          </Field>
        </div>
        <div className="grid grid-cols-3 gap-3">
          <Field label="Max marks *">
            <Input type="number" min={1} value={maxMarks} onChange={(e) => setMaxMarks(e.target.value)} />
          </Field>
          <Field label="Obtained">
            <Input type="number" min={0} value={obtained} onChange={(e) => setObtained(e.target.value)} />
          </Field>
          <Field label="Duration (min)">
            <Input type="number" min={0} value={duration} onChange={(e) => setDuration(e.target.value)} />
          </Field>
        </div>
        {chapterOptions.length > 0 ? (
          <Field label="Chapters covered">
            <div className="max-h-32 overflow-y-auto rounded-lg border border-line p-1.5">
              {chapterOptions.map((c) => (
                <label key={c.id} className="flex items-center gap-2 py-0.5 text-[11px]">
                  <input
                    type="checkbox"
                    checked={chapters.includes(c.id)}
                    onChange={(e) => setChapters((prev) => (e.target.checked ? [...prev, c.id] : prev.filter((x) => x !== c.id)))}
                  />
                  <span className="truncate">{c.name}</span>
                </label>
              ))}
            </div>
          </Field>
        ) : null}
        <Field label="Weak topics">
          <Input value={weak} onChange={(e) => setWeak(e.target.value)} placeholder="Comma separated" />
        </Field>
        <Field label="Remarks">
          <Textarea value={remarks} onChange={(e) => setRemarks(e.target.value)} />
        </Field>
      </div>
    </Modal>
  )
}
