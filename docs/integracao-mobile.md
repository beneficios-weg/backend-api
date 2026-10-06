# Integração com o Mobile

## Princípio

O Mobile consome; o Backend publica o contrato. Necessidades de tela ajudam a
priorizar operações, mas não autorizam o consumidor a inventar rotas ou schemas.

## Matriz de dependências

| Fluxo do Mobile | Dependência do Backend | Estado |
|---|---|---|
| Login | Operação e ciclo de sessão | DECISÃO PENDENTE |
| Primeiro acesso | Regra e operação de ativação | DECISÃO PENDENTE |
| Recuperação | Regra e operação de recuperação | DECISÃO PENDENTE |
| Landing/início | Categorias e estabelecimentos relevantes | DECISÃO PENDENTE |
| Categoria | Consulta filtrada de estabelecimentos | DECISÃO PENDENTE |
| Loja | Detalhe e benefício principal | DECISÃO PENDENTE |
| Favoritos | Consulta e mutação idempotente | DECISÃO PENDENTE |
| Mapa | Coordenadas e contexto de proximidade | DECISÃO PENDENTE |
| Lojas visitadas | Registro e histórico de visitas | DECISÃO PENDENTE |

## Offline e sincronização

O Mobile prevê cache de categorias, estabelecimentos, benefícios, favoritos e
visitas, com `lastSyncAt`, além de uma `SYNC_QUEUE` conceitual para mutações.
Esses nomes descrevem estado local, não campos obrigatórios da API.

Para suportar reenvios, o Backend deve definir idempotência, conflitos,
ordenamento, exclusão, timestamps autoritativos e formato de erro recuperável.
Essa semântica ainda é **DECISÃO PENDENTE**.

## Processo de mudança

Toda alteração de contrato que afete estados de loading, vazio, erro, offline ou
fluxo de tela deve ser registrada e comunicada ao responsável pelo Mobile antes
de ser considerada pronta para consumo.
