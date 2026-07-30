ALTER TABLE categories
    ADD COLUMN IF NOT EXISTS parent_id BIGINT,
    ADD COLUMN IF NOT EXISTS depth INTEGER NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS display_order INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_categories_parent'
    ) THEN
        ALTER TABLE categories
            ADD CONSTRAINT fk_categories_parent
            FOREIGN KEY (parent_id) REFERENCES categories(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_categories_parent_order
    ON categories(parent_id, display_order);

CREATE INDEX IF NOT EXISTS idx_categories_active_order
    ON categories(active, depth, display_order);
