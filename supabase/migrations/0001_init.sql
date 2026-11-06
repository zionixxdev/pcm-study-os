-- PCM Study OS — initial schema
-- Every user-owned table carries user_id -> auth.users(id) and is protected by
-- Row Level Security. Child rows are additionally constrained to a parent that
-- belongs to the same user.

create extension if not exists "pgcrypto";

-- ---------------------------------------------------------------- profiles
create table if not exists public.pcm_profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  display_name text,
  avatar_url text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

-- --------------------------------------------------------- academic years
create table if not exists public.pcm_academic_years (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  name text not null,
  start_date date not null,
  end_date date not null,
  is_current boolean not null default false,
  archived boolean not null default false,
  notes text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create index if not exists academic_years_user_idx on public.pcm_academic_years (user_id, start_date desc);

-- --------------------------------------------------------------- subjects
create table if not exists public.pcm_subjects (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  year_id uuid not null references public.pcm_academic_years(id) on delete cascade,
  name text not null,
  description text,
  icon text,
  color text not null default '#4f8cff',
  priority text not null default 'medium' check (priority in ('low','medium','high')),
  goal text,
  target_date date,
  "order" numeric not null default 0,
  archived boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create index if not exists subjects_user_year_idx on public.pcm_subjects (user_id, year_id, "order");

-- --------------------------------------------------------------- chapters
create table if not exists public.pcm_chapters (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  subject_id uuid not null references public.pcm_subjects(id) on delete cascade,
  name text not null,
  description text,
  priority text not null default 'medium' check (priority in ('low','medium','high')),
  estimated_hours numeric,
  deadline date,
  notes text,
  "order" numeric not null default 0,
  archived boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create index if not exists chapters_user_subject_idx on public.pcm_chapters (user_id, subject_id, "order");

-- ----------------------------------------------------------------- topics
create table if not exists public.pcm_topics (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  chapter_id uuid not null references public.pcm_chapters(id) on delete cascade,
  name text not null,
  description text,
  notes text,
  priority text not null default 'medium' check (priority in ('low','medium','high')),
  estimated_minutes integer,
  target_date date,
  status text not null default 'not_started'
    check (status in ('not_started','in_progress','completed','needs_revision')),
  favourite boolean not null default false,
  "order" numeric not null default 0,
  archived boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create index if not exists topics_user_chapter_idx on public.pcm_topics (user_id, chapter_id, "order");
create index if not exists topics_user_status_idx on public.pcm_topics (user_id, status);

-- -------------------------------------------------------------- subtopics
create table if not exists public.pcm_subtopics (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  topic_id uuid not null references public.pcm_topics(id) on delete cascade,
  name text not null,
  notes text,
  difficulty text not null default 'medium' check (difficulty in ('easy','medium','hard')),
  completed boolean not null default false,
  "order" numeric not null default 0,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create index if not exists subtopics_user_topic_idx on public.pcm_subtopics (user_id, topic_id, "order");

-- ------------------------------------------------------ study components
create table if not exists public.pcm_components (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  topic_id uuid not null references public.pcm_topics(id) on delete cascade,
  type text not null check (type in ('theory','formulas','derivations','ncert','numericals','questions','revision','test')),
  done boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (topic_id, type)
);
create index if not exists components_user_topic_idx on public.pcm_components (user_id, topic_id);

-- ------------------------------------------------------------------ tasks
create table if not exists public.pcm_tasks (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  year_id uuid not null references public.pcm_academic_years(id) on delete cascade,
  title text not null,
  description text,
  subject_id uuid references public.pcm_subjects(id) on delete set null,
  chapter_id uuid references public.pcm_chapters(id) on delete set null,
  topic_id uuid references public.pcm_topics(id) on delete set null,
  priority text not null default 'medium' check (priority in ('low','medium','high')),
  status text not null default 'pending' check (status in ('pending','in_progress','completed')),
  start_date date,
  due_date date,
  estimated_minutes integer not null default 30,
  recurring text not null default 'none' check (recurring in ('none','daily','weekly')),
  "order" numeric not null default 0,
  completed_at timestamptz,
  archived boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create index if not exists tasks_user_year_due_idx on public.pcm_tasks (user_id, year_id, due_date);
create index if not exists tasks_user_status_idx on public.pcm_tasks (user_id, status);

-- -------------------------------------------------------- study sessions
create table if not exists public.pcm_sessions (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  year_id uuid not null references public.pcm_academic_years(id) on delete cascade,
  subject_id uuid references public.pcm_subjects(id) on delete set null,
  chapter_id uuid references public.pcm_chapters(id) on delete set null,
  topic_id uuid references public.pcm_topics(id) on delete set null,
  started_at timestamptz not null,
  ended_at timestamptz not null,
  minutes integer not null check (minutes >= 0),
  mode text not null default 'pomodoro' check (mode in ('pomodoro','focus','stopwatch')),
  notes text,
  archived boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create index if not exists sessions_user_started_idx on public.pcm_sessions (user_id, started_at desc);

-- ------------------------------------------------------- revision items
create table if not exists public.pcm_revisions (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  year_id uuid not null references public.pcm_academic_years(id) on delete cascade,
  topic_id uuid references public.pcm_topics(id) on delete cascade,
  subtopic_id uuid references public.pcm_subtopics(id) on delete cascade,
  label text not null,
  due_date date not null,
  interval_index integer not null default 0,
  status text not null default 'upcoming' check (status in ('due','upcoming','overdue','completed')),
  last_rating text check (last_rating in ('again','hard','good','easy')),
  notes text,
  completed_at timestamptz,
  archived boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create index if not exists revisions_user_due_idx on public.pcm_revisions (user_id, due_date);

create table if not exists public.pcm_revision_history (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  revision_id uuid not null references public.pcm_revisions(id) on delete cascade,
  rating text not null check (rating in ('again','hard','good','easy')),
  notes text,
  at timestamptz not null default now(),
  archived boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create index if not exists revision_history_user_idx on public.pcm_revision_history (user_id, at desc);

-- ------------------------------------------------------- formula library
create table if not exists public.pcm_formulas (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  year_id uuid not null references public.pcm_academic_years(id) on delete cascade,
  subject_id uuid not null references public.pcm_subjects(id) on delete cascade,
  chapter_id uuid references public.pcm_chapters(id) on delete set null,
  topic_id uuid references public.pcm_topics(id) on delete set null,
  title text not null,
  category text not null default 'formula',
  expression text not null,
  explanation text,
  symbols text,
  units text,
  notes text,
  favourite boolean not null default false,
  archived boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create index if not exists formulas_user_subject_idx on public.pcm_formulas (user_id, subject_id);

-- ----------------------------------------------------- practice tracker
create table if not exists public.pcm_practice (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  year_id uuid not null references public.pcm_academic_years(id) on delete cascade,
  subject_id uuid not null references public.pcm_subjects(id) on delete cascade,
  chapter_id uuid references public.pcm_chapters(id) on delete set null,
  topic_id uuid references public.pcm_topics(id) on delete set null,
  title text not null,
  question text,
  source text,
  difficulty text not null default 'medium' check (difficulty in ('easy','medium','hard')),
  attempt_date date not null default current_date,
  minutes integer,
  result text not null default 'correct' check (result in ('correct','incorrect','partial','skipped')),
  notes text,
  retry boolean not null default false,
  archived boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create index if not exists practice_user_subject_idx on public.pcm_practice (user_id, subject_id, attempt_date desc);

-- ----------------------------------------------------- mistake notebook
create table if not exists public.pcm_mistakes (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  year_id uuid not null references public.pcm_academic_years(id) on delete cascade,
  subject_id uuid not null references public.pcm_subjects(id) on delete cascade,
  chapter_id uuid references public.pcm_chapters(id) on delete set null,
  topic_id uuid references public.pcm_topics(id) on delete set null,
  question text not null,
  wrong_approach text,
  correct_approach text,
  explanation text,
  category text not null default 'conceptual'
    check (category in ('conceptual','formula','calculation','sign','unit','silly','incomplete','time','other')),
  date date not null default current_date,
  retry_date date,
  resolved boolean not null default false,
  archived boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create index if not exists mistakes_user_subject_idx on public.pcm_mistakes (user_id, subject_id, date desc);

-- ------------------------------------------------------------------ exams
create table if not exists public.pcm_exams (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  year_id uuid not null references public.pcm_academic_years(id) on delete cascade,
  subject_id uuid references public.pcm_subjects(id) on delete set null,
  name text not null,
  type text not null default 'unit' check (type in ('unit','monthly','half_yearly','annual','mock','custom')),
  date date not null,
  chapters_covered text[] not null default '{}',
  max_marks numeric not null check (max_marks > 0),
  obtained_marks numeric check (obtained_marks >= 0),
  duration_minutes integer,
  remarks text,
  weak_topics text,
  archived boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint marks_within_max check (obtained_marks is null or obtained_marks <= max_marks)
);
create index if not exists exams_user_date_idx on public.pcm_exams (user_id, date desc);

-- ------------------------------------------------------------------ notes
create table if not exists public.pcm_notes (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  year_id uuid not null references public.pcm_academic_years(id) on delete cascade,
  subject_id uuid references public.pcm_subjects(id) on delete set null,
  chapter_id uuid references public.pcm_chapters(id) on delete set null,
  topic_id uuid references public.pcm_topics(id) on delete set null,
  title text not null,
  body text not null default '',
  category text,
  favourite boolean not null default false,
  archived boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create index if not exists notes_user_year_idx on public.pcm_notes (user_id, year_id, updated_at desc);

-- -------------------------------------------------------------- resources
create table if not exists public.pcm_resources (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  year_id uuid not null references public.pcm_academic_years(id) on delete cascade,
  subject_id uuid references public.pcm_subjects(id) on delete set null,
  chapter_id uuid references public.pcm_chapters(id) on delete set null,
  topic_id uuid references public.pcm_topics(id) on delete set null,
  title text not null,
  url text not null,
  archived boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

-- ------------------------------------------------------------------ goals
create table if not exists public.pcm_goals (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  year_id uuid not null references public.pcm_academic_years(id) on delete cascade,
  title text not null,
  description text,
  kind text not null default 'custom'
    check (kind in ('daily','weekly','monthly','subject','chapter','hours','revision','custom')),
  target numeric not null check (target > 0),
  current numeric not null default 0,
  deadline date,
  status text not null default 'active' check (status in ('active','completed','missed','archived')),
  archived boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

-- ------------------------------------------------------- import history
create table if not exists public.pcm_import_history (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  entity text not null,
  count integer not null default 0,
  summary text,
  at timestamptz not null default now(),
  archived boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

-- ------------------------------------------------ preferences & settings
create table if not exists public.pcm_user_settings (
  id text primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  data jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.pcm_dashboard_preferences (
  id text primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  data jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

-- ---------------------------------------------------------------- triggers
create or replace function public.pcm_touch_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

do $$
declare
  t text;
begin
  foreach t in array array[
    'pcm_profiles','pcm_academic_years','pcm_subjects','pcm_chapters','pcm_topics','pcm_subtopics','pcm_components',
    'pcm_tasks','pcm_sessions','pcm_revisions','pcm_revision_history','pcm_formulas','pcm_practice','pcm_mistakes',
    'pcm_exams','pcm_notes','pcm_resources','pcm_goals','pcm_import_history','pcm_user_settings','pcm_dashboard_preferences'
  ]
  loop
    execute format('drop trigger if exists %I_touch on public.%I', t, t);
    execute format('create trigger %I_touch before update on public.%I for each row execute function public.pcm_touch_updated_at()', t, t);
  end loop;
end;
$$;

-- Create a profile row for every new auth user.
create or replace function public.pcm_handle_new_user()
returns trigger
language plpgsql
security definer set search_path = public
as $$
begin
  insert into public.pcm_profiles (id, display_name)
  values (new.id, coalesce(new.raw_user_meta_data->>'full_name', split_part(new.email, '@', 1)))
  on conflict (id) do nothing;
  return new;
end;
$$;

drop trigger if exists pcm_on_auth_user_created on auth.users;
create trigger pcm_on_auth_user_created
  after insert on auth.users
  for each row execute function public.pcm_handle_new_user();
