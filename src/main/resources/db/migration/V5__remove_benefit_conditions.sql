CREATE TABLE benefit_legacy_condition_archive (
    benefit_id UUID PRIMARY KEY REFERENCES benefit (id) ON DELETE CASCADE,
    legacy_value TEXT NOT NULL
);

INSERT INTO benefit_legacy_condition_archive (benefit_id, legacy_value)
SELECT id, conditions
FROM benefit;

ALTER TABLE benefit
    DROP COLUMN conditions;
