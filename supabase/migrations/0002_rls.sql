-- PCM Study OS — Row Level Security
-- Every table is locked down: a user can only touch rows they own, and child
-- rows must point at a parent that belongs to the same user.

alter table public.pcm_profiles enable row level security;
alter table public.pcm_academic_years enable row level security;
alter table public.pcm_subjects enable row level security;
alter table public.pcm_chapters enable row level security;
alter table public.pcm_topics enable row level security;
alter table public.pcm_subtopics enable row level security;
alter table public.pcm_components enable row level security;
alter table public.pcm_tasks enable row level security;
alter table public.pcm_sessions enable row level security;
alter table public.pcm_revisions enable row level security;
alter table public.pcm_revision_history enable row level security;
alter table public.pcm_formulas enable row level security;
alter table public.pcm_practice enable row level security;
alter table public.pcm_mistakes enable row level security;
alter table public.pcm_exams enable row level security;
alter table public.pcm_notes enable row level security;
alter table public.pcm_resources enable row level security;
alter table public.pcm_goals enable row level security;
alter table public.pcm_import_history enable row level security;
alter table public.pcm_user_settings enable row level security;
alter table public.pcm_dashboard_preferences enable row level security;

-- ------------------------------------------------------------------ profiles
drop policy if exists profiles_select on public.pcm_profiles;
create policy profiles_select on public.pcm_profiles for select using (auth.uid() = id);

drop policy if exists profiles_update on public.pcm_profiles;
create policy profiles_update on public.pcm_profiles for update using (auth.uid() = id) with check (auth.uid() = id);

drop policy if exists profiles_insert on public.pcm_profiles;
create policy profiles_insert on public.pcm_profiles for insert with check (auth.uid() = id);

-- ---------------------------------------------- simple owner-only tables
do $$
declare
  t text;
begin
  foreach t in array array[
    'pcm_academic_years','pcm_subjects','pcm_chapters','pcm_topics','pcm_subtopics','pcm_components',
    'pcm_tasks','pcm_sessions','pcm_revisions','pcm_revision_history','pcm_formulas','pcm_practice',
    'pcm_mistakes','pcm_exams','pcm_notes','pcm_resources','pcm_goals','pcm_import_history',
    'pcm_user_settings','pcm_dashboard_preferences'
  ]
  loop
    execute format('drop policy if exists %I_owner on public.%I', t, t);
    execute format(
      'create policy %I_owner on public.%I for all to authenticated
         using (auth.uid() = user_id) with check (auth.uid() = user_id)',
      t, t
    );
  end loop;
end;
$$;

-- ------------------------------------ parent ownership for child tables
-- These guards stop a user attaching a row to somebody else's syllabus tree.

create or replace function public.pcm_owns_academic_year(p_id uuid)
returns boolean language sql stable security definer set search_path = public as $$
  select exists (select 1 from public.pcm_academic_years where id = p_id and user_id = auth.uid());
$$;

create or replace function public.pcm_owns_subject(p_id uuid)
returns boolean language sql stable security definer set search_path = public as $$
  select exists (select 1 from public.pcm_subjects where id = p_id and user_id = auth.uid());
$$;

create or replace function public.pcm_owns_chapter(p_id uuid)
returns boolean language sql stable security definer set search_path = public as $$
  select exists (select 1 from public.pcm_chapters where id = p_id and user_id = auth.uid());
$$;

create or replace function public.pcm_owns_topic(p_id uuid)
returns boolean language sql stable security definer set search_path = public as $$
  select exists (select 1 from public.pcm_topics where id = p_id and user_id = auth.uid());
$$;

create or replace function public.pcm_owns_revision(p_id uuid)
returns boolean language sql stable security definer set search_path = public as $$
  select exists (select 1 from public.pcm_revisions where id = p_id and user_id = auth.uid());
$$;

drop policy if exists subjects_parent_guard on public.pcm_subjects;
create policy subjects_parent_guard on public.pcm_subjects as restrictive for insert
  with check (public.pcm_owns_academic_year(year_id));

drop policy if exists chapters_parent_guard on public.pcm_chapters;
create policy chapters_parent_guard on public.pcm_chapters as restrictive for insert
  with check (public.pcm_owns_subject(subject_id));

drop policy if exists topics_parent_guard on public.pcm_topics;
create policy topics_parent_guard on public.pcm_topics as restrictive for insert
  with check (public.pcm_owns_chapter(chapter_id));

drop policy if exists subtopics_parent_guard on public.pcm_subtopics;
create policy subtopics_parent_guard on public.pcm_subtopics as restrictive for insert
  with check (public.pcm_owns_topic(topic_id));

drop policy if exists components_parent_guard on public.pcm_components;
create policy components_parent_guard on public.pcm_components as restrictive for insert
  with check (public.pcm_owns_topic(topic_id));

drop policy if exists revision_history_parent_guard on public.pcm_revision_history;
create policy revision_history_parent_guard on public.pcm_revision_history as restrictive for insert
  with check (public.pcm_owns_revision(revision_id));

drop policy if exists formulas_parent_guard on public.pcm_formulas;
create policy formulas_parent_guard on public.pcm_formulas as restrictive for insert
  with check (public.pcm_owns_academic_year(year_id) and public.pcm_owns_subject(subject_id));

drop policy if exists practice_parent_guard on public.pcm_practice;
create policy practice_parent_guard on public.pcm_practice as restrictive for insert
  with check (public.pcm_owns_academic_year(year_id) and public.pcm_owns_subject(subject_id));

drop policy if exists mistakes_parent_guard on public.pcm_mistakes;
create policy mistakes_parent_guard on public.pcm_mistakes as restrictive for insert
  with check (public.pcm_owns_academic_year(year_id) and public.pcm_owns_subject(subject_id));

-- Note: anon role gets no access at all — every table requires an authenticated
-- user whose uid matches user_id.
