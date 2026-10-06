-- Initial MVP contract: technical decisions documented in docs/implementacao-mvp.md.
create table public.categories (
  id uuid primary key default gen_random_uuid(),
  name text not null check (length(name) between 1 and 100),
  active boolean not null default true
);
create table public.establishments (
  id uuid primary key default gen_random_uuid(),
  category_id uuid not null references public.categories(id),
  name text not null check (length(name) between 1 and 200),
  description text not null default '',
  address text not null default '',
  latitude double precision not null check (latitude between -90 and 90),
  longitude double precision not null check (longitude between -180 and 180),
  active boolean not null default true
);
create index establishments_category_idx on public.establishments(category_id);
create table public.benefits (
  id uuid primary key default gen_random_uuid(),
  establishment_id uuid not null unique references public.establishments(id) on delete cascade,
  title text not null check (length(title) between 1 and 200),
  description text not null default '',
  terms text not null default '',
  valid_until timestamptz,
  active boolean not null default true
);
create table public.favorites (
  user_id uuid not null references auth.users(id) on delete cascade,
  establishment_id uuid not null references public.establishments(id),
  is_favorite boolean not null,
  changed_at timestamptz not null,
  primary key (user_id, establishment_id)
);
create table public.visits (
  user_id uuid not null references auth.users(id) on delete cascade,
  id uuid not null,
  establishment_id uuid not null references public.establishments(id),
  detected_at timestamptz not null,
  dwell_ms integer not null check (dwell_ms between 30000 and 900000),
  received_at timestamptz not null default now(),
  primary key (user_id, id)
);
create index visits_user_time_idx on public.visits(user_id, detected_at desc);

alter table public.categories enable row level security;
alter table public.establishments enable row level security;
alter table public.benefits enable row level security;
alter table public.favorites enable row level security;
alter table public.visits enable row level security;

revoke all on public.categories, public.establishments, public.benefits, public.favorites, public.visits from anon, authenticated;
grant select on public.categories, public.establishments, public.benefits, public.favorites, public.visits to authenticated;
grant insert, update on public.favorites to authenticated;
grant insert on public.visits to authenticated;
grant all on public.categories, public.establishments, public.benefits, public.favorites, public.visits to service_role;

create policy categories_read on public.categories for select to authenticated using (active);
create policy establishments_read on public.establishments for select to authenticated using (
  active and exists (select 1 from public.categories c where c.id = category_id and c.active)
);
create policy benefits_read on public.benefits for select to authenticated using (
  active and (valid_until is null or valid_until > now()) and
  exists (select 1 from public.establishments e where e.id = establishment_id and e.active)
);
create policy favorites_read on public.favorites for select to authenticated using ((select auth.uid()) = user_id);
create policy favorites_insert on public.favorites for insert to authenticated with check (
  (select auth.uid()) = user_id and changed_at <= now() + interval '5 minutes' and
  exists (select 1 from public.establishments e where e.id = establishment_id and e.active)
);
create policy favorites_update on public.favorites for update to authenticated using ((select auth.uid()) = user_id) with check (
  (select auth.uid()) = user_id and changed_at <= now() + interval '5 minutes' and
  exists (select 1 from public.establishments e where e.id = establishment_id and e.active)
);
create policy visits_read on public.visits for select to authenticated using ((select auth.uid()) = user_id);
create policy visits_insert on public.visits for insert to authenticated with check (
  (select auth.uid()) = user_id and detected_at <= now() + interval '5 minutes' and
  detected_at >= now() - interval '30 days' and
  exists (select 1 from public.establishments e where e.id = establishment_id and e.active)
);

-- Invoker privileges retain RLS. Tombstones and timestamps prevent older queued
-- favorites from overwriting a more recent change on another device.
create function public.set_favorite(p_establishment_id uuid, p_is_favorite boolean, p_changed_at timestamptz)
returns void language sql security invoker set search_path = '' as $$
  insert into public.favorites(user_id, establishment_id, is_favorite, changed_at)
  values ((select auth.uid()), p_establishment_id, p_is_favorite, p_changed_at)
  on conflict (user_id, establishment_id) do update
    set is_favorite = excluded.is_favorite, changed_at = excluded.changed_at
    where public.favorites.changed_at < excluded.changed_at;
$$;
revoke all on function public.set_favorite(uuid, boolean, timestamptz) from public, anon;
grant execute on function public.set_favorite(uuid, boolean, timestamptz) to authenticated;

-- Visit identity is stable across retries. An existing id never mutates history.
create function public.record_visit(p_id uuid, p_establishment_id uuid, p_detected_at timestamptz, p_dwell_ms integer)
returns void language sql security invoker set search_path = '' as $$
  insert into public.visits(user_id, id, establishment_id, detected_at, dwell_ms)
  values ((select auth.uid()), p_id, p_establishment_id, p_detected_at, p_dwell_ms)
  on conflict (user_id, id) do nothing;
$$;
revoke all on function public.record_visit(uuid, uuid, timestamptz, integer) from public, anon;
grant execute on function public.record_visit(uuid, uuid, timestamptz, integer) to authenticated;
