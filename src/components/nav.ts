import {
  BarChart3,
  BookOpen,
  CalendarDays,
  ClipboardList,
  LayoutDashboard,
  Repeat,
  Settings as SettingsIcon,
  Sigma,
  StickyNote,
  Target,
  Timer,
  TriangleAlert,
  PencilRuler,
} from 'lucide-react'
import type { LucideIcon } from 'lucide-react'

export interface NavItem {
  to: string
  label: string
  short: string
  icon: LucideIcon
  primary?: boolean
}

export const NAV_ITEMS: NavItem[] = [
  { to: '/', label: 'Dashboard', short: 'Home', icon: LayoutDashboard, primary: true },
  { to: '/syllabus', label: 'Syllabus', short: 'Syllabus', icon: BookOpen, primary: true },
  { to: '/planner', label: 'Planner', short: 'Planner', icon: CalendarDays, primary: true },
  { to: '/timer', label: 'Focus Timer', short: 'Focus', icon: Timer, primary: true },
  { to: '/revision', label: 'Revision', short: 'Revision', icon: Repeat },
  { to: '/formulas', label: 'Formula Library', short: 'Formulas', icon: Sigma },
  { to: '/practice', label: 'Practice Tracker', short: 'Practice', icon: PencilRuler },
  { to: '/mistakes', label: 'Mistake Notebook', short: 'Mistakes', icon: TriangleAlert },
  { to: '/tests', label: 'Tests', short: 'Tests', icon: ClipboardList },
  { to: '/notes', label: 'Notes', short: 'Notes', icon: StickyNote },
  { to: '/goals', label: 'Goals', short: 'Goals', icon: Target },
  { to: '/analytics', label: 'Analytics', short: 'Analytics', icon: BarChart3 },
  { to: '/settings', label: 'Settings', short: 'Settings', icon: SettingsIcon },
]
