# Scaffold Supabase — 2026-10-06

## Decisão e escopo

O responsável pelo projeto confirmou Supabase como backend. O repositório mantém a estrutura oficial gerada por `supabase init`, com CLI 2.119.0 fixada e lockfile npm. Não há servidor Node: o package.json serve apenas para ferramentas e comandos do Supabase.

Nenhuma tabela de domínio, migration SQL, policy RLS, seed, Edge Function, regra de negócio ou integração mobile foi implementada. O diretório migrations está reservado para mudanças futuras. O seed está desabilitado enquanto não houver dados definidos.

## Divergência em relação à documentação histórica

- Antes: repositório somente documental, sem stack aprovada nem comandos executáveis.
- Agora: Supabase confirmado e scaffold local gerado, com scripts de desenvolvimento.
- Impacto: instalação e inicialização podem ser reproduzidas; contratos e modelo de dados continuam pendentes.
- Resolução: usar o README e este registro como referência de inicialização. Documentos anteriores descrevem planejamento/histórico, não uma API de domínio implementada.

## Organização

- `package.json` / `package-lock.json`: CLI fixada e scripts.
- `supabase/config.toml`: configuração local padrão, PostgreSQL 17 e identificador local `weg-benefits-backend`.
- `supabase/migrations/`: reservado; sem schema de domínio.
- `.gitignore` / `supabase/.gitignore`: excluem dependências, segredos e metadados locais.

Nenhum projeto remoto foi criado ou vinculado. Não há credenciais de produção. O scaffold não precisa de .env para iniciar localmente.

## Revisão conjunta

O mobile está no repositório separado `beneficios-weg/mobile`, branch `chore/mobile-scaffold`: Svelte + TypeScript + Vite + Capacitor, Android gerado. Os projetos ainda não estão integrados.

Futuras alterações devem definir contratos e modelos antes de implementar. Toda tabela exposta deverá ter RLS e policies revisadas; chaves secretas/service_role nunca podem ir ao cliente. Isso é orientação futura, sem implementação nesta base.

## Validações executadas

- `supabase init` com CLI 2.119.0: arquivos oficiais gerados e configuração aceita no start.
- `npm ci`: instalação reproduzível concluída.
- `npm audit`: zero vulnerabilidades.
- `supabase start` / `supabase status`: serviços locais iniciados e status consultado.
- Studio local: HTTP 200.
- PostgreSQL: `select 1` retornou 1; schema public contém zero tabelas.

A análise de logs local (`analytics.enabled`) foi desabilitada: o coletor Vector tentou acessar o Docker pela porta 2375, indisponível nesta máquina. Essa função auxiliar não é necessária ao scaffold. Nenhuma configuração de segurança do Docker foi relaxada.
