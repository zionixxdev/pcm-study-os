import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { Search as SearchIcon } from 'lucide-react'
import { Badge, Card, EmptyState, Input, PageHeader, Select, Tabs } from '../components/ui'
import { useApp } from '../store/AppContext'
import { useYearData } from '../hooks/useYearData'
import { useLiveQuery } from 'dexie-react-hooks'
import { db } from '../lib/db'

type Kind = 'all' | 'subject' | 'chapter' | 'topic' | 'subtopic' | 'note' | 'formula' | 'task' | 'mistake' | 'test'

interface Hit {
  kind: Exclude<Kind, 'all'>
  title: string
  subtitle?: string
  to: string
}

export default function Search() {
  const { currentYear } = useApp()
  const yearId = currentYear?.id ?? null
  const data = useYearData(yearId)
  const [query, setQuery] = useState('')
  const [kind, setKind] = useState<Kind>('all')

  const extras = useLiveQuery(
    async () => {
      if (!yearId) return { notes: [], formulas: [], tasks: [], mistakes: [], exams: [] }
      const [notes, formulas, tasks, mistakes, exams] = await Promise.all([
        db.notes.where({ yearId }).toArray(),
        db.formulas.where({ yearId }).toArray(),
        db.tasks.where({ yearId }).toArray(),
        db.mistakes.where({ yearId }).toArray(),
        db.exams.where({ yearId }).toArray(),
      ])
      return { notes, formulas, tasks, mistakes, exams }
    },
    [yearId],
  )

  const hits = useMemo(() => {
    const q = query.trim().toLowerCase()
    if (!q) return []
    const out: Hit[] = []
    for (const s of data?.subjects ?? []) {
      if (s.name.toLowerCase().includes(q)) out.push({ kind: 'subject', title: s.name, subtitle: 'Subject', to: '/syllabus' })
    }
    for (const c of data?.chapters ?? []) {
      if (c.name.toLowerCase().includes(q)) {
        const subject = data?.subjects.find((s) => s.id === c.subjectId)?.name
        out.push({ kind: 'chapter', title: c.name, subtitle: subject, to: '/syllabus' })
      }
    }
    for (const t of data?.topics ?? []) {
      if (t.name.toLowerCase().includes(q)) {
        const chapter = data?.chapters.find((c) => c.id === t.chapterId)?.name
        out.push({ kind: 'topic', title: t.name, subtitle: chapter, to: '/syllabus' })
      }
    }
    for (const st of data?.subtopics ?? []) {
      if (st.name.toLowerCase().includes(q)) {
        const topic = data?.topics.find((t) => t.id === st.topicId)?.name
        out.push({ kind: 'subtopic', title: st.name, subtitle: topic, to: '/syllabus' })
      }
    }
    for (const n of extras?.notes ?? []) {
      if ((n.title + ' ' + n.body).toLowerCase().includes(q)) out.push({ kind: 'note', title: n.title, subtitle: 'Note', to: '/notes' })
    }
    for (const f of extras?.formulas ?? []) {
      if ((f.title + ' ' + f.expression).toLowerCase().includes(q)) out.push({ kind: 'formula', title: f.title, subtitle: f.category, to: '/formulas' })
    }
    for (const t of extras?.tasks ?? []) {
      if (t.title.toLowerCase().includes(q)) out.push({ kind: 'task', title: t.title, subtitle: t.status, to: '/planner' })
    }
    for (const m of extras?.mistakes ?? []) {
      if (m.question.toLowerCase().includes(q)) out.push({ kind: 'mistake', title: m.question, subtitle: m.category, to: '/mistakes' })
    }
    for (const e of extras?.exams ?? []) {
      if (e.name.toLowerCase().includes(q)) out.push({ kind: 'test', title: e.name, subtitle: e.date, to: '/tests' })
    }
    return out.filter((h) => kind === 'all' || h.kind === kind).slice(0, 80)
  }, [query, kind, data, extras])

  return (
    <div>
      <PageHeader title="Search" subtitle="Subjects, chapters, topics, notes, formulas, tasks, mistakes and tests" />

      <div className="relative mb-3">
        <SearchIcon size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-muted" />
        <Input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Search everything in this academic year…" className="h-11 pl-9" autoFocus />
      </div>

      <div className="mb-3 overflow-x-auto">
        <Tabs
          value={kind}
          onChange={setKind}
          options={[
            { value: 'all', label: 'All' },
            { value: 'topic', label: 'Topics' },
            { value: 'subtopic', label: 'Subtopics' },
            { value: 'note', label: 'Notes' },
            { value: 'formula', label: 'Formulas' },
            { value: 'task', label: 'Tasks' },
            { value: 'mistake', label: 'Mistakes' },
            { value: 'test', label: 'Tests' },
          ]}
        />
      </div>

      {query.trim() === '' ? (
        <EmptyState title="Start typing" hint="Results are scoped to the selected academic year." />
      ) : hits.length === 0 ? (
        <EmptyState title="No matches" hint="Try a shorter word, or check the academic year selected in the header." />
      ) : (
        <ul className="space-y-1">
          {hits.map((h, i) => (
            <li key={`${h.kind}-${h.title}-${i}`}>
              <Link to={h.to} className="block">
                <Card className="flex items-center justify-between gap-2 p-2.5 hover:border-accent/60">
                  <div className="min-w-0">
                    <p className="truncate text-sm">{h.title}</p>
                    {h.subtitle ? <p className="truncate text-[11px] text-muted">{h.subtitle}</p> : null}
                  </div>
                  <Badge tone="neutral">{h.kind}</Badge>
                </Card>
              </Link>
            </li>
          ))}
        </ul>
      )}

      <div className="mt-4">
        <Select value={kind} onChange={(e) => setKind(e.target.value as Kind)} className="max-w-xs">
          <option value="all">Filter: everything</option>
          <option value="topic">Topics</option>
          <option value="note">Notes</option>
          <option value="formula">Formulas</option>
          <option value="task">Tasks</option>
          <option value="mistake">Mistakes</option>
          <option value="test">Tests</option>
        </Select>
      </div>
    </div>
  )
}
