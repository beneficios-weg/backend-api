# Plano de implementação — API WEG Benefits

**Estado:** plano de referência; módulos de negócio ainda não estão confirmados como implementados.
**Base conhecida:** Java 21, Spring Boot 4.1.1, Maven, PostgreSQL 17 local, Flyway, Spring Security, Actuator e Springdoc OpenAPI. Revalidar no projeto antes de executar cada etapa.

## 1. Objetivo e fronteiras

Construir uma API REST segura, documentada e como fonte de verdade para:

- o app mobile dos colaboradores, que consulta o catálogo e opera seus próprios favoritos/visitas;
- o cliente desktop administrativo, que cria e atualiza contas e administra o catálogo através da API.

Nenhum cliente acessa o PostgreSQL diretamente. A detecção de entrada/permanência em um estabelecimento acontece no mobile, incluindo o trabalho nativo em segundo plano. O servidor não acompanha o usuário em tempo real e não persiste uma trilha contínua de GPS.

## 2. Arquitetura alvo

Começar como **monólito modular**: uma aplicação Spring Boot, uma implantação e um banco PostgreSQL. Não introduzir microserviços, filas distribuídas ou infraestrutura desnecessária para o MVP.

### Módulos lógicos

| Pacote | Responsabilidade |
|---|---|
| `config` | configuração por ambiente, segurança HTTP, CORS e beans transversais |
| `common` | erros HTTP, validações compartilhadas estritamente necessárias, request ID e logging técnico |
| `identity` | contas, credenciais, papéis, login, tokens e operações administrativas de usuários |
| `catalog` | categorias, marcas/franquias, unidades, benefícios, horários, produtos/serviços, administração e consultas |
| `engagement` | favoritos e visitas associadas à conta autenticada |
| `audit` (a criar quando necessário) | trilha de ações administrativas e retenção/auditoria |

Evitar classes ou subcamadas vazias. Cada módulo deve encapsular seus casos de uso e persistência; outros módulos não devem alterar tabelas diretamente. Controllers recebem DTOs e delegam regras a serviços de aplicação. Entidades JPA não são respostas públicas.

### Persistência e localização

- PostgreSQL é a fonte de verdade; Flyway versiona o esquema.
- IDs devem ser opacos para os clientes. Para usuários, o ID inteiro único é gerado pelo banco. UUID continua como recomendação para outras entidades, sujeito à confirmação antes da primeira migração de negócio.
- Recomenda-se PostGIS para filtro/ordenação por proximidade quando a equipe fechar o ambiente de desenvolvimento e entrega. A decisão deve anteceder o módulo de busca espacial.
- Endereços, coordenadas de estabelecimentos e raio de visita são dados do catálogo; coordenadas de usuários não devem ser guardadas continuamente.
- Arquivar/inativar registros do catálogo em vez de exclusão física quando houver referências históricas.

## 3. Requisitos transversais

- Prefixo versionado `/v1`, HTTPS fora do ambiente local e payloads JSON.
- Autorização verificada no servidor para cada rota; ocultar botões no desktop não protege endpoints.
- Erros uniformes, validação server-side, limites de paginação e busca, request ID e logs sem segredos/PII desnecessária.
- Datas de eventos em UTC/ISO 8601; horários de funcionamento calculados no fuso da unidade.
- Escritas repetíveis após offline devem ser idempotentes quando a operação puder ser enfileirada.
- OpenAPI é publicado como contrato; mudança de comportamento exige atualização do contrato e coordenação com os repositórios clientes.
- Configurações e segredos vêm do ambiente/secret store; credenciais locais de Compose são exclusivas de desenvolvimento.

## 4. Roadmap por módulos

### Etapa 0 — Fundação técnica (relatada como executada; reconfirmar)

**Inclui:** classe principal; configuração PostgreSQL por ambiente; Docker Compose local; Flyway; Actuator com exposição restrita; CORS configurável; request ID; handler comum de erros; OpenAPI; README.

**Antes de avançar:** conferir estado do Git e arquivos reais; iniciar PostgreSQL; iniciar API; conferir health e OpenAPI. O aviso do Flyway sobre zero migrações é esperado até haver a primeira migração. A porta local pode ser configurada para `8081` se `8080` estiver ocupada.

**Concluído quando:** execução local e conexão com banco foram demonstradas e o procedimento está documentado.

### Etapa 1 — Identity: usuários, autenticação e autorização (em andamento)

**Escopo confirmado:** contas criadas exclusivamente por ADMIN, login por username único ou e-mail com senha, e exatamente dois papéis: `ADMIN` e `COLLABORATOR`. ADMIN pode criar outros ADMINs, editar qualquer conta, usar favoritos/visitas próprias, e desativar apenas COLLABORATOR; não pode desativar ADMIN nem a própria conta. Novas contas do painel são ativas e COLLABORATOR por padrão. Não haverá cadastro próprio, redefinição de senha, senha temporária nem troca forçada no primeiro acesso. O primeiro ADMIN será provisionado por CLI, ativo e com papel ADMIN fixo; ID e timestamps são gerados pelo sistema. O ADMIN define a senha comum dos demais usuários. A conta só será persistida depois que a Gmail API aceitar o envio das credenciais; se houver erro/rejeição da API, a conta não será criada e o sistema avisará o ADMIN. A resposta de sucesso da Gmail API conta como aceitação, mesmo que ocorra bounce posterior, que não desfaz a conta.

**Dados do usuário definidos:** ID inteiro gerado pelo banco; username único, 3–75 caracteres ASCII minúsculos `a-z`, dígitos, ponto, hífen ou sublinhado, com remoção de espaços externos e normalização para minúsculo; e-mail obrigatório, único, normalizado e restrito a `gmail.com` ou `estudante.sesisenai.org.br`; senha persistida somente como hash seguro (`senha_hash`), entre 6 e 72 caracteres, aceitando qualquer caractere sem normalização; booleano `Ativo`, true ao criar; timestamps gerados pelo sistema.

**Decisões confirmadas para esta atividade:** sessão server-side persistida no PostgreSQL, cookie persistente e sem expiração automática; encerra por logout, desativação ou revogação. Não será adicionado fluxo CSRF dedicado; o risco de ações originadas por outro site em navegador com sessão ativa foi aceito para o escopo acadêmico e deve ser reconsiderado antes de exposição pública. A allowlist CORS permanece explícita e com credenciais; o mobile nativo não usa CORS de navegador. Envio escolhido: Gmail API com OAuth 2.0 e escopo `gmail.send`; a conta só é persistida quando a API aceita o envio. Falhas síncronas não criam a conta; bounces posteriores não a desfazem. Client secret e refresh token ficam fora do repositório; em Testing, o refresh token expira em sete dias. O POM contém OAuth2 Resource Server, que valida bearer tokens, mas não emite tokens; essa dependência continua sem uso nesta etapa e não equivale a Spring Authorization Server.

ADMIN pode promover COLLABORATOR para ADMIN; não pode rebaixar uma conta ADMIN para COLLABORATOR. COLLABORATOR não pode alterar papéis. O CLI coleta a senha em prompt interativo sem eco, sem senha em argumentos ou logs.

**Entregue nesta etapa parcial:** tabela de usuário, sessões JDBC, login/logout/perfil, rotas administrativas de criação/listagem/detalhe/edição/desativação e CLI interativo do primeiro ADMIN. Senhas são armazenadas com PBKDF2; entrada de senha não é registrada nem devolvida; conta inativa não autentica; desativação e edição revogam sessões. A API responde com erro genérico ao ADMIN se o envio pela Gmail API falhar. O requisito aprovado de enviar senha reutilizável por e-mail continua sendo risco de exposição.

**Rotas implementadas:** `POST /v1/auth/login`, `POST /v1/auth/logout`, `GET /v1/me`; `GET/POST /v1/admin/users`, `GET/PATCH /v1/admin/users/{id}` e `POST /v1/admin/users/{id}/deactivation`. Não existe `/auth/register` público nem refresh token no modelo de sessão.

**Ainda necessário para concluir a etapa:** configurar os segredos OAuth e validar o envio real pela Gmail API; executar fluxo manual com PostgreSQL (CLI, login, autorização, criação e desativação); documentar resultados reais. Nenhum desses itens foi validado neste trabalho.

### Etapa 2 — Catalog: modelo e administração

**Entidades candidatas:** Category, Brand, Establishment, Benefit, OpeningHour, ProductService. Revisar documentos originais e modelo existente antes de consolidar campos, tamanho e obrigatoriedade. Horários precisam suportar dias fechados e faixas que atravessam a meia-noite se o requisito pedir.

**Escopo:** migrações Flyway, constraints, validação, DTOs, consultas administrativas paginadas e rotas de criação/edição/arquivamento protegidas. Operações relacionadas devem ser transacionais. Auditoria mínima registra ator, ação, entidade, data/hora e request ID, sem guardar segredos ou payload sensível completo.

**Concluído quando:** ADMIN administra entidades aprovadas via API; COLLABORATOR recebe 403 em escrita administrativa; arquivamento preserva histórico e integridade referencial.

### Etapa 3 — Catalog: leitura e descoberta mobile

**Escopo:** categorias, marcas, listagem e detalhe de estabelecimento; benefícios, serviços e horários; busca por texto; filtros; paginação/limites; ordenação por nome e distância. `distance`, `isOpen` e `isFavorite` são valores calculados, não campos persistidos duplicados.

**Busca espacial:** após aprovar PostGIS, filtrar/ordenar no banco e definir SRID/unidade, índice e raio. Sem coordenadas, busca textual/categoria continua disponível e distância é ausente. A API não deve depender de serviço externo de mapas para devolver resultados.

**Concluído quando:** mobile pode completar a descoberta do protótipo com dados ativos, dados paginados e respostas documentadas.

### Etapa 4 — Engagement: favoritos, visitas e operação offline

**Escopo:** favoritos próprios, histórico de visitas próprio e gravação de visita confirmada enviada pelo mobile. IDs de proprietário sempre derivam da sessão. Favoritar deve ser idempotente; visitas podem ocorrer em dias diferentes e usam chave de idempotência para reenvio.

**Contrato de visita:** definir campos mínimos, relógio/event timestamp e política para visita duplicada próxima no tempo. Receber apenas visita final e prova mínima aprovada; não receber trajetória GPS completa. O mobile mantém cache e fila offline. A API aceita o reenvio e reporta resultado inequívoco.

**Concluído quando:** conta nunca lê/modifica ações de outra; reenvio não duplica; desativação da conta impede novas gravações autenticadas.

### Etapa 5 — Integração, operação e release

**Escopo:** revisão integral do contrato OpenAPI, CORS por ambiente, TLS/HTTPS no ambiente de entrega, limites/rate limiting, gestão de segredos, logs e auditoria, backups/restauração, métricas/health/readiness, configuração de deploy e dados de demonstração fictícios.

**Concluído quando:** clientes mobile e desktop integram com o contrato publicado; ambiente de demonstração é repetível; backup e restauração têm procedimento; limitações e riscos conhecidos estão registrados.

## 5. Estratégia de validação

Cada etapa deve ter validação proporcional e registrada: compilação, inicialização local, migrações, autorização e exemplos de requisição/resposta. Antes de release, planejar testes unitários para regras, testes de integração com PostgreSQL e testes de autorização/isolamento. Nunca afirmar validação que não foi executada. Dados de teste são fictícios.

## 6. Fora do MVP salvo aprovação posterior

Microserviços, painel de RH/ERP, pagamentos, chat, navegação própria, posicionamento indoor BLE/UWB, trilha GPS contínua, upload de mídia sem provedor definido, avaliações/comentários e analytics comportamental.

## 7. Dependências de sequência

`Fundação → Identity/Admin inicial → Catálogo/migrações → CRUD Admin → Leitura/Discovery → Engagement → Hardening/integração`.

O cliente desktop depende de Identity e das rotas administrativas do Catalog. O mobile depende de Identity, leitura do Catalog e depois Engagement. A interface nativa de geofencing é responsabilidade do projeto mobile; o backend depende apenas do contrato de visita acordado.
