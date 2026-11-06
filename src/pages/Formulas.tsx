import { useMemo, useState } from 'react'
import { useLiveQuery } from 'dexie-react-hooks'
import { Plus, Search, Star, Trash2, Pencil } from 'lucide-react'
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
  Textarea,
  cx,
  useToast,
} from '../components/ui'
import { useApp } from '../store/AppContext'
import { useYearData } from '../hooks/useYearData'
import { createRow, db, deleteRow, updateRow } from '../lib/db'
import { MathText } from '../components/MathText'
import { SyllabusSelect, type SyllabusSelection } from '../components/SyllabusPicker'
import type { FormulaEntry } from '../types'

const CATEGORIES: Array<FormulaEntry['category']> = ['formula', 'derivation', 'law', 'definition', 'unit', 'equation', 'reaction', 'identity', 'theorem', 'property', 'concept']

export default function Formulas() {
  const { currentYear } = useApp()
  const { push } = useToast()
  const yearId = currentYear?.id ?? null
  const data = useYearData(yearId)
  const entries = useLiveQuery(async () => (yearId ? db.formulas.where({ yearId }).toArray() : []), [yearId])
  const [query, setQuery] = useState('')
  const [subjectId, setSubjectId] = useState('')
  const [category, setCategory] = useState('')
  const [onlyFav, setOnlyFav] = useState(false)
  const [form, setForm] = useState<{ open: boolean; entry?: FormulaEntry }>({ open: false })
  const [confirmId, setConfirmId] = useState<string | null>(null)

  const subjectName = (id?: string) => data?.subjects.find((s) => s.id === id)?.name ?? ''
  const chapterName = (id?: string) => data?.chapters.find((c) => c.id === id)?.name ?? ''

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase()
    return (entries ?? [])
      .filter((e) => (!subjectId || e.subjectId === subjectId) && (!category || e.category === category) && (!onlyFav || e.favourite))
      .filter((e) => !q || [e.title, e.expression, e.explanation, e.symbols, e.notes, chapterName(e.chapterId)].join(' ').toLowerCase().includes(q))
      .sort((a, b) => a.title.localeCompare(b.title))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [entries, query, subjectId, category, onlyFav, data])

  return (
    <div>
      <PageHeader
        title="Formula library"
        subtitle={`${entries?.length ?? 0} entries`}
        action={
          <Button size="sm" onClick={() => setForm({ open: true })}>
            <Plus size={14} /> Entry
          </Button>
        }
      />

      <Card className="mb-3 p-2.5">
        <div className="flex flex-col gap-2 sm:flex-row">
          <div className="relative flex-1">
            <Search size={14} className="absolute left-2.5 top-1/2 -translate-y-1/2 text-muted" />
            <Input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Search formulas, laws, derivations…" className="pl-8" />
          </div>
          <Select value={subjectId} onChange={(e) => setSubjectId(e.target.value)} className="sm:w-40">
            <option value="">All subjects</option>
            {(data?.subjects ?? []).map((s) => (
              <option key={s.id} value={s.id}>
                {s.name}
              </option>
            ))}
          </Select>
          <Select value={category} onChange={(e) => setCategory(e.target.value)} className="sm:w-36">
            <option value="">All types</option>
            {CATEGORIES.map((c) => (
              <option key={c} value={c}>
                {c}
              </option>
            ))}
          </Select>
          <Button variant={onlyFav ? 'primary' : 'secondary'} onClick={() => setOnlyFav((f) => !f)} aria-label="Favourites only">
            <Star size={14} />
          </Button>
        </div>
      </Card>

      {filtered.length === 0 ? (
        <EmptyState
          title="No entries yet"
          hint="Store a formula with its symbols and units once — then search it during revision."
          action={<Button onClick={() => setForm({ open: true })}>Add formula</Button>}
        />
      ) : (
        <div className="grid gap-2 sm:grid-cols-2">
          {filtered.map((e) => (
            <Card key={e.id} className="p-3">
              <div className="flex items-start justify-between gap-2">
                <div className="min-w-0">
                  <p className="truncate text-sm font-medium">{e.title}</p>
                  <p className="text-[10px] text-muted">
                    {subjectName(e.subjectId)} {e.chapterId ? `· ${chapterName(e.chapterId)}` : ''}
                  </p>
                </div>
                <div className="flex items-center gap-0.5">
                  <button onClick={() => void updateRow('formulas', e.id, { favourite: !e.favourite })} aria-label="Toggle favourite" className={cx('p-1', e.favourite ? 'text-amber-400' : 'text-muted')}>
                    <Star size={13} />
                  </button>
                  <button onClick={() => setForm({ open: true, entry: e })} aria-label="Edit" className="p-1 text-muted">
                    <Pencil size={12} />
                  </button>
                  <button onClick={() => setConfirmId(e.id)} aria-label="Delete" className="p-1 text-muted hover:text-red-400">
                    <Trash2 size={12} />
                  </button>
                </div>
              </div>
              <div className="mt-2 overflow-x-auto rounded-lg border border-line bg-raised px-2 py-2">
                <MathText text={e.expression} className="text-sm" />
              </div>
              {e.symbols ? <p className="mt-1.5 text-[11px] text-muted">Where: {e.symbols}</p> : null}
              {e.units ? <p className="text-[11px] text-muted">Units: {e.units}</p> : null}
              {e.explanation ? <p className="mt-1 line-clamp-2 text-[11px] text-muted">{e.explanation}</p> : null}
              <div className="mt-1.5 flex items-center gap-1">
                <Badge tone="accent">{e.category}</Badge>
                {e.notes ? <span className="truncate text-[10px] text-muted">{e.notes}</span> : null}
              </div>
            </Card>
          ))}
        </div>
      )}

      <FormulaForm
        open={form.open}
        entry={form.entry}
        yearId={yearId}
        subjects={data?.subjects ?? []}
        onClose={() => setForm({ open: false })}
      />

      <ConfirmDialog
        open={Boolean(confirmId)}
        title="Delete entry?"
        message="This formula will be removed from your library."
        onCancel={() => setConfirmId(null)}
        onConfirm={async () => {
          if (confirmId) await deleteRow('formulas', confirmId)
          setConfirmId(null)
          push('Deleted.', 'success')
        }}
      />
    </div>
  )
}

function FormulaForm({
  open,
  entry,
  yearId,
  subjects,
  onClose,
}: {
  open: boolean
  entry?: FormulaEntry
  yearId: string | null
  subjects: Array<{ id: string; name: string }>
  onClose: () => void
}) {
  const { push } = useToast()
  const [title, setTitle] = useState(entry?.title ?? '')
  const [category, setCategory] = useState<FormulaEntry['category']>(entry?.category ?? 'formula')
  const [expression, setExpression] = useState(entry?.expression ?? '')
  const [explanation, setExplanation] = useState(entry?.explanation ?? '')
  const [symbols, setSymbols] = useState(entry?.symbols ?? '')
  const [units, setUnits] = useState(entry?.units ?? '')
  const [notes, setNotes] = useState(entry?.notes ?? '')
  const [sel, setSel] = useState<SyllabusSelection>({ subjectId: entry?.subjectId, chapterId: entry?.chapterId, topicId: entry?.topicId })

  const key = `${open}-${entry?.id ?? 'new'}`
  const [lastKey, setLastKey] = useState(key)
  if (key !== lastKey) {
    setLastKey(key)
    setTitle(entry?.title ?? '')
    setCategory(entry?.category ?? 'formula')
    setExpression(entry?.expression ?? '')
    setExplanation(entry?.explanation ?? '')
    setSymbols(entry?.symbols ?? '')
    setUnits(entry?.units ?? '')
    setNotes(entry?.notes ?? '')
    setSel({ subjectId: entry?.subjectId, chapterId: entry?.chapterId, topicId: entry?.topicId })
  }

  const submit = async () => {
    if (!title.trim() || !expression.trim()) {
      push('Title and expression are required.', 'error')
      return
    }
    if (!sel.subjectId) {
      push('Choose a subject.', 'error')
      return
    }
    const payload = {
      title: title.trim(),
      category,
      expression,
      explanation,
      symbols,
      units,
      notes,
      subjectId: sel.subjectId,
      chapterId: sel.chapterId,
      topicId: sel.topicId,
    }
    if (entry) {
      await updateRow('formulas', entry.id, payload)
      push('Entry updated.', 'success')
    } else {
      if (!yearId) return
      await createRow('formulas', { ...payload, yearId })
      push('Entry added.', 'success')
    }
    onClose()
  }

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={entry ? 'Edit entry' : 'New formula entry'}
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
        <div className="grid grid-cols-2 gap-3">
          <Field label="Title *">
            <Input value={title} onChange={(e) => setTitle(e.target.value)} />
          </Field>
          <Field label="Type">
            <Select value={category} onChange={(e) => setCategory(e.target.value as FormulaEntry['category'])}>
              {CATEGORIES.map((c) => (
                <option key={c} value={c}>
                  {c}
                </option>
              ))}
            </Select>
          </Field>
        </div>
        <Field label="Expression *" hint="Use $…$ for inline maths, e.g. $v = u + at$">
          <Textarea value={expression} onChange={(e) => setExpression(e.target.value)} className="font-mono" placeholder="v = u + at" />
        </Field>
        {expression ? (
          <div className="overflow-x-auto rounded-lg border border-line bg-raised px-3 py-2">
            <MathText text={expression} />
          </div>
        ) : null}
        <SyllabusSelect value={sel} onChange={setSel} required />
        <Field label="Symbols">
          <Input value={symbols} onChange={(e) => setSymbols(e.target.value)} placeholder="v = final velocity, u = initial velocity…" />
        </Field>
        <div className="grid grid-cols-2 gap-3">
          <Field label="SI units">
            <Input value={units} onChange={(e) => setUnits(e.target.value)} placeholder="m/s" />
          </Field>
          <Field label="Notes">
            <Input value={notes} onChange={(e) => setNotes(e.target.value)} />
          </Field>
        </div>
        <Field label="Explanation">
          <Textarea value={explanation} onChange={(e) => setExplanation(e.target.value)} />
        </Field>
        {subjects.length === 0 ? <p className="text-[11px] text-amber-400">Add a subject first.</p> : null}
      </div>
    </Modal>
  )
}

