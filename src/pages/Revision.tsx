import { useMemo, useState } from 'react'
import { Check, Clock, Plus, Repeat, Trash2 } from 'lucide-react'
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
  useToast,
} from '../components/ui'
import { useApp } from '../store/AppContext'
import { useRevisions } from '../hooks/useStats'
import { useYearData } from '../hooks/useYearData'
import { createRow, deleteRow, updateRow } from '../lib/db'
import { effectiveStatus, nextIntervalIndex, nextRevisionDate } from '../lib/revision'
import { formatDate, todayStr } from '../lib/dates'
import type { RevisionItem, RevisionRating, RevisionStatus } from '../types'

type Filter = 'all' | RevisionStatus

const RATING_LABEL: Record<RevisionRating, string> = {
  again: 'Again (forgot)',
  hard: 'Hard',
  good: 'Good',
  easy: 'Easy',
}

export default function Revision() {
  const { currentYear, settings } = useApp()
  const { push } = useToast()
  const yearId = currentYear?.id ?? null
  const revisions = useRevisions(yearId)
  const data = useYearData(yearId)
  const [filter, setFilter] = useState<Filter>('all')
  const [addOpen, setAddOpen] = useState(false)
  const [rateTarget, setRateTarget] = useState<RevisionItem | null>(null)
  const [confirmId, setConfirmId] = useState<string | null>(null)
  const today = todayStr()

  const items = useMemo(
    () => (revisions ?? []).map((r) => ({ ...r, effective: effectiveStatus(r, today) })),
    [revisions, today],
  )

  const counts = useMemo(() => {
    const c = { due: 0, overdue: 0, upcoming: 0, completed: 0 }
    for (const r of items) c[r.effective]++
    return c
  }, [items])

  const visible = items
    .filter((r) => filter === 'all' || r.effective === filter)
    .sort((a, b) => a.dueDate.localeCompare(b.dueDate))

  const topicName = (id?: string) => (data?.topics ?? []).find((t) => t.id === id)?.name

  return (
    <div>
      <PageHeader
        title="Revision"
        subtitle={`Spaced repetition · intervals ${settings.revisionIntervals.join(', ')} days`}
        action={
          <Button size="sm" onClick={() => setAddOpen(true)}>
            <Plus size={14} /> Revision
          </Button>
        }
      />

      <div className="mb-3 grid grid-cols-2 gap-2 sm:grid-cols-4">
        <Stat label="Due today" value={counts.due} />
        <Stat label="Overdue" value={counts.overdue} />
        <Stat label="Upcoming" value={counts.upcoming} />
        <Stat label="Completed" value={counts.completed} />
      </div>

      <div className="mb-3">
        <Tabs
          value={filter}
          onChange={setFilter}
          options={[
            { value: 'all', label: 'All' },
            { value: 'due', label: 'Due' },
            { value: 'overdue', label: 'Overdue' },
            { value: 'upcoming', label: 'Upcoming' },
            { value: 'completed', label: 'Done' },
          ]}
        />
      </div>

      {visible.length === 0 ? (
        <EmptyState
          title="No revision items"
          hint="Add a topic to revise. After each revision the next date is scheduled automatically."
          action={<Button onClick={() => setAddOpen(true)}>Add revision</Button>}
          icon={<Repeat size={22} />}
        />
      ) : (
        <ul className="space-y-1.5">
          {visible.map((r) => (
            <li key={r.id}>
              <Card className="flex items-start gap-2 p-2.5">
                <div className="min-w-0 flex-1">
                  <p className="flex items-center gap-1.5 text-sm">
                    <span className={r.effective === 'completed' ? 'text-muted line-through' : ''}>{r.label}</span>
                    {topicName(r.topicId) ? <span className="text-[10px] text-muted">· {topicName(r.topicId)}</span> : null}
                  </p>
                  <p className="mt-0.5 flex flex-wrap items-center gap-1.5 text-[11px] text-muted">
                    <Clock size={10} /> {formatDate(r.dueDate)}
                    <span>· interval {settings.revisionIntervals[Math.min(r.intervalIndex, settings.revisionIntervals.length - 1)] ?? 1}d</span>
                    {r.lastRating ? <span>· last: {r.lastRating}</span> : null}
                  </p>
                  {r.notes ? <p className="mt-1 text-[11px] text-muted">{r.notes}</p> : null}
                </div>
                <Badge tone={r.effective === 'overdue' ? 'danger' : r.effective === 'due' ? 'warn' : r.effective === 'completed' ? 'success' : 'neutral'}>
                  {r.effective}
                </Badge>
                {r.effective !== 'completed' ? (
                  <Button size="sm" variant="secondary" onClick={() => setRateTarget(r)} aria-label="Complete revision">
                    <Check size={13} />
                  </Button>
                ) : null}
                <button onClick={() => setConfirmId(r.id)} aria-label="Delete revision" className="p-1 text-muted hover:text-red-400">
                  <Trash2 size={13} />
                </button>
              </Card>
            </li>
          ))}
        </ul>
      )}

      <AddRevision
        open={addOpen}
        onClose={() => setAddOpen(false)}
        yearId={yearId}
        topics={(data?.topics ?? []).map((t) => ({ id: t.id, name: t.name }))}
      />

      <RateRevision item={rateTarget} onClose={() => setRateTarget(null)} intervals={settings.revisionIntervals} />

      <ConfirmDialog
        open={Boolean(confirmId)}
        title="Delete revision?"
        message="This revision entry will be removed."
        onCancel={() => setConfirmId(null)}
        onConfirm={async () => {
          if (confirmId) await deleteRow('revisions', confirmId)
          setConfirmId(null)
          push('Revision deleted.', 'success')
        }}
      />
    </div>
  )
}

function AddRevision({ open, onClose, yearId, topics }: { open: boolean; onClose: () => void; yearId: string | null; topics: Array<{ id: string; name: string }> }) {
  const { push } = useToast()
  const [topicId, setTopicId] = useState('')
  const [label, setLabel] = useState('')
  const [dueDate, setDueDate] = useState(todayStr())
  const [notes, setNotes] = useState('')

  const submit = async () => {
    if (!yearId) return
    const finalLabel = label.trim() || topics.find((t) => t.id === topicId)?.name
    if (!finalLabel) {
      push('Pick a topic or type a label.', 'error')
      return
    }
    // Avoid accidental duplicates for the same topic on the same day.
    const existing = await import('../lib/db').then((m) => m.db.revisions.where({ yearId }).toArray())
    const dup = existing.find((r) => r.dueDate === dueDate && ((topicId && r.topicId === topicId) || r.label === finalLabel))
    if (dup) {
      push('A revision for this already exists on that date.', 'error')
      return
    }
    await createRow('revisions', {
      yearId,
      topicId: topicId || undefined,
      label: finalLabel,
      dueDate,
      intervalIndex: 0,
      status: 'upcoming',
      notes: notes.trim() || undefined,
    })
    push('Revision scheduled.', 'success')
    setLabel('')
    setNotes('')
    setTopicId('')
    onClose()
  }

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Add revision"
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            Cancel
          </Button>
          <Button onClick={() => void submit()}>Schedule</Button>
        </>
      }
    >
      <div className="space-y-3">
        <Field label="Topic">
          <Select value={topicId} onChange={(e) => setTopicId(e.target.value)}>
            <option value="">—</option>
            {topics.map((t) => (
              <option key={t.id} value={t.id}>
                {t.name}
              </option>
            ))}
          </Select>
        </Field>
        <Field label="Or custom label">
          <Input value={label} onChange={(e) => setLabel(e.target.value)} placeholder="e.g. Kinematics formulas" />
        </Field>
        <Field label="Revision date">
          <Input type="date" value={dueDate} onChange={(e) => setDueDate(e.target.value)} />
        </Field>
        <Field label="Notes">
          <Textarea value={notes} onChange={(e) => setNotes(e.target.value)} />
        </Field>
      </div>
    </Modal>
  )
}

function RateRevision({ item, onClose, intervals }: { item: RevisionItem | null; onClose: () => void; intervals: number[] }) {
  const { push } = useToast()
  const [rating, setRating] = useState<RevisionRating>('good')
  const [notes, setNotes] = useState('')

  const submit = async () => {
    if (!item) return
    const idx = nextIntervalIndex(item.intervalIndex, rating, intervals.length)
    const next = nextRevisionDate(todayStr(), intervals, idx)
    await updateRow('revisions', item.id, {
      status: 'completed',
      completedAt: new Date().toISOString(),
      lastRating: rating,
      notes: notes.trim() || item.notes,
    })
    await createRow('revisionHistory', {
      revisionId: item.id,
      rating,
      notes: notes.trim() || undefined,
      at: new Date().toISOString(),
    })
    await createRow('revisions', {
      yearId: item.yearId,
      topicId: item.topicId,
      subtopicId: item.subtopicId,
      label: item.label,
      dueDate: next,
      intervalIndex: idx,
      status: 'upcoming',
    })
    push(`Next revision scheduled for ${formatDate(next)}.`, 'success')
    setNotes('')
    setRating('good')
    onClose()
  }

  return (
    <Modal
      open={Boolean(item)}
      onClose={onClose}
      title="How well did you remember?"
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            Cancel
          </Button>
          <Button onClick={() => void submit()}>Complete &amp; schedule next</Button>
        </>
      }
    >
      <p className="mb-3 text-sm text-muted">{item?.label}</p>
      <div className="grid grid-cols-2 gap-2">
        {(Object.keys(RATING_LABEL) as RevisionRating[]).map((r) => (
          <button
            key={r}
            onClick={() => setRating(r)}
            className={
              rating === r
                ? 'rounded-lg border border-accent bg-accent/10 px-2 py-2 text-xs text-accent'
                : 'rounded-lg border border-line px-2 py-2 text-xs text-muted hover:border-accent/50'
            }
          >
            {RATING_LABEL[r]}
          </button>
        ))}
      </div>
      <div className="mt-3">
        <Field label="Notes">
          <Textarea value={notes} onChange={(e) => setNotes(e.target.value)} className="min-h-[60px]" />
        </Field>
      </div>
      <p className="mt-2 text-[11px] text-muted">
        Next interval: {intervals[Math.min(nextIntervalIndex(item?.intervalIndex ?? 0, rating, intervals.length), intervals.length - 1)] ?? 1} days
      </p>
    </Modal>
  )
}
