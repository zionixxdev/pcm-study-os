import { useEffect, useState } from 'react'
import { Button, Field, Input, Modal, Select, Textarea, Tabs, useToast } from './ui'
import { SyllabusSelect, type SyllabusSelection } from './SyllabusPicker'
import { createRow } from '../lib/db'
import { todayStr } from '../lib/dates'
import { useApp } from '../store/AppContext'
import { nextRevisionDate } from '../lib/revision'
import type { MistakeCategory, Priority } from '../types'

type Mode = 'task' | 'topic' | 'note' | 'revision' | 'mistake'

export function QuickCapture({ open, onClose }: { open: boolean; onClose: () => void }) {
  const { currentYear, settings } = useApp()
  const { push } = useToast()
  const [mode, setMode] = useState<Mode>('task')
  const [title, setTitle] = useState('')
  const [body, setBody] = useState('')
  const [sel, setSel] = useState<SyllabusSelection>({})
  const [priority, setPriority] = useState<Priority>(settings.defaultPriority)
  const [due, setDue] = useState(todayStr())
  const [minutes, setMinutes] = useState('30')
  const [category, setCategory] = useState<MistakeCategory>('conceptual')
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    if (open) {
      setTitle('')
      setBody('')
      setSel({})
      setDue(todayStr())
      setPriority(settings.defaultPriority)
    }
  }, [open, settings.defaultPriority])

  const yearId = currentYear?.id
  const canSave = Boolean(yearId && title.trim())

  const save = async () => {
    if (!yearId) {
      push('Create an academic year first.', 'error')
      return
    }
    if (!title.trim()) {
      push('A title is required.', 'error')
      return
    }
    setBusy(true)
    try {
      if (mode === 'task') {
        await createRow('tasks', {
          yearId,
          title: title.trim(),
          description: body.trim() || undefined,
          subjectId: sel.subjectId,
          chapterId: sel.chapterId,
          topicId: sel.topicId,
          priority,
          status: 'pending',
          dueDate: due || undefined,
          estimatedMinutes: Number(minutes) || 30,
          recurring: 'none',
          order: Date.now(),
        })
      } else if (mode === 'topic') {
        if (!sel.chapterId) throw new Error('Choose a chapter for the new topic.')
        await createRow('topics', {
          chapterId: sel.chapterId,
          name: title.trim(),
          notes: body.trim() || undefined,
          priority,
          status: 'not_started',
          order: Date.now(),
        })
      } else if (mode === 'note') {
        await createRow('notes', {
          yearId,
          title: title.trim(),
          body,
          subjectId: sel.subjectId,
          chapterId: sel.chapterId,
          topicId: sel.topicId,
          category: 'quick',
        })
      } else if (mode === 'revision') {
        await createRow('revisions', {
          yearId,
          topicId: sel.topicId,
          label: title.trim(),
          dueDate: due || todayStr(),
          intervalIndex: 0,
          status: 'upcoming',
          notes: body.trim() || undefined,
        })
      } else {
        if (!sel.subjectId) throw new Error('Choose a subject for the mistake.')
        await createRow('mistakes', {
          yearId,
          subjectId: sel.subjectId,
          chapterId: sel.chapterId,
          topicId: sel.topicId,
          question: title.trim(),
          explanation: body.trim() || undefined,
          category,
          date: todayStr(),
          retryDate: nextRevisionDate(todayStr(), settings.revisionIntervals, 0),
          resolved: false,
        })
      }
      push('Saved.', 'success')
      onClose()
    } catch (err) {
      push(err instanceof Error ? err.message : 'Could not save.', 'error')
    } finally {
      setBusy(false)
    }
  }

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Quick capture"
      wide
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            Cancel
          </Button>
          <Button onClick={() => void save()} disabled={!canSave || busy}>
            {busy ? 'Saving…' : 'Save'}
          </Button>
        </>
      }
    >
      <div className="space-y-4">
        <Tabs
          value={mode}
          onChange={setMode}
          options={[
            { value: 'task', label: 'Task' },
            { value: 'topic', label: 'Topic' },
            { value: 'note', label: 'Note' },
            { value: 'revision', label: 'Revision' },
            { value: 'mistake', label: 'Mistake' },
          ]}
        />

        <Field label={mode === 'note' ? 'Title' : mode === 'mistake' ? 'Question / problem' : mode === 'task' ? 'Task' : 'Name'}>
          <Input value={title} onChange={(e) => setTitle(e.target.value)} placeholder="Type here…" autoFocus />
        </Field>

        <SyllabusSelect value={sel} onChange={setSel} showTopic={mode !== 'topic'} required={mode === 'mistake'} />

        {mode === 'task' || mode === 'revision' ? (
          <div className="grid grid-cols-2 gap-3">
            <Field label={mode === 'revision' ? 'Revision date' : 'Due date'}>
              <Input type="date" value={due} onChange={(e) => setDue(e.target.value)} />
            </Field>
            {mode === 'task' ? (
              <Field label="Estimate (minutes)">
                <Input type="number" min={5} step={5} value={minutes} onChange={(e) => setMinutes(e.target.value)} />
              </Field>
            ) : null}
          </div>
        ) : null}

        {mode === 'task' || mode === 'topic' ? (
          <Field label="Priority">
            <Select value={priority} onChange={(e) => setPriority(e.target.value as Priority)}>
              <option value="high">High</option>
              <option value="medium">Medium</option>
              <option value="low">Low</option>
            </Select>
          </Field>
        ) : null}

        {mode === 'mistake' ? (
          <Field label="Mistake type">
            <Select value={category} onChange={(e) => setCategory(e.target.value as MistakeCategory)}>
              <option value="conceptual">Conceptual error</option>
              <option value="formula">Formula error</option>
              <option value="calculation">Calculation error</option>
              <option value="sign">Sign error</option>
              <option value="unit">Unit error</option>
              <option value="silly">Silly mistake</option>
              <option value="incomplete">Incomplete answer</option>
              <option value="time">Time management</option>
              <option value="other">Other</option>
            </Select>
          </Field>
        ) : null}

        <Field label={mode === 'note' ? 'Note' : 'Details (optional)'}>
          <Textarea value={body} onChange={(e) => setBody(e.target.value)} placeholder="Optional details…" />
        </Field>
      </div>
    </Modal>
  )
}
