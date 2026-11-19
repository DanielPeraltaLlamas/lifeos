-- Life OS: bloques de enfoque (Entrega 4a). Pegar en Supabase → SQL Editor → Run.
create table if not exists public.focus_session (
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  id text not null,
  task_id text,
  project_id text,
  title text,
  started_at bigint,
  planned_ms bigint,
  paused_ms bigint,
  paused_at bigint,
  ended_at bigint,
  block_index bigint,
  block_total bigint,
  created_at bigint,
  updated_at bigint not null,
  deleted_at bigint,
  synced_at bigint not null default 0,
  primary key (user_id, id)
);
create index if not exists focus_session_synced on public.focus_session(user_id, synced_at);
alter table public.focus_session enable row level security;
drop policy if exists "solo el dueño" on public.focus_session;
create policy "solo el dueño" on public.focus_session for all to authenticated
  using (user_id = auth.uid()) with check (user_id = auth.uid());
grant select, insert, update, delete on public.focus_session to authenticated;
drop trigger if exists lww on public.focus_session;
create trigger lww before insert or update on public.focus_session
  for each row execute function public.lifeos_lww();
