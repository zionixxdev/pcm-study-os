import { useMemo, useState } from 'react'
import { useLiveQuery } from 'dexie-react-hooks'
import { Bar, BarChart, CartesianGrid, Cell, Line, LineChart, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { Badge, Card, EmptyState, Input, PageHeader, ProgressBar, SectionTitle, Select, Stat, Tabs } from '../components/ui'
import { useApp } from '../store/AppContext'
import { useYearData } from '../hooks/useYearData'
import { useRevisions, useSessions, computeStats } from '../hooks/useStats'
import { buildProgress } from '../lib/progress'
import { db } from '../lib/db'
import { addDays, formatMinutes, todayStr } from '../lib/dates'

type Range = '7' | '30' | '90' | 'year' | 'custom'

export default function Analytics() {
  const { currentYear } = useApp()
  const yearId = currentYear?.id ?? null
  const data = useYearData(yearId)
  const sessions = useSessions(yearId)
  const revisions = useRevisions(yearId)
  const exams = useLiveQuery(async () => (yearId ? db.exams.where({ yearId }).toArray() : []), [yearId])
  const practice = useLiveQuery(async () => (yearId ? db.practice.where({ yearId }).toArray() : []), [yearId])
  const mistakes = useLiveQuery(async () => (yearId ? db.mistakes.where({ yearId }).toArray() : []), [yearId])
  const [range, setRange] = useState<Range>('30')
  const [from, setFrom] = useState(addDays(todayStr(), -30))
  const [to, setTo] = useState(todayStr())

  const bounds = useMemo(() => {
    const today = todayStr()
    if (range === 'custom') return { start: from, end: to }
    if (range === 'year') return { start: currentYear?.startDate ?? addDays(today, -365), end: currentYear?.endDate ?? today }
    return { start: addDays(today, -Number(range) + 1), end: today }
  }, [range, from, to, currentYear])

  const filteredSessions = (sessions ?? []).filter((s) => {
    const d = s.startedAt.slice(0, 10)
    return d >= bounds.start && d <= bounds.end
  })
  const stats = useMemo(() => computeStats(filteredSessions), [filteredSessions])

  const progress = useMemo(
    () =>
      data
        ? buildProgress({ subjects: data.subjects, chapters: data.chapters, topics: data.topics, subtopics: data.subtopics, components: data.components })
        : null,
    [data],
  )

  const daily = useMemo(() => {
    const map = new Map<string, number>()
    for (const s of filteredSessions) {
      const d = s.startedAt.slice(0, 10)
      map.set(d, (map.get(d) ?? 0) + s.minutes)
    }
    const days: Array<{ date: string; minutes: number }> = []
    const span = Math.min(120, Math.max(1, Math.round((new Date(bounds.end).getTime() - new Date(bounds.start).getTime()) / 86400000) + 1))
    for (let i = span - 1; i >= 0; i--) {
      const d = addDays(bounds.end, -i)
      days.push({ date: d.slice(5), minutes: map.get(d) ?? 0 })
    }
    return days
  }, [filteredSessions, bounds])

  const bySubject = useMemo(() => {
    return (data?.subjects ?? []).map((s) => ({
      name: s.name,
      color: s.color,
      minutes: stats.bySubject[s.id] ?? 0,
      progress: progress?.subjectPct.get(s.id) ?? 0,
    }))
  }, [data, stats, progress])

  const testTrend = useMemo(
    () =>
      (exams ?? [])
        .filter((e) => typeof e.obtainedMarks === 'number' && e.maxMarks > 0)
        .sort((a, b) => a.date.localeCompare(b.date))
        .map((e) => ({ date: e.date.slice(5), score: Math.round(((e.obtainedMarks ?? 0) / e.maxMarks) * 100) })),
    [exams],
  )

  const accuracy = useMemo(() => {
    const rows = practice ?? []
    const attempted = rows.filter((r) => r.result !== 'skipped')
    const correct = rows.filter((r) => r.result === 'correct').length
    return attempted.length ? Math.round((correct / attempted.length) * 100) : 0
  }, [practice])

  const topicStrength = useMemo(() => {
    if (!progress) return { weak: [], strong: [] }
    const rated = (data?.topics ?? [])
      .filter((t) => !t.archived)
      .map((t) => ({ name: t.name, pct: progress.topicPct.get(t.id) ?? 0 }))
      .filter((t) => t.pct > 0)
      .sort((a, b) => a.pct - b.pct)
    return { weak: rated.slice(0, 5), strong: [...rated].reverse().slice(0, 5) }
  }, [data, progress])

  const revisionRate = useMemo(() => {
    const list = revisions ?? []
    const done = list.filter((r) => r.status === 'completed').length
    return list.length ? Math.round((done / list.length) * 100) : 0
  }, [revisions])

  const mistakeCategories = useMemo(() => {
    const map = new Map<string, number>()
    for (const m of mistakes ?? []) map.set(m.category, (map.get(m.category) ?? 0) + 1)
    return [...map.entries()].map(([name, value]) => ({ name, value }))
  }, [mistakes])

  const tooltipStyle = {
    background: 'rgb(var(--c-surface))',
    border: '1px solid rgb(var(--c-line))',
    borderRadius: 8,
    fontSize: 11,
  }

  const hasData = (sessions ?? []).length > 0 || (data?.topics.length ?? 0) > 0

  return (
    <div>
      <PageHeader title="Analytics" subtitle={`${currentYear?.name ?? ''} · ${bounds.start} → ${bounds.end}`} />

      <div className="mb-3 flex flex-wrap items-center gap-2">
        <Tabs
          value={range}
          onChange={setRange}
          options={[
            { value: '7', label: '7 days' },
            { value: '30', label: '30 days' },
            { value: '90', label: '90 days' },
            { value: 'year', label: 'Year' },
            { value: 'custom', label: 'Custom' },
          ]}
        />
        {range === 'custom' ? (
          <div className="flex items-center gap-1.5">
            <Input type="date" value={from} onChange={(e) => setFrom(e.target.value)} className="w-36" />
            <span className="text-xs text-muted">→</span>
            <Input type="date" value={to} onChange={(e) => setTo(e.target.value)} className="w-36" />
          </div>
        ) : null}
      </div>

      {!hasData ? (
        <EmptyState title="Not enough data yet" hint="Record study sessions, complete topics and log tests — these charts use only your real records." />
      ) : (
        <div className="space-y-3">
          <div className="grid grid-cols-2 gap-2 sm:grid-cols-4">
            <Stat label="Syllabus" value={`${progress?.overall ?? 0}%`} />
            <Stat label="Study time" value={formatMinutes(stats.totalMinutes)} />
            <Stat label="Daily average" value={formatMinutes(stats.avgDailyMinutes)} />
            <Stat label="Revision rate" value={`${revisionRate}%`} />
          </div>

          <Card className="p-3">
            <SectionTitle>Daily study time</SectionTitle>
            <div className="h-44 w-full">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={daily} margin={{ top: 4, right: 6, left: -24, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="rgb(var(--c-line))" vertical={false} />
                  <XAxis dataKey="date" tick={{ fontSize: 9, fill: 'rgb(var(--c-muted))' }} interval="preserveStartEnd" />
                  <YAxis tick={{ fontSize: 9, fill: 'rgb(var(--c-muted))' }} />
                  <Tooltip contentStyle={tooltipStyle} formatter={(v: number) => [`${v} min`, 'Study']} />
                  <Bar dataKey="minutes" fill="rgb(var(--c-accent))" radius={[3, 3, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </Card>

          <div className="grid gap-3 lg:grid-cols-2">
            <Card className="p-3">
              <SectionTitle>Subject progress</SectionTitle>
              <ul className="space-y-2">
                {bySubject.map((s) => (
                  <li key={s.name}>
                    <div className="flex items-center justify-between text-[11px]">
                      <span className="truncate">{s.name}</span>
                      <span className="text-muted">
                        {s.progress}% · {formatMinutes(s.minutes)}
                      </span>
                    </div>
                    <ProgressBar value={s.progress} className="mt-1" tone={s.color} />
                  </li>
                ))}
              </ul>
            </Card>

            <Card className="p-3">
              <SectionTitle>Study time by subject</SectionTitle>
              {stats.totalMinutes === 0 ? (
                <p className="text-xs text-muted">No sessions in this range.</p>
              ) : (
                <div className="h-40 w-full">
                  <ResponsiveContainer width="100%" height="100%">
                    <PieChart>
                      <Pie data={bySubject.filter((s) => s.minutes > 0)} dataKey="minutes" nameKey="name" innerRadius={35} outerRadius={60} paddingAngle={2}>
                        {bySubject.filter((s) => s.minutes > 0).map((s) => (
                          <Cell key={s.name} fill={s.color} />
                        ))}
                      </Pie>
                      <Tooltip contentStyle={tooltipStyle} formatter={(v: number) => [`${v} min`, 'Study']} />
                    </PieChart>
                  </ResponsiveContainer>
                </div>
              )}
            </Card>

            <Card className="p-3">
              <SectionTitle>Test performance</SectionTitle>
              {testTrend.length < 2 ? (
                <p className="text-xs text-muted">Log at least two graded tests to see a trend.</p>
              ) : (
                <div className="h-40 w-full">
                  <ResponsiveContainer width="100%" height="100%">
                    <LineChart data={testTrend} margin={{ top: 4, right: 8, left: -24, bottom: 0 }}>
                      <CartesianGrid strokeDasharray="3 3" stroke="rgb(var(--c-line))" vertical={false} />
                      <XAxis dataKey="date" tick={{ fontSize: 9, fill: 'rgb(var(--c-muted))' }} />
                      <YAxis domain={[0, 100]} tick={{ fontSize: 9, fill: 'rgb(var(--c-muted))' }} />
                      <Tooltip contentStyle={tooltipStyle} formatter={(v: number) => [`${v}%`, 'Score']} />
                      <Line type="monotone" dataKey="score" stroke="#39c6a4" strokeWidth={2} dot={{ r: 2 }} />
                    </LineChart>
                  </ResponsiveContainer>
                </div>
              )}
            </Card>

            <Card className="p-3">
              <SectionTitle>Weakest &amp; strongest topics</SectionTitle>
              <div className="grid grid-cols-2 gap-2">
                <div>
                  <p className="mb-1 text-[10px] uppercase tracking-wide text-red-400">Weakest</p>
                  <ul className="space-y-1">
                    {topicStrength.weak.length === 0 ? <li className="text-[11px] text-muted">—</li> : topicStrength.weak.map((t) => (
                      <li key={t.name} className="flex items-center justify-between gap-1 text-[11px]">
                        <span className="truncate">{t.name}</span>
                        <span className="text-muted">{t.pct}%</span>
                      </li>
                    ))}
                  </ul>
                </div>
                <div>
                  <p className="mb-1 text-[10px] uppercase tracking-wide text-emerald-400">Strongest</p>
                  <ul className="space-y-1">
                    {topicStrength.strong.length === 0 ? <li className="text-[11px] text-muted">—</li> : topicStrength.strong.map((t) => (
                      <li key={t.name} className="flex items-center justify-between gap-1 text-[11px]">
                        <span className="truncate">{t.name}</span>
                        <span className="text-muted">{t.pct}%</span>
                      </li>
                    ))}
                  </ul>
                </div>
              </div>
            </Card>
          </div>

          <div className="grid gap-2 sm:grid-cols-3">
            <Stat label="Numerical accuracy" value={`${accuracy}%`} hint={`${practice?.length ?? 0} questions logged`} />
            <Stat label="Mistakes logged" value={mistakes?.length ?? 0} hint={`${(mistakes ?? []).filter((m) => !m.resolved).length} unresolved`} />
            <Stat label="Topics completed" value={`${progress?.completedTopics ?? 0}/${progress?.totalTopics ?? 0}`} />
          </div>

          {mistakeCategories.length > 0 ? (
            <Card className="p-3">
              <SectionTitle>Mistake patterns</SectionTitle>
              <div className="flex flex-wrap gap-1.5">
                {mistakeCategories.sort((a, b) => b.value - a.value).map((c) => (
                  <Badge key={c.name} tone="warn">
                    {c.name}: {c.value}
                  </Badge>
                ))}
              </div>
            </Card>
          ) : null}

          <div className="flex items-center gap-2">
            <Select value={range} onChange={(e) => setRange(e.target.value as Range)} className="max-w-[160px]">
              <option value="7">Last 7 days</option>
              <option value="30">Last 30 days</option>
              <option value="90">Last 90 days</option>
              <option value="year">Academic year</option>
              <option value="custom">Custom</option>
            </Select>
            <p className="text-[11px] text-muted">All figures are computed from stored records.</p>
          </div>
        </div>
      )}
    </div>
  )
}
