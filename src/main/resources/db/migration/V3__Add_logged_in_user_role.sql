ALTER TABLE devices
    ADD COLUMN IF NOT EXISTS logged_in_user_role VARCHAR(50);
