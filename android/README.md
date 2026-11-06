# PCM Study OS — Android

A genuinely native Android app: Kotlin, Jetpack Compose, Material 3, Room/SQLite.
No WebView, no accounts, no cloud, no network permission. Everything lives on the
device and works from the very first launch without a connection.

This is a port of the web app in the repository root. The web version stays where
it is; this folder is the native application.

---

## Build and install (no computer needed)

Every build runs on GitHub Actions.

1. Open **Actions → Android build** in the repository.
2. Wait for the green tick (about 4–6 minutes).
3. Download the artifact **`PCM-Study-OS-Android-APK`** at the bottom of the run.
4. Unzip it and copy `app-debug.apk` to the phone (USB, Drive, or any file
   transfer).
5. On the phone, open the file and allow "Install unknown apps" for the app you
   are installing from.

The debug APK is signed with the standard Android debug key, so it installs
without any secrets. It targets `minSdk 26` (Android 8.0) and runs on 32-bit
ARMv7 — including Android 12 Go on a TECNO Pop 7.

Tag a release to get a release build:

```bash
git tag v1.0.0
git push origin v1.0.0
```

Without signing secrets the release workflow builds the debug APK instead, so a
tag never fails and no keystore is ever committed. To sign properly, add these
repository secrets: `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`,
`ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`.

## Local build (optional)

```bash
cd android
./gradlew :app:assembleDebug     # APK at app/build/outputs/apk/debug/
./gradlew :app:testDebugUnitTest # unit + Room tests
./gradlew :app:lintDebug
```

Requires JDK 17 and the Android SDK (platform 35, build-tools 34.0.0).

---

## Architecture

```
app/src/main/java/dev/zionixx/pcmstudyos/
  data/local/entity/    18 Room entities (all UUID primary keys)
  data/local/dao/       DAOs: syllabus, study, progress joins, maintenance
  data/local/seed/      editable CBSE Class 11 syllabus + installer
  data/repo/            repositories (syllabus, study, tools, settings, backup)
  domain/               pure logic: progress, catch-up, revision, stats, parsing
  ui/theme/             Material 3 dark/light schemes, accent, font scale
  ui/components/        design system, Canvas charts, Markdown + math rendering
  ui/screens/           one file per feature area + view models
  ui/nav/               Navigation Compose graph and bottom bar
  timer/                foreground focus-timer service and controller
  util/                 notifications, WorkManager reminder
```

**Layering.** UI → ViewModel (StateFlow) → Repository → DAO → Room. Business
rules live in `domain/` as pure functions so they are unit-testable without
Android.

**Reactivity.** Screens collect `Flow`s from Room, so a write anywhere updates
every affected screen. Progress is recomputed from a change signal rather than
stored as a column — no stale percentages.

**Threading.** All database work runs on Room's own executors; UI state is
exposed as `StateFlow` collected with `collectAsStateWithLifecycle`.

## Database

Room 2.6.1, database name `pcm-study-os.db`, currently at **version 2**.

| Entity | Notes |
| --- | --- |
| `AcademicYear` | top of the hierarchy; `isCurrent` marks the active year |
| `Subject` → `Chapter` → `Topic` → `Subtopic` | cascading foreign keys, ordered by `sortOrder` |
| `StudyComponent` | theory / formulas / derivations / NCERT / numericals / questions / revision / test practice per topic |
| `StudyTask` | planner entries, recurring support, priority, estimate |
| `StudySession` | focus sessions (real minutes only) |
| `RevisionItem` / `RevisionHistory` | spaced repetition and its audit trail |
| `FormulaEntry` | formulas, derivations, laws, definitions, units |
| `PracticeRecord` | question-level accuracy tracking |
| `Mistake` | wrong vs correct approach, category, retry date |
| `Exam` / `TestResult` | tests and per-chapter breakdowns |
| `Note` | Markdown notes |
| `Goal` | daily/weekly/subject/hours/revision goals |
| `StudyStreak` | one row per active day, so streaks never scan the session table |

Indexes exist on every foreign key and on the columns the UI sorts or filters by
(`dueDate`, `status`, `priority`, `attemptDate`, `date`).

**Migrations.** `MIGRATION_1_2` adds `topics.difficulty` (with a default, so old
rows are preserved) and creates `test_results`. No table is ever dropped or
recreated. Schemas are exported to `app/schemas/` for future migration tests.

**Deletion rules.** Deleting a subject cascades to chapters → topics →
subtopics/components (foreign keys with `ON DELETE CASCADE`). The UI asks for
confirmation first, and archiving is offered instead where history matters.

**Seeding.** An empty database is initialised once with the editable CBSE Class 11
syllabus (5 subjects, 47 chapters, 172 topics, 488 subtopics) inside a single
transaction.

## Feature map (web → Android)

| Web module | Android screen |
| --- | --- |
| Dashboard | `ScreensHome.kt` — progress ring, subject cards, today's tasks, streaks, upcoming tests, quick actions |
| Academic years | Settings → Academic years (create, edit, switch, delete, reseed) |
| Syllabus | `ScreensSyllabus.kt` — drill-down subjects → chapters → topics → subtopics, component chips, reorder, duplicate, delete |
| Bulk import | `ImportScreen` — paste text/CSV/JSON, preview with counts and warnings, then import |
| Planner | `ScreensPlanner.kt` — week strip, day list, overdue block, task editor |
| Catch-up planner | bottom sheet in the planner — deterministic, shows per-day load and "at risk" flags |
| Focus timer | `ScreensStudy.kt` — Pomodoro / custom / stopwatch, subject tagging, foreground service so it survives backgrounding |
| Revision | `ScreensStudy.kt` — filters, recall rating, automatic next date, history |
| Formula library | `ScreensTools.kt` — search, favourites, pretty-printed notation |
| Practice tracker | `ScreensTools.kt` — results, accuracy, retry flags |
| Mistake notebook | `ScreensTools.kt` — categories, resolve, wrong/correct approach |
| Tests | `ScreensTools.kt` — marks, percentages, trend line |
| Notes | `ScreensTools.kt` — Markdown with headings, lists, tables and `$maths$` |
| Analytics | `ScreensStudy.kt` — range switch, daily bars, subject donut, test trend, weakest topics |
| Goals | `ScreensStudy.kt` — progress against target |
| Search | `ScreensHome.kt` — subjects, chapters, topics, notes, formulas, tasks, mistakes, tests |
| Data management | Settings → Data — JSON backup, restore (validated), CSV, PDF report, integrity check |

## Deliberate differences from the web app

- **No cloud.** No Supabase, no Firebase, no login, no sync. The `INTERNET`
  permission is not even requested, so the app physically cannot phone home.
- **No chart library.** Charts are drawn on Compose `Canvas` — smaller APK, no
  extra dependency, no layout cost on a 3 GB phone.
- **No TeX engine.** Formulas and notes store plain text and are prettified
  (`x^2` → `x²`, `\sqrt` → `√`, `->` → `→`). This is honest notation rendering,
  not LaTeX typesetting.
- **Reorder by buttons, not drag-and-drop.** Reliable on a 720p screen and much
  cheaper than a drag implementation.
- **Notifications are local only.** WorkManager raises at most one reminder a day
  while data is due; there is no push server.

## Tests

```bash
./gradlew :app:testDebugUnitTest
```

- `ProgressTest` — topic/chapter/subject roll-up, empty structures, archived rows
- `CatchupPlannerTest` — capacity, study days, ordering, at-risk detection, determinism
- `RevisionScheduleTest` — ladder, ratings, next dates, status classification
- `ImportParserTest` — paste format, CSV (quoted commas), JSON, duplicates, malformed input
- `StudyStatsTest` / `GradingTest` / `MathFormatTest` — study time, streaks, percentages, notation
- `DatabaseTest` (Robolectric) — create/read, cascade delete, persistence after reopen,
  backup/restore round-trip, restore rejection, year isolation

## Low-end device notes

Target: TECNO Pop 7, Android 12 Go, ARMv7, 3 GB RAM, 720×1612.

- Lazy lists everywhere; no nested vertical scrolling.
- Charts and rings are single `Canvas` draws — no animation, no recomposition loops.
- `material-icons-extended` is deliberately **not** used (it adds thousands of
  vectors and slows dexing); the small core icon set plus text labels is enough.
- Release builds enable R8 shrinking and resource shrinking.
- No background polling: one WorkManager job per day, and the timer only runs
  while a session is active.

## Known limitations

- Screen-level Compose UI tests are written for instrumentation
  (`androidTest`) but are not run in CI, because the emulator job is slow and
  flaky on free runners. The unit and Room tests do run.
- Home-screen widgets are not provided — this is an app, not a widget provider.
- If you clear app data or uninstall, the local database is gone; export a JSON
  backup first.
