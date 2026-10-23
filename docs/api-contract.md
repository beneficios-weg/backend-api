# Contrato inicial da API — WEG Benefits

**Status:** rotas marcadas como implementadas existem no código; demais rotas permanecem propostas. A especificação OpenAPI gerada pelo código é a referência do comportamento efetivamente publicado. Atualize este documento junto com mudanças de contrato.

## Convenções propostas

- Prefixo `/v1`; JSON UTF-8; HTTPS em ambientes compartilhados.
- Datas/hora ISO 8601 com offset explícito; eventos persistidos em UTC.
- IDs de Catalog são UUIDs (D-042); ID de usuário é inteiro (D-017).

**Modelo Catalog implementado em código; V3 foi aplicada ao banco local em 2026-10-21:** Benefit é cadastrado com um ou mais Discounts na mesma tela e começa inativo. Depois, Establishment é criado com Category e Benefit existentes. Cada Establishment recebe exatamente um Benefit e cada Benefit pode pertencer a no máximo um Establishment; o benefício ativa no primeiro vínculo e permanece ativo/inativo conforme edição manual após isso (D-044 a D-050). A contagem anterior à aplicação confirmou zero linhas em Establishment e Benefit. Em outros bancos, V3 falha se essas tabelas tiverem dados, pois o campo textual antigo não pode ser convertido automaticamente em `value` e `description`.
- Listas com paginação e tamanho máximo fixo no servidor.
- Erro uniforme RFC 9457 Problem Detail com `status`, `title`, `detail` e `requestId`; nunca incluir stack trace, senha, token, hash ou GPS bruto.
- `X-Request-Id` acompanha a requisição/resposta conforme a fundação existente.
- Rotas de negócio exigem autenticação salvo login; health checks e documentação OpenAPI/Swagger são públicos.

## Contas e autenticação — Identity

Rotas implementadas nesta entrega:

- `POST /v1/auth/login` — username ou e-mail e senha; autentica e define cookie de sessão `HttpOnly`.
- `POST /v1/auth/logout` — invalida a sessão e expira o cookie.
- `GET /v1/me` — perfil da conta autenticada.
- `GET /v1/admin/users?page=0&size=20` — lista paginada (máximo 100 por página); ADMIN.
- `POST /v1/admin/users` — ADMIN cria uma conta; não há cadastro próprio (D-006).
- `GET /v1/admin/users/{id}` — detalhes não sensíveis; ADMIN.
- `PATCH /v1/admin/users/{id}` — atualizar campos aprovados; ADMIN.
- `POST /v1/admin/users/{id}/deactivation` — desativar COLLABORATOR; não permite desativar ADMIN nem a própria conta (D-035).

`POST /v1/admin/users` aceita username, email, password e role opcional; omitir role cria COLLABORATOR. SMTP precisa aceitar o envio para concluir a criação. `PATCH /v1/admin/users/{id}` altera username, email e/ou role; password não é editável por esta rota. Atualizações invalidam sessões existentes da conta.

Provisionamento do primeiro ADMIN é operacional, via `FirstAdminProvisioningRunner`; não existe endpoint de bootstrap. Ver comandos no README.

Campos de conta definidos para planejamento: ID inteiro único gerado pelo banco; username único de 3 a 75 caracteres, ASCII minúsculo `a-z`, dígitos, `.`, `-`, `_`, com espaços externos removidos e normalizado para minúsculo; e-mail obrigatório, válido e único, normalizado e limitado aos domínios `gmail.com` e `estudante.sesisenai.org.br`; senha guardada somente como hash, de 6 a 72 caracteres, aceitando qualquer caractere sem normalização; conta ativa; timestamps gerados pelo sistema. Novas contas pelo painel começam como `COLLABORATOR`; o primeiro ADMIN é criado pelo CLI ativo com papel `ADMIN`. Os únicos papéis são `ADMIN` e `COLLABORATOR` (D-018 a D-024).

O ADMIN cadastrará a senha comum da conta; não haverá senha temporária, troca forçada no primeiro acesso nem redefinição de senha (D-007/D-012). Para criar uma conta, a API envia username e senha para o e-mail informado usando SMTP do Gmail, com remetente `wegbenefits@gmail.com` e senha de app (D-021/D-027). A conta só é persistida se Gmail aceitar a mensagem; erro/rejeição SMTP significa que não haverá conta e o ADMIN verá aviso no sistema. Gmail aceitar a mensagem conta como sucesso; um bounce posterior não desfaz a conta (D-026). A senha de app exige verificação em duas etapas e deve ser guardada fora do repositório. O primeiro ADMIN será provisionado por CLI: o operador informa username, e-mail e senha; ID e timestamps são gerados pelo sistema, conta é ativa e papel é ADMIN (D-019). ADMIN pode editar usuários e desativar apenas contas COLLABORATOR; é vedado desativar qualquer ADMIN ou a própria conta. A API nunca deve devolver senha em resposta nem registrá-la em logs. Enviar senha reutilizável por e-mail traz risco de exposição.

## Leitura de catálogo — Catalog

Implementado na etapa Catalog:

- `GET /v1/categories`
- `GET /v1/services` (opções ativas usadas no catálogo)
- `GET /v1/establishments?query=&categoryId=&serviceId=&page=&size=`
- `GET /v1/establishments/{id}`
- `GET /v1/establishments/{id}/benefits` se não estiverem incluídos no detalhe

O catálogo requer sessão autenticada. Listas aceitam `page` e `size`; consultas administrativas têm máximo de 100 itens por página e a busca pública de estabelecimentos limita a 50. `GET /v1/categories/options` retorna a paleta de cores permitida e indica Iconify como fonte de busca de ícones.

Estabelecimentos, categorias, benefícios e serviços inativos não aparecem nas consultas normais. Benefícios são retornados sem nome/título e vinculados a um único estabelecimento. Ícones de categoria são identificadores Iconify; o cliente mobile baixa somente ícones usados ainda ausentes durante sincronização online e mantém cópia local para uso offline. A URL HTTPS opcional da fachada pode ser armazenada em cache pelo cliente. Não há horários, rota de marcas ou consulta de proximidade nesta versão. Busca espacial depende da decisão sobre PostGIS (P-006).

## Administração de catálogo — Catalog/Admin

Implementado para ADMIN, com paginação, criação, edição parcial e desativação lógica:

Proposta de famílias de rotas protegidas pelo papel ADMIN:

- `/v1/admin/categories`
- `/v1/admin/services`
- `/v1/admin/establishments`
- `/v1/admin/benefits`

As rotas administrativas incluem `GET` paginado e por ID, `POST`, `PATCH /{id}` e `POST /{id}/deactivation` em cada família. As rotas administrativas exigem papel ADMIN; as de leitura exigem sessão autenticada.

Na implementação atual, cada família tem consulta/listagem, criação, edição parcial e desativação lógica protegidas por ADMIN. A desativação preserva registros e relações e oculta o item das consultas normais; não há exclusão física. Category armazena nome, chave Iconify e uma das oito cores predefinidas (`#1B5E20`, `#0D47A1`, `#4A148C`, `#B71C1C`, `#E65100`, `#006064`, `#4E342E`, `#37474F`). Establishment aceita URL HTTPS opcional de fachada. O DTO de Benefit não tem mais o texto `discount`: exige `discounts` não vazia com `value` (2–15) e `description` (10–300), ambos obrigatórios. `BenefitResponse.establishmentId` é `null` até o vínculo; `EstablishmentResponse.benefitId` identifica o benefício exclusivo.

Exemplo do fluxo administrativo:

1. `POST /v1/admin/benefits` recebe `audience`, `participationInstructions`, `contact`, `conditions` e `discounts: [{"value":"Até 50%","description":"em produtos selecionados"}]`; responde com benefício inativo e `establishmentId: null`.
2. `POST /v1/admin/establishments` exige também `categoryId` e `benefitId`; ao concluir, associa um único benefício ao estabelecimento e ativa o benefício se for seu primeiro vínculo.
3. `PATCH /v1/admin/benefits/{id}` pode substituir a lista inteira de `discounts`; quando o campo é enviado, deve conter pelo menos um item. Omiti-lo mantém a lista atual.
4. `PATCH /v1/admin/establishments/{id}` aceita `benefitId` para associação ou substituição; o benefício escolhido não pode pertencer a outro estabelecimento.

`V3__benefit_discounts_and_exclusive_establishment.sql` muda o esquema sem editar V2. Antes de executá-la, verificar se existem linhas em `establishment` ou `benefit`; se houver, a migração interrompe com orientação para mapear os dados. Não dividir o campo legado `discount` por suposição.

## Ações pessoais — Engagement

- `GET /v1/me/favorites`
- `PUT /v1/me/favorites/{establishmentId}` — adicionar de forma idempotente
- `DELETE /v1/me/favorites/{establishmentId}`
- `GET /v1/me/visits` — histórico paginado da conta autenticada
- `POST /v1/me/visits` — visita final confirmada, com chave de idempotência aprovada

O servidor deriva o proprietário da sessão; nenhum endpoint aceita `userId` do cliente. Visitas repetidas em dias diferentes devem ser permitidas. Não enviar nem armazenar trilhas contínuas de GPS.

## Autorização proposta

| Papel | Acesso |
|---|---|
| `COLLABORATOR` | leitura do catálogo; próprio perfil, favoritos e visitas |
| `ADMIN` | administração de contas e catálogo; leitura do catálogo; edição de qualquer conta; criação de ADMINs; desativa apenas COLLABORATOR; usa seus próprios favoritos e visitas |

Há exatamente esses dois papéis. ADMIN também pode executar suas próprias ações pessoais de favoritos e visitas.

Autenticação planejada por sessão server-side persistida no PostgreSQL e cookie persistente `HttpOnly`, sem expiração automática; logout invalida a sessão (D-025/D-031/D-034). A atividade aceitou não criar fluxo CSRF dedicado (D-032); isso deixa risco de ações originadas por outro site em navegador com sessão ativa e deve ser revisto antes de exposição pública. Sessões de conta desativada devem ser invalidadas ou bloqueadas.

**CORS:** a fundação já tem allowlist configurável. Não será adicionada configuração extra para esta atividade; confirmar se o desktop será navegador em origem diferente da API (P-028). Nesse caso, manter origens explícitas com credenciais e sem wildcard. CORS é aplicado por navegadores e não substitui autenticação/autorização nem se aplica ao cliente mobile nativo.

Os domínios `gmail.com` e `estudante.sesisenai.org.br` são os únicos destinatários permitidos. O remetente escolhido é `wegbenefits@gmail.com`; o envio é por SMTP autenticado do Gmail com senha de app. Em falha/rejeição SMTP, não persistir a conta, mostrar aviso e permitir que ADMIN tente novamente após corrigir a causa. Não guardar senha de usuário em texto puro.

ADMIN pode criar outros ADMINs, editar qualquer conta e promover COLLABORATOR para ADMIN. Não pode rebaixar uma conta ADMIN para COLLABORATOR. COLLABORATOR não pode alterar papéis. ADMIN usa suas próprias ações de favoritos e visitas, e não pode desativar nenhuma conta ADMIN nem a própria conta. O CLI recebe username e e-mail em entrada interativa comum e solicita a senha sem eco no terminal, sem incluí-la em argumentos ou logs.

## Status HTTP propostos

`200` leitura/alteração com resposta; `201` criação; `204` remoção/ação sem corpo; `400` formato/validação; `401` sem sessão válida; `403` papel ou propriedade insuficiente; `404` não encontrado/indisponível; `409` conflito/idempotência; `422` regra de domínio; `429` limite excedido; `5xx` erro genérico sem detalhe interno.
