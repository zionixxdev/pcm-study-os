import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  ArrowRight,
  CalendarClock,
  Check,
  ChevronUp,
  ChevronDown,
  Eye,
  EyeOff,
  Flame,
  Plus,
  RotateCcw,
  Sparkles,
  Target,
} from 'lucide-react'
import { Badge, Button, Card, EmptyState, Modal, ProgressBar, SectionTitle, Stat, cx, useToast } from '../components/ui'
import { useApp } from '../store/AppContext'
import { useYearData } from '../hooks/useYearData'
import { useGoals, useRevisions, useSessions, useTasks, computeStats } from '../hooks/useStats'
import { buildProgress } from '../lib/progress'
import { effectiveStatus } from '../lib/revision'
import { completeTask, isOverdue } from '../lib/tasks'
import { formatDate, formatMinutes, todayStr } from '../lib/dates'
import { db } from '../lib/db'
import { useLiveQuery } from 'dexie-react-hooks'

const WIDGETS = [
  { key: 'progress', label: 'Overall progress' },
  { key: 'subjects', label: 'Subject progress' },
  { key: 'today', label: "Today's tasks" },
  { key: 'stats', label: 'Study statistics' },
  { key: 'streak', label: 'Study streak' },
  { key: 'revision', label: 'Revision overview' },
  { key: 'deadlines', label: 'Upcoming deadlines' },
  { key: 'actions', label: 'Quick actions' },
]

export default function Dashboard() {
  const { currentYear, prefs, savePrefs, settings } = useApp()
  const { push } = useToast()
  const yearId = currentYear?.id ?? null
  const data = useYearData(yearId)
  const tasks = useTasks(yearId)
  const sessions = useSessions(yearId)
  const revisions = useRevisions(yearId)
  const goals = useGoals(yearId)
  const exams = useLiveQuery(async () => (yearId ? db.exams.where({ yearId }).toArray() : []), [yearId])
  const [customise, setCustomise] = useState(false)
  const today = todayStr()

  const progress = useMemo(
    () =>
      data
        ? buildProgress({
            subjects: data.subjects,
            chapters: data.chapters,
            topics: data.topics,
            subtopics: data.subtopics,
            components: data.components,
          })
        : null,
    [data],
  )

  const stats = useMemo(() => computeStats(sessions ?? []), [sessions])

  const todaysTasks = useMemo(
    () => (tasks ?? []).filter((t) => t.dueDate === today || isOverdue(t, today)),
    [tasks, today],
  )
  const overdue = todaysTasks.filter((t) => isOverdue(t, today))

  const revisionBuckets = useMemo(() => {
    const list = revisions ?? []
    const due = list.filter((r) => effectiveStatus(r, today) === 'due')
    const late = list.filter((r) => effectiveStatus(r, today) === 'overdue')
    const soon = list.filter((r) => effectiveStatus(r, today) === 'upcoming').slice(0, 5)
    return { due, late, soon }
  }, [revisions, today])

  const deadlines = useMemo(() => {
    const items: Array<{ label: string; date: string; kind: string; to: string }> = []
    for (const t of tasks ?? []) {
      if (t.status !== 'completed' && t.dueDate && t.dueDate >= today) items.push({ label: t.title, date: t.dueDate, kind: 'Task', to: '/planner' })
    }
    for (const e of exams ?? []) {
      if (e.date >= today) items.push({ label: e.name, date: e.date, kind: 'Exam', to: '/tests' })
    }
    for (const g of goals ?? []) {
      if (g.status === 'active' && g.deadline) items.push({ label: g.title, date: g.deadline, kind: 'Goal', to: '/goals' })
    }
    for (const c of data?.chapters ?? []) {
      if (c.deadline && c.deadline >= today && (progress?.chapterPct.get(c.id) ?? 0) < 100) {
        items.push({ label: c.name, date: c.deadline, kind: 'Chapter', to: '/syllabus' })
      }
    }
    return items.sort((a, b) => a.date.localeCompare(b.date)).slice(0, 6)
  }, [tasks, exams, goals, data, progress, today])

  const visible = prefs.order.filter((k) => !prefs.hidden.includes(k))

  const toggleHidden = (key: string) => {
    const hidden = prefs.hidden.includes(key) ? prefs.hidden.filter((h) => h !== key) : [...prefs.hidden, key]
    void savePrefs({ hidden })
  }
  const move = (key: string, dir: -1 | 1) => {
    const order = [...prefs.order]
    const i = order.indexOf(key)
    const j = i + dir
    if (i < 0 || j < 0 || j >= order.length) return
    ;[order[i], order[j]] = [order[j], order[i]]
    void savePrefs({ order })
  }

  if (!currentYear) {
    return <EmptyState title="No academic year yet" hint="Create one in Settings to start tracking." action={<Link to="/settings"><Button>Open settings</Button></Link>} />
  }

  const widget = (key: string) => {
    switch (key) {
      case 'progress':
        return (
          <Card className="p-4" key={key}>
            <SectionTitle action={<Link to="/syllabus" className="text-xs text-accent">Open syllabus</Link>}>Overall progress</SectionTitle>
            <div className="flex items-end gap-3">
              <p className="text-3xl font-semibold tabular-nums">{progress?.overall ?? 0}%</p>
              <p className="pb-1 text-xs text-muted">of the {currentYear.name} syllabus</p>
            </div>
            <ProgressBar value={progress?.overall ?? 0} className="mt-3 h-2" />
            <div className="mt-4 grid grid-cols-2 gap-2 sm:grid-cols-4">
              <Stat label="Chapters" value={`${progress?.completedChapters ?? 0}/${progress?.totalChapters ?? 0}`} />
              <Stat label="Topics done" value={progress?.completedTopics ?? 0} />
              <Stat label="Remaining" value={progress?.remainingTopics ?? 0} />
              <Stat label="Subjects" value={data?.subjects.length ?? 0} />
            </div>
          </Card>
        )
      case 'subjects':
        return (
          <div key={key}>
            <SectionTitle action={<Link to="/syllabus" className="text-xs text-accent">Manage</Link>}>Subject progress</SectionTitle>
            {(data?.subjects.length ?? 0) === 0 ? (
              <EmptyState title="No subjects yet" hint="Add your first subject in Syllabus." />
            ) : (
              <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
                {data?.subjects.map((s) => {
                  const pct = progress?.subjectPct.get(s.id) ?? 0
                  const chapters = (data?.chapters ?? []).filter((c) => c.subjectId === s.id)
                  return (
                    <Card key={s.id} className="p-3">
                      <div className="flex items-center justify-between gap-2">
                        <div className="flex min-w-0 items-center gap-2">
                          <span className="h-2.5 w-2.5 shrink-0 rounded-full" style={{ backgroundColor: s.color }} />
                          <p className="truncate text-sm font-medium">{s.name}</p>
                        </div>
                        <span className="text-xs tabular-nums text-muted">{pct}%</span>
                      </div>
                      <ProgressBar value={pct} className="mt-2" tone={s.color} />
                      <p className="mt-1.5 text-[11px] text-muted">
                        {chapters.filter((c) => (progress?.chapterPct.get(c.id) ?? 0) === 100).length}/{chapters.length} chapters complete
                      </p>
                    </Card>
                  )
                })}
              </div>
            )}
          </div>
        )
      case 'today':
        return (
          <Card className="p-4" key={key}>
            <SectionTitle action={<Link to="/planner" className="text-xs text-accent">Planner</Link>}>Today&apos;s tasks</SectionTitle>
            {todaysTasks.length === 0 ? (
              <EmptyState title="Nothing due today" hint="Add a task from the planner or press N to quick-capture." />
            ) : (
              <ul className="space-y-1.5">
                {todaysTasks.slice(0, 6).map((t) => (
                  <li key={t.id} className="flex items-center gap-2 rounded-lg border border-line px-2.5 py-2">
                    <button
                      aria-label={`Complete ${t.title}`}
                      onClick={async () => {
                        await completeTask(t)
                        push('Task completed.', 'success')
                      }}
                      className="flex h-5 w-5 shrink-0 items-center justify-center rounded border border-line text-transparent hover:border-accent hover:text-accent"
                    >
                      <Check size={13} />
                    </button>
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-sm">{t.title}</p>
                      <p className="text-[11px] text-muted">
                        {formatMinutes(t.estimatedMinutes)} · {t.priority}
                        {isOverdue(t, today) ? ' · overdue' : ''}
                      </p>
                    </div>
                    {isOverdue(t, today) ? <Badge tone="danger">Overdue</Badge> : <Badge tone="neutral">{t.status.replace('_', ' ')}</Badge>}
                  </li>
                ))}
              </ul>
            )}
            {overdue.length > 0 ? <p className="mt-2 text-[11px] text-amber-400">{overdue.length} overdue — try the catch-up planner.</p> : null}
          </Card>
        )
      case 'stats':
        return (
          <div key={key}>
            <SectionTitle>Study statistics</SectionTitle>
            <div className="grid grid-cols-2 gap-2 sm:grid-cols-4">
              <Stat label="Today" value={formatMinutes(stats.todayMinutes)} hint={`target ${formatMinutes(settings.dailyTargetMinutes)}`} />
              <Stat label="This week" value={formatMinutes(stats.weekMinutes)} />
              <Stat label="This month" value={formatMinutes(stats.monthMinutes)} />
              <Stat label="Daily average" value={formatMinutes(stats.avgDailyMinutes)} hint="on active days" />
            </div>
          </div>
        )
      case 'streak':
        return (
          <Card className="p-4" key={key}>
            <SectionTitle>Study streak</SectionTitle>
            <div className="flex items-center gap-4">
              <div className="flex items-center gap-2">
                <Flame size={22} className={stats.currentStreak > 0 ? 'text-amber-400' : 'text-muted'} />
                <div>
                  <p className="text-xl font-semibold tabular-nums">{stats.currentStreak}</p>
                  <p className="text-[11px] text-muted">current streak</p>
                </div>
              </div>
              <div>
                <p className="text-xl font-semibold tabular-nums">{stats.longestStreak}</p>
                <p className="text-[11px] text-muted">longest streak</p>
              </div>
            </div>
            <div className="mt-3 flex items-end gap-1">
              {stats.last7.map((d) => {
                const h = Math.min(100, (d.minutes / Math.max(60, settings.dailyTargetMinutes)) * 100)
                return (
                  <div key={d.date} className="flex-1" title={`${formatDate(d.date)} · ${formatMinutes(d.minutes)}`}>
                    <div className="flex h-10 items-end rounded bg-raised">
                      <div className="w-full rounded bg-accent/70" style={{ height: `${Math.max(4, h)}%` }} />
                    </div>
                    <p className="mt-1 text-center text-[9px] text-muted">{d.date.slice(8)}</p>
                  </div>
                )
              })}
            </div>
          </Card>
        )
      case 'revision':
        return (
          <Card className="p-4" key={key}>
            <SectionTitle action={<Link to="/revision" className="text-xs text-accent">Review</Link>}>Revision overview</SectionTitle>
            <div className="grid grid-cols-3 gap-2">
              <Stat label="Due today" value={revisionBuckets.due.length} />
              <Stat label="Overdue" value={revisionBuckets.late.length} />
              <Stat label="Upcoming" value={revisionBuckets.soon.length} />
            </div>
            {revisionBuckets.due.length + revisionBuckets.late.length === 0 ? (
              <p className="mt-3 text-[11px] text-muted">Nothing to revise right now.</p>
            ) : (
              <ul className="mt-3 space-y-1">
                {[...revisionBuckets.late, ...revisionBuckets.due].slice(0, 4).map((r) => (
                  <li key={r.id} className="flex items-center justify-between gap-2 text-xs">
                    <span className="truncate">{r.label}</span>
                    <span className="text-muted">{formatDate(r.dueDate)}</span>
                  </li>
                ))}
              </ul>
            )}
          </Card>
        )
      case 'deadlines':
        return (
          <Card className="p-4" key={key}>
            <SectionTitle action={<CalendarClock size={14} className="text-muted" />}>Upcoming deadlines</SectionTitle>
            {deadlines.length === 0 ? (
              <p className="text-xs text-muted">No deadlines in the near future.</p>
            ) : (
              <ul className="space-y-1.5">
                {deadlines.map((d, i) => (
                  <li key={`${d.label}-${i}`}>
                    <Link to={d.to} className="flex items-center justify-between gap-2 rounded-lg px-1 py-1 hover:bg-raised">
                      <span className="flex min-w-0 items-center gap-2">
                        <Badge tone={d.kind === 'Exam' ? 'danger' : d.kind === 'Chapter' ? 'accent' : 'neutral'}>{d.kind}</Badge>
                        <span className="truncate text-xs">{d.label}</span>
                      </span>
                      <span className="shrink-0 text-[11px] text-muted">{formatDate(d.date)}</span>
                    </Link>
                  </li>
                ))}
              </ul>
            )}
          </Card>
        )
      case 'actions':
        return (
          <div key={key}>
            <SectionTitle>Quick actions</SectionTitle>
            <div className="grid grid-cols-2 gap-2 sm:grid-cols-4">
              {[
                { to: '/syllabus', label: 'Add subject', icon: Plus },
                { to: '/planner', label: 'Add task', icon: CalendarClock },
                { to: '/timer', label: 'Start timer', icon: Sparkles },
                { to: '/revision', label: 'Add revision', icon: RotateCcw },
                { to: '/notes', label: 'Add note', icon: Eye },
                { to: '/tests', label: 'Record test', icon: Target },
                { to: '/mistakes', label: 'Add mistake', icon: ChevronUp },
                { to: '/analytics', label: 'Analytics', icon: ArrowRight },
              ].map((a) => (
                <Link
                  key={a.label}
                  to={a.to}
                  className="flex items-center gap-2 rounded-lg border border-line bg-surface px-3 py-2.5 text-xs hover:border-accent/60"
                >
                  <a.icon size={15} className="text-accent" />
                  {a.label}
                </Link>
              ))}
            </div>
          </div>
        )
      default:
        return null
    }
  }

  return (
    <div>
      <div className="mb-5 flex flex-wrap items-end justify-between gap-2">
        <div>
          <h1 className="text-xl font-semibold tracking-tight sm:text-2xl">Dashboard</h1>
          <p className="mt-0.5 text-sm text-muted">
            {currentYear.name} · {formatDate(today)}
          </p>
        </div>
        <Button variant="secondary" size="sm" onClick={() => setCustomise(true)}>
          Customise
        </Button>
      </div>

      <div className={cx('grid gap-3', prefs.density === 'compact' ? 'gap-2' : '')}>
        {visible.map((key) => widget(key))}
      </div>

      <Modal open={customise} onClose={() => setCustomise(false)} title="Dashboard widgets">
        <p className="mb-3 text-xs text-muted">Show, hide and reorder widgets. Your layout is saved on this device.</p>
        <ul className="space-y-1.5">
          {prefs.order.map((key, i) => {
            const meta = WIDGETS.find((w) => w.key === key)
            const hidden = prefs.hidden.includes(key)
            return (
              <li key={key} className="flex items-center gap-2 rounded-lg border border-line px-2 py-1.5">
                <span className="flex-1 text-sm">{meta?.label ?? key}</span>
                <button onClick={() => move(key, -1)} disabled={i === 0} aria-label="Move up" className="p-1 text-muted disabled:opacity-30">
                  <ChevronUp size={15} />
                </button>
                <button onClick={() => move(key, 1)} disabled={i === prefs.order.length - 1} aria-label="Move down" className="p-1 text-muted disabled:opacity-30">
                  <ChevronDown size={15} />
                </button>
                <button onClick={() => toggleHidden(key)} aria-label={hidden ? 'Show widget' : 'Hide widget'} className="p-1 text-muted">
                  {hidden ? <EyeOff size={15} /> : <Eye size={15} />}
                </button>
              </li>
            )
          })}
        </ul>
        <div className="mt-4 flex items-center justify-between gap-2">
          <Button variant="ghost" size="sm" onClick={() => void savePrefs({ hidden: [], order: WIDGETS.map((w) => w.key), density: 'comfortable' })}>
            Reset layout
          </Button>
          <Button size="sm" onClick={() => setCustomise(false)}>
            Done
          </Button>
        </div>
      </Modal>
    </div>
  )
}
