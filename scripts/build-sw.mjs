/* Generated at build time by scripts/build-sw.mjs — do not edit by hand. */
import { readdirSync, statSync, writeFileSync, existsSync } from 'node:fs'
import { join, relative } from 'node:path'

const DIST = 'dist'

function walk(dir) {
  const out = []
  for (const entry of readdirSync(dir)) {
    const full = join(dir, entry)
    if (statSync(full).isDirectory()) out.push(...walk(full))
    else out.push(full)
  }
  return out
}

if (!existsSync(DIST)) {
  console.error('dist/ not found — run vite build first.')
  process.exit(1)
}

const files = walk(DIST)
  .map((f) => relative(DIST, f).split('\\').join('/'))
  .filter((f) => !f.endsWith('sw.js') && !f.endsWith('.map'))

const assets = files.filter((f) => /\.(js|css|png|svg|woff2?|webmanifest|ico)$/.test(f))
const version = Date.now().toString(36)
const precache = ['/', 'index.html', ...files.filter((f) => f !== 'index.html')]

const sw = `/* PCM Study OS service worker — precache ${files.length} files, version ${version} */
const CACHE = 'pcm-study-os-${version}'
const PRECACHE = ${JSON.stringify(precache, null, 0)}
const ASSETS = new Set(${JSON.stringify(assets)})

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE).then((cache) =>
      // Individual failures must not abort the whole install.
      Promise.all(
        PRECACHE.map((url) => cache.add(new Request(url, { cache: 'reload' })).catch(() => null)),
      ),
    ).then(() => self.skipWaiting()),
  )
})

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys()
      .then((keys) => Promise.all(keys.filter((k) => k !== CACHE).map((k) => caches.delete(k))))
      .then(() => self.clients.claim()),
  )
})

self.addEventListener('message', (event) => {
  if (event.data === 'SKIP_WAITING') self.skipWaiting()
})

self.addEventListener('fetch', (event) => {
  const request = event.request
  if (request.method !== 'GET') return

  const url = new URL(request.url)

  // Never cache API traffic (Supabase) — offline writes are queued in IndexedDB.
  if (url.origin !== self.location.origin) return

  // SPA navigations: always fall back to the cached shell.
  if (request.mode === 'navigate') {
    event.respondWith(
      fetch(request)
        .then((response) => {
          const copy = response.clone()
          caches.open(CACHE).then((cache) => cache.put('index.html', copy)).catch(() => null)
          return response
        })
        .catch(() => caches.match('index.html').then((cached) => cached || caches.match('/'))),
    )
    return
  }

  const path = url.pathname.replace(/^\\//, '')

  // Hashed build assets: cache-first, they never change.
  if (ASSETS.has(path)) {
    event.respondWith(
      caches.match(request).then(
        (cached) =>
          cached ||
          fetch(request).then((response) => {
            const copy = response.clone()
            caches.open(CACHE).then((cache) => cache.put(request, copy)).catch(() => null)
            return response
          }),
      ),
    )
    return
  }

  // Everything else same-origin: network first, cache as a fallback.
  event.respondWith(
    fetch(request)
      .then((response) => {
        const copy = response.clone()
        caches.open(CACHE).then((cache) => cache.put(request, copy)).catch(() => null)
        return response
      })
      .catch(() => caches.match(request)),
  )
})
`

writeFileSync(join(DIST, 'sw.js'), sw)
console.log(`sw.js written · ${files.length} precached files · ${assets.length} static assets`)
