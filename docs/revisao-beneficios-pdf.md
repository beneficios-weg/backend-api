# Revisão do catálogo a partir do PDF — 2026-10-27

## Objetivo e alcance

Preparar as ofertas do documento fornecido pelo usuário para o contrato atual de Benefit e
pesquisar unidades físicas relacionadas. O usuário delimitou a pesquisa de redes a **Jaraguá do
Sul e cidades próximas**. Foram priorizados também Guaramirim, Schroeder, Corupá, Massaranduba,
Joinville, Pomerode e Blumenau. Cidades citadas expressamente no documento e unidades da revisão
anterior foram mantidas como referências complementares, sem presumir uma busca nacional completa.

Esta etapa prepara dados. Nenhum registro foi criado/alterado na API, nenhum banco foi acessado,
nenhuma migração foi aplicada e nenhum comportamento da aplicação foi modificado.

## Resultado

| Item | Resultado |
|---|---|
| Documento | 59 páginas; conteúdo principal em capturas de tela |
| Extração | 120 capturas, sendo 6 menus e 114 ocorrências de cartões |
| Consolidação | 2 cartões repetidos consolidados: Uningá e Vila Encantada |
| Ofertas detalhadas | 112 payloads com formato válido, em lotes de 100 e 12 |
| Referências somente no índice | Construthema e MBA ECA USP, sem payload inventado |
| Categorias | 7 categorias da carga anterior preservadas |
| Pesquisa de unidades | 41 unidades/candidatos; 34 ofertas detalhadas têm alguma unidade candidata |
| Modalidade online | 52 ofertas classificadas como online na fonte/revisão |
| Geocodificação nova | 25 respostas HTTP 200; 20 sugestões com correspondência de cidade, rua e número |
| Pontos anteriores | 9 pontos preservados; campus Florianópolis continua adiado |

As 41 unidades incluem dez registros da revisão anterior e a Construthema, cujo benefício não tem
regras suficientes no PDF. Uma unidade pode aparecer relacionada a mais de uma oferta no material
de pesquisa; isso sinaliza uma pendência de representação e não autoriza criar vínculos incompatíveis
com o modelo. O número não significa 41 estabelecimentos prontos para importação.

## Arquivos preparados

O pacote portátil local é `target/catalogo-beneficios-pdf-revisado-2026-10-27.zip`.
Os arquivos descompactados estão em `target/catalogo-pdf-2026-10-27/`:

- `README.md`: ordem de uso, limites dos lotes e leitura dos arquivos no PowerShell.
- `00_benefits_manifest.json`: origem, páginas, capturas, OCR bruto, payload normalizado,
  referências estáveis `PDF-001` a `PDF-112`, divergências e pesquisa por oferta.
- `01_categories_batch.json`: categorias anteriores, para reutilização ou criação apenas das ausentes.
- `02_benefits_batch_01.json` e `02_benefits_batch_02.json`: corpos de criação com 100 e 12 ofertas.
  Os respectivos índices relacionam posição, nome do parceiro e categoria com o UUID a ser retornado.
- `03_establishments_research_not_api_payload.json`: unidades, fontes, endereços,
  sugestões separadas das coordenadas finais e pendências.
- `04_benefits_review.csv`, `06_catalogo-revisao.md` e `07_establishments_review.csv`:
  conferência humana de todas as ofertas e unidades, com links de fontes e mapa.
- `05_geocoding_summary.json` e `geoapify/`: respostas de geocodificação e atribuições.
- `08_pendencias.md`: revisão comercial e do modelo, incluindo casos sem ponto confirmado.
- `09_import_mapping_template.json`: anotações dos UUIDs reais, sem gerar IDs fictícios.
- `10_establishment_drafts_not_api_payload.json`: rascunhos com campos ausentes e bloqueios explícitos.
- `11_validation.json`: resultado real das verificações locais.
- `fontes/`: cópia idêntica do PDF, suas capturas, origem anterior de estabelecimentos e aprovação
  anterior das coordenadas. Os arquivos anteriores não foram sobrescritos.

Os dados detalhados e cupons do documento permanecem em artefatos locais ignorados pelo Git.
Somente este relatório, o índice documental e o diário são versionados. Copiar o ZIP para um local
escolhido pelo usuário antes de executar `mvn clean`, que remove `target/`.

## Adequação ao contrato

Os lotes contêm apenas `audience`, `participationInstructions`, `contact` e
`discounts[].value/description`. Não há `conditions`, nome de parceiro, categoria nem IDs de unidade
no payload de Benefit. Os nomes ficam no manifesto e nos índices de importação.

Percentuais, públicos, comprovantes, cupons e restrições foram preservados ou reescritos sem ampliar
a oferta. Os destaques dos descontos respeitam 2–15 caracteres; descrições, 10–300; instruções,
50–300. Instruções muito curtas receberam uma introdução neutra. Erros de OCR em cupons, contatos
e links identificados foram conferidos nas capturas, mantendo o texto bruto para rastreabilidade.
As duas ocorrências idênticas foram consolidadas; versões com termos diferentes permaneceram separadas.

Cada lote usa `POST /v1/admin/benefits/batch` e é atômico individualmente. Não há upsert ou
idempotência de criação. Consultar os dados existentes antes de enviar; após timeout, conferir a
listagem antes de repetir. Os UUIDs retornados devem ser associados aos índices na mesma ordem.
Os rascunhos de estabelecimentos **não são payloads válidos**: faltam UUIDs e, em alguns casos,
campos ou decisões. Não enviar esses arquivos diretamente à API.

## Exemplos de fontes de unidades

| Parceiro/unidade | Evidência consultada | Situação |
|---|---|---|
| KNN Jaraguá, Guaramirim e Massaranduba | Páginas oficiais da rede | Endereços encontrados; conferir pontos e participação/serviços locais |
| UniCesumar Jaraguá | Página oficial do polo | Endereço e CEP encontrados |
| Uningá Jaraguá | Lista oficial de polos | Endereço encontrado; duas campanhas divergentes no PDF |
| Four Bridges Joinville | Contato oficial da escola | Endereço completo encontrado |
| My Target Joinville | Perfil institucional e diretório de navegação | Unidade Costa e Silva identificada |
| Care Jaraguá | PDF do usuário, guia institucional de parceiros e espelho CNES | Seis ofertas de profissionais na mesma clínica; vínculo exige revisão do modelo |
| Karine Scoz | CNES público, com telefone coincidente | Consultório identificado; sem associar por inferência à Casa Barão |
| VitalVille Joinville | Contato oficial | Endereço completo coincide com o PDF |
| Grupo Ethernos | Lista oficial de unidades | Escritórios de Guaramirim e Massaranduba identificados |
| Construthema Jaraguá | Site oficial e política de privacidade | Unidade encontrada; faltam regras do benefício no PDF |

Fontes verificáveis: [KNN Jaraguá](https://www.knnidiomas.com.br/unidades/knn-jaragua-do-sul),
[KNN Guaramirim](https://www.knnidiomas.com.br/unidades/knn-guaramirim),
[KNN Massaranduba](https://www.knnidiomas.com.br/unidades/knn-massaranduba-02),
[UniCesumar](https://www.unicesumar.edu.br/ead/polo/jaragua-do-sul/),
[Uningá](https://uninga.br/polos/page/20/),
[Four Bridges](https://www.fourbridges.com.br/contato/),
[My Target](https://www.linkedin.com/school/mytargetidiomasetecnologia/),
[guia institucional com Care](https://www.planoleier.com.br/GUIA_PARCEIROS_2025.pdf),
[CNES Karine Scoz](https://cnes2.datasus.gov.br/Mod_Conjunto.asp?VCo_Unidade=4208900558001),
[VitalVille](https://vitalville.com.br/contato/),
[Grupo Ethernos](https://grupoethernos.com.br/unidades/) e
[Construthema](https://www.construthema.com.br/).
O inventário local registra as fontes específicas e limitações das demais unidades.

Encontrar um endereço não confirma que a unidade aceite o convênio. Guias de outros planos
foram usados somente para identificação/localização, sem transplantar seus descontos para o WEG.
Endereços residenciais sem evidência de atendimento público, homônimos e sedes administrativas
de serviços online não foram convertidos em estabelecimentos.

## Pendências preservadas

1. **Online:** o modelo público atual depende de Establishment com endereço e coordenadas.
   Ofertas online podem ser cadastradas inativas, mas sua publicação exige uma decisão própria.
   Não foram criados pontos fictícios para contornar essa limitação.
2. **Care e Uningá:** o material reúne ofertas diferentes para a mesma unidade. O modelo atual
   admite exatamente um Benefit por Establishment. Não foram duplicadas clínicas, escolhidas
   campanhas ou fundidas regras sem aprovação.
3. **Vigência:** cupons Wanke citam 2025; MBA EACH tem prazo 25/06 sem ano; MBA USP/Esalq menciona
   segundo semestre de 2026. Não presumir que sejam atuais. Uningá e Verbalize mantêm versões divergentes.
4. **Índice incompleto:** faltam os cartões com regras de Construthema e MBA ECA USP. MBA EACH USP
   é outra referência e não foi usado como substituto de MBA ECA USP.
5. **Endereços:** Elian tem locais distintos em sites do grupo; confirmar retirada. Travelum tem
   endereços diferentes no site próprio e no portal municipal. Uninter possui referências antigas;
   Malwee exige confirmação da participação da loja de fábrica. Algumas fontes Univali divergem
   em rua, CEP ou número; preservada a revisão anterior sem reconciliar por suposição.
6. **Campos:** complementar CEP/bairro/número onde ausentes e UUIDs de relações. Complementos de
   sala foram preservados nos metadados, mas o DTO não tem campo próprio. Não converter km em número predial.
7. **Coordenadas:** as 20 sugestões novas aguardam conferência. A aprovação anterior permanece
   limitada aos pontos já confirmados pelo usuário; nenhuma aprovação foi estendida implicitamente.

## Validação real

Executados scripts locais de extração PDF, OCR nativo do Windows, normalização, pesquisa,
geocodificação e montagem do pacote. `target/package-pdf-catalog.local.py` retornou `PASS` para:
estrutura/limites dos 112 payloads, lotes de 100/12, cobertura das 120 capturas, referências
bidirecionais benefício–unidade, coordenadas dentro dos limites, JSONs legíveis, integridade do ZIP,
preservação das origens por SHA-256 e ausência da chave Geoapify nos arquivos e no ZIP.
`target/validate-pdf-package.local.py` também retornou `PASS` para as sete categorias,
limites de nome/descrição dos 41 rascunhos e 132 links relativos. `git diff --check` passou.

A primeira tentativa de rede no ambiente restrito produziu 24 falhas sem resposta HTTP; as consultas
foram refeitas com acesso à rede, obtendo 25 respostas HTTP 200 no total, incluindo um endereço
adicional. As falhas iniciais foram preservadas no pacote. Não houve teste HTTP da aplicação,
build, teste automatizado de código, escrita no banco ou deploy nesta etapa.

SHA-256 do PDF: `334c690060576b0e1f25a96ca2f9c980b819a0b3f3164f29a2919584e5a816cd`.
SHA-256 da origem anterior de estabelecimentos, inalterada:
`05c7858f540bb0b65cc7872cf6271542c7b03c78ac2bf107d80019a112ca1557`.

## Próxima etapa

Revisar primeiro as campanhas e os casos online/mesma clínica; conferir os novos pontos.
Depois mapear UUIDs e completar os campos do subconjunto aprovado para gerar o lote final de
estabelecimentos. Alterar o modelo da API ou executar a carga são tarefas posteriores.
