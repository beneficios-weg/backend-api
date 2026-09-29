# Contrato da API

## Situação

**DECISÃO PENDENTE:** não há especificação OpenAPI, endpoints, payloads, códigos
HTTP, paginação ou formato de erro publicados neste repositório.

## Capacidades solicitadas pelo consumidor Mobile

| Capacidade | Necessidade conhecida | Contrato publicado |
|---|---|---|
| Autenticar | Login do colaborador | Não |
| Primeiro acesso | Ativação ou definição inicial de acesso | Não |
| Recuperar acesso | Fluxo de recuperação de senha | Não |
| Listar categorias | Alimentar início e navegação por categoria | Não |
| Consultar estabelecimentos | Busca, filtros, proximidade e mapa | Não |
| Detalhar estabelecimento | Exibir dados e benefício principal | Não |
| Gerenciar favoritos | Consultar e sincronizar preferência | Não |
| Registrar visita | Sincronizar visita detectada pelo Mobile | Não |
| Consultar visitas | Exibir histórico | Não |

Rotas como `GET /categories`, `GET /establishments` e `GET /visits` apareceram
apenas como exemplos no briefing do Mobile. Elas **não são contrato aprovado**.

## Itens obrigatórios antes da integração

O contrato publicado deve definir:

- URL base e versionamento;
- autenticação e autorização por operação;
- método, caminho, parâmetros e cabeçalhos;
- schemas de requisição e resposta;
- códigos HTTP de sucesso e erro;
- formato comum de erros e validações;
- paginação, busca, filtros e ordenação;
- idempotência para mutações e sincronização offline;
- semântica de datas, horários, coordenadas e campos calculados.

## Governança

A especificação versionada neste repositório deve ser a fonte de verdade. Uma
mudança incompatível precisa ser registrada e alinhada com o Mobile antes da
adoção. Exemplos em documentação do consumidor não substituem essa especificação.
