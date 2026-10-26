Flyway versioned migrations are applied from this directory. `V1__identity_and_sessions.sql`
defines the initial user table and PostgreSQL-backed Spring Session tables. `V2__catalog.sql`
creates the initial Catalog schema. `V3__benefit_discounts_and_exclusive_establishment.sql`
reconciles Benefit/Discount and refuses to run if any Establishment or Benefit rows exist, because
the legacy discount text cannot be split into the new fields without a reviewed mapping. Inspect and
map such data before applying V3. `V4__engagement_favorites_and_visits.sql` creates user-owned
favorite and visit records. `V5__remove_benefit_conditions.sql` archives existing Benefit condition
text and removes the field from the Benefit table. `V6__allow_shared_benefits.sql` removes the unique
constraint on `establishment.benefit_id` so multiple establishments can reference the same Benefit.
Add new migrations for later modules; never edit a migration after it has been applied to a shared
database.
