DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM establishment) OR EXISTS (SELECT 1 FROM benefit) THEN
        RAISE EXCEPTION 'V3 cannot migrate existing Catalog data automatically. Export and map existing benefits and establishments before retrying.'
            USING ERRCODE = 'check_violation';
    END IF;
END
$$;

ALTER TABLE benefit
    ALTER COLUMN active SET DEFAULT FALSE,
    DROP COLUMN discount,
    DROP COLUMN establishment_id,
    ADD COLUMN first_linked_at TIMESTAMPTZ;

DROP INDEX IF EXISTS ix_benefit_establishment_active;

ALTER TABLE establishment
    ADD COLUMN benefit_id UUID NOT NULL REFERENCES benefit (id);

CREATE UNIQUE INDEX ux_establishment_benefit_id ON establishment (benefit_id);

CREATE TABLE benefit_discount (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    benefit_id UUID NOT NULL REFERENCES benefit (id),
    value VARCHAR(15) NOT NULL,
    description VARCHAR(300) NOT NULL,
    CONSTRAINT ck_benefit_discount_value_length CHECK
        (char_length(btrim(value)) BETWEEN 2 AND 15),
    CONSTRAINT ck_benefit_discount_description_length CHECK
        (char_length(btrim(description)) BETWEEN 10 AND 300)
);

CREATE INDEX ix_benefit_discount_benefit_id ON benefit_discount (benefit_id);
