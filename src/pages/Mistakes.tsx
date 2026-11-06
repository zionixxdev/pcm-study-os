import { useMemo, useState } from 'react'
import { useLiveQuery } from 'dexie-react-hooks'
import { Check, Pencil, Plus, Search, Trash2 } from 'lucide-react'
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
  Tabs,
  Textarea,
  cx,
  useToast,
} from '../components/ui'
import { useApp } from '../store/AppContext'
import { useYearData } from '../hooks/useYearData'
import { createRow, db, deleteRow, updateRow } from '../lib/db'
import { SyllabusSelect, type SyllabusSelection } from '../components/SyllabusPicker'
import { formatDate, todayStr } from '../lib/dates'
import { nextRevisionDate } from '../lib/revision'
import type { Mistake, MistakeCategory } from '../types'

const CATEGORY_LABEL: Record<MistakeCategory, string> = {
  conceptual: 'Conceptual',
  formula: 'Formula',
  calculation: 'Calculation',
  sign: 'Sign',
  unit: 'Unit',
  silly: 'Silly',
  incomplete: 'Incomplete',
  time: 'Time management',
  other: 'Other',
}

export default function Mistakes() {
  const { currentYear, settings } = useApp()
  const { push } = useToast()
  const yearId = currentYear?.id ?? null
  const data = useYearData(yearId)
  const rows = useLiveQuery(async () => (yearId ? db.mistakes.where({ yearId }).toArray() : []), [yearId])
  const [query, setQuery] = useState('')
  const [subjectId, setSubjectId] = useState('')
  const [category, setCategory] = useState('')
  const [tab, setTab] = useState<'open' | 'resolved' | 'all'>('open')
  const [form, setForm] = useState<{ open: boolean; entry?: Mistake }>({ open: false })
  const [confirmId, setConfirmId] = useState<string | null>(null)

  const list = useMemo(() => rows ?? [], [rows])
  const subjectName = (id?: string) => data?.subjects.find((s) => s.id === id)?.name ?? ''

  const visible = useMemo(() => {
    const q = query.trim().toLowerCase()
    return list
      .filter((m) => (tab === 'all' ? true : tab === 'open' ? !m.resolved : m.resolved))
      .filter((m) => (!subjectId || m.subjectId === subjectId) && (!category || m.category === category))
      .filter((m) => !q || [m.question, m.explanation, m.wrongApproach, m.correctApproach].join(' ').toLowerCase().includes(q))
      .sort((a, b) => b.date.localeCompare(a.date))
  }, [list, tab, subjectId, category, query])

  const pattern = useMemo(() => {
    const counts = new Map<MistakeCategory, number>()
    for (const m of list) counts.set(m.category, (counts.get(m.category) ?? 0) + 1)
    return [...counts.entries()].sort((a, b) => b[1] - a[1]).slice(0, 4)
  }, [list])

  return (
    <div>
      <PageHeader
        title="Mistake notebook"
        subtitle={`${list.filter((m) => !m.resolved).length} unresolved`}
        action={
          <Button size="sm" onClick={() => setForm({ open: true })}>
            <Plus size={14} /> Mistake
          </Button>
        }
      />

      <div className="mb-3 grid grid-cols-2 gap-2 sm:grid-cols-4">
        <Stat label="Total" value={list.length} />
        <Stat label="Resolved" value={list.filter((m) => m.resolved).length} />
        <Stat label="Open" value={list.filter((m) => !m.resolved).length} />
        <Stat label="Top pattern" value={pattern[0] ? CATEGORY_LABEL[pattern[0][0]] : '—'} hint={pattern[0] ? `${pattern[0][1]} entries` : undefined} />
      </div>

      <Card className="mb-3 p-2.5">
        <div className="flex flex-col gap-2 sm:flex-row">
          <div className="relative flex-1">
            <Search size={14} className="absolute left-2.5 top-1/2 -translate-y-1/2 text-muted" />
            <Input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Search mistakes…" className="pl-8" />
          </div>
          <Select value={subjectId} onChange={(e) => setSubjectId(e.target.value)} className="sm:w-40">
            <option value="">All subjects</option>
            {(data?.subjects ?? []).map((s) => (
              <option key={s.id} value={s.id}>
                {s.name}
              </option>
            ))}
          </Select>
          <Select value={category} onChange={(e) => setCategory(e.target.value)} className="sm:w-40">
            <option value="">All types</option>
            {Object.entries(CATEGORY_LABEL).map(([v, l]) => (
              <option key={v} value={v}>
                {l}
              </option>
            ))}
          </Select>
        </div>
      </Card>

      <div className="mb-3">
        <Tabs
          value={tab}
          onChange={setTab}
          options={[
            { value: 'open', label: 'Open' },
            { value: 'resolved', label: 'Resolved' },
            { value: 'all', label: 'All' },
          ]}
        />
      </div>

      {visible.length === 0 ? (
        <EmptyState title="No mistakes logged" hint="Recording why an answer went wrong is the fastest way to stop repeating it." action={<Button onClick={() => setForm({ open: true })}>Log mistake</Button>} />
      ) : (
        <ul className="space-y-1.5">
          {visible.map((m) => (
            <li key={m.id}>
              <Card className="p-2.5">
                <div className="flex items-start gap-2">
                  <button
                    onClick={() => void updateRow('mistakes', m.id, { resolved: !m.resolved })}
                    aria-label="Toggle resolved"
                    className={cx('mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded border', m.resolved ? 'border-emerald-500 bg-emerald-500 text-white' : 'border-line text-transparent hover:border-accent')}
                  >
                    <Check size={12} />
                  </button>
                  <div className="min-w-0 flex-1">
                    <p className={cx('text-sm', m.resolved && 'text-muted line-through')}>{m.question}</p>
                    <p className="mt-0.5 flex flex-wrap items-center gap-1.5 text-[11px] text-muted">
                      <Badge tone="warn">{CATEGORY_LABEL[m.category]}</Badge>
                      {subjectName(m.subjectId)} · {formatDate(m.date)}
                      {m.retryDate ? ` · retry ${formatDate(m.retryDate)}` : ''}
                    </p>
                    {m.wrongApproach ? <p className="mt-1 text-[11px] text-red-400/90">✗ {m.wrongApproach}</p> : null}
                    {m.correctApproach ? <p className="text-[11px] text-emerald-400/90">✓ {m.correctApproach}</p> : null}
                    {m.explanation ? <p className="mt-0.5 text-[11px] text-muted">{m.explanation}</p> : null}
                  </div>
                  <div className="flex gap-0.5">
                    <button onClick={() => setForm({ open: true, entry: m })} aria-label="Edit" className="p-1 text-muted">
                      <Pencil size={12} />
                    </button>
                    <button onClick={() => setConfirmId(m.id)} aria-label="Delete" className="p-1 text-muted hover:text-red-400">
                      <Trash2 size={12} />
                    </button>
                  </div>
                </div>
              </Card>
            </li>
          ))}
        </ul>
      )}

      <MistakeForm open={form.open} entry={form.entry} yearId={yearId} intervals={settings.revisionIntervals} onClose={() => setForm({ open: false })} />

      <ConfirmDialog
        open={Boolean(confirmId)}
        title="Delete mistake?"
        message="This entry will be removed from your notebook."
        onCancel={() => setConfirmId(null)}
        onConfirm={async () => {
          if (confirmId) await deleteRow('mistakes', confirmId)
          setConfirmId(null)
          push('Deleted.', 'success')
        }}
      />
    </div>
  )
}

function MistakeForm({
  open,
  entry,
  yearId,
  intervals,
  onClose,
}: {
  open: boolean
  entry?: Mistake
  yearId: string | null
  intervals: number[]
  onClose: () => void
}) {
  const { push } = useToast()
  const [question, setQuestion] = useState(entry?.question ?? '')
  const [wrong, setWrong] = useState(entry?.wrongApproach ?? '')
  const [correct, setCorrect] = useState(entry?.correctApproach ?? '')
  const [explanation, setExplanation] = useState(entry?.explanation ?? '')
  const [category, setCategory] = useState<MistakeCategory>(entry?.category ?? 'conceptual')
  const [date, setDate] = useState(entry?.date ?? todayStr())
  const [retryDate, setRetryDate] = useState(entry?.retryDate ?? nextRevisionDate(todayStr(), intervals, 1))
  const [sel, setSel] = useState<SyllabusSelection>({ subjectId: entry?.subjectId, chapterId: entry?.chapterId, topicId: entry?.topicId })

  const key = `${open}-${entry?.id ?? 'new'}`
  const [lastKey, setLastKey] = useState(key)
  if (key !== lastKey) {
    setLastKey(key)
    setQuestion(entry?.question ?? '')
    setWrong(entry?.wrongApproach ?? '')
    setCorrect(entry?.correctApproach ?? '')
    setExplanation(entry?.explanation ?? '')
    setCategory(entry?.category ?? 'conceptual')
    setDate(entry?.date ?? todayStr())
    setRetryDate(entry?.retryDate ?? nextRevisionDate(todayStr(), intervals, 1))
    setSel({ subjectId: entry?.subjectId, chapterId: entry?.chapterId, topicId: entry?.topicId })
  }

  const submit = async () => {
    if (!question.trim()) {
      push('Describe the question.', 'error')
      return
    }
    if (!sel.subjectId) {
      push('Choose a subject.', 'error')
      return
    }
    const payload = {
      question: question.trim(),
      wrongApproach: wrong,
      correctApproach: correct,
      explanation,
      category,
      date,
      retryDate: retryDate || undefined,
      subjectId: sel.subjectId,
      chapterId: sel.chapterId,
      topicId: sel.topicId,
    }
    if (entry) {
      await updateRow('mistakes', entry.id, payload)
      push('Mistake updated.', 'success')
    } else {
      if (!yearId) return
      await createRow('mistakes', { ...payload, resolved: false, yearId })
      push('Mistake logged.', 'success')
    }
    onClose()
  }

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={entry ? 'Edit mistake' : 'Log mistake'}
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
        <Field label="Question / problem *">
          <Textarea value={question} onChange={(e) => setQuestion(e.target.value)} />
        </Field>
        <SyllabusSelect value={sel} onChange={setSel} required />
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          <Field label="My incorrect approach">
            <Textarea value={wrong} onChange={(e) => setWrong(e.target.value)} />
          </Field>
          <Field label="Correct approach">
            <Textarea value={correct} onChange={(e) => setCorrect(e.target.value)} />
          </Field>
        </div>
        <Field label="Why it went wrong">
          <Textarea value={explanation} onChange={(e) => setExplanation(e.target.value)} />
        </Field>
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
          <Field label="Type">
            <Select value={category} onChange={(e) => setCategory(e.target.value as MistakeCategory)}>
              {Object.entries(CATEGORY_LABEL).map(([v, l]) => (
                <option key={v} value={v}>
                  {l}
                </option>
              ))}
            </Select>
          </Field>
          <Field label="Date">
            <Input type="date" value={date} onChange={(e) => setDate(e.target.value)} />
          </Field>
          <Field label="Retry date">
            <Input type="date" value={retryDate} onChange={(e) => setRetryDate(e.target.value)} />
          </Field>
        </div>
      </div>
    </Modal>
  )
}
