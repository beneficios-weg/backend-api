-- Permit the approved name "Lazer" without padding or changing existing records.
ALTER TABLE catalog_category DROP CONSTRAINT ck_catalog_category_name_length;
ALTER TABLE catalog_category ADD CONSTRAINT ck_catalog_category_name_length
    CHECK (char_length(name) BETWEEN 5 AND 75);
