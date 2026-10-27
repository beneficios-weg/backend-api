-- Existing partners retain their physical location and relationships.
ALTER TABLE establishment
    ADD COLUMN channel VARCHAR(16) NOT NULL DEFAULT 'PHYSICAL',
    ALTER COLUMN postal_code DROP NOT NULL,
    ALTER COLUMN street_number DROP NOT NULL,
    ALTER COLUMN street DROP NOT NULL,
    ALTER COLUMN district DROP NOT NULL,
    ALTER COLUMN city DROP NOT NULL,
    ALTER COLUMN state DROP NOT NULL,
    ALTER COLUMN latitude DROP NOT NULL,
    ALTER COLUMN longitude DROP NOT NULL,
    ADD CONSTRAINT ck_establishment_channel CHECK
        (channel IN ('PHYSICAL', 'ONLINE', 'HYBRID')),
    ADD CONSTRAINT ck_establishment_channel_location CHECK (
        (channel = 'ONLINE'
            AND postal_code IS NULL AND street_number IS NULL AND street IS NULL
            AND district IS NULL AND city IS NULL AND state IS NULL
            AND latitude IS NULL AND longitude IS NULL AND altitude IS NULL)
        OR
        (channel IN ('PHYSICAL', 'HYBRID')
            AND postal_code IS NOT NULL AND street_number IS NOT NULL AND street IS NOT NULL
            AND district IS NOT NULL AND city IS NOT NULL AND state IS NOT NULL
            AND latitude IS NOT NULL AND longitude IS NOT NULL)
    );

CREATE INDEX ix_establishment_channel_active ON establishment (channel, active);
