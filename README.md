# WEG Benefits API

Repositório reservado ao Backend do WEG Benefits, aplicativo corporativo para
colaboradores descobrirem benefícios em estabelecimentos parceiros próximos.

## Estado atual

Em 29 de setembro de 2026, este repositório contém somente documentação. Ainda
não há aplicação, contrato HTTP publicado, modelo de dados, migrações, testes ou
configuração de execução. Nenhuma tecnologia de Backend foi aprovada no Git.

Por isso, não existem comandos de instalação ou execução válidos nem variáveis
de ambiente confirmadas. Eles devem ser documentados depois que a implementação
for adicionada, sem antecipar decisões técnicas.

## Responsabilidade

Este repositório é a fonte de verdade para:

- modelo de dados e DER;
- banco de dados e migrações;
- endpoints, payloads e códigos HTTP;
- autenticação e autorização da API;
- validações e campos calculados no servidor.

O repositório Mobile é a fonte de verdade para telas, navegação, UX, estado,
permissões, armazenamento local, funcionamento offline e consumo da API. Os
repositórios permanecem separados; este projeto não deve virar um monorepo.

## Documentação

- [Visão geral](docs/visao-geral.md)
- [Estado atual e inventário](docs/estado-atual.md)
- [Arquitetura do Backend](docs/arquitetura-backend.md)
- [Modelo de dados e DER](docs/modelo-de-dados.md)
- [Contrato da API](docs/contrato-api.md)
- [Autenticação e autorização](docs/autenticacao.md)
- [Visitas e geolocalização](docs/visitas-e-geolocalizacao.md)
- [Integração com o Mobile](docs/integracao-mobile.md)
- [Estratégia de testes](docs/testes-backend.md)
- [Decisões abertas](docs/decisoes-abertas.md)
- [Relatório da consolidação](docs/relatorio-consolidacao.md)

Consulte também [AI_CONTEXT.md](AI_CONTEXT.md) antes de qualquer trabalho
assistido por IA.

## Regra de divergência

Conflitos entre documentação e implementação não devem ser corrigidos
silenciosamente. Registre o comportamento documentado, o encontrado, o impacto
e a recomendação; use **DECISÃO PENDENTE** quando a solução depender da equipe.
