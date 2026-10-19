# Instruções para trabalhar na API WEG Benefits

## Leitura obrigatória antes de cada tarefa

Antes de planejar ou editar código, leia nesta ordem:

1. `AGENTS.md`;
2. `README.md`;
3. `docs/README.md`;
4. `docs/implementation-plan.md`;
5. `docs/decision-register.md`;
6. `docs/api-contract.md` quando a tarefa alterar endpoints, DTOs ou integração com clientes;
7. o código e as migrações existentes dos módulos afetados.

Os documentos descrevem intenção e decisões. O código e as migrações mostram o estado implementado. Se houver divergência, não presuma que um lado está correto: descreva-a, preserve dados/alterações existentes e proponha a menor reconciliação segura.

## Limites do produto

**Decisões de Identity:** existem somente os papéis ADMIN e COLLABORATOR. Somente ADMIN cria usuários; o primeiro ADMIN será provisionado por script CLI, sem endpoint de bootstrap. O CLI cria o primeiro ADMIN ativo, gera ID/timestamps e usa papel fixo ADMIN; solicita username e e-mail normalmente e senha em prompt sem eco, sem incluir a senha em argumentos ou logs. O painel cria usuários ativos como COLLABORATOR por padrão. ADMIN pode criar ADMINs, editar qualquer conta, promover COLLABORATOR para ADMIN e usar seus próprios favoritos/visitas; não pode rebaixar ADMIN para COLLABORATOR nem desativar ADMIN ou a própria conta. COLLABORATOR não altera papéis. ADMIN pode desativar apenas COLLABORATOR. ADMIN define a senha comum da conta; não há senha temporária, troca forçada no primeiro acesso nem redefinição. A conta só é persistida quando o Gmail aceita o envio SMTP de credenciais; em erro/rejeição, não há conta e o ADMIN recebe aviso. Aceitação SMTP conta como sucesso, mesmo se houver bounce posterior. Login aceita username único ou e-mail com senha. Usuário tem ID inteiro automático; username único 3–75 caracteres ASCII minúsculos `a-z`, dígitos, `.`, `-`, `_`, removendo espaços externos e normalizando para minúsculo; e-mail obrigatório/válido/único somente nos domínios `gmail.com` ou `estudante.sesisenai.org.br`; senha armazenada apenas como hash, entre 6 e 72 caracteres, aceitando qualquer caractere e sem normalização; estado `Ativo`; timestamps do sistema. Remetente: `wegbenefits@gmail.com`; envio por SMTP do Gmail com senha de app, habilitada mediante verificação em duas etapas e armazenada fora do repositório. Para Identity, o usuário decidiu sessão server-side sem expiração automática, armazenada no PostgreSQL e com cookie persistente até logout; desativação/revogação também encerram acesso. Não haverá fluxo CSRF dedicado nesta atividade; registrar o risco de ações no navegador usando uma sessão ativa. Não adicionar política CORS além da allowlist já existente, salvo se um cliente web for servido em origem diferente da API. O código atual ainda é stateless, permite todas as rotas e desativa CSRF; autenticação por sessão não foi implementada. Não implemente funcionalidades de Identity por inferência.

- O back-end é uma API Java/Spring Boot, monólito modular.
- Clientes mobile e desktop acessam os dados somente pela API; nunca se conectam diretamente ao banco.
- O aplicativo mobile não administra o catálogo.
- Somente ADMIN cria contas. Não há cadastro próprio nem redefinição de senha; não há senha temporária nem troca forçada no primeiro acesso. As credenciais são enviadas automaticamente ao e-mail cadastrado pelo ADMIN; em caso de falha, o sistema apresenta um aviso ao ADMIN.
- O desktop administrativo escreve no catálogo por endpoints protegidos da API.
- Favoritos e visitas são dados pessoais do colaborador e não concedem acesso a dados de outras contas.
- A localização em segundo plano é detectada pelo cliente mobile e seus módulos nativos. A API recebe o evento de visita final e não mantém trilhas contínuas de GPS.
- O app cliente mantém cache e fila offline. A API deve aceitar reenvio idempotente das operações aprovadas.

## Regras de implementação

**Atualização de Identity — 2026-10-19:** as rotas de autenticação por sessão e administração de usuários estão implementadas parcialmente; a desativação é `POST /v1/admin/users/{id}/deactivation` conforme D-035. Considere obsoleta a frase acima que descreve o código como stateless e sem autenticação. Consulte `docs/implementation-log.md` para o estado validado. As demais regras de não inferir pendências permanecem vigentes.

- Implemente somente a etapa/módulo solicitado; não antecipe os demais.
- Inspecione o estado do Git, o POM, os arquivos, migrações e dependências antes de editar.
- Por padrão, crie branches de trabalho a partir de `develop` e abra pull requests com destino a `develop`; só use outro destino quando o usuário solicitar.
- Ao iniciar a API localmente, use a porta `8081` (configurável por `SERVER_PORT`).
- Quando uma alteração coerente estiver concluída e revisada, faça um commit se o projeto estiver em Git e o commit for apropriado; preserve mudanças alheias/incompletas e informe o hash. Não crie commits se não houver repositório Git.
- Preserve alterações locais e migrações aplicadas. Nunca reescreva uma migração já aplicada.
- Use DTOs na fronteira HTTP; não exponha entidades JPA.
- Valide entradas, relações, papel e propriedade no servidor.
- Não registre nem devolva senha, hash, tokens ou localização precisa desnecessária.
- Não invente resposta para item marcado `PENDENTE` no registro de decisões.
- Atualize contrato e documentos no mesmo trabalho quando o comportamento mudar.
- Não afirme que compilou, executou ou validou algo sem ter feito isso; informe comandos e resultados reais.

## Relatório de encerramento da tarefa

Ao concluir uma etapa, retorne um resumo que o usuário possa colar na conversa de planejamento:

1. etapa e objetivo;
2. arquivos criados/alterados;
3. comportamento implementado e rotas/migrações adicionadas;
4. decisões tomadas e divergências encontradas;
5. comandos executados e resultados reais (compilação, testes, execução manual);
6. limitações, erros e riscos restantes;
7. próximo módulo recomendado.

Atualize `docs/implementation-log.md` com o mesmo resumo conciso. Não marque etapas futuras como concluídas.
