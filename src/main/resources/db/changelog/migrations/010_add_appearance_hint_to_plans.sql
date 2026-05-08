-- liquibase formatted sql

-- changeset daniil:010_add_appearance_hint_to_plans
ALTER TABLE evening_plans ADD COLUMN appearance_hint VARCHAR(200);
