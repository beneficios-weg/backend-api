# WEG Benefits — continuidade em outra máquina

**Atualizado em 30/10/2026.** Este documento permite retomar o projeto sem acesso à conversa anterior. É um retrato verificado, não uma declaração de que todas as etapas estão concluídas. Não contém senhas, hashes, tokens, cookies ou chaves privadas.

## 1. Primeiro leia isto

O Backend está publicado, com a migração **V9 aplicada**, health/readiness UP e nome de categoria aceitando cinco caracteres. A integração inicial do Mobile já está em develop. Os dois problemas mais recentes foram investigados:

1. **Saúde mostra dez estabelecimentos porque a carga está incompleta.** Dos 30 benefícios dessa categoria, 20 ainda não têm estabelecimento ativo. Não foi encontrado corte de dez no Mobile: ele percorre as páginas da API. No total, faltam estabelecimentos para **49 dos 112 benefícios do PDF**.
2. **O nome correto é Lazer.** O banco ainda pode mostrar Lazer e Turismo. V9 corrigiu a validação que impedia o nome de cinco caracteres, mas **não renomeia a linha automaticamente**. O PATCH autenticado ainda não teve execução/sucesso confirmado pelo operador. Conferir o nome atual antes de agir.

**Próxima ação concreta:** conferir/corrigir Lazer com o script do pacote; depois sincronizar o app e retomar a pesquisa/revisão dos estabelecimentos ausentes. Não repetir a carga inteira nem reativar cópias desativadas para aumentar contagens.

## 2. Repositórios, PRs e implantação

| Item | Estado verificado |
|---|---|
| Backend | [beneficios-weg/backend-api](https://github.com/beneficios-weg/backend-api), Java 21, Spring Boot 4.1.1, Maven |
| Mobile | [beneficios-weg/mobile](https://github.com/beneficios-weg/mobile), Svelte 5/TypeScript/Vite, Capacitor Android |
| Branch de integração | develop nos dois repositórios; branches de trabalho com prefixo codex/; abrir PRs para develop |
| Backend PR #64 | [Mesclada](https://github.com/beneficios-weg/backend-api/pull/64); merge fe4271f682ec412536b324e89bd849d1c95c7ee9 |
| Mobile PR #45 | [Mesclada](https://github.com/beneficios-weg/mobile/pull/45); merge faf6bd38df175f1baf489825c2a97fafbeb20bbc |
| Backend PR #62 | [Aberta](https://github.com/beneficios-weg/backend-api/pull/62); manutenção de senha ADMIN por CLI, branch codex/admin-password-maintenance, último commit funcional 054c8d4 |
| Backend PR #65 | [Aberta](https://github.com/beneficios-weg/backend-api/pull/65); registro do deploy e documentos de continuidade, branch codex/catalog-v9-deploy-record; não exige redeploy |
| API hospedada | https://weg-benefits-api.onrender.com |
| Render | My Workspace, workspace tea-dbfj5v8n74is73ekan5g, serviço weg-benefits-api / srv-dbfkgnu7bikc73dtmou0 |
| Deploy confirmado | [dep-dbiaalutlv9c739a6u1g](https://dashboard.render.com/web/srv-dbfkgnu7bikc73dtmou0/deploys/dep-dbiaalutlv9c739a6u1g), commit fe4271f, live em 30/10/2026 às 14:14:39 UTC (11:14:39 de Brasília) |
| Banco hospedado | Neon, projeto little-bread-23232609, branch production / br-dark-truth-b6lcjmzq, banco neondb |

O deploy da PR 64 foi acionado após verificar que nenhum novo deploy estava em andamento. Apesar da configuração de auto-deploy habilitada, o serviço ainda executava a PR 56; a causa da ausência do disparo automático não foi investigada. Sempre conferir commit/status antes de disparar outro deploy. Não tomar build/trigger aceito como confirmação de versão live.

A aplicação usa Render Free e Neon externo para uma atividade sem custo. Não contratar plano pago, comprar domínio nem criar outro banco/projeto por inferência. O domínio padrão onrender.com foi aceito. Não trocar pelo PostgreSQL gratuito temporário do Render.

## 3. Preparar a outra máquina

Necessários: Git e acesso aos repositórios; JDK 21/Maven Wrapper para o Backend; Node/npm compatíveis com o lockfile para o Mobile. A máquina anterior foi validada com Node 24.11.1/npm 11.6.2. Docker Desktop é necessário somente para PostgreSQL local/testes locais, não para usar o Mobile contra o Render. Android Studio/SDK só são necessários para testar/gerar o aplicativo Android.

### Clonar em pastas novas

```powershell
git clone --branch develop https://github.com/beneficios-weg/backend-api.git benefits
git clone --branch develop https://github.com/beneficios-weg/mobile.git weg-benefits
```

Se já existem checkouts, começar com `git status`; preservar alterações locais e só então fazer fetch/troca de branch/`git pull --ff-only`. Não usar reset --hard nem sobrescrever arquivos de configuração. Não presumir que main contém os mesmos commits de develop.

Enquanto a PR 65 estiver aberta, o documento mais recente está nessa PR/branch e no ZIP entregue, podendo ainda não aparecer no clone de develop. O código funcional da PR 64 já está em develop. A manutenção de senha da PR 62 **ainda não está em develop**; não presumir que seu script exista após o clone.

### Restaurar os arquivos locais do pacote

1. Extrair o ZIP de continuidade em uma pasta nova.
2. Ler `LEIA-PRIMEIRO.md` e este documento em `backend/docs/`.
3. Copiar **o conteúdo de `backend/target/` do pacote** para `target/` no novo clone do Backend, preservando a estrutura. Não sobrescrever arquivos existentes sem comparar.
4. `backend/docs/`, `backend/AGENTS.md` e `mobile/docs/` no ZIP são snapshots de referência; não substituem um pull e não devem sobrescrever documentação mais nova.
5. Conferir `INVENTARIO.json`/`VERIFICACAO.json` para integridade e arquivos incluídos. ZIP não é backup do banco.

`target/` é ignorado pelo Git. As fontes PDF, capturas, mapas de UUIDs, relatórios e operadores locais **não chegam por git clone**. Preservar o pacote antes de executar `mvn clean` ou apagar target. `node_modules`, dist, JAR, volumes Docker, configurações privadas e sessões também não são transportados pelo Git/ZIP.

## 4. Executar e conferir

### Mobile consumindo o Render

```powershell
Set-Location .\weg-benefits
npm ci
if (-not (Test-Path .env.local)) { Copy-Item .env.example .env.local }
npm run dev
```

Abrir **http://127.0.0.1:5173**. O `.env.example` já aponta para o Render:

```dotenv
VITE_API_URL=https://weg-benefits-api.onrender.com
VITE_API_PROXY_PATH=/backend
API_PROXY_TARGET=https://weg-benefits-api.onrender.com
```

Essas URLs são públicas. Nunca colocar senha/chave/token em VITE_*. No navegador local, o Vite usa proxy de mesma origem `/backend` para preservar a sessão. No Android, usa CapacitorHttp nativo e HTTPS. Publicar só dist em hospedagem estática não cria o proxy necessário para uma versão web hospedada.

### Conferir a API hospedada sem login

```powershell
$baseUrl = 'https://weg-benefits-api.onrender.com'
(Invoke-RestMethod "$baseUrl/actuator/health" -TimeoutSec 300).status
(Invoke-RestMethod "$baseUrl/actuator/health/readiness" -TimeoutSec 300).status
$spec = Invoke-RestMethod "$baseUrl/v1/api-docs" -TimeoutSec 300
$spec.components.schemas.CategoryPatchRequest.properties.name.minLength
```

Último resultado confirmado: UP, UP, 5. A primeira chamada pode demorar quando o serviço gratuito está inativo. Swagger: `/swagger-ui.html`. Rotas de negócio exigem sessão; abrir a raiz da API não é uma tela de cadastro.

### Backend local — somente se precisar

```powershell
Set-Location .\benefits
# Ajustar para o caminho REAL do JDK 21 da nova máquina:
$env:JAVA_HOME = 'C:\CAMINHO\DO\JDK-21'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$env:SERVER_PORT = '8081'
docker compose up -d postgres
docker compose ps
.\mvnw.cmd spring-boot:run
```

Esperar PostgreSQL healthy. Padrões exclusivos de desenvolvimento: localhost:5432, banco benefits, usuário benefits_dev, senha benefits_dev_only. Porta da API **8081**, sempre. Se existirem variáveis DB_URL/DB_USERNAME/DB_PASSWORD herdadas, conferir privadamente que não apontam para Neon antes de iniciar uma instância local. Não imprimir variáveis secretas.

Um volume Docker novo não contém o ADMIN nem o catálogo da outra máquina. Não provisionar um segundo ADMIN no banco remoto e não reenviar os lotes para preencher o banco local por engano. Cadastro/provisionamento exigem configuração Gmail e são operações separadas. Para trabalhar no Mobile contra o Render, basta usar a conta já existente.

## 5. Finalizar a correção de Lazer

O pacote inclui o operador local já preparado e testado:

```powershell
Set-Location .\benefits
.\target\corrigir-categoria-lazer.ps1
```

Ele confere OpenAPI minLength 5, solicita login e **senha atual do ADMIN do Render em prompt oculto**, valida papel e categoria, faz somente o PATCH necessário e verifica ID, ícone, cor, estado, criação e vínculos. Fecha apenas a sessão temporária que criou. Se o nome já for Lazer, não repete o PATCH. Interrompe se a categoria divergir. Não pede senha PostgreSQL/Gmail e não grava senhas/cookies em arquivos.

Destino exato já conferido:

```http
PATCH /v1/admin/categories/559285ef-2424-4237-ad43-460e35c4c052
Content-Type: application/json

{"name":"Lazer"}
```

Antes da mudança, havia **8 estabelecimentos ativos vinculados**. Confirmar o resultado real do operador; depois usar Atualizar/reconectar no app. Cache offline conserva o nome antigo até sincronizar. Nunca criar uma categoria nova para substituir essa, nem preencher o nome com palavras extras para satisfazer tamanho mínimo.

## 6. Catálogo: o que falta cadastrar

| Categoria correta | Ofertas do PDF | Benefícios com estabelecimento | Estabelecimentos ativos | Benefícios sem estabelecimento |
|---|---:|---:|---:|---:|
| Clientes WEG | 4 | 3 | 3 | 1 |
| Educação | 21 | 14 | 18 | 7 |
| Idiomas | 36 | 25 | 25 | 11 |
| Cursos e Treinamentos | 4 | 4 | 4 | 0 |
| Saúde e Bem-Estar | 30 | 10 | 10 | 20 |
| Lazer | 12 | 6 | 8 | 6 |
| Estética e Vestuário | 5 | 1 | 1 | 4 |
| Total do PDF | 112 | 63 | 69 | 49 |

O banco tem ainda um benefício/estabelecimento histórico de teste: total de 113 benefícios e 70 estabelecimentos ativos. Há 139 linhas administrativas de estabelecimento, sendo **69 cópias inativas** de uma carga duplicada. A correção dessas cópias foi executada e verificada; não excluir fisicamente nem reativá-las. UUIDs das primeiras criações ativas estão em `18_active_establishment_id_map.json`; a resposta do segundo POST contém IDs das cópias inativas e não deve orientar novos vínculos.

Existem também **24 unidades candidatas pendentes**. Elas não correspondem necessariamente aos 49 benefícios e alguns já têm outra unidade publicada. Não somar essas contagens. Ver [lista nominal dos 49 e diagnóstico](catalogo-cobertura-e-nomes.md) e o CSV do pacote.

### Materiais locais necessários

| Caminho em target/ | Uso |
|---|---|
| catalogo-pdf-fiel-2026-10-27/ | Manifesto das 112 ofertas, PDF original, 120 capturas, pesquisa/proveniência e aprovações |
| catalogo-estabelecimentos-render-2026-10-28/ids-render.json | UUIDs reais das sete categorias e 112 benefícios; chave histórica Lazer e Turismo ainda pode aparecer |
| catalogo-estabelecimentos-render-2026-10-28/18_active_establishment_id_map.json | UUIDs dos 69 estabelecimentos ativos originais |
| catalogo-estabelecimentos-render-2026-10-28/10_unidades-pendentes.csv | Motivos de pendência das 24 unidades candidatas |
| auditoria-catalogo-2026-10-30/ | Resumo e CSV dos 49 benefícios sem estabelecimento, referências/páginas/pendências e PATCH preparado |
| catalog-v9-deploy-verification.json | Evidência histórica do deploy e indicação de PATCH ainda não confirmado |
| corrigir-categoria-lazer.ps1 | Operador autenticado do nome, descrito acima |

Os READMEs dos pacotes antigos e campos como contractErrors retratam a fase anterior a V7/V8: **não usar a afirmação histórica de 40 textos incompatíveis como estado atual**. V7/V8 já resolveram textos longos e ofertas ONLINE. As fontes históricas continuam intactas para rastreabilidade. A nova preparação com V9 não é um lote incremental para reenviar.

Preparar uma **nova pasta de revisão**, sem chamadas à API:

```powershell
python .\scripts\prepare-catalog-load.py `
  --source .\target\catalogo-pdf-fiel-2026-10-27 `
  --ids .\target\catalogo-estabelecimentos-render-2026-10-28\ids-render.json `
  --output .\target\revisao-continuacao-nova
```

A pasta de saída deve ser nova. O relatório `09_catalog_coverage.json` evidencia cada benefício sem estabelecimento pronto, mesmo sem candidato. Última preparação: 69 prontos, 24 unidades pendentes, 49 benefícios sem estabelecimento pronto. Isso não prova importação. Não reenviar 01/02/06 antigos: categorias/benefícios e 69 estabelecimentos já existem. Pesquisar e revisar os dados faltantes, conferir o catálogo atual e preparar **somente os novos estabelecimentos completos**, reutilizando UUIDs reais.

Lotes HTTP têm limite de 100 itens e são atômicos por recurso, mas **não têm chave de idempotência**. Depois de timeout, consultar antes de repetir POST. Benefit começa inativo e ativa no primeiro vínculo; a desativação manual permanece.

## 7. Decisões de produto que devem ser preservadas

- Monólito modular: identity, catalog, engagement, common/config. Mobile/desktop acessam somente a API, prefixo `/v1`; Mobile não administra catálogo.
- ADMIN/COLLABORATOR apenas. ADMIN cria/edita contas, promove COLLABORATOR para ADMIN e desativa apenas COLLABORATOR. Não rebaixa/desativa ADMIN nem a própria conta. Primeiro ADMIN por CLI; não há endpoint de bootstrap, cadastro público, senha temporária, troca forçada ou recuperação de senha.
- Username único ASCII minúsculo, 3–75, letras/dígitos/`.`/`-`/`_`; e-mail único obrigatório somente gmail.com ou estudante.sesisenai.org.br. Senha 6–72, sem normalização, apenas hash PBKDF2 no banco. ID de usuário inteiro; IDs Catalog UUID.
- Sessão server-side persistida no PostgreSQL, sem expiração automática conforme decisão acadêmica; logout/desativação/revogação encerram acesso. Não trocar por JWT/OAuth por inferência. Fluxo CSRF dedicado ficou fora; risco conhecido. CORS permanece allowlist existente.
- Gmail API por HTTPS/OAuth gmail.send, remetente wegbenefits@gmail.com. Não voltar a SMTP/senha de app. Conta só persiste após aceitação pela Gmail API; falha avisa ADMIN e não cria conta. Aceitação não garante entrega final. Segredos OAuth ficam fora do Git/Mobile.
- Cada Establishment tem um Benefit; Benefit pode ser compartilhado por várias unidades. Benefit tem ao menos um Discount, value 2–15 e description obrigatória. Sem nome/título/conditions em Benefit. V5 arquivou condições antigas internamente.
- PHYSICAL/HYBRID exigem endereço/coordenadas completos; ONLINE fica sem endereço/coordenadas, aparece em lista/favoritos e fora do mapa (mappable=false). Mapa usa forMap=true ou filtra cache. ONLINE não aceita visita presencial.
- Sem horários, PostGIS, avaliações ou Brand nesta entrega. Ícones Iconify: baixar/cachear somente os selecionados. Imagem de fachada HTTPS opcional; sem upload implementado.
- Preservar redação do PDF, erros, datas, contatos e cupons. Corrigir só OCR para coincidir com a imagem. Não resumir, parafrasear ou preencher texto artificialmente. Clínica Care: somente PDF-080, 20% em Nutrição, Ana Paula Schreiner, página 46. Não agrupar outros profissionais por mencionarem Care em contato.
- Geoapify aprovada para geocodificar endereços do catálogo. Pesquisa limitada a Jaraguá do Sul e cidades próximas, além de unidades expressas na fonte. Conferir antes de persistir; 20 pares anteriores foram aprovados separadamente. Não inventar coordenadas/endereço, mudar PHYSICAL/HYBRID para ONLINE para contornar pendência, expor chave no Mobile ou enviar localização pessoal à Geoapify. Não trocar tiles/roteamento nem criar endpoint de geocodificação sem tarefa própria.
- Mobile mantém cache/fila por conta até reconexão, sem prazo. Não reenviar pendências sob outra conta; 401 bloqueia sincronização e oferece descarte. Visita após três minutos de permanência, evento final sem trilha GPS; chave estável estabelecimento+UTC e deduplicação de 30 minutos na API.

## 8. Autenticação e integração Mobile

Rotas principais implementadas: POST `/v1/auth/login`, POST `/v1/auth/logout`, GET `/v1/me`; administração `/v1/admin/users` e POST `/v1/admin/users/{id}/deactivation`; Catalog `/v1/categories`, `/v1/services`, `/v1/establishments`, detalhe/benefícios e famílias administrativas; Engagement `/v1/me/favorites` e `/v1/me/visits`. Contrato detalhado: [api-contract.md](api-contract.md) e [http-api.md](http-api.md).

Mobile implementa login real por username/e-mail, cookie, catálogo paginado, detalhes, favoritos, leitura do histórico, cache IndexedDB e fila de favoritos por conta, bloqueio em 401 e cache de ícones selecionados. API fica em `src/api`, estado/orquestração em `src/lib/siteState.svelte.ts`, adaptação em `apiAdapters.ts`, armazenamento em `src/storage/cache.ts`.

Documentação antiga de prévias/dados fictícios é histórica. O app conectado não deve usar fixtures de catalog.ts nas rotas reais. Na máquina anterior, o usuário confirmou login real e troca da senha no ambiente hospedado. **Android ainda não foi homologado**: sem APK/aparelho confirmado, localização atual, detector/envio automático de novas visitas, mapa base offline ou PWA. A regra dos três minutos está aprovada; raio, precisão, intervalo e desempate ainda precisam ser fechados para implementar o detector nativo.

A fila offline implementada é de favoritos. Não afirmar que já existe fila de novas visitas: o histórico lido é de visitas recebidas pela API. Navegador sem PWA exige que o app já esteja carregado para usar o cache; tiles/imagens externas podem faltar offline.

## 9. ADMIN, credenciais e PR 62

O usuário solicitou trocar a senha nos ambientes local e Render e informou conhecer a atual. A exceção operacional é um CLI separado, exigindo ADMIN ativo e senha atual, sem Spring/HTTP/Flyway, sem e-mail e sem endpoint de recuperação. Na execução **Render**, o operador recebeu “Senha alterada. Sessões anteriores removidas: 3” e confirmou que funcionou. **A troca local não teve sucesso confirmado.** O script de Lazer usa a nova senha atualmente válida do Render.

PR 62 continua aberta; seus arquivos não estão em develop. Para estudar/manter essa ferramenta, buscar a branch codex/admin-password-maintenance e ler docs/admin-password-maintenance.md nela. Não iniciar a API inteira de uma branch antiga para operar a senha nem criar outra conta/endpoint. O pacote inclui a documentação dessa PR como referência separada, não como código integrado.

Credencial PostgreSQL não é login ADMIN. Para operações remotas da manutenção, o projeto usou host direto Neon e role neondb_owner; copiar dados atuais pelo painel privado, sem senha em URL/argumento/chat. Uma senha de banco foi exposta na conversa e foi recomendada sua rotação junto de DB_PASSWORD no Render; **a rotação não foi confirmada**. Não recuperar/repetir o valor antigo do histórico.

Na nova máquina, entrar nas contas GitHub/Render/Neon/Google pelos meios normais. O ZIP não leva credenciais, sessões, arquivos .env privados, chaves Geoapify/Gmail ou banco local. A API hospedada já tem configuração; não precisa extrair segredos de produção para executar o Mobile contra ela. Configuração privada local, se necessária, deve vir dos responsáveis/painéis por canal seguro.

## 10. Validações já feitas e o que não afirmar

- Correção Catalog/V9: 16 testes Java passaram e um teste condicional da fixture local foi pulado; seis testes Python passaram. Upgrade e PATCH Lazer foram verificados em PostgreSQL local isolado benefits_catalog_test, preservando metadados/vínculos. Preparação real e comparação dos 112 textos exportados passaram sem reescrita dos campos comerciais conferidos.
- Deploy real: status live, Flyway V9 aplicada, health/readiness UP, OpenAPI minLength 5. Logs error consultados na janela do deploy retornaram vazios. A primeira chamada HTTP durante a troca de instâncias excedeu 45 segundos; após live os GETs passaram.
- Operador Lazer: cinco cenários simulados passaram tanto em PowerShell 7 quanto em Windows PowerShell 5. Não houve PATCH real confirmado.
- Mobile: npm ci/check/test/build e cap sync passaram na integração; 30 testes e ensaio com fixture de 70 estabelecimentos/offline/replay. Isso não comprova cookie/background em Android real.
- CLI ADMIN: testes transacionais isolados, build e prompts foram verificados na PR 62. A execução remota real foi feita pelo usuário, não pelo agente.

Testes de regressão usuais:

```powershell
# Backend, dentro de benefits:
python -B .\scripts\test_prepare_catalog_load.py
.\mvnw.cmd '-Dtest=CatalogServiceTests' test
# Mobile, dentro de weg-benefits:
npm run check
npm test
npm run build
```

Testes de persistência exigem configurar **somente a base local isolada** conforme os testes/documentos do repositório. Não apontar DB_URL/CATALOG_TEST_DB_URL para Neon/produção. O override `-Dspring.http.clients.imperative.factory=simple` foi usado nos testes de uma máquina Windows devido a uma falha de loopback do cliente JDK; não transformar isso em configuração de produção por inferência. Resultados acima são históricos; registrar os testes realmente executados na nova máquina.

## 11. Roteiro de retomada para o próximo agente

1. Conferir Git/PRs e ler AGENTS.md, README.md, docs/README.md, implementation-plan.md, decision-register.md, contrato e código/migrações afetados. Preservar diferenças locais e instruções mais novas.
2. Verificar health/OpenAPI e o nome atual de Lazer. Se ainda pendente, operador executa o script com senha privada no terminal. Conferir oito vínculos ou justificar uma mudança legítima posterior e sincronizar o Mobile.
3. Abrir auditoria/CSV dos 49 e pesquisa das 24 unidades; separar ofertas ainda sem qualquer candidato das unidades com campos/participação/pontos pendentes. Preservar escrita/fonte; confirmar dados antes de cadastrar somente novos estabelecimentos.
4. Revalidar contagens/textos e ausência de duplicatas. Só depois tratar a carga como concluída. Não confundir número de benefícios, estabelecimentos, unidades candidatas e cópias inativas.
5. Retomar homologação nativa Android e decisões da detecção de visitas. Backup/restauração independente ainda precisa de procedimento testado. Não estimar conclusão total apenas porque health está UP.

Fazer commits quando houver uma alteração coerente concluída/revisada; branches codex/ a partir de develop e PRs para develop. API local em 8081. Atualizar o diário e o contrato junto de alterações de comportamento. Não instalar ferramentas globalmente, alterar outros repositórios sem escopo, reescrever migrações aplicadas ou inferir decisões pendentes.

### Texto para colar na nova conversa

> Continue o projeto WEG Benefits a partir de docs/continuidade-2026-10-30.md e do pacote local que transferi. Leia primeiro AGENTS.md e a documentação exigida, inspecione Git e o código, e confira o estado remoto atual. Backend e Mobile são repositórios separados; trabalhe em branches codex/ e abra PRs para develop, com commits quando apropriados. Use porta 8081 para a API local. A API Render já publicou a V9; o PATCH do nome Lazer ainda não foi confirmado. Existem 49 benefícios sem estabelecimento ativo, incluindo 20 de Saúde; não é um corte de dez na UI. Primeiro confirme/corrija Lazer pelo fluxo autenticado preparado e depois retome a revisão das unidades ausentes, sem inventar localização, reescrever textos do PDF, reenviar a carga inteira ou reativar cópias. Não peça senhas no chat, não presuma que PRs abertas foram integradas e registre o que efetivamente validar.
