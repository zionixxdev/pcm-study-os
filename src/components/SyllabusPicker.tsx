import { useMemo } from 'react'
import { Select, Label } from './ui'
import { useYearData } from '../hooks/useYearData'
import { useApp } from '../store/AppContext'

export interface SyllabusSelection {
  subjectId?: string
  chapterId?: string
  topicId?: string
}

export function SyllabusSelect({
  value,
  onChange,
  showTopic = true,
  required,
}: {
  value: SyllabusSelection
  onChange: (next: SyllabusSelection) => void
  showTopic?: boolean
  required?: boolean
}) {
  const { currentYear } = useApp()
  const data = useYearData(currentYear?.id ?? null)
  const subjects = data?.subjects ?? []
  const chapters = useMemo(
    () => (data?.chapters ?? []).filter((c) => !value.subjectId || c.subjectId === value.subjectId),
    [data, value.subjectId],
  )
  const topics = useMemo(
    () => (data?.topics ?? []).filter((t) => !value.chapterId || t.chapterId === value.chapterId),
    [data, value.chapterId],
  )

  return (
    <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
      <div>
        <Label>Subject{required ? ' *' : ''}</Label>
        <Select
          value={value.subjectId ?? ''}
          required={required}
          onChange={(e) => onChange({ subjectId: e.target.value || undefined, chapterId: undefined, topicId: undefined })}
        >
          <option value="">—</option>
          {subjects.map((s) => (
            <option key={s.id} value={s.id}>
              {s.name}
            </option>
          ))}
        </Select>
      </div>
      <div>
        <Label>Chapter</Label>
        <Select
          value={value.chapterId ?? ''}
          onChange={(e) => onChange({ ...value, chapterId: e.target.value || undefined, topicId: undefined })}
        >
          <option value="">—</option>
          {chapters.map((c) => (
            <option key={c.id} value={c.id}>
              {c.name}
            </option>
          ))}
        </Select>
      </div>
      {showTopic ? (
        <div>
          <Label>Topic</Label>
          <Select
            value={value.topicId ?? ''}
            onChange={(e) => onChange({ ...value, topicId: e.target.value || undefined })}
          >
            <option value="">—</option>
            {topics.map((t) => (
              <option key={t.id} value={t.id}>
                {t.name}
              </option>
            ))}
          </Select>
        </div>
      ) : null}
    </div>
  )
}
