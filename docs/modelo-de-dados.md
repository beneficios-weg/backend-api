# Modelo de dados e DER — WEG Benefits

## Estado

Este documento registra as decisões de modelagem e distingue o projeto aprovado do que o código já implementa. A reconciliação foi implementada em código e na migração V3; a migração não foi executada neste trabalho.

## Revisão recebida em 2026-10-20 — decisões reconciliadas

O documento de referência [`WEG Benefits — Modelagem revisada`](referencia/WEG-Benefits-Modelagem-revisada.pdf) propõe:

- IDs inteiros para Category, Establishment, Benefit, Discount e Service.
- Uma relação 1:1 entre Establishment e Benefit, e uma relação 1:N entre Benefit e Discount.
- Uma entidade Discount com `value` (2–15 caracteres), `description` (10–300 caracteres) e referência ao Benefit; `discount`, antes um campo textual em Benefit, deixa de aparecer como atributo direto.
- Ícone de Category selecionado entre opções oferecidas pelo próprio sistema.
- Username descrito como texto de 3–75 caracteres com apenas letras minúsculas; Estado como texto de 2–75 caracteres.

Decisões da equipe após a revisão: IDs do catálogo continuam UUID (incluindo a nova entidade Discount); cada Establishment tem um único Benefit, e cada Benefit pode pertencer a no máximo um Establishment. Benefit pode ser cadastrado sem estabelecimento para seguir o fluxo da tela; o benefício começa inativo e ativa no primeiro vínculo. O usuário confirmou que benefício é exclusivo de um estabelecimento; a legenda plural “Estabelecimentos vinculados” do protótipo diverge dessa decisão e deve ser ajustada no cliente. Cada Benefit exige pelo menos um Discount; `value` e `description` são obrigatórios. Establishment exige Category ativa e Benefit existente com Discount. Iconify permanece como fonte de ícones; username mantém D-022; Estado permanece como sigla UF. A relação e o campo textual antigos são substituídos.

### Estado implementado antes da reconciliação

- A migração `V2__catalog.sql` define Category, Service, Establishment e Benefit com UUID e mantém o campo antigo `discount`.
- A migração `V3__benefit_discounts_and_exclusive_establishment.sql` remove o campo antigo, cria `benefit_discount`, adiciona `establishment.benefit_id` obrigatório e único e registra `first_linked_at`. Benefit pode existir antes de qualquer estabelecimento; o FK obrigatório no lado Establishment garante a associação no banco. V3 aborta se encontrar qualquer linha em Establishment ou Benefit, pois não há mapeamento sem perda do desconto textual legado nem associação segura para dados existentes. Nenhuma migração já aplicada foi editada.

## Catalog aprovado para a primeira versão

- **Category:** identificador UUID gerado pelo sistema (D-042), nome (6–75 caracteres), identificador de ícone escolhido no Iconify, cor escolhida de uma paleta definida pelo projeto, estado ativo.
- **Establishment:** identificador UUID; nome (2–75); descrição (50–300); endereço (CEP, número, rua, bairro, cidade e sigla UF); latitude/longitude e altitude conforme a modelagem revisada; URL HTTPS opcional da fachada; estado ativo; pertence a uma Category ativa e exige um Benefit.
- **Benefit:** identificador UUID; público, instruções de participação (50–300), contato e condições (10–300), obrigatórios (D-041); estado ativo e timestamps do sistema. Não tem nome/título. Cada Benefit possui pelo menos um Discount, pode estar sem estabelecimento antes da associação e fica exclusivo de um estabelecimento quando vinculado. Começa inativo e ativa no primeiro vínculo. Limites máximos de público e contato continuam pendentes (P-007).
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
Establishment (exige 1 Benefit) ─── (0..1 Establishment por Benefit) Benefit 1 ─── N Discount
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
