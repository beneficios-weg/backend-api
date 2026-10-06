# WEG Benefits Backend — Supabase

MVP para revisão técnica: Supabase Auth, catálogo, favoritos e visitas, com PostgreSQL, RLS e sincronização idempotente. CLI 2.119.0 fixada no lockfile; nenhum servidor Node próprio.

## Executar localmente

Pré-requisitos: Node >=22.12.0 e Docker Desktop Linux ativo.

```sh
npm ci
npm run supabase:start
npm run db:migrate
npm run db:test
npm run supabase:status
```

Studio: http://127.0.0.1:54323. API: http://127.0.0.1:54321. PostgreSQL: porta 54322. Chaves locais aparecem no status; nunca commitá-las. No mobile use somente chave publishable/anon com o JWT do usuário.

`npm run supabase:stop` preserva os dados. `db:migrate` aplica migrations pendentes localmente. `db:test` usa Docker/psql e reverte seus fixtures com ROLLBACK.

## Contrato e decisões

- [Implementação, contrato, RLS, decisões iniciais e limites](docs/implementacao-mvp.md)
- [Migration inicial](supabase/migrations/20261006120658_initial_benefits.sql)
- [Testes de acesso e sincronização](tests/access-and-sync.sql)
- [Mobile implementado em branch separada](https://github.com/beneficios-weg/mobile/tree/feat/confirmed-mobile)
- [Contexto para agentes](AI_CONTEXT.md)

Modelo inicial: categorias, estabelecimentos, benefício principal, favoritos por usuário e visitas com UUID idempotente. Catálogo é somente leitura para colaboradores; cadastro administrativo pelo Studio. Não há seed automático nem dados de parceiros reais. Primeiro acesso/recuperação usam Supabase Auth por e-mail; não há SSO WEG.

Nenhum projeto remoto foi criado/vinculado ou alterado. Antes de publicar, revisar o modelo, conflitos, limites de envio/retensão, autorização corporativa e configuração de e-mail/redirects com o tech lead.

A documentação do scaffold e da consolidação anterior permanece como histórico; a migration e `docs/implementacao-mvp.md` descrevem este branch.
