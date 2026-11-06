import { useMemo, useState } from 'react'
import { useLiveQuery } from 'dexie-react-hooks'
import { Plus, RefreshCw, Trash2, Pencil } from 'lucide-react'
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
  ProgressBar,
  Select,
  Stat,
  Tabs,
  Textarea,
  useToast,
} from '../components/ui'
import { useApp } from '../store/AppContext'
import { useYearData } from '../hooks/useYearData'
import { createRow, db, deleteRow, updateRow } from '../lib/db'
import { SyllabusSelect, type SyllabusSelection } from '../components/SyllabusPicker'
import { formatDate, todayStr } from '../lib/dates'
import type { PracticeQuestion } from '../types'

const RESULT_TONE = { correct: 'success', incorrect: 'danger', partial: 'warn', skipped: 'neutral' } as const

export default function Practice() {
  const { currentYear } = useApp()
  const { push } = useToast()
  const yearId = currentYear?.id ?? null
  const data = useYearData(yearId)
  const rows = useLiveQuery(async () => (yearId ? db.practice.where({ yearId }).toArray() : []), [yearId])
  const [filter, setFilter] = useState<'all' | 'retry' | 'wrong'>('all')
  const [form, setForm] = useState<{ open: boolean; entry?: PracticeQuestion }>({ open: false })
  const [confirmId, setConfirmId] = useState<string | null>(null)

  const list = useMemo(() => rows ?? [], [rows])
  const attempted = list.filter((r) => r.result !== 'skipped')
  const correct = list.filter((r) => r.result === 'correct').length
  const incorrect = list.filter((r) => r.result === 'incorrect').length
  const partial = list.filter((r) => r.result === 'partial').length
  const accuracy = attempted.length ? Math.round(((correct + partial * 0.5) / attempted.length) * 100) : 0

  const byChapter = useMemo(() => {
    const map = new Map<string, { total: number; correct: number }>()
    for (const r of list) {
      const key = r.chapterId ?? 'unassigned'
      const cur = map.get(key) ?? { total: 0, correct: 0 }
      cur.total++
      if (r.result === 'correct') cur.correct++
      map.set(key, cur)
    }
    return [...map.entries()].map(([id, v]) => ({
      name: data?.chapters.find((c) => c.id === id)?.name ?? 'Unassigned',
      ...v,
      pct: v.total ? Math.round((v.correct / v.total) * 100) : 0,
    })).sort((a, b) => a.pct - b.pct)
  }, [list, data])

  const visible = list
    .filter((r) => (filter === 'all' ? true : filter === 'retry' ? r.retry : r.result === 'incorrect' || r.result === 'partial'))
    .sort((a, b) => b.attemptDate.localeCompare(a.attemptDate))

  const subjectName = (id?: string) => data?.subjects.find((s) => s.id === id)?.name ?? ''

  return (
    <div>
      <PageHeader
        title="Practice tracker"
        subtitle={`${list.length} questions logged`}
        action={
          <Button size="sm" onClick={() => setForm({ open: true })}>
            <Plus size={14} /> Question
          </Button>
        }
      />

      <div className="mb-3 grid grid-cols-2 gap-2 sm:grid-cols-4">
        <Stat label="Attempted" value={attempted.length} />
        <Stat label="Correct" value={correct} />
        <Stat label="Incorrect" value={incorrect} />
        <Stat label="Accuracy" value={`${accuracy}%`} hint="partials count half" />
      </div>

      {byChapter.length > 0 ? (
        <Card className="mb-3 p-3">
          <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-muted">Chapter performance</p>
          <ul className="space-y-1.5">
            {byChapter.slice(0, 6).map((c) => (
              <li key={c.name}>
                <div className="flex items-center justify-between text-[11px]">
                  <span className="truncate">{c.name}</span>
                  <span className="text-muted">
                    {c.correct}/{c.total} · {c.pct}%
                  </span>
                </div>
                <ProgressBar value={c.pct} className="mt-1" tone={c.pct < 50 ? '#f87171' : c.pct < 75 ? '#f2a03d' : '#39c6a4'} />
              </li>
            ))}
          </ul>
        </Card>
      ) : null}

      <div className="mb-3">
        <Tabs
          value={filter}
          onChange={setFilter}
          options={[
            { value: 'all', label: 'All' },
            { value: 'wrong', label: 'Wrong / partial' },
            { value: 'retry', label: 'To retry' },
          ]}
        />
      </div>

      {visible.length === 0 ? (
        <EmptyState title="No questions logged" hint="Log a numerical after solving it — accuracy and weak chapters build up automatically." action={<Button onClick={() => setForm({ open: true })}>Log question</Button>} />
      ) : (
        <ul className="space-y-1.5">
          {visible.map((r) => (
            <li key={r.id}>
              <Card className="flex items-start gap-2 p-2.5">
                <div className="min-w-0 flex-1">
                  <p className="flex flex-wrap items-center gap-1.5 text-sm">
                    <span className="truncate">{r.title}</span>
                    <Badge tone={RESULT_TONE[r.result]}>{r.result}</Badge>
                    {r.retry ? <Badge tone="warn">retry</Badge> : null}
                  </p>
                  <p className="text-[11px] text-muted">
                    {subjectName(r.subjectId)} · {formatDate(r.attemptDate)} · {r.difficulty}
                    {r.source ? ` · ${r.source}` : ''}
                    {r.minutes ? ` · ${r.minutes} min` : ''}
                  </p>
                  {r.question ? <p className="mt-1 line-clamp-2 text-[11px] text-muted">{r.question}</p> : null}
                </div>
                <div className="flex gap-0.5">
                  <button onClick={() => void updateRow('practice', r.id, { retry: !r.retry })} aria-label="Toggle retry" className="p-1 text-muted">
                    <RefreshCw size={12} />
                  </button>
                  <button onClick={() => setForm({ open: true, entry: r })} aria-label="Edit" className="p-1 text-muted">
                    <Pencil size={12} />
                  </button>
                  <button onClick={() => setConfirmId(r.id)} aria-label="Delete" className="p-1 text-muted hover:text-red-400">
                    <Trash2 size={12} />
                  </button>
                </div>
              </Card>
            </li>
          ))}
        </ul>
      )}

      <PracticeForm open={form.open} entry={form.entry} yearId={yearId} onClose={() => setForm({ open: false })} />

      <ConfirmDialog
        open={Boolean(confirmId)}
        title="Delete record?"
        message="This practice record will be removed."
        onCancel={() => setConfirmId(null)}
        onConfirm={async () => {
          if (confirmId) await deleteRow('practice', confirmId)
          setConfirmId(null)
          push('Deleted.', 'success')
        }}
      />
    </div>
  )
}

function PracticeForm({ open, entry, yearId, onClose }: { open: boolean; entry?: PracticeQuestion; yearId: string | null; onClose: () => void }) {
  const { push } = useToast()
  const [title, setTitle] = useState(entry?.title ?? '')
  const [question, setQuestion] = useState(entry?.question ?? '')
  const [source, setSource] = useState(entry?.source ?? '')
  const [difficulty, setDifficulty] = useState<PracticeQuestion['difficulty']>(entry?.difficulty ?? 'medium')
  const [result, setResult] = useState<PracticeQuestion['result']>(entry?.result ?? 'correct')
  const [minutes, setMinutes] = useState(String(entry?.minutes ?? ''))
  const [notes, setNotes] = useState(entry?.notes ?? '')
  const [retry, setRetry] = useState(entry?.retry ?? false)
  const [attemptDate, setAttemptDate] = useState(entry?.attemptDate ?? todayStr())
  const [sel, setSel] = useState<SyllabusSelection>({ subjectId: entry?.subjectId, chapterId: entry?.chapterId, topicId: entry?.topicId })

  const key = `${open}-${entry?.id ?? 'new'}`
  const [lastKey, setLastKey] = useState(key)
  if (key !== lastKey) {
    setLastKey(key)
    setTitle(entry?.title ?? '')
    setQuestion(entry?.question ?? '')
    setSource(entry?.source ?? '')
    setDifficulty(entry?.difficulty ?? 'medium')
    setResult(entry?.result ?? 'correct')
    setMinutes(String(entry?.minutes ?? ''))
    setNotes(entry?.notes ?? '')
    setRetry(entry?.retry ?? false)
    setAttemptDate(entry?.attemptDate ?? todayStr())
    setSel({ subjectId: entry?.subjectId, chapterId: entry?.chapterId, topicId: entry?.topicId })
  }

  const submit = async () => {
    if (!title.trim()) {
      push('A title is required.', 'error')
      return
    }
    if (!sel.subjectId) {
      push('Choose a subject.', 'error')
      return
    }
    const payload = {
      title: title.trim(),
      question,
      source,
      difficulty,
      result,
      minutes: minutes ? Number(minutes) : undefined,
      notes,
      retry,
      attemptDate,
      subjectId: sel.subjectId,
      chapterId: sel.chapterId,
      topicId: sel.topicId,
    }
    if (entry) {
      await updateRow('practice', entry.id, payload)
      push('Record updated.', 'success')
    } else {
      if (!yearId) return
      await createRow('practice', { ...payload, yearId })
      push('Record added.', 'success')
    }
    onClose()
  }

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={entry ? 'Edit question' : 'Log question'}
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
        <Field label="Title / identifier *">
          <Input value={title} onChange={(e) => setTitle(e.target.value)} placeholder="NCERT Ex 3.14 Q12" />
        </Field>
        <Field label="Question text">
          <Textarea value={question} onChange={(e) => setQuestion(e.target.value)} />
        </Field>
        <SyllabusSelect value={sel} onChange={setSel} required />
        <div className="grid grid-cols-2 gap-3">
          <Field label="Result">
            <Select value={result} onChange={(e) => setResult(e.target.value as PracticeQuestion['result'])}>
              <option value="correct">Correct</option>
              <option value="partial">Partially correct</option>
              <option value="incorrect">Incorrect</option>
              <option value="skipped">Skipped</option>
            </Select>
          </Field>
          <Field label="Difficulty">
            <Select value={difficulty} onChange={(e) => setDifficulty(e.target.value as PracticeQuestion['difficulty'])}>
              <option value="easy">Easy</option>
              <option value="medium">Medium</option>
              <option value="hard">Hard</option>
            </Select>
          </Field>
        </div>
        <div className="grid grid-cols-2 gap-3">
          <Field label="Attempt date">
            <Input type="date" value={attemptDate} onChange={(e) => setAttemptDate(e.target.value)} />
          </Field>
          <Field label="Time taken (min)">
            <Input type="number" min={0} value={minutes} onChange={(e) => setMinutes(e.target.value)} />
          </Field>
        </div>
        <div className="grid grid-cols-2 gap-3">
          <Field label="Source">
            <Input value={source} onChange={(e) => setSource(e.target.value)} placeholder="NCERT / reference book" />
          </Field>
          <Field label="Needs retry">
            <Select value={retry ? 'yes' : 'no'} onChange={(e) => setRetry(e.target.value === 'yes')}>
              <option value="no">No</option>
              <option value="yes">Yes</option>
            </Select>
          </Field>
        </div>
        <Field label="Notes">
          <Textarea value={notes} onChange={(e) => setNotes(e.target.value)} />
        </Field>
      </div>
    </Modal>
  )
}
