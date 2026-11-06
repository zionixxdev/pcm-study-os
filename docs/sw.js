/* PCM Study OS service worker — precache 102 files, version mw4onv3b */
const CACHE = 'pcm-study-os-mw4onv3b'
const PRECACHE = ["/","index.html","_redirects","assets/Analytics-CUy8hnke.js","assets/Dashboard-BzkX7mw-.js","assets/Formulas-B2y8bRts.css","assets/Formulas-kqBhElQZ.js","assets/Goals-CDNLUxh2.js","assets/KaTeX_AMS-Regular-BQhdFMY1.woff2","assets/KaTeX_AMS-Regular-DMm9YOAa.woff","assets/KaTeX_AMS-Regular-DRggAlZN.ttf","assets/KaTeX_Caligraphic-Bold-ATXxdsX0.ttf","assets/KaTeX_Caligraphic-Bold-BEiXGLvX.woff","assets/KaTeX_Caligraphic-Bold-Dq_IR9rO.woff2","assets/KaTeX_Caligraphic-Regular-CTRA-rTL.woff","assets/KaTeX_Caligraphic-Regular-Di6jR-x-.woff2","assets/KaTeX_Caligraphic-Regular-wX97UBjC.ttf","assets/KaTeX_Fraktur-Bold-BdnERNNW.ttf","assets/KaTeX_Fraktur-Bold-BsDP51OF.woff","assets/KaTeX_Fraktur-Bold-CL6g_b3V.woff2","assets/KaTeX_Fraktur-Regular-CB_wures.ttf","assets/KaTeX_Fraktur-Regular-CTYiF6lA.woff2","assets/KaTeX_Fraktur-Regular-Dxdc4cR9.woff","assets/KaTeX_Main-Bold-Cx986IdX.woff2","assets/KaTeX_Main-Bold-Jm3AIy58.woff","assets/KaTeX_Main-Bold-waoOVXN0.ttf","assets/KaTeX_Main-BoldItalic-DxDJ3AOS.woff2","assets/KaTeX_Main-BoldItalic-DzxPMmG6.ttf","assets/KaTeX_Main-BoldItalic-SpSLRI95.woff","assets/KaTeX_Main-Italic-3WenGoN9.ttf","assets/KaTeX_Main-Italic-BMLOBm91.woff","assets/KaTeX_Main-Italic-NWA7e6Wa.woff2","assets/KaTeX_Main-Regular-B22Nviop.woff2","assets/KaTeX_Main-Regular-Dr94JaBh.woff","assets/KaTeX_Main-Regular-ypZvNtVU.ttf","assets/KaTeX_Math-BoldItalic-B3XSjfu4.ttf","assets/KaTeX_Math-BoldItalic-CZnvNsCZ.woff2","assets/KaTeX_Math-BoldItalic-iY-2wyZ7.woff","assets/KaTeX_Math-Italic-DA0__PXp.woff","assets/KaTeX_Math-Italic-flOr_0UB.ttf","assets/KaTeX_Math-Italic-t53AETM-.woff2","assets/KaTeX_SansSerif-Bold-CFMepnvq.ttf","assets/KaTeX_SansSerif-Bold-D1sUS0GD.woff2","assets/KaTeX_SansSerif-Bold-DbIhKOiC.woff","assets/KaTeX_SansSerif-Italic-C3H0VqGB.woff2","assets/KaTeX_SansSerif-Italic-DN2j7dab.woff","assets/KaTeX_SansSerif-Italic-YYjJ1zSn.ttf","assets/KaTeX_SansSerif-Regular-BNo7hRIc.ttf","assets/KaTeX_SansSerif-Regular-CS6fqUqJ.woff","assets/KaTeX_SansSerif-Regular-DDBCnlJ7.woff2","assets/KaTeX_Script-Regular-C5JkGWo-.ttf","assets/KaTeX_Script-Regular-D3wIWfF6.woff2","assets/KaTeX_Script-Regular-D5yQViql.woff","assets/KaTeX_Size1-Regular-C195tn64.woff","assets/KaTeX_Size1-Regular-Dbsnue_I.ttf","assets/KaTeX_Size1-Regular-mCD8mA8B.woff2","assets/KaTeX_Size2-Regular-B7gKUWhC.ttf","assets/KaTeX_Size2-Regular-Dy4dx90m.woff2","assets/KaTeX_Size2-Regular-oD1tc_U0.woff","assets/KaTeX_Size3-Regular-CTq5MqoE.woff","assets/KaTeX_Size3-Regular-DgpXs0kz.ttf","assets/KaTeX_Size4-Regular-BF-4gkZK.woff","assets/KaTeX_Size4-Regular-DWFBv043.ttf","assets/KaTeX_Size4-Regular-Dl5lxZxV.woff2","assets/KaTeX_Typewriter-Regular-C0xS9mPB.woff","assets/KaTeX_Typewriter-Regular-CO6r4hn1.woff2","assets/KaTeX_Typewriter-Regular-D3Ib7_Hf.ttf","assets/Mistakes-BJmKrY8V.js","assets/Notes-CulP5qCd.js","assets/Planner-c8LR8s9f.js","assets/Practice-2x08b3-J.js","assets/Revision-Bkkys3Wr.js","assets/Search-BgMlj8G6.js","assets/Settings-D8Chy5lq.js","assets/Syllabus-Dn8pknTC.js","assets/Tests-C0kBZRRj.js","assets/Timer-DCATcsFS.js","assets/archive-Cjrdd6lJ.js","assets/browser-DoFuJwgN.js","assets/charts-C2VK1mB3.js","assets/check-BQoTF_0f.js","assets/chevron-down-RArLGvCb.js","assets/chevron-right-fO9njtNX.js","assets/db-B2401vb1.js","assets/eye-BAAaoWxi.js","assets/html2canvas.esm-Dw6mdHwC.js","assets/index-DTPXdll0.js","assets/index-DUTMfd-R.css","assets/index.es-Bll10xSC.js","assets/katex-JXajUVY-.js","assets/pdf-CU495f9Z.js","assets/pencil-B92N-Acv.js","assets/progress-BOPSUQrk.js","assets/purify.es-C0Ri02_H.js","assets/rotate-ccw-Dp9osfSO.js","assets/tasks-BfoVp7Kh.js","assets/trash-2-Ce5RxHHI.js","assets/useStats-CU1K1VLn.js","assets/vendor-6XqOfgen.js","icons/icon-192.png","icons/icon-512.png","icons/maskable-512.png","manifest.webmanifest"]
const ASSETS = new Set(["assets/Analytics-CUy8hnke.js","assets/Dashboard-BzkX7mw-.js","assets/Formulas-B2y8bRts.css","assets/Formulas-kqBhElQZ.js","assets/Goals-CDNLUxh2.js","assets/KaTeX_AMS-Regular-BQhdFMY1.woff2","assets/KaTeX_AMS-Regular-DMm9YOAa.woff","assets/KaTeX_Caligraphic-Bold-BEiXGLvX.woff","assets/KaTeX_Caligraphic-Bold-Dq_IR9rO.woff2","assets/KaTeX_Caligraphic-Regular-CTRA-rTL.woff","assets/KaTeX_Caligraphic-Regular-Di6jR-x-.woff2","assets/KaTeX_Fraktur-Bold-BsDP51OF.woff","assets/KaTeX_Fraktur-Bold-CL6g_b3V.woff2","assets/KaTeX_Fraktur-Regular-CTYiF6lA.woff2","assets/KaTeX_Fraktur-Regular-Dxdc4cR9.woff","assets/KaTeX_Main-Bold-Cx986IdX.woff2","assets/KaTeX_Main-Bold-Jm3AIy58.woff","assets/KaTeX_Main-BoldItalic-DxDJ3AOS.woff2","assets/KaTeX_Main-BoldItalic-SpSLRI95.woff","assets/KaTeX_Main-Italic-BMLOBm91.woff","assets/KaTeX_Main-Italic-NWA7e6Wa.woff2","assets/KaTeX_Main-Regular-B22Nviop.woff2","assets/KaTeX_Main-Regular-Dr94JaBh.woff","assets/KaTeX_Math-BoldItalic-CZnvNsCZ.woff2","assets/KaTeX_Math-BoldItalic-iY-2wyZ7.woff","assets/KaTeX_Math-Italic-DA0__PXp.woff","assets/KaTeX_Math-Italic-t53AETM-.woff2","assets/KaTeX_SansSerif-Bold-D1sUS0GD.woff2","assets/KaTeX_SansSerif-Bold-DbIhKOiC.woff","assets/KaTeX_SansSerif-Italic-C3H0VqGB.woff2","assets/KaTeX_SansSerif-Italic-DN2j7dab.woff","assets/KaTeX_SansSerif-Regular-CS6fqUqJ.woff","assets/KaTeX_SansSerif-Regular-DDBCnlJ7.woff2","assets/KaTeX_Script-Regular-D3wIWfF6.woff2","assets/KaTeX_Script-Regular-D5yQViql.woff","assets/KaTeX_Size1-Regular-C195tn64.woff","assets/KaTeX_Size1-Regular-mCD8mA8B.woff2","assets/KaTeX_Size2-Regular-Dy4dx90m.woff2","assets/KaTeX_Size2-Regular-oD1tc_U0.woff","assets/KaTeX_Size3-Regular-CTq5MqoE.woff","assets/KaTeX_Size4-Regular-BF-4gkZK.woff","assets/KaTeX_Size4-Regular-Dl5lxZxV.woff2","assets/KaTeX_Typewriter-Regular-C0xS9mPB.woff","assets/KaTeX_Typewriter-Regular-CO6r4hn1.woff2","assets/Mistakes-BJmKrY8V.js","assets/Notes-CulP5qCd.js","assets/Planner-c8LR8s9f.js","assets/Practice-2x08b3-J.js","assets/Revision-Bkkys3Wr.js","assets/Search-BgMlj8G6.js","assets/Settings-D8Chy5lq.js","assets/Syllabus-Dn8pknTC.js","assets/Tests-C0kBZRRj.js","assets/Timer-DCATcsFS.js","assets/archive-Cjrdd6lJ.js","assets/browser-DoFuJwgN.js","assets/charts-C2VK1mB3.js","assets/check-BQoTF_0f.js","assets/chevron-down-RArLGvCb.js","assets/chevron-right-fO9njtNX.js","assets/db-B2401vb1.js","assets/eye-BAAaoWxi.js","assets/html2canvas.esm-Dw6mdHwC.js","assets/index-DTPXdll0.js","assets/index-DUTMfd-R.css","assets/index.es-Bll10xSC.js","assets/katex-JXajUVY-.js","assets/pdf-CU495f9Z.js","assets/pencil-B92N-Acv.js","assets/progress-BOPSUQrk.js","assets/purify.es-C0Ri02_H.js","assets/rotate-ccw-Dp9osfSO.js","assets/tasks-BfoVp7Kh.js","assets/trash-2-Ce5RxHHI.js","assets/useStats-CU1K1VLn.js","assets/vendor-6XqOfgen.js","icons/icon-192.png","icons/icon-512.png","icons/maskable-512.png","manifest.webmanifest"])

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

  const path = url.pathname.replace(/^\//, '')

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
