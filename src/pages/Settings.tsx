import { useEffect, useMemo, useRef, useState } from 'react'
import { useLiveQuery } from 'dexie-react-hooks'
import {
  Bell,
  Database,
  Download,
  FileSpreadsheet,
  FileText,
  LogOut,
  Palette,
  Plus,
  Trash2,
  Upload,
  User,
  Users,
} from 'lucide-react'
import {
  Badge,
  Button,
  Card,
  ConfirmDialog,
  Field,
  Input,
  Modal,
  PageHeader,
  SectionTitle,
  Select,
  Stat,
  Textarea,
  Toggle,
  cx,
  useToast,
} from '../components/ui'
import { useApp } from '../store/AppContext'
import { useYearData } from '../hooks/useYearData'
import { useRevisions, useSessions, useTasks, computeStats } from '../hooks/useStats'
import { buildProgress } from '../lib/progress'
import { createRow, db, deleteRow, updateRow, clearAllData } from '../lib/db'
import { installSyllabus } from '../lib/seed/install'
import {
  downloadBackup,
  exportCsv,
  exportPdf,
  importBackup,
  validateBackup,
  type BackupFile,
} from '../lib/export'
import { supabaseConfigured, deleteCloudAccount, getEnabledProviders } from '../lib/supabase'
import { formatDate, formatMinutes, todayStr } from '../lib/dates'
import type { AcademicYear, Priority } from '../types'

const ACCENTS = ['#4f8cff', '#39c6a4', '#f2a03d', '#c084fc', '#f87171', '#38bdf8', '#a3e635']
const DAYS = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']

export default function SettingsPage() {
  const app = useApp()
  const { push } = useToast()
  const { settings, saveSettings, years, currentYear, setCurrentYear, userEmail, signedIn, signInGoogle, signOutUser, sync, syncNow } = app
  const yearId = currentYear?.id ?? null
  const data = useYearData(yearId)
  const tasks = useTasks(yearId)
  const sessions = useSessions(yearId)
  const revisions = useRevisions(yearId)
  const exams = useLiveQuery(async () => (yearId ? db.exams.where({ yearId }).toArray() : []), [yearId])
  const practice = useLiveQuery(async () => (yearId ? db.practice.where({ yearId }).toArray() : []), [yearId])
  const pending = useLiveQuery(async () => db.outbox.count(), [])

  const [yearForm, setYearForm] = useState<{ open: boolean; year?: AcademicYear }>({ open: false })
  const [confirm, setConfirm] = useState<null | { title: string; message: string; confirmLabel?: string; onConfirm: () => void }>(null)
  const [backupPreview, setBackupPreview] = useState<{ backup: BackupFile; counts: Record<string, number>; total: number } | null>(null)
  const fileRef = useRef<HTMLInputElement>(null)
  const [notifPermission, setNotifPermission] = useState<string>(typeof Notification !== 'undefined' ? Notification.permission : 'unsupported')
  const [googleEnabled, setGoogleEnabled] = useState<boolean | null>(null)

  useEffect(() => {
    if (!supabaseConfigured) return
    let alive = true
    void getEnabledProviders().then((providers) => {
      if (alive && providers) setGoogleEnabled(providers.google)
    })
    return () => {
      alive = false
    }
  }, [])

  const progress = useMemo(
    () => (data ? buildProgress({ subjects: data.subjects, chapters: data.chapters, topics: data.topics, subtopics: data.subtopics, components: data.components }) : null),
    [data],
  )
  const stats = useMemo(() => computeStats(sessions ?? []), [sessions])

  /* ------------------------------ exports ------------------------------ */

  const exportSubjects = () =>
    exportCsv('subjects.csv', (data?.subjects ?? []) as unknown as Record<string, unknown>[], [
      { key: 'name', label: 'Subject' },
      { key: 'description', label: 'Description' },
      { key: 'priority', label: 'Priority' },
      { key: 'progress', label: 'Progress %', get: (r) => progress?.subjectPct.get((r as { id: string }).id) ?? 0 },
    ])

  const exportChapters = () =>
    exportCsv('chapters.csv', (data?.chapters ?? []) as unknown as Record<string, unknown>[], [
      { key: 'name', label: 'Chapter' },
      { key: 'subject', label: 'Subject', get: (r) => data?.subjects.find((s) => s.id === (r as { subjectId: string }).subjectId)?.name ?? '' },
      { key: 'priority', label: 'Priority' },
      { key: 'deadline', label: 'Deadline' },
      { key: 'progress', label: 'Progress %', get: (r) => progress?.chapterPct.get((r as { id: string }).id) ?? 0 },
    ])

  const exportTopics = () =>
    exportCsv('topics.csv', (data?.topics ?? []) as unknown as Record<string, unknown>[], [
      { key: 'name', label: 'Topic' },
      { key: 'chapter', label: 'Chapter', get: (r) => data?.chapters.find((c) => c.id === (r as { chapterId: string }).chapterId)?.name ?? '' },
      { key: 'status', label: 'Status' },
      { key: 'priority', label: 'Priority' },
      { key: 'progress', label: 'Progress %', get: (r) => progress?.topicPct.get((r as { id: string }).id) ?? 0 },
    ])

  const exportTasksCsv = () =>
    exportCsv('tasks.csv', (tasks ?? []) as unknown as Record<string, unknown>[], [
      { key: 'title', label: 'Task' },
      { key: 'status', label: 'Status' },
      { key: 'priority', label: 'Priority' },
      { key: 'dueDate', label: 'Due' },
      { key: 'estimatedMinutes', label: 'Estimate (min)' },
    ])

  const exportSessionsCsv = () =>
    exportCsv('study-sessions.csv', (sessions ?? []) as unknown as Record<string, unknown>[], [
      { key: 'startedAt', label: 'Started' },
      { key: 'minutes', label: 'Minutes' },
      { key: 'mode', label: 'Mode' },
      { key: 'notes', label: 'Notes' },
    ])

  const exportRevisionsCsv = () =>
    exportCsv('revisions.csv', (revisions ?? []) as unknown as Record<string, unknown>[], [
      { key: 'label', label: 'Item' },
      { key: 'dueDate', label: 'Due' },
      { key: 'status', label: 'Status' },
      { key: 'lastRating', label: 'Last rating' },
      { key: 'intervalIndex', label: 'Interval index' },
    ])

  const exportTestsCsv = () =>
    exportCsv('test-results.csv', (exams ?? []) as unknown as Record<string, unknown>[], [
      { key: 'name', label: 'Test' },
      { key: 'type', label: 'Type' },
      { key: 'date', label: 'Date' },
      { key: 'obtainedMarks', label: 'Obtained' },
      { key: 'maxMarks', label: 'Max' },
      { key: 'percentage', label: '%', get: (r) => {
        const e = r as { obtainedMarks?: number; maxMarks: number }
        return typeof e.obtainedMarks === 'number' && e.maxMarks ? Math.round((e.obtainedMarks / e.maxMarks) * 100) : ''
      } },
    ])

  const exportPracticeCsv = () =>
    exportCsv('practice.csv', (practice ?? []) as unknown as Record<string, unknown>[], [
      { key: 'title', label: 'Question' },
      { key: 'result', label: 'Result' },
      { key: 'difficulty', label: 'Difficulty' },
      { key: 'attemptDate', label: 'Attempted' },
      { key: 'retry', label: 'Retry' },
    ])

  const pdfProgress = () => {
    const subjects = (data?.subjects ?? []).map((s) => [s.name, `${progress?.subjectPct.get(s.id) ?? 0}%`])
    const chapters = (data?.chapters ?? []).slice(0, 200).map((c) => [
      c.name,
      data?.subjects.find((s) => s.id === c.subjectId)?.name ?? '',
      `${progress?.chapterPct.get(c.id) ?? 0}%`,
      c.deadline ? formatDate(c.deadline) : '—',
    ])
    exportPdf('Academic progress report', `${currentYear?.name ?? ''} · generated ${formatDate(todayStr())}`, [
      {
        heading: 'Overall',
        columns: ['Metric', 'Value'],
        rows: [
          ['Syllabus completion', `${progress?.overall ?? 0}%`],
          ['Chapters completed', `${progress?.completedChapters ?? 0}/${progress?.totalChapters ?? 0}`],
          ['Topics completed', `${progress?.completedTopics ?? 0}/${progress?.totalTopics ?? 0}`],
          ['Study time logged', formatMinutes(stats.totalMinutes)],
          ['Current streak', `${stats.currentStreak} days`],
        ],
      },
      { heading: 'Subject progress', columns: ['Subject', 'Completion'], rows: subjects },
      { heading: 'Chapter completion', columns: ['Chapter', 'Subject', 'Completion', 'Deadline'], rows: chapters },
    ])
  }

  const pdfStudyTime = () =>
    exportPdf('Study time report', `${currentYear?.name ?? ''} · generated ${formatDate(todayStr())}`, [
      {
        heading: 'Totals',
        columns: ['Period', 'Study time'],
        rows: [
          ['Today', formatMinutes(stats.todayMinutes)],
          ['Last 7 days', formatMinutes(stats.weekMinutes)],
          ['Last 30 days', formatMinutes(stats.monthMinutes)],
          ['All time (this year)', formatMinutes(stats.totalMinutes)],
          ['Daily average', formatMinutes(stats.avgDailyMinutes)],
        ],
      },
      {
        heading: 'Last 14 days',
        columns: ['Date', 'Study time'],
        rows: [...(sessions ?? [])]
          .reduce<Record<string, number>>((acc, s) => {
            const d = s.startedAt.slice(0, 10)
            acc[d] = (acc[d] ?? 0) + s.minutes
            return acc
          }, {}) as unknown as never,
        note: 'Daily totals are derived from saved focus sessions.',
      },
    ])

  const pdfTests = () =>
    exportPdf('Test performance report', `${currentYear?.name ?? ''} · generated ${formatDate(todayStr())}`, [
      {
        heading: 'Tests',
        columns: ['Test', 'Type', 'Date', 'Marks', '%'],
        rows: (exams ?? []).map((e) => [
          e.name,
          e.type,
          formatDate(e.date),
          typeof e.obtainedMarks === 'number' ? `${e.obtainedMarks}/${e.maxMarks}` : `—/${e.maxMarks}`,
          typeof e.obtainedMarks === 'number' && e.maxMarks ? `${Math.round((e.obtainedMarks / e.maxMarks) * 100)}%` : '—',
        ]),
      },
    ])

  const pdfRevision = () =>
    exportPdf('Revision report', `${currentYear?.name ?? ''} · generated ${formatDate(todayStr())}`, [
      {
        heading: 'Revision items',
        columns: ['Item', 'Due', 'Status', 'Last rating'],
        rows: (revisions ?? []).map((r) => [r.label, formatDate(r.dueDate), r.status, r.lastRating ?? '—']),
      },
    ])

  /* ------------------------------ backup ------------------------------- */

  const onFile = async (file: File) => {
    try {
      const text = await file.text()
      const raw: unknown = JSON.parse(text)
      const validation = validateBackup(raw)
      if (!validation.ok) {
        push(validation.error ?? 'Invalid backup file.', 'error')
        return
      }
      setBackupPreview({ backup: raw as BackupFile, counts: validation.counts, total: validation.total })
    } catch {
      push('That file is not valid JSON.', 'error')
    }
  }

  const runImport = async (mode: 'merge' | 'replace') => {
    if (!backupPreview) return
    const result = await importBackup(backupPreview.backup, mode)
    setBackupPreview(null)
    push(`${result.inserted} records imported, ${result.skipped} skipped.`, 'success')
    await app.reloadYears()
  }

  const requestNotifications = async () => {
    if (typeof Notification === 'undefined') {
      setNotifPermission('unsupported')
      push('This browser does not support notifications.', 'error')
      return
    }
    const result = await Notification.requestPermission()
    setNotifPermission(result)
    if (result === 'granted') {
      await saveSettings({ notificationsEnabled: true })
      push('Reminders enabled.', 'success')
    } else {
      await saveSettings({ notificationsEnabled: false })
      push('Permission denied — in-app reminders will be used instead.', 'error')
    }
  }

  return (
    <div className="space-y-4">
      <PageHeader title="Settings" subtitle="Appearance, study preferences, academic years, data and account" />

      {/* --------------------------- Profile --------------------------- */}
      <Card className="p-3">
        <SectionTitle><span className="flex items-center gap-1.5"><User size={13} /> Profile</span></SectionTitle>
        <div className="space-y-2 text-sm">
          <div className="flex items-center justify-between gap-2">
            <span className="text-muted">Email</span>
            <span>{userEmail ?? 'Local device only'}</span>
          </div>
          <div className="flex items-center justify-between gap-2">
            <span className="text-muted">Cloud sync</span>
            <Badge tone={supabaseConfigured ? (sync.state === 'failed' ? 'danger' : 'success') : 'neutral'}>
              {supabaseConfigured ? sync.state : 'not configured'}
            </Badge>
          </div>
          {supabaseConfigured ? (
            signedIn ? (
              <Button variant="secondary" size="sm" onClick={() => void signOutUser()}>
                <LogOut size={14} /> Sign out
              </Button>
            ) : googleEnabled === false ? (
              <div className="rounded-lg border border-line bg-raised p-2.5 text-[11px] text-muted">
                <p className="font-medium text-ink">Google sign-in is not enabled yet</p>
                <p className="mt-1">
                  Enable it in Supabase → Authentication → Providers → Google, then add the OAuth client ID and secret
                  from Google Cloud Console. Redirect URI:{' '}
                  <code className="break-all">https://&lt;project-ref&gt;.supabase.co/auth/v1/callback</code>
                </p>
                <p className="mt-1">Until then the app runs in local-only mode — nothing is lost, nothing is uploaded.</p>
              </div>
            ) : (
              <Button
                size="sm"
                onClick={async () => {
                  try {
                    await signInGoogle()
                  } catch (err) {
                    push(err instanceof Error ? err.message : 'Sign-in failed.', 'error')
                  }
                }}
              >
                Continue with Google
              </Button>
            )
          ) : (
            <p className="text-[11px] text-muted">
              Add VITE_SUPABASE_URL and VITE_SUPABASE_ANON_KEY to enable Google sign-in and cloud sync. Everything works
              offline without them.
            </p>
          )}
        </div>
      </Card>

      {/* -------------------------- Appearance ------------------------- */}
      <Card className="p-3">
        <SectionTitle><span className="flex items-center gap-1.5"><Palette size={13} /> Appearance</span></SectionTitle>
        <div className="space-y-3">
          <Field label="Theme">
            <Select value={settings.theme} onChange={(e) => void saveSettings({ theme: e.target.value as typeof settings.theme })}>
              <option value="dark">Dark</option>
              <option value="light">Light</option>
              <option value="system">System</option>
            </Select>
          </Field>
          <Field label="Accent colour">
            <div className="flex flex-wrap gap-2">
              {ACCENTS.map((c) => (
                <button
                  key={c}
                  onClick={() => void saveSettings({ accent: c })}
                  aria-label={`Accent ${c}`}
                  className={cx('h-7 w-7 rounded-full border-2', settings.accent === c ? 'border-ink' : 'border-transparent')}
                  style={{ backgroundColor: c }}
                />
              ))}
            </div>
          </Field>
          <div className="grid grid-cols-2 gap-3">
            <Field label="Font size">
              <Select value={settings.fontSize} onChange={(e) => void saveSettings({ fontSize: e.target.value as typeof settings.fontSize })}>
                <option value="sm">Small</option>
                <option value="base">Default</option>
                <option value="lg">Large</option>
              </Select>
            </Field>
            <Field label="Compact mode">
              <div className="flex h-10 items-center gap-2">
                <Toggle checked={settings.compact} onChange={(v) => void saveSettings({ compact: v })} label="Compact mode" />
                <span className="text-[11px] text-muted">Tighter spacing</span>
              </div>
            </Field>
          </div>
        </div>
      </Card>

      {/* ---------------------- Study preferences ---------------------- */}
      <Card className="p-3">
        <SectionTitle><span className="flex items-center gap-1.5"><Bell size={13} /> Study preferences</span></SectionTitle>
        <div className="space-y-3">
          <div className="grid grid-cols-2 gap-3">
            <Field label="Daily study target (minutes)">
              <Input
                type="number"
                min={15}
                step={15}
                value={settings.dailyTargetMinutes}
                onChange={(e) => void saveSettings({ dailyTargetMinutes: Number(e.target.value) || 0 })}
              />
            </Field>
            <Field label="Default task priority">
              <Select value={settings.defaultPriority} onChange={(e) => void saveSettings({ defaultPriority: e.target.value as Priority })}>
                <option value="high">High</option>
                <option value="medium">Medium</option>
                <option value="low">Low</option>
              </Select>
            </Field>
          </div>
          <Field label="Preferred study days">
            <div className="flex flex-wrap gap-1">
              {DAYS.map((d, i) => {
                const active = settings.studyDays.includes(i)
                return (
                  <button
                    key={d}
                    onClick={() =>
                      void saveSettings({
                        studyDays: active ? settings.studyDays.filter((x) => x !== i) : [...settings.studyDays, i].sort(),
                      })
                    }
                    className={cx('rounded-md border px-2 py-1 text-[11px]', active ? 'border-accent bg-accent/15 text-accent' : 'border-line text-muted')}
                  >
                    {d}
                  </button>
                )
              })}
            </div>
          </Field>
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
            <Field label="Focus (min)">
              <Input type="number" min={1} value={settings.pomodoro.focus} onChange={(e) => void saveSettings({ pomodoro: { ...settings.pomodoro, focus: Number(e.target.value) || 25 } })} />
            </Field>
            <Field label="Short break">
              <Input type="number" min={1} value={settings.pomodoro.short} onChange={(e) => void saveSettings({ pomodoro: { ...settings.pomodoro, short: Number(e.target.value) || 5 } })} />
            </Field>
            <Field label="Long break">
              <Input type="number" min={1} value={settings.pomodoro.long} onChange={(e) => void saveSettings({ pomodoro: { ...settings.pomodoro, long: Number(e.target.value) || 15 } })} />
            </Field>
            <Field label="Rounds">
              <Input type="number" min={1} value={settings.pomodoro.rounds} onChange={(e) => void saveSettings({ pomodoro: { ...settings.pomodoro, rounds: Number(e.target.value) || 4 } })} />
            </Field>
          </div>
          <Field label="Revision intervals (days)" hint="Comma separated; the spaced-repetition schedule uses these in order.">
            <Input
              value={settings.revisionIntervals.join(', ')}
              onChange={(e) => {
                const nums = e.target.value
                  .split(',')
                  .map((v) => Number(v.trim()))
                  .filter((n) => Number.isFinite(n) && n > 0)
                if (nums.length) void saveSettings({ revisionIntervals: nums })
              }}
            />
          </Field>
        </div>
      </Card>

      {/* -------------------------- Academic -------------------------- */}
      <Card className="p-3">
        <SectionTitle
          action={
            <Button size="sm" variant="secondary" onClick={() => setYearForm({ open: true })}>
              <Plus size={13} /> Year
            </Button>
          }
        >
          <span className="flex items-center gap-1.5"><Users size={13} /> Academic years</span>
        </SectionTitle>
        <ul className="space-y-1.5">
          {years.map((y) => (
            <li key={y.id} className="flex flex-wrap items-center justify-between gap-2 rounded-lg border border-line px-2 py-1.5">
              <div className="min-w-0">
                <p className="flex items-center gap-1.5 text-sm">
                  <span className="truncate">{y.name}</span>
                  {y.id === currentYear?.id ? <Badge tone="accent">current</Badge> : null}
                  {y.archived ? <Badge>archived</Badge> : null}
                </p>
                <p className="text-[10px] text-muted">
                  {formatDate(y.startDate)} → {formatDate(y.endDate)}
                </p>
              </div>
              <div className="flex items-center gap-1">
                {y.id !== currentYear?.id ? (
                  <Button size="sm" variant="ghost" onClick={() => void setCurrentYear(y.id)}>
                    Use
                  </Button>
                ) : null}
                <Button size="sm" variant="ghost" onClick={() => setYearForm({ open: true, year: y })}>
                  Edit
                </Button>
                <Button size="sm" variant="ghost" onClick={() => void updateRow('academicYears', y.id, { archived: !y.archived })}>
                  {y.archived ? 'Restore' : 'Archive'}
                </Button>
                {years.length > 1 ? (
                  <button
                    aria-label="Delete year"
                    className="p-1 text-muted hover:text-red-400"
                    onClick={() =>
                      setConfirm({
                        title: 'Delete academic year?',
                        message: `Deleting "${y.name}" removes its subjects, chapters and topics. Archiving keeps the history instead.`,
                        onConfirm: async () => {
                          await deleteRow('academicYears', y.id)
                          if (y.id === currentYear?.id) {
                            const next = years.find((x) => x.id !== y.id)
                            if (next) await setCurrentYear(next.id)
                          }
                          await app.reloadYears()
                          push('Academic year deleted.', 'success')
                        },
                      })
                    }
                  >
                    <Trash2 size={13} />
                  </button>
                ) : null}
              </div>
            </li>
          ))}
        </ul>
        <p className="mt-2 text-[11px] text-muted">
          Archiving keeps every record. Progress, tasks and analytics always follow the year selected in the header.
        </p>
      </Card>

      {/* ----------------------------- Data ---------------------------- */}
      <Card className="p-3">
        <SectionTitle><span className="flex items-center gap-1.5"><Database size={13} /> Data</span></SectionTitle>
        <div className="space-y-3">
          <div className="flex flex-wrap gap-1.5">
            {[
              { label: 'Subjects CSV', run: exportSubjects },
              { label: 'Chapters CSV', run: exportChapters },
              { label: 'Topics CSV', run: exportTopics },
              { label: 'Tasks CSV', run: exportTasksCsv },
              { label: 'Sessions CSV', run: exportSessionsCsv },
              { label: 'Revisions CSV', run: exportRevisionsCsv },
              { label: 'Tests CSV', run: exportTestsCsv },
              { label: 'Practice CSV', run: exportPracticeCsv },
            ].map((b) => (
              <Button key={b.label} size="sm" variant="secondary" onClick={b.run}>
                <FileSpreadsheet size={12} /> {b.label}
              </Button>
            ))}
          </div>
          <div className="flex flex-wrap gap-1.5">
            <Button size="sm" variant="secondary" onClick={pdfProgress}>
              <FileText size={12} /> Progress report PDF
            </Button>
            <Button size="sm" variant="secondary" onClick={pdfStudyTime}>
              <FileText size={12} /> Study-time PDF
            </Button>
            <Button size="sm" variant="secondary" onClick={pdfTests}>
              <FileText size={12} /> Test report PDF
            </Button>
            <Button size="sm" variant="secondary" onClick={pdfRevision}>
              <FileText size={12} /> Revision PDF
            </Button>
          </div>
          <div className="flex flex-wrap gap-1.5 border-t border-line pt-3">
            <Button size="sm" onClick={() => void downloadBackup().then(() => push('Backup downloaded.', 'success'))}>
              <Download size={12} /> JSON backup
            </Button>
            <Button size="sm" variant="secondary" onClick={() => fileRef.current?.click()}>
              <Upload size={12} /> Restore backup
            </Button>
            <input
              ref={fileRef}
              type="file"
              accept="application/json"
              className="hidden"
              onChange={(e) => {
                const file = e.target.files?.[0]
                if (file) void onFile(file)
                e.target.value = ''
              }}
            />
            <Button size="sm" variant="ghost" onClick={() => void syncNow().then(() => push('Sync finished.', 'success'))}>
              Sync now
            </Button>
          </div>
          <div className="grid grid-cols-2 gap-2 sm:grid-cols-4">
            <Stat label="Pending ops" value={pending ?? 0} />
            <Stat label="Last sync" value={sync.lastSyncAt ? new Date(sync.lastSyncAt).toLocaleDateString() : '—'} />
            <Stat label="Sync state" value={sync.state} />
            <Stat label="Storage" value="This device" hint="IndexedDB" />
          </div>
          {sync.message ? <p className="text-[11px] text-amber-400">{sync.message}</p> : null}
        </div>
      </Card>

      {/* ------------------------- Notifications ----------------------- */}
      <Card className="p-3">
        <SectionTitle>Notifications</SectionTitle>
        <div className="flex flex-wrap items-center justify-between gap-2">
          <p className="max-w-md text-[11px] text-muted">
            Reminders for due revisions, deadlines and daily goals. Browser notifications only appear while the app is
            open — PCM Study OS does not run a push server. If permission is denied, in-app reminders are used instead.
          </p>
          <div className="flex items-center gap-2">
            <Badge tone={notifPermission === 'granted' ? 'success' : notifPermission === 'denied' ? 'danger' : 'neutral'}>{notifPermission}</Badge>
            <Toggle
              checked={settings.notificationsEnabled}
              onChange={async (v) => {
                if (v && notifPermission !== 'granted') await requestNotifications()
                else await saveSettings({ notificationsEnabled: v })
              }}
              label="Enable notifications"
            />
          </div>
        </div>
      </Card>

      {/* ---------------------------- Account -------------------------- */}
      <Card className="p-3">
        <SectionTitle>Account &amp; danger zone</SectionTitle>
        <div className="flex flex-wrap gap-1.5">
          <Button
            size="sm"
            variant="secondary"
            onClick={() =>
              setConfirm({
                title: 'Clear this device?',
                message: 'All local data on this device will be deleted. Export a backup first if you want to keep it.',
                confirmLabel: 'Delete local data',
                onConfirm: async () => {
                  await clearAllData()
                  window.location.reload()
                },
              })
            }
          >
            <Trash2 size={12} /> Delete local data
          </Button>
          {signedIn ? (
            <Button
              size="sm"
              variant="danger"
              onClick={() =>
                setConfirm({
                  title: 'Delete cloud account data?',
                  message: 'This calls the account-deletion function to remove your cloud records, then signs you out. Local data stays unless you clear it separately.',
                  confirmLabel: 'Delete cloud data',
                  onConfirm: async () => {
                    const { error } = await deleteCloudAccount()
                    if (error) push(error, 'error')
                    else push('Cloud data deletion requested.', 'success')
                  },
                })
              }
            >
              Delete cloud data
            </Button>
          ) : null}
          <Button
            size="sm"
            variant="ghost"
            onClick={() =>
              setConfirm({
                title: 'Reload default syllabus?',
                message: 'Adds the editable CBSE Class 11 seed subjects to the current year. Existing subjects are kept.',
                confirmLabel: 'Add syllabus',
                onConfirm: async () => {
                  if (!yearId) return
                  await installSyllabus(yearId)
                  push('Seed syllabus added.', 'success')
                },
              })
            }
          >
            Add CBSE seed syllabus
          </Button>
        </div>
        <p className="mt-2 text-[11px] text-muted">
          PCM Study OS v1.0 · local-first. Your data lives in this browser and, when signed in, in your own Supabase
          project.
        </p>
      </Card>

      <YearForm open={yearForm.open} year={yearForm.year} onClose={() => setYearForm({ open: false })} />

      <Modal
        open={Boolean(backupPreview)}
        onClose={() => setBackupPreview(null)}
        title="Restore backup"
        footer={
          <>
            <Button variant="ghost" onClick={() => setBackupPreview(null)}>
              Cancel
            </Button>
            <Button variant="secondary" onClick={() => void runImport('merge')}>
              Merge
            </Button>
            <Button
              variant="danger"
              onClick={() =>
                setConfirm({
                  title: 'Replace all local data?',
                  message: 'Existing records on this device will be overwritten by the backup.',
                  confirmLabel: 'Replace',
                  onConfirm: () => void runImport('replace'),
                })
              }
            >
              Replace
            </Button>
          </>
        }
      >
        <p className="text-sm">Backup from {backupPreview ? formatDate(backupPreview.backup.exportedAt.slice(0, 10)) : ''} with {backupPreview?.total} records.</p>
        <ul className="mt-2 max-h-48 space-y-0.5 overflow-y-auto text-[11px] text-muted">
          {Object.entries(backupPreview?.counts ?? {}).map(([k, v]) => (
            <li key={k} className="flex justify-between">
              <span>{k}</span>
              <span>{v}</span>
            </li>
          ))}
        </ul>
        <p className="mt-2 text-[11px] text-muted">Merge keeps existing records and adds missing ones. Replace overwrites everything.</p>
      </Modal>

      <ConfirmDialog
        open={Boolean(confirm)}
        title={confirm?.title ?? ''}
        message={confirm?.message ?? ''}
        confirmLabel={confirm?.confirmLabel}
        onCancel={() => setConfirm(null)}
        onConfirm={async () => {
          const fn = confirm?.onConfirm
          setConfirm(null)
          await fn?.()
        }}
      />
    </div>
  )
}

function YearForm({ open, year, onClose }: { open: boolean; year?: AcademicYear; onClose: () => void }) {
  const { push } = useToast()
  const { reloadYears } = useApp()
  const [name, setName] = useState(year?.name ?? '')
  const [start, setStart] = useState(year?.startDate ?? '2027-04-01')
  const [end, setEnd] = useState(year?.endDate ?? '2028-03-31')
  const [notes, setNotes] = useState(year?.notes ?? '')

  const key = `${open}-${year?.id ?? 'new'}`
  const [lastKey, setLastKey] = useState(key)
  if (key !== lastKey) {
    setLastKey(key)
    setName(year?.name ?? '')
    setStart(year?.startDate ?? '2027-04-01')
    setEnd(year?.endDate ?? '2028-03-31')
    setNotes(year?.notes ?? '')
  }

  const submit = async () => {
    if (!name.trim()) {
      push('A year name is required.', 'error')
      return
    }
    if (start >= end) {
      push('The end date must be after the start date.', 'error')
      return
    }
    if (year) {
      await updateRow('academicYears', year.id, { name: name.trim(), startDate: start, endDate: end, notes })
      push('Academic year updated.', 'success')
    } else {
      await createRow('academicYears', { name: name.trim(), startDate: start, endDate: end, isCurrent: false, notes })
      push('Academic year created.', 'success')
    }
    await reloadYears()
    onClose()
  }

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={year ? 'Edit academic year' : 'New academic year'}
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            Cancel
          </Button>
          <Button onClick={() => void submit()}>{year ? 'Save' : 'Create'}</Button>
        </>
      }
    >
      <div className="space-y-3">
        <Field label="Name *">
          <Input value={name} onChange={(e) => setName(e.target.value)} placeholder="Class 12 — 2027–28" />
        </Field>
        <div className="grid grid-cols-2 gap-3">
          <Field label="Start date">
            <Input type="date" value={start} onChange={(e) => setStart(e.target.value)} />
          </Field>
          <Field label="End date">
            <Input type="date" value={end} onChange={(e) => setEnd(e.target.value)} />
          </Field>
        </div>
        <Field label="Notes">
          <Textarea value={notes} onChange={(e) => setNotes(e.target.value)} />
        </Field>
      </div>
    </Modal>
  )
}
