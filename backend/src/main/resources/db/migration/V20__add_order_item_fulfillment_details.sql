ALTER TABLE order_items
    ADD COLUMN IF NOT EXISTS fulfillment_status VARCHAR(20);

UPDATE order_items
SET fulfillment_status = 'PENDING'
WHERE fulfillment_status IS NULL;

ALTER TABLE order_items
    ALTER COLUMN fulfillment_status SET DEFAULT 'PENDING',
    ALTER COLUMN fulfillment_status SET NOT NULL;

ALTER TABLE order_items
    ADD COLUMN IF NOT EXISTS carrier VARCHAR(50),
    ADD COLUMN IF NOT EXISTS tracking_number VARCHAR(100),
    ADD COLUMN IF NOT EXISTS shipped_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS delivered_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_order_items_fulfillment_status
    ON order_items (fulfillment_status);
