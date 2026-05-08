-- liquibase formatted sql

-- changeset daniil:008_add_google_sub_to_users
ALTER TABLE users ADD COLUMN google_sub VARCHAR(255) UNIQUE;
