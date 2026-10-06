# Visão geral

## Produto

WEG Benefits é um produto mobile-first para colaboradores descobrirem benefícios
em estabelecimentos parceiros, inclusive a partir da localização do aparelho.
O Backend deverá fornecer dados e regras de servidor para essa experiência.

## Contexto de entrega

A equipe informada possui quatro integrantes e cerca de 30 horas para o projeto.
As decisões devem privilegiar um MVP verificável e evitar complexidade que não
seja exigida pelo produto.

## Domínios funcionais conhecidos

As necessidades comunicadas pelo Mobile abrangem:

- identidade: login, primeiro acesso e recuperação de senha;
- catálogo: categorias, estabelecimentos e benefícios;
- preferências: estabelecimentos favoritos;
- localização: proximidade e dados necessários ao mapa;
- histórico: registro e consulta de visitas.

Essa lista delimita o problema, mas não define entidades, rotas ou payloads.
Somente um contrato aprovado e versionado neste repositório poderá fazê-lo.

## Fronteiras

O Backend valida e persiste dados remotos, calcula campos sob sua
responsabilidade e publica o contrato HTTP. O Mobile decide como apresentar os
dados, solicitar permissões, manter cache, operar offline e sincronizar ações.

Pontos de GPS usados pelo Mobile durante a análise de visita são descritos como
temporários; não há requisito para persistência permanente do trajeto no
servidor. Qualquer ampliação dessa coleta exige decisão explícita de privacidade.
