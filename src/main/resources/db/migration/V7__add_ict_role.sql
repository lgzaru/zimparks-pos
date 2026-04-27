-- Drop the existing role check constraint and recreate it with ICT included
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;

ALTER TABLE users
    ADD CONSTRAINT users_role_check
        CHECK (role IN ('OPERATOR', 'SUPERVISOR', 'ADMIN', 'ICT'));
