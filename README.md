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

## Public demo deployment (Render + Neon)

The deployment configuration uses a free Render web service for the API and Neon Free for PostgreSQL.
Render's free web service can sleep after inactivity, so the first request after sleep may take longer.
The database is external to Render; do not add a Render Free PostgreSQL resource because it expires.

1. Create a PostgreSQL project in Neon Free and copy its pooled connection details. In Render, set
   `DB_URL` as a JDBC URL (`jdbc:postgresql://<pooler-host>/<database>?sslmode=require`) and set
   `DB_USERNAME` and `DB_PASSWORD` separately. Keep all credentials in Render environment secrets.
2. Merge the deployment-configuration pull request into `develop`, then create/sync a Blueprint in
   Render from `beneficios-weg/backend-api`, branch `develop`, using the root `render.yaml`.
3. Supply the database and Gmail OAuth variables when Render asks for them. Configure
   `CORS_ALLOWED_ORIGINS` with exact browser-client origins, comma-separated; never use `*` in
   production. Set `SESSION_COOKIE_SECURE=true` (already configured by the Blueprint).
4. Ensure the Google OAuth consent app is published and has completed any verification required for
   `gmail.send`. While the app remains in Testing, its refresh token is short-lived and must be
   renewed; deployment alone does not make it permanent.
5. Wait for Render's `/actuator/health/readiness` check to pass. Flyway applies the migrations on
   startup. Provision the initial ADMIN only after the production database and Gmail API secrets are
   ready; do not reuse the local development database password.

The Neon Free plan has usage limits and a six-hour instant-restore window. It is not a substitute for
an independent backup. Until an off-site backup and restore procedure is configured, treat this as a
demo environment and export important data regularly. The Render service URL uses its generated
`onrender.com` subdomain; no custom domain is required.

Before creating users or provisioning the initial ADMIN, configure Gmail API OAuth credentials:
`GMAIL_CLIENT_ID`, `GMAIL_CLIENT_SECRET`, and `GMAIL_REFRESH_TOKEN`. Set `GMAIL_SENDER` to
`wegbenefits@gmail.com` (the default). Keep the client secret and refresh token in environment
secrets; never commit them. The API exchanges the refresh token for an access token and sends the
message through Gmail over HTTPS. A Gmail API error rolls back user creation and is returned to the
administrator as a generic API problem response. Gmail accepting the API request does not guarantee
delivery to the recipient; a later bounce does not roll back the account.

In Render, add these values under the service's **Environment** settings. For local PowerShell,
enter the confidential values without echoing them into the terminal:

```powershell
$env:GMAIL_CLIENT_ID = Read-Host "OAuth client ID"
$clientSecretSecure = Read-Host "OAuth client secret" -AsSecureString
$env:GMAIL_CLIENT_SECRET = [System.Net.NetworkCredential]::new("", $clientSecretSecure).Password
$refreshTokenSecure = Read-Host "OAuth refresh token" -AsSecureString
$env:GMAIL_REFRESH_TOKEN = [System.Net.NetworkCredential]::new("", $refreshTokenSecure).Password
$env:GMAIL_SENDER = "wegbenefits@gmail.com"
```

If the OAuth consent screen is still in **Testing**, Google limits the refresh-token lifetime to
seven days for this Gmail scope. Reauthorize when it expires, or complete the Google publishing and
verification steps before relying on it for a persistent deployment. Publishing alone does not
complete verification for the sensitive `gmail.send` scope.

`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `PORT`, `SERVER_PORT`, `CORS_ALLOWED_ORIGINS`,
`GMAIL_CLIENT_ID`, `GMAIL_CLIENT_SECRET`, `GMAIL_REFRESH_TOKEN`, and `GMAIL_SENDER` can be configured
per environment. The local port remains `8081` unless `SERVER_PORT` is set; `PORT` takes precedence
when the hosting platform supplies it. Production CORS origins must be explicit; wildcard origins are
rejected. Set `SESSION_COOKIE_SECURE=true` behind HTTPS.

Provision the first ADMIN from an interactive PowerShell terminal after the database and Gmail API
OAuth configuration are ready. The password is read without echo and is not passed as an argument:

```powershell
.\mvnw.cmd -DskipTests package
java -jar .\target\benefits-0.0.1-SNAPSHOT.jar --app.cli.provision-admin=true --spring.main.web-application-type=none
```

## Available infrastructure endpoints

- Health: `http://localhost:8081/actuator/health`
- OpenAPI JSON: `http://localhost:8081/v1/api-docs`
- Swagger UI: `http://localhost:8081/swagger-ui.html`

## HTTP API reference

See [docs/http-api.md](docs/http-api.md) for implemented routes, authentication, request fields,
JSON examples, and the bulk benefit creation endpoint.

Only Actuator health is exposed. Health responses do not include component details; Gmail credentials
are not checked by the health endpoint. Flyway V1 creates Identity and PostgreSQL-backed sessions;
V2/V3 create Catalog; V4 creates favorites and visits. V3 aborts startup if the database has legacy
rows in `establishment` or `benefit`; inspect and map those records before applying that migration.

## Initial package boundaries

- `com.weg.benefits.catalog`: catalog administration and its use cases.
- `com.weg.benefits.identity`: account, session authentication, initial ADMIN CLI and user administration.
- `com.weg.benefits.engagement`: authenticated user's favorites and visits.
- `com.weg.benefits.common`: cross-cutting HTTP and logging infrastructure.
- `com.weg.benefits.config`: application configuration.

There is no public account registration or password reset. Catalog, Identity and Engagement routes,
including which are implemented, are listed in [docs/api-contract.md](docs/api-contract.md).
