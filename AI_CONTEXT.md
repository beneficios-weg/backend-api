# Contexto para agentes — WEG Benefits API

## Limites do repositório

- Este é o repositório exclusivo do Backend (`weg-benefits-api` / `backend-api`).
- Não altere o repositório Mobile a partir daqui.
- Não transforme os dois projetos em monorepo.
- Altere somente arquivos deste repositório.

## Autoridade de cada projeto

O Backend é a fonte de verdade para modelo de dados, DER, banco de dados,
endpoints, payloads, códigos HTTP, autenticação, validações e campos calculados
pelo servidor.

O Mobile é a fonte de verdade para telas, navegação, componentes, UX, design,
estado, permissões, armazenamento local, offline e consumo da API.

O Backend não deve impor mudanças no fluxo visual. Quando uma alteração de
contrato afetar telas ou UX, registre a necessidade de alinhamento com o Mobile.

## Estado observado em 29/09/2026

O Git ainda não contém código-fonte, stack definida, DER, banco, migrações,
contrato de API, autenticação nem testes. A documentação atual não aprova essas
decisões; ela apenas organiza responsabilidades, necessidades e pendências.

Não invente comandos, endpoints, payloads, códigos HTTP, nomes de tabelas,
variáveis de ambiente ou tecnologias. Uma sugestão deve ser identificada como
recomendação e nunca tratada como implementação existente.

## Contexto funcional recebido do Mobile

O Mobile prevê login, primeiro acesso, recuperação de senha, categorias,
estabelecimentos, benefícios, favoritos e visitas. Usa geolocalização para
proximidade, mapa e detecção de visitas, além de cache e uma fila conceitual de
sincronização offline. Esses itens são necessidades do consumidor; ainda não
constituem contrato do Backend.

Favoritos são independentes de visitas. O MVP considera um benefício principal
por estabelecimento, mas a representação no modelo e na API está pendente.

## Procedimento para conflitos

Nunca corrija uma divergência silenciosamente. Registre:

1. comportamento documentado;
2. comportamento encontrado;
3. impacto;
4. recomendação.

Use **DECISÃO PENDENTE** quando a resposta depender da equipe. Preserve conteúdo
útil e atualize apenas o que estiver comprovadamente obsoleto.
