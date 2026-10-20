CREATE TABLE catalog_category (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(75) NOT NULL,
    icon_key VARCHAR(160) NOT NULL,
    color_hex VARCHAR(7) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_catalog_category_name_length CHECK (char_length(name) BETWEEN 6 AND 75),
    CONSTRAINT ck_catalog_category_icon_key CHECK (icon_key ~ '^[a-z0-9-]+:[a-z0-9-]+$'),
    CONSTRAINT ck_catalog_category_color CHECK (color_hex IN
        ('#1B5E20', '#0D47A1', '#4A148C', '#B71C1C', '#E65100', '#006064', '#4E342E', '#37474F'))
);

CREATE INDEX ix_catalog_category_active_name ON catalog_category (active, name);

CREATE TABLE catalog_service (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(75) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_catalog_service_name_length CHECK (char_length(name) BETWEEN 3 AND 75)
);

CREATE INDEX ix_catalog_service_active_name ON catalog_service (active, name);
CREATE UNIQUE INDEX ux_catalog_service_name_lower ON catalog_service (lower(name));

CREATE TABLE establishment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(75) NOT NULL,
    description VARCHAR(300) NOT NULL,
    postal_code VARCHAR(8) NOT NULL,
    street_number INTEGER NOT NULL,
    street VARCHAR(75) NOT NULL,
    district VARCHAR(75) NOT NULL,
    city VARCHAR(75) NOT NULL,
    state CHAR(2) NOT NULL,
    latitude NUMERIC(9, 6) NOT NULL,
    longitude NUMERIC(9, 6) NOT NULL,
    altitude NUMERIC(9, 2),
    facade_image_url VARCHAR(2048),
    category_id UUID NOT NULL REFERENCES catalog_category (id),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_establishment_name_length CHECK (char_length(name) BETWEEN 2 AND 75),
    CONSTRAINT ck_establishment_description_length CHECK (char_length(description) BETWEEN 50 AND 300),
    CONSTRAINT ck_establishment_postal_code CHECK (postal_code ~ '^[0-9]{8}$'),
    CONSTRAINT ck_establishment_street_length CHECK (char_length(street) BETWEEN 3 AND 75),
    CONSTRAINT ck_establishment_district_length CHECK (char_length(district) BETWEEN 3 AND 75),
    CONSTRAINT ck_establishment_city_length CHECK (char_length(city) BETWEEN 3 AND 75),
    CONSTRAINT ck_establishment_state CHECK (state ~ '^[A-Z]{2}$'),
    CONSTRAINT ck_establishment_latitude CHECK (latitude BETWEEN -90 AND 90),
    CONSTRAINT ck_establishment_longitude CHECK (longitude BETWEEN -180 AND 180),
    CONSTRAINT ck_establishment_facade_url CHECK
        (facade_image_url IS NULL OR facade_image_url ~ '^https://[^[:space:]]+$')
);

CREATE INDEX ix_establishment_active_category_name ON establishment (active, category_id, name);

CREATE TABLE establishment_service (
    establishment_id UUID NOT NULL REFERENCES establishment (id),
    service_id UUID NOT NULL REFERENCES catalog_service (id),
    PRIMARY KEY (establishment_id, service_id)
);

CREATE INDEX ix_establishment_service_service ON establishment_service (service_id, establishment_id);

CREATE TABLE benefit (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    audience TEXT NOT NULL,
    participation_instructions VARCHAR(300) NOT NULL,
    contact TEXT NOT NULL,
    discount TEXT NOT NULL,
    conditions VARCHAR(300) NOT NULL,
    establishment_id UUID NOT NULL REFERENCES establishment (id),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_benefit_audience_not_blank CHECK (char_length(btrim(audience)) > 0),
    CONSTRAINT ck_benefit_instructions_length CHECK
        (char_length(participation_instructions) BETWEEN 50 AND 300),
    CONSTRAINT ck_benefit_contact_not_blank CHECK (char_length(btrim(contact)) > 0),
    CONSTRAINT ck_benefit_discount_not_blank CHECK (char_length(btrim(discount)) > 0),
    CONSTRAINT ck_benefit_conditions_length CHECK (char_length(conditions) BETWEEN 10 AND 300)
);

CREATE INDEX ix_benefit_establishment_active ON benefit (establishment_id, active);
