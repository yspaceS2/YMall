ALTER TABLE products
    ADD COLUMN IF NOT EXISTS approved_at TIMESTAMP WITHOUT TIME ZONE;

UPDATE products
SET approved_at = COALESCE(updated_at, created_at)
WHERE status = 'APPROVED'
  AND approved_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_products_merchandising
    ON products (status, stock, approved_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_payments_merchandising_sales
    ON payments (result, approved_at DESC, order_id);
