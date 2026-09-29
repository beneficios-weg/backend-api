# Relatório da consolidação documental

Data: **29/09/2026**

Branch: `docs/consolidacao-projeto`

## Arquivos criados

- `AI_CONTEXT.md`;
- `docs/visao-geral.md`;
- `docs/estado-atual.md`;
- `docs/arquitetura-backend.md`;
- `docs/modelo-de-dados.md`;
- `docs/contrato-api.md`;
- `docs/autenticacao.md`;
- `docs/visitas-e-geolocalizacao.md`;
- `docs/integracao-mobile.md`;
- `docs/testes-backend.md`;
- `docs/decisoes-abertas.md`;
- `docs/relatorio-consolidacao.md`.

## Arquivo atualizado

- `README.md` — substituição do título isolado por objetivo, limites,
  inventário resumido e índice de documentação.

## Conflitos e inconsistências encontrados

### 1. Backend indicado como fonte de verdade sem contrato publicado

1. **Comportamento documentado:** o Backend define endpoints, payloads, códigos
   HTTP, autenticação, validações e modelo de dados.
2. **Comportamento encontrado:** o commit inicial possuía somente um README de
   uma linha e nenhum artefato de contrato ou implementação.
3. **Impacto:** o Mobile não consegue integrar sem assumir comportamentos.
4. **Recomendação:** aprovar e versionar modelo, autenticação e contrato mínimo
   antes da integração.

Status: **DECISÃO PENDENTE**.

### 2. Exemplos de rotas no briefing do Mobile

1. **Comportamento documentado:** o briefing cita exemplos como
   `GET /categories`, `GET /establishments` e `GET /visits`.
2. **Comportamento encontrado:** nenhuma dessas rotas existe ou está publicada
   neste repositório.
3. **Impacto:** tratar exemplos como contrato pode gerar implementações
   incompatíveis entre os projetos.
4. **Recomendação:** usar os exemplos apenas como insumo e publicar as operações
   aprovadas em uma especificação versionada pelo Backend.

Status: **DECISÃO PENDENTE**.

### 3. Necessidades offline sem semântica de servidor

1. **Comportamento documentado:** o Mobile prevê cache, `lastSyncAt` e uma
   `SYNC_QUEUE` conceitual.
2. **Comportamento encontrado:** não há regra de idempotência, conflito,
   timestamps ou reenvio definida pelo Backend.
3. **Impacto:** favoritos e visitas podem duplicar ou divergir.
4. **Recomendação:** definir a semântica antes de implementar a sincronização.

Status: **DECISÃO PENDENTE**.

## Dependências do repositório Mobile

- confirmação dos dados mínimos exigidos por cada tela;
- validação dos estados de loading, vazio, erro e offline contra o contrato;
- definição do formato e frequência das mutações enfileiradas;
- alinhamento sobre coordenadas, proximidade e detecção de visita;
- aviso prévio quando uma mudança de Backend afetar fluxo ou UX.

## Descrição preparada para o Pull Request

### Resumo

Consolida a documentação inicial do WEG Benefits Backend, registra o estado real
do repositório e separa responsabilidades entre Backend e Mobile sem inventar um
contrato ainda inexistente.

### Alterações

- amplia o README com escopo, estado e índice;
- adiciona contexto para futuros agentes;
- documenta visão, arquitetura, dados, API, autenticação, visitas, integração e
  testes;
- registra inconsistências, decisões pendentes e dependências do Mobile.

### Validação

- links relativos revisados;
- nenhuma dependência ou funcionalidade adicionada;
- nenhum segredo, token ou senha incluído;
- exemplos do Mobile identificados explicitamente como não contratuais.
