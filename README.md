# PCM Study OS

A personal academic management system for a CBSE Class 11 PCM student — and any
student who wants to track a syllabus properly. Offline-first PWA, local
IndexedDB storage, optional Supabase cloud sync with row-level security.

**Not a mockup.** Every screen reads and writes real records; every percentage is
computed from your data.

---

## What it does

| Area | Capabilities |
| --- | --- |
| Dashboard | Progress, subject cards, today's tasks, study stats, streaks, revision overview, deadlines, quick actions. Widgets can be shown, hidden and reordered. |
| Academic years | Multiple years (Class 11 — 2026–27, Class 12 — 2027–28, custom). Switch, edit, archive, copy a syllabus structure into another year. Progress never mixes between years. |
| Syllabus | Year → Subject → Chapter → Topic → Subtopic, unlimited depth. Per-topic study components (theory, formulas, derivations, NCERT, numericals, questions, revision, test practice). Automatic progress roll-up. |
| Bulk import | Paste text, CSV or JSON; preview, edit, duplicate detection, then import into a new or existing chapter. |
| Planner | Day / week / month / calendar views, priorities, deadlines, recurring tasks, and a deterministic catch-up planner that respects daily capacity and deadlines. |
| Focus timer | Pomodoro, custom focus and stopwatch modes, subject/chapter/topic tagging, session history. Only real focused time is saved. |
| Revision | Spaced repetition (1/3/7/14/30 days by default, configurable), recall ratings, automatic next-date scheduling, history. |
| Formula library | Formulas, derivations, laws, definitions, units, reactions, theorems — rendered with KaTeX, searchable, favouritable. |
| Practice tracker | Question-level log with accuracy, chapter-wise performance and a retry queue. |
| Mistake notebook | Wrong approach vs correct approach, mistake categories, retry dates, pattern detection. |
| Tests | Unit/monthly/half-yearly/annual/mock, marks, percentages, score trend chart, weak topics. |
| Notes | Markdown notes with headings, lists, tables and `$maths$`. |
| Analytics | Date-ranged charts for study time, subject progress, test trend, accuracy, weakest/strongest topics. |
| Data | CSV export per table, PDF reports, full JSON backup with validated restore (merge or replace). |
| Search | Global search across subjects, chapters, topics, subtopics, notes, formulas, tasks, mistakes and tests. |

## Tech stack

React 18 · TypeScript · Vite 5 · Tailwind CSS · Dexie (IndexedDB) · Supabase
(Postgres, Auth, RLS) · Recharts · KaTeX · jsPDF · lucide-react · Vitest.

## Getting started

```bash
npm install
cp .env.example .env.local   # optional — the app runs fully local without it
npm run dev
```

```bash
npm run typecheck   # tsc --noEmit across app + config
npm run lint        # eslint, zero warnings allowed
npm run test        # vitest (40 unit tests)
npm run build       # tsc + vite build + service worker generation
npm run preview     # serve the production build
```

## Cloud sync (optional)

1. Create a Supabase project.
2. Run the migrations in order:
   - `supabase/migrations/0001_init.sql`
   - `supabase/migrations/0002_rls.sql`
3. Put the project URL and **anon/publishable** key in `.env.local`:

```env
VITE_SUPABASE_URL=https://<ref>.supabase.co
VITE_SUPABASE_ANON_KEY=<anon key>
```

The anon key is safe in the browser — every table is protected by RLS policies
that require `auth.uid() = user_id`. The service-role key is never used by the
client and must never be committed.

### Google sign-in

Supabase → Authentication → Providers → Google, then add the client ID/secret
from Google Cloud Console (APIs & Services → Credentials → OAuth client → Web
application). Authorised redirect URI:

```
https://<project-ref>.supabase.co/auth/v1/callback
```

Site URL / additional redirect URLs must include the deployed app origin (for
example `https://<user>.github.io/pcm-study-os/`).

Without Google configured, the app still works: it runs in local-only mode and
stores everything on the device.

## Offline behaviour

- A generated service worker (`scripts/build-sw.mjs`) precaches the app shell and
  every hashed asset, so the app opens offline after the first visit.
- All reads and writes go to IndexedDB first. Writes queue in an `outbox` table.
- `lib/sync.ts` pushes the outbox, then pulls remote rows. A row with a queued
  local change is never overwritten; otherwise the newer `updatedAt` wins.
- The header shows the true state: synced, pending, syncing, failed or offline.
  It never claims a sync that did not happen.

## Project layout

```
src/
  components/   design system, layout, quick capture, markdown, maths
  hooks/        live queries (year data, tasks, sessions, stats)
  lib/          db (Dexie), progress, catchup, revision, import, export, sync, supabase
  lib/seed/     editable CBSE Class 11 syllabus
  pages/        one file per navigation section
  store/        app context (settings, years, auth, sync)
  types/        domain types
supabase/migrations/   versioned SQL (schema + RLS)
scripts/build-sw.mjs   service worker generator
```

## Notes on the seeded syllabus

The Class 11 CBSE seed follows the **2025-26 rationalised** curriculum
(14 Physics chapters — "Physical World" removed; 9 Chemistry chapters; 14
Mathematics chapters; English Core Hornbill/Snapshots; Computer Science 083).
It is **editable data, not a locked curriculum**: rename, reorder, delete or
extend anything. Check it against your school's split-up before relying on it.

Informatics Practices (065) is deliberately *not* merged into Computer Science —
add it as a separate subject if you take it.

## Limitations

- A PWA cannot add native Android home-screen widgets. The app installs to the
  home screen and works offline, but there is no widget.
- Browser notifications only fire while the app is open; there is no push server.
- Account deletion calls an optional `delete-account` Edge Function. Deploy it if
  you want server-side deletion; otherwise clear local data and delete the
  Supabase auth user manually.
- Charts are code-split per route so the dashboard never pays for Recharts.

## Licence

MIT — use it, fork it, adapt it for your own class.
