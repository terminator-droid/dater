-- liquibase formatted sql

-- changeset daniil:009_fix_phone_length_and_cascades
ALTER TABLE users ALTER COLUMN phone TYPE VARCHAR(64);

ALTER TABLE swipes DROP CONSTRAINT IF EXISTS swipes_swiper_id_fkey;
ALTER TABLE swipes DROP CONSTRAINT IF EXISTS swipes_swiped_id_fkey;
ALTER TABLE swipes ADD CONSTRAINT swipes_swiper_id_fkey FOREIGN KEY (swiper_id) REFERENCES users (id) ON DELETE CASCADE;
ALTER TABLE swipes ADD CONSTRAINT swipes_swiped_id_fkey FOREIGN KEY (swiped_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE matches DROP CONSTRAINT IF EXISTS matches_user1_id_fkey;
ALTER TABLE matches DROP CONSTRAINT IF EXISTS matches_user2_id_fkey;
ALTER TABLE matches ADD CONSTRAINT matches_user1_id_fkey FOREIGN KEY (user1_id) REFERENCES users (id) ON DELETE CASCADE;
ALTER TABLE matches ADD CONSTRAINT matches_user2_id_fkey FOREIGN KEY (user2_id) REFERENCES users (id) ON DELETE CASCADE;
