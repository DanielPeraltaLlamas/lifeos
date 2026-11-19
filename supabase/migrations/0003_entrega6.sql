-- Life OS: Entrega 6 (cierre del día, notas con título, uso diario, revisión semanal, preferencias).
-- Pegar en Supabase → SQL Editor → Run.
alter table public.daily_plan add column if not exists closed_at bigint;
alter table public.note add column if not exists title text;

create table if not exists public.usage_log (
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  id text not null, scroll_ms bigint, created_at bigint, updated_at bigint not null, deleted_at bigint,
  synced_at bigint not null default 0, primary key (user_id, id)
);
create table if not exists public.week_review (
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  id text not null, note text, goals text, done_at bigint, created_at bigint, updated_at bigint not null, deleted_at bigint,
  synced_at bigint not null default 0, primary key (user_id, id)
);
create table if not exists public.pref (
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  id text not null, value text, created_at bigint, updated_at bigint not null, deleted_at bigint,
  synced_at bigint not null default 0, primary key (user_id, id)
);

do $$
declare t text;
begin
  foreach t in array array['usage_log', 'week_review', 'pref'] loop
    execute format('create index if not exists %1$s_synced on public.%1$s(user_id, synced_at)', t);
    execute format('alter table public.%s enable row level security', t);
    execute format('drop policy if exists "solo el dueño" on public.%s', t);
    execute format('create policy "solo el dueño" on public.%s for all to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid())', t);
    execute format('grant select, insert, update, delete on public.%s to authenticated', t);
    execute format('drop trigger if exists lww on public.%s', t);
    execute format('create trigger lww before insert or update on public.%s for each row execute function public.lifeos_lww()', t);
  end loop;
end $$;
