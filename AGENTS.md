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

**Decisões de Identity:** existem somente os papéis ADMIN e COLLABORATOR. Somente ADMIN cria usuários; o primeiro ADMIN será provisionado por script CLI, sem endpoint de bootstrap. O CLI cria o primeiro ADMIN ativo, gera ID/timestamps e usa papel fixo ADMIN; solicita username e e-mail normalmente e senha em prompt sem eco, sem incluir a senha em argumentos ou logs. O painel cria usuários ativos como COLLABORATOR por padrão. ADMIN pode criar ADMINs, editar qualquer conta, promover COLLABORATOR para ADMIN e usar seus próprios favoritos/visitas; não pode rebaixar ADMIN para COLLABORATOR nem desativar ADMIN ou a própria conta. COLLABORATOR não altera papéis. ADMIN pode desativar apenas COLLABORATOR. ADMIN define a senha comum da conta; não há senha temporária, troca forçada no primeiro acesso nem redefinição. A conta só é persistida quando a Gmail API aceita o envio de credenciais (resposta HTTP de sucesso); em erro/rejeição, não há conta e o ADMIN recebe aviso. Aceitação pela API não garante entrega final, e um bounce posterior não desfaz a conta. Login aceita username único ou e-mail com senha. Usuário tem ID inteiro automático; username único 3–75 caracteres ASCII minúsculos `a-z`, dígitos, `.`, `-`, `_`, removendo espaços externos e normalizando para minúsculo; e-mail obrigatório/válido/único somente nos domínios `gmail.com` ou `estudante.sesisenai.org.br`; senha armazenada apenas como hash, entre 6 e 72 caracteres, aceitando qualquer caractere e sem normalização; estado `Ativo`; timestamps do sistema. Remetente: `wegbenefits@gmail.com`; envio pela Gmail API com OAuth 2.0 e escopo `gmail.send`; client secret e refresh token ficam fora do repositório. Para Identity, o usuário decidiu sessão server-side sem expiração automática, armazenada no PostgreSQL e com cookie persistente até logout; desativação/revogação também encerram acesso. Não haverá fluxo CSRF dedicado nesta atividade; registrar o risco de ações no navegador usando uma sessão ativa. Não adicionar política CORS além da allowlist já existente, salvo se um cliente web for servido em origem diferente da API. Não implemente funcionalidades de Identity por inferência.

- O back-end é uma API Java/Spring Boot, monólito modular.
- Clientes mobile e desktop acessam os dados somente pela API; nunca se conectam diretamente ao banco.
- O aplicativo mobile não administra o catálogo.
- Somente ADMIN cria contas. Não há cadastro próprio nem redefinição de senha; não há senha temporária nem troca forçada no primeiro acesso. As credenciais são enviadas automaticamente ao e-mail cadastrado pelo ADMIN; em caso de falha, o sistema apresenta um aviso ao ADMIN.
- O desktop administrativo escreve no catálogo por endpoints protegidos da API.
- Favoritos e visitas são dados pessoais do colaborador e não concedem acesso a dados de outras contas.
- A localização em segundo plano é detectada pelo cliente mobile e seus módulos nativos. A API recebe o evento de visita final e não mantém trilhas contínuas de GPS.
- O app cliente mantém cache e fila offline. A API deve aceitar reenvio idempotente das operações aprovadas.
- Dados e operações mantidos offline pelo cliente podem permanecer sem prazo de expiração até a reconexão (D-051). Se a conta estiver desativada na sincronização, a API recusa as operações; o app informa a desativação e oferece apagar a fila local. Operações pendentes nunca são enviadas sob outra conta (D-054).
- O cliente registra uma visita depois que o usuário permanecer 3 minutos no estabelecimento; a API recebe apenas o evento final, sem trilha GPS (D-008/D-052). Reenvios usam chave estável por estabelecimento e timestamp UTC; duplicatas do mesmo usuário/estabelecimento dentro de 30 minutos não criam outra visita (D-053).
- Em pedidos de sincronização com sessão revogada, a API responde `401`; a resposta não revela se a causa foi desativação ou sessão inválida. O cliente deve informar que a sessão/conta não está disponível, não reenviar sob outra conta e oferecer apagar a fila (D-054).

**Decisões do modelo Catalog — revisão atualizada em 2026-10-27:** Category, Service, Establishment, Benefit e Discount usam UUID. Cada Establishment exige exatamente um Benefit; um Benefit pode estar sem vínculo ou ser compartilhado por vários Establishments (D-057). Cada Benefit exige pelo menos um Discount, com `value` obrigatório (2–15 caracteres) e `description` obrigatório/não vazio, sem limite de domínio. Instruções de participação e descrição do estabelecimento também são texto obrigatório sem limite de domínio; nome do estabelecimento tem mínimo de 2 caracteres. Preservar a escrita recebida, sem truncar, preencher ou corrigir. Benefício começa inativo e ativa ao primeiro vínculo; desativação manual permanece. Establishment exige Category ativa e Benefit existente com desconto. Seu `channel` é PHYSICAL (padrão compatível), ONLINE ou HYBRID. ONLINE não possui endereço/coordenadas, aparece na lista e tem `mappable=false`; físico/híbrido exigem localização completa. O mapa usa `forMap=true`. Visitas a ONLINE são recusadas com 400; favoritos continuam permitidos. Permanecem Iconify, username D-022 e Estado como sigla UF. V3 é histórica; V6 remove exclusividade, V7 amplia textos e V8 adiciona canais preservando dados e vínculos.

**Geocodificação — decisão de 2026-10-27:** Geoapify foi escolhida para geocodificar endereços de estabelecimentos e apoiar o preenchimento/revisão de `latitude` e `longitude` no catálogo. O resultado deve ser conferido antes de persistir. Esta decisão não autoriza enviar localização pessoal dos colaboradores, trocar tiles/roteamento, adicionar uma chamada Geoapify ao backend ou incluir chave no Mobile. Integração automatizada/endpoint administrativo exige tarefa própria; começar com preparação/revisão de dados. Manter atribuições exigidas pelo plano e pela fonte dos dados.

**Revisão do catálogo — orientação de 2026-10-27:** preservar a escrita dos benefícios do PDF, inclusive erros de digitação, datas, contatos e cupons; corrigir somente reconhecimento OCR para coincidir com a captura, sem paráfrases, preenchimentos de tamanho ou alertas comerciais inseridos no texto (D-061). A oferta atribuída à Clínica Care é apenas 20% no serviço de Nutrição, Ana Paula Schreiner, página 46; o site Care no contato de outro profissional não autoriza agrupar a oferta na clínica nem alterar cardinalidade (D-062). O usuário aprovou os 20 pares de coordenadas selecionados, com aprovação registrada separadamente de importação (D-063). Ofertas online sem endereço/coordenadas foram implementadas na API por canal de Establishment (D-064); a adaptação da tela Mobile permanece no cliente. O preparador de carga não importa dados e não inventa UUIDs, endereços nem resolução de campanhas conflitantes.

## Regras de implementação

**Catálogo e nomes — 2026-10-30:** o nome correto da categoria é **Lazer**, conforme usuário e menu do PDF; não ampliar para Lazer e Turismo. V9 permite nomes de categoria de 5–75 caracteres, preservando dados. Corrigir a categoria existente por PATCH após publicar V9; não duplicar categorias nem reativar cópias para aumentar contagens. A carga é parcial: 49 benefícios do PDF ainda sem estabelecimento ativo (20 de Saúde), conforme auditoria em `docs/catalogo-cobertura-e-nomes.md`. O preparador evidencia cobertura; arquivos preparados não significam importação.

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
