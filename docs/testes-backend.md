# Estratégia de testes do Backend

## Situação

Não há framework, suíte ou pipeline de testes no repositório.

## Cobertura mínima planejada

Quando houver implementação, a estratégia deve cobrir:

- autenticação, primeiro acesso e recuperação;
- autorização de dados pertencentes ao usuário;
- validações de categorias, estabelecimentos e benefícios;
- busca, filtros, ordenação e paginação;
- inclusão e remoção idempotente de favoritos;
- registro idempotente e consulta de visitas;
- reenvio após operação offline;
- códigos HTTP e formato de erros;
- migrações e integridade do modelo de dados;
- proteção de dados sensíveis em logs e respostas.

## Registro de execução

Casos manuais ou automatizados devem tornar visíveis:

| Campo | Conteúdo esperado |
|---|---|
| Cenário | Comportamento sob teste |
| Pré-condição | Estado e dados necessários |
| Passos | Requisição ou ação executada |
| Resultado esperado | Contrato e efeito esperados |
| Resultado obtido | Evidência da execução |
| Status | Aprovado, reprovado ou bloqueado |

**DECISÃO PENDENTE:** escolher ferramentas, níveis de teste, dados de teste e
critérios de CI depois que a stack for aprovada.
