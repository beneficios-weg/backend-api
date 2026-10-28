# Carga do catálogo e parceiros online

## Contrato da revisão de 2026-10-27

Cada estabelecimento continua com exatamente um benefício; um benefício pode ser compartilhado.
O canal pertence ao estabelecimento: PHYSICAL, ONLINE ou HYBRID. O padrão de criação é PHYSICAL,
preservando clientes anteriores e registros existentes. ONLINE não recebe endereço nem coordenadas;
PHYSICAL/HYBRID exigem localização completa. Não usar um endereço fictício para cadastrar oferta online.

A lista comum inclui todos os canais. A tela Mobile fornece ao mapa somente resultados de
`GET /v1/establishments?forMap=true` (paginar até o fim) ou filtra `mappable=true` no cache.
`Map.svelte` continua sendo apresentação; adaptar o serviço/tela de origem dos dados no repositório Mobile.
`channel=ONLINE` permite uma lista só de parceiros online. Favoritos são permitidos; visitas a ONLINE
retornam 400. A API continua recebendo somente o evento final de visita, sem trilha GPS.

`PATCH` para ONLINE limpa a localização na mesma transação; retornar a físico/híbrido exige localização
completa no payload. Não se apagam favoritos/visitas históricos nessa mudança. Ativação do benefício
no primeiro vínculo, desativação manual e categoria ativa seguem as regras anteriores.

V7 amplia instruções e descrições para TEXT obrigatório/não vazio, e nome do estabelecimento para
TEXT com mínimo de 2. V8 adiciona canais com constraints condicionais de localização. As migrações
V1–V6 são preservadas. UF mantém CHAR(2) no banco e seu mapeamento JPA foi alinhado. `Discount.value`
continua com 2–15 caracteres. Não há normalização, preenchimento ou correção dos textos comerciais.

## Preparar arquivos sem fazer importação

O preparador usa somente Python 3 e biblioteca padrão. Não instala dependências, acessa serviços ou
altera arquivos de origem. A saída deve ser uma pasta nova. Os documentos reais e dados preparados
ficam em `target/` (ignorado pelo Git); copie o pacote para outra máquina antes de limpar essa pasta.

```powershell
python .\scripts\prepare-catalog-load.py `
  --source .\target\catalogo-pdf-fiel-2026-10-27 `
  --output .\target\catalogo-carga-v8-2026-10-27
```

Arquivos produzidos:

- `01_categories_batch.json`: categorias propostas; reutilizar categorias já existentes quando aplicável.
- `02_benefits_batch_001.json` e `002`: 112 benefícios, em lotes de 100 e 12, com texto literal.
- `03_benefit_response_index.json`: referência PDF → arquivo de lote → posição da resposta (base zero).
- `04_ids_template.json`: mapa a preencher com UUIDs reais de categorias e benefícios.
- `05_establishments_review_not_api_payload.json`: rascunhos de parceiros online/unidades pesquisadas e pendências.
- `06_establishments_batch_*.json`: somente itens completos após fornecer mapa de UUIDs reais.
- `07_establishment_response_index.json`: referências dos itens incluídos em 06 e posição das respostas.
- `08_validation.json`: contagens e declaração de que não houve importação.

Na preparação inicial, não há lote 06: faltam os UUIDs retornados pela API. Não enviar 03/04/05/07/08
a nenhum endpoint. Não gerar UUIDs aleatórios para preencher referências. Criar categoria/benefício
antes do estabelecimento não publica a oferta: Benefit começa inativo e ativa no primeiro vínculo.

Após importar conscientemente categorias/benefícios na versão V8, guardar respostas. O endpoint
`POST /v1/admin/benefits/batch` retorna um array na mesma ordem enviada; usar 03 para associar cada
UUID à referência PDF. Um exemplo de transformação **local** de respostas já salvas:

```powershell
$pasta = '.\target\catalogo-carga-v8-2026-10-27'
$mapa = Get-Content "$pasta\04_ids_template.json" -Raw | ConvertFrom-Json
$indice = Get-Content "$pasta\03_benefit_response_index.json" -Raw | ConvertFrom-Json
foreach ($lote in ($indice | Group-Object batchFile)) {
  # Salve cada resposta HTTP com o mesmo nome do lote, trocando .json por .response.json.
  $arquivoResposta = $lote.Name.Replace('.json', '.response.json')
  $resposta = @(Get-Content "$pasta\$arquivoResposta" -Raw | ConvertFrom-Json)
  if ($resposta.Count -ne $lote.Count) { throw 'Resposta não corresponde ao lote' }
  foreach ($item in $lote.Group) {
    $mapa.benefits.($item.reference) = $resposta[$item.responseIndex].id
  }
}
# Preencher também mapa.categories com os UUIDs reais de cada categoria, pelo nome exato.
$mapa | ConvertTo-Json -Depth 20 | Set-Content "$pasta\ids-reais.json" -Encoding UTF8
python .\scripts\prepare-catalog-load.py `
  --source .\target\catalogo-pdf-fiel-2026-10-27 `
  --ids "$pasta\ids-reais.json" --output .\target\catalogo-carga-com-ids
```

Enviar somente os wrappers 01/02/06 aos endpoints `/v1/admin/categories/batch`, `/benefits/batch`
e `/establishments/batch`, respectivamente, com sessão ADMIN. Os lotes são atômicos por requisição,
não entre endpoints. Não repetir POST após timeout sem consultar a API: criação em lote não tem
chave de idempotência. Conferir respostas e catálogo existente para evitar duplicatas.

## Conferência de UTF-8 no PowerShell

Uma resposta lida pelo Windows PowerShell pode apresentar `EducaÃ§Ã£o` onde a fonte tem
`Educação`. Isso exige conferir os bytes HTTP antes de concluir que o texto no banco foi alterado.
Preservar respostas anteriores e não reenviar POSTs para tentar corrigir acentos.

Salvar a resposta de um GET diretamente em arquivo e lê-la explicitamente como UTF-8:

```powershell
Invoke-WebRequest `
  -Uri "$baseUrl/v1/admin/categories?page=0&size=100" `
  -WebSession $benefitsSession -UseBasicParsing -TimeoutSec 300 `
  -OutFile '.\target\categorias-http-utf8.json'
$categorias = Get-Content '.\target\categorias-http-utf8.json' -Raw -Encoding UTF8 | ConvertFrom-Json
```

Fazer a mesma conferência dos benefícios, paginando com `size=100`. Comparar os campos pelos UUIDs
retornados nos lotes e pelo índice de referências do PDF. Corrigir somente a leitura de uma cópia
local se os bytes confirmarem isso; qualquer alteração de dados armazenados exige diagnóstico próprio.
Com os UUIDs ASCII intactos, é possível preparar referências sem normalizar textos comerciais.
Arquivos preparados não equivalem a cadastros concluídos.

Ao salvar respostas de POST, `Invoke-WebRequest -InFile <payload> -OutFile <resposta>` também
preserva os bytes para leitura posterior com `Get-Content -Encoding UTF8 | ConvertFrom-Json`.
Isso não torna o POST idempotente: depois de timeout, conferir o catálogo antes de repetir.

## Pendências preservadas na pesquisa

O pacote contém 52 rascunhos online e 41 unidades pesquisadas; isso não confirma que todas as
unidades participem do convênio nem cobre todos os parceiros físicos/híbridos do PDF. Endereço,
participação e UUIDs ausentes ficam explícitos em 05, sem fabricar dados. Os 20 pares novos aprovados
permanecem distintos da aprovação de importação. A Care recebe somente PDF-080, 20% em Nutrição.
Uningá tem duas campanhas para a mesma unidade; não escolher uma ou duplicar a unidade por inferência.
Elian/Travelum mantêm divergências de endereço. Construthema e MBA ECA USP não ganham ofertas inventadas.
Datas/cupons e erros de escrita da fonte permanecem; não houve validação comercial de vigência.

## Executar testes isolados no PowerShell

Usar exclusivamente um banco descartável chamado `benefits_catalog_test`, nunca Neon ou a base de
desenvolvimento com registros reais. O nome `benefits-catalog-test` abaixo deve estar livre.

```powershell
docker run -d --name benefits-catalog-test `
  -e POSTGRES_DB=benefits_catalog_test -e POSTGRES_USER=benefits_test `
  -e POSTGRES_PASSWORD=benefits_test_only -p 127.0.0.1:55439:5432 postgres:17-alpine
docker exec benefits-catalog-test pg_isready -U benefits_test -d benefits_catalog_test
$env:DB_URL = 'jdbc:postgresql://127.0.0.1:55439/benefits_catalog_test'
$env:DB_USERNAME = 'benefits_test'
$env:DB_PASSWORD = 'benefits_test_only' # Exclusiva do banco temporário de teste.
$env:CATALOG_TEST_DB_URL = $env:DB_URL
# Opcional: validar e persistir os 112 rascunhos locais na mesma suíte.
$env:CATALOG_SOURCE_FIXTURE = (Resolve-Path '.\target\catalogo-pdf-fiel-2026-10-27\02_benefits_literal_drafts_not_api_payload.json').Path
.\mvnw.cmd test
python -B -m unittest discover -s scripts -p 'test_*.py'
# Remova somente o contêiner de teste criado acima, após conferir o nome.
docker rm -f benefits-catalog-test
Remove-Item Env:CATALOG_TEST_DB_URL, Env:CATALOG_SOURCE_FIXTURE -ErrorAction SilentlyContinue
# Restaurar DB_URL/DB_USERNAME/DB_PASSWORD do ambiente escolhido antes de iniciar a API.
```

Os testes Catalog com PostgreSQL são condicionados à variável `CATALOG_TEST_DB_URL` apontando ao
banco de teste; sem fixture local, apenas a verificação dos 112 rascunhos é omitida. Testes unitários
usam dados fictícios. A suíte cobre autorização, HTTP, transação de lote, canais/mapa, texto literal,
visitas online e atualização V6→V8 preservando registros. Não envia e-mails.

Neste Windows, o JDK precisou de caminho curto para sockets temporários. Se ocorrer
`Unable to establish loopback connection`, usar a pasta target existente:

```powershell
$socketDir = (Resolve-Path '.\target').Path.Replace('\', '/')
.\mvnw.cmd "-DargLine=-Djdk.net.unixdomain.tmpdir=$socketDir" test
```
