DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'members'
          AND column_name = 'password'
    ) THEN
        ALTER TABLE members ALTER COLUMN password DROP NOT NULL;
    END IF;
END
$$;
