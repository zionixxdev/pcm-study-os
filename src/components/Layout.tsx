import { useEffect, useMemo, useState } from 'react'
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { ChevronLeft, CloudOff, Cloud, CloudUpload, Menu, Plus, RefreshCw, Search, TriangleAlert, X } from 'lucide-react'
import { NAV_ITEMS } from './nav'
import { Button, cx } from './ui'
import { useApp } from '../store/AppContext'
import { QuickCapture } from './QuickCapture'
import { InstallPrompt } from './InstallPrompt'
import { fullSync } from '../lib/sync'

const SYNC_LABEL: Record<string, { text: string; tone: string }> = {
  synced: { text: 'Synced', tone: 'text-emerald-400' },
  pending: { text: 'Pending', tone: 'text-amber-400' },
  syncing: { text: 'Syncing', tone: 'text-accent' },
  failed: { text: 'Sync failed', tone: 'text-red-400' },
  offline: { text: 'Offline', tone: 'text-muted' },
  disabled: { text: 'Local only', tone: 'text-muted' },
}

function SyncBadge() {
  const { sync } = useApp()
  const label = SYNC_LABEL[sync.state] ?? SYNC_LABEL.disabled
  const Icon = sync.state === 'offline' ? CloudOff : sync.state === 'syncing' ? RefreshCw : sync.state === 'failed' ? TriangleAlert : sync.state === 'pending' ? CloudUpload : Cloud
  return (
    <button
      onClick={() => void fullSync()}
      title={sync.message ?? (sync.lastSyncAt ? `Last sync ${new Date(sync.lastSyncAt).toLocaleString()}` : 'Not synced yet')}
      className="flex items-center gap-1.5 rounded-lg border border-line px-2 py-1.5 text-[11px] hover:border-accent/60"
    >
      <Icon size={13} className={cx(label.tone, sync.state === 'syncing' && 'animate-spin')} />
      <span className={cx('hidden sm:inline', label.tone)}>{label.text}</span>
      {sync.pending > 0 ? <span className="rounded bg-raised px-1 text-[10px]">{sync.pending}</span> : null}
    </button>
  )
}

export function Layout() {
  const { years, currentYear, setCurrentYear, settings } = useApp()
  const [collapsed, setCollapsed] = useState(() => localStorage.getItem('sidebar') === 'collapsed')
  const [moreOpen, setMoreOpen] = useState(false)
  const [quickOpen, setQuickOpen] = useState(false)
  const location = useLocation()
  const navigate = useNavigate()

  useEffect(() => {
    setMoreOpen(false)
  }, [location.pathname])

  useEffect(() => {
    localStorage.setItem('sidebar', collapsed ? 'collapsed' : 'open')
  }, [collapsed])

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      const target = e.target as HTMLElement | null
      const typing = target && ['INPUT', 'TEXTAREA', 'SELECT'].includes(target.tagName)
      if (typing) return
      if (e.key === 'n' && !e.metaKey && !e.ctrlKey) {
        e.preventDefault()
        setQuickOpen(true)
      }
      if (e.key === '/') {
        e.preventDefault()
        navigate('/search')
      }
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [navigate])

  const primary = useMemo(() => NAV_ITEMS.filter((n) => n.primary), [])
  const secondary = useMemo(() => NAV_ITEMS.filter((n) => !n.primary), [])

  return (
    <div className="min-h-screen bg-bg">
      {/* ---------------------------- Sidebar ---------------------------- */}
      <aside
        className={cx(
          'fixed inset-y-0 left-0 z-30 hidden flex-col border-r border-line bg-surface lg:flex',
          collapsed ? 'w-[68px]' : 'w-60',
        )}
      >
        <div className="flex h-14 items-center gap-2 border-b border-line px-3">
          <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-accent text-sm font-bold text-white">P</div>
          {!collapsed ? (
            <div className="min-w-0">
              <p className="truncate text-sm font-semibold">PCM Study OS</p>
              <p className="truncate text-[11px] text-muted">{currentYear?.name ?? 'No year'}</p>
            </div>
          ) : null}
        </div>

        <nav className="flex-1 overflow-y-auto px-2 py-3">
          {NAV_ITEMS.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === '/'}
              className={({ isActive }) =>
                cx(
                  'mb-0.5 flex items-center gap-2.5 rounded-lg px-2.5 py-2 text-sm transition-colors',
                  isActive ? 'bg-accent/15 text-accent' : 'text-muted hover:bg-raised hover:text-ink',
                  collapsed && 'justify-center px-0',
                )
              }
              title={item.label}
            >
              <item.icon size={17} className="shrink-0" />
              {!collapsed ? <span className="truncate">{item.label}</span> : null}
            </NavLink>
          ))}
        </nav>

        <div className="border-t border-line p-2">
          <button
            onClick={() => setCollapsed((c) => !c)}
            className="flex w-full items-center gap-2 rounded-lg px-2.5 py-2 text-xs text-muted hover:bg-raised hover:text-ink"
          >
            <ChevronLeft size={15} className={cx('transition-transform', collapsed && 'rotate-180')} />
            {!collapsed ? 'Collapse' : null}
          </button>
        </div>
      </aside>

      {/* ----------------------------- Header ---------------------------- */}
      <div className={cx('transition-[padding]', collapsed ? 'lg:pl-[68px]' : 'lg:pl-60')}>
        <header className="sticky top-0 z-20 flex h-14 items-center gap-2 border-b border-line bg-bg/95 px-3 backdrop-blur sm:px-4">
          <div className="flex items-center gap-2 lg:hidden">
            <div className="flex h-7 w-7 items-center justify-center rounded-md bg-accent text-xs font-bold text-white">P</div>
            <span className="text-sm font-semibold">PCM Study OS</span>
          </div>

          <div className="ml-auto flex items-center gap-1.5">
            <select
              aria-label="Academic year"
              value={currentYear?.id ?? ''}
              onChange={(e) => void setCurrentYear(e.target.value)}
              className="max-w-[130px] rounded-lg border border-line bg-surface px-2 py-1.5 text-[11px] text-ink focus:border-accent focus:outline-none sm:max-w-[180px]"
            >
              {years.map((y) => (
                <option key={y.id} value={y.id}>
                  {y.name}
                </option>
              ))}
            </select>
            <SyncBadge />
            <button
              onClick={() => navigate('/search')}
              aria-label="Search"
              className="rounded-lg border border-line p-1.5 text-muted hover:border-accent/60 hover:text-ink"
            >
              <Search size={16} />
            </button>
            <Button size="sm" onClick={() => setQuickOpen(true)} className="h-8">
              <Plus size={14} />
              <span className="hidden sm:inline">Capture</span>
            </Button>
          </div>
        </header>

        <main className={cx('mx-auto w-full max-w-6xl px-3 pb-24 pt-4 sm:px-4 lg:pb-10', settings.compact && 'text-sm')}>
          <InstallPrompt />
          <Outlet />
        </main>
      </div>

      {/* --------------------------- Bottom nav --------------------------- */}
      <nav className="fixed bottom-0 left-0 right-0 z-30 border-t border-line bg-surface/98 pb-[env(safe-area-inset-bottom)] backdrop-blur lg:hidden">
        <div className="flex items-stretch">
          {primary.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === '/'}
              className={({ isActive }) =>
                cx(
                  'flex flex-1 flex-col items-center gap-0.5 py-2 text-[10px]',
                  isActive ? 'text-accent' : 'text-muted',
                )
              }
            >
              <item.icon size={19} />
              <span>{item.short}</span>
            </NavLink>
          ))}
          <button
            onClick={() => setMoreOpen(true)}
            className={cx('flex flex-1 flex-col items-center gap-0.5 py-2 text-[10px]', moreOpen ? 'text-accent' : 'text-muted')}
          >
            <Menu size={19} />
            <span>More</span>
          </button>
        </div>
      </nav>

      {/* ---------------------------- More sheet -------------------------- */}
      {moreOpen ? (
        <div className="fixed inset-0 z-40 bg-black/60 lg:hidden" onClick={() => setMoreOpen(false)}>
          <div className="absolute bottom-0 left-0 right-0 rounded-t-2xl border-t border-line bg-surface p-3 pb-8" onClick={(e) => e.stopPropagation()}>
            <div className="mb-2 flex items-center justify-between">
              <p className="text-sm font-semibold">All sections</p>
              <button onClick={() => setMoreOpen(false)} aria-label="Close" className="rounded p-1 text-muted hover:text-ink">
                <X size={18} />
              </button>
            </div>
            <div className="grid grid-cols-3 gap-2">
              {[...secondary, { to: '/search', label: 'Search', short: 'Search', icon: Search }].map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  className={({ isActive }) =>
                    cx(
                      'flex flex-col items-center gap-1 rounded-lg border border-line px-2 py-3 text-[11px]',
                      isActive ? 'border-accent/60 text-accent' : 'text-muted',
                    )
                  }
                >
                  <item.icon size={18} />
                  {item.short}
                </NavLink>
              ))}
            </div>
          </div>
        </div>
      ) : null}

      <QuickCapture open={quickOpen} onClose={() => setQuickOpen(false)} />
    </div>
  )
}
