export type ID = string
export type Priority = 'low' | 'medium' | 'high'
export type TopicStatus = 'not_started' | 'in_progress' | 'completed' | 'needs_revision'
export type TaskStatus = 'pending' | 'in_progress' | 'completed'
export type ComponentType =
  | 'theory'
  | 'formulas'
  | 'derivations'
  | 'ncert'
  | 'numericals'
  | 'questions'
  | 'revision'
  | 'test'

export interface Base {
  id: ID
  userId: string
  createdAt: string
  updatedAt: string
  archived?: boolean
}

export interface AcademicYear extends Base {
  name: string
  startDate: string
  endDate: string
  isCurrent: boolean
  notes?: string
}

export interface Subject extends Base {
  yearId: ID
  name: string
  description?: string
  icon?: string
  color: string
  priority: Priority
  goal?: string
  targetDate?: string
  order: number
}

export interface Chapter extends Base {
  subjectId: ID
  name: string
  description?: string
  priority: Priority
  estimatedHours?: number
  deadline?: string
  notes?: string
  order: number
}

export interface Topic extends Base {
  chapterId: ID
  name: string
  description?: string
  notes?: string
  priority: Priority
  estimatedMinutes?: number
  targetDate?: string
  status: TopicStatus
  favourite?: boolean
  order: number
}

export interface Subtopic extends Base {
  topicId: ID
  name: string
  notes?: string
  difficulty: 'easy' | 'medium' | 'hard'
  completed: boolean
  order: number
}

export interface StudyComponent extends Base {
  topicId: ID
  type: ComponentType
  done: boolean
}

export interface Task extends Base {
  yearId: ID
  title: string
  description?: string
  subjectId?: ID
  chapterId?: ID
  topicId?: ID
  priority: Priority
  status: TaskStatus
  startDate?: string
  dueDate?: string
  estimatedMinutes: number
  recurring: 'none' | 'daily' | 'weekly'
  order: number
  completedAt?: string
}

export interface StudySession extends Base {
  yearId: ID
  subjectId?: ID
  chapterId?: ID
  topicId?: ID
  startedAt: string
  endedAt: string
  minutes: number
  mode: 'pomodoro' | 'focus' | 'stopwatch'
  notes?: string
}

export type RevisionStatus = 'due' | 'upcoming' | 'overdue' | 'completed'

export interface RevisionItem extends Base {
  yearId: ID
  topicId?: ID
  subtopicId?: ID
  label: string
  dueDate: string
  intervalIndex: number
  status: RevisionStatus
  lastRating?: RevisionRating
  notes?: string
  completedAt?: string
}

export type RevisionRating = 'again' | 'hard' | 'good' | 'easy'

export interface RevisionHistoryEntry extends Base {
  revisionId: ID
  rating: RevisionRating
  notes?: string
  at: string
}

export interface FormulaEntry extends Base {
  yearId: ID
  subjectId: ID
  chapterId?: ID
  topicId?: ID
  title: string
  category: 'formula' | 'derivation' | 'law' | 'definition' | 'unit' | 'equation' | 'reaction' | 'identity' | 'theorem' | 'property' | 'concept'
  expression: string
  explanation?: string
  symbols?: string
  units?: string
  notes?: string
  favourite?: boolean
}

export interface PracticeQuestion extends Base {
  yearId: ID
  subjectId: ID
  chapterId?: ID
  topicId?: ID
  title: string
  question?: string
  source?: string
  difficulty: 'easy' | 'medium' | 'hard'
  attemptDate: string
  minutes?: number
  result: 'correct' | 'incorrect' | 'partial' | 'skipped'
  notes?: string
  retry: boolean
}

export type MistakeCategory =
  | 'conceptual'
  | 'formula'
  | 'calculation'
  | 'sign'
  | 'unit'
  | 'silly'
  | 'incomplete'
  | 'time'
  | 'other'

export interface Mistake extends Base {
  yearId: ID
  subjectId: ID
  chapterId?: ID
  topicId?: ID
  question: string
  wrongApproach?: string
  correctApproach?: string
  explanation?: string
  category: MistakeCategory
  date: string
  retryDate?: string
  resolved: boolean
}

export interface Exam extends Base {
  yearId: ID
  subjectId?: ID
  name: string
  type: 'unit' | 'monthly' | 'half_yearly' | 'annual' | 'mock' | 'custom'
  date: string
  chaptersCovered: string[]
  maxMarks: number
  obtainedMarks?: number
  durationMinutes?: number
  remarks?: string
  weakTopics?: string
}

export interface Note extends Base {
  yearId: ID
  subjectId?: ID
  chapterId?: ID
  topicId?: ID
  title: string
  body: string
  category?: string
  favourite?: boolean
}

export interface Resource extends Base {
  yearId: ID
  subjectId?: ID
  chapterId?: ID
  topicId?: ID
  title: string
  url: string
}

export interface Goal extends Base {
  yearId: ID
  title: string
  description?: string
  kind: 'daily' | 'weekly' | 'monthly' | 'subject' | 'chapter' | 'hours' | 'revision' | 'custom'
  target: number
  current: number
  deadline?: string
  status: 'active' | 'completed' | 'missed' | 'archived'
}

export interface ImportHistoryEntry extends Base {
  entity: string
  count: number
  summary: string
  at: string
}

export interface Settings {
  id: string
  theme: 'dark' | 'light' | 'system'
  accent: string
  fontSize: 'sm' | 'base' | 'lg'
  compact: boolean
  dailyTargetMinutes: number
  studyDays: number[]
  pomodoro: { focus: number; short: number; long: number; rounds: number }
  revisionIntervals: number[]
  defaultPriority: Priority
  notificationsEnabled: boolean
  onboardingDone: boolean
}

export interface DashboardPrefs {
  id: string
  order: string[]
  hidden: string[]
  density: 'compact' | 'comfortable'
}
