# Testes de integração de sessões e Engagement

## Execução no PowerShell

Requisitos: Java 21, Docker Desktop com engine Linux ativo e porta `8081` livre.
O Maven Wrapper já pertence ao projeto. Não é necessário configurar Gmail, Neon ou contas reais.

Na raiz do repositório:

```powershell
# Se necessário, aponte JAVA_HOME para o JDK 21 instalado na sua máquina.
$env:JAVA_HOME = 'C:\caminho\para\jdk-21'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
.\scripts\test-api-integration.ps1
```

Se todas as dependências Maven já estiverem no cache, pode usar
`.\scripts\test-api-integration.ps1 -Offline`. Esse parâmetro desabilita downloads do Maven;
o Docker ainda precisa dispor da imagem `postgres:17-alpine` ou baixá-la.

O script cria um contêiner com nome exclusivo, publica o PostgreSQL somente no loopback em porta
aleatória e usa `tmpfs`, sem volume persistente. Cria `benefits_api_test` para sessões/Engagement e
`benefits_catalog_test` para os testes existentes do catálogo. Define as variáveis de banco apenas
durante a execução, chama `mvnw.cmd verify`, restaura as variáveis anteriores e remove o contêiner
que criou, inclusive em caso de falha. Não encerra aplicações que ocupem a porta `8081`.

A nova suíte somente é habilitada com `API_TEST_DB_URL` no formato
`jdbc:postgresql://127.0.0.1:<porta>/benefits_api_test` (ou `localhost`). O script verifica que ela
foi executada e não aceita sucesso com todos os seus testes omitidos. As credenciais fixas do
script são exclusivamente dos bancos descartáveis. Nunca substituir essas URLs por Neon ou
por bancos que contenham dados a preservar.

## Cobertura efetiva

`SessionEngagementIntegrationTests` contém 15 cenários, sem transação abrangendo o teste:
as requisições passam pelos filtros reais do Spring Security/Spring Session e as transações dos
serviços são confirmadas ou revertidas no PostgreSQL. O teste não injeta autenticação fictícia
nem transporta `MockHttpSession`; utiliza o cookie emitido pelo login e recuperado pelo repositório
JDBC na próxima requisição.

- Login por username/e-mail; senha incorreta, conta inexistente e conta inativa.
- Sessões persistidas sem expiração por inatividade; cookie persistente, `HttpOnly`, `SameSite=Lax`.
- Logout elimina somente sua sessão e expira o cookie; o cookie anterior deixa de autenticar.
- Requisições HTTP reais ao Tomcat na porta `8081` para login, perfil, logout e rejeição do cookie antigo.
- ADMIN acessa administração; COLLABORATOR recebe `403` nas famílias administrativas, incluindo lotes.
- Criação administrativa armazena hash e confirma a conta após aceitação simulada pelo Gmail.
- Rejeição HTTP do Gmail reverte a conta já inserida/flushada; uma nova tentativa pode usar o mesmo username.
- Edição/promoção e desativação revogam todas as sessões da conta, preservando sessões de outras contas.
- Rebaixamento e desativação de ADMIN, autodesativação e troca de senha por PATCH continuam proibidos.
- Favoritos: inclusão/remoção idempotentes, persistência e isolamento entre proprietários.
- Visitas: UTC, reenvio exato/com outro offset, eventos antigos offline, duplicidade até 30 minutos
  inclusive, nova visita fora da janela, histórico paginado e isolamento por proprietário.
- Quatro requisições concorrentes de favoritos e quatro de visitas não duplicam os registros.
- ONLINE permite favorito e rejeita visita; estabelecimento físico desativado ainda recebe visita offline.
- Conta inativa ou ausente na verificação de gravação retorna o mesmo problema `401` da sessão inválida,
  sem informar qual condição ocorreu e com `requestId` consistente no corpo/cabeçalho.

Somente as chamadas HTTP ao Google são substituídas por `MockRestServiceServer`, com valores
fictícios. O `CredentialMailSender` real, o encoder e os serviços/repositórios da aplicação são usados.
Não há entrega real de e-mail nem uso de segredos OAuth. O restante da cobertura HTTP usa MockMvc
com os filtros reais; isso não equivale a homologação em Android ou no Render.

## Correção de revogação encontrada pelos testes

A biblioteca Spring Session JDBC 4.1.1 presente no ambiente local usava, na exclusão por ID,
uma condição `MAX_INACTIVE_INTERVAL >= 0`. Com o intervalo negativo aprovado para esta atividade,
as sessões permaneciam no banco após chamadas de revogação. `SessionConfiguration` configura
somente a exclusão explícita como `DELETE FROM %TABLE_NAME% WHERE SESSION_ID = ?`, usando o
[customizador suportado pelo Spring Session](https://docs.spring.io/spring-session/reference/configuration/jdbc.html).
A limpeza por expiração usa outra consulta; a política de sessão sem expiração permanece.
Não foi necessário mudar versões, dependências ou migrações.

O primeiro teste de cookie executado somente com contexto servlet simulado não refletia os atributos
do servidor embarcado. A suíte passou a iniciar o Tomcat e verificar também HTTP real; os atributos
configurados já eram aplicados corretamente nesse ambiente. Não foi alterada a configuração de
cookies da aplicação para contornar a simulação.

## Resultados e limites

Execução em 12/11/2026 com Java 21 e PostgreSQL 17.11: `BUILD SUCCESS` no `verify`;
42 testes contabilizados, 41 executados com sucesso, zero falhas/erros e um omitido.
O comando foi conferido em PowerShell 7 e Windows PowerShell 5.1; a execução final em 5.1 também
confirmou a limpeza automática. O PostgreSQL de desenvolvimento permaneceu saudável e a porta
`8081` foi liberada ao encerrar o contexto de teste.
Os 15 cenários novos foram executados integralmente. O teste omitido pertence ao catálogo e requer
`CATALOG_SOURCE_FIXTURE`, arquivo local opcional com os 112 rascunhos do PDF; não é uma omissão
dos testes de sessão/Engagement. Relatórios ficam em `target/surefire-reports/`.

A primeira execução, anterior às correções, reproduziu as respostas `401` divergentes e a falha
de remoção das sessões na edição/desativação. A execução corrigida também validou o logout real.
Nenhum banco de desenvolvimento/hospedado foi usado. Estes resultados não comprovam a validade
atual do OAuth Gmail no Render, a integração nativa dos clientes, backup/restauração ou a conclusão
da carga do catálogo; essas etapas continuam pendentes.
