# Diário de implementação — API WEG Benefits

Este arquivo é atualizado ao terminar cada etapa, com base no que foi realmente alterado e executado. O plano não é evidência de implementação.

## Estado de entrada registrado em 2026-10-16

- Projeto criado pelo Spring Initializr; classe principal existente em `com.weg.benefits`.
- Relato de implementação de fundação: configuração de PostgreSQL/Flyway, Actuator, CORS, request ID, handler de erros, Docker Compose local e Springdoc.
- POM relatado com Java 21, Spring Boot 4.1.1, PostgreSQL, JPA, Flyway, Spring Security e OAuth2 Resource Server; confirmar conteúdo atual.
- Maven Wrapper e JDK Microsoft 21.0.12.1 funcionaram no PowerShell.
- PostgreSQL 17.11 conectou à API em `localhost:5432`; banco `benefits` iniciou vazio e Flyway informou zero migrações.
- Spring Boot iniciou em `localhost:8081` porque a porta 8080 estava ocupada pelo Apache Tomcat local.
- Não há funcionalidades de negócio confirmadas como concluídas.

## Entradas por módulo

### Planejamento de Identity — 2026-10-16

- **Objetivo e estado:** Registrar definições do usuário para papéis, administração de contas e provisionamento inicial; nenhuma funcionalidade foi implementada.
- **Arquivos/migrações alterados:** `AGENTS.md`, `README.md`, `docs/README.md`, `docs/decision-register.md`, `docs/implementation-plan.md` e `docs/api-contract.md`; nenhum código ou migração alterado.
- **Rotas/contrato:** Sem alteração de rotas implementadas. O contrato proposto agora documenta apenas ADMIN/COLLABORATOR, limites para desativação e CLI inicial.
- **Decisões tomadas e pendentes:** Confirmados papéis e regras de desativação, CLI recebe atributos e envia login/senha por e-mail. Modelo de autenticação e serviço de e-mail seguem como recomendações pendentes; registrada preocupação com envio de senha reutilizável por e-mail.
- **Validações executadas e resultados:** Leitura dos documentos e POM; não foram executados testes nem alterado código.
- **Problemas/riscos restantes:** Definir o modelo de autenticação, serviço/configuração de e-mail, detalhes seguros do CLI e quais atributos são entradas versus valores gerados/defaults, se ADMIN executa ações pessoais e demais pendências já listadas.
- **Próxima dependência/módulo:** Fechar decisões de Identity antes da migração e implementação.

### Planejamento de domínio de e-mail — 2026-10-16

- **Objetivo e estado:** Registrar que os endereços dos usuários poderão usar `@gmail.com` ou `@estudante.sesisenai.org.br` e distinguir destinatário do remetente do serviço.
- **Arquivos/migrações alterados:** `AGENTS.md`, `README.md`, `docs/decision-register.md` e `docs/api-contract.md`; nenhum código ou migração alterado.
- **Rotas/contrato:** Sem alteração.
- **Decisões tomadas e pendentes:** Domínios de destinatários registrados; ainda falta confirmar se a lista é exclusiva e quem pode verificar/configurar o domínio remetente.
- **Validações executadas e resultados:** Nenhum teste ou validação de aplicação executado.
- **Problemas/riscos restantes:** O serviço de envio precisa de um remetente autorizado; `gmail.com` como endereço de usuário não confere autorização para enviar como `@gmail.com`.
- **Próxima dependência/módulo:** Confirmar domínio remetente e provedor antes de implementar envio.

### Planejamento do remetente — 2026-10-16

- **Objetivo e estado:** Registrar `wegbenefits@gmail.com` como remetente pretendido e ajustar a recomendação de provedor para envio autenticado via Google.
- **Arquivos/migrações alterados:** `AGENTS.md`, `README.md`, `docs/decision-register.md` e `docs/api-contract.md`; nenhum código ou migração alterado.
- **Rotas/contrato:** Sem alteração.
- **Decisões tomadas e pendentes:** Endereço From definido; ainda escolher Gmail API/OAuth ou SMTP com senha de app, sujeito às opções habilitadas na conta.
- **Validações executadas e resultados:** Nenhum teste ou validação de aplicação executado.
- **Problemas/riscos restantes:** Senha de app é menos recomendada e exige verificação em duas etapas; usar OAuth é preferível quando viável. O requisito de enviar senha de usuário por e-mail também permanece como risco a revisar.
- **Próxima dependência/módulo:** Escolher a integração Google e configurar segredo fora do código-fonte antes de implementar envio.

### Planejamento de Identity — 2026-10-16 (decisões adicionais)

- **Objetivo e estado:** Registrar as definições do usuário para permissões, campos da conta, username, senha mínima e autenticação por sessão; nenhuma funcionalidade foi implementada.
- **Arquivos/migrações alterados:** `AGENTS.md`, `docs/decision-register.md`, `docs/api-contract.md` e `docs/implementation-plan.md`; nenhum código ou migração alterado.
- **Rotas/contrato:** Sem rotas implementadas. Contrato proposto atualizado com privilégios de ADMIN e regra de sessão.
- **Decisões tomadas e pendentes:** Confirmados ADMIN criando outros ADMINs, editando qualquer conta inclusive a própria, usando suas próprias ações pessoais, domínios de e-mail restritos, ID/timestamps/defaults automáticos, username ASCII minúsculo normalizado, senha mínima 6 e sessão server-side. Pendentes método Google, expiração de sessão, tamanho máximo de senha, mudança de papel e recuperação segura de falha de envio.
- **Validações executadas e resultados:** Leitura dos documentos; nenhum teste executado e nenhum código alterado.
- **Problemas/riscos restantes:** O mínimo de 6 caracteres é fraco; envio de senha reutilizável por e-mail permanece um risco; não se pode retentar entrega guardando a senha em texto puro.
- **Próxima dependência/módulo:** Escolher método de envio autenticado pelo Gmail e fechar pendências residuais de Identity.

### Planejamento de envio antes da criação da conta — 2026-10-16

- **Objetivo e estado:** Registrar que a conta só deve ser criada após o Gmail aceitar o envio das credenciais.
- **Arquivos/migrações alterados:** `AGENTS.md`, `README.md`, `docs/decision-register.md`, `docs/api-contract.md`, `docs/implementation-plan.md` e `docs/README.md`; nenhum código ou migração alterado.
- **Rotas/contrato:** O contrato planejado prevê falha de envio sem persistir a conta e aviso ao ADMIN; sem rota implementada.
- **Decisões tomadas e pendentes:** Falha imediata do envio significa conta não criada. Ainda definir se sucesso é aceitação do Gmail ou confirmação posterior de entrega/bounce.
- **Validações executadas e resultados:** Nenhum teste ou validação de aplicação executado.
- **Problemas/riscos restantes:** Envio aceito não garante entrega na caixa de entrada; há uma janela de falha entre aceitação externa e persistência no banco. A integração precisa tratar esse caso sem guardar senha em texto puro.
- **Próxima dependência/módulo:** Escolher autenticação Gmail e definir semântica de entrega antes da implementação.

### Planejamento de envio SMTP Gmail — 2026-10-19

- **Objetivo e estado:** Registrar a escolha de SMTP com senha de app e a aceitação pelo Gmail como critério síncrono de sucesso do envio.
- **Arquivos/migrações alterados:** `AGENTS.md`, `README.md`, `docs/README.md`, `docs/decision-register.md`, `docs/implementation-plan.md` e `docs/api-contract.md`; nenhum código ou migração alterado.
- **Rotas/contrato:** Sem rotas implementadas; o contrato prevê não persistir conta se SMTP falhar e avisar ADMIN.
- **Decisões tomadas e pendentes:** Gmail aceitar SMTP significa sucesso; bounce posterior não reverte a conta. Senha de app escolhida, exige 2FA e será segredo externo ao repositório.
- **Validações executadas e resultados:** Documentação e POM lidos conforme instruções; nenhum teste executado.
- **Problemas/riscos restantes:** Senha reutilizável ainda é enviada por e-mail; permanecem pendentes expiração de sessão, tamanho máximo da senha, alteração de papel e coleta segura do CLI.
- **Próxima dependência/módulo:** Fechar pendências residuais e, antes de implementar, validar acesso 2FA à conta remetente e configurar o segredo fora do código.

### Planejamento de Identity — 2026-10-19 (decisões adicionais)

- **Objetivo e estado:** Registrar limite máximo da senha, transição de papéis e coleta da senha no CLI; esclarecer relação entre expiração da sessão e localização mobile. Nenhuma funcionalidade foi implementada.
- **Arquivos/migrações alterados:** `AGENTS.md`, `README.md`, `docs/README.md`, `docs/decision-register.md`, `docs/implementation-plan.md`, `docs/api-contract.md` e este log; nenhum código ou migração alterado.
- **Rotas/contrato:** Sem rotas implementadas. Planejamento registra que ADMIN pode promover COLLABORATOR para ADMIN, mas não rebaixar ADMIN; COLLABORATOR não altera papéis.
- **Decisões tomadas e pendentes:** Senha de 6 a 72 caracteres. CLI solicita senha por prompt interativo sem eco e não a inclui em argumentos/logs. A expiração da sessão restringe chamadas autenticadas e pode adiar sincronização da fila, mas não interrompe por si só detecção nativa de localização. Duração da sessão e políticas de CSRF/CORS seguem pendentes.
- **Validações executadas e resultados:** Documentos lidos na ordem definida; revisão textual das referências atualizadas. Nenhum teste foi criado ou executado; código não foi alterado.
- **Problemas/riscos restantes:** Ainda definir duração da sessão, CSRF/CORS e regra de caracteres da senha; eventos offline dependem de autenticação válida para sincronizar.
- **Próxima dependência/módulo:** Fechar duração de sessão e políticas CSRF/CORS antes de implementar Identity.

### Planejamento de sessão, CSRF e CORS — 2026-10-19

- **Objetivo e estado:** Explicar políticas de sessão/CSRF/CORS e registrar propostas para avaliação. Nenhuma funcionalidade foi implementada.
- **Arquivos/migrações alterados:** `AGENTS.md`, `README.md`, `docs/README.md`, `docs/decision-register.md`, `docs/implementation-plan.md`, `docs/api-contract.md` e este log; nenhum código ou migração alterado.
- **Rotas/contrato:** Nenhuma rota implementada. O contrato registra a proposta de token CSRF em cabeçalho nas operações de escrita e CORS com allowlist de origens por ambiente.
- **Decisões tomadas e pendentes:** Preferência do usuário por sessão tão longa quanto razoável. Propostas ainda não aprovadas: expiração deslizante após 30 dias sem atividade, limite absoluto de 90 dias; CSRF habilitado em operações de escrita; CORS com origens explícitas e credenciais, sem wildcard. A expiração não para a detecção nativa de localização, mas pode adiar o envio dos eventos enfileirados.
- **Validações executadas e resultados:** Documentos de planejamento lidos na ordem indicada; documentação oficial do Spring consultada para CSRF/CORS. Nenhum teste ou execução de aplicação.
- **Problemas/riscos restantes:** Duração e políticas ainda aguardam aprovação; origens finais dependem do endereço do cliente desktop em cada ambiente.
- **Próxima dependência/módulo:** Aprovar/ajustar valores da sessão e política CSRF/CORS antes de implementar Identity.

### Planejamento de sessão sem expiração — 2026-10-19

- **Objetivo e estado:** Registrar a decisão de sessão sem expiração automática e o escopo de CSRF/CORS para a atividade. Nenhuma funcionalidade foi implementada.
- **Arquivos/migrações alterados:** `AGENTS.md`, `README.md`, `docs/README.md`, `docs/decision-register.md`, `docs/implementation-plan.md`, `docs/api-contract.md` e este log; nenhum código ou migração alterado.
- **Rotas/contrato:** Nenhuma rota implementada. O contrato planejado registra sessão server-side sem timeout automático, logout/invalidação explícita e ausência de fluxo CSRF dedicado.
- **Decisões tomadas e pendentes:** Usuário aprovou sessões sem expiração automática e não adicionar fluxo CSRF dedicado para esta atividade. Não adicionar CORS além da allowlist que já existe; confirmar se o desktop será navegador cross-origin. Sem CSRF, existe risco de ação originada por outro site usando sessão ativa no navegador.
- **Validações executadas e resultados:** Documentação lida na ordem exigida; inspeção de `WebSecurityConfiguration.java` e `application.properties` confirmou que o código atual é stateless, permite todas as rotas, desativa CSRF e já configura CORS por allowlist. Nenhum teste ou execução de aplicação.
- **Problemas/riscos restantes:** Identity ainda não implementa sessão. Sessão sem timeout mantém risco após perda de dispositivo/cookie; cookie e armazenamento duráveis precisam ser definidos. Rever CSRF antes de exposição pública; CORS só é necessário se o desktop web for cross-origin.
- **Próxima dependência/módulo:** Ao iniciar Identity, implementar o modelo de sessão e invalidar sessão em logout/desativação; confirmar tipo/origem do desktop antes de integração.

### Fechamento de decisões de sessão e senha — 2026-10-19

- **Objetivo e estado:** Registrar aceitação de senha sem restrições de caracteres e sessão persistente sem expiração automática. Nenhuma funcionalidade foi implementada.
- **Arquivos/migrações alterados:** `AGENTS.md`, `README.md`, `docs/README.md`, `docs/decision-register.md`, `docs/implementation-plan.md`, `docs/api-contract.md` e este log; nenhum código ou migração alterado.
- **Rotas/contrato:** Nenhuma rota implementada. Contrato atualizado para sessão server-side no PostgreSQL, cookie persistente sem prazo automático e senha de 6–72 caracteres sem normalização.
- **Decisões tomadas e pendentes:** Usuário aceitou ambas as propostas. Sessão encerra por logout, desativação/revogação. CORS adicional depende de o desktop ser navegador servido em origem distinta. Não criar fluxo CSRF dedicado nesta atividade; risco permanece documentado.
- **Validações executadas e resultados:** Documentos lidos na ordem definida; conferido que o diretório não tem entrada `.git`. Nenhum teste executado e nenhum código alterado.
- **Problemas/riscos restantes:** Código atual permanece stateless, permite todas as rotas e desativa CSRF; Identity precisa substituir essa configuração. Sessões sem expiração e sem CSRF dedicado devem ser reavaliadas antes de exposição pública.
- **Próxima dependência/módulo:** Implementar Identity em etapas; confirmar tipo/origem do cliente desktop antes da integração CORS.

Adicione uma entrada neste formato ao concluir cada etapa:

### [ID e nome do módulo] — AAAA-MM-DD

- **Objetivo e estado:**
- **Arquivos/migrações alterados:**
- **Rotas/contrato:**
- **Decisões tomadas e pendentes:**
- **Validações executadas e resultados:**
- **Problemas/riscos restantes:**
- **Próxima dependência/módulo:**

### Etapa 1 — Identity (fundação parcial) — 2026-10-19

- **Objetivo e estado:** Iniciar Identity com contas, sessão persistente, login, administração e provisionamento inicial. Entrega parcial; configuração real do SMTP e execução manual ainda precisam ser verificadas.
- **Arquivos/migrações alterados:** `pom.xml`; `src/main/resources/application.properties`; nova `src/main/resources/db/migration/V1__identity_and_sessions.sql`; classes em `identity`; `WebSecurityConfiguration.java`, `ApiExceptionHandler.java` e `SecurityProblemWriter.java`; `README.md`, `docs/api-contract.md`, `docs/decision-register.md`, `docs/implementation-plan.md` e este log.
- **Rotas/contrato:** Implementados `POST /v1/auth/login`, `POST /v1/auth/logout`, `GET /v1/me`, `GET/POST /v1/admin/users`, `GET/PATCH /v1/admin/users/{id}` e `POST /v1/admin/users/{id}/deactivation`. Sem endpoint de cadastro público. Flyway cria usuário e tabelas JDBC de sessão; nenhuma tabela de catálogo/engagement.
- **Decisões tomadas e divergências:** Rota de desativação confirmada pelo usuário como `POST /v1/admin/users/{id}/deactivation` (D-035). Mantido OAuth2 Resource Server do POM sem configurar OAuth/JWT, pois autenticação definida é sessão; não foi removida dependência. CLI usa terminal interativo com senha sem eco. Erro no envio SMTP é devolvido ao administrador como resposta HTTP genérica.
- **Validações executadas e resultados:** `docker compose ps` confirmou PostgreSQL 17 saudável. A migração V1 foi aplicada com sucesso durante a inicialização anterior; `mvnw test` com JDK Microsoft 21.0.12.1 repetido em 2026-10-19 passou: 1 teste existente (`contextLoads`), 0 falhas/erros. Uma tentativa intermediária sem permissão de rede falhou ao abrir TCP para PostgreSQL (`SocketException: Permission denied: getsockopt`); repetida com acesso local autorizado, passou. Corrigi o tipo Jackson2/Jackson3 após erro de inicialização; nenhum teste novo foi criado. O servidor web ainda não iniciou neste ambiente: 8080 já estava ocupado e, em 8081, o Tomcat recebeu `SocketException: Invalid argument: connect` ao abrir o pipe loopback do seletor NIO. Health/OpenAPI HTTP não foram verificados.
- **Problemas/riscos restantes:** SMTP depende de `MAIL_APP_PASSWORD` externo e não foi exercitado. E-mail aceito externamente seguido de falha no commit pode entregar credenciais de conta não persistida. Sessão sem expiração e ausência de CSRF dedicado são riscos aceitos para a atividade, a rever antes de exposição pública. Rate limiting não foi implementado. Compilador reporta uso de API deprecated em `IdentityService`; build passa, mas a API deve ser revisada. A porta 8080 permanece ocupada por processo desconhecido e não foi encerrada.
- **Próxima dependência/módulo:** Repetir startup HTTP fora da restrição de socket do ambiente e validar health/OpenAPI e fluxos manuais; SMTP real exige a senha de app externa. Em seguida, Catalog: fechar modelo antes de criar migrações.

### Preferências de fluxo Git e execução local — 2026-10-19

- **Objetivo e estado:** Registrar que novas pull requests devem ter `develop` como base e que a aplicação local deve iniciar na porta `8081`.
- **Arquivos alterados:** `AGENTS.md`, `README.md`, `src/main/resources/application.properties` e este log.
- **Comportamento:** `server.port` usa `SERVER_PORT` com default `8081`; documentação de execução e endpoints foi atualizada. Instruções de trabalho agora orientam abrir PRs para `develop` por padrão.
- **Validação:** Preferência aplicada em branch criada de `origin/develop` após confirmar que o commit da fundação já está integrado a `develop`. Nenhum teste foi executado para essa alteração de configuração/documentação.
- **Próximo passo:** Para a próxima mudança de código, partir de `develop`, iniciar na porta 8081 e abrir PR para `develop`.

### Ajuste do health check e diagnóstico SMTP — 2026-10-19

- **Objetivo e estado:** Evitar que credenciais SMTP inválidas tornem o health agregado da API indisponível quando aplicação e banco estão operacionais.
- **Arquivos alterados:** `src/main/resources/application.properties`, `README.md` e este log.
- **Comportamento:** Desativado o `MailHealthIndicator` agregado com `management.health.mail.enabled=false`. O envio SMTP continua obrigatório no cadastro e no provisionamento de usuários; falha do Gmail continua impedindo a criação da conta.
- **Diagnóstico:** O log do usuário confirma que a aplicação iniciou em `8081`, conectou ao PostgreSQL 17.11 e validou a migração V1. A chamada ao health retornou 503 porque o Gmail rejeitou a autenticação SMTP (`535 BadCredentials`).
- **Validação:** `mvnw test` com JDK Microsoft 21.0.12.1 passou: 1 teste existente (`contextLoads`), 0 falhas/erros. Uma tentativa de iniciar HTTP temporariamente na porta 18081 carregou contexto, banco e Flyway, mas Tomcat não abriu o seletor NIO devido a `SocketException: Invalid argument: connect` no ambiente. Portanto, o status HTTP após a mudança ainda precisa ser confirmado no PowerShell local após reiniciar.
- **Limitações e riscos restantes:** A disponibilidade SMTP não aparece no health agregado; verificar o SMTP no fluxo de criação de conta. As credenciais inválidas precisam ser corrigidas antes de criar usuários ou provisionar o ADMIN.
- **Próximo passo:** Reiniciar com Java 21 e validar `/actuator/health` na porta 8081. Configurar uma senha de app válida antes de testar criação de usuários.

### Decisões de escopo do Catalog — 2026-10-20

- **Objetivo e estado:** Consolidar o modelo revisado e as decisões para preparar a primeira implementação de Catalog. Nenhuma funcionalidade ou migração de Catalog foi criada nesta atualização.
- **Arquivos alterados:** `docs/decision-register.md`, `docs/implementation-plan.md`, `docs/api-contract.md`, `docs/modelo-de-dados.md` e este log.
- **Comportamento/contrato planejado:** Benefício não tem nome/título e cada benefício pertence a um estabelecimento; um estabelecimento pode ter vários benefícios. A primeira versão não armazena nem retorna horários. Fachada aceita URL HTTPS opcional. Desativação será lógica, preservando referências e ocultando itens inativos das consultas normais. Categorias terão cor de paleta do projeto e chave Iconify; mobile baixa apenas ícones selecionados durante sincronização online e mantém cache local para uso offline.
- **Decisões e divergências:** O plano/contrato anteriores ainda mencionavam horários, marcas e distância; foram alinhados para excluir horários e não antecipar marcas ou busca espacial. O conteúdo não especifica nulabilidade/limites de `publico`, `contact` e `discount`; P-007 permanece aberto apenas para esses detalhes. PostGIS permanece pendente, mas não bloqueia CRUD sem proximidade.
- **Validações executadas e resultados:** Inspecionados estado Git limpo, POM, plano, registro, contrato, código e migrações. Nenhum teste foi criado ou executado e nenhuma compilação foi executada. A tentativa de criar branch falhou porque o ambiente negou escrita em `.git` (`Permission denied`); os arquivos de documentação estão editados na branch existente.
- **Limitações e riscos restantes:** Ícones e imagens recém-adicionados só ficam disponíveis offline após o cliente sincronizar e armazenar cache enquanto estiver online. O cliente mobile não está neste repositório. Ainda é necessário fechar P-007 antes da migração de Benefit; valores concretos da paleta também precisam ser definidos no contrato/implementação.
- **Próxima dependência/módulo:** Fechar limites/nulabilidade de Benefit e definir a paleta; depois implementar Catalog em branch de trabalho baseada em `develop`, sem horários, Brand ou busca espacial.

### Catalog — primeira implementação — 2026-10-20

- **Objetivo e estado:** Implementar o modelo inicial, administração e leitura autenticada do catálogo; código preparado e compilado, execução com PostgreSQL ainda não validada.
- **Arquivos criados/alterados:** Classes JPA, repositórios, DTOs, serviço e controllers em `src/main/java/com/weg/benefits/catalog/`; `src/main/resources/db/migration/V2__catalog.sql`; handler comum de conflito HTTP; contrato, registro de decisões, modelo, plano e este log.
- **Comportamento/rotas:** CRUD administrativo (GET paginado/detalhe, POST, PATCH e POST deactivation) para `/v1/admin/categories`, `/services`, `/establishments` e `/benefits`; leitura ativa em `/v1/categories`, `/v1/categories/options`, `/v1/services`, `/v1/establishments`, `/v1/establishments/{id}` e `/v1/establishments/{id}/benefits`. A configuração de segurança existente exige ADMIN para `/v1/admin/**` e sessão para as demais rotas. V2 cria Category, Service, Establishment, relação N:N e Benefit com desativação lógica e UUID.
- **Decisões/divergências:** D-041 confirma todos os campos de Benefit obrigatórios. D-042 confirma UUID para as quatro entidades Catalog. `audience`, `contact` e `discount` usam TEXT porque seus limites máximos continuam sem decisão (P-007). Não foram incluídos horários, Brand, PostGIS ou busca por distância.
- **Comandos e resultados:** `mvnw.cmd -DskipTests compile` com Microsoft JDK 21.0.12.1 passou. Nenhum teste foi adicionado ou executado. `mvnw.cmd spring-boot:run` foi tentado em 8081; a inicialização não conectou ao banco porque `localhost:5432` recusou a conexão. `docker compose ps` confirmou que o Docker Desktop engine não está acessível. A migração V2 não foi aplicada e endpoints não foram exercitados.
- **Limitações/riscos:** Limites de público/contato/desconto seguem pendentes; valores da paleta de oito cores são a paleta inicial do projeto; runtime, Flyway V2 e autorização ainda precisam de verificação com PostgreSQL em execução. Commit `bca3426` foi criado na branch `codex/catalog-foundation` e publicado em `origin`; a PR para `develop` ainda não foi aberta porque o navegador estava sem sessão GitHub e o `gh` CLI não está instalado.
- **Próxima etapa:** Iniciar Docker/PostgreSQL, executar API na porta 8081 e validar migração/health/endpoints sem executar testes automatizados; resolver P-007 antes de impor limites a Benefit; abrir a PR para `develop` após autenticar GitHub/conectar a integração.

### Revisão do modelo de dados recebida — 2026-10-20

- **Objetivo e estado:** Preservar e comparar a modelagem atualizada enviada pela equipe. Revisão documental concluída; alterações de esquema e comportamento aguardam reconciliação das divergências.
- **Arquivos alterados:** Adicionado `docs/referencia/WEG-Benefits-Modelagem-revisada.pdf`; atualizados `docs/modelo-de-dados.md`, `docs/decision-register.md`, `docs/README.md` e este log. Nenhum código, endpoint ou migração foi alterado.
- **Modelo/contrato:** O PDF propõe IDs inteiros para entidades de Catalog, relação Establishment 1:1 Benefit, entidade Discount 1:N Benefit, seleção de ícones por opções do sistema, username só com letras minúsculas e Estado de 2–75 caracteres.
- **Decisões e divergências:** P-017 a P-022 registram conflitos com decisões anteriores, contratos/validadores e V2. O estado aprovado e implementado foi preservado; V2 não foi editada. A cópia do PDF tem o mesmo SHA-256 da origem.
- **Validações executadas:** Verificado que o PDF de origem estava acessível, extraído seu texto (6 páginas), inspecionados modelo, registro, contrato, código e migrações. Links relativos adicionados apontam para arquivos existentes. Nenhum teste ou compilação foi executado.
- **Limitações/riscos restantes:** Não é seguro converter `UUID` em inteiros nem separar o texto de desconto em valor/descrição sem confirmar impacto na API/clientes e definir a preservação de dados existentes. Também faltam definições inequívocas para a cardinalidade e para as regras de username, ícones e estado.
- **Próxima dependência/módulo:** Resolver P-017 a P-022 com a equipe; então especificar uma migração nova de reconciliação de Catalog e atualizar entidades, DTOs e contrato, preservando V2.

### Decisões após revisão do modelo Catalog — 2026-10-20

- **Objetivo e estado:** Registrar as respostas da equipe à modelagem atualizada. Decisões de produto consolidadas; código e banco ainda não reconciliados.
- **Arquivos alterados:** `AGENTS.md`, `docs/modelo-de-dados.md`, `docs/decision-register.md`, `docs/implementation-plan.md`, `docs/api-contract.md` e este log. Nenhuma classe ou migração foi alterada.
- **Modelo/contrato:** IDs do catálogo continuam UUID; Establishment e Benefit têm relação 1:1; cada Benefit terá um ou mais Discounts. Iconify, username D-022 e UF como sigla permanecem.
- **Decisões e pendências:** Registradas D-043 a D-046; P-017 a P-022 foram resolvidas. P-023 define se Establishment pode existir temporariamente sem Benefit, P-024 confirma obrigatoriedade de value/description, e P-025 verifica/preserva dados já gravados. A resposta ao item 3 foi interpretada como “pelo menos um Discount por Benefit”.
- **Validações executadas:** Documentação revisada e diff verificado. Nenhum teste ou compilação foi executado.
- **Limitações/riscos restantes:** V2 ainda permite múltiplos benefícios por estabelecimento e mantém `discount` como texto. Não alterá-la; criar migração nova após fechar o fluxo de criação e o mapeamento dos dados.
- **Próxima dependência/módulo:** Confirmar P-023 a P-025; então atualizar Catalog com migração Flyway nova, entidades, DTOs e contrato.

### Fluxo de Benefit/Discount — 2026-10-20

- **Objetivo e estado:** Registrar como os usuários do desktop irão gerenciar benefícios, descontos e pré-requisitos para estabelecimento. Decisões de produto adicionadas; o desenho HTTP aguarda o protótipo.
- **Arquivos alterados:** `AGENTS.md`, `docs/modelo-de-dados.md`, `docs/decision-register.md`, `docs/implementation-plan.md`, `docs/api-contract.md` e este log. Nenhum código ou migração foi alterado.
- **Comportamento aprovado:** `Discount.value` (2–15) e `Discount.description` (10–300) são obrigatórios. Benefício e descontos são criados/editados na mesma tela; Establishment só pode ser criado quando Category, Benefit e Discount existirem.
- **Decisões e pendências:** D-047/D-048 registram as decisões. P-023 aguarda o protótipo para definir DTOs/operação HTTP e limite transacional; P-025 continua exigindo uma estratégia que preserve dados já gravados.
- **Validações executadas:** Revisão documental e `git diff --check`; nenhum teste ou compilação foi executado.
- **Limitações/riscos restantes:** Não se deve implementar o fluxo de Catalog por suposição de payload ou de criação antes de analisar o protótipo. V2 não deve ser reescrita.
- **Próxima dependência/módulo:** Receber o protótipo, fechar P-023 e verificar dados locais; depois criar migração nova e alinhar entidades, serviços, DTOs e contrato.

### Revisão do protótipo de Benefit — 2026-10-20

- **Objetivo e estado:** Registrar o fluxo visível nas telas de criação e edição de benefício. Fluxo de interface entendido; cardinalidade inversa ainda pendente.
- **Arquivos alterados:** `AGENTS.md`, `docs/modelo-de-dados.md`, `docs/decision-register.md`, `docs/implementation-plan.md`, `docs/api-contract.md` e este log. Nenhum código ou migração foi alterado.
- **Comportamento visível:** Benefício é criado/edita-se numa tela própria com um ou mais descontos; começa inativo, ativa na primeira associação a estabelecimento, e desativação manual permanece mesmo com vínculos. A criação do estabelecimento requer categoria e benefício/desconto existentes.
- **Decisões e divergências:** Value/description obrigatórios confirmados (D-047); telas adicionam D-049. O rótulo plural “Estabelecimentos vinculados” sugere que um Benefit pode estar em múltiplos Establishments, mas P-026 registra a necessidade de confirmação contra a resposta anterior de relação 1:1.
- **Validações executadas:** Inspeção visual das duas telas enviadas e revisão dos documentos. Nenhum teste ou compilação foi executado.
- **Limitações/riscos restantes:** A direção da relação Benefit–Establishment determina FK, unicidade e payloads da API; não implementá-la até confirmar P-026. A consulta ao PostgreSQL local permanece indisponível neste ambiente.
- **Próxima dependência/módulo:** Confirmar se Benefit pode ser reutilizado por vários Establishments; depois definir associação, atualizar Catalog e preparar migração nova com preservação de dados.

### Catalog — benefício exclusivo e descontos estruturados — 2026-10-20

- **Objetivo e estado:** Aplicar a decisão de que Benefit é exclusivo de um Establishment e implementar o fluxo Benefit/Discount aprovado. Código compilado; V3 ainda não foi aplicada e a etapa permanece parcial até validação no PostgreSQL.
- **Arquivos criados/alterados:** Criado `src/main/java/com/weg/benefits/catalog/Discount.java` e `src/main/resources/db/migration/V3__benefit_discounts_and_exclusive_establishment.sql`. Alterados `Benefit.java`, `Establishment.java`, `BenefitRepository.java`, `CatalogDtos.java`, `CatalogService.java`, README, AGENTS, contrato, plano, registro de decisões, modelo de dados, índice de documentação, README de migrações e este log.
- **Comportamento/rotas:** Sem novas rotas. `POST/PATCH /v1/admin/benefits` passa a receber a lista de descontos com `value` (2–15) e `description` (10–300), e Benefit começa inativo sem estabelecimento. `POST /v1/admin/establishments` passa a exigir `benefitId` e `categoryId`; o vínculo é exclusivo e ativa o benefício apenas no primeiro vínculo. PATCH de estabelecimento permite associar/trocar benefício; não permite reutilizar benefício vinculado a outro estabelecimento. V3 cria `benefit_discount`, adiciona `establishment.benefit_id` obrigatório e único, registra `first_linked_at` e remove o desconto textual antigo, sem editar V2.
- **Decisões e divergências:** D-050/P-026 confirmam Benefit exclusivo de um estabelecimento; Benefit pode existir sem vínculo antes. A legenda plural “Estabelecimentos vinculados” no protótipo entra em conflito e deve ser ajustada no cliente. V3 falha com segurança se houver qualquer linha em `establishment` ou `benefit`; P-025 requer verificar e mapear dados antes da aplicação.
- **Comandos e resultados:** `mvnw.cmd -DskipTests compile` com Microsoft JDK 21.0.12.1 passou (`BUILD SUCCESS`). `git diff --check` passou. Não foram criados nem executados testes. A migração não foi aplicada e a API não foi iniciada; o conteúdo local do PostgreSQL, health e endpoints não foram verificados nesta etapa.
- **Limitações/riscos:** Se o banco local já tiver estabelecimentos ou benefícios, a atualização falhará deliberadamente até haver mapeamento manual/revisado dos dados; não converter o desconto legado por inferência. A constraint da tabela garante que todo estabelecimento tenha um benefício exclusivo; a regra de ao menos um desconto por benefício é validada pela API. O cliente desktop deve alinhar a legenda plural à cardinalidade aprovada.
- **Próxima dependência/módulo:** Verificar contagens do banco local, decidir como mapear dados se houver, aplicar V3 e validar manualmente na porta 8081 criação/edição de Benefit/Discount, associação exclusiva, ativação inicial e conflitos; depois seguir para Engagement.

### Catalog — aplicação da migração V3 — 2026-10-21

- **Objetivo e estado:** Conferir se havia dados legados antes da migração e aplicar o novo esquema Catalog no banco local.
- **Arquivos alterados:** `docs/api-contract.md`, `docs/decision-register.md`, `docs/README.md` e este log.
- **Comportamento/rotas/migrações:** O usuário executou `docker compose ps` e confirmou PostgreSQL healthy. As contagens pré-migração de `establishment` e `benefit` foram zero. Ao iniciar a API, Flyway validou três migrações, encontrou esquema v2 e aplicou V3 com sucesso; o esquema local avançou para v3. Não houve alteração de rotas neste trabalho.
- **Decisões/divergências:** Nenhum mapeamento de dados foi necessário para o banco local. P-025 está resolvido para essa base; a proteção permanece para outras bases com dados. A V3 está aplicada, mas a API não completou o início HTTP neste ambiente.
- **Comandos e resultados:** `docker compose ps` do usuário mostrou `weg-benefits-postgres` healthy. Consulta SQL do usuário retornou zero estabelecimentos e zero benefícios. `mvnw.cmd spring-boot:run` com Microsoft JDK 21.0.12.1 conectou ao PostgreSQL 17.11 e registrou `Successfully applied 1 migration ... now at version v3`. A inicialização avançou até o JPA; Tomcat falhou ao criar seu seletor NIO com `SocketException: Invalid argument: connect` ao tentar loopback. A primeira tentativa sem elevação foi bloqueada com `Permission denied: getsockopt`. Health HTTP e rotas Catalog não foram verificadas. Docker CLI não estava acessível ao processo de verificação posterior (`dockerDesktopLinuxEngine: Access denied`), então não foi feita consulta adicional de schema.
- **Limitações/riscos:** O banco local está migrado, mas não se confirmou o status `/actuator/health` nem o comportamento HTTP. O erro de Tomcat parece restrição de loopback do ambiente de execução; o usuário precisa iniciar a API localmente na porta 8081 para validar o health e seguir os fluxos manuais.
- **Próxima dependência/módulo:** Iniciar `.\mvnw.cmd spring-boot:run` localmente com Java 21 e confirmar `/actuator/health`; então validar CRUD de Benefit/Discount e associação exclusiva do Establishment, sem rodar testes automatizados.

### Catalog — health confirmado após V3 — 2026-10-21

- **Objetivo e estado:** Confirmar que a API local iniciou após configurar Maven para usar Java 21 e aplicar V3.
- **Arquivos alterados:** Este diário de implementação.
- **Comportamento/rotas/migrações:** O usuário confirmou `UP` em `http://localhost:8081/actuator/health`; a API está acessível e responde ao health. V3 já havia sido aplicada pelo Flyway. Nenhuma rota de negócio foi exercitada.
- **Decisões/divergências:** A falha anterior de inicialização do Maven era Java 8 no terminal, que não carrega o plugin do Spring Boot; com Java 21 a aplicação iniciou no ambiente do usuário.
- **Comandos e resultados:** Resultado fornecido pelo usuário: `(Invoke-RestMethod http://localhost:8081/actuator/health).status` retornou `UP`. Nenhum teste automatizado foi criado ou executado.
- **Limitações/riscos:** Health confirma aplicação/banco conforme configuração, mas não valida autenticação nem operações do Catalog.
- **Próxima dependência/módulo:** Validar manualmente criação/edição de Benefit com descontos e associação exclusiva ao criar Establishment; depois seguir para Engagement.

### Correção do modo CLI para provisionamento do primeiro ADMIN — 2026-10-21

- **Objetivo e estado:** Corrigir a falha relatada ao iniciar o provisionamento CLI sem servidor web. A alteração foi compilada; o provisionamento ainda precisa ser repetido pelo usuário.
- **Arquivos alterados:** `src/main/java/com/weg/benefits/identity/IdentityService.java` e este diário.
- **Comportamento:** `IdentityService` agora recebe o repositório de sessões por `ObjectProvider`, resolvendo-o somente nas operações que revogam sessões. O contexto CLI pode iniciar sem o bean de sessões HTTP; na aplicação web, atualização/desativação ainda exige o repositório e revoga as sessões correspondentes.
- **Decisões/divergências:** A causa foi a ausência de `FindByIndexNameSessionRepository` quando `spring.main.web-application-type=none`. Nenhuma regra de Identity nem migração foi alterada.
- **Comandos e resultados:** `mvnw.cmd -DskipTests compile` com Microsoft JDK 21.0.12.1 passou (`BUILD SUCCESS`). A primeira tentativa não iniciou o Maven porque um caminho incorreto de `JAVA_HOME` foi usado; repetida com o JDK existente, compilou. Nenhum teste foi criado ou executado; a inicialização CLI e SMTP ainda não foram repetidos.
- **Limitações/riscos:** O usuário precisa repetir o comando CLI em terminal interativo com PostgreSQL e SMTP configurados para confirmar o provisionamento completo.
- **Próxima etapa:** Reexecutar o provisionamento CLI e confirmar que ele solicita usuário/e-mail/senha antes de persistir o primeiro ADMIN.

### Isolamento da configuração HTTP no modo CLI — 2026-10-21

- **Objetivo e estado:** Corrigir a falha seguinte do provisionamento CLI, causada por componentes de autenticação HTTP carregados num contexto sem servidor web. Pacote compilado e inicialização manual validada até o prompt; nenhum ADMIN foi provisionado.
- **Arquivos criados/alterados:** Criado `src/main/java/com/weg/benefits/config/PasswordEncodingConfiguration.java`; alterados `src/main/java/com/weg/benefits/config/WebSecurityConfiguration.java`, `src/main/java/com/weg/benefits/identity/AuthController.java` e este log.
- **Comportamento:** Codificador de senha continua disponível no CLI. Configuração HTTP e `AuthController` só são carregados em aplicação Servlet. O CLI iniciou com PostgreSQL 17.11, validou três migrações, confirmou esquema v3 atualizado e alcançou os prompts de provisionamento; a execução foi interrompida antes da coleta completa e envio SMTP.
- **Decisões/divergências:** A exceção relatada ocorreu porque `AuthenticationConfiguration` não existe em `web-application-type=none`. A primeira verificação de runtime foi bloqueada pelo sandbox ao abrir conexão local; repetida com acesso local, a inicialização alcançou o prompt. Nenhuma regra de negócio ou migração mudou.
- **Comandos e resultados:** `mvnw.cmd -DskipTests package` com Microsoft JDK 21.0.12.1 passou (`BUILD SUCCESS`; testes ignorados). O comando do jar em modo CLI conectou ao banco e chegou a `Username do primeiro ADMIN:`. Foi interrompido antes da conclusão do provisionamento; nenhum teste foi criado ou executado.
- **Limitações/riscos:** Ainda falta executar o fluxo completo interativamente com SMTP configurado para confirmar aceitação do Gmail e criação do primeiro ADMIN.
- **Próxima etapa:** Reexecutar o comando do README em terminal interativo, inserir os dados do primeiro ADMIN e confirmar a mensagem de sucesso do SMTP; depois iniciar API na porta 8081 e verificar health/login.

## Importante

Não registrar senhas, tokens, hashes, chaves, coordenadas pessoais ou justificativas sigilosas. O resumo retornado ao usuário deve permitir atualizar a conversa de planejamento sem expor segredos.
