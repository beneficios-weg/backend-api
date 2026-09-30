# Modelo de dados e DER

## Situação

**DECISÃO PENDENTE:** não existe modelo de dados, DER, esquema ou migração no
repositório. Portanto, nenhum nome de entidade, campo, chave ou relacionamento é
oficial.

## Conceitos exigidos pelo produto

O briefing do Mobile revela a necessidade de representar, conceitualmente:

- usuário/colaborador;
- categoria;
- estabelecimento;
- benefício;
- favorito;
- visita.

Esses conceitos não equivalem automaticamente a tabelas. O desenho deve decidir
identificadores, cardinalidades, nulabilidade, auditoria e regras de exclusão.

## Regras a esclarecer

- se o usuário é mantido localmente ou vem de provedor corporativo;
- relação entre estabelecimentos, categorias e benefícios;
- significado e vigência de um benefício principal no MVP;
- unicidade e remoção de favoritos;
- dados mínimos e idempotência do registro de visita;
- retenção de visitas e tratamento de dados de localização;
- campos calculados no servidor, como distância ou disponibilidade.

## DER

O DER deve ser adicionado somente após aprovação do modelo e mantido coerente
com as migrações. Até lá, não há DER oficial para o Mobile usar como contrato.
