# WEG Benefits API

Initial Spring Boot backend skeleton for Java 21, Spring Boot 4.1.1, Maven, and PostgreSQL.
The API is the boundary for desktop and mobile clients; clients must not connect to the database.
The planned API prefix is `/v1`.

## Responsabilidades

O Backend é a fonte de verdade para o modelo de dados, banco, migrações, endpoints, payloads,
autenticação e validações do servidor. O Mobile é a fonte de verdade para telas, UX, navegação,
permissões, estado, armazenamento local e consumo da API. Os projetos permanecem separados.

## API planning documentation

Planning and integration references are in [docs/README.md](docs/README.md). The API contract marks
which Identity routes are implemented; remaining decisions and risks stay in the decision register.
Os documentos originais de planejamento recebidos do remoto foram preservados e listados no índice;
afirmações sobre stack ainda não decidida ou ausência de implementação são snapshots históricos.

O contexto e os documentos históricos originais continuam disponíveis:

- [Contexto para agentes](AI_CONTEXT.md)
- [Visão geral](docs/visao-geral.md)
- [Estado atual e inventário](docs/estado-atual.md)
- [Arquitetura do Backend](docs/arquitetura-backend.md)
- [Modelo de dados e DER](docs/modelo-de-dados.md)
- [Contrato da API original](docs/contrato-api.md)
- [Autenticação e autorização](docs/autenticacao.md)
- [Visitas e geolocalização](docs/visitas-e-geolocalizacao.md)
- [Integração com o Mobile](docs/integracao-mobile.md)
- [Estratégia de testes](docs/testes-backend.md)
- [Decisões abertas originais](docs/decisoes-abertas.md)
- [Relatório da consolidação](docs/relatorio-consolidacao.md)

## Local development

The Compose file uses the development-only credentials `benefits_dev` / `benefits_dev_only`.
Do not reuse them outside local development.

Start PostgreSQL from PowerShell in the repository directory:

```powershell
docker compose up -d postgres
docker compose ps
```

Wait for the database health status to become `healthy`, then run the API:

```powershell
.\mvnw.cmd spring-boot:run
```

The default local connection is `jdbc:postgresql://localhost:5432/benefits`. Override it through
environment variables when needed:

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/benefits"
$env:DB_USERNAME = "benefits_dev"
$env:DB_PASSWORD = "benefits_dev_only"
$env:CORS_ALLOWED_ORIGINS = "http://localhost:3000,http://localhost:5173"
.\mvnw.cmd spring-boot:run
```

Set `MAIL_APP_PASSWORD` before creating users or provisioning the initial ADMIN. It is the Google
App Password for `wegbenefits@gmail.com` and must not be committed. `MAIL_USERNAME` is the Gmail SMTP
account and `MAIL_FROM` defaults to the agreed sender. SMTP failure rolls back user creation and is
returned to the administrator as a generic API problem response.

`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `SERVER_PORT`, `CORS_ALLOWED_ORIGINS`, `MAIL_USERNAME`,
`MAIL_APP_PASSWORD`, and `MAIL_FROM` can be configured per environment. The API defaults to port
`8081`; set `SERVER_PORT` to override it. Production CORS origins must be explicit; wildcard origins
are rejected. Set `SESSION_COOKIE_SECURE=true` when HTTPS is terminated in a shared or production
environment.

Provision the first ADMIN from an interactive PowerShell terminal after the database and SMTP
configuration are ready. The password is read without echo and is not passed as an argument:

```powershell
.\mvnw.cmd -DskipTests package
java -jar .\target\benefits-0.0.1-SNAPSHOT.jar --app.cli.provision-admin=true --spring.main.web-application-type=none
```

## Available infrastructure endpoints

- Health: `http://localhost:8081/actuator/health`
- OpenAPI JSON: `http://localhost:8081/v1/api-docs`
- Swagger UI: `http://localhost:8081/swagger-ui.html`

Only Actuator health is exposed. Health responses do not include component details, and the aggregate
health check covers the application/database rather than SMTP. SMTP is required for account creation;
invalid Gmail credentials make that operation fail even when API health is `UP`. Flyway V1 creates the
Identity user and PostgreSQL-backed Spring Session tables; V2/V3 define Catalog. V3 aborts startup if
the database already contains rows in `establishment` or `benefit`; inspect and map those records before
starting the updated API against a database with Catalog data. Engagement tables are not implemented.

## Initial package boundaries

- `com.weg.benefits.catalog`: catalog administration and its use cases.
- `com.weg.benefits.identity`: account, session authentication, initial ADMIN CLI and user administration.
- `com.weg.benefits.engagement`: favorites and visits, when those features are designed.
- `com.weg.benefits.common`: cross-cutting HTTP and logging infrastructure.
- `com.weg.benefits.config`: application configuration.

There is no public account registration, password reset, favorites or visits. Catalog administration
and read routes are listed in [docs/api-contract.md](docs/api-contract.md); Identity routes and their
current scope are listed there as well.
