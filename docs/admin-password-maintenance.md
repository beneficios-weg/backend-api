# Troca de senha do ADMIN por manutenção CLI

O usuário solicitou em 2026-10-28 a troca nos ambientes local e Render, informando que ainda conhece a senha atual. Esta ferramenta exige essa senha e altera somente uma conta ADMIN ativa já existente. Não implementa recuperação de senha, troca pelo aplicativo ou criação de conta. `PATCH /v1/admin/users/{id}` continua recusando `password`.

## Preparar no Windows PowerShell

Abra seu próprio terminal interativo. Não envie as senhas ao chat, não as coloque em argumentos/arquivos e não redirecione a entrada ou saída da ferramenta. Não é necessário abrir outra conta, apagar o banco ou redeployar a API para executar a manutenção.

```powershell
Set-Location C:\Users\samuel_silva-mello\Desktop\benefits
# Use o caminho do seu JDK 21. Este é o JDK já disponível nesta máquina:
$env:JAVA_HOME = 'C:\Users\samuel_silva-mello\.jdks\ms-21.0.12.1'
& .\mvnw.cmd -DskipTests package
```

O build precisa terminar com `BUILD SUCCESS`. O script verifica JDK 21 e presença da classe de manutenção no JAR. Ele usa o JAR local, sem inicializar o servidor Spring, sem Flyway e sem enviar e-mail. A entrada principal normal da API permanece `BenefitsApplication`.

## 1. Ambiente local

```powershell
docker compose up -d postgres
& .\scripts\change-admin-password.ps1 -Environment local
```

Preencha, nesta ordem:

1. Porta PostgreSQL: Enter usa **5432**, ou informe a porta que seu Compose publica.
2. Banco: Enter usa **benefits**.
3. Usuário do PostgreSQL: Enter usa **benefits_dev**.
4. Senha do PostgreSQL: Enter usa o padrão **exclusivo de desenvolvimento** do Compose; se alterou a credencial local, digite a correspondente sem eco.
5. Após a mensagem de conexão autenticada, username ou e-mail do ADMIN que deseja alterar.
6. Senha **atual do ADMIN**, que é diferente da credencial do PostgreSQL.
7. Nova senha do ADMIN e sua confirmação, ambas sem eco.

`local` só conecta a `localhost` e não herda `DB_URL`, `DB_USERNAME` ou `DB_PASSWORD` do terminal, evitando usar acidentalmente o banco remoto. A ferramenta imprime host/porta/banco antes de solicitar as credenciais. Seu ADMIN deve existir no banco escolhido.

## 2. Ambiente hospedado no Render

O PostgreSQL que atende o Render está no projeto Neon já existente. No console Neon, selecione o **mesmo projeto, branch e banco usados pela API** e abra **Connect**. Desabilite **Connection pooling** para copiar o host direto, sem `-pooler`. Não crie outro projeto/banco e não altere as variáveis do serviço Render.

```powershell
& .\scripts\change-admin-password.ps1 -Environment render
```

Preencha:

1. **Host direto do Neon**, por exemplo `ep-exemplo.us-east-2.aws.neon.tech`: somente hostname, sem `postgresql://`, `jdbc:`, usuário, senha ou parâmetros. O exemplo não é um destino real.
2. **Banco** efetivamente usado pela API: Enter usa `neondb`, mas confira o nome no painel antes de aceitar.
3. **Usuário do PostgreSQL** exibido no Connect.
4. **Senha do PostgreSQL** exibida no Connect, digitada sem eco. Esta não é a senha de login do ADMIN.
5. Após a mensagem de conexão autenticada, username ou e-mail do ADMIN da API hospedada.
6. Senha atual desse ADMIN, nova senha e confirmação, sem eco.

O modo remoto aceita apenas host direto `*.neon.tech`, porta 5432, e usa TLS com verificação do certificado/hostname (`verify-full`, `DefaultJavaSSLFactory`). A conexão usa o driver PostgreSQL já existente, sem dependência adicional. Se houver falha de certificado, corrija o JDK/truststore; não desative a verificação. Referências: [pgJDBC SSL](https://jdbc.postgresql.org/documentation/ssl/) e [Neon: pooling e conexão direta](https://neon.com/docs/connect/connection-pooling).

## Resultado e limites

### Diagnosticar uma falha sem revelar credenciais

Para conferir somente a conexão, sem fornecer nenhuma senha do ADMIN:

```powershell
.\scripts\change-admin-password.ps1 -Environment render -CheckConnection
# Para conferir o PostgreSQL local, use -Environment local -CheckConnection.
```

Este modo solicita host/porta/banco/usuário e senha do PostgreSQL em prompt oculto, abre e fecha a conexão, sem executar SQL, sem migrações e sem solicitar login/senha do ADMIN. Não modifica dados ou sessões. O servidor pode registrar a tentativa de conexão/autenticação. Sucesso deste diagnóstico valida conexão e autenticação do banco, não a conta ADMIN nem permissões de manutenção. Na execução normal, a conexão agora também é aberta antes de pedir as credenciais do ADMIN.

O CLI atualizado imprime uma categoria fixa antes do aviso de resultado não confirmado. Não imprime mensagens de exceção, SQL, stack traces ou dados fornecidos. Os códigos PostgreSQL usados na classificação seguem a [documentação oficial de SQLSTATE](https://www.postgresql.org/docs/current/errcodes-appendix.html).

- `DB_AUTH`: autenticação recusada pelo PostgreSQL; conferir **usuário/senha do banco**, distintos do ADMIN. Se a senha da role foi trocada no Neon, usar a nova e atualizar também `DB_PASSWORD` no Render.
- `TLS`, `DNS`, `TIMEOUT`, `REDE`, `SOCKET`, `EOF`, `DB_CONNECTION`: conferir certificado/JDK, host, conectividade ou disponibilidade conforme a mensagem. Não desativar TLS. Quando disponível, `DB_CONNECTION` inclui somente um SQLSTATE de uma lista permitida (08001, 08004, 08006, 08007, 08P01), sem texto de exceção.
- `DB_NAME`, `DB_SCHEMA`, `DB_PERMISSION`: conferir banco/branch/migrações ou permissões; não recriar dados para contornar o erro.
- `DB_LOCK`: evitar manutenção e uso concorrentes da conta.
- `CLI_FAILURE`: falha não classificada; confirmar JDK 21 e recompilar o JAR.

Ao aparecer **“Conexão com o PostgreSQL autenticada”**, o driver já abriu a conexão com as credenciais do banco. Isso ainda não significa que validou o ADMIN ou trocou a senha. Em falha, compartilhar somente a categoria, o SQLSTATE permitido e o texto fixo; nunca a conexão completa ou senhas. O aviso de resultado não confirmado permanece: testar login antes de repetir uma manutenção, principalmente se a conexão caiu durante o commit. Uma falha no modo `-CheckConnection` não iniciou manutenção.

Após atualizar o código, recompile com `.\mvnw.cmd -DskipTests package` para o script usar as novas mensagens. A correção é local ao CLI, sem redeploy da API.

- Exige ADMIN ativo e senha atual correta. COLLABORATOR, conta inativa/inexistente, senha atual incorreta, nova senha igual à atual ou confirmação diferente não alteram a conta.
- Nova senha: 6–72 caracteres, sem normalização. Preserva espaços e outros caracteres. Rejeita entrada inteiramente em branco para manter compatibilidade com `LoginRequest.password`, que já usa `@NotBlank`; essa validação não muda o contrato HTTP.
- Usa exatamente o mesmo PBKDF2 e parâmetros da API. Guarda somente o hash; altera `password_hash` e `updated_at`, preservando ID, login, e-mail, papel, estado, data de criação e demais dados.
- Bloqueia a linha durante a troca. Atualização do hash e remoção das sessões existentes dessa conta ocorrem na mesma transação. Falha na remoção das sessões desfaz a atualização. Sessões das outras contas são preservadas; atributos da sessão são removidos pela FK/cascade já existente.
- Ao ver **“Senha alterada”**, entre novamente com a nova senha. Após concluir cada ambiente, valide login e `GET /v1/me` nele; as duas execuções são independentes. Evite logins/requisições concorrentes da conta durante a manutenção; uma requisição já em andamento não é cancelada pelo CLI.
- Em erro de conexão/commit, a ferramenta não imprime exceções internas e pode não conseguir confirmar o resultado. Teste o login antes de repetir, pois uma interrupção após o commit pode ocorrer mesmo com a troca realizada.
- Sem terminal interativo, a ferramenta encerra com código 2, sem conectar. Não há fallback que leia senha em texto visível ou arquivo. Credenciais ficam em memória durante a execução e não são enviadas por e-mail.
- Nenhuma senha real foi alterada durante a implementação: a execução final é do operador no próprio terminal. Conexão remota/autenticação real permanecem a validar pelo operador.

## Testes automatizados isolados

Somente a base local de nome `benefits_identity_maintenance_test` é aceita pelos testes de persistência; cada teste cria e remove seu próprio schema aleatório. Não apontar testes para `benefits` ou Neon.

```powershell
$env:IDENTITY_MAINTENANCE_TEST_DB_URL = 'jdbc:postgresql://localhost:5432/benefits_identity_maintenance_test'
$env:DB_URL = $env:IDENTITY_MAINTENANCE_TEST_DB_URL
# O override abaixo é apenas para os testes nesta máquina Windows (falha loopback do cliente HTTP JDK).
& .\mvnw.cmd '-Dspring.http.clients.imperative.factory=simple' '-Dtest=AdminPasswordMaintenance*,BenefitsApplicationTests,CredentialMailSenderTests' package
Remove-Item Env:DB_URL, Env:IDENTITY_MAINTENANCE_TEST_DB_URL
```

O banco de teste deve existir antes do comando. O teste de contexto Spring aplica as migrações já existentes somente na base isolada; a ferramenta de manutenção não aplica migrações. Consulte o diário para resultados efetivamente executados.

O script preserva Unicode configurando UTF-8 apenas no terminal durante o processo Java e restaurando a codificação anterior no `finally`. Se uma política local impedir scripts PowerShell, use diretamente o mesmo comando Java em seu terminal, sem modificar políticas do sistema:

```powershell
& "$env:JAVA_HOME\bin\java.exe" '-Dloader.main=com.weg.benefits.identity.AdminPasswordMaintenanceCli' '-cp' '.\target\benefits-0.0.1-SNAPSHOT.jar' 'org.springframework.boot.loader.launch.PropertiesLauncher' --local
# Para o outro ambiente, substitua somente --local por --render.
```

No comando direto, use um terminal já em UTF-8 para preservar Unicode. Prefira o script, que prepara/restaura essa codificação automaticamente. Resultados detalhados: [validação desta entrega](testes/admin-password-maintenance-2026-10-28.md).
