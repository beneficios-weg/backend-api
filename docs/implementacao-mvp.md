# Implementação inicial para revisão técnica

## Escopo confirmado e decisões iniciais

Confirmados no histórico: Svelte/TypeScript/Vite/Capacitor no mobile; Supabase Auth, PostgreSQL e RLS no backend; SQLite local com fila; detecção híbrida por geofences e sessões curtas Kotlin no Android; componentes próprios. iOS/Swift é etapa futura.

O histórico não fechou o modelo de dados, identidade corporativa, parâmetros exatos de visita, contrato de sincronização ou provedor de mapas. Este branch oferece decisões iniciais de MVP para revisão, não afirma que elas foram previamente aprovadas:

- Supabase Auth por e-mail/senha; primeiro acesso e recuperação padrão. Não é SSO WEG nem valida vínculo empregatício.
- Uma categoria por estabelecimento e um benefício principal por estabelecimento.
- Catálogo disponível somente para usuários autenticados; manutenção do catálogo restrita ao administrador.
- Favoritos separados de visitas, por usuário. Tombstones (`is_favorite=false`) preservam remoções. Mudança com timestamp maior vence; relógios mais de cinco minutos no futuro são rejeitados. O limite e o tratamento de relógios incorretos precisam de revisão.
- Visita identificada pelo UUID gerado no aparelho, com resumo mínimo: estabelecimento, horário e permanência. Reenvio com o mesmo ID não duplica nem altera o evento.
- Visitas futuras além de cinco minutos ou anteriores a 30 dias são rejeitadas. Esses limites são decisões iniciais de retenção/envio, não critérios finais de negócio.
- O backend recebe a detecção local; isso não comprova entrada física nem constitui mecanismo antifraude.

## Contrato implementado

Supabase Data API REST (`/rest/v1`) com JWT do usuário. A chave publishable/anon identifica o projeto; não substitui o JWT.

| Recurso | Campos relevantes | Acesso mobile |
|---|---|---|
| `categories` | `id`, `name`, `active` | SELECT de categorias ativas |
| `establishments` | `id`, `category_id`, `name`, `description`, `address`, `latitude`, `longitude`, `active` | SELECT de parceiros ativos em categoria ativa |
| `benefits` | `id`, `establishment_id`, `title`, `description`, `terms`, `valid_until`, `active` | SELECT de benefícios ativos e vigentes |
| `favorites` | `user_id`, `establishment_id`, `is_favorite`, `changed_at` | SELECT apenas do próprio usuário |
| `visits` | `user_id`, `id`, `establishment_id`, `detected_at`, `dwell_ms`, `received_at` | SELECT apenas do próprio usuário |

Mutação de favorito: RPC `set_favorite(p_establishment_id uuid, p_is_favorite boolean, p_changed_at timestamptz)`.

Envio de visita: RPC `record_visit(p_id uuid, p_establishment_id uuid, p_detected_at timestamptz, p_dwell_ms integer)`.

Ambas inferem o usuário de `auth.uid()`, usam `SECURITY INVOKER`, respeitam RLS e não aceitam `user_id` do cliente. Não há endpoints customizados ou Edge Functions.

## Segurança e dados

RLS ativado em todas as tabelas; `anon` não consulta nem grava dados de domínio. Policies incluem propriedade do registro, inclusive `USING` e `WITH CHECK` nas atualizações. Nenhuma policy usa `user_metadata`; nenhuma função usa `SECURITY DEFINER`. Chaves secret/service_role ficam fora do cliente.

Não há catálogo real nem seed automático. Testes criam dados fictícios e removem/revertem esses dados. Publicação remota, integração corporativa e dados reais continuam fora desta execução.

## Instalação local

Node >=22.12, npm e Docker Linux ativo:

```sh
npm ci
npm run supabase:start
npm run db:migrate
npm run db:test
```

O teste SQL roda numa transação com `ROLLBACK`, cobrindo RLS entre dois usuários, restrição anônima, proteção do catálogo, conflitos de favoritos e idempotência de visitas.

Studio: `http://127.0.0.1:54323`. Status/chaves locais: `npm run supabase:status`. Não commitá-las. O Supabase CLI requer Docker para os serviços; não há servidor Node para iniciar.

Redirects locais permitem `http://localhost:5173/`, `http://127.0.0.1:5173/` e `benefits://auth/callback`. E-mails locais vão para Mailpit; homologação/produção requer configuração de SMTP e redirects adequados no projeto remoto.

`npm run supabase:stop` preserva os dados. `db:migrate` aplica migrations pendentes sem resetar banco. Não foi criado/vinculado projeto remoto nem executado `db push` remoto.

## Validações desta entrega

- Migration aplicada somente no ambiente local.
- Testes SQL de acesso e sincronização passaram.
- Fluxos reais mobile web/Supabase testados com usuários temporários locais: login, catálogo, detalhes, favorito offline, reconexão, persistência, remoção, logout e isolamento entre contas.
- Cadastro e recuperação completa com e-mail local/PKCE, atualização de senha e login com a nova senha passaram no navegador. Deep links em aparelho físico e SMTP/SSO de produção seguem pendentes.

Documentos anteriores sobre ausência de stack/schema são registros históricos. Este documento e as migrations descrevem o comportamento implementado neste branch.
