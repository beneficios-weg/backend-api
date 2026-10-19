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

## Importante

Não registrar senhas, tokens, hashes, chaves, coordenadas pessoais ou justificativas sigilosas. O resumo retornado ao usuário deve permitir atualizar a conversa de planejamento sem expor segredos.
