# Modelo de dados e DER — WEG Benefits

## Estado

Este documento registra a modelagem aprovada anteriormente e, abaixo, a nova revisão recebida em 2026-10-20, ainda em reconciliação. O código e as migrações continuam sendo a fonte do estado executado; conflitos com decisões aprovadas não são aplicados até revisão.

## Revisão recebida em 2026-10-20 — pendente de reconciliação

O documento de referência [`WEG Benefits — Modelagem revisada`](referencia/WEG-Benefits-Modelagem-revisada.pdf) propõe:

- IDs inteiros para Category, Establishment, Benefit, Discount e Service.
- Uma relação 1:1 entre Establishment e Benefit, e uma relação 1:N entre Benefit e Discount.
- Uma entidade Discount com `value` (2–15 caracteres), `description` (10–300 caracteres) e referência ao Benefit; `discount`, antes um campo textual em Benefit, deixa de aparecer como atributo direto.
- Ícone de Category selecionado entre opções oferecidas pelo próprio sistema.
- Username descrito como texto de 3–75 caracteres com apenas letras minúsculas; Estado como texto de 2–75 caracteres.

Esta revisão diverge das decisões anteriores D-022, D-036, D-040 e D-042, além do modelo de Benefit implementado em V2. O novo material foi preservado, mas não foi tratado como autorização para alterar silenciosamente essas decisões. Consulte P-017 a P-022 em `decision-register.md` antes de uma migração de reconciliação.

### Estado implementado antes da reconciliação

- A migração `V2__catalog.sql` já define Category, Service, Establishment e Benefit com UUID; Benefit possui um campo textual `discount`, e vários benefícios podem pertencer ao mesmo estabelecimento.
- A migração aplicada nunca deve ser reescrita. Mudanças confirmadas deverão ser feitas em migração Flyway nova, preservando dados e considerando a quebra de IDs usada pela API/clientes.

## Catalog aprovado para a primeira versão

- **Category:** identificador UUID gerado pelo sistema (D-042), nome (6–75 caracteres), identificador de ícone escolhido no Iconify, cor escolhida de uma paleta definida pelo projeto, estado ativo.
- **Establishment:** identificador UUID; nome (2–75); descrição (50–300); endereço (CEP, número, rua, bairro, cidade e UF); latitude/longitude e altitude conforme a modelagem revisada; URL HTTPS opcional da fachada; estado ativo; pertence a uma Category.
- **Benefit:** identificador UUID; público, instruções de participação (50–300), contato, desconto textual e condições (10–300), todos obrigatórios (D-041); estado ativo e timestamps do sistema. Não tem nome/título. Cada Benefit pertence a exatamente um Establishment; um Establishment pode ter vários Benefits. Limites máximos de público, contato e desconto continuam pendentes (P-007); a migração usa `TEXT` para esses três campos.
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
Establishment 1 ─── N Benefit
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
