import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import { db, createRow, updateRow, setCurrentUserId, getMeta, setMeta, LOCAL_USER } from '../lib/db'
import { todayStr } from '../lib/dates'
import { installSyllabus } from '../lib/seed/install'
import { supabase, supabaseConfigured, signInWithGoogle, signOut as sbSignOut } from '../lib/supabase'
import { initSync, fullSync, getSyncStatus, subscribeSync, refreshPendingCount, type SyncStatus } from '../lib/sync'
import type { AcademicYear, DashboardPrefs, Settings } from '../types'

export const DEFAULT_SETTINGS: Settings = {
  id: 'app',
  theme: 'dark',
  accent: '#4f8cff',
  fontSize: 'base',
  compact: false,
  dailyTargetMinutes: 240,
  studyDays: [0, 1, 2, 3, 4, 5],
  pomodoro: { focus: 25, short: 5, long: 15, rounds: 4 },
  revisionIntervals: [1, 3, 7, 14, 30],
  defaultPriority: 'medium',
  notificationsEnabled: false,
  onboardingDone: false,
}

export const DEFAULT_PREFS: DashboardPrefs = {
  id: 'dashboard',
  order: ['progress', 'subjects', 'today', 'stats', 'streak', 'revision', 'deadlines', 'actions'],
  hidden: [],
  density: 'comfortable',
}

interface AppContextValue {
  ready: boolean
  bootError: string | null
  settings: Settings
  saveSettings: (patch: Partial<Settings>) => Promise<void>
  prefs: DashboardPrefs
  savePrefs: (patch: Partial<DashboardPrefs>) => Promise<void>
  years: AcademicYear[]
  currentYear: AcademicYear | null
  setCurrentYear: (id: string) => Promise<void>
  reloadYears: () => Promise<void>
  userEmail: string | null
  signedIn: boolean
  authLoading: boolean
  signInGoogle: () => Promise<void>
  signOutUser: () => Promise<void>
  sync: SyncStatus
  syncNow: () => Promise<void>
}

const AppCtx = createContext<AppContextValue | null>(null)

export function useApp(): AppContextValue {
  const ctx = useContext(AppCtx)
  if (!ctx) throw new Error('useApp must be used inside AppProvider')
  return ctx
}

export function AppProvider({ children }: { children: ReactNode }) {
  const [ready, setReady] = useState(false)
  const [bootError, setBootError] = useState<string | null>(null)
  const [settings, setSettings] = useState<Settings>(DEFAULT_SETTINGS)
  const [prefs, setPrefs] = useState<DashboardPrefs>(DEFAULT_PREFS)
  const [years, setYears] = useState<AcademicYear[]>([])
  const [currentYearId, setCurrentYearId] = useState<string | null>(null)
  const [userEmail, setUserEmail] = useState<string | null>(null)
  const [authLoading, setAuthLoading] = useState(supabaseConfigured)
  const [sync, setSync] = useState<SyncStatus>(getSyncStatus())
  const booted = useRef(false)

  const loadYears = useCallback(async () => {
    const list = (await db.academicYears.toArray()).sort((a, b) => (a.startDate < b.startDate ? 1 : -1))
    setYears(list)
    return list
  }, [])

  /* ------------------------------ bootstrap ------------------------------ */
  useEffect(() => {
    if (booted.current) return
    booted.current = true

    const run = async () => {
      try {
        const storedSettings = await db.settings.get('app')
        if (storedSettings) setSettings({ ...DEFAULT_SETTINGS, ...storedSettings })
        else await db.settings.put(DEFAULT_SETTINGS)

        const storedPrefs = await db.dashboardPrefs.get('dashboard')
        if (storedPrefs) setPrefs({ ...DEFAULT_PREFS, ...storedPrefs })
        else await db.dashboardPrefs.put(DEFAULT_PREFS)

        let list = await loadYears()
        if (list.length === 0) {
          const yearId = await createRow('academicYears', {
            name: 'Class 11 — 2026–27',
            startDate: '2026-04-01',
            endDate: '2027-03-31',
            isCurrent: true,
          })
          await installSyllabus(yearId)
          list = await loadYears()
        }

        const storedCurrent = await getMeta('currentYearId')
        const current = list.find((y) => y.id === storedCurrent) ?? list.find((y) => y.isCurrent) ?? list[0]
        if (current) {
          setCurrentYearId(current.id)
          if (current.id !== storedCurrent) await setMeta('currentYearId', current.id)
        }

        await initSync()
        setSync(getSyncStatus())
        setReady(true)
      } catch (err) {
        setBootError(err instanceof Error ? err.message : 'The local database could not be opened.')
        setReady(true)
      }
    }
    void run()
  }, [loadYears])

  /* -------------------------------- auth -------------------------------- */
  useEffect(() => {
    if (!supabase) {
      setCurrentUserId(LOCAL_USER)
      setAuthLoading(false)
      return
    }
    let mounted = true
    supabase.auth.getSession().then(({ data }) => {
      if (!mounted) return
      const user = data.session?.user
      setCurrentUserId(user?.id ?? LOCAL_USER)
      setUserEmail(user?.email ?? null)
      setAuthLoading(false)
      if (user) void fullSync()
    })
    const { data: sub } = supabase.auth.onAuthStateChange((_event, session) => {
      const user = session?.user
      setCurrentUserId(user?.id ?? LOCAL_USER)
      setUserEmail(user?.email ?? null)
      setAuthLoading(false)
    })
    return () => {
      mounted = false
      sub.subscription.unsubscribe()
    }
  }, [])

  useEffect(() => subscribeSync(setSync), [])

  /* ----------------------------- appearance ----------------------------- */
  useEffect(() => {
    const root = document.documentElement
    const apply = () => {
      const theme =
        settings.theme === 'system'
          ? window.matchMedia('(prefers-color-scheme: light)').matches
            ? 'light'
            : 'dark'
          : settings.theme
      root.dataset.theme = theme
      root.dataset.font = settings.fontSize
      root.style.setProperty('--c-accent', hexToRgbTriplet(settings.accent))
      const meta = document.querySelector('meta[name="theme-color"]')
      if (meta) meta.setAttribute('content', theme === 'light' ? '#f6f8fb' : '#0b0f14')
    }
    apply()
    if (settings.theme !== 'system') return
    const mq = window.matchMedia('(prefers-color-scheme: light)')
    mq.addEventListener('change', apply)
    return () => mq.removeEventListener('change', apply)
  }, [settings.theme, settings.accent, settings.fontSize])

  /* ------------------------------- actions ------------------------------ */
  const saveSettings = useCallback(async (patch: Partial<Settings>) => {
    const next = { ...settings, ...patch }
    setSettings(next)
    await db.settings.put(next)
    await refreshPendingCount()
  }, [settings])

  const savePrefs = useCallback(async (patch: Partial<DashboardPrefs>) => {
    const next = { ...prefs, ...patch }
    setPrefs(next)
    await db.dashboardPrefs.put(next)
  }, [prefs])

  const setCurrentYear = useCallback(async (id: string) => {
    setCurrentYearId(id)
    await setMeta('currentYearId', id)
    for (const y of years) {
      if (y.isCurrent !== (y.id === id)) await updateRow('academicYears', y.id, { isCurrent: y.id === id })
    }
    await loadYears()
  }, [years, loadYears])

  const signInGoogle = useCallback(async () => {
    const { error } = await signInWithGoogle()
    if (error) throw new Error(error)
  }, [])

  const signOutUser = useCallback(async () => {
    await sbSignOut()
    setCurrentUserId(LOCAL_USER)
    setUserEmail(null)
  }, [])

  const syncNow = useCallback(async () => {
    await fullSync()
  }, [])

  const currentYear = useMemo(
    () => years.find((y) => y.id === currentYearId) ?? null,
    [years, currentYearId],
  )

  const value = useMemo<AppContextValue>(
    () => ({
      ready,
      bootError,
      settings,
      saveSettings,
      prefs,
      savePrefs,
      years,
      currentYear,
      setCurrentYear,
      reloadYears: async () => {
        await loadYears()
      },
      userEmail,
      signedIn: Boolean(userEmail),
      authLoading,
      signInGoogle,
      signOutUser,
      sync,
      syncNow,
    }),
    [ready, bootError, settings, saveSettings, prefs, savePrefs, years, currentYear, setCurrentYear, loadYears, userEmail, authLoading, signInGoogle, signOutUser, sync, syncNow],
  )

  return <AppCtx.Provider value={value}>{children}</AppCtx.Provider>
}

function hexToRgbTriplet(hex: string): string {
  const clean = hex.replace('#', '')
  const full = clean.length === 3 ? clean.split('').map((c) => c + c).join('') : clean
  const num = Number.parseInt(full || '4f8cff', 16)
  const r = (num >> 16) & 255
  const g = (num >> 8) & 255
  const b = num & 255
  return `${r} ${g} ${b}`
}

/** Convenience: today's date string, recomputed on render. */
export const today = todayStr
