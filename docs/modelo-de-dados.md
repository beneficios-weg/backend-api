# Modelo de dados e DER — WEG Benefits

## Estado

Este documento registra as decisões de modelagem e distingue o projeto aprovado do que o código já implementa. O código e as migrações continuam sendo a fonte do estado executado; a reconciliação aprovada em 2026-10-20 ainda não foi migrada para o código.

## Revisão recebida em 2026-10-20 — decisões reconciliadas

O documento de referência [`WEG Benefits — Modelagem revisada`](referencia/WEG-Benefits-Modelagem-revisada.pdf) propõe:

- IDs inteiros para Category, Establishment, Benefit, Discount e Service.
- Uma relação 1:1 entre Establishment e Benefit, e uma relação 1:N entre Benefit e Discount.
- Uma entidade Discount com `value` (2–15 caracteres), `description` (10–300 caracteres) e referência ao Benefit; `discount`, antes um campo textual em Benefit, deixa de aparecer como atributo direto.
- Ícone de Category selecionado entre opções oferecidas pelo próprio sistema.
- Username descrito como texto de 3–75 caracteres com apenas letras minúsculas; Estado como texto de 2–75 caracteres.

Decisões da equipe após a revisão: IDs do catálogo continuam UUID (incluindo a nova entidade Discount); cada Establishment terá exatamente um Benefit; cada Benefit terá pelo menos um Discount; `value` e `description` são obrigatórios. Benefícios e descontos são criados/editados na mesma tela própria antes de serem associados; o benefício começa inativo e ativa na primeira associação a um estabelecimento. A tela mostra estabelecimentos vinculados (plural), sugerindo que um Benefit possa ser compartilhado por mais de um Establishment; esse lado da cardinalidade ainda aguarda confirmação em P-026. Um Establishment exige Category e Benefit existentes, e o Benefit deve ter Discount. Iconify permanece como fonte de ícones; username mantém D-022; Estado permanece como sigla UF. A relação anteriormente aprovada em D-036 e o campo textual `benefit.discount` são substituídos.

### Estado implementado antes da reconciliação

- A migração `V2__catalog.sql` já define Category, Service, Establishment e Benefit com UUID; Benefit possui um campo textual `discount`, e ainda permite vários benefícios por estabelecimento. A nova entidade Discount não está implementada.
- A migração aplicada nunca deve ser reescrita. Mudanças confirmadas deverão ser feitas em migração Flyway nova, preservando dados e considerando a quebra de IDs usada pela API/clientes.

## Catalog aprovado para a primeira versão

- **Category:** identificador UUID gerado pelo sistema (D-042), nome (6–75 caracteres), identificador de ícone escolhido no Iconify, cor escolhida de uma paleta definida pelo projeto, estado ativo.
- **Establishment:** identificador UUID; nome (2–75); descrição (50–300); endereço (CEP, número, rua, bairro, cidade e sigla UF); latitude/longitude e altitude conforme a modelagem revisada; URL HTTPS opcional da fachada; estado ativo; pertence a uma Category e exige um Benefit.
- **Benefit:** identificador UUID; público, instruções de participação (50–300), contato e condições (10–300), obrigatórios (D-041); estado ativo e timestamps do sistema. Não tem nome/título. Cada Benefit possui pelo menos um Discount; começa inativo e ativa quando associado pela primeira vez a um Establishment. A possibilidade de associação do mesmo Benefit a vários Establishments aguarda P-026. Limites máximos de público e contato continuam pendentes (P-007); V2 ainda guarda o desconto legado em `TEXT` até a migração de reconciliação.
- **Discount:** identificador UUID conforme o padrão confirmado para o catálogo; cada Discount pertence a exatamente um Benefit. Valor textual de 2–15 caracteres e descrição textual de 10–300 caracteres; a cardinalidade mínima de um por Benefit foi confirmada.
- **Service:** identificador UUID; nome único (3–75); estado ativo. Establishment e Service têm relação muitos-para-muitos.

As linhas inativas são preservadas e excluídas das consultas normais do catálogo. Não há exclusão física de estabelecimentos, benefícios, categorias ou serviços nesta etapa. Os vínculos históricos devem permanecer íntegros.

## Ícones e uso offline

Category persiste a chave do ícone, por exemplo `mdi:store`, não uma URL de imagem ou um arquivo SVG arbitrário. O seletor administrativo pesquisa e seleciona no Iconify. Durante a sincronização online do catálogo, o mobile baixa apenas os ícones selecionados ainda ausentes e os mantém em cache local. Um ícone selecionado após a última sincronização só estará disponível offline depois de ser sincronizado. Os clientes devem respeitar as licenças das coleções utilizadas.

## Imagens e horários

- A fachada é opcional e representada por URL HTTPS. A API não recebe nem armazena arquivos nesta versão. Para visualização offline, o cliente precisa baixar a imagem quando online e mantê-la no cache.
- Horários de funcionamento não são armazenados, consultados ou retornados pela primeira versão da API. Reabrir esta decisão em etapa futura caso o recurso entre no escopo.

## Cardinalidades

```text
Category 1 ─── N Establishment
Establishment N ─── 1 Benefit 1 ─── N Discount (lado Establishment→Benefit pendente P-026)
Establishment N ─── N Service
```

Favoritos e visitas pertencem ao módulo Engagement, ainda não implementado. Quando introduzidos, referências históricas devem continuar apontando para estabelecimentos desativados; a política de reenvio offline de visita será fechada antes de Engagement.

## Fora do escopo ou ainda pendente

- Brand/franquia não entra na primeira versão sem necessidade aprovada.
- PostGIS e busca/ordenação espacial seguem pendentes (P-006); latitude e longitude do catálogo não obrigam esta extensão para CRUD.
- O uso de altitude no algoritmo de estabelecimento dominante precisa de regra própria antes de Discovery.
- Tamanhos e nulabilidade de campos sem limite explícito no documento de modelagem devem ser consolidados no contrato de Catalog antes da migração correspondente.
- Não há entidades para avaliação, comentário, pagamento ou trilha contínua de GPS.

## Divergência histórica de Identity

O documento de modelagem recebido descrevia username somente com letras minúsculas. A decisão vigente em Identity aceita ASCII `a-z`, dígitos, `.`, `-` e `_`, conforme `AGENTS.md` e D-022. Esta divergência não altera o modelo de Catalog.
