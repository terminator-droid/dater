-- liquibase formatted sql

-- changeset daniil:007_create_matches
CREATE TABLE matches (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user1_id   UUID        NOT NULL REFERENCES users (id),
    user2_id   UUID        NOT NULL REFERENCES users (id),
    venue_id   UUID        NOT NULL REFERENCES venues (id),
    plan1_id   UUID        NOT NULL REFERENCES evening_plans (id),
    plan2_id   UUID        NOT NULL REFERENCES evening_plans (id),
    date       DATE        NOT NULL,
    status     VARCHAR(10) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_match UNIQUE (user1_id, user2_id, date)
);

CREATE INDEX idx_matches_user1_date ON matches (user1_id, date);
CREATE INDEX idx_matches_user2_date ON matches (user2_id, date);
