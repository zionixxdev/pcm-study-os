import { useMemo, useState } from 'react'
import {
  Archive,
  ArrowLeft,
  Check,
  ChevronDown,
  ChevronRight,
  Copy,
  FileUp,
  GripVertical,
  Layers,
  Pencil,
  Plus,
  Trash2,
} from 'lucide-react'
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
  Textarea,
  Toggle,
  cx,
  useToast,
} from '../components/ui'
import { useApp } from '../store/AppContext'
import { useYearData } from '../hooks/useYearData'
import { buildProgress } from '../lib/progress'
import {
  createRow,
  createRows,
  db,
  deleteChapterCascade,
  deleteSubjectCascade,
  deleteRow,
  deleteTopicCascade,
  updateRow,
} from '../lib/db'
import { markDuplicates, parseAuto, type ParsedChapter } from '../lib/parseImport'
import { todayStr } from '../lib/dates'
import type { Chapter, ComponentType, Priority, Subject, Topic, TopicStatus } from '../types'

const COLORS = ['#4f8cff', '#39c6a4', '#f2a03d', '#c084fc', '#f87171', '#38bdf8', '#a3e635', '#fb7185']
const STATUS_LABEL: Record<TopicStatus, string> = {
  not_started: 'Not started',
  in_progress: 'In progress',
  completed: 'Completed',
  needs_revision: 'Needs revision',
}
const COMPONENT_LABEL: Record<ComponentType, string> = {
  theory: 'Theory',
  formulas: 'Formulas',
  derivations: 'Derivations',
  ncert: 'NCERT',
  numericals: 'Numericals',
  questions: 'Questions',
  revision: 'Revision',
  test: 'Test practice',
}

export default function Syllabus() {
  const { currentYear, years, settings } = useApp()
  const { push } = useToast()
  const yearId = currentYear?.id ?? null
  const data = useYearData(yearId)
  const [selectedSubject, setSelectedSubject] = useState<string | null>(null)
  const [showArchived, setShowArchived] = useState(false)
  const [subjectForm, setSubjectForm] = useState<{ open: boolean; subject?: Subject }>({ open: false })
  const [chapterForm, setChapterForm] = useState<{ open: boolean; subjectId?: string; chapter?: Chapter }>({ open: false })
  const [topicForm, setTopicForm] = useState<{ open: boolean; chapterId?: string; topic?: Topic }>({ open: false })
  const [importOpen, setImportOpen] = useState(false)
  const [copyOpen, setCopyOpen] = useState(false)
  const [confirm, setConfirm] = useState<null | { title: string; message: string; onConfirm: () => void }>(null)
  const [expanded, setExpanded] = useState<Record<string, boolean>>({})
  const [expandedTopic, setExpandedTopic] = useState<Record<string, boolean>>({})

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

  const subjects = (data?.subjects ?? []).filter((s) => showArchived || !s.archived)
  const subject = subjects.find((s) => s.id === selectedSubject) ?? null
  const chapters = (data?.chapters ?? [])
    .filter((c) => (!subject ? false : c.subjectId === subject.id) && (showArchived || !c.archived))
    .sort((a, b) => a.order - b.order)

  const moveOrder = async (table: 'subjects' | 'chapters' | 'topics', rows: Array<{ id: string; order: number }>, index: number, dir: -1 | 1) => {
    const j = index + dir
    if (j < 0 || j >= rows.length) return
    const a = rows[index]
    const b = rows[j]
    await updateRow(table, a.id, { order: b.order })
    await updateRow(table, b.id, { order: a.order })
  }

  const copySyllabus = async (targetYearId: string) => {
    if (!yearId) return
    const srcSubjects = data?.subjects ?? []
    if (!srcSubjects.length) {
      push('Nothing to copy.', 'error')
      return
    }
    for (const s of srcSubjects) {
      const newSubjectId = await createRow('subjects', {
        yearId: targetYearId,
        name: s.name,
        description: s.description,
        color: s.color,
        icon: s.icon,
        priority: s.priority,
        goal: s.goal,
        targetDate: s.targetDate,
        order: s.order,
      })
      const srcChapters = (data?.chapters ?? []).filter((c) => c.subjectId === s.id)
      for (const c of srcChapters) {
        const newChapterId = await createRow('chapters', {
          subjectId: newSubjectId,
          name: c.name,
          description: c.description,
          priority: c.priority,
          estimatedHours: c.estimatedHours,
          deadline: c.deadline,
          order: c.order,
        })
        const srcTopics = (data?.topics ?? []).filter((t) => t.chapterId === c.id)
        for (const t of srcTopics) {
          const newTopicId = await createRow('topics', {
            chapterId: newChapterId,
            name: t.name,
            description: t.description,
            priority: t.priority,
            estimatedMinutes: t.estimatedMinutes,
            status: 'not_started',
            order: t.order,
          })
          const subs = (data?.subtopics ?? []).filter((x) => x.topicId === t.id)
          if (subs.length) {
            await createRows('subtopics', subs.map((x) => ({ topicId: newTopicId, name: x.name, difficulty: x.difficulty, completed: false, order: x.order })))
          }
          const comps = (data?.components ?? []).filter((x) => x.topicId === t.id)
          if (comps.length) {
            await createRows('components', comps.map((x) => ({ topicId: newTopicId, type: x.type, done: false })))
          }
        }
      }
    }
    push('Syllabus copied. Progress starts fresh in the target year.', 'success')
    setCopyOpen(false)
  }

  return (
    <div>
      <PageHeader
        title="Syllabus"
        subtitle={currentYear ? `${currentYear.name} · ${subjects.length} subjects` : undefined}
        action={
          <>
            <Button variant="secondary" size="sm" onClick={() => setImportOpen(true)}>
              <FileUp size={14} /> Import
            </Button>
            <Button variant="secondary" size="sm" onClick={() => setCopyOpen(true)}>
              <Copy size={14} /> <span className="hidden sm:inline">Copy to year</span>
            </Button>
            <Button size="sm" onClick={() => setSubjectForm({ open: true })}>
              <Plus size={14} /> Subject
            </Button>
          </>
        }
      />

      <div className="mb-3 flex items-center justify-between">
        <p className="text-xs text-muted">Academic Year → Subject → Chapter → Topic → Subtopic</p>
        <label className="flex items-center gap-2 text-[11px] text-muted">
          Archived
          <Toggle checked={showArchived} onChange={setShowArchived} label="Show archived items" />
        </label>
      </div>

      {!subject ? (
        <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
          {subjects.length === 0 ? (
            <div className="sm:col-span-2">
              <EmptyState
                title="No subjects in this year"
                hint="Add a subject, or import a chapter list in bulk."
                action={<Button onClick={() => setSubjectForm({ open: true })}>Add subject</Button>}
                icon={<Layers size={22} />}
              />
            </div>
          ) : (
            subjects.map((s, i) => {
              const pct = progress?.subjectPct.get(s.id) ?? 0
              const chs = (data?.chapters ?? []).filter((c) => c.subjectId === s.id)
              return (
                <Card key={s.id} className="p-3">
                  <button className="flex w-full items-start gap-2 text-left" onClick={() => setSelectedSubject(s.id)}>
                    <span className="mt-1 h-2.5 w-2.5 shrink-0 rounded-full" style={{ backgroundColor: s.color }} />
                    <span className="min-w-0 flex-1">
                      <span className="flex items-center gap-1.5">
                        <span className="truncate text-sm font-medium">{s.name}</span>
                        {s.archived ? <Badge>Archived</Badge> : null}
                      </span>
                      <span className="mt-0.5 block truncate text-[11px] text-muted">
                        {chs.length} chapters · {pct}% complete
                      </span>
                      <ProgressBar value={pct} className="mt-2" tone={s.color} />
                    </span>
                    <ChevronRight size={16} className="mt-1 text-muted" />
                  </button>
                  <div className="mt-2 flex items-center gap-1 border-t border-line pt-2">
                    <Button size="sm" variant="ghost" onClick={() => setSubjectForm({ open: true, subject: s })} aria-label="Edit subject">
                      <Pencil size={13} />
                    </Button>
                    <Button size="sm" variant="ghost" onClick={() => void updateRow('subjects', s.id, { archived: !s.archived })} aria-label="Archive subject">
                      <Archive size={13} />
                    </Button>
                    <Button
                      size="sm"
                      variant="ghost"
                      aria-label="Delete subject"
                      onClick={() =>
                        setConfirm({
                          title: 'Delete subject?',
                          message: `This deletes "${s.name}" and all of its chapters, topics and subtopics. Consider archiving instead.`,
                          onConfirm: async () => {
                            await deleteSubjectCascade(s.id)
                            push('Subject deleted.', 'success')
                          },
                        })
                      }
                    >
                      <Trash2 size={13} />
                    </Button>
                    <span className="ml-auto flex items-center gap-0.5">
                      <Button size="sm" variant="ghost" disabled={i === 0} onClick={() => void moveOrder('subjects', subjects, i, -1)} aria-label="Move up">
                        ↑
                      </Button>
                      <Button size="sm" variant="ghost" disabled={i === subjects.length - 1} onClick={() => void moveOrder('subjects', subjects, i, 1)} aria-label="Move down">
                        ↓
                      </Button>
                    </span>
                  </div>
                </Card>
              )
            })
          )}
        </div>
      ) : (
        <div>
          <div className="mb-3 flex items-center gap-2">
            <Button variant="ghost" size="sm" onClick={() => setSelectedSubject(null)}>
              <ArrowLeft size={14} /> Subjects
            </Button>
          </div>

          <Card className="mb-3 p-3">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <div className="flex items-center gap-2">
                <span className="h-3 w-3 rounded-full" style={{ backgroundColor: subject.color }} />
                <h2 className="text-base font-semibold">{subject.name}</h2>
                <Badge tone="accent">{progress?.subjectPct.get(subject.id) ?? 0}%</Badge>
              </div>
              <div className="flex items-center gap-1">
                <Button size="sm" variant="secondary" onClick={() => setChapterForm({ open: true, subjectId: subject.id })}>
                  <Plus size={13} /> Chapter
                </Button>
                <Button size="sm" variant="ghost" onClick={() => setSubjectForm({ open: true, subject })} aria-label="Edit subject">
                  <Pencil size={13} />
                </Button>
              </div>
            </div>
            {subject.description ? <p className="mt-1.5 text-xs text-muted">{subject.description}</p> : null}
            {subject.goal ? <p className="mt-1 text-xs text-muted">Goal: {subject.goal}</p> : null}
          </Card>

          {chapters.length === 0 ? (
            <EmptyState
              title="No chapters yet"
              hint="Add a chapter, or paste a whole chapter list with Import."
              action={<Button onClick={() => setChapterForm({ open: true, subjectId: subject.id })}>Add chapter</Button>}
            />
          ) : (
            <div className="space-y-2">
              {chapters.map((chapter, ci) => {
                const chapterPct = progress?.chapterPct.get(chapter.id) ?? 0
                const topics = (data?.topics ?? []).filter((t) => t.chapterId === chapter.id && (showArchived || !t.archived)).sort((a, b) => a.order - b.order)
                const isOpen = expanded[chapter.id] ?? ci === 0
                return (
                  <Card key={chapter.id}>
                    <div className="flex items-start gap-2 p-3">
                      <button onClick={() => setExpanded((e) => ({ ...e, [chapter.id]: !isOpen }))} className="mt-0.5 text-muted" aria-label="Toggle chapter">
                        {isOpen ? <ChevronDown size={16} /> : <ChevronRight size={16} />}
                      </button>
                      <div className="min-w-0 flex-1">
                        <div className="flex flex-wrap items-center gap-1.5">
                          <p className="text-sm font-medium">{chapter.name}</p>
                          {chapter.priority === 'high' ? <Badge tone="warn">High</Badge> : null}
                          {chapter.archived ? <Badge>Archived</Badge> : null}
                        </div>
                        <p className="mt-0.5 text-[11px] text-muted">
                          {topics.length} topics · {chapterPct}% {chapter.deadline ? `· due ${chapter.deadline}` : ''}
                        </p>
                        <ProgressBar value={chapterPct} className="mt-2" />
                      </div>
                      <div className="flex items-center gap-0.5">
                        <Button size="sm" variant="ghost" onClick={() => setTopicForm({ open: true, chapterId: chapter.id })} aria-label="Add topic">
                          <Plus size={13} />
                        </Button>
                        <Button size="sm" variant="ghost" onClick={() => setChapterForm({ open: true, subjectId: subject.id, chapter })} aria-label="Edit chapter">
                          <Pencil size={13} />
                        </Button>
                        <Button
                          size="sm"
                          variant="ghost"
                          aria-label="Duplicate chapter"
                          onClick={async () => {
                            const newId = await createRow('chapters', {
                              subjectId: chapter.subjectId,
                              name: `${chapter.name} (copy)`,
                              description: chapter.description,
                              priority: chapter.priority,
                              estimatedHours: chapter.estimatedHours,
                              order: chapter.order + 0.5,
                            })
                            for (const t of topics) {
                              const nt = await createRow('topics', {
                                chapterId: newId,
                                name: t.name,
                                description: t.description,
                                priority: t.priority,
                                status: 'not_started',
                                order: t.order,
                              })
                              const subs = (data?.subtopics ?? []).filter((x) => x.topicId === t.id)
                              if (subs.length) await createRows('subtopics', subs.map((x) => ({ topicId: nt, name: x.name, difficulty: x.difficulty, completed: false, order: x.order })))
                              const comps = (data?.components ?? []).filter((x) => x.topicId === t.id)
                              if (comps.length) await createRows('components', comps.map((x) => ({ topicId: nt, type: x.type, done: false })))
                            }
                            push('Chapter duplicated.', 'success')
                          }}
                        >
                          <Copy size={13} />
                        </Button>
                        <Button
                          size="sm"
                          variant="ghost"
                          aria-label="Delete chapter"
                          onClick={() =>
                            setConfirm({
                              title: 'Delete chapter?',
                              message: `"${chapter.name}" and all of its topics will be removed.`,
                              onConfirm: async () => {
                                await deleteChapterCascade(chapter.id)
                                push('Chapter deleted.', 'success')
                              },
                            })
                          }
                        >
                          <Trash2 size={13} />
                        </Button>
                      </div>
                    </div>

                    {isOpen ? (
                      <div className="border-t border-line px-3 py-2">
                        {topics.length === 0 ? (
                          <p className="py-2 text-xs text-muted">No topics yet.</p>
                        ) : (
                          <ul className="space-y-1">
                            {topics.map((topic, ti) => {
                              const tPct = progress?.topicPct.get(topic.id) ?? 0
                              const subs = (data?.subtopics ?? []).filter((s) => s.topicId === topic.id).sort((a, b) => a.order - b.order)
                              const comps = (data?.components ?? []).filter((c) => c.topicId === topic.id)
                              const topicOpen = expandedTopic[topic.id] ?? false
                              return (
                                <li key={topic.id} className="rounded-lg border border-line/70">
                                  <div className="flex items-center gap-2 px-2 py-1.5">
                                    <GripVertical size={12} className="text-muted/50" />
                                    <button className="min-w-0 flex-1 text-left" onClick={() => setExpandedTopic((e) => ({ ...e, [topic.id]: !topicOpen }))}>
                                      <span className="flex items-center gap-1.5">
                                        <span className={cx('truncate text-xs', tPct === 100 && 'text-muted line-through')}>{topic.name}</span>
                                        {topic.favourite ? <span className="text-amber-400">★</span> : null}
                                        {topic.archived ? <Badge>Archived</Badge> : null}
                                      </span>
                                      <span className="mt-0.5 block text-[10px] text-muted">
                                        {STATUS_LABEL[topic.status]} · {subs.length} subtopics · {tPct}%
                                      </span>
                                    </button>
                                    <select
                                      aria-label={`Status for ${topic.name}`}
                                      value={topic.status}
                                      onChange={(e) => void updateRow('topics', topic.id, { status: e.target.value as TopicStatus })}
                                      className="rounded border border-line bg-raised px-1 py-1 text-[10px]"
                                    >
                                      {Object.entries(STATUS_LABEL).map(([v, l]) => (
                                        <option key={v} value={v}>
                                          {l}
                                        </option>
                                      ))}
                                    </select>
                                    <button onClick={() => setTopicForm({ open: true, chapterId: chapter.id, topic })} aria-label="Edit topic" className="p-1 text-muted">
                                      <Pencil size={12} />
                                    </button>
                                    <button
                                      aria-label="Delete topic"
                                      className="p-1 text-muted"
                                      onClick={() =>
                                        setConfirm({
                                          title: 'Delete topic?',
                                          message: `"${topic.name}" will be removed with its subtopics.`,
                                          onConfirm: async () => {
                                            await deleteTopicCascade(topic.id)
                                            push('Topic deleted.', 'success')
                                          },
                                        })
                                      }
                                    >
                                      <Trash2 size={12} />
                                    </button>
                                    <span className="flex flex-col">
                                      <button onClick={() => void moveOrder('topics', topics, ti, -1)} disabled={ti === 0} aria-label="Move up" className="text-[9px] text-muted disabled:opacity-30">
                                        ▲
                                      </button>
                                      <button onClick={() => void moveOrder('topics', topics, ti, 1)} disabled={ti === topics.length - 1} aria-label="Move down" className="text-[9px] text-muted disabled:opacity-30">
                                        ▼
                                      </button>
                                    </span>
                                  </div>

                                  {topicOpen ? (
                                    <div className="space-y-2 border-t border-line/70 px-2 py-2">
                                      {comps.length > 0 ? (
                                        <div className="flex flex-wrap gap-1">
                                          {comps.map((c) => (
                                            <button
                                              key={c.id}
                                              onClick={() => void updateRow('components', c.id, { done: !c.done })}
                                              className={cx(
                                                'rounded-md border px-1.5 py-1 text-[10px]',
                                                c.done ? 'border-emerald-500/40 bg-emerald-500/15 text-emerald-400' : 'border-line text-muted',
                                              )}
                                            >
                                              {c.done ? '✓ ' : ''}
                                              {COMPONENT_LABEL[c.type] ?? c.type}
                                            </button>
                                          ))}
                                        </div>
                                      ) : null}

                                      {subs.length > 0 ? (
                                        <ul className="space-y-0.5">
                                          {subs.map((s) => (
                                            <li key={s.id} className="flex items-center gap-2">
                                              <button
                                                aria-label={`Toggle ${s.name}`}
                                                onClick={() => void updateRow('subtopics', s.id, { completed: !s.completed })}
                                                className={cx(
                                                  'flex h-4 w-4 shrink-0 items-center justify-center rounded border',
                                                  s.completed ? 'border-emerald-500 bg-emerald-500 text-white' : 'border-line text-transparent',
                                                )}
                                              >
                                                <Check size={10} />
                                              </button>
                                              <span className={cx('flex-1 text-[11px]', s.completed && 'text-muted line-through')}>{s.name}</span>
                                              <button onClick={() => void deleteRow('subtopics', s.id)} aria-label="Delete subtopic" className="p-0.5 text-muted">
                                                <Trash2 size={11} />
                                              </button>
                                            </li>
                                          ))}
                                        </ul>
                                      ) : null}

                                      <InlineAdd
                                        placeholder="Add subtopic…"
                                        onAdd={async (name) => {
                                          await createRow('subtopics', {
                                            topicId: topic.id,
                                            name,
                                            difficulty: 'medium',
                                            completed: false,
                                            order: subs.length,
                                          })
                                        }}
                                      />
                                      {topic.notes ? <p className="rounded bg-raised p-2 text-[11px] text-muted">{topic.notes}</p> : null}
                                    </div>
                                  ) : null}
                                </li>
                              )
                            })}
                          </ul>
                        )}
                        <div className="mt-2">
                          <InlineAdd placeholder="Add topic…" onAdd={async (name) => {
                            await createRow('topics', {
                              chapterId: chapter.id,
                              name,
                              priority: settings.defaultPriority,
                              status: 'not_started',
                              order: topics.length,
                            })
                          }} />
                        </div>
                      </div>
                    ) : null}
                  </Card>
                )
              })}
            </div>
          )}
        </div>
      )}

      <SubjectForm
        open={subjectForm.open}
        subject={subjectForm.subject}
        yearId={yearId}
        onClose={() => setSubjectForm({ open: false })}
      />
      <ChapterForm
        open={chapterForm.open}
        chapter={chapterForm.chapter}
        subjectId={chapterForm.subjectId}
        onClose={() => setChapterForm({ open: false })}
      />
      <TopicForm
        open={topicForm.open}
        topic={topicForm.topic}
        chapterId={topicForm.chapterId}
        onClose={() => setTopicForm({ open: false })}
      />

      <BulkImport
        open={importOpen}
        onClose={() => setImportOpen(false)}
        yearId={yearId}
        subjects={data?.subjects ?? []}
        chapters={data?.chapters ?? []}
        topics={data?.topics ?? []}
      />

      <Modal
        open={copyOpen}
        onClose={() => setCopyOpen(false)}
        title="Copy syllabus to another year"
        footer={
          <>
            <Button variant="ghost" onClick={() => setCopyOpen(false)}>
              Cancel
            </Button>
          </>
        }
      >
        <p className="mb-3 text-xs text-muted">
          Copies the full subject → chapter → topic → subtopic structure into another academic year. Progress, tasks and
          sessions are not copied.
        </p>
        <ul className="space-y-1.5">
          {years.filter((y) => y.id !== yearId).map((y) => (
            <li key={y.id} className="flex items-center justify-between rounded-lg border border-line px-2 py-1.5">
              <span className="text-sm">{y.name}</span>
              <Button
                size="sm"
                variant="secondary"
                onClick={() =>
                  setConfirm({
                    title: 'Copy syllabus?',
                    message: `The structure will be added to "${y.name}". Existing subjects there are kept.`,
                    onConfirm: () => void copySyllabus(y.id),
                  })
                }
              >
                Copy
              </Button>
            </li>
          ))}
          {years.length <= 1 ? <p className="text-xs text-muted">Create another academic year in Settings first.</p> : null}
        </ul>
      </Modal>

      <ConfirmDialog
        open={Boolean(confirm)}
        title={confirm?.title ?? ''}
        message={confirm?.message ?? ''}
        onCancel={() => setConfirm(null)}
        onConfirm={async () => {
          const action = confirm?.onConfirm
          setConfirm(null)
          await action?.()
        }}
      />
    </div>
  )
}

function InlineAdd({ placeholder, onAdd }: { placeholder: string; onAdd: (name: string) => Promise<void> }) {
  const [value, setValue] = useState('')
  const [busy, setBusy] = useState(false)
  return (
    <form
      className="flex items-center gap-1"
      onSubmit={async (e) => {
        e.preventDefault()
        if (!value.trim() || busy) return
        setBusy(true)
        try {
          await onAdd(value.trim())
          setValue('')
        } finally {
          setBusy(false)
        }
      }}
    >
      <input
        value={value}
        onChange={(e) => setValue(e.target.value)}
        placeholder={placeholder}
        className="flex-1 rounded border border-line bg-transparent px-2 py-1 text-[11px] focus:border-accent focus:outline-none"
      />
      <Button size="sm" variant="ghost" type="submit" disabled={!value.trim() || busy} aria-label={placeholder}>
        <Plus size={13} />
      </Button>
    </form>
  )
}

function SubjectForm({ open, subject, yearId, onClose }: { open: boolean; subject?: Subject; yearId: string | null; onClose: () => void }) {
  const { push } = useToast()
  const [name, setName] = useState(subject?.name ?? '')
  const [description, setDescription] = useState(subject?.description ?? '')
  const [color, setColor] = useState(subject?.color ?? COLORS[0])
  const [priority, setPriority] = useState<Priority>(subject?.priority ?? 'medium')
  const [goal, setGoal] = useState(subject?.goal ?? '')
  const [targetDate, setTargetDate] = useState(subject?.targetDate ?? '')

  // Reset when the dialog is reopened for a different subject.
  const key = `${open}-${subject?.id ?? 'new'}`
  const [lastKey, setLastKey] = useState(key)
  if (key !== lastKey) {
    setLastKey(key)
    setName(subject?.name ?? '')
    setDescription(subject?.description ?? '')
    setColor(subject?.color ?? COLORS[0])
    setPriority(subject?.priority ?? 'medium')
    setGoal(subject?.goal ?? '')
    setTargetDate(subject?.targetDate ?? '')
  }

  const submit = async () => {
    if (!name.trim()) {
      push('A subject name is required.', 'error')
      return
    }
    if (subject) {
      await updateRow('subjects', subject.id, { name: name.trim(), description, color, priority, goal, targetDate: targetDate || undefined })
      push('Subject updated.', 'success')
    } else {
      if (!yearId) return
      const count = await db.subjects.where({ yearId }).count()
      await createRow('subjects', {
        yearId,
        name: name.trim(),
        description,
        color,
        priority,
        goal,
        targetDate: targetDate || undefined,
        order: count,
      })
      push('Subject created.', 'success')
    }
    onClose()
  }

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={subject ? 'Edit subject' : 'New subject'}
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            Cancel
          </Button>
          <Button onClick={() => void submit()}>{subject ? 'Save' : 'Create'}</Button>
        </>
      }
    >
      <div className="space-y-3">
        <Field label="Name *">
          <Input value={name} onChange={(e) => setName(e.target.value)} placeholder="Physics" />
        </Field>
        <Field label="Description">
          <Textarea value={description} onChange={(e) => setDescription(e.target.value)} />
        </Field>
        <Field label="Accent colour">
          <div className="flex flex-wrap gap-2">
            {COLORS.map((c) => (
              <button
                key={c}
                onClick={() => setColor(c)}
                aria-label={`Colour ${c}`}
                className={cx('h-7 w-7 rounded-full border-2', color === c ? 'border-ink' : 'border-transparent')}
                style={{ backgroundColor: c }}
              />
            ))}
          </div>
        </Field>
        <Field label="Priority">
          <Select value={priority} onChange={(e) => setPriority(e.target.value as Priority)}>
            <option value="high">High</option>
            <option value="medium">Medium</option>
            <option value="low">Low</option>
          </Select>
        </Field>
        <Field label="Goal">
          <Input value={goal} onChange={(e) => setGoal(e.target.value)} placeholder="e.g. 90% in half-yearly" />
        </Field>
        <Field label="Target completion date">
          <Input type="date" value={targetDate} onChange={(e) => setTargetDate(e.target.value)} />
        </Field>
      </div>
    </Modal>
  )
}

function ChapterForm({ open, chapter, subjectId, onClose }: { open: boolean; chapter?: Chapter; subjectId?: string; onClose: () => void }) {
  const { push } = useToast()
  const [name, setName] = useState(chapter?.name ?? '')
  const [description, setDescription] = useState(chapter?.description ?? '')
  const [priority, setPriority] = useState<Priority>(chapter?.priority ?? 'medium')
  const [hours, setHours] = useState(String(chapter?.estimatedHours ?? ''))
  const [deadline, setDeadline] = useState(chapter?.deadline ?? '')
  const [notes, setNotes] = useState(chapter?.notes ?? '')

  const key = `${open}-${chapter?.id ?? 'new'}`
  const [lastKey, setLastKey] = useState(key)
  if (key !== lastKey) {
    setLastKey(key)
    setName(chapter?.name ?? '')
    setDescription(chapter?.description ?? '')
    setPriority(chapter?.priority ?? 'medium')
    setHours(String(chapter?.estimatedHours ?? ''))
    setDeadline(chapter?.deadline ?? '')
    setNotes(chapter?.notes ?? '')
  }

  const submit = async () => {
    if (!name.trim()) {
      push('A chapter name is required.', 'error')
      return
    }
    const payload = {
      name: name.trim(),
      description,
      priority,
      estimatedHours: hours ? Number(hours) : undefined,
      deadline: deadline || undefined,
      notes,
    }
    if (chapter) {
      await updateRow('chapters', chapter.id, payload)
      push('Chapter updated.', 'success')
    } else {
      if (!subjectId) return
      const count = await db.chapters.where({ subjectId }).count()
      await createRow('chapters', { ...payload, subjectId, order: count })
      push('Chapter created.', 'success')
    }
    onClose()
  }

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={chapter ? 'Edit chapter' : 'New chapter'}
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            Cancel
          </Button>
          <Button onClick={() => void submit()}>{chapter ? 'Save' : 'Create'}</Button>
        </>
      }
    >
      <div className="space-y-3">
        <Field label="Name *">
          <Input value={name} onChange={(e) => setName(e.target.value)} />
        </Field>
        <Field label="Description">
          <Textarea value={description} onChange={(e) => setDescription(e.target.value)} />
        </Field>
        <div className="grid grid-cols-2 gap-3">
          <Field label="Priority">
            <Select value={priority} onChange={(e) => setPriority(e.target.value as Priority)}>
              <option value="high">High</option>
              <option value="medium">Medium</option>
              <option value="low">Low</option>
            </Select>
          </Field>
          <Field label="Estimated hours">
            <Input type="number" min={0} step={0.5} value={hours} onChange={(e) => setHours(e.target.value)} />
          </Field>
        </div>
        <Field label="Deadline">
          <Input type="date" value={deadline} onChange={(e) => setDeadline(e.target.value)} min={todayStr()} />
        </Field>
        <Field label="Notes">
          <Textarea value={notes} onChange={(e) => setNotes(e.target.value)} />
        </Field>
      </div>
    </Modal>
  )
}

function TopicForm({ open, topic, chapterId, onClose }: { open: boolean; topic?: Topic; chapterId?: string; onClose: () => void }) {
  const { push } = useToast()
  const [name, setName] = useState(topic?.name ?? '')
  const [description, setDescription] = useState(topic?.description ?? '')
  const [notes, setNotes] = useState(topic?.notes ?? '')
  const [priority, setPriority] = useState<Priority>(topic?.priority ?? 'medium')
  const [status, setStatus] = useState<TopicStatus>(topic?.status ?? 'not_started')
  const [minutes, setMinutes] = useState(String(topic?.estimatedMinutes ?? ''))
  const [targetDate, setTargetDate] = useState(topic?.targetDate ?? '')

  const key = `${open}-${topic?.id ?? 'new'}`
  const [lastKey, setLastKey] = useState(key)
  if (key !== lastKey) {
    setLastKey(key)
    setName(topic?.name ?? '')
    setDescription(topic?.description ?? '')
    setNotes(topic?.notes ?? '')
    setPriority(topic?.priority ?? 'medium')
    setStatus(topic?.status ?? 'not_started')
    setMinutes(String(topic?.estimatedMinutes ?? ''))
    setTargetDate(topic?.targetDate ?? '')
  }

  const submit = async () => {
    if (!name.trim()) {
      push('A topic name is required.', 'error')
      return
    }
    const payload = {
      name: name.trim(),
      description,
      notes,
      priority,
      status,
      estimatedMinutes: minutes ? Number(minutes) : undefined,
      targetDate: targetDate || undefined,
    }
    if (topic) {
      await updateRow('topics', topic.id, payload)
      push('Topic updated.', 'success')
    } else {
      if (!chapterId) return
      const count = await db.topics.where({ chapterId }).count()
      await createRow('topics', { ...payload, chapterId, order: count })
      push('Topic created.', 'success')
    }
    onClose()
  }

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={topic ? 'Edit topic' : 'New topic'}
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            Cancel
          </Button>
          <Button onClick={() => void submit()}>{topic ? 'Save' : 'Create'}</Button>
        </>
      }
    >
      <div className="space-y-3">
        <Field label="Name *">
          <Input value={name} onChange={(e) => setName(e.target.value)} />
        </Field>
        <Field label="Description">
          <Textarea value={description} onChange={(e) => setDescription(e.target.value)} />
        </Field>
        <div className="grid grid-cols-2 gap-3">
          <Field label="Status">
            <Select value={status} onChange={(e) => setStatus(e.target.value as TopicStatus)}>
              {Object.entries(STATUS_LABEL).map(([v, l]) => (
                <option key={v} value={v}>
                  {l}
                </option>
              ))}
            </Select>
          </Field>
          <Field label="Priority">
            <Select value={priority} onChange={(e) => setPriority(e.target.value as Priority)}>
              <option value="high">High</option>
              <option value="medium">Medium</option>
              <option value="low">Low</option>
            </Select>
          </Field>
        </div>
        <div className="grid grid-cols-2 gap-3">
          <Field label="Estimated minutes">
            <Input type="number" min={0} step={5} value={minutes} onChange={(e) => setMinutes(e.target.value)} />
          </Field>
          <Field label="Target date">
            <Input type="date" value={targetDate} onChange={(e) => setTargetDate(e.target.value)} />
          </Field>
        </div>
        <Field label="Personal notes">
          <Textarea value={notes} onChange={(e) => setNotes(e.target.value)} />
        </Field>
      </div>
    </Modal>
  )
}

function BulkImport({
  open,
  onClose,
  yearId,
  subjects,
  chapters,
  topics,
}: {
  open: boolean
  onClose: () => void
  yearId: string | null
  subjects: Subject[]
  chapters: Chapter[]
  topics: Topic[]
}) {
  const { push } = useToast()
  const [text, setText] = useState('')
  const [parsed, setParsed] = useState<ParsedChapter[] | null>(null)
  const [warnings, setWarnings] = useState<string[]>([])
  const [subjectId, setSubjectId] = useState('')
  const [mode, setMode] = useState<'new' | 'existing'>('new')
  const [targetChapterId, setTargetChapterId] = useState('')
  const [skipDuplicates, setSkipDuplicates] = useState(true)
  const [busy, setBusy] = useState(false)

  const destinationChapters = chapters.filter((c) => !subjectId || c.subjectId === subjectId)

  const doParse = () => {
    const result = parseAuto(text)
    const existing = mode === 'existing' ? topics.filter((t) => t.chapterId === targetChapterId).map((t) => t.name) : topics.filter((t) => destinationChapters.some((c) => c.id === t.chapterId)).map((t) => t.name)
    setParsed(markDuplicates(result.chapters, existing))
    setWarnings(result.warnings)
  }

  const commit = async () => {
    if (!yearId || !parsed || parsed.length === 0) return
    if (!subjectId) {
      push('Choose a destination subject.', 'error')
      return
    }
    if (mode === 'existing' && !targetChapterId) {
      push('Choose a destination chapter.', 'error')
      return
    }
    setBusy(true)
    try {
      let imported = 0
      if (mode === 'existing') {
        for (const chapter of parsed) {
          const existingCount = topics.filter((t) => t.chapterId === targetChapterId).length
          let order = existingCount
          for (const topic of chapter.topics) {
            const dup = (topic as ParsedTopicWithDup).duplicate
            if (dup && skipDuplicates) continue
            const id = await createRow('topics', {
              chapterId: targetChapterId,
              name: topic.name,
              priority: 'medium',
              status: 'not_started',
              order: order++,
            })
            if (topic.subtopics.length) {
              await createRows('subtopics', topic.subtopics.map((name, i) => ({ topicId: id, name, difficulty: 'medium' as const, completed: false, order: i })))
            }
            imported++
          }
        }
      } else {
        const base = chapters.filter((c) => c.subjectId === subjectId).length
        let chapterOrder = base
        for (const chapter of parsed) {
          const chapterId = await createRow('chapters', {
            subjectId,
            name: chapter.name,
            priority: 'medium',
            order: chapterOrder++,
          })
          let order = 0
          for (const topic of chapter.topics) {
            const dup = (topic as ParsedTopicWithDup).duplicate
            if (dup && skipDuplicates) continue
            const id = await createRow('topics', {
              chapterId,
              name: topic.name,
              priority: 'medium',
              status: 'not_started',
              order: order++,
            })
            if (topic.subtopics.length) {
              await createRows('subtopics', topic.subtopics.map((name, i) => ({ topicId: id, name, difficulty: 'medium' as const, completed: false, order: i })))
            }
            imported++
          }
        }
      }
      await createRow('importHistory', {
        entity: 'syllabus',
        count: imported,
        summary: `${mode === 'new' ? 'New chapters' : 'Added to chapter'} · ${parsed.length} chapter(s) parsed`,
        at: new Date().toISOString(),
      })
      push(`${imported} topics imported.`, 'success')
      setText('')
      setParsed(null)
      onClose()
    } catch (err) {
      push(err instanceof Error ? err.message : 'Import failed.', 'error')
    } finally {
      setBusy(false)
    }
  }

  const totalTopics = parsed?.reduce((n, c) => n + c.topics.length, 0) ?? 0
  const totalSubs = parsed?.reduce((n, c) => n + c.topics.reduce((m, t) => m + t.subtopics.length, 0), 0) ?? 0

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Bulk import"
      wide
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            Cancel
          </Button>
          {parsed ? (
            <Button variant="secondary" onClick={() => setParsed(null)}>
              Edit input
            </Button>
          ) : (
            <Button variant="secondary" onClick={doParse} disabled={!text.trim()}>
              Preview
            </Button>
          )}
          <Button onClick={() => void commit()} disabled={!parsed || busy || totalTopics === 0}>
            {busy ? 'Importing…' : `Import ${totalTopics} topics`}
          </Button>
        </>
      }
    >
      {!parsed ? (
        <div className="space-y-3">
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <Field label="Destination subject *">
              <Select value={subjectId} onChange={(e) => { setSubjectId(e.target.value); setTargetChapterId('') }}>
                <option value="">Choose…</option>
                {subjects.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.name}
                  </option>
                ))}
              </Select>
            </Field>
            <Field label="Create">
              <Select value={mode} onChange={(e) => setMode(e.target.value as 'new' | 'existing')}>
                <option value="new">New chapters from headings</option>
                <option value="existing">Add topics to one chapter</option>
              </Select>
            </Field>
          </div>
          {mode === 'existing' ? (
            <Field label="Destination chapter *">
              <Select value={targetChapterId} onChange={(e) => setTargetChapterId(e.target.value)}>
                <option value="">Choose…</option>
                {destinationChapters.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </Select>
            </Field>
          ) : null}
          <Field label="Paste text, CSV or JSON" hint="Headings like 'Chapter 1: Units and Measurements' start a chapter. Lines starting with - are topics; indented ones become subtopics.">
            <Textarea value={text} onChange={(e) => setText(e.target.value)} className="min-h-[180px] font-mono text-xs" placeholder={'Chapter 1: Units and Measurements\n- Physical quantities\n  - Fundamental units\n  - Derived units\n- Dimensional analysis'} />
          </Field>
        </div>
      ) : (
        <div className="space-y-3">
          <div className="flex flex-wrap items-center gap-2 text-xs">
            <Badge tone="accent">{parsed.length} chapters</Badge>
            <Badge tone="accent">{totalTopics} topics</Badge>
            <Badge tone="accent">{totalSubs} subtopics</Badge>
            <label className="ml-auto flex items-center gap-2 text-muted">
              Skip duplicates
              <Toggle checked={skipDuplicates} onChange={setSkipDuplicates} label="Skip duplicate topics" />
            </label>
          </div>
          {warnings.length > 0 ? (
            <ul className="rounded-lg border border-amber-500/30 bg-amber-500/10 p-2 text-[11px] text-amber-300">
              {warnings.map((w) => (
                <li key={w}>• {w}</li>
              ))}
            </ul>
          ) : null}
          <div className="max-h-72 space-y-2 overflow-y-auto rounded-lg border border-line p-2">
            {parsed.map((c, ci) => (
              <div key={`${c.name}-${ci}`}>
                <p className="text-xs font-semibold">{c.name}</p>
                <ul className="ml-3 mt-1 space-y-0.5">
                  {c.topics.map((t, ti) => {
                    const dup = (t as ParsedTopicWithDup).duplicate
                    return (
                      <li key={`${t.name}-${ti}`} className="text-[11px] text-muted">
                        {dup ? <Badge tone="warn">duplicate</Badge> : null} {t.name}
                        {t.subtopics.length ? <span className="text-muted/70"> ({t.subtopics.length} sub)</span> : null}
                      </li>
                    )
                  })}
                </ul>
              </div>
            ))}
          </div>
        </div>
      )}
    </Modal>
  )
}

interface ParsedTopicWithDup {
  name: string
  subtopics: string[]
  duplicate?: boolean
}

