import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Pause, Play, RotateCcw, Square, Timer as TimerIcon } from 'lucide-react'
import { Badge, Button, Card, Field, Input, Modal, PageHeader, ProgressBar, Tabs, Textarea, useToast } from '../components/ui'
import { useApp } from '../store/AppContext'
import { SyllabusSelect, type SyllabusSelection } from '../components/SyllabusPicker'
import { useSessions, computeStats } from '../hooks/useStats'
import { createRow } from '../lib/db'
import { formatMinutes, todayStr } from '../lib/dates'

type Mode = 'pomodoro' | 'focus' | 'stopwatch'
type Phase = 'focus' | 'short' | 'long'

interface PersistedTimer {
  mode: Mode
  phase: Phase
  running: boolean
  startedAt: number | null
  accumulatedMs: number
  plannedMs: number
  round: number
  sessionStart: string
  notes: string
  subjectId?: string
  chapterId?: string
  topicId?: string
}

const STORAGE_KEY = 'pcm-timer-state'

function load(): PersistedTimer | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? (JSON.parse(raw) as PersistedTimer) : null
  } catch {
    return null
  }
}

export default function TimerPage() {
  const { currentYear, settings } = useApp()
  const { push } = useToast()
  const yearId = currentYear?.id ?? null
  const sessions = useSessions(yearId)
  const stats = useMemo(() => computeStats(sessions ?? []), [sessions])

  const [mode, setMode] = useState<Mode>('pomodoro')
  const [phase, setPhase] = useState<Phase>('focus')
  const [running, setRunning] = useState(false)
  const [startedAt, setStartedAt] = useState<number | null>(null)
  const [accumulatedMs, setAccumulatedMs] = useState(0)
  const [plannedMs, setPlannedMs] = useState(settings.pomodoro.focus * 60000)
  const [round, setRound] = useState(1)
  const [sessionStart, setSessionStart] = useState(new Date().toISOString())
  const [notes, setNotes] = useState('')
  const [sel, setSel] = useState<SyllabusSelection>({})
  const [now, setNow] = useState(Date.now())
  const [summary, setSummary] = useState<null | { minutes: number; mode: Mode }>(null)
  const [customFocus, setCustomFocus] = useState(String(settings.pomodoro.focus))
  const restored = useRef(false)

  /* ------------------------- restore after reload ------------------------- */
  useEffect(() => {
    if (restored.current) return
    restored.current = true
    const saved = load()
    if (!saved) return
    setMode(saved.mode)
    setPhase(saved.phase)
    setAccumulatedMs(saved.accumulatedMs)
    setPlannedMs(saved.plannedMs)
    setRound(saved.round)
    setSessionStart(saved.sessionStart)
    setNotes(saved.notes)
    setSel({ subjectId: saved.subjectId, chapterId: saved.chapterId, topicId: saved.topicId })
    if (saved.running && saved.startedAt) {
      // Keep counting only the time the tab was actually open is impossible
      // after a reload, so the gap is not credited. We resume from saved state.
      setStartedAt(Date.now())
      setRunning(true)
    }
  }, [])

  /* ------------------------------ persistence ----------------------------- */
  useEffect(() => {
    const state: PersistedTimer = {
      mode,
      phase,
      running,
      startedAt: running ? Date.now() : null,
      accumulatedMs,
      plannedMs,
      round,
      sessionStart,
      notes,
      ...sel,
    }
    localStorage.setItem(STORAGE_KEY, JSON.stringify(state))
  }, [mode, phase, running, accumulatedMs, plannedMs, round, sessionStart, notes, sel])

  /* -------------------------------- ticking ------------------------------- */
  useEffect(() => {
    if (!running) return
    const id = window.setInterval(() => setNow(Date.now()), 500)
    return () => window.clearInterval(id)
  }, [running])

  const elapsedMs = accumulatedMs + (running && startedAt ? Math.max(0, now - startedAt) : 0)
  const totalMs = mode === 'stopwatch' ? elapsedMs : plannedMs
  const remainingMs = Math.max(0, totalMs - elapsedMs)
  const progress = mode === 'stopwatch' ? 100 : Math.min(100, (elapsedMs / Math.max(1, plannedMs)) * 100)

  const reset = useCallback(() => {
    setRunning(false)
    setStartedAt(null)
    setAccumulatedMs(0)
    setSessionStart(new Date().toISOString())
  }, [])

  const start = () => {
    setStartedAt(Date.now())
    setRunning(true)
  }

  const pause = () => {
    if (running && startedAt) setAccumulatedMs((a) => a + (Date.now() - startedAt))
    setStartedAt(null)
    setRunning(false)
  }

  const saveSession = async (minutes: number) => {
    if (!yearId) {
      push('Create an academic year first.', 'error')
      return
    }
    if (minutes < 1) {
      push('Sessions under a minute are not recorded.', 'error')
      return
    }
    await createRow('sessions', {
      yearId,
      subjectId: sel.subjectId,
      chapterId: sel.chapterId,
      topicId: sel.topicId,
      startedAt: sessionStart,
      endedAt: new Date().toISOString(),
      minutes,
      mode: mode === 'pomodoro' ? 'pomodoro' : mode === 'focus' ? 'focus' : 'stopwatch',
      notes: notes.trim() || undefined,
    })
    setSummary({ minutes, mode })
    setNotes('')
    reset()
  }

  const stop = () => {
    const minutes = Math.round(elapsedMs / 60000)
    pause()
    void saveSession(minutes)
  }

  /* --------------------------- pomodoro completion ------------------------ */
  useEffect(() => {
    if (!running || mode === 'stopwatch') return
    if (remainingMs > 0) return
    const minutes = Math.round(elapsedMs / 60000)
    setRunning(false)
    setStartedAt(null)
    void (async () => {
      await saveSession(minutes)
      if (phase === 'focus') {
        const nextRound = round + 1
        const longBreak = nextRound % (settings.pomodoro.rounds || 4) === 1
        setPhase(longBreak ? 'long' : 'short')
        setPlannedMs((longBreak ? settings.pomodoro.long : settings.pomodoro.short) * 60000)
        setRound(nextRound)
      } else {
        setPhase('focus')
        setPlannedMs(settings.pomodoro.focus * 60000)
      }
      setAccumulatedMs(0)
    })()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [remainingMs, running, mode])

  const applyMode = (m: Mode) => {
    pause()
    setMode(m)
    setAccumulatedMs(0)
    setPhase('focus')
    if (m === 'pomodoro') setPlannedMs(settings.pomodoro.focus * 60000)
    if (m === 'focus') setPlannedMs((Number(customFocus) || 25) * 60000)
    if (m === 'stopwatch') setPlannedMs(0)
    setSessionStart(new Date().toISOString())
  }

  const display = () => {
    const ms = mode === 'stopwatch' ? elapsedMs : remainingMs
    const totalSeconds = Math.floor(ms / 1000)
    const h = Math.floor(totalSeconds / 3600)
    const m = Math.floor((totalSeconds % 3600) / 60)
    const s = totalSeconds % 60
    return h > 0 ? `${h}:${`${m}`.padStart(2, '0')}:${`${s}`.padStart(2, '0')}` : `${`${m}`.padStart(2, '0')}:${`${s}`.padStart(2, '0')}`
  }

  return (
    <div>
      <PageHeader title="Focus timer" subtitle={`${formatMinutes(stats.todayMinutes)} studied today`} />

      <div className="grid gap-3 lg:grid-cols-3">
        <Card className="p-4 lg:col-span-2">
          <Tabs
            value={mode}
            onChange={applyMode}
            options={[
              { value: 'pomodoro', label: 'Pomodoro' },
              { value: 'focus', label: 'Custom focus' },
              { value: 'stopwatch', label: 'Stopwatch' },
            ]}
          />

          <div className="my-6 flex flex-col items-center">
            <div className="flex items-center gap-2">
              <Badge tone={phase === 'focus' ? 'accent' : 'success'}>
                {mode === 'stopwatch' ? 'Stopwatch' : phase === 'focus' ? `Focus · round ${round}` : phase === 'short' ? 'Short break' : 'Long break'}
              </Badge>
              {running ? <Badge tone="warn">Running</Badge> : null}
            </div>
            <p className="mt-3 font-mono text-5xl font-semibold tabular-nums sm:text-6xl">{display()}</p>
            {mode !== 'stopwatch' ? <ProgressBar value={progress} className="mt-4 w-full max-w-sm" /> : null}

            <div className="mt-6 flex flex-wrap items-center justify-center gap-2">
              {running ? (
                <Button variant="secondary" onClick={pause}>
                  <Pause size={16} /> Pause
                </Button>
              ) : (
                <Button onClick={start}>
                  <Play size={16} /> {accumulatedMs > 0 ? 'Resume' : 'Start'}
                </Button>
              )}
              <Button variant="secondary" onClick={() => void stop()} disabled={elapsedMs < 30000}>
                <Square size={15} /> Stop &amp; save
              </Button>
              <Button variant="ghost" onClick={reset} aria-label="Reset timer">
                <RotateCcw size={15} />
              </Button>
            </div>
            <p className="mt-2 text-[11px] text-muted">
              {elapsedMs < 30000 ? 'A session is saved once you have focused for at least 30 seconds.' : 'Only the time you actually focus is recorded.'}
            </p>
          </div>

          <div className="space-y-3 border-t border-line pt-3">
            <SyllabusSelect value={sel} onChange={setSel} />
            {mode === 'focus' ? (
              <Field label="Custom focus length (minutes)">
                <div className="flex gap-2">
                  <Input type="number" min={1} max={240} value={customFocus} onChange={(e) => setCustomFocus(e.target.value)} />
                  <Button variant="secondary" onClick={() => setPlannedMs((Number(customFocus) || 25) * 60000)}>
                    Apply
                  </Button>
                </div>
              </Field>
            ) : null}
            <Field label="Session notes">
              <Textarea value={notes} onChange={(e) => setNotes(e.target.value)} placeholder="What did you cover?" className="min-h-[60px]" />
            </Field>
          </div>
        </Card>

        <div className="space-y-3">
          <Card className="p-3">
            <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-muted">Pomodoro settings</p>
            <div className="grid grid-cols-2 gap-2 text-xs">
              <div className="rounded-lg border border-line p-2">
                <p className="text-[10px] text-muted">Focus</p>
                <p className="font-medium">{settings.pomodoro.focus} min</p>
              </div>
              <div className="rounded-lg border border-line p-2">
                <p className="text-[10px] text-muted">Short break</p>
                <p className="font-medium">{settings.pomodoro.short} min</p>
              </div>
              <div className="rounded-lg border border-line p-2">
                <p className="text-[10px] text-muted">Long break</p>
                <p className="font-medium">{settings.pomodoro.long} min</p>
              </div>
              <div className="rounded-lg border border-line p-2">
                <p className="text-[10px] text-muted">Rounds</p>
                <p className="font-medium">{settings.pomodoro.rounds}</p>
              </div>
            </div>
            <p className="mt-2 text-[11px] text-muted">Change these in Settings → Study preferences.</p>
          </Card>

          <Card className="p-3">
            <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-muted">Recent sessions</p>
            {(sessions ?? []).length === 0 ? (
              <p className="text-[11px] text-muted">No sessions recorded yet.</p>
            ) : (
              <ul className="space-y-1">
                {[...(sessions ?? [])]
                  .sort((a, b) => b.startedAt.localeCompare(a.startedAt))
                  .slice(0, 6)
                  .map((s) => (
                    <li key={s.id} className="flex items-center justify-between gap-2 text-[11px]">
                      <span className="flex items-center gap-1.5 text-muted">
                        <TimerIcon size={11} />
                        {s.startedAt.slice(0, 10) === todayStr() ? 'Today' : s.startedAt.slice(0, 10)}
                      </span>
                      <span className="tabular-nums">{formatMinutes(s.minutes)}</span>
                    </li>
                  ))}
              </ul>
            )}
          </Card>
        </div>
      </div>

      <Modal
        open={Boolean(summary)}
        onClose={() => setSummary(null)}
        title="Session saved"
        footer={<Button onClick={() => setSummary(null)}>Done</Button>}
      >
        <p className="text-sm">
          You focused for <strong>{formatMinutes(summary?.minutes ?? 0)}</strong>
          {summary?.mode === 'pomodoro' ? ' in this pomodoro.' : '.'}
        </p>
        <p className="mt-2 text-xs text-muted">Today&apos;s total: {formatMinutes(stats.todayMinutes)}</p>
      </Modal>
    </div>
  )
}

