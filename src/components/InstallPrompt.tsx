import { useEffect, useState } from 'react'
import { Download, X } from 'lucide-react'
import { Button } from './ui'

interface BeforeInstallPromptEvent extends Event {
  prompt: () => Promise<void>
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed' }>
}

/** Shows an install button when the browser offers one. A normal PWA cannot add
 * Android home-screen widgets — this only installs the app itself. */
export function InstallPrompt() {
  const [event, setEvent] = useState<BeforeInstallPromptEvent | null>(null)
  const [dismissed, setDismissed] = useState(() => localStorage.getItem('install-dismissed') === '1')

  useEffect(() => {
    const handler = (e: Event) => {
      e.preventDefault()
      setEvent(e as BeforeInstallPromptEvent)
    }
    window.addEventListener('beforeinstallprompt', handler)
    return () => window.removeEventListener('beforeinstallprompt', handler)
  }, [])

  if (!event || dismissed) return null

  return (
    <div className="mb-3 flex items-center gap-2 rounded-lg border border-accent/40 bg-accent/10 px-3 py-2">
      <Download size={15} className="text-accent" />
      <p className="flex-1 text-xs">Install PCM Study OS for offline use.</p>
      <Button
        size="sm"
        onClick={async () => {
          await event.prompt()
          const choice = await event.userChoice
          if (choice.outcome === 'accepted') setEvent(null)
        }}
      >
        Install
      </Button>
      <button
        aria-label="Dismiss install prompt"
        className="text-muted hover:text-ink"
        onClick={() => {
          localStorage.setItem('install-dismissed', '1')
          setDismissed(true)
        }}
      >
        <X size={15} />
      </button>
    </div>
  )
}
