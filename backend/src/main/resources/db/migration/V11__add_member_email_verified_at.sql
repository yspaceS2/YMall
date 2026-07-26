ALTER TABLE members
    ADD COLUMN IF NOT EXISTS email_verified_at TIMESTAMP;

UPDATE members
SET email_verified_at = COALESCE(email_verified_at, created_at, CURRENT_TIMESTAMP)
WHERE email_verified_at IS NULL;

ALTER TABLE members
    ALTER COLUMN email_verified_at SET NOT NULL;
