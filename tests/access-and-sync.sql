\set ON_ERROR_STOP on
begin;
insert into auth.users(id, email) values
 ('10000000-0000-0000-0000-000000000001', 'rls-a@example.invalid'),
 ('10000000-0000-0000-0000-000000000002', 'rls-b@example.invalid');
insert into public.categories(id, name) values ('20000000-0000-0000-0000-000000000001', 'Teste');
insert into public.establishments(id, category_id, name, latitude, longitude) values
 ('30000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', 'Parceiro fictício de teste', -26.48, -49.06);
insert into public.benefits(establishment_id, title) values ('30000000-0000-0000-0000-000000000001', 'Benefício fictício');

set local role authenticated;
select set_config('request.jwt.claims', '{"sub":"10000000-0000-0000-0000-000000000001","role":"authenticated"}', true);
select public.set_favorite('30000000-0000-0000-0000-000000000001', true, now());
select public.set_favorite('30000000-0000-0000-0000-000000000001', false, now() - interval '1 minute');
select public.record_visit('40000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001', now(), 180000);
select public.record_visit('40000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001', now(), 180000);
do $$ begin
  if (select count(*) from public.visits) <> 1 then raise exception 'Visit retry was not idempotent'; end if;
  if not (select is_favorite from public.favorites limit 1) then raise exception 'Older favorite overwrote newer state'; end if;
  begin
    insert into public.favorites(user_id, establishment_id, is_favorite, changed_at) values
      ('10000000-0000-0000-0000-000000000002', '30000000-0000-0000-0000-000000000001', true, now());
    raise exception 'Cross-user insertion succeeded';
  exception when insufficient_privilege then null;
  end;
  begin
    insert into public.visits(user_id, id, establishment_id, detected_at, dwell_ms) values
      ('10000000-0000-0000-0000-000000000002', gen_random_uuid(), '30000000-0000-0000-0000-000000000001', now(), 180000);
    raise exception 'Cross-user visit insertion succeeded';
  exception when insufficient_privilege then null;
  end;
  begin
    insert into public.categories(name) values ('Unauthorized');
    raise exception 'Catalog write succeeded';
  exception when insufficient_privilege then null;
  end;
end $$;
select set_config('request.jwt.claims', '{"sub":"10000000-0000-0000-0000-000000000002","role":"authenticated"}', true);
do $$ begin
  if (select count(*) from public.visits) <> 0 then raise exception 'Cross-user visit leakage'; end if;
  if (select count(*) from public.favorites) <> 0 then raise exception 'Cross-user favorite leakage'; end if;
end $$;
reset role;
set local role anon;
do $$ begin
  begin
    perform * from public.establishments;
    raise exception 'Anonymous catalog read succeeded';
  exception when insufficient_privilege then null;
  end;
end $$;
reset role;
rollback;
\echo 'PASS: RLS isolation, anonymous restriction, catalog protection, favorite conflict and visit idempotency'
