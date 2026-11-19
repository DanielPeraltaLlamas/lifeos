-- Life OS: tablas de sincronización (Entrega 3).
-- Se pega entero en Supabase → SQL Editor → Run. Se puede ejecutar más de una vez.
--
-- Cada tabla es copia de la local (shared/src/commonMain/sqldelight) más:
--   user_id   dueño de la fila (lo pone Postgres con auth.uid()).
--   synced_at milisegundos del servidor en que cambió la fila; los dispositivos
--             descargan lo que tenga synced_at mayor que lo último que vieron.
-- Conflictos: gana la fila con updated_at mayor (trigger lifeos_lww).

create or replace function public.lifeos_lww() returns trigger
language plpgsql set search_path = '' as $$
begin
  if tg_op = 'UPDATE' and new.updated_at <= old.updated_at then
    return null;  -- lo que llega es más viejo o igual: se ignora
  end if;
  new.synced_at := (extract(epoch from clock_timestamp()) * 1000)::bigint;
  return new;
end $$;

create table if not exists public.project (
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  id text not null,
  name text,
  kind text,
  description text,
  due_date text,
  sort_order bigint,
  created_at bigint,
  updated_at bigint not null,
  deleted_at bigint,
  synced_at bigint not null default 0,
  primary key (user_id, id)
);
create index if not exists project_synced on public.project(user_id, synced_at);
alter table public.project enable row level security;
drop policy if exists "solo el dueño" on public.project;
create policy "solo el dueño" on public.project for all to authenticated
  using (user_id = auth.uid()) with check (user_id = auth.uid());
grant select, insert, update, delete on public.project to authenticated;
drop trigger if exists lww on public.project;
create trigger lww before insert or update on public.project
  for each row execute function public.lifeos_lww();

create table if not exists public.milestone (
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  id text not null,
  project_id text,
  title text,
  deliverable text,
  due_date text,
  created_at bigint,
  updated_at bigint not null,
  deleted_at bigint,
  synced_at bigint not null default 0,
  primary key (user_id, id)
);
create index if not exists milestone_synced on public.milestone(user_id, synced_at);
alter table public.milestone enable row level security;
drop policy if exists "solo el dueño" on public.milestone;
create policy "solo el dueño" on public.milestone for all to authenticated
  using (user_id = auth.uid()) with check (user_id = auth.uid());
grant select, insert, update, delete on public.milestone to authenticated;
drop trigger if exists lww on public.milestone;
create trigger lww before insert or update on public.milestone
  for each row execute function public.lifeos_lww();

create table if not exists public.task (
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  id text not null,
  project_id text,
  milestone_id text,
  title text,
  notes text,
  due_date text,
  planned_date text,
  estimate_blocks bigint,
  done_at bigint,
  created_at bigint,
  updated_at bigint not null,
  deleted_at bigint,
  synced_at bigint not null default 0,
  primary key (user_id, id)
);
create index if not exists task_synced on public.task(user_id, synced_at);
alter table public.task enable row level security;
drop policy if exists "solo el dueño" on public.task;
create policy "solo el dueño" on public.task for all to authenticated
  using (user_id = auth.uid()) with check (user_id = auth.uid());
grant select, insert, update, delete on public.task to authenticated;
drop trigger if exists lww on public.task;
create trigger lww before insert or update on public.task
  for each row execute function public.lifeos_lww();

create table if not exists public.daily_plan (
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  id text not null,
  first_task_id text,
  chosen_at bigint,
  created_at bigint,
  updated_at bigint not null,
  deleted_at bigint,
  synced_at bigint not null default 0,
  primary key (user_id, id)
);
create index if not exists daily_plan_synced on public.daily_plan(user_id, synced_at);
alter table public.daily_plan enable row level security;
drop policy if exists "solo el dueño" on public.daily_plan;
create policy "solo el dueño" on public.daily_plan for all to authenticated
  using (user_id = auth.uid()) with check (user_id = auth.uid());
grant select, insert, update, delete on public.daily_plan to authenticated;
drop trigger if exists lww on public.daily_plan;
create trigger lww before insert or update on public.daily_plan
  for each row execute function public.lifeos_lww();

create table if not exists public.habit (
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  id text not null,
  name text,
  period text,
  target bigint,
  target_max bigint,
  sort_order bigint,
  created_at bigint,
  updated_at bigint not null,
  deleted_at bigint,
  synced_at bigint not null default 0,
  primary key (user_id, id)
);
create index if not exists habit_synced on public.habit(user_id, synced_at);
alter table public.habit enable row level security;
drop policy if exists "solo el dueño" on public.habit;
create policy "solo el dueño" on public.habit for all to authenticated
  using (user_id = auth.uid()) with check (user_id = auth.uid());
grant select, insert, update, delete on public.habit to authenticated;
drop trigger if exists lww on public.habit;
create trigger lww before insert or update on public.habit
  for each row execute function public.lifeos_lww();

create table if not exists public.habit_log (
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  id text not null,
  habit_id text,
  date text,
  logged_at bigint,
  created_at bigint,
  updated_at bigint not null,
  deleted_at bigint,
  synced_at bigint not null default 0,
  primary key (user_id, id)
);
create index if not exists habit_log_synced on public.habit_log(user_id, synced_at);
alter table public.habit_log enable row level security;
drop policy if exists "solo el dueño" on public.habit_log;
create policy "solo el dueño" on public.habit_log for all to authenticated
  using (user_id = auth.uid()) with check (user_id = auth.uid());
grant select, insert, update, delete on public.habit_log to authenticated;
drop trigger if exists lww on public.habit_log;
create trigger lww before insert or update on public.habit_log
  for each row execute function public.lifeos_lww();

create table if not exists public.deadline (
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  id text not null,
  project_id text,
  title text,
  kind text,
  due_date text,
  done_at bigint,
  created_at bigint,
  updated_at bigint not null,
  deleted_at bigint,
  synced_at bigint not null default 0,
  primary key (user_id, id)
);
create index if not exists deadline_synced on public.deadline(user_id, synced_at);
alter table public.deadline enable row level security;
drop policy if exists "solo el dueño" on public.deadline;
create policy "solo el dueño" on public.deadline for all to authenticated
  using (user_id = auth.uid()) with check (user_id = auth.uid());
grant select, insert, update, delete on public.deadline to authenticated;
drop trigger if exists lww on public.deadline;
create trigger lww before insert or update on public.deadline
  for each row execute function public.lifeos_lww();

create table if not exists public.inbox_item (
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  id text not null,
  text text,
  source text,
  created_at bigint,
  updated_at bigint not null,
  deleted_at bigint,
  synced_at bigint not null default 0,
  primary key (user_id, id)
);
create index if not exists inbox_item_synced on public.inbox_item(user_id, synced_at);
alter table public.inbox_item enable row level security;
drop policy if exists "solo el dueño" on public.inbox_item;
create policy "solo el dueño" on public.inbox_item for all to authenticated
  using (user_id = auth.uid()) with check (user_id = auth.uid());
grant select, insert, update, delete on public.inbox_item to authenticated;
drop trigger if exists lww on public.inbox_item;
create trigger lww before insert or update on public.inbox_item
  for each row execute function public.lifeos_lww();

create table if not exists public.note (
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  id text not null,
  project_id text,
  kind text,
  body text,
  url text,
  created_at bigint,
  updated_at bigint not null,
  deleted_at bigint,
  synced_at bigint not null default 0,
  primary key (user_id, id)
);
create index if not exists note_synced on public.note(user_id, synced_at);
alter table public.note enable row level security;
drop policy if exists "solo el dueño" on public.note;
create policy "solo el dueño" on public.note for all to authenticated
  using (user_id = auth.uid()) with check (user_id = auth.uid());
grant select, insert, update, delete on public.note to authenticated;
drop trigger if exists lww on public.note;
create trigger lww before insert or update on public.note
  for each row execute function public.lifeos_lww();

create table if not exists public.event (
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  id text not null,
  project_id text,
  title text,
  kind text,
  date text,
  start_min bigint,
  end_min bigint,
  repeat_weekly bigint,
  until_date text,
  skip_dates text,
  location text,
  created_at bigint,
  updated_at bigint not null,
  deleted_at bigint,
  synced_at bigint not null default 0,
  primary key (user_id, id)
);
create index if not exists event_synced on public.event(user_id, synced_at);
alter table public.event enable row level security;
drop policy if exists "solo el dueño" on public.event;
create policy "solo el dueño" on public.event for all to authenticated
  using (user_id = auth.uid()) with check (user_id = auth.uid());
grant select, insert, update, delete on public.event to authenticated;
drop trigger if exists lww on public.event;
create trigger lww before insert or update on public.event
  for each row execute function public.lifeos_lww();
