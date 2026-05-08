-- liquibase formatted sql

-- changeset daniil:006_create_swipes
CREATE TABLE swipes (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    swiper_id  UUID        NOT NULL REFERENCES users (id),
    swiped_id  UUID        NOT NULL REFERENCES users (id),
    venue_id   UUID        NOT NULL REFERENCES venues (id),
    direction  VARCHAR(10) NOT NULL,
    date       DATE        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_swipe UNIQUE (swiper_id, swiped_id, venue_id, date)
);

CREATE INDEX idx_swipes_swiper_date ON swipes (swiper_id, date);
CREATE INDEX idx_swipes_swiped_direction ON swipes (swiped_id, direction);
