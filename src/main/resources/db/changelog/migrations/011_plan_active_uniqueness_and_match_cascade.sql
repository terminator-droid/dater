-- liquibase formatted sql

-- changeset daniil:011_active_plan_partial_unique_index
CREATE UNIQUE INDEX uq_active_plan_per_user_date
    ON evening_plans (user_id, date)
    WHERE status = 'ACTIVE';

-- changeset daniil:011_match_plan_cascade
ALTER TABLE matches DROP CONSTRAINT IF EXISTS matches_plan1_id_fkey;
ALTER TABLE matches DROP CONSTRAINT IF EXISTS matches_plan2_id_fkey;
ALTER TABLE matches
    ADD CONSTRAINT matches_plan1_id_fkey
    FOREIGN KEY (plan1_id) REFERENCES evening_plans (id) ON DELETE CASCADE;
ALTER TABLE matches
    ADD CONSTRAINT matches_plan2_id_fkey
    FOREIGN KEY (plan2_id) REFERENCES evening_plans (id) ON DELETE CASCADE;
