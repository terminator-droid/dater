-- liquibase formatted sql

-- changeset daniil:005_create_evening_plans
CREATE TABLE evening_plans (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    venue_id      UUID        NOT NULL REFERENCES venues (id),
    date          DATE        NOT NULL,
    drink_tonight VARCHAR(100),
    status        VARCHAR(20) NOT NULL DEFAULT 'PLANNED',
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_plan_user_venue_date UNIQUE (user_id, venue_id, date)
);

CREATE INDEX idx_evening_plans_user_date ON evening_plans (user_id, date);

-- changeset daniil:005_create_evening_plan_topics
CREATE TABLE evening_plan_topics (
    plan_id  UUID        NOT NULL REFERENCES evening_plans (id) ON DELETE CASCADE,
    topic_id VARCHAR(50) NOT NULL,
    CONSTRAINT uq_plan_topic UNIQUE (plan_id, topic_id)
);
