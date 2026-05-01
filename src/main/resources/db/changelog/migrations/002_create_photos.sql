-- liquibase formatted sql

-- changeset daniil:002_create_photos
CREATE TABLE photos (
    id          UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID    NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    s3_key      VARCHAR(500) NOT NULL,
    position    INT     NOT NULL DEFAULT 0,
    is_primary  BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_photos_user_id ON photos (user_id);
