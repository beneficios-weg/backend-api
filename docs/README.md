# Documentação da API WEG Benefits

Esta pasta é a referência de planejamento e integração do back-end. Ela não substitui o código, o POM, as migrações ou a especificação OpenAPI gerada pela aplicação.

## Referências de planejamento preservadas do remoto

Os documentos abaixo vieram do histórico `main` de 29/09/2026 e foram mantidos. Eles registram o
planejamento original; afirmações de que não havia stack, código ou autenticação são snapshots
históricos. Para o estado atual, consulte `../AGENTS.md`, `../README.md`, este índice, o contrato e o
registro de decisões.

- [`visao-geral.md`](visao-geral.md)
- [`estado-atual.md`](estado-atual.md)
- [`arquitetura-backend.md`](arquitetura-backend.md)
- [`modelo-de-dados.md`](modelo-de-dados.md)
- [`contrato-api.md`](contrato-api.md)
- [`autenticacao.md`](autenticacao.md)
- [`visitas-e-geolocalizacao.md`](visitas-e-geolocalizacao.md)
- [`integracao-mobile.md`](integracao-mobile.md)
- [`testes-backend.md`](testes-backend.md)
- [`decisoes-abertas.md`](decisoes-abertas.md)
- [`relatorio-consolidacao.md`](relatorio-consolidacao.md)
- [`../AI_CONTEXT.md`](../AI_CONTEXT.md)

## Documentos

- `implementation-plan.md`: arquitetura proposta, módulos, ordem, entregas e critérios de conclusão.
- `decision-register.md`: decisões confirmadas e itens que ainda precisam de aprovação.
- `api-contract.md`: contrato HTTP inicial proposto para alinhar API, mobile e desktop; endpoints ainda não implementados não devem ser tratados como existentes.
- [`http-api.md`](http-api.md): referência prática das rotas HTTP implementadas, autenticação, payloads e exemplos JSON; inclui cadastro em lote dos recursos do Catalog.
- [`relatorio-integracao-mobile-localizacao.md`](relatorio-integracao-mobile-localizacao.md): relatório independente da integração API–Mobile, avaliação da Geoapify, contrato atual de catálogo/visitas e proposta de localização em foreground/background.
- [`geocodificacao-catalogo.md`](geocodificacao-catalogo.md): consultas Geoapify, 13 coordenadas confirmadas pelo usuário, 3 casos adiados, vínculos dos 18 estabelecimentos/benefícios da carga e divergências ainda pendentes para importação.
- [`revisao-beneficios-pdf.md`](revisao-beneficios-pdf.md): revisão das 112 ofertas detalhadas do PDF, lotes de criação, pesquisa regional de unidades e pendências de vigência, localização e representação de ofertas online.
- `implementation-log.md`: estado real das etapas, baseado em mudanças e validações efetivamente realizadas.
- [`modelo-de-dados.md`](modelo-de-dados.md): modelo aprovado atualmente e divergências da revisão mais recente em reconciliação.
- [`referencia/WEG-Benefits-Modelagem-revisada.pdf`](referencia/WEG-Benefits-Modelagem-revisada.pdf): PDF de modelagem recebido em 2026-10-20, preservado como material de referência; consulte o registro de decisões para conflitos pendentes.

## Como manter estes documentos

Antes de cada módulo, o agente deve ler [`../AGENTS.md`](../AGENTS.md), [`../README.md`](../README.md), este índice, o plano e o registro de decisões; para mudanças de API, deve ler também o contrato. Deve inspecionar o código e as migrações do módulo. Ao terminar, atualiza o log, o contrato se necessário e devolve ao usuário o relatório previsto em `../AGENTS.md`.

O usuário trará esse relatório à conversa de planejamento. A conversa atualizará as recomendações e o próximo módulo; não se deve assumir que o contexto foi sincronizado sem receber o relatório.

## Estado inicial registrado

Com base no relato da equipe em 16/10/2026: projeto Spring Boot 4.1.1 / Java 21, PostgreSQL 17 em desenvolvimento local, Flyway, Actuator e OpenAPI. A aplicação iniciou em `localhost:8081` após o PostgreSQL estar disponível. Os pacotes previstos/plantados são `config`, `common`, `identity`, `catalog` e `engagement`. Ainda não há funcionalidades de negócio confirmadas como implementadas. Reconfirme o repositório antes de tomar este estado como atual.

## Estado após a fundação parcial de Identity — 2026-10-19

Identity já tem migração de usuários/sessões, login por username ou e-mail, logout, perfil próprio, rotas administrativas de usuários e CLI interativo para o primeiro ADMIN. Catalog tem rotas de administração e leitura; V3 foi aplicada ao banco local, mas as rotas ainda precisam de validação HTTP manual. Engagement continua sem funcionalidades. Consulte o contrato e o log para estado e verificações reais.

## Decisões de Identity atualizadas

O primeiro ADMIN será provisionado via CLI; o sistema gera ID/timestamps e cria essa conta ativa com papel ADMIN. O CLI coleta a senha em prompt interativo sem eco. Novas contas pelo painel começam ativas como COLLABORATOR. Há somente ADMIN e COLLABORATOR; ADMIN pode criar ADMINs, editar qualquer conta, promover COLLABORATOR para ADMIN, usar favoritos/visitas próprios e desativar somente colaboradores; não pode rebaixar ADMIN. COLLABORATOR não altera papéis. Não haverá cadastro próprio nem redefinição de senha. Login aceita username único ou e-mail com senha; usernames são ASCII minúsculos normalizados, e os e-mails ficam restritos a `gmail.com` e `estudante.sesisenai.org.br`. Senhas aceitam qualquer caractere e têm de 6 a 72 caracteres. A autenticação planejada é por sessão server-side sem expiração automática, armazenada no PostgreSQL e com cookie persistente; logout, desativação ou revogação encerram acesso. A atividade aceita o risco de não adicionar um fluxo CSRF dedicado. A allowlist CORS já existente será usada somente se um cliente web for publicado em origem diferente da API; CORS de navegador não se aplica ao mobile nativo. O envio usa a Gmail API, OAuth 2.0 e o escopo `gmail.send`; client secret e refresh token ficam fora do repositório. Se o app OAuth permanecer em Testing, o refresh token expira após sete dias. A conta só será criada quando a Gmail API aceitar o envio; erro significa nenhuma conta e aviso ao ADMIN. Veja as pendências no `decision-register.md`.
