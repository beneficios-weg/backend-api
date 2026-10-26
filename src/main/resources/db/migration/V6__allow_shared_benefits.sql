DROP INDEX IF EXISTS ux_establishment_benefit_id;

CREATE INDEX ix_establishment_benefit_id ON establishment (benefit_id);
