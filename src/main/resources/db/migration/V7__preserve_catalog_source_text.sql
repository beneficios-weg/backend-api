-- Widen existing columns without rewriting historical migrations or changing their content.
ALTER TABLE benefit
    ALTER COLUMN participation_instructions TYPE TEXT,
    DROP CONSTRAINT ck_benefit_instructions_length,
    ADD CONSTRAINT ck_benefit_instructions_not_blank CHECK
        (char_length(btrim(participation_instructions)) > 0);

ALTER TABLE benefit_discount
    ALTER COLUMN description TYPE TEXT,
    DROP CONSTRAINT ck_benefit_discount_description_length,
    ADD CONSTRAINT ck_benefit_discount_description_not_blank CHECK
        (char_length(btrim(description)) > 0);

ALTER TABLE establishment
    ALTER COLUMN name TYPE TEXT,
    ALTER COLUMN description TYPE TEXT,
    DROP CONSTRAINT ck_establishment_name_length,
    DROP CONSTRAINT ck_establishment_description_length,
    ADD CONSTRAINT ck_establishment_name_not_blank CHECK
        (char_length(btrim(name)) >= 2),
    ADD CONSTRAINT ck_establishment_description_not_blank CHECK
        (char_length(btrim(description)) > 0);
