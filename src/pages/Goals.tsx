import { useMemo, useState } from 'react'
import { Archive, Pencil, Plus, Target, Trash2 } from 'lucide-react'
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
  Textarea,
  useToast,
} from '../components/ui'
import { useApp } from '../store/AppContext'
import { useGoals, useRevisions, useSessions, computeStats } from '../hooks/useStats'
import { createRow, deleteRow, updateRow } from '../lib/db'
import { formatDate, todayStr } from '../lib/dates'
import type { Goal } from '../types'

const KIND_LABEL: Record<Goal['kind'], string> = {
  daily: 'Daily',
  weekly: 'Weekly',
  monthly: 'Monthly',
  subject: 'Subject',
  chapter: 'Chapter',
  hours: 'Study hours',
  revision: 'Revision',
  custom: 'Custom',
}

export default function Goals() {
  const { currentYear } = useApp()
  const { push } = useToast()
  const yearId = currentYear?.id ?? null
  const goals = useGoals(yearId)
  const sessions = useSessions(yearId)
  const revisions = useRevisions(yearId)
  const [form, setForm] = useState<{ open: boolean; entry?: Goal }>({ open: false })
  const [confirmId, setConfirmId] = useState<string | null>(null)
  const [showArchived, setShowArchived] = useState(false)
  const today = todayStr()

  const stats = useMemo(() => computeStats(sessions ?? []), [sessions])

  /** Hours and revision goals show a computed value as a suggestion. */
  const computed = (goal: Goal): number => {
    if (goal.kind === 'hours') return Math.round((stats.weekMinutes / 60) * 10) / 10
    if (goal.kind === 'revision') return (revisions ?? []).filter((r) => r.status === 'completed').length
    return goal.current
  }

  const list = (goals ?? [])
    .filter((g) => showArchived || g.status !== 'archived')
    .sort((a, b) => (a.deadline ?? '9999').localeCompare(b.deadline ?? '9999'))

  const active = list.filter((g) => g.status === 'active')
  const completed = list.filter((g) => g.status === 'completed')
  const missed = list.filter((g) => g.status === 'missed' || (g.status === 'active' && g.deadline && g.deadline < today))

  return (
    <div>
      <PageHeader
        title="Goals"
        subtitle={`${active.length} active`}
        action={
          <Button size="sm" onClick={() => setForm({ open: true })}>
            <Plus size={14} /> Goal
          </Button>
        }
      />

      <div className="mb-3 grid grid-cols-3 gap-2">
        <Stat label="Active" value={active.length} />
        <Stat label="Completed" value={completed.length} />
        <Stat label="Missed" value={missed.length} />
      </div>

      <label className="mb-3 flex items-center gap-2 text-[11px] text-muted">
        <input type="checkbox" checked={showArchived} onChange={(e) => setShowArchived(e.target.checked)} /> Show archived
      </label>

      {list.length === 0 ? (
        <EmptyState title="No goals yet" hint="Set a weekly study-hour target or a chapter deadline." action={<Button onClick={() => setForm({ open: true })}>Create goal</Button>} icon={<Target size={22} />} />
      ) : (
        <ul className="space-y-1.5">
          {list.map((g) => {
            const value = g.kind === 'hours' || g.kind === 'revision' ? computed(g) : g.current
            const pct = g.target > 0 ? Math.min(100, Math.round((value / g.target) * 100)) : 0
            const late = g.status === 'active' && g.deadline && g.deadline < today
            return (
              <li key={g.id}>
                <Card className="p-3">
                  <div className="flex items-start justify-between gap-2">
                    <div className="min-w-0">
                      <p className="flex flex-wrap items-center gap-1.5 text-sm font-medium">
                        <span className="truncate">{g.title}</span>
                        <Badge tone="accent">{KIND_LABEL[g.kind]}</Badge>
                        {g.status === 'completed' ? <Badge tone="success">done</Badge> : null}
                        {g.status === 'archived' ? <Badge>archived</Badge> : null}
                        {late ? <Badge tone="danger">overdue</Badge> : null}
                      </p>
                      <p className="text-[11px] text-muted">
                        {value} / {g.target}
                        {g.deadline ? ` · by ${formatDate(g.deadline)}` : ''}
                      </p>
                      {g.description ? <p className="mt-1 text-[11px] text-muted">{g.description}</p> : null}
                    </div>
                    <div className="flex gap-0.5">
                      {g.status === 'active' ? (
                        <Button
                          size="sm"
                          variant="ghost"
                          aria-label="Mark complete"
                          onClick={() => void updateRow('goals', g.id, { status: 'completed', current: g.target })}
                        >
                          ✓
                        </Button>
                      ) : null}
                      <button onClick={() => setForm({ open: true, entry: g })} aria-label="Edit goal" className="p-1 text-muted">
                        <Pencil size={12} />
                      </button>
                      <button onClick={() => void updateRow('goals', g.id, { status: 'archived' })} aria-label="Archive goal" className="p-1 text-muted">
                        <Archive size={12} />
                      </button>
                      <button onClick={() => setConfirmId(g.id)} aria-label="Delete goal" className="p-1 text-muted hover:text-red-400">
                        <Trash2 size={12} />
                      </button>
                    </div>
                  </div>
                  <ProgressBar value={pct} className="mt-2" tone={pct >= 100 ? '#39c6a4' : undefined} />
                </Card>
              </li>
            )
          })}
        </ul>
      )}

      <GoalForm open={form.open} entry={form.entry} yearId={yearId} onClose={() => setForm({ open: false })} />

      <ConfirmDialog
        open={Boolean(confirmId)}
        title="Delete goal?"
        message="This goal will be removed."
        onCancel={() => setConfirmId(null)}
        onConfirm={async () => {
          if (confirmId) await deleteRow('goals', confirmId)
          setConfirmId(null)
          push('Deleted.', 'success')
        }}
      />
    </div>
  )
}

function GoalForm({ open, entry, yearId, onClose }: { open: boolean; entry?: Goal; yearId: string | null; onClose: () => void }) {
  const { push } = useToast()
  const [title, setTitle] = useState(entry?.title ?? '')
  const [description, setDescription] = useState(entry?.description ?? '')
  const [kind, setKind] = useState<Goal['kind']>(entry?.kind ?? 'weekly')
  const [target, setTarget] = useState(String(entry?.target ?? 10))
  const [current, setCurrent] = useState(String(entry?.current ?? 0))
  const [deadline, setDeadline] = useState(entry?.deadline ?? '')

  const key = `${open}-${entry?.id ?? 'new'}`
  const [lastKey, setLastKey] = useState(key)
  if (key !== lastKey) {
    setLastKey(key)
    setTitle(entry?.title ?? '')
    setDescription(entry?.description ?? '')
    setKind(entry?.kind ?? 'weekly')
    setTarget(String(entry?.target ?? 10))
    setCurrent(String(entry?.current ?? 0))
    setDeadline(entry?.deadline ?? '')
  }

  const submit = async () => {
    if (!title.trim()) {
      push('A goal title is required.', 'error')
      return
    }
    if (!(Number(target) > 0)) {
      push('Target must be greater than zero.', 'error')
      return
    }
    const payload = {
      title: title.trim(),
      description,
      kind,
      target: Number(target),
      current: Number(current) || 0,
      deadline: deadline || undefined,
    }
    if (entry) {
      await updateRow('goals', entry.id, payload)
      push('Goal updated.', 'success')
    } else {
      if (!yearId) return
      await createRow('goals', { ...payload, status: 'active', yearId })
      push('Goal created.', 'success')
    }
    onClose()
  }

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={entry ? 'Edit goal' : 'New goal'}
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            Cancel
          </Button>
          <Button onClick={() => void submit()}>{entry ? 'Save' : 'Create'}</Button>
        </>
      }
    >
      <div className="space-y-3">
        <Field label="Title *">
          <Input value={title} onChange={(e) => setTitle(e.target.value)} placeholder="Finish Mechanics by Diwali" />
        </Field>
        <Field label="Description">
          <Textarea value={description} onChange={(e) => setDescription(e.target.value)} />
        </Field>
        <div className="grid grid-cols-2 gap-3">
          <Field label="Type">
            <Select value={kind} onChange={(e) => setKind(e.target.value as Goal['kind'])}>
              {Object.entries(KIND_LABEL).map(([v, l]) => (
                <option key={v} value={v}>
                  {l}
                </option>
              ))}
            </Select>
          </Field>
          <Field label="Deadline">
            <Input type="date" value={deadline} onChange={(e) => setDeadline(e.target.value)} />
          </Field>
        </div>
        <div className="grid grid-cols-2 gap-3">
          <Field label="Target">
            <Input type="number" min={1} value={target} onChange={(e) => setTarget(e.target.value)} />
          </Field>
          <Field label="Current">
            <Input type="number" min={0} value={current} onChange={(e) => setCurrent(e.target.value)} />
          </Field>
        </div>
      </div>
    </Modal>
  )
}
