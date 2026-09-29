# Decisões abertas

Todos os itens abaixo estão marcados como **DECISÃO PENDENTE** porque não há
evidência de aprovação ou implementação no Git.

| Tema | Decisão necessária | Dependência/impacto |
|---|---|---|
| Stack | Runtime, framework e versões | Instalação, arquitetura e testes |
| Persistência | Banco, ambiente e migrações | Modelo, operação e implantação |
| Modelo/DER | Entidades, relações e restrições | Contrato e integridade |
| Identidade | Provedor e fluxos de acesso | Login, recuperação e segurança |
| Contrato | Versionamento e especificação executável | Integração Mobile |
| Erros | Formato, códigos HTTP e validações | UX de erro do Mobile |
| Consultas | Busca, filtros, ordenação e paginação | Landing, categoria e mapa |
| Benefício principal | Regra e cardinalidade no MVP | Tela de estabelecimento |
| Favoritos | Semântica, unicidade e sincronização | Offline e preferências |
| Visitas | Payload, validação, retenção e idempotência | Histórico e offline |
| Geolocalização | Dados recebidos e campos calculados | Privacidade e proximidade |
| Sincronização | Conflitos e timestamps autoritativos | `SYNC_QUEUE` do Mobile |
| Observabilidade | Logs, métricas e correlação | Suporte e segurança |
| Implantação | Ambientes, secrets e CI/CD | Execução e operação |

## Recomendação de ordem

1. aprovar stack e identidade;
2. aprovar modelo de dados e regras de privacidade;
3. publicar contrato mínimo versionado;
4. validar o contrato com o responsável pelo Mobile;
5. implementar migrações, operações e testes em fatias verticais.

A ordem acima é recomendação de trabalho, não uma decisão já tomada.
