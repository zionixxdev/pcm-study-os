import { Component, Suspense, lazy, type ErrorInfo, type ReactNode } from 'react'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AppProvider, useApp } from './store/AppContext'
import { Layout } from './components/Layout'
import { ToastProvider, Button, Card } from './components/ui'

const Dashboard = lazy(() => import('./pages/Dashboard'))
const Syllabus = lazy(() => import('./pages/Syllabus'))
const Planner = lazy(() => import('./pages/Planner'))
const TimerPage = lazy(() => import('./pages/Timer'))
const Revision = lazy(() => import('./pages/Revision'))
const Formulas = lazy(() => import('./pages/Formulas'))
const Practice = lazy(() => import('./pages/Practice'))
const Mistakes = lazy(() => import('./pages/Mistakes'))
const Tests = lazy(() => import('./pages/Tests'))
const Notes = lazy(() => import('./pages/Notes'))
const Analytics = lazy(() => import('./pages/Analytics'))
const Goals = lazy(() => import('./pages/Goals'))
const Search = lazy(() => import('./pages/Search'))
const SettingsPage = lazy(() => import('./pages/Settings'))

class ErrorBoundary extends Component<{ children: ReactNode }, { error: Error | null }> {
  state = { error: null as Error | null }

  static getDerivedStateFromError(error: Error) {
    return { error }
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('Application error', error, info.componentStack)
  }

  render() {
    if (this.state.error) {
      return (
        <div className="flex min-h-screen items-center justify-center p-4">
          <Card className="max-w-md p-6">
            <h1 className="text-lg font-semibold">Something went wrong</h1>
            <p className="mt-2 text-sm text-muted">{this.state.error.message}</p>
            <p className="mt-2 text-xs text-muted">
              Your saved data is still on this device. Reloading usually fixes this.
            </p>
            <Button className="mt-4" onClick={() => window.location.reload()}>
              Reload app
            </Button>
          </Card>
        </div>
      )
    }
    return this.props.children
  }
}

function Loading() {
  return (
    <div className="flex min-h-[60vh] items-center justify-center">
      <div className="flex flex-col items-center gap-2 text-muted">
        <div className="h-6 w-6 animate-spin rounded-full border-2 border-line border-t-accent" />
        <p className="text-xs">Loading…</p>
      </div>
    </div>
  )
}

function Shell() {
  const { ready, bootError } = useApp()
  if (!ready) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <Loading />
      </div>
    )
  }
  if (bootError) {
    return (
      <div className="flex min-h-screen items-center justify-center p-4">
        <Card className="max-w-md p-6">
          <h1 className="text-lg font-semibold">Local storage unavailable</h1>
          <p className="mt-2 text-sm text-muted">{bootError}</p>
          <p className="mt-2 text-xs text-muted">
            PCM Study OS stores your data in this browser. Private/incognito windows and blocked site data can prevent
            that. Allow site data and reload.
          </p>
          <Button className="mt-4" onClick={() => window.location.reload()}>
            Retry
          </Button>
        </Card>
      </div>
    )
  }
  return (
    <Suspense fallback={<Loading />}>
      <Routes>
        <Route element={<Layout />}>
          <Route index element={<Dashboard />} />
          <Route path="syllabus" element={<Syllabus />} />
          <Route path="planner" element={<Planner />} />
          <Route path="timer" element={<TimerPage />} />
          <Route path="revision" element={<Revision />} />
          <Route path="formulas" element={<Formulas />} />
          <Route path="practice" element={<Practice />} />
          <Route path="mistakes" element={<Mistakes />} />
          <Route path="tests" element={<Tests />} />
          <Route path="notes" element={<Notes />} />
          <Route path="goals" element={<Goals />} />
          <Route path="analytics" element={<Analytics />} />
          <Route path="search" element={<Search />} />
          <Route path="settings" element={<SettingsPage />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Route>
      </Routes>
    </Suspense>
  )
}

export default function App() {
  const basename = import.meta.env.BASE_URL.replace(/\/$/, '') || '/'
  return (
    <ErrorBoundary>
      <ToastProvider>
        <AppProvider>
          <BrowserRouter basename={basename} future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
            <Shell />
          </BrowserRouter>
        </AppProvider>
      </ToastProvider>
    </ErrorBoundary>
  )
}
