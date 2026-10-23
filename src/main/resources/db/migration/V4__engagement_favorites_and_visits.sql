CREATE TABLE user_favorite (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id INTEGER NOT NULL REFERENCES app_user (id),
    establishment_id UUID NOT NULL REFERENCES establishment (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ux_user_favorite_owner_establishment UNIQUE (user_id, establishment_id)
);

CREATE INDEX ix_user_favorite_created ON user_favorite (user_id, created_at);

CREATE TABLE user_visit (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id INTEGER NOT NULL REFERENCES app_user (id),
    establishment_id UUID NOT NULL REFERENCES establishment (id),
    occurred_at TIMESTAMPTZ NOT NULL,
    received_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ux_user_visit_event_key UNIQUE (user_id, establishment_id, occurred_at)
);

CREATE INDEX ix_user_visit_owner_time ON user_visit (user_id, occurred_at DESC);
