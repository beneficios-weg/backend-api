Flyway versioned migrations are applied from this directory. `V1__identity_and_sessions.sql`
defines the initial user table and PostgreSQL-backed Spring Session tables. Add new migrations
for later modules; never edit a migration after it has been applied to a shared database.
