# Registro de decisões — API WEG Benefits

Atualize este arquivo ao confirmar decisões. Uma recomendação não vira decisão até ser aprovada pela equipe. Não preencha pendências por inferência.

## Decisões confirmadas

| ID | Decisão | Estado |
|---|---|---|
| D-001 | API em Java/Spring Boot; projeto usa Java 21 e Spring Boot 4.1.1 | Confirmada pelo projeto/relato; revalidar POM |
| D-002 | A API é a fonte de verdade; mobile e desktop usam API, sem acesso direto ao banco | Confirmada |
| D-003 | Back-end em repositório separado do app mobile | Confirmada |
| D-004 | Desktop administra catálogo por endpoints protegidos da API | Confirmada |
| D-005 | Mobile não faz CRUD administrativo do catálogo | Confirmada |
| D-006 | Somente ADMIN pode criar contas; não haverá cadastro próprio | Confirmada pelo usuário |
| D-007 | Não haverá redefinição de senha, senha temporária nem troca forçada no primeiro acesso | Confirmada pelo usuário |
| D-008 | Visitas em background são detectadas pelo cliente mobile/nativo; servidor recebe a visita final, sem trilha GPS contínua | Confirmada como fronteira arquitetural |
| D-009 | Favoritos e visitas são operações pessoais previstas; nenhuma conta acessa ações de outra | Confirmada no escopo do produto; validar restrição em `AGENTS.md` e contrato |
| D-010 | Aplicação inicia localmente em `8081` quando `8080` está ocupada; PostgreSQL local em `localhost:5432` | Confirmada como configuração de desenvolvimento, não obrigação de produção |
| D-011 | Provisionar o primeiro ADMIN por script CLI, sem endpoint de bootstrap exposto | Confirmada pelo usuário; detalhes de segurança e operação do script devem ser definidos antes da implementação |
| D-012 | A senha cadastrada pelo ADMIN para cada usuário é a senha comum da conta, não uma senha temporária | Confirmada pelo usuário |
| D-013 | Após o ADMIN criar a conta, o sistema enviará automaticamente as credenciais (usuário e senha) ao endereço de e-mail cadastrado para o usuário | Confirmada pelo usuário |
| D-014 | Login aceita nome de usuário único ou e-mail, sempre com senha | Confirmada pelo usuário; regras de normalização registradas em D-020 e D-022 |
| D-015 | Se o envio das credenciais falhar, o ADMIN receberá um aviso dentro do sistema | Confirmada pelo usuário |
| D-016 | É permitido usar um serviço externo gratuito para envio de e-mail; não é necessário ter provedor próprio | Confirmada como permissão geral; para esta aplicação foi escolhido SMTP da própria conta Gmail (D-027) |
| D-017 | Campos de usuário: ID inteiro único; username único de 3 a 75 caracteres; e-mail obrigatório, válido e único; senha armazenada somente como hash seguro; `Ativo` booleano; datas de criação e atualização | Confirmada; detalhes estão refinados em D-020, D-022 a D-024 |
| D-018 | Os únicos papéis são `ADMIN` e `COLLABORATOR`. ADMIN pode criar outros ADMINs, editar qualquer conta (inclusive a própria e outras ADMIN), usar favoritos/visitas próprios e desativar apenas COLLABORATOR; nunca desativa ADMIN nem a própria conta | Confirmada pelo usuário; mudança de papel detalhada em D-028 |
| D-019 | O CLI de provisionamento inicial cria o primeiro ADMIN ativo, com papel fixo ADMIN; recebe username, e-mail e senha, gera o ID e timestamps automaticamente e envia login/senha ao e-mail informado | Confirmada pelo usuário; envio SMTP Gmail com senha de app (D-027); senha solicitada em prompt sem eco e fora de argumentos/logs (D-030) |
| D-020 | Os únicos domínios aceitos para e-mail de usuário são `gmail.com` e `estudante.sesisenai.org.br` | Confirmada pelo usuário; armazenar e comparar endereço normalizado |
| D-021 | O endereço remetente das mensagens será `wegbenefits@gmail.com` | Confirmada pelo usuário; integração/autenticação com Google ainda precisa ser escolhida e configurada fora do código-fonte |
| D-022 | Username aceita somente ASCII minúsculo `a-z`, dígitos e `.`, `-`, `_`, entre 3 e 75 caracteres; remover espaços externos e normalizar para minúsculo antes da unicidade | Confirmada pelo usuário |
| D-023 | ID de usuário é inteiro gerado pelo banco; timestamps de criação/atualização são gerados pelo sistema; novas contas criadas no painel são ativas e têm papel padrão COLLABORATOR | Confirmada pelo usuário; a criação de ADMIN por outro ADMIN continua permitida conforme D-018 |
| D-024 | A senha terá de 6 a 72 caracteres, aceitando qualquer caractere sem normalização | Confirmada pelo usuário. Seis caracteres é um mínimo fraco para conta comum e deve ser reavaliado se o escopo permitir |
| D-025 | Autenticação usará sessão mantida no servidor, persistida no PostgreSQL, com cookie `HttpOnly` persistente e sem expiração automática; logout invalida sessão | Confirmada pelo usuário; desativação/revogação também encerram acesso |
| D-026 | A conta só será criada se o Gmail aceitar o envio SMTP das credenciais; uma rejeição/erro significa que não haverá conta e o ADMIN será avisado. A aceitação SMTP conta como sucesso, mesmo que ocorra bounce posterior | Confirmada pelo usuário; um bounce posterior não desfaz a conta |
| D-027 | Envio de e-mail será por SMTP do Gmail usando senha de app da conta `wegbenefits@gmail.com` | Confirmada pelo usuário; exige verificação em duas etapas e armazenamento do segredo fora do código/repositório |
| D-028 | ADMIN pode promover uma conta COLLABORATOR para ADMIN; não pode rebaixar uma conta ADMIN. COLLABORATOR não altera papéis | Confirmada pelo usuário |
| D-029 | O limite máximo de senha será 72 caracteres | Confirmada pelo usuário; mínimo de 6 já consta em D-024 |
| D-030 | O CLI coleta a senha por prompt interativo sem eco; senha não deve ser passada como argumento nem registrada em logs | Confirmada pelo usuário como abordagem aceitável |
| D-031 | A sessão não terá expiração automática por inatividade nem limite máximo; encerra por logout, desativação/revogação ou perda do armazenamento | Confirmada pelo usuário para a atividade; risco de sessão esquecida ou exposta aceito |
| D-032 | Não será criado fluxo CSRF dedicado nesta atividade | Confirmada pelo usuário para o escopo acadêmico; risco de ações originadas por outro site em navegador com sessão ativa deve ser reavaliado antes de exposição pública |
| D-033 | Não será adicionada configuração CORS além da allowlist já existente, salvo necessidade do cliente web | Confirmada como preferência atual; CORS não se aplica a cliente mobile nativo e só será necessário para navegador em origem diferente |
| D-034 | Para manter sessão após fechar o cliente ou reiniciar a API, persistir a sessão no PostgreSQL e manter cookie persistente sem prazo automático até logout/revogação | Proposta aceita pelo usuário |
| D-035 | A rota para desativação de usuário será `POST /v1/admin/users/{id}/deactivation` | Confirmada pelo usuário em 2026-10-19 |
| D-036 | Benefício não terá nome/título próprio; cada benefício pertence a exatamente um estabelecimento, e um estabelecimento pode ter vários benefícios | Parcialmente supersedida por D-044: permanece sem nome/título e pertence a um estabelecimento, mas a cardinalidade agora é 1:1 |
| D-037 | Horários de funcionamento ficam fora da primeira versão da API: não serão armazenados, consultados nem retornados | Confirmada pelo usuário |
| D-038 | A fachada pode ter URL HTTPS opcional informada pelo ADMIN; a API não fará upload nem armazenará o arquivo de imagem | Proposta aceita pelo usuário; o cliente deve baixar e manter em cache imagens necessárias para uso offline |
| D-039 | Itens do catálogo serão desativados sem exclusão física; registros e relações históricas serão preservados, e itens inativos não aparecem nas consultas normais do catálogo | Proposta aceita pelo usuário; regras de sincronização offline de visitas ficam para Engagement |
| D-040 | Categorias usam cor de uma paleta definida pelo projeto e um identificador de ícone escolhido no Iconify. O mobile baixa apenas ícones selecionados ainda ausentes durante a sincronização online e os mantém em cache para uso offline | Confirmada pelo usuário; Iconify não é fonte disponível durante uso offline antes do primeiro download do ícone |
| D-041 | Todos os campos de Benefit — público, instruções de participação, contato, desconto, condições e estabelecimento — são obrigatórios | Confirmada originalmente pelo usuário; após D-045, desconto é uma entidade separada e cada Benefit deve ter pelo menos um Discount |
| D-042 | Category, Service, Establishment e Benefit usarão identificadores UUID | Confirmada pelo usuário em 2026-10-20; IDs de usuários permanecem inteiros conforme D-017 |
| D-043 | A revisão da modelagem mantém UUID para Category, Service, Establishment e Benefit; a nova entidade Discount seguirá UUID conforme o padrão do catálogo | Confirmada pelo usuário em 2026-10-20 |
| D-044 | Cada Establishment deverá referenciar exatamente um Benefit, substituindo a cardinalidade anterior de D-036 | Confirmada pelo usuário em 2026-10-20; a cardinalidade inversa (um Benefit ligado a quantos Establishments) aguarda P-026 |
| D-045 | Discount será entidade própria 1:N por Benefit; cada Benefit terá pelo menos um Discount. `value` terá 2–15 caracteres e `description` 10–300 | Confirmada pelo usuário em 2026-10-20; a migração deve preservar os descontos legados |
| D-046 | Permanecem as decisões existentes de Iconify (D-040), username (D-022) e Estado como sigla UF; os trechos divergentes do PDF não as substituem | Confirmada pelo usuário em 2026-10-20 |
| D-047 | `Discount.value` e `Discount.description` são obrigatórios; limites de 2–15 e 10–300 caracteres, respectivamente | Confirmada pelo usuário em 2026-10-20 |
| D-048 | Benefício e descontos são criados/editados na mesma tela administrativa antes da associação. Establishment só pode ser criado quando Category e Benefit com Discount existirem | Confirmada pelo usuário em 2026-10-20; o protótipo recebido em 2026-10-20 orienta o fluxo de telas |
| D-049 | Benefit começa inativo e ativa quando associado pela primeira vez a um Establishment; uma desativação manual permanece mesmo com estabelecimentos vinculados | Confirmada pelo texto visível no protótipo enviado em 2026-10-20 |

## Pendências que bloqueiam ou afetam etapas

| ID | Pergunta | Recomendação atual | Resolver antes de |
|---|---|---|---|
| P-028 | O cliente desktop será navegador em origem diferente da API? | Preferir mesma origem ou cliente nativo; se for cross-origin no navegador, usar allowlist CORS explícita com credenciais. A fundação já configura CORS por allowlist | Integração |
| P-006 | PostGIS será habilitado no banco local e no ambiente de entrega? | Recomendado para proximidade; definir imagem/extensão, migração, índice e custo operacional | Discovery |
| P-007 | Fechar limites máximos de `publico` e `contact` em Benefit | Obrigatoriedade está fechada (D-041); proposta atual é não impor limite de domínio, persistindo como texto PostgreSQL. Limites de Discount foram definidos em D-045 | Revisão do modelo; não bloqueia Catalog com campos textuais |
| P-008 | Brand faz parte do MVP? Quais campos teria? | Não incluir Brand sem necessidade aprovada; o documento revisado inclui Service | Pode ser adiado; não bloqueia Catalog sem Brand |
| P-009 | Como representar horários especiais, feriados, intervalos e virada de dia? | Horários estão fora da primeira versão da API (D-037) | Não aplicável nesta etapa; reabrir se horários forem aprovados |
| P-010 | Como arquivar unidade com favoritos/visitas existentes? | Desativação lógica, preservando linhas e relações; ocultar inativos das consultas comuns (D-039) | Fechado para Catalog; sincronização de visitas offline depende de P-011 |
| P-011 | Qual regra exata confirma visita e trata duplicidade? | Evento final do mobile com chave idempotente; definir janela e evidência mínima sem histórico GPS | Engagement |
| P-012 | Dados disponíveis offline por quanto tempo e o que ocorre após expiração/desativação de conta? | Cache de leitura com prazo; fila de escrita identificável e sincronizada após autenticação | Mobile/Engagement |
| P-013 | Fotos serão URLs pré-cadastradas ou upload com storage? | URL HTTPS opcional, sem upload nesta versão (D-038) | Fechado para Catalog |
| P-014 | Avaliações 1–5 entram no MVP? | Manter fora até aprovação explícita | Catalog/Engagement |
| P-015 | Onde a API será hospedada e como backups/restauração serão operados? | Decidir antes da entrega compartilhada | Release |
| P-016 | Qual o limite real de horas/equipe dedicado à API? | Plano original citava 30 horas para o projeto inteiro; repartir com mobile e desktop | Planejamento |
| P-017 | IDs numéricos substituem UUID para as entidades Catalog existentes? | Resolvido por D-043: os IDs atuais continuam UUID; nenhum ID de rota muda | Fechado |
| P-018 | Cada Establishment terá exatamente um Benefit? | Resolvido por D-044; falta escolher se a associação é obrigatória já na criação do estabelecimento (P-023) | Fechado para a cardinalidade |
| P-019 | Discount será separado e cada Benefit terá pelo menos um? | Resolvido por D-045; a preservação do legado segue em P-025 | Fechado para o modelo lógico |
| P-020 | Os ícones deixarão de usar Iconify? | Resolvido por D-046: manter a abordagem Iconify aprovada anteriormente | Fechado |
| P-021 | Username aceitará somente letras minúsculas? | Resolvido por D-046: manter a regra D-022 | Fechado |
| P-022 | Estado aceitará nomes com 2–75 caracteres? | Resolvido por D-046: manter somente sigla UF | Fechado |
| P-023 | Como representar pela API o cadastro de Benefit/Discount na tela própria e depois a associação de Category/Benefit durante a criação de Establishment? | A tela cria Benefit inativo com um ou mais Discounts; Establishment referencia Category e Benefit existentes. Atualizar contrato após resolver a cardinalidade P-026 | Antes de atualizar rotas/DTOs de Catalog |
| P-024 | `Discount.value` e `Discount.description` são ambos obrigatórios? | Resolvido por D-047 | Fechado |
| P-025 | Existem registros Catalog/Benefit no banco local que precisam ser preservados? | Inspecionar contagens; não descartar benefícios múltiplos nem inventar divisão do texto legado em valor/descrição | Antes da migração V3 |
| P-026 | Um Benefit pode ser associado a vários Establishments? | O protótipo mostra “Estabelecimentos vinculados” no plural e benefício ativado na primeira associação, sugerindo sim; confirmar antes de escolher `UNIQUE` ou relacionamento inverso | Antes da migração V3 |

## Registro de alteração

| Data | Mudança | Aprovado por |
|---|---|---|
| 2026-10-16 | Documento de referência inicial preparado a partir do contexto e dos relatos do projeto | Equipe deve revisar e aprovar |
| 2026-10-16 | Primeiro ADMIN será provisionado por script CLI; ADMIN cadastra a senha comum de cada usuário, sem senha temporária | Usuário |
| 2026-10-16 | Somente ADMIN cria usuários, não há redefinição de senha e as credenciais serão entregues por e-mail | Usuário |
| 2026-10-16 | Login aceita nome de usuário único ou e-mail com senha; o envio das credenciais por e-mail será automático após o cadastro administrativo | Usuário |
| 2026-10-16 | Enviar para o e-mail cadastrado pelo ADMIN para o usuário; notificar o ADMIN se o envio falhar | Usuário |
| 2026-10-16 | Pode ser usado um serviço externo gratuito; aviso de falha será apresentado no sistema ao ADMIN | Usuário |
| 2026-10-16 | Esquema funcional dos campos de usuário informado: ID inteiro, usuário único com formato/tamanho definidos, e-mail obrigatório/único, hash de senha, estado ativo e timestamps | Usuário |
| 2026-10-16 | Papéis limitados a ADMIN e COLLABORATOR; ADMIN edita usuários e desativa somente colaboradores, nunca ADMIN ou a própria conta | Usuário |
| 2026-10-16 | CLI inicial recebe os atributos do primeiro ADMIN e envia login e senha ao e-mail indicado | Usuário |
| 2026-10-16 | Endereços de e-mail dos usuários poderão usar `gmail.com` ou `estudante.sesisenai.org.br` | Usuário |
| 2026-10-16 | Endereço remetente das mensagens será `wegbenefits@gmail.com`; usar envio autenticado pela própria conta Google, sem serviço terceirizado fingindo ser Gmail | Usuário |
| 2026-10-16 | ADMIN pode criar ADMINs, editar sua própria conta e outras contas ADMIN e usar favoritos/visitas; novos usuários são ativos COLLABORATOR por padrão; e-mails ficam limitados aos dois domínios declarados | Usuário |
| 2026-10-16 | Username ASCII minúsculo com normalização; senha com mínimo de 6; ID e timestamps gerados pelo sistema; CLI usa os defaults de primeiro ADMIN; autenticação por sessão server-side/cookie | Usuário |
| 2026-10-16 | Não criar a conta quando o serviço de e-mail rejeitar/falhar no envio; criar somente após o envio funcionar | Usuário |
| 2026-10-19 | SMTP do Gmail com senha de app; aceitação SMTP conta como sucesso, ainda que haja bounce posterior | Usuário |
| 2026-10-19 | Senha de 6 a 72 caracteres; ADMIN pode promover COLLABORATOR, mas não rebaixar ADMIN; CLI solicita senha sem eco | Usuário |
| 2026-10-19 | Preferência por sessão longa; propostas de 30 dias inativa/90 dias máximo e de CSRF/CORS registradas para avaliação, ainda não aprovadas | Usuário (preferência); proposta do assistente |
| 2026-10-19 | Sessão sem expiração automática e persistida no PostgreSQL; senha aceita qualquer caractere; sem fluxo CSRF dedicado nesta atividade; CORS adicional apenas se o desktop web for cross-origin | Usuário |
| 2026-10-20 | Modelagem revisada: benefício sem nome/título, relação estabelecimento 1:N benefício, sem horários, imagem de fachada opcional por URL e itens inativos preservados | Usuário |
| 2026-10-20 | Ícones de categoria serão selecionados no Iconify; mobile sincroniza e armazena em cache somente os ícones usados para suportar uso offline | Usuário |
| 2026-10-20 | Todos os campos de Benefit são obrigatórios; limites para público, contato e desconto não foram especificados e continuam abertos | Usuário |
| 2026-10-20 | IDs de Category, Service, Establishment e Benefit serão UUID | Usuário |
| 2026-10-20 | UUIDs existentes mantidos; Establishment terá exatamente um Benefit e Benefit terá um ou mais Discounts separados; Iconify, username atual e sigla UF permanecem | Usuário |
| 2026-10-20 | Value e description de Discount são obrigatórios; benefício e descontos ficam na mesma tela; Establishment exige Category, Benefit e Discount | Usuário |

## Pontos de implementação pendentes

- As decisões de produto Identity estão fechadas para esta atividade, inclusive a rota D-035. Antes de exposição pública, reavaliar sessão sem expiração, ausência de fluxo CSRF dedicado e envio de senha reutilizável por e-mail. SMTP exige verificação em duas etapas e segredo fora do código/repositório. Confirmar a origem/tipo do cliente desktop antes da integração cross-origin; origens devem continuar explícitas e sem wildcard.

## Divergências documentais preservadas

- Os documentos herdados do `origin/main` (29/09/2026) registram que stack, código, migrações, endpoints e autenticação ainda não existiam ou estavam pendentes. O estado atual do repositório confirma Java 21, Spring Boot 4.1.1, PostgreSQL, Flyway e uma entrega parcial de Identity. Os documentos antigos foram preservados como snapshots; para comportamento atual, usar este registro, `docs/api-contract.md` e `docs/implementation-log.md`.
- `docs/visao-geral.md` e `docs/autenticacao.md` refletem necessidades iniciais de primeiro acesso/recuperação. A decisão atual aprovada é sem cadastro próprio, senha temporária ou redefinição; somente ADMIN cria contas e entrega credenciais por e-mail. Não transformar a necessidade mobile antiga em endpoint sem nova aprovação.
