import { useMemo, useState } from 'react'
import { Check, ChevronLeft, ChevronRight, Plus, Sparkles, Trash2, Pencil, RotateCcw } from 'lucide-react'
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
  Tabs,
  Textarea,
  cx,
  useToast,
} from '../components/ui'
import { useApp } from '../store/AppContext'
import { useTasks } from '../hooks/useStats'
import { SyllabusSelect, type SyllabusSelection } from '../components/SyllabusPicker'
import { createRow, deleteRow, updateRow } from '../lib/db'
import { completeTask, reopenTask } from '../lib/tasks'
import { planCatchup, summarisePlan, type CatchupSuggestion } from '../lib/catchup'
import { addDays, formatDate, formatMinutes, todayStr, weekdayIndex } from '../lib/dates'
import type { Priority, Task, TaskStatus } from '../types'

type View = 'day' | 'week' | 'month' | 'calendar'

const PRIORITY_TONE: Record<Priority, 'danger' | 'warn' | 'neutral'> = { high: 'danger', medium: 'warn', low: 'neutral' }

export default function Planner() {
  const { currentYear } = useApp()
  const { push } = useToast()
  const yearId = currentYear?.id ?? null
  const tasks = useTasks(yearId)
  const [view, setView] = useState<View>('day')
  const [anchor, setAnchor] = useState(todayStr())
  const [form, setForm] = useState<{ open: boolean; task?: Task; date?: string }>({ open: false })
  const [catchupOpen, setCatchupOpen] = useState(false)
  const [confirmId, setConfirmId] = useState<string | null>(null)

  const today = todayStr()
  const list = useMemo(() => tasks ?? [], [tasks])

  const grouped = useMemo(() => {
    const map = new Map<string, Task[]>()
    for (const t of list) {
      const key = t.dueDate ?? 'unscheduled'
      const arr = map.get(key) ?? []
      arr.push(t)
      map.set(key, arr)
    }
    for (const arr of map.values()) arr.sort((a, b) => a.order - b.order)
    return map
  }, [list])

  const weekDays = useMemo(() => {
    const start = addDays(anchor, -weekdayIndex(anchor))
    return Array.from({ length: 7 }, (_, i) => addDays(start, i))
  }, [anchor])

  const monthDays = useMemo(() => {
    const first = `${anchor.slice(0, 7)}-01`
    const startPad = weekdayIndex(first)
    const daysInMonth = new Date(Number(anchor.slice(0, 4)), Number(anchor.slice(5, 7)), 0).getDate()
    const cells: Array<string | null> = Array.from({ length: startPad }, () => null)
    for (let d = 1; d <= daysInMonth; d++) cells.push(`${anchor.slice(0, 7)}-${`${d}`.padStart(2, '0')}`)
    return cells
  }, [anchor])

  const shift = (dir: -1 | 1) => {
    if (view === 'day') setAnchor(addDays(anchor, dir))
    else if (view === 'week') setAnchor(addDays(anchor, dir * 7))
    else {
      const [y, m] = anchor.split('-').map(Number)
      const d = new Date(y, m - 1 + dir, 1)
      setAnchor(`${d.getFullYear()}-${`${d.getMonth() + 1}`.padStart(2, '0')}-01`)
    }
  }

  const toggle = async (task: Task) => {
    if (task.status === 'completed') await reopenTask(task)
    else {
      await completeTask(task)
      push('Task completed.', 'success')
    }
  }

  const taskRow = (task: Task) => (
    <li key={task.id} className="flex items-start gap-2 rounded-lg border border-line bg-surface px-2.5 py-2">
      <button
        aria-label={task.status === 'completed' ? `Reopen ${task.title}` : `Complete ${task.title}`}
        onClick={() => void toggle(task)}
        className={cx(
          'mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded border',
          task.status === 'completed' ? 'border-emerald-500 bg-emerald-500 text-white' : 'border-line text-transparent hover:border-accent',
        )}
      >
        <Check size={12} />
      </button>
      <div className="min-w-0 flex-1">
        <p className={cx('truncate text-sm', task.status === 'completed' && 'text-muted line-through')}>{task.title}</p>
        <p className="text-[11px] text-muted">
          {formatMinutes(task.estimatedMinutes)} · {task.priority} priority
          {task.dueDate ? ` · ${formatDate(task.dueDate)}` : ' · unscheduled'}
          {task.recurring !== 'none' ? ` · ${task.recurring}` : ''}
        </p>
        {task.description ? <p className="mt-0.5 line-clamp-2 text-[11px] text-muted">{task.description}</p> : null}
      </div>
      <div className="flex flex-col items-end gap-1">
        <Badge tone={task.status === 'completed' ? 'success' : PRIORITY_TONE[task.priority]}>{task.status === 'completed' ? 'Done' : task.priority}</Badge>
        <div className="flex gap-0.5">
          <button onClick={() => setForm({ open: true, task })} aria-label="Edit task" className="p-1 text-muted hover:text-ink">
            <Pencil size={12} />
          </button>
          <button onClick={() => setConfirmId(task.id)} aria-label="Delete task" className="p-1 text-muted hover:text-red-400">
            <Trash2 size={12} />
          </button>
        </div>
      </div>
    </li>
  )

  const dayTasks = (date: string) => grouped.get(date) ?? []
  const overdue = list.filter((t) => t.status !== 'completed' && t.dueDate && t.dueDate < today)

  return (
    <div>
      <PageHeader
        title="Planner"
        subtitle={`${list.filter((t) => t.status !== 'completed').length} open tasks`}
        action={
          <>
            <Button variant="secondary" size="sm" onClick={() => setCatchupOpen(true)}>
              <Sparkles size={14} /> Catch up
            </Button>
            <Button size="sm" onClick={() => setForm({ open: true, date: anchor })}>
              <Plus size={14} /> Task
            </Button>
          </>
        }
      />

      <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
        <Tabs
          value={view}
          onChange={setView}
          options={[
            { value: 'day', label: 'Day' },
            { value: 'week', label: 'Week' },
            { value: 'month', label: 'Month' },
            { value: 'calendar', label: 'Calendar' },
          ]}
        />
        <div className="flex items-center gap-1">
          <Button variant="ghost" size="sm" onClick={() => shift(-1)} aria-label="Previous">
            <ChevronLeft size={15} />
          </Button>
          <button onClick={() => setAnchor(today)} className="rounded-md border border-line px-2 py-1 text-[11px] text-muted">
            {view === 'day' ? formatDate(anchor) : anchor.slice(0, 7)}
          </button>
          <Button variant="ghost" size="sm" onClick={() => shift(1)} aria-label="Next">
            <ChevronRight size={15} />
          </Button>
        </div>
      </div>

      {view === 'day' ? (
        <div className="space-y-3">
          {overdue.length > 0 && anchor === today ? (
            <Card className="p-3">
              <p className="mb-2 text-xs font-semibold text-red-400">Overdue ({overdue.length})</p>
              <ul className="space-y-1.5">{overdue.map(taskRow)}</ul>
            </Card>
          ) : null}
          <Card className="p-3">
            <p className="mb-2 text-xs font-semibold text-muted">{formatDate(anchor)}</p>
            {dayTasks(anchor).length === 0 ? (
              <EmptyState title="No tasks for this day" action={<Button size="sm" onClick={() => setForm({ open: true, date: anchor })}>Add task</Button>} />
            ) : (
              <ul className="space-y-1.5">{dayTasks(anchor).map(taskRow)}</ul>
            )}
          </Card>
        </div>
      ) : null}

      {view === 'week' ? (
        <div className="grid grid-cols-1 gap-2 sm:grid-cols-2 lg:grid-cols-4">
          {weekDays.map((d) => (
            <Card key={d} className={cx('p-2.5', d === today && 'border-accent/60')}>
              <div className="mb-2 flex items-center justify-between">
                <p className="text-[11px] font-semibold">{formatDate(d)}</p>
                <button onClick={() => { setAnchor(d); setView('day') }} className="text-[10px] text-accent">
                  open
                </button>
              </div>
              {dayTasks(d).length === 0 ? (
                <p className="text-[11px] text-muted">—</p>
              ) : (
                <ul className="space-y-1">
                  {dayTasks(d).map((t) => (
                    <li key={t.id} className="flex items-start gap-1.5">
                      <button onClick={() => void toggle(t)} aria-label="Toggle task" className={cx('mt-0.5 h-3 w-3 shrink-0 rounded-sm border', t.status === 'completed' ? 'border-emerald-500 bg-emerald-500' : 'border-line')} />
                      <span className={cx('text-[11px]', t.status === 'completed' && 'text-muted line-through')}>{t.title}</span>
                    </li>
                  ))}
                </ul>
              )}
            </Card>
          ))}
        </div>
      ) : null}

      {view === 'month' ? (
        <div className="space-y-1.5">
          {[...grouped.entries()]
            .filter(([date]) => date.startsWith(anchor.slice(0, 7)))
            .sort((a, b) => a[0].localeCompare(b[0]))
            .map(([date, items]) => (
              <Card key={date} className="p-2.5">
                <p className="mb-1.5 text-[11px] font-semibold text-muted">{formatDate(date)}</p>
                <ul className="space-y-1">{items.map(taskRow)}</ul>
              </Card>
            ))}
          {[...grouped.keys()].filter((d) => d.startsWith(anchor.slice(0, 7))).length === 0 ? <EmptyState title="Nothing scheduled this month" /> : null}
        </div>
      ) : null}

      {view === 'calendar' ? (
        <Card className="p-2">
          <div className="grid grid-cols-7 gap-1 text-center text-[10px] text-muted">
            {['M', 'T', 'W', 'T', 'F', 'S', 'S'].map((d, i) => (
              <div key={`${d}${i}`} className="py-1">
                {d}
              </div>
            ))}
          </div>
          <div className="grid grid-cols-7 gap-1">
            {monthDays.map((d, i) => {
              if (!d) return <div key={`pad-${i}`} />
              const items = dayTasks(d)
              const open = items.filter((t) => t.status !== 'completed').length
              return (
                <button
                  key={d}
                  onClick={() => { setAnchor(d); setView('day') }}
                  className={cx(
                    'flex aspect-square flex-col items-center justify-center rounded-md border text-[11px]',
                    d === today ? 'border-accent bg-accent/10' : 'border-line hover:border-accent/50',
                  )}
                >
                  <span>{Number(d.slice(8))}</span>
                  {items.length > 0 ? (
                    <span className={cx('mt-0.5 h-1.5 w-1.5 rounded-full', open ? 'bg-accent' : 'bg-emerald-500')} />
                  ) : null}
                </button>
              )
            })}
          </div>
          <p className="mt-2 flex items-center gap-3 px-1 text-[10px] text-muted">
            <span className="flex items-center gap-1"><span className="h-1.5 w-1.5 rounded-full bg-accent" /> open</span>
            <span className="flex items-center gap-1"><span className="h-1.5 w-1.5 rounded-full bg-emerald-500" /> done</span>
          </p>
        </Card>
      ) : null}

      <TaskForm open={form.open} task={form.task} date={form.date} yearId={yearId} onClose={() => setForm({ open: false })} />

      <CatchupModal open={catchupOpen} onClose={() => setCatchupOpen(false)} tasks={list} />

      <ConfirmDialog
        open={Boolean(confirmId)}
        title="Delete task?"
        message="This task will be removed permanently."
        onCancel={() => setConfirmId(null)}
        onConfirm={async () => {
          if (confirmId) await deleteRow('tasks', confirmId)
          setConfirmId(null)
          push('Task deleted.', 'success')
        }}
      />
    </div>
  )
}

function TaskForm({ open, task, date, yearId, onClose }: { open: boolean; task?: Task; date?: string; yearId: string | null; onClose: () => void }) {
  const { push } = useToast()
  const { settings } = useApp()
  const [title, setTitle] = useState(task?.title ?? '')
  const [description, setDescription] = useState(task?.description ?? '')
  const [sel, setSel] = useState<SyllabusSelection>({ subjectId: task?.subjectId, chapterId: task?.chapterId, topicId: task?.topicId })
  const [priority, setPriority] = useState<Priority>(task?.priority ?? settings.defaultPriority)
  const [status, setStatus] = useState<TaskStatus>(task?.status ?? 'pending')
  const [startDate, setStartDate] = useState(task?.startDate ?? '')
  const [dueDate, setDueDate] = useState(task?.dueDate ?? date ?? todayStr())
  const [minutes, setMinutes] = useState(String(task?.estimatedMinutes ?? 30))
  const [recurring, setRecurring] = useState<Task['recurring']>(task?.recurring ?? 'none')

  const key = `${open}-${task?.id ?? 'new'}-${date ?? ''}`
  const [lastKey, setLastKey] = useState(key)
  if (key !== lastKey) {
    setLastKey(key)
    setTitle(task?.title ?? '')
    setDescription(task?.description ?? '')
    setSel({ subjectId: task?.subjectId, chapterId: task?.chapterId, topicId: task?.topicId })
    setPriority(task?.priority ?? settings.defaultPriority)
    setStatus(task?.status ?? 'pending')
    setStartDate(task?.startDate ?? '')
    setDueDate(task?.dueDate ?? date ?? todayStr())
    setMinutes(String(task?.estimatedMinutes ?? 30))
    setRecurring(task?.recurring ?? 'none')
  }

  const submit = async () => {
    if (!title.trim()) {
      push('A task title is required.', 'error')
      return
    }
    const payload = {
      title: title.trim(),
      description,
      subjectId: sel.subjectId,
      chapterId: sel.chapterId,
      topicId: sel.topicId,
      priority,
      status,
      startDate: startDate || undefined,
      dueDate: dueDate || undefined,
      estimatedMinutes: Number(minutes) || 30,
      recurring,
    }
    if (task) {
      await updateRow('tasks', task.id, payload)
      push('Task updated.', 'success')
    } else {
      if (!yearId) return
      await createRow('tasks', { ...payload, yearId, order: Date.now() })
      push('Task created.', 'success')
    }
    onClose()
  }

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={task ? 'Edit task' : 'New task'}
      wide
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            Cancel
          </Button>
          <Button onClick={() => void submit()}>{task ? 'Save' : 'Create'}</Button>
        </>
      }
    >
      <div className="space-y-3">
        <Field label="Title *">
          <Input value={title} onChange={(e) => setTitle(e.target.value)} />
        </Field>
        <Field label="Description">
          <Textarea value={description} onChange={(e) => setDescription(e.target.value)} />
        </Field>
        <SyllabusSelect value={sel} onChange={setSel} />
        <div className="grid grid-cols-2 gap-3">
          <Field label="Priority">
            <Select value={priority} onChange={(e) => setPriority(e.target.value as Priority)}>
              <option value="high">High</option>
              <option value="medium">Medium</option>
              <option value="low">Low</option>
            </Select>
          </Field>
          <Field label="Status">
            <Select value={status} onChange={(e) => setStatus(e.target.value as TaskStatus)}>
              <option value="pending">Pending</option>
              <option value="in_progress">In progress</option>
              <option value="completed">Completed</option>
            </Select>
          </Field>
        </div>
        <div className="grid grid-cols-2 gap-3">
          <Field label="Start date">
            <Input type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} />
          </Field>
          <Field label="Deadline">
            <Input type="date" value={dueDate} onChange={(e) => setDueDate(e.target.value)} />
          </Field>
        </div>
        <div className="grid grid-cols-2 gap-3">
          <Field label="Estimate (minutes)">
            <Input type="number" min={5} step={5} value={minutes} onChange={(e) => setMinutes(e.target.value)} />
          </Field>
          <Field label="Repeat">
            <Select value={recurring} onChange={(e) => setRecurring(e.target.value as Task['recurring'])}>
              <option value="none">None</option>
              <option value="daily">Daily</option>
              <option value="weekly">Weekly</option>
            </Select>
          </Field>
        </div>
      </div>
    </Modal>
  )
}

function CatchupModal({ open, onClose, tasks }: { open: boolean; onClose: () => void; tasks: Task[] }) {
  const { settings } = useApp()
  const { push } = useToast()
  const [suggestions, setSuggestions] = useState<CatchupSuggestion[] | null>(null)
  const [accepted, setAccepted] = useState<Record<string, boolean>>({})
  const [capacity, setCapacity] = useState(String(settings.dailyTargetMinutes))

  const run = () => {
    const result = planCatchup({
      today: todayStr(),
      tasks,
      dailyCapacityMinutes: Number(capacity) || 0,
      studyDays: settings.studyDays,
    })
    setSuggestions(result)
    setAccepted(Object.fromEntries(result.map((s) => [s.taskId, true])))
  }

  const apply = async () => {
    if (!suggestions) return
    let count = 0
    for (const s of suggestions) {
      if (!accepted[s.taskId]) continue
      await updateRow('tasks', s.taskId, { dueDate: s.date, startDate: s.date })
      count++
    }
    push(`${count} tasks rescheduled.`, 'success')
    setSuggestions(null)
    onClose()
  }

  const summary = suggestions ? summarisePlan(suggestions, Number(capacity) || 0) : []

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Smart catch-up planner"
      wide
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            Close
          </Button>
          {suggestions ? (
            <Button onClick={() => void apply()}>Apply {Object.values(accepted).filter(Boolean).length}</Button>
          ) : (
            <Button onClick={run} disabled={tasks.length === 0}>
              Generate plan
            </Button>
          )}
        </>
      }
    >
      {!suggestions ? (
        <div className="space-y-3">
          <p className="text-xs text-muted">
            This planner is deterministic: it looks at unfinished tasks, your daily capacity and your study days, then
            spreads the work without overlapping or overloading a day. Nothing is changed until you apply the plan.
          </p>
          <Field label="Daily study capacity (minutes)">
            <Input type="number" min={30} step={15} value={capacity} onChange={(e) => setCapacity(e.target.value)} />
          </Field>
          <p className="text-[11px] text-muted">
            Study days: {settings.studyDays.length ? settings.studyDays.map((d) => ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'][d]).join(', ') : 'every day'}
          </p>
        </div>
      ) : suggestions.length === 0 ? (
        <EmptyState title="Nothing to reschedule" hint="All tasks are either complete or already fit your capacity." />
      ) : (
        <div className="space-y-3">
          <div className="flex flex-wrap gap-1.5">
            {summary.map((d) => (
              <span key={d.date} className="rounded border border-line px-1.5 py-0.5 text-[10px] text-muted">
                {formatDate(d.date)} · {formatMinutes(d.planned)}/{formatMinutes(d.capacity)}
              </span>
            ))}
          </div>
          <ul className="max-h-72 space-y-1 overflow-y-auto">
            {suggestions.map((s) => (
              <li key={s.taskId} className="flex items-start gap-2 rounded-lg border border-line px-2 py-1.5">
                <input
                  type="checkbox"
                  checked={accepted[s.taskId] ?? false}
                  onChange={(e) => setAccepted((a) => ({ ...a, [s.taskId]: e.target.checked }))}
                  aria-label={`Accept ${s.title}`}
                  className="mt-1"
                />
                <div className="min-w-0 flex-1">
                  <p className="truncate text-xs">{s.title}</p>
                  <p className="text-[10px] text-muted">
                    {formatDate(s.date)} · {formatMinutes(s.minutes)} · {s.reason}
                  </p>
                </div>
                {s.atRisk ? <Badge tone="danger">At risk</Badge> : <Badge tone="success">Fits</Badge>}
              </li>
            ))}
          </ul>
          <Button variant="ghost" size="sm" onClick={run}>
            <RotateCcw size={13} /> Recalculate
          </Button>
        </div>
      )}
    </Modal>
  )
}

