# Validação — manutenção da senha ADMIN — 2026-10-28

Ambiente: Windows, Java 21.0.12.1, Spring Boot 4.1.1, PostgreSQL 17.11 local. Nenhum teste conectou ao Neon, alterou a senha real do ADMIN ou enviou e-mail.

## Resultados reais

- `mvnw.cmd -o -Dtest=AdminPasswordMaintenanceValidationTests test`: 3 aprovados.
- `mvnw.cmd -o -DskipTests package`: BUILD SUCCESS. Manifest preservou `Start-Class: com.weg.benefits.BenefitsApplication`; classe de manutenção e driver PostgreSQL estão no JAR. Sem alteração do POM/dependências.
- Com `IDENTITY_MAINTENANCE_TEST_DB_URL=jdbc:postgresql://localhost:5432/benefits_identity_maintenance_test`, `mvnw.cmd -o -Dtest=AdminPasswordMaintenanceTests,AdminPasswordMaintenanceValidationTests test`: 9 aprovados, zero falhas/erros/ignorados. Cada teste usou schema próprio e o removeu depois; não usou a base `benefits`.
- O teste adicional `BenefitsApplicationTests` falhou nas duas primeiras tentativas na criação do cliente HTTP JDK (`Unable to establish loopback connection` / `Invalid argument: connect`, UnixDomainSockets/Pipe no Windows). Não foi falha da conexão PostgreSQL nem da transação da manutenção. Não alteramos a API para contornar esse problema local.
- Rodada final: `mvnw.cmd -o -Dspring.http.clients.imperative.factory=simple -Dtest=AdminPasswordMaintenance*,BenefitsApplicationTests,CredentialMailSenderTests package`, com `DB_URL` também apontando ao banco isolado: **12 testes aprovados, zero falhas/erros/ignorados, BUILD SUCCESS**. O override foi somente um argumento desse comando de teste, sem alteração em propriedades de produção. São 9 testes novos, 1 de contexto e 2 de e-mail com mocks.
- Parser PowerShell 7 e Windows PowerShell 5.1: PASS. Execução não interativa pelos dois shells recusada, sem conexão, usando o JAR empacotado; retorno Java 2 e script 1. Java recusou argumento de senha não suportado sem repetir seu conteúdo.
- Ensaio de terminal interativo com credenciais fictícias e banco de teste: prompts exibidos em UTF-8, senhas sem eco; conexão do JAR à base isolada concluída e conta inexistente recusada sem alteração. Uma primeira tentativa sem permissão de rede foi inconclusiva; repetida com acesso local, apresentou a recusa esperada. Nenhuma senha real foi digitada pelo agente.
- `git diff --check`: PASS antes do commit.

## Garantias exercitadas nos testes novos

1. Nova senha gera hash compatível com o encoder da API e a anterior deixa de corresponder; caracteres e espaços externos são preservados.
2. Somente a conta-alvo muda hash/data; identidade, papel, estado, criação e outra conta permanecem.
3. Somente sessões da conta são apagadas; atributos são removidos pela FK e sessões alheias ficam intactas.
4. Senha atual incorreta, conta ausente/inativa/COLLABORATOR, nova senha fora dos limites ou igual à atual não mudam hash/sessões.
5. Falha forçada ao apagar sessões desfaz a atualização do hash e preserva os dados anteriores.
6. Trocas concorrentes usando a senha antiga não conseguem concluir ambas.
7. Configuração remota exige host direto Neon e TLS validado; portas, hosts e nomes que tentam injetar credenciais/parâmetros são recusados.
8. Destino local permanece localhost e entradas sem conteúdo/tamanho válido são recusadas, inclusive senha totalmente em branco já rejeitada pelo login HTTP.

Execução com as credenciais reais e login posterior em cada ambiente ficam para o operador. O ensaio interativo foi uma recusa com conta fictícia; a troca bem-sucedida e o rollback foram exercitados pelos testes automatizados PostgreSQL. Não foi validada a conexão TLS ao projeto Neon nesta tarefa.
